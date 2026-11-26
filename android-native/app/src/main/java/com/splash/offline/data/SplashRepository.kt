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
import androidx.compose.ui.graphics.toArgb
import com.splash.offline.SplashBiomeWidgetProvider
import com.splash.offline.SplashNotificationScheduler
import com.splash.offline.SplashQuickWidgetProvider
import com.splash.offline.SplashWidgetProvider
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
                    customGoals.add(
                        GoalItem(
                            id = cg.getInt("id"),
                            titleShortEs = cg.optString("titleEs"),
                            titleEs = cg.optString("titleEs"),
                            subEs = cg.optString("subEs", "Personal"),
                            titleShortEn = cg.optString("titleEn", cg.optString("titleEs")),
                            titleEn = cg.optString("titleEn", cg.optString("titleEs")),
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
                            earnedXp = c.optInt("earnedXp", 0)
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

            val weeklyArr = o.optJSONArray("weeklyIds")
            val weeklyIds = if (weeklyArr != null && weeklyArr.length() == 3) {
                listOf(weeklyArr.getInt(0), weeklyArr.getInt(1), weeklyArr.getInt(2))
            } else {
                listOf(38, 39, 40)
            }

            val weekIso = currentWeekIso()
            val savedWeek = o.optString("todayWeekIso", weekIso)
            val sameWeek = savedWeek == weekIso
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
                userName = o.optString("userName", "Explorador").ifBlank { "Explorador" },
                activeBiomeIndex = o.optInt("activeBiomeIndex", 0).coerceIn(0, SplashCatalogData.BIOMES.lastIndex),
                unlockedBiomeCount = o.optInt("unlockedBiomeCount", 1).coerceIn(1, SplashCatalogData.BIOMES.size),
                biomeXpById = xpMap,
                betaMode = o.optBoolean("betaMode", false),
                todayDateIso = todayIso,
                todayWeekIso = weekIso,
                quickUsedToday = if (sameDay) o.optInt("quickUsedToday", 0) else 0,
                mediumUsedToday = if (sameDay) o.optInt("mediumUsedToday", 0) else 0,
                weeklyUsedThisWeek = if (sameWeek) o.optInt("weeklyUsedThisWeek", 0) else 0,
                currentQuickIds = o.optInt("quickGenId", 1) to o.optInt("quickBiomeId", 101),
                currentMediumIds = o.optInt("medGenId", 21) to o.optInt("medBiomeId", 102),
                currentWeeklyIds = weeklyIds,
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
        o.put("activeBiomeIndex", state.activeBiomeIndex)
        o.put("unlockedBiomeCount", state.unlockedBiomeCount)
        o.put("betaMode", state.betaMode)
        o.put("todayDateIso", state.todayDateIso)
        o.put("todayWeekIso", state.todayWeekIso.ifBlank { currentWeekIso() })
        o.put("quickUsedToday", state.quickUsedToday)
        o.put("mediumUsedToday", state.mediumUsedToday)
        o.put("weeklyUsedThisWeek", state.weeklyUsedThisWeek)
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
                        .put("titleEs", g.titleEs)
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
        val quickGoal = state.catalog.firstOrNull { it.id == state.currentQuickIds.first }
        val medGoal = state.catalog.firstOrNull { it.id == state.currentMediumIds.first }

        renderWidgetBiomeBitmap(biome)

        context.getSharedPreferences("splash_widget", Context.MODE_PRIVATE).edit()
            .putString("biomeName", biome.name(state.lang))
            .putString("biomeShort", biome.shortName(state.lang))
            .putString("levelLabel", levelLabel)
            .putString("streakLabel", streakLabel)
            .putString("weatherLabel", if (isEn) "Serene" else "En calma")
            .putString("goal2Title", quickGoal?.displayShort(state.lang) ?: "Splash")
            .putString("goal15Title", medGoal?.displayShort(state.lang) ?: "Paseo")
            .putString("themeMode", state.widgetThemePref.wire)
            .putBoolean("isEn", isEn)
            .apply()

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
            when (g.category) {
                GoalCategory.RAPIDO -> quickPool.put(g.displayTitle(state.lang))
                GoalCategory.MEDIO -> medioPool.put(g.displayTitle(state.lang))
                GoalCategory.LARGO -> Unit
            }
        }
        val notifPayload = JSONObject()
            .put("lang", state.lang.code)
            .put("quickBadge", if (isEn) "Splash · 2 min" else "Splash · Aviso Rápido")
            .put("medioBadge", if (isEn) "Splash · 15 min" else "Splash · Enfoque")
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

    private fun renderWidgetBiomeBitmap(biome: BiomeDef) {
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

            val cx = size / 2f
            val cy = size / 2f + 16f
            val rx = 86f
            val ry = 44f

            val topPath = Path().apply {
                moveTo(cx, cy - ry)
                lineTo(cx + rx, cy)
                lineTo(cx, cy + ry)
                lineTo(cx - rx, cy)
                close()
            }
            val leftPath = Path().apply {
                moveTo(cx - rx, cy)
                lineTo(cx, cy + ry)
                lineTo(cx, cy + ry + 26f)
                lineTo(cx - rx, cy + 26f)
                close()
            }
            val rightPath = Path().apply {
                moveTo(cx, cy + ry)
                lineTo(cx + rx, cy)
                lineTo(cx + rx, cy + 26f)
                lineTo(cx, cy + ry + 26f)
                close()
            }

            val pTop = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = biome.accentHex.toArgb() }
            val pWater = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = biome.secondaryHex.toArgb() }
            val pLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3E3831.toInt() }
            val pRight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF28241F.toInt() }

            canvas.drawPath(leftPath, pLeft)
            canvas.drawPath(rightPath, pRight)
            canvas.drawPath(topPath, pTop)

            val poolPath = Path().apply {
                moveTo(cx, cy - ry * 0.45f)
                lineTo(cx + rx * 0.48f, cy)
                lineTo(cx, cy + ry * 0.45f)
                lineTo(cx - rx * 0.48f, cy)
                close()
            }
            canvas.drawPath(poolPath, pWater)

            val pPeak = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = biome.tertiaryHex.toArgb() }
            canvas.drawCircle(cx - 28f, cy - 18f, 18f, pPeak)
            canvas.drawCircle(cx + 24f, cy - 10f, 14f, pPeak)

            val out = File(context.filesDir, "widget_biome.png")
            FileOutputStream(out).use { fos ->
                bmp.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            bmp.recycle()
        } catch (_: Throwable) {
        }
    }
}
