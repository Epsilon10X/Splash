package com.splash.offline.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.splash.offline.SplashNotificationScheduler
import com.splash.offline.audio.SplashAudioManager
import com.splash.offline.data.ActiveSession
import com.splash.offline.data.AppLanguage
import com.splash.offline.data.AppTab
import com.splash.offline.data.BiomeCelebrationEvent
import com.splash.offline.data.BiomeLayoutMode
import com.splash.offline.data.CatalogFilter
import com.splash.offline.data.CompletionLog
import com.splash.offline.data.DaySchedule
import com.splash.offline.data.GoalCategory
import com.splash.offline.data.GoalItem
import com.splash.offline.data.MapStyle
import com.splash.offline.data.PendingCameraGoal
import com.splash.offline.data.PendingNoteCompletion
import com.splash.offline.data.PhotoStorageHelper
import com.splash.offline.data.SplashCatalogData
import com.splash.offline.data.SplashRepository
import com.splash.offline.data.SplashStrings
import com.splash.offline.data.SplashUiState
import com.splash.offline.data.ThemeMode
import com.splash.offline.data.TimeBucket
import com.splash.offline.data.VerificationType
import com.splash.offline.data.WallSubTab
import com.splash.offline.data.WidgetThemePref
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SplashRepository(application)
    val audioManager = SplashAudioManager(application)

    private val baseCatalog = repo.loadCatalog()
    private val _uiState = MutableStateFlow(repo.loadState(baseCatalog))
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val notifPrefs = application.getSharedPreferences(
        SplashNotificationScheduler.PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val notifPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == SplashNotificationScheduler.KEY_PENDING_NOTIF_GOAL_ID ||
            key == SplashNotificationScheduler.KEY_PENDING_NOTIF_GOAL_TS
        ) {
            syncPendingNotifiedGoal()
        }
    }

    init {
        ensureValidDailyPicks()
        syncPendingNotifiedGoal()
        repo.syncNativeIntegrations(_uiState.value)
        notifPrefs.registerOnSharedPreferenceChangeListener(notifPrefsListener)
    }

    private fun updateAndPersist(
        forceRescheduleNotifs: Boolean = false,
        transform: (SplashUiState) -> SplashUiState
    ) {
        _uiState.update { current ->
            val updated = transform(current)
            repo.saveState(updated, forceRescheduleNotifs)
            updated
        }
    }

    private fun ensureValidDailyPicks() {
        val st = _uiState.value
        val activeBiomeId = st.activeBiome.id
        val biomeQuick = st.catalog.firstOrNull {
            !it.isHidden && it.biomeExclusive == activeBiomeId && it.category == GoalCategory.RAPIDO
        } ?: st.catalog.firstOrNull { !it.isHidden && it.category == GoalCategory.RAPIDO }
        val biomeMed = st.catalog.firstOrNull {
            !it.isHidden && it.biomeExclusive == activeBiomeId && it.category == GoalCategory.MEDIO
        } ?: st.catalog.firstOrNull { !it.isHidden && it.category == GoalCategory.MEDIO }

        val genQuick = st.catalog.firstOrNull { it.id == st.currentQuickIds.first && !it.isHidden }
            ?: st.catalog.firstOrNull { !it.isHidden && it.biomeExclusive == null && it.category == GoalCategory.RAPIDO }
        val genMed = st.catalog.firstOrNull { it.id == st.currentMediumIds.first && !it.isHidden }
            ?: st.catalog.firstOrNull { !it.isHidden && it.biomeExclusive == null && it.category == GoalCategory.MEDIO }

        updateAndPersist { cur ->
            cur.copy(
                currentQuickIds = (genQuick?.id ?: 1) to (biomeQuick?.id ?: 101),
                currentMediumIds = (genMed?.id ?: 21) to (biomeMed?.id ?: 102)
            )
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectTimeBucket(bucket: TimeBucket) {
        _uiState.update { it.copy(timeBucket = bucket) }
    }

    fun selectWallSubTab(subTab: WallSubTab) {
        _uiState.update { it.copy(wallSubTab = subTab) }
    }

    fun selectMapStyle(style: MapStyle) {
        _uiState.update { it.copy(mapStyle = style) }
    }

    fun toggleMapShowAllColor() {
        _uiState.update { it.copy(mapShowAllColor = !it.mapShowAllColor) }
    }

    fun selectBiomeLayout(mode: BiomeLayoutMode) {
        _uiState.update { it.copy(biomeLayoutMode = mode) }
    }

    fun switchBiome(index: Int) {
        val st = _uiState.value
        val maxAllowed = if (st.betaMode) SplashCatalogData.BIOMES.lastIndex else (st.unlockedBiomeCount - 1)
        val clamped = index.coerceIn(0, maxAllowed)
        val targetBiomeId = SplashCatalogData.BIOMES[clamped].id
        val bQuick = st.catalog.firstOrNull { it.biomeExclusive == targetBiomeId && it.category == GoalCategory.RAPIDO }?.id
            ?: st.currentQuickIds.second
        val bMed = st.catalog.firstOrNull { it.biomeExclusive == targetBiomeId && it.category == GoalCategory.MEDIO }?.id
            ?: st.currentMediumIds.second
        updateAndPersist {
            it.copy(
                activeBiomeIndex = clamped,
                betaStageOverride = null,
                currentQuickIds = it.currentQuickIds.first to bQuick,
                currentMediumIds = it.currentMediumIds.first to bMed
            )
        }
    }

    fun rerollSuggestions() {
        val st = _uiState.value
        val activeBiomeId = st.activeBiome.id
        when (st.timeBucket) {
            TimeBucket.QUICK_2M -> {
                if (st.quickUsedToday >= 2 && st.extraQuickGoalId != null) {
                    requestExtraGoal(TimeBucket.QUICK_2M)
                    return
                }
                val pool = st.catalog.filter {
                    !it.isHidden && it.category == GoalCategory.RAPIDO && it.biomeExclusive == null && it.id != st.currentQuickIds.first
                }
                val picked = pool.randomOrNull() ?: return
                val bPool = st.catalog.filter { !it.isHidden && it.biomeExclusive == activeBiomeId && it.category == GoalCategory.RAPIDO }
                val bPicked = bPool.firstOrNull() ?: picked
                updateAndPersist { it.copy(currentQuickIds = picked.id to bPicked.id) }
            }
            TimeBucket.MEDIUM_15M -> {
                if (st.mediumUsedToday >= 1 && st.extraMediumGoalId != null) {
                    requestExtraGoal(TimeBucket.MEDIUM_15M)
                    return
                }
                val pool = st.catalog.filter {
                    !it.isHidden && it.category == GoalCategory.MEDIO && it.biomeExclusive == null && it.id != st.currentMediumIds.first
                }
                val picked = pool.randomOrNull() ?: return
                val bPool = st.catalog.filter { !it.isHidden && it.biomeExclusive == activeBiomeId && it.category == GoalCategory.MEDIO }
                val bPicked = bPool.firstOrNull() ?: picked
                updateAndPersist { it.copy(currentMediumIds = picked.id to bPicked.id) }
            }
            TimeBucket.WEEKLY -> {
                if (st.weeklyUsedThisWeek >= 4 && st.extraWeeklyGoalId != null) {
                    requestExtraGoal(TimeBucket.WEEKLY)
                    return
                }
                val pool = st.catalog.filter { !it.isHidden && it.category == GoalCategory.LARGO }
                val next3 = pool.shuffled().take(3).map { it.id }
                if (next3.isNotEmpty()) {
                    updateAndPersist { it.copy(currentWeeklyIds = next3) }
                }
            }
        }
    }

    fun requestExtraGoal(bucket: TimeBucket) {
        val st = _uiState.value
        when (bucket) {
            TimeBucket.QUICK_2M -> {
                val allQuick = st.catalog.filter { !it.isHidden && it.category == GoalCategory.RAPIDO }
                val pool = allQuick.filter { it.id != st.extraQuickGoalId }
                val picked = (if (pool.isNotEmpty()) pool else allQuick).randomOrNull() ?: return
                _uiState.update { it.copy(extraQuickGoalId = picked.id) }
                repo.syncNativeIntegrations(_uiState.value)
            }
            TimeBucket.MEDIUM_15M -> {
                val allMed = st.catalog.filter { !it.isHidden && it.category == GoalCategory.MEDIO }
                val pool = allMed.filter { it.id != st.extraMediumGoalId }
                val picked = (if (pool.isNotEmpty()) pool else allMed).randomOrNull() ?: return
                _uiState.update { it.copy(extraMediumGoalId = picked.id) }
                repo.syncNativeIntegrations(_uiState.value)
            }
            TimeBucket.WEEKLY -> {
                val allWeekly = st.catalog.filter { !it.isHidden && it.category == GoalCategory.LARGO }
                val pool = allWeekly.filter { it.id != st.extraWeeklyGoalId }
                val picked = (if (pool.isNotEmpty()) pool else allWeekly).randomOrNull() ?: return
                _uiState.update { it.copy(extraWeeklyGoalId = picked.id) }
                repo.syncNativeIntegrations(_uiState.value)
            }
        }
    }

    fun dismissExtraGoal(bucket: TimeBucket) {
        _uiState.update {
            when (bucket) {
                TimeBucket.QUICK_2M -> it.copy(extraQuickGoalId = null)
                TimeBucket.MEDIUM_15M -> it.copy(extraMediumGoalId = null)
                TimeBucket.WEEKLY -> it.copy(extraWeeklyGoalId = null)
            }
        }
        repo.syncNativeIntegrations(_uiState.value)
    }

    private fun computeRemainingSec(s: ActiveSession): Int {
        val endMs = s.runningEndElapsedMs ?: return s.remainingSeconds
        val diffMs = endMs - SystemClock.elapsedRealtime()
        if (diffMs <= 0L) return 0
        return ((diffMs + 999L) / 1000L).toInt().coerceIn(0, s.totalSeconds)
    }

    fun startGoal(
        goal: GoalItem,
        isBiomeMission: Boolean = false,
        isExtra: Boolean = false,
        isFromNotification: Boolean = false
    ) {
        when (goal.verification) {
            VerificationType.TEMPORIZADOR, VerificationType.PUENTE_ANALOGICO -> {
                val mins = if (goal.category == GoalCategory.MEDIO) 15 else if (goal.category == GoalCategory.LARGO) 30 else 2
                val totalSec = (mins * 60).coerceAtLeast(30)
                val sessionKey = "goal_${goal.id}"
                val st = _uiState.value
                val existing = st.activeSession
                if (existing != null && existing.sessionKey == sessionKey) {
                    return
                }
                val savedRem = st.savedTimerRemainingSec[sessionKey]?.coerceIn(1, totalSec) ?: totalSec
                _uiState.update {
                    it.copy(
                        activeSession = ActiveSession(
                            sessionKey = sessionKey,
                            goal = goal,
                            durationMinutes = mins,
                            totalSeconds = totalSec,
                            remainingSeconds = savedRem,
                            isRunning = false,
                            runningEndElapsedMs = null,
                            pausedByAppExit = false,
                            isFinished = false,
                            isBiomeMission = isBiomeMission,
                            isExtra = isExtra,
                            isFromWidget = false,
                            isFromNotification = isFromNotification
                        )
                    )
                }
            }
            VerificationType.SOCIAL, VerificationType.TOQUE -> {
                _uiState.update {
                    it.copy(
                        pendingNoteCompletion = PendingNoteCompletion(
                            goal = goal,
                            isBiomeMission = isBiomeMission,
                            isExtra = isExtra,
                            isFromNotification = isFromNotification
                        )
                    )
                }
            }
        }
    }

    fun confirmPendingNoteCompletion(note: String?) {
        val pending = _uiState.value.pendingNoteCompletion ?: return
        _uiState.update { it.copy(pendingNoteCompletion = null) }
        completeGoalNow(
            goal = pending.goal,
            isBiomeMission = pending.isBiomeMission,
            isExtra = pending.isExtra,
            photoPath = pending.photoPath,
            isFromNotification = pending.isFromNotification,
            note = note
        )
    }

    fun dismissPendingNoteCompletion() {
        _uiState.update { it.copy(pendingNoteCompletion = null) }
    }

    fun deleteMemoryPhoto(completionId: Long) {
        val target = _uiState.value.completions.firstOrNull { it.id == completionId } ?: return
        target.photoPath?.let { path ->
            runCatching { File(path).delete() }
        }
        updateAndPersist { cur ->
            val nextList = if (target.goalId == 0) {
                cur.completions.filterNot { it.id == completionId }
            } else {
                cur.completions.map { if (it.id == completionId) it.copy(photoPath = null) else it }
            }
            cur.copy(
                completions = nextList,
                toastMessage = SplashStrings.t(cur.lang, "Foto eliminada", "Photo deleted")
            )
        }
    }

    fun updateMemoryBiome(completionId: Long, biomeId: String) {
        if (SplashCatalogData.BIOMES.none { it.id == biomeId }) return
        updateAndPersist { cur ->
            cur.copy(
                completions = cur.completions.map {
                    if (it.id == completionId) it.copy(biomeId = biomeId) else it
                },
                toastMessage = SplashStrings.t(cur.lang, "Foto organizada en bioma", "Photo moved to biome")
            )
        }
    }

    fun openWidgetTimer(minutes: Int) {
        val m = minutes.coerceIn(1, 120)
        val totalSec = (m * 60).coerceAtLeast(30)
        val sessionKey = "widget_$m"
        val st = _uiState.value
        val existing = st.activeSession
        if (existing != null && existing.sessionKey == sessionKey) {
            return
        }
        val savedRem = st.savedTimerRemainingSec[sessionKey]?.coerceIn(1, totalSec) ?: totalSec
        val dummyGoal = GoalItem(
            id = 9000 + m,
            titleShortEs = "Temporizador $m min",
            titleEs = "Enfoque sin pantalla ($m min)",
            subEs = "Temporizador AMOLED",
            titleShortEn = "$m min Timer",
            titleEn = "Screen-free Focus ($m min)",
            subEn = "AMOLED Timer",
            category = if (m <= 5) GoalCategory.RAPIDO else if (m <= 20) GoalCategory.MEDIO else GoalCategory.LARGO,
            iconName = "wb_sunny",
            verification = VerificationType.TEMPORIZADOR
        )
        _uiState.update {
            it.copy(
                activeSession = ActiveSession(
                    sessionKey = sessionKey,
                    goal = dummyGoal,
                    durationMinutes = m,
                    totalSeconds = totalSec,
                    remainingSeconds = savedRem,
                    isRunning = false,
                    runningEndElapsedMs = null,
                    pausedByAppExit = false,
                    isFinished = false,
                    isFromWidget = true
                )
            )
        }
    }

    fun toggleActiveTimerRunning() {
        val s = _uiState.value.activeSession ?: return
        if (s.isFinished) return
        val app = getApplication<Application>()
        if (s.isRunning) {
            val rem = computeRemainingSec(s)
            SplashNotificationScheduler.cancelTimerDone(app)
            val isPartial = rem in 1 until s.totalSeconds
            val nextTimers = _uiState.value.savedTimerRemainingSec.toMutableMap().apply {
                if (isPartial) put(s.sessionKey, rem) else remove(s.sessionKey)
            }
            updateAndPersist { cur ->
                cur.copy(
                    savedTimerRemainingSec = nextTimers,
                    activeSession = s.copy(
                        isRunning = false,
                        runningEndElapsedMs = null,
                        remainingSeconds = rem,
                        pausedByAppExit = false
                    )
                )
            }
        } else {
            val rem = s.remainingSeconds.coerceIn(1, s.totalSeconds)
            val endElapsed = SystemClock.elapsedRealtime() + rem * 1000L
            val triggerWall = System.currentTimeMillis() + rem * 1000L
            val lang = _uiState.value.lang
            SplashNotificationScheduler.scheduleTimerDone(
                app,
                triggerWall,
                SplashStrings.t(lang, "Temporizador completado", "Timer finished"),
                s.goal.displayTitle(lang),
                s.sessionKey
            )
            val nextTimers = _uiState.value.savedTimerRemainingSec.toMutableMap().apply {
                if (rem < s.totalSeconds) put(s.sessionKey, rem) else remove(s.sessionKey)
            }
            updateAndPersist { cur ->
                cur.copy(
                    savedTimerRemainingSec = nextTimers,
                    activeSession = s.copy(
                        isRunning = true,
                        runningEndElapsedMs = endElapsed,
                        remainingSeconds = rem,
                        pausedByAppExit = false
                    )
                )
            }
        }
    }

    fun resetActiveTimer() {
        val s = _uiState.value.activeSession ?: return
        SplashNotificationScheduler.cancelTimerDone(getApplication())
        val nextTimers = _uiState.value.savedTimerRemainingSec.toMutableMap().apply {
            remove(s.sessionKey)
        }
        updateAndPersist { cur ->
            cur.copy(
                savedTimerRemainingSec = nextTimers,
                activeSession = s.copy(
                    remainingSeconds = s.totalSeconds,
                    isRunning = false,
                    runningEndElapsedMs = null,
                    pausedByAppExit = false,
                    isFinished = false
                )
            )
        }
    }

    fun tickActiveTimer() {
        val s = _uiState.value.activeSession ?: return
        if (!s.isRunning || s.isFinished) return
        val rem = computeRemainingSec(s)
        if (rem <= 0) {
            val app = getApplication<Application>()
            SplashNotificationScheduler.cancelTimerDone(app)
            val alreadyChimed = SplashNotificationScheduler.consumeTimerChimePlayed(app, s.sessionKey)
            if (!alreadyChimed) {
                audioManager.playGentleCompletionChime()
            }
            triggerHaptic()
            val nextTimers = _uiState.value.savedTimerRemainingSec.toMutableMap().apply {
                remove(s.sessionKey)
            }
            updateAndPersist { cur ->
                cur.copy(
                    savedTimerRemainingSec = nextTimers,
                    activeSession = s.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        runningEndElapsedMs = null,
                        pausedByAppExit = false,
                        isFinished = true
                    )
                )
            }
        } else if (rem != s.remainingSeconds) {
            _uiState.update { cur ->
                val active = cur.activeSession
                if (active != null && active.sessionKey == s.sessionKey) {
                    cur.copy(activeSession = active.copy(remainingSeconds = rem))
                } else cur
            }
        }
    }

    fun onAppBackgroundedWithScreenOn() {
        val s = _uiState.value.activeSession
        audioManager.stop()
        if (s != null && s.isRunning) {
            val rem = computeRemainingSec(s)
            SplashNotificationScheduler.cancelTimerDone(getApplication())
            val isPartial = rem in 1 until s.totalSeconds
            val nextTimers = _uiState.value.savedTimerRemainingSec.toMutableMap().apply {
                if (isPartial) put(s.sessionKey, rem) else remove(s.sessionKey)
            }
            updateAndPersist { cur ->
                cur.copy(
                    isAmbientAudioPlaying = false,
                    savedTimerRemainingSec = nextTimers,
                    activeSession = s.copy(
                        isRunning = false,
                        runningEndElapsedMs = null,
                        remainingSeconds = rem,
                        pausedByAppExit = true
                    )
                )
            }
        } else if (_uiState.value.isAmbientAudioPlaying) {
            _uiState.update { it.copy(isAmbientAudioPlaying = false) }
        }
    }

    fun onAppForegrounded() {
        val today = LocalDate.now().toString()
        val weekIso = repo.currentWeekIso()
        val cur = _uiState.value
        if (cur.todayDateIso != today || cur.todayWeekIso != weekIso) {
            val dayChanged = cur.todayDateIso != today
            val weekChanged = cur.todayWeekIso != weekIso
            updateAndPersist { st ->
                val activeBiomeId = st.activeBiome.id
                val nextQuick = if (dayChanged) {
                    val qGen = st.catalog.filter { !it.isHidden && it.category == GoalCategory.RAPIDO && it.biomeExclusive == null }
                        .randomOrNull()?.id ?: st.currentQuickIds.first
                    val qBio = st.catalog.firstOrNull { !it.isHidden && it.biomeExclusive == activeBiomeId && it.category == GoalCategory.RAPIDO }?.id
                        ?: st.currentQuickIds.second
                    qGen to qBio
                } else st.currentQuickIds
                val nextMed = if (dayChanged) {
                    val mGen = st.catalog.filter { !it.isHidden && it.category == GoalCategory.MEDIO && it.biomeExclusive == null }
                        .randomOrNull()?.id ?: st.currentMediumIds.first
                    val mBio = st.catalog.firstOrNull { !it.isHidden && it.biomeExclusive == activeBiomeId && it.category == GoalCategory.MEDIO }?.id
                        ?: st.currentMediumIds.second
                    mGen to mBio
                } else st.currentMediumIds
                val nextWeekly = if (weekChanged) {
                    st.catalog.filter { !it.isHidden && it.category == GoalCategory.LARGO }
                        .shuffled()
                        .take(3)
                        .map { it.id }
                        .ifEmpty { st.currentWeeklyIds }
                } else st.currentWeeklyIds
                st.copy(
                    todayDateIso = today,
                    todayWeekIso = weekIso,
                    quickUsedToday = if (dayChanged) 0 else st.quickUsedToday,
                    mediumUsedToday = if (dayChanged) 0 else st.mediumUsedToday,
                    surpriseUsedToday = if (dayChanged) 0 else st.surpriseUsedToday,
                    extraQuickGoalId = if (dayChanged) null else st.extraQuickGoalId,
                    extraMediumGoalId = if (dayChanged) null else st.extraMediumGoalId,
                    weeklyUsedThisWeek = if (weekChanged) 0 else st.weeklyUsedThisWeek,
                    extraWeeklyGoalId = if (weekChanged) null else st.extraWeeklyGoalId,
                    currentQuickIds = nextQuick,
                    currentMediumIds = nextMed,
                    currentWeeklyIds = nextWeekly
                )
            }
        } else {
            repo.syncNativeIntegrations(cur)
        }
        syncPendingNotifiedGoal()
        if (_uiState.value.activeSession?.isRunning == true) {
            tickActiveTimer()
        }
    }

    private fun syncPendingNotifiedGoal() {
        val pendingId = SplashNotificationScheduler.getPendingNotificationGoalId(getApplication())
        if (pendingId > 0) {
            val exists = _uiState.value.catalog.any { it.id == pendingId && !it.isHidden }
            if (exists) {
                _uiState.update { it.copy(notifiedGoalId = pendingId) }
            } else {
                SplashNotificationScheduler.clearPendingNotificationGoal(getApplication())
            }
        }
    }

    fun openNotifiedGoal(goalId: Int) {
        val exists = _uiState.value.catalog.any { it.id == goalId && !it.isHidden }
        if (!exists) return
        SplashNotificationScheduler.savePendingNotificationGoal(getApplication(), goalId)
        _uiState.update {
            it.copy(
                notifiedGoalId = goalId,
                activeTab = AppTab.HOY,
                isSettingsOpen = false,
                isFeedbackOpen = false,
                is3DOpen = false
            )
        }
    }

    fun dismissNotifiedGoal() {
        SplashNotificationScheduler.clearPendingNotificationGoal(getApplication())
        _uiState.update { it.copy(notifiedGoalId = null) }
    }

    fun openGoalPhotoCapture(goal: GoalItem, isBiomeMission: Boolean = false, isExtra: Boolean = false) {
        _uiState.update {
            it.copy(pendingCameraGoal = PendingCameraGoal(goal = goal, isBiomeMission = isBiomeMission, isExtra = isExtra, isStandaloneMemory = false))
        }
    }

    fun openStandalonePhotoCapture() {
        _uiState.update {
            it.copy(pendingCameraGoal = PendingCameraGoal(goal = null, isStandaloneMemory = true))
        }
    }

    fun openCameraForPendingNote() {
        val pending = _uiState.value.pendingNoteCompletion ?: return
        _uiState.update {
            it.copy(
                pendingCameraGoal = PendingCameraGoal(
                    goal = pending.goal,
                    isBiomeMission = pending.isBiomeMission,
                    isExtra = pending.isExtra,
                    isStandaloneMemory = false,
                    isForPendingNote = true
                )
            )
        }
    }

    fun attachGalleryPhotoToPendingNote(sourceUri: Uri) {
        val savedPath = PhotoStorageHelper.saveNormalizedPhoto(getApplication(), sourceUri)
        if (savedPath == null) {
            showToast(SplashStrings.t(_uiState.value.lang, "No se pudo importar la foto", "Could not import photo"))
            return
        }
        _uiState.update { cur ->
            val curPending = cur.pendingNoteCompletion ?: return@update cur
            cur.copy(pendingNoteCompletion = curPending.copy(photoPath = savedPath))
        }
    }

    fun removePhotoFromPendingNote() {
        _uiState.update { cur ->
            val curPending = cur.pendingNoteCompletion ?: return@update cur
            cur.copy(pendingNoteCompletion = curPending.copy(photoPath = null))
        }
    }

    fun dismissCameraCapture() {
        _uiState.update { it.copy(pendingCameraGoal = null) }
    }

    fun saveCameraMemoryUri(sourceUri: Uri) {
        val pending = _uiState.value.pendingCameraGoal ?: return
        val app = getApplication<Application>()
        val savedPath = PhotoStorageHelper.saveNormalizedPhoto(app, sourceUri)
        _uiState.update { it.copy(pendingCameraGoal = null) }
        if (savedPath == null) {
            showToast(SplashStrings.t(_uiState.value.lang, "No se pudo guardar la foto", "Could not save photo"))
            return
        }
        if (pending.isForPendingNote) {
            _uiState.update { cur ->
                val curPending = cur.pendingNoteCompletion ?: return@update cur
                cur.copy(pendingNoteCompletion = curPending.copy(photoPath = savedPath))
            }
            return
        }
        val targetGoal = pending.goal
        if (pending.isStandaloneMemory || targetGoal == null) {
            appendStandalonePhotoMemory(savedPath)
        } else {
            _uiState.update {
                it.copy(
                    pendingNoteCompletion = PendingNoteCompletion(
                        goal = targetGoal,
                        isBiomeMission = pending.isBiomeMission,
                        isExtra = pending.isExtra,
                        photoPath = savedPath
                    )
                )
            }
        }
    }

    fun saveGalleryMemoryUri(sourceUri: Uri) {
        val app = getApplication<Application>()
        val savedPath = PhotoStorageHelper.saveNormalizedPhoto(app, sourceUri)
        if (savedPath == null) {
            showToast(SplashStrings.t(_uiState.value.lang, "No se pudo importar la foto", "Could not import photo"))
            return
        }
        appendStandalonePhotoMemory(savedPath)
    }

    private fun appendStandalonePhotoMemory(savedPath: String) {
        val st = _uiState.value
        val today = LocalDate.now().toString()
        val newLog = CompletionLog(
            id = System.currentTimeMillis(),
            dateIso = today,
            timestampMs = System.currentTimeMillis(),
            goalId = 0,
            titleEs = "Recuerdo sin pantalla",
            titleEn = "Screen-free memory",
            category = GoalCategory.LARGO,
            biomeId = st.activeBiome.id,
            photoPath = savedPath,
            earnedXp = 0
        )
        updateAndPersist { cur ->
            cur.copy(
                completions = listOf(newLog) + cur.completions,
                toastMessage = SplashStrings.t(cur.lang, "Foto guardada en Recuerdos", "Photo saved to Memories")
            )
        }
    }

    fun exportMemoryPhotoToGallery(filePath: String, title: String) {
        val ok = PhotoStorageHelper.exportPhotoToDeviceGallery(getApplication(), filePath, title)
        val lang = _uiState.value.lang
        showToast(
            if (ok) SplashStrings.t(lang, "Foto guardada en la galería del móvil", "Photo saved to device gallery")
            else SplashStrings.t(lang, "No se pudo guardar en la galería", "Could not save to gallery")
        )
    }

    fun closeActiveSession() {
        val s = _uiState.value.activeSession
        audioManager.stop()
        SplashNotificationScheduler.cancelTimerDone(getApplication())
        if (s != null) {
            val rem = if (s.isRunning) computeRemainingSec(s) else s.remainingSeconds
            val isPartial = !s.isFinished && rem in 1 until s.totalSeconds
            val nextTimers = _uiState.value.savedTimerRemainingSec.toMutableMap().apply {
                if (isPartial) put(s.sessionKey, rem) else remove(s.sessionKey)
            }
            updateAndPersist { cur ->
                cur.copy(
                    activeSession = null,
                    isAmbientAudioPlaying = false,
                    savedTimerRemainingSec = nextTimers
                )
            }
        } else {
            _uiState.update { it.copy(activeSession = null, isAmbientAudioPlaying = false) }
        }
    }

    fun completeActiveSession() {
        val session = _uiState.value.activeSession ?: return
        audioManager.stop()
        SplashNotificationScheduler.cancelTimerDone(getApplication())
        val nextTimers = _uiState.value.savedTimerRemainingSec.toMutableMap().apply {
            remove(session.sessionKey)
        }
        if (session.isFromWidget) {
            triggerHaptic()
            updateAndPersist { cur ->
                cur.copy(
                    activeSession = null,
                    isAmbientAudioPlaying = false,
                    savedTimerRemainingSec = nextTimers,
                    toastMessage = SplashStrings.t(cur.lang, "Temporizador completado.", "Timer finished.")
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    activeSession = null,
                    isAmbientAudioPlaying = false,
                    savedTimerRemainingSec = nextTimers,
                    pendingNoteCompletion = PendingNoteCompletion(
                        goal = session.goal,
                        isBiomeMission = session.isBiomeMission,
                        isExtra = session.isExtra,
                        isFromNotification = session.isFromNotification
                    )
                )
            }
        }
    }

    fun previewXpFor(pending: PendingNoteCompletion): Int {
        val st = _uiState.value
        val weekIso = repo.currentWeekIso()
        val prevWeekly = if (st.todayWeekIso == weekIso) st.weeklyUsedThisWeek else 0
        return when {
            pending.isFromNotification -> {
                if (st.surpriseUsedToday >= 5) 0
                else if (pending.goal.category == GoalCategory.MEDIO) 2 else 1
            }
            pending.isExtra -> 0
            pending.goal.category == GoalCategory.RAPIDO && st.quickUsedToday >= 2 -> 0
            pending.goal.category == GoalCategory.MEDIO && st.mediumUsedToday >= 1 -> 0
            pending.goal.category == GoalCategory.LARGO && prevWeekly >= 4 -> 0
            pending.goal.category == GoalCategory.RAPIDO -> 2
            pending.goal.category == GoalCategory.MEDIO -> 3
            else -> 4
        }
    }

    fun completeGoalNow(
        goal: GoalItem,
        isBiomeMission: Boolean = false,
        isExtra: Boolean = false,
        photoPath: String? = null,
        isFromNotification: Boolean = false,
        note: String? = null
    ) {
        triggerHaptic()
        val st = _uiState.value
        val today = LocalDate.now().toString()
        val weekIso = repo.currentWeekIso()
        val prevWeekly = if (st.todayWeekIso == weekIso) st.weeklyUsedThisWeek else 0
        val xpGain = when {
            isFromNotification -> {
                if (st.surpriseUsedToday >= 5) 0
                else if (goal.category == GoalCategory.MEDIO) 2 else 1
            }
            isExtra -> 0
            goal.category == GoalCategory.RAPIDO && st.quickUsedToday >= 2 -> 0
            goal.category == GoalCategory.MEDIO && st.mediumUsedToday >= 1 -> 0
            goal.category == GoalCategory.LARGO && prevWeekly >= 4 -> 0
            goal.category == GoalCategory.RAPIDO -> 2
            goal.category == GoalCategory.MEDIO -> 3
            else -> 4
        }

        val activeBiome = st.activeBiome
        val prevXp = st.biomeXpById[activeBiome.id] ?: 0
        val nextXp = (prevXp + xpGain).coerceAtMost(SplashCatalogData.MAX_BIOME_XP)
        val nextMap = st.biomeXpById.toMutableMap().apply { put(activeBiome.id, nextXp) }

        var nextUnlocked = st.unlockedBiomeCount
        var unlockTargetIdx: Int? = null
        var unlockedBanner = false
        if (nextXp >= SplashCatalogData.MAX_BIOME_XP &&
            st.activeBiomeIndex == st.unlockedBiomeCount - 1 &&
            st.unlockedBiomeCount < SplashCatalogData.BIOMES.size
        ) {
            nextUnlocked = (st.unlockedBiomeCount + 1).coerceAtMost(SplashCatalogData.BIOMES.size)
            val targetIdx = nextUnlocked - 1
            unlockTargetIdx = targetIdx
            nextMap.putIfAbsent(SplashCatalogData.BIOMES[targetIdx].id, 0)
            unlockedBanner = true
        }

        val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
        val newLog = CompletionLog(
            id = System.currentTimeMillis(),
            dateIso = today,
            timestampMs = System.currentTimeMillis(),
            goalId = goal.id,
            titleEs = goal.titleEs,
            titleEn = goal.titleEn,
            category = goal.category,
            biomeId = activeBiome.id,
            photoPath = photoPath,
            earnedXp = xpGain,
            note = cleanNote
        )

        val nextBiomeDef = unlockTargetIdx?.let { SplashCatalogData.BIOMES.getOrNull(it) }
        val msg = if (unlockedBanner && nextBiomeDef != null) {
            val newBiomeName = nextBiomeDef.name(st.lang)
            SplashStrings.t(st.lang, "Bioma completado. Desbloqueaste: $newBiomeName", "Biome completed. Unlocked: $newBiomeName")
        } else if (xpGain > 0) {
            SplashStrings.t(st.lang, "Hecho (+$xpGain pt bioma). Ya puedes bloquear el móvil.", "Done (+$xpGain biome pt). You can lock your phone now.")
        } else if (isExtra || isFromNotification) {
            SplashStrings.t(st.lang, "Completado en Muro Real (+0 pts bioma).", "Saved to Real Wall (+0 biome pts).")
        } else {
            SplashStrings.t(st.lang, "Completado en Muro Real.", "Saved to Real Wall.")
        }

        val clearedNotified = st.notifiedGoalId == goal.id
        if (clearedNotified) {
            SplashNotificationScheduler.clearPendingNotificationGoal(getApplication())
        }

        val celebrationEvent = if (xpGain > 0 || unlockedBanner) {
            BiomeCelebrationEvent(
                id = System.currentTimeMillis(),
                category = goal.category,
                earnedXp = xpGain,
                isSurprise = isFromNotification,
                prevXp = prevXp,
                nextXp = nextXp,
                biomeUnlocked = unlockedBanner,
                completedBiomeId = activeBiome.id,
                completedBiomeName = activeBiome.name(st.lang),
                nextBiomeId = nextBiomeDef?.id,
                nextBiomeIndex = unlockTargetIdx,
                nextBiomeName = nextBiomeDef?.name(st.lang)
            )
        } else null

        updateAndPersist { cur ->
            val baseWeekly = if (cur.todayWeekIso == weekIso) cur.weeklyUsedThisWeek else 0
            cur.copy(
                todayDateIso = today,
                todayWeekIso = weekIso,
                quickUsedToday = if (goal.category == GoalCategory.RAPIDO && !isExtra && !isFromNotification) cur.quickUsedToday + 1 else cur.quickUsedToday,
                mediumUsedToday = if (goal.category == GoalCategory.MEDIO && !isExtra && !isFromNotification) cur.mediumUsedToday + 1 else cur.mediumUsedToday,
                weeklyUsedThisWeek = if (goal.category == GoalCategory.LARGO && !isExtra && !isFromNotification) baseWeekly + 1 else baseWeekly,
                surpriseUsedToday = if (isFromNotification && xpGain > 0) cur.surpriseUsedToday + 1 else cur.surpriseUsedToday,
                extraQuickGoalId = if (isExtra && goal.category == GoalCategory.RAPIDO) null else cur.extraQuickGoalId,
                extraMediumGoalId = if (isExtra && goal.category == GoalCategory.MEDIO) null else cur.extraMediumGoalId,
                extraWeeklyGoalId = if (isExtra && goal.category == GoalCategory.LARGO) null else cur.extraWeeklyGoalId,
                notifiedGoalId = if (cur.notifiedGoalId == goal.id) null else cur.notifiedGoalId,
                biomeXpById = nextMap,
                unlockedBiomeCount = nextUnlocked,
                betaStageOverride = null,
                biomeCelebration = celebrationEvent ?: cur.biomeCelebration,
                completions = listOf(newLog) + cur.completions,
                toastMessage = msg
            )
        }
        if (!isExtra && !isFromNotification) {
            rerollAfterCompletion(goal.category, goal.id)
        }
    }

    private fun rerollAfterCompletion(cat: GoalCategory, completedGoalId: Int) {
        val st = _uiState.value
        val activeBiomeId = st.activeBiome.id
        when (cat) {
            GoalCategory.RAPIDO -> {
                val allGen = st.catalog.filter {
                    !it.isHidden && it.category == GoalCategory.RAPIDO && it.biomeExclusive == null
                }
                val pool = allGen.filter { it.id != st.currentQuickIds.first && it.id != completedGoalId }
                val next = (if (pool.isNotEmpty()) pool else allGen).randomOrNull() ?: return
                val bNext = st.catalog.firstOrNull { !it.isHidden && it.biomeExclusive == activeBiomeId && it.category == GoalCategory.RAPIDO }?.id
                    ?: st.currentQuickIds.second
                updateAndPersist { it.copy(currentQuickIds = next.id to bNext) }
            }
            GoalCategory.MEDIO -> {
                val allGen = st.catalog.filter {
                    !it.isHidden && it.category == GoalCategory.MEDIO && it.biomeExclusive == null
                }
                val pool = allGen.filter { it.id != st.currentMediumIds.first && it.id != completedGoalId }
                val next = (if (pool.isNotEmpty()) pool else allGen).randomOrNull() ?: return
                val bNext = st.catalog.firstOrNull { !it.isHidden && it.biomeExclusive == activeBiomeId && it.category == GoalCategory.MEDIO }?.id
                    ?: st.currentMediumIds.second
                updateAndPersist { it.copy(currentMediumIds = next.id to bNext) }
            }
            GoalCategory.LARGO -> {
                val shown = st.currentWeeklyIds
                val candidates = st.catalog.filter {
                    !it.isHidden && it.category == GoalCategory.LARGO && it.id !in shown && it.id != completedGoalId
                }
                val replacement = candidates.randomOrNull()
                val nextWeekly = if (completedGoalId in shown) {
                    if (replacement != null) {
                        shown.map { if (it == completedGoalId) replacement.id else it }
                    } else {
                        shown.filterNot { it == completedGoalId }
                    }
                } else if (replacement != null) {
                    (listOf(replacement.id) + shown.dropLast(1)).distinct().take(3)
                } else {
                    shown
                }
                if (nextWeekly != shown) {
                    updateAndPersist { it.copy(currentWeeklyIds = nextWeekly) }
                }
            }
        }
    }

    fun clearBiomeCelebration(eventId: Long) {
        val current = _uiState.value
        val ev = current.biomeCelebration ?: return
        if (ev.id != eventId) return
        val nextIdx = ev.nextBiomeIndex
        if (nextIdx != null && nextIdx in SplashCatalogData.BIOMES.indices) {
            val targetBiomeId = SplashCatalogData.BIOMES[nextIdx].id
            val bQuick = current.catalog.firstOrNull { !it.isHidden && it.biomeExclusive == targetBiomeId && it.category == GoalCategory.RAPIDO }?.id
                ?: current.currentQuickIds.second
            val bMed = current.catalog.firstOrNull { !it.isHidden && it.biomeExclusive == targetBiomeId && it.category == GoalCategory.MEDIO }?.id
                ?: current.currentMediumIds.second
            updateAndPersist { cur ->
                if (cur.biomeCelebration?.id != eventId) cur
                else cur.copy(
                    activeBiomeIndex = nextIdx,
                    biomeCelebration = null,
                    currentQuickIds = cur.currentQuickIds.first to bQuick,
                    currentMediumIds = cur.currentMediumIds.first to bMed
                )
            }
        } else {
            _uiState.update { cur ->
                if (cur.biomeCelebration?.id == eventId) cur.copy(biomeCelebration = null) else cur
            }
        }
    }

    fun previewBiomeCelebration(category: GoalCategory) {
        val st = _uiState.value
        val b = st.activeBiome
        val curXp = (st.biomeXpById[b.id] ?: 0).coerceIn(0, SplashCatalogData.MAX_BIOME_XP)
        val gain = when (category) {
            GoalCategory.RAPIDO -> 2
            GoalCategory.MEDIO -> 3
            GoalCategory.LARGO -> 4
        }
        val prevXp: Int
        val nextXp: Int
        if (curXp + gain <= SplashCatalogData.MAX_BIOME_XP) {
            prevXp = curXp
            nextXp = curXp + gain
            val map = st.biomeXpById.toMutableMap().apply { put(b.id, nextXp) }
            updateAndPersist { cur ->
                cur.copy(
                    biomeXpById = map,
                    betaStageOverride = null,
                    isSettingsOpen = false,
                    activeTab = AppTab.HOY,
                    biomeCelebration = BiomeCelebrationEvent(
                        id = System.currentTimeMillis(),
                        category = category,
                        earnedXp = gain,
                        isSurprise = false,
                        prevXp = prevXp,
                        nextXp = nextXp
                    )
                )
            }
        } else {
            prevXp = (curXp - gain).coerceAtLeast(0)
            nextXp = curXp
            _uiState.update { cur ->
                cur.copy(
                    betaStageOverride = null,
                    isSettingsOpen = false,
                    activeTab = AppTab.HOY,
                    biomeCelebration = BiomeCelebrationEvent(
                        id = System.currentTimeMillis(),
                        category = category,
                        earnedXp = gain,
                        isSurprise = false,
                        prevXp = prevXp,
                        nextXp = nextXp
                    )
                )
            }
        }
    }

    fun grantBetaXp(points: Int) {
        val st = _uiState.value
        val b = st.activeBiome
        val prev = st.biomeXpById[b.id] ?: 0
        val next = (prev + points).coerceIn(0, SplashCatalogData.MAX_BIOME_XP)
        val map = st.biomeXpById.toMutableMap().apply { put(b.id, next) }
        var unlocked = st.unlockedBiomeCount
        if (next >= SplashCatalogData.MAX_BIOME_XP && st.activeBiomeIndex == unlocked - 1 && unlocked < SplashCatalogData.BIOMES.size) {
            unlocked += 1
        }
        updateAndPersist { cur ->
            cur.copy(
                biomeXpById = map,
                unlockedBiomeCount = unlocked,
                betaStageOverride = null,
                toastMessage = "+$points% -> ${b.shortName(cur.lang)} ($next/100)"
            )
        }
    }

    fun setBetaStageOverride(stage: Int?) {
        _uiState.update { it.copy(betaStageOverride = stage) }
    }

    fun toggle3DExplorer(open: Boolean, targetBiomeIndex: Int? = null) {
        val st = _uiState.value
        if (targetBiomeIndex != null) {
            val maxAllowed = if (st.betaMode) SplashCatalogData.BIOMES.lastIndex else (st.unlockedBiomeCount - 1)
            val clamped = targetBiomeIndex.coerceIn(0, maxAllowed)
            updateAndPersist { it.copy(activeBiomeIndex = clamped, is3DOpen = open) }
        } else {
            if (!open && st.isAmbientAudioPlaying) {
                audioManager.stop()
                _uiState.update { it.copy(is3DOpen = false, isAmbientAudioPlaying = false) }
            } else {
                _uiState.update { it.copy(is3DOpen = open) }
            }
        }
    }

    fun toggleAmbientAudio() {
        val playing = audioManager.toggle(_uiState.value.activeBiome.id)
        _uiState.update { it.copy(isAmbientAudioPlaying = playing) }
    }

    fun toggleDioramaNightPreview() {
        _uiState.update { it.copy(dioramaNightPreview = !it.dioramaNightPreview) }
    }

    fun setSettingsOpen(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun setFeedbackOpen(open: Boolean) {
        _uiState.update { it.copy(isFeedbackOpen = open) }
    }

    fun setCreateGoalOpen(open: Boolean) {
        _uiState.update { it.copy(isCreateGoalOpen = open) }
    }

    fun setUserName(name: String) {
        val clean = name.trim().take(28)
        updateAndPersist {
            it.copy(
                userName = clean,
                toastMessage = SplashStrings.t(it.lang, "Nombre guardado", "Name saved")
            )
        }
    }

    fun setDailyNotifCount(count: Int) {
        val clamped = count.coerceIn(2, 7)
        updateAndPersist(forceRescheduleNotifs = true) {
            it.copy(dailyNotifCount = clamped)
        }
    }

    fun switchLanguage(target: AppLanguage) {
        if (_uiState.value.lang == target) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLanguageSwitching = true) }
            delay(220)
            updateAndPersist { it.copy(lang = target) }
            delay(260)
            _uiState.update { it.copy(isLanguageSwitching = false) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        updateAndPersist { it.copy(themeMode = mode) }
    }

    fun setWidgetThemePref(pref: WidgetThemePref) {
        updateAndPersist { it.copy(widgetThemePref = pref) }
    }

    fun setBetaMode(enabled: Boolean) {
        updateAndPersist {
            it.copy(
                betaMode = enabled,
                betaStageOverride = if (enabled) it.betaStageOverride else null
            )
        }
    }

    fun completeActiveBiomeBeta() {
        val st = _uiState.value
        val b = st.activeBiome
        val prevXp = (st.biomeXpById[b.id] ?: 0).coerceIn(0, SplashCatalogData.MAX_BIOME_XP)
        val map = st.biomeXpById.toMutableMap().apply { put(b.id, SplashCatalogData.MAX_BIOME_XP) }
        val nextBiomeIdx = (st.activeBiomeIndex + 1).takeIf { it < SplashCatalogData.BIOMES.size }
        val nextUnlocked = if (nextBiomeIdx != null) {
            maxOf(st.unlockedBiomeCount, nextBiomeIdx + 1).coerceAtMost(SplashCatalogData.BIOMES.size)
        } else {
            st.unlockedBiomeCount
        }
        val nextBiomeDef = nextBiomeIdx?.let { SplashCatalogData.BIOMES[it] }
        if (nextBiomeDef != null) {
            map.putIfAbsent(nextBiomeDef.id, 0)
        }
        val animPrev = if (prevXp >= SplashCatalogData.MAX_BIOME_XP) 90 else prevXp
        updateAndPersist { cur ->
            cur.copy(
                biomeXpById = map,
                unlockedBiomeCount = nextUnlocked,
                betaStageOverride = null,
                isSettingsOpen = false,
                activeTab = AppTab.HOY,
                biomeCelebration = BiomeCelebrationEvent(
                    id = System.currentTimeMillis(),
                    category = GoalCategory.LARGO,
                    earnedXp = SplashCatalogData.MAX_BIOME_XP - animPrev,
                    isSurprise = false,
                    prevXp = animPrev,
                    nextXp = SplashCatalogData.MAX_BIOME_XP,
                    biomeUnlocked = true,
                    completedBiomeId = b.id,
                    completedBiomeName = b.name(cur.lang),
                    nextBiomeId = nextBiomeDef?.id,
                    nextBiomeIndex = nextBiomeIdx,
                    nextBiomeName = nextBiomeDef?.name(cur.lang)
                ),
                toastMessage = if (nextBiomeDef != null) {
                    SplashStrings.t(cur.lang, "Bioma completado · Siguiente: ${nextBiomeDef.name(cur.lang)}", "Biome completed · Next: ${nextBiomeDef.name(cur.lang)}")
                } else {
                    SplashStrings.t(cur.lang, "Bioma completado (100/100)", "Biome completed (100/100)")
                }
            )
        }
    }

    fun resetAllProgress() {
        val today = LocalDate.now().toString()
        updateAndPersist { cur ->
            cur.copy(
                activeBiomeIndex = 0,
                unlockedBiomeCount = 1,
                biomeXpById = mapOf("valle" to 0),
                betaStageOverride = null,
                quickUsedToday = 0,
                mediumUsedToday = 0,
                weeklyUsedThisWeek = 0,
                surpriseUsedToday = 0,
                extraQuickGoalId = null,
                extraMediumGoalId = null,
                extraWeeklyGoalId = null,
                biomeCelebration = null,
                completions = emptyList(),
                selectedDayIso = today,
                toastMessage = SplashStrings.t(cur.lang, "Progreso reiniciado desde 0", "Progress reset from 0")
            )
        }
    }

    fun updateCatalogSearch(q: String) {
        _uiState.update { it.copy(catalogSearch = q) }
    }

    fun updateCatalogFilter(f: CatalogFilter) {
        _uiState.update { it.copy(catalogFilter = f) }
    }

    fun createCustomGoal(title: String, description: String, category: GoalCategory, areaEs: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val cleanDesc = description.trim().ifEmpty { cleanTitle }
        val nextId = (_uiState.value.catalog.maxOfOrNull { it.id } ?: 200) + 1
        val item = GoalItem(
            id = nextId,
            titleShortEs = cleanTitle.take(36),
            titleEs = cleanDesc,
            subEs = areaEs,
            titleShortEn = cleanTitle.take(36),
            titleEn = cleanDesc,
            subEn = SplashStrings.areaFor(AppLanguage.EN, areaEs),
            category = category,
            iconName = "eco",
            verification = if (category == GoalCategory.LARGO) VerificationType.PUENTE_ANALOGICO else VerificationType.TOQUE,
            isFavorite = true,
            isCustom = true
        )
        updateAndPersist { cur ->
            cur.copy(
                catalog = listOf(item) + cur.catalog,
                isCreateGoalOpen = false,
                toastMessage = SplashStrings.t(
                    cur.lang,
                    "Objetivo guardado · ya entra en Hoy y notificaciones",
                    "Goal saved · now in Today & notifications"
                )
            )
        }
    }

    fun toggleFavoriteGoal(goalId: Int) {
        updateAndPersist { cur ->
            cur.copy(
                catalog = cur.catalog.map { if (it.id == goalId) it.copy(isFavorite = !it.isFavorite) else it }
            )
        }
    }

    fun toggleDayHour(dayKey: String, hour: Int) {
        val curSched = _uiState.value.notifSchedule.toMutableMap()
        val curDay = curSched[dayKey] ?: DaySchedule(true, setOf(hour))
        val nextHours = curDay.hours.toMutableSet().apply {
            if (contains(hour)) remove(hour) else add(hour)
        }
        curSched[dayKey] = curDay.copy(enabled = nextHours.isNotEmpty(), hours = nextHours)
        updateAndPersist(forceRescheduleNotifs = true) { it.copy(notifSchedule = curSched) }
    }

    fun applySchedulePreset(preset: String) {
        val workHours = (9..21).toSet()
        val monHours = (9..13).toSet()
        val next = when (preset) {
            "weekdays" -> mapOf(
                "mon" to DaySchedule(true, workHours),
                "tue" to DaySchedule(true, workHours),
                "wed" to DaySchedule(true, workHours),
                "thu" to DaySchedule(true, workHours),
                "fri" to DaySchedule(true, workHours),
                "sat" to DaySchedule(false, workHours),
                "sun" to DaySchedule(false, workHours)
            )
            "all" -> listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
                .associateWith { DaySchedule(true, workHours) }
            "mute" -> listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
                .associateWith { DaySchedule(false, emptySet()) }
            else -> mapOf(
                "mon" to DaySchedule(true, monHours),
                "tue" to DaySchedule(true, workHours),
                "wed" to DaySchedule(true, workHours),
                "thu" to DaySchedule(true, workHours),
                "fri" to DaySchedule(true, workHours),
                "sat" to DaySchedule(false, workHours),
                "sun" to DaySchedule(false, workHours)
            )
        }
        updateAndPersist(forceRescheduleNotifs = true) { it.copy(notifSchedule = next) }
    }

    fun selectCalendarMonth(delta: Int) {
        _uiState.update { it.copy(selectedMonthOffset = it.selectedMonthOffset + delta) }
    }

    fun selectDayIso(iso: String) {
        _uiState.update { it.copy(selectedDayIso = iso) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    private fun triggerHaptic() {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val v = app.getSystemService(Vibrator::class.java)
                v?.vibrate(VibrationEffect.createOneShot(24L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Throwable) {
        }
    }

    override fun onCleared() {
        notifPrefs.unregisterOnSharedPreferenceChangeListener(notifPrefsListener)
        audioManager.stop()
        super.onCleared()
    }
}
