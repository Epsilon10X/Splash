package com.splash.offline.viewmodel

import android.app.Application
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
import com.splash.offline.data.BiomeLayoutMode
import com.splash.offline.data.CatalogFilter
import com.splash.offline.data.CompletionLog
import com.splash.offline.data.DaySchedule
import com.splash.offline.data.GoalCategory
import com.splash.offline.data.GoalItem
import com.splash.offline.data.MapStyle
import com.splash.offline.data.PendingCameraGoal
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

    init {
        ensureValidDailyPicks()
        repo.syncNativeIntegrations(_uiState.value)
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
                if (st.mediumUsedToday >= 2 && st.extraMediumGoalId != null) {
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
                val pool = st.catalog.filter { !it.isHidden && it.category == GoalCategory.LARGO }
                val next3 = pool.shuffled().take(3).map { it.id }
                if (next3.size == 3) {
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
            }
            TimeBucket.MEDIUM_15M -> {
                val allMed = st.catalog.filter { !it.isHidden && it.category == GoalCategory.MEDIO }
                val pool = allMed.filter { it.id != st.extraMediumGoalId }
                val picked = (if (pool.isNotEmpty()) pool else allMed).randomOrNull() ?: return
                _uiState.update { it.copy(extraMediumGoalId = picked.id) }
            }
            TimeBucket.WEEKLY -> Unit
        }
    }

    fun dismissExtraGoal(bucket: TimeBucket) {
        _uiState.update {
            when (bucket) {
                TimeBucket.QUICK_2M -> it.copy(extraQuickGoalId = null)
                TimeBucket.MEDIUM_15M -> it.copy(extraMediumGoalId = null)
                TimeBucket.WEEKLY -> it
            }
        }
    }

    private fun computeRemainingSec(s: ActiveSession): Int {
        val endMs = s.runningEndElapsedMs ?: return s.remainingSeconds
        val diffMs = endMs - SystemClock.elapsedRealtime()
        if (diffMs <= 0L) return 0
        return ((diffMs + 999L) / 1000L).toInt().coerceIn(0, s.totalSeconds)
    }

    fun startGoal(goal: GoalItem, isBiomeMission: Boolean = false, isExtra: Boolean = false) {
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
                            isFromWidget = false
                        )
                    )
                }
            }
            VerificationType.SOCIAL, VerificationType.TOQUE -> {
                completeGoalNow(goal, isBiomeMission = isBiomeMission, isExtra = isExtra)
            }
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
        if (_uiState.value.activeSession?.isRunning == true) {
            tickActiveTimer()
        }
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
        val targetGoal = pending.goal
        if (pending.isStandaloneMemory || targetGoal == null) {
            appendStandalonePhotoMemory(savedPath)
        } else {
            completeGoalNow(
                goal = targetGoal,
                isBiomeMission = pending.isBiomeMission,
                isExtra = pending.isExtra,
                photoPath = savedPath
            )
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
                    savedTimerRemainingSec = nextTimers
                )
            }
            completeGoalNow(
                goal = session.goal,
                isBiomeMission = session.isBiomeMission,
                isExtra = session.isExtra
            )
        }
    }

    fun completeGoalNow(
        goal: GoalItem,
        isBiomeMission: Boolean = false,
        isExtra: Boolean = false,
        photoPath: String? = null
    ) {
        triggerHaptic()
        val st = _uiState.value
        val today = LocalDate.now().toString()
        val weekIso = repo.currentWeekIso()
        val prevWeekly = if (st.todayWeekIso == weekIso) st.weeklyUsedThisWeek else 0
        val xpGain = when {
            isExtra -> 0
            goal.category == GoalCategory.RAPIDO && st.quickUsedToday >= 2 -> 0
            goal.category == GoalCategory.MEDIO && st.mediumUsedToday >= 2 -> 0
            goal.category == GoalCategory.LARGO && prevWeekly >= 2 -> 0
            goal.category == GoalCategory.RAPIDO -> 1
            goal.category == GoalCategory.MEDIO -> 4
            else -> 8
        }

        val activeBiome = st.activeBiome
        val prevXp = st.biomeXpById[activeBiome.id] ?: 0
        val nextXp = (prevXp + xpGain).coerceAtMost(80)
        val nextMap = st.biomeXpById.toMutableMap().apply { put(activeBiome.id, nextXp) }

        var nextUnlocked = st.unlockedBiomeCount
        var nextActiveIdx = st.activeBiomeIndex
        var unlockedBanner = false
        if (nextXp >= 80 && st.activeBiomeIndex == st.unlockedBiomeCount - 1 && st.unlockedBiomeCount < SplashCatalogData.BIOMES.size) {
            nextUnlocked = (st.unlockedBiomeCount + 1).coerceAtMost(SplashCatalogData.BIOMES.size)
            nextActiveIdx = nextUnlocked - 1
            nextMap.putIfAbsent(SplashCatalogData.BIOMES[nextActiveIdx].id, 0)
            unlockedBanner = true
        }

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
            earnedXp = xpGain
        )

        val msg = if (unlockedBanner) {
            val newBiomeName = SplashCatalogData.BIOMES[nextActiveIdx].name(st.lang)
            SplashStrings.t(st.lang, "Bioma completado. Desbloqueaste: $newBiomeName", "Biome completed. Unlocked: $newBiomeName")
        } else if (xpGain > 0) {
            SplashStrings.t(st.lang, "Hecho (+$xpGain pt bioma). Ya puedes bloquear el móvil.", "Done (+$xpGain biome pt). You can lock your phone now.")
        } else {
            SplashStrings.t(st.lang, "Objetivo extra completado.", "Extra goal completed.")
        }

        updateAndPersist { cur ->
            val baseWeekly = if (cur.todayWeekIso == weekIso) cur.weeklyUsedThisWeek else 0
            cur.copy(
                todayDateIso = today,
                todayWeekIso = weekIso,
                quickUsedToday = if (goal.category == GoalCategory.RAPIDO && !isExtra) cur.quickUsedToday + 1 else cur.quickUsedToday,
                mediumUsedToday = if (goal.category == GoalCategory.MEDIO && !isExtra) cur.mediumUsedToday + 1 else cur.mediumUsedToday,
                weeklyUsedThisWeek = if (goal.category == GoalCategory.LARGO && !isExtra) baseWeekly + 1 else baseWeekly,
                extraQuickGoalId = if (isExtra && goal.category == GoalCategory.RAPIDO) null else cur.extraQuickGoalId,
                extraMediumGoalId = if (isExtra && goal.category == GoalCategory.MEDIO) null else cur.extraMediumGoalId,
                biomeXpById = nextMap,
                unlockedBiomeCount = nextUnlocked,
                activeBiomeIndex = nextActiveIdx,
                betaStageOverride = null,
                completions = listOf(newLog) + cur.completions,
                toastMessage = msg
            )
        }
        rerollAfterCompletion(goal.category)
    }

    private fun rerollAfterCompletion(cat: GoalCategory) {
        val st = _uiState.value
        val activeBiomeId = st.activeBiome.id
        if (cat == GoalCategory.RAPIDO) {
            val pool = st.catalog.filter { !it.isHidden && it.category == GoalCategory.RAPIDO && it.biomeExclusive == null && it.id != st.currentQuickIds.first }
            val next = pool.randomOrNull() ?: return
            val bNext = st.catalog.firstOrNull { !it.isHidden && it.biomeExclusive == activeBiomeId && it.category == GoalCategory.RAPIDO }?.id ?: st.currentQuickIds.second
            updateAndPersist { it.copy(currentQuickIds = next.id to bNext) }
        } else if (cat == GoalCategory.MEDIO) {
            val pool = st.catalog.filter { !it.isHidden && it.category == GoalCategory.MEDIO && it.biomeExclusive == null && it.id != st.currentMediumIds.first }
            val next = pool.randomOrNull() ?: return
            val bNext = st.catalog.firstOrNull { !it.isHidden && it.biomeExclusive == activeBiomeId && it.category == GoalCategory.MEDIO }?.id ?: st.currentMediumIds.second
            updateAndPersist { it.copy(currentMediumIds = next.id to bNext) }
        }
    }

    fun grantBetaXp(points: Int) {
        val st = _uiState.value
        val b = st.activeBiome
        val prev = st.biomeXpById[b.id] ?: 0
        val next = (prev + points).coerceIn(0, 80)
        val map = st.biomeXpById.toMutableMap().apply { put(b.id, next) }
        var unlocked = st.unlockedBiomeCount
        if (next >= 80 && st.activeBiomeIndex == unlocked - 1 && unlocked < SplashCatalogData.BIOMES.size) {
            unlocked += 1
        }
        updateAndPersist { cur ->
            cur.copy(
                biomeXpById = map,
                unlockedBiomeCount = unlocked,
                betaStageOverride = null,
                toastMessage = "+$points XP -> ${b.shortName(cur.lang)} ($next/80)"
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
        val clean = name.trim().take(28).ifEmpty { "Explorador" }
        updateAndPersist {
            it.copy(
                userName = clean,
                toastMessage = SplashStrings.t(it.lang, "Nombre guardado", "Name saved")
            )
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
        val map = st.biomeXpById.toMutableMap().apply { put(b.id, 80) }
        val nextUnlocked = (st.unlockedBiomeCount + 1).coerceAtMost(SplashCatalogData.BIOMES.size)
        updateAndPersist { cur ->
            cur.copy(
                biomeXpById = map,
                unlockedBiomeCount = nextUnlocked,
                activeBiomeIndex = (nextUnlocked - 1).coerceAtMost(SplashCatalogData.BIOMES.lastIndex),
                betaStageOverride = null,
                toastMessage = SplashStrings.t(cur.lang, "Bioma completado (80/80)", "Biome completed (80/80)")
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
                extraQuickGoalId = null,
                extraMediumGoalId = null,
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

    fun createCustomGoal(title: String, category: GoalCategory, areaEs: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val nextId = (_uiState.value.catalog.maxOfOrNull { it.id } ?: 200) + 1
        val item = GoalItem(
            id = nextId,
            titleShortEs = clean.take(26),
            titleEs = clean,
            subEs = areaEs,
            titleShortEn = clean.take(26),
            titleEn = clean,
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
                toastMessage = SplashStrings.t(cur.lang, "Objetivo guardado en Catálogo", "Goal saved in Catalog")
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
        audioManager.stop()
        super.onCleared()
    }
}
