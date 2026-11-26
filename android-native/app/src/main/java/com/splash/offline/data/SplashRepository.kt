package com.splash.offline.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import com.splash.offline.SplashBiomeWidgetProvider
import com.splash.offline.SplashNotificationScheduler
import com.splash.offline.SplashQuickWidgetProvider
import com.splash.offline.SplashWidgetProvider
import com.splash.offline.ui.components.renderBiomeDioramaBitmap
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.temporal.WeekFields
import org.json.JSONArray
import org.json.JSONObject

class SplashRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("splash_native_v2", Context.MODE_PRIVATE)

    fun currentWeekIso(): String {
        val today = LocalDate.now()
        val wf = WeekFields.ISO
        val weekYear = today.get(wf.weekBasedYear())
        val weekNum = today.get(wf.weekOfWeekBasedYear())
        return String.format("%d-W%02d", weekYear, weekNum)
    }

    fun loadCatalog(): List<GoalItem> {
        val result = mutableListOf<GoalItem>()
        try {
            val raw = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val esObj = o.optJSONObject("es")
                val enObj = o.optJSONObject("en")
                val cat = when (o.optString("categoria", "RAPIDO")) {
                    "MEDIO" -> GoalCategory.MEDIO
                    "LARGO" -> GoalCategory.LARGO
                    else -> GoalCategory.RAPIDO
                }
                val ver = when (o.optString("tipoVerificacion", "TOQUE")) {
                    "TEMPORIZADOR" -> VerificationType.TEMPORIZADOR
                    "SOCIAL" -> VerificationType.SOCIAL
                    "PUENTE_ANALOGICO" -> VerificationType.PUENTE_ANALOGICO
                    else -> VerificationType.TOQUE
                }
                result.add(
                    GoalItem(
                        id = o.getInt("id"),
                        titleShortEs = esObj?.optString("tituloCorto").orEmpty().ifBlank { o.optString("tituloCorto") },
                        titleEs = esObj?.optString("titulo").orEmpty().ifBlank { o.optString("titulo") },
                        subEs = esObj?.optString("subcategoria").orEmpty().ifBlank { o.optString("subcategoria") },
                        titleShortEn = enObj?.optString("tituloCorto").orEmpty().ifBlank { o.optString("tituloCorto") },
                        titleEn = enObj?.optString("titulo").orEmpty().ifBlank { o.optString("titulo") },
                        subEn = enObj?.optString("subcategoria").orEmpty().ifBlank { o.optString("subcategoria") },
                        category = cat,
                        iconName = o.optString("icono", "eco"),
                        verification = ver,
                        biomeExclusive = o.optString("biomaExclusivo", "").takeIf { it.isNotBlank() },
                        isBiomeSecret = o.optBoolean("esSecretoBioma", false),
                        isFavorite = o.optBoolean("esFavorito", false),
                        isHidden = o.optBoolean("estaOculto", false)
                    )
                )
            }
        } catch (_: Throwable) {
        }
        return result
    }

    fun defaultSchedule(): Map<String, DaySchedule> {
        val defaultHours = (9..21).toSet()
        val monHours = (9..13).toSet()
        return mapOf(
            "mon" to DaySchedule(true, monHours),
            "tue" to DaySchedule(true, defaultHours),
            "wed" to DaySchedule(true, defaultHours),
            "thu" to DaySchedule(true, defaultHours),
            "fri" to DaySchedule(true, defaultHours),
            "sat" to DaySchedule(false, defaultHours),
            "sun" to DaySchedule(false, defaultHours)
        )
    }

    fun loadState(baseCatalog: List<GoalItem>): SplashUiState {
        val todayIso = LocalDate.now().toString()
        val raw = prefs.getString("state_json", null)
        if (raw.isNullOrBlank()) {
            return SplashUiState(
                todayDateIso = todayIso,
                selectedDayIso = todayIso,
                catalog = baseCatalog,
                notifSchedule = defaultSchedule()
            )
        }
        return try {
            val o = JSONObject(raw)
            val lang = if (o.optString("lang") == "en") AppLanguage.EN else AppLanguage.ES
            val themeMode = when (o.optString("themeMode")) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            val widgetTheme = when (o.optString("widgetTheme")) {
                "light" -> WidgetThemePref.LIGHT
                "dark" -> WidgetThemePref.DARK
                else -> WidgetThemePref.AUTO
            }
            val savedDate = o.optString("todayDateIso", todayIso)
            val sameDay = savedDate == todayIso

            val xpMap = mutableMapOf("valle" to 0)
            o.optJSONObject("biomeXp")?.let { xpObj ->
                xpObj.keys().forEach { k -> xpMap[k] = xpObj.optInt(k, 0) }
            }

            val favIds = mutableSetOf<Int>()
            val hiddenIds = mutableSetOf<Int>()
            o.optJSONArray("favIds")?.let { arr -> for (i in 0 until arr.length()) favIds.add(arr.getInt(i)) }
            o.optJSONArray("hiddenIds")?.let { arr -> for (i in 0 until arr.length()) hiddenIds.add(arr.getInt(i)) }

            val customGoals = mutableListOf<GoalItem>()
            o.optJSONArray("customGoals")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val cg = arr.getJSONObject(i)
                    val cat = when (cg.optString("category")) {
                        "MEDIO" -> GoalCategory.MEDIO
                        "LARGO" -> GoalCategory.LARGO
                        else -> GoalCategory.RAPIDO
                    }
                    val cgTitleEs = cg.optString("titleEs")
                    val cgTitleEn = cg.optString("titleEn", cgTitleEs)
                    val cgShortEs = cg.optString("titleShortEs").ifBlank { cgTitleEs.take(36) }
                    val cgShortEn = cg.optString("titleShortEn").ifBlank { cgTitleEn.take(36) }
                    customGoals.add(
                        GoalItem(
                            id = cg.getInt("id"),
                            titleShortEs = cgShortEs,
                            titleEs = cgTitleEs,
                            subEs = cg.optString("subEs", "Personal"),
                            titleShortEn = cgShortEn,
                            titleEn = cgTitleEn,
                            subEn = cg.optString("subEn", "Custom"),
                            category = cat,
                            iconName = cg.optString("iconName", "eco"),
                            verification = if (cat == GoalCategory.LARGO) VerificationType.PUENTE_ANALOGICO else VerificationType.TOQUE,
                            isFavorite = cg.optBoolean("isFavorite", false),
                            isCustom = true
                        )
                    )
                }
            }

            val mergedCatalog = (baseCatalog.map { g ->
                g.copy(
                    isFavorite = favIds.contains(g.id) || (g.isFavorite && !hiddenIds.contains(-g.id)),
                    isHidden = hiddenIds.contains(g.id)
                )
            } + customGoals)

            val completions = mutableListOf<CompletionLog>()
            o.optJSONArray("completions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    val cat = when (c.optString("category")) {
                        "MEDIO" -> GoalCategory.MEDIO
                        "LARGO" -> GoalCategory.LARGO
                        else -> GoalCategory.RAPIDO
                    }
                    completions.add(
                        CompletionLog(
                            id = c.getLong("id"),
                            dateIso = c.getString("dateIso"),
                            timestampMs = c.optLong("timestampMs", 0L),
                            goalId = c.getInt("goalId"),
                            titleEs = c.optString("titleEs"),
                            titleEn = c.optString("titleEn", c.optString("titleEs")),
                            category = cat,
                            biomeId = c.optString("biomeId", "valle"),
                            photoPath = c.optString("photoPath", "").takeIf { it.isNotBlank() },
                            earnedXp = c.optInt("earnedXp", 0),
                            note = c.optString("note", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }

            val sched = defaultSchedule().toMutableMap()
            o.optJSONObject("schedule")?.let { sObj ->
                sObj.keys().forEach { dayKey ->
                    val dObj = sObj.optJSONObject(dayKey) ?: return@forEach
                    val hrs = mutableSetOf<Int>()
                    dObj.optJSONArray("hours")?.let { arr ->
                        for (j in 0 until arr.length()) hrs.add(arr.getInt(j))
                    }
                    sched[dayKey] = DaySchedule(dObj.optBoolean("enabled", false), hrs)
                }
            }

            val weekIso = currentWeekIso()
            val savedWeek = o.optString("todayWeekIso", weekIso)
            val sameWeek = savedWeek == weekIso

            val activeBiomeIdx = o.optInt("activeBiomeIndex", 0).coerceIn(0, SplashCatalogData.BIOMES.lastIndex)
            val activeBiomeId = SplashCatalogData.BIOMES[activeBiomeIdx].id
            val quickIds = if (sameDay) {
                o.optInt("quickGenId", 1) to o.optInt("quickBiomeId", 101)
            } else {
                val qGen = mergedCatalog.filter { !it.isHidden && it.category == GoalCategory.RAPIDO && it.biomeExclusive == null }
                    .randomOrNull()?.id ?: 1
                val qBio = mergedCatalog.firstOrNull { !it.isHidden && it.biomeExclusive == activeBiomeId && it.category == GoalCategory.RAPIDO }?.id ?: 101
                qGen to qBio
            }
            val medIds = if (sameDay) {
                o.optInt("medGenId", 21) to o.optInt("medBiomeId", 102)
            } else {
                val mGen = mergedCatalog.filter { !it.isHidden && it.category == GoalCategory.MEDIO && it.biomeExclusive == null }
                    .randomOrNull()?.id ?: 21
                val mBio = mergedCatalog.firstOrNull { !it.isHidden && it.biomeExclusive == activeBiomeId && it.category == GoalCategory.MEDIO }?.id ?: 102
                mGen to mBio
            }
            val weeklyArr = o.optJSONArray("weeklyIds")
            val weeklyIds = if (sameWeek && weeklyArr != null && weeklyArr.length() in 1..3) {
                buildList { for (i in 0 until weeklyArr.length()) add(weeklyArr.getInt(i)) }
            } else {
                mergedCatalog.filter { !it.isHidden && it.category == GoalCategory.LARGO }
                    .shuffled()
                    .take(3)
                    .map { it.id }
                    .ifEmpty { listOf(38, 39, 40) }
            }
            val savedTimers = mutableMapOf<String, Int>()
            o.optJSONObject("savedTimers")?.let { tObj ->
                tObj.keys().forEach { k ->
                    val rem = tObj.optInt(k, 0)
                    if (rem > 0) savedTimers[k] = rem
                }
            }

            SplashUiState(
                lang = lang,
                themeMode = themeMode,
                widgetThemePref = widgetTheme,
                userName = o.optString("userName", ""),
                activeBiomeIndex = activeBiomeIdx,
                unlockedBiomeCount = o.optInt("unlockedBiomeCount", 1).coerceIn(1, SplashCatalogData.BIOMES.size),
                biomeXpById = xpMap,
                betaMode = o.optBoolean("betaMode", false),
                todayDateIso = todayIso,
                todayWeekIso = weekIso,
                quickUsedToday = if (sameDay) o.optInt("quickUsedToday", 0) else 0,
                mediumUsedToday = if (sameDay) o.optInt("mediumUsedToday", 0) else 0,
                weeklyUsedThisWeek = if (sameWeek) o.optInt("weeklyUsedThisWeek", 0) else 0,
                surpriseUsedToday = if (sameDay) o.optInt("surpriseUsedToday", 0) else 0,
                currentQuickIds = quickIds,
                currentMediumIds = medIds,
                currentWeeklyIds = weeklyIds,
                dailyNotifCount = o.optInt("dailyNotifCount", 4).coerceIn(2, 7),
                savedTimerRemainingSec = savedTimers,
                catalog = mergedCatalog,
                completions = completions,
                selectedDayIso = todayIso,
                notifSchedule = sched
            )
        } catch (_: Throwable) {
            SplashUiState(
                todayDateIso = todayIso,
                todayWeekIso = currentWeekIso(),
                selectedDayIso = todayIso,
                catalog = baseCatalog,
                notifSchedule = defaultSchedule()
            )
        }
    }

    fun saveState(state: SplashUiState, forceRescheduleNotifs: Boolean = false) {
        val o = JSONObject()
        o.put("lang", state.lang.code)
        o.put("themeMode", when (state.themeMode) {
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
            ThemeMode.SYSTEM -> "system"
        })
        o.put("widgetTheme", state.widgetThemePref.wire)
        o.put("userName", state.userName)
        o.put("dailyNotifCount", state.dailyNotifCount.coerceIn(2, 7))
        o.put("activeBiomeIndex", state.activeBiomeIndex)
        o.put("unlockedBiomeCount", state.unlockedBiomeCount)
        o.put("betaMode", state.betaMode)
        o.put("todayDateIso", state.todayDateIso)
        o.put("todayWeekIso", state.todayWeekIso.ifBlank { currentWeekIso() })
        o.put("quickUsedToday", state.quickUsedToday)
        o.put("mediumUsedToday", state.mediumUsedToday)
        o.put("weeklyUsedThisWeek", state.weeklyUsedThisWeek)
        o.put("surpriseUsedToday", state.surpriseUsedToday)
        o.put("quickGenId", state.currentQuickIds.first)
        o.put("quickBiomeId", state.currentQuickIds.second)
        o.put("medGenId", state.currentMediumIds.first)
        o.put("medBiomeId", state.currentMediumIds.second)
        o.put("weeklyIds", JSONArray(state.currentWeeklyIds))

        val timersObj = JSONObject()
        state.savedTimerRemainingSec.forEach { (k, v) -> if (v > 0) timersObj.put(k, v) }
        o.put("savedTimers", timersObj)

        val xpObj = JSONObject()
        state.biomeXpById.forEach { (k, v) -> xpObj.put(k, v) }
        o.put("biomeXp", xpObj)

        val favArr = JSONArray()
        val hidArr = JSONArray()
        val customArr = JSONArray()
        state.catalog.forEach { g ->
            if (g.isCustom) {
                customArr.put(
                    JSONObject()
                        .put("id", g.id)
                        .put("titleShortEs", g.titleShortEs)
                        .put("titleEs", g.titleEs)
                        .put("titleShortEn", g.titleShortEn)
                        .put("titleEn", g.titleEn)
                        .put("subEs", g.subEs)
                        .put("subEn", g.subEn)
                        .put("category", g.category.name)
                        .put("iconName", g.iconName)
                        .put("isFavorite", g.isFavorite)
                )
            } else {
                if (g.isFavorite) favArr.put(g.id)
                if (g.isHidden) hidArr.put(g.id)
            }
        }
        o.put("favIds", favArr)
        o.put("hiddenIds", hidArr)
        o.put("customGoals", customArr)

        val photoLogs = state.completions.filter { !it.photoPath.isNullOrBlank() }
        val toSave = (state.completions.take(160) + photoLogs).distinctBy { it.id }
        val compArr = JSONArray()
        toSave.forEach { c ->
            compArr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("dateIso", c.dateIso)
                    .put("timestampMs", c.timestampMs)
                    .put("goalId", c.goalId)
                    .put("titleEs", c.titleEs)
                    .put("titleEn", c.titleEn)
                    .put("category", c.category.name)
                    .put("biomeId", c.biomeId)
                    .put("photoPath", c.photoPath ?: "")
                    .put("earnedXp", c.earnedXp)
                    .put("note", c.note ?: "")
            )
        }
        o.put("completions", compArr)

        val schedObj = JSONObject()
        state.notifSchedule.forEach { (day, ds) ->
            schedObj.put(
                day,
                JSONObject()
                    .put("enabled", ds.enabled)
                    .put("hours", JSONArray(ds.hours.sorted()))
            )
        }
        o.put("schedule", schedObj)

        prefs.edit().putString("state_json", o.toString()).apply()
        syncNativeIntegrations(state, forceRescheduleNotifs)
    }

    fun syncNativeIntegrations(state: SplashUiState, forceRescheduleNotifs: Boolean = false) {
        val isEn = state.lang == AppLanguage.EN
        val biome = state.activeBiome
        val levelLabel = (if (isEn) "Lvl. " else "Nv. ") + state.effectiveBiomeLevel
        val streakLabel = SplashStrings.streakLabel(state.lang, state.streakDays)
        val todayIso = state.todayDateIso.ifBlank { LocalDate.now().toString() }

        val quickBiomeGoal = state.catalog.firstOrNull { it.id == state.currentQuickIds.second }
        val quickGenGoal = state.catalog.firstOrNull { it.id == state.currentQuickIds.first }
        val medGoal = state.catalog.firstOrNull { it.id == state.currentMediumIds.first }

        val flashTodayLogs = state.completions.filter {
            it.dateIso == todayIso && it.category == GoalCategory.RAPIDO
        }
        val flashUsed = state.quickUsedToday.coerceIn(0, 2)

        fun logDisplayTitle(log: CompletionLog): String {
            val found = state.catalog.firstOrNull { it.id == log.goalId }
            return found?.displayShort(state.lang) ?: if (isEn) log.titleEn else log.titleEs
        }

        val doneSub = if (isEn) "Completed · Flash" else "Completado · Flash"
        val flash1Title: String
        val flash1Sub: String
        val flash1Done: Boolean
        val flash2Title: String
        val flash2Sub: String
        val flash2Done: Boolean

        if (flashUsed >= 2) {
            val extraGoal = state.extraQuickGoalId?.let { eid -> state.catalog.firstOrNull { it.id == eid } }
            val firstDone = flashTodayLogs.getOrNull(1)?.let { logDisplayTitle(it) }
                ?: flashTodayLogs.firstOrNull()?.let { logDisplayTitle(it) }
                ?: (if (isEn) "Flash goal 1" else "Flash 1 completado")
            val secondDone = flashTodayLogs.firstOrNull()?.let { logDisplayTitle(it) }
                ?: (if (isEn) "Flash goal 2" else "Flash 2 completado")
            flash1Title = if (extraGoal != null) secondDone else firstDone
            flash1Sub = "$doneSub (2/2)"
            flash1Done = true
            if (extraGoal != null) {
                flash2Title = extraGoal.displayShort(state.lang)
                flash2Sub = if (isEn) "Extra Flash · 2 min" else "Flash extra · 2 min"
                flash2Done = false
            } else {
                flash2Title = secondDone
                flash2Sub = "$doneSub (2/2)"
                flash2Done = true
            }
        } else if (flashUsed == 1) {
            val doneLog = flashTodayLogs.firstOrNull()
            flash1Title = doneLog?.let { logDisplayTitle(it) }
                ?: (if (isEn) "Flash goal (1/2)" else "Flash completado (1/2)")
            flash1Sub = "$doneSub (1/2)"
            flash1Done = true

            val completedWasBiome = doneLog != null && doneLog.goalId == quickBiomeGoal?.id
            val nextPending = if (completedWasBiome) {
                quickGenGoal ?: quickBiomeGoal
            } else {
                quickBiomeGoal ?: quickGenGoal
            }
            flash2Title = nextPending?.displayShort(state.lang) ?: "Splash"
            flash2Sub = if (nextPending?.biomeExclusive != null) {
                "Flash · ${biome.shortName(state.lang)}"
            } else {
                "Flash · 2 min"
            }
            flash2Done = false
        } else {
            val firstPending = quickBiomeGoal ?: quickGenGoal
            val secondPending = quickGenGoal ?: quickBiomeGoal
            flash1Title = firstPending?.displayShort(state.lang) ?: "Splash"
            flash1Sub = if (firstPending?.biomeExclusive != null) {
                "Flash · ${biome.shortName(state.lang)}"
            } else {
                "Flash · 2 min"
            }
            flash1Done = false
            flash2Title = secondPending?.displayShort(state.lang) ?: "Splash"
            flash2Sub = "Flash · 2 min"
            flash2Done = false
        }

        val flashStatusLabel = if (flashUsed >= 2) {
            if (isEn) "Flash 2/2 · Done" else "Flash 2/2 · Completado"
        } else {
            "Flash $flashUsed/2"
        }

        renderWidgetBiomeBitmap(biome, state.effectiveBiomeLevel, state.activeBiomeXp)

        context.getSharedPreferences("splash_widget", Context.MODE_PRIVATE).edit()
            .putString("biomeName", biome.name(state.lang))
            .putString("biomeShort", biome.shortName(state.lang))
            .putString("levelLabel", levelLabel)
            .putString("streakLabel", streakLabel)
            .putString("weatherLabel", if (isEn) "Serene" else "En calma")
            .putString("goal2Title", flash1Title)
            .putString("goal15Title", medGoal?.displayShort(state.lang) ?: "Paseo")
            .putString("flash1Title", flash1Title)
            .putString("flash1Sub", flash1Sub)
            .putBoolean("flash1Done", flash1Done)
            .putString("flash2Title", flash2Title)
            .putString("flash2Sub", flash2Sub)
            .putBoolean("flash2Done", flash2Done)
            .putString("flashStatusLabel", flashStatusLabel)
            .putString("themeMode", state.widgetThemePref.wire)
            .putBoolean("isEn", isEn)
            .commit()

        try {
            val awm = AppWidgetManager.getInstance(context)
            val idsMain = awm.getAppWidgetIds(ComponentName(context, SplashWidgetProvider::class.java))
            if (idsMain.isNotEmpty()) SplashWidgetProvider.updateAllWidgets(context, awm, idsMain)
            val idsBiome = awm.getAppWidgetIds(ComponentName(context, SplashBiomeWidgetProvider::class.java))
            if (idsBiome.isNotEmpty()) SplashBiomeWidgetProvider.updateAllWidgets(context, awm, idsBiome)
            val idsQuick = awm.getAppWidgetIds(ComponentName(context, SplashQuickWidgetProvider::class.java))
            if (idsQuick.isNotEmpty()) SplashQuickWidgetProvider.updateAllWidgets(context, awm, idsQuick)
        } catch (_: Throwable) {
        }

        val quickPool = JSONArray()
        val medioPool = JSONArray()
        state.catalog.filter { !it.isHidden }.forEach { g ->
            val itemJson = JSONObject()
                .put("id", g.id)
                .put("shortTitle", g.displayShort(state.lang))
                .put("body", g.displayTitle(state.lang))
            when (g.category) {
                GoalCategory.RAPIDO -> quickPool.put(itemJson)
                GoalCategory.MEDIO -> medioPool.put(itemJson)
                GoalCategory.LARGO -> Unit
            }
        }
        val notifPayload = JSONObject()
            .put("lang", state.lang.code)
            .put("dailyCount", state.dailyNotifCount.coerceIn(2, 7))
            .put("quickBadge", "Splash · Flash")
            .put("medioBadge", if (isEn) "Splash · Focus" else "Splash · Enfoque")
            .put("quickPool", quickPool)
            .put("medioPool", medioPool)

        val schedJson = JSONObject()
        state.notifSchedule.forEach { (k, v) ->
            schedJson.put(
                k,
                JSONObject().put("enabled", v.enabled).put("hours", JSONArray(v.hours.sorted()))
            )
        }
        notifPayload.put("schedule", schedJson)

        SplashNotificationScheduler.savePayload(context, notifPayload.toString())
        SplashNotificationScheduler.scheduleNext(context, forceRescheduleNotifs)
    }

    private fun renderWidgetBiomeBitmap(biome: BiomeDef, stage: Int, xp: Int) {
        try {
            val bmp = renderBiomeDioramaBitmap(
                biomeId = biome.id,
                stage = stage,
                points = xp,
                widthPx = 320,
                heightPx = 320,
                zoom = 1.02f,
                nightMode = false
            ).asAndroidBitmap()
            val out = File(context.filesDir, "widget_biome.png")
            FileOutputStream(out).use { fos ->
                bmp.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            bmp.recycle()
        } catch (_: Throwable) {
            try {
                val size = 256
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                bgPaint.shader = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    biome.accentHex.copy(alpha = 0.22f).toArgb(),
                    biome.secondaryHex.copy(alpha = 0.28f).toArgb(),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(RectF(8f, 8f, size - 8f, size - 8f), 44f, 44f, bgPaint)
                val out = File(context.filesDir, "widget_biome.png")
                FileOutputStream(out).use { fos ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 95, fos)
                }
                bmp.recycle()
            } catch (_: Throwable) {
            }
        }
    }
}
