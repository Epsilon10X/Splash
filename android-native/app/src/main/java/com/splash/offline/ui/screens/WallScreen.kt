package com.splash.offline.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.splash.offline.data.AppLanguage
import com.splash.offline.data.CompletionLog
import com.splash.offline.data.GoalCategory
import com.splash.offline.data.MapStyle
import com.splash.offline.data.SplashCatalogData
import com.splash.offline.data.SplashStrings
import com.splash.offline.data.SplashUiState
import com.splash.offline.data.WallSubTab
import com.splash.offline.ui.components.SAMPLE_MEMORIES
import com.splash.offline.ui.components.SampleMemoryArtwork
import com.splash.offline.ui.components.SampleMemoryItem
import com.splash.offline.ui.components.WorldMapCanvas
import com.splash.offline.ui.theme.GoalCardPalettes
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private sealed interface MemoryViewerTarget {
    data class RealMemory(val log: CompletionLog) : MemoryViewerTarget
    data class SampleMemory(val sample: SampleMemoryItem) : MemoryViewerTarget
}

private enum class MemoryFilter {
    ALL, REAL, SAMPLE
}

private enum class MemoriesViewMode {
    PHOTOS, LOGS
}

private enum class MemoriesGroupBy {
    DAY, MONTH, BIOME
}

private fun formatMemoryMonth(ymStr: String, lang: AppLanguage): String {
    return try {
        val ym = YearMonth.parse(ymStr)
        val locale = if (lang == AppLanguage.EN) Locale.ENGLISH else Locale("es", "ES")
        ym.month.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.uppercase() } + " ${ym.year}"
    } catch (_: Throwable) {
        ymStr
    }
}

@Composable
fun WallScreen(
    state: SplashUiState,
    onSelectSubTab: (WallSubTab) -> Unit,
    onSelectMapStyle: (MapStyle) -> Unit,
    onToggleMapAllColor: () -> Unit,
    onMonthDelta: (Int) -> Unit,
    onSelectDay: (String) -> Unit,
    onOpenBiome3D: (Int) -> Unit,
    onTakeMemoryPhoto: () -> Unit,
    onPickGalleryPhoto: () -> Unit,
    onExportPhotoToGallery: (String, String) -> Unit,
    onDeleteMemoryPhoto: (Long) -> Unit,
    onUpdateMemoryBiome: (Long, String) -> Unit,
    onShowToast: (String) -> Unit
) {
    var viewerTarget by remember { mutableStateOf<MemoryViewerTarget?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 124.dp)
    ) {
        Text(
            text = SplashStrings.t(state.lang, "Muro Real", "Real Wall"),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                WallSubTab.MY_WALL to SplashStrings.t(state.lang, "Muro", "Wall"),
                WallSubTab.MEMORIES to SplashStrings.t(state.lang, "Recuerdos", "Memories"),
                WallSubTab.CIRCLE to SplashStrings.t(state.lang, "Círculo", "Circle"),
                WallSubTab.MAP to SplashStrings.t(state.lang, "Mapa", "Map")
            ).forEach { (tab, label) ->
                ToggleButton(
                    checked = state.wallSubTab == tab,
                    onCheckedChange = { onSelectSubTab(tab) },
                    colors = ToggleButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        AnimatedContent(
            targetState = state.wallSubTab,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                    fadeOut(spring(stiffness = Spring.StiffnessMedium))
            },
            label = "wallSub"
        ) { sub ->
            when (sub) {
                WallSubTab.MY_WALL -> MyWallContent(
                    state = state,
                    onMonthDelta = onMonthDelta,
                    onSelectDay = onSelectDay,
                    onOpenMemoriesTab = { onSelectSubTab(WallSubTab.MEMORIES) },
                    onOpenMemoryViewer = { viewerTarget = it }
                )
                WallSubTab.MEMORIES -> MemoriesWallContent(
                    state = state,
                    onTakeMemoryPhoto = onTakeMemoryPhoto,
                    onPickGalleryPhoto = onPickGalleryPhoto,
                    onOpenMemoryViewer = { viewerTarget = it }
                )
                WallSubTab.CIRCLE -> CircleWallContent(state)
                WallSubTab.MAP -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                MapStyle.PRUEBA_1 to SplashStrings.t(state.lang, "Prueba 1", "Style 1"),
                                MapStyle.PRUEBA_2 to SplashStrings.t(state.lang, "Prueba 2", "Style 2"),
                                MapStyle.PRUEBA_3 to SplashStrings.t(state.lang, "Prueba 3", "Style 3"),
                                MapStyle.PRUEBA_4 to SplashStrings.t(state.lang, "Prueba 4", "Style 4")
                            ).forEach { (style, label) ->
                                ToggleButton(
                                    checked = state.mapStyle == style,
                                    onCheckedChange = { onSelectMapStyle(style) },
                                    colors = ToggleButtonDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                                        checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleMapAllColor() }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp)
                                ) {
                                    Text(
                                        text = SplashStrings.t(state.lang, "Todo a color", "All in color"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (state.mapShowAllColor) {
                                            SplashStrings.t(
                                                state.lang,
                                                "Todos los biomas se muestran a color",
                                                "All biomes shown in color"
                                            )
                                        } else {
                                            SplashStrings.t(
                                                state.lang,
                                                "Biomas bloqueados en gris (${state.unlockedBiomeCount}/10)",
                                                "Locked biomes in grey (${state.unlockedBiomeCount}/10)"
                                            )
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = state.mapShowAllColor,
                                    onCheckedChange = { onToggleMapAllColor() }
                                )
                            }
                        }

                        WorldMapCanvas(
                            lang = state.lang,
                            mapStyle = state.mapStyle,
                            showAllColor = state.mapShowAllColor,
                            activeBiomeIndex = state.activeBiomeIndex,
                            activeBiomeLevel = state.effectiveBiomeLevel,
                            unlockedCount = state.unlockedBiomeCount,
                            betaMode = state.betaMode,
                            biomeXpById = state.biomeXpById,
                            onBiomeSelected = onOpenBiome3D,
                            onLockedBiomeTapped = onShowToast,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                        )
                    }
                }
            }
        }
    }

    viewerTarget?.let { target ->
        val resolvedTarget = if (target is MemoryViewerTarget.RealMemory) {
            state.completions.firstOrNull { it.id == target.log.id }?.let { MemoryViewerTarget.RealMemory(it) }
                ?: target
        } else {
            target
        }
        MemoryLightboxDialog(
            state = state,
            target = resolvedTarget,
            onExportPhotoToGallery = onExportPhotoToGallery,
            onDeleteMemoryPhoto = { id ->
                onDeleteMemoryPhoto(id)
                viewerTarget = null
            },
            onUpdateMemoryBiome = onUpdateMemoryBiome,
            onClose = { viewerTarget = null }
        )
    }
}

@Composable
private fun MyWallContent(
    state: SplashUiState,
    onMonthDelta: (Int) -> Unit,
    onSelectDay: (String) -> Unit,
    onOpenMemoriesTab: () -> Unit,
    onOpenMemoryViewer: (MemoryViewerTarget) -> Unit
) {
    val ym = remember(state.selectedMonthOffset) {
        YearMonth.now().plusMonths(state.selectedMonthOffset.toLong())
    }
    val locale = if (state.lang == AppLanguage.EN) Locale.ENGLISH else Locale("es", "ES")
    val monthTitle = remember(ym, state.lang) {
        ym.month.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.uppercase() } + " ${ym.year}"
    }
    val countsByDate = remember(state.completions) {
        state.completions.groupingBy { it.dateIso }.eachCount()
    }
    val photosByDate = remember(state.completions) {
        state.completions
            .filter { !it.photoPath.isNullOrBlank() && File(it.photoPath).exists() }
            .groupBy { it.dateIso }
    }
    val firstDayOffset = (ym.atDay(1).dayOfWeek.value - 1)
    val daysInMonth = ym.lengthOfMonth()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Calendar Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { onMonthDelta(-1) }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
                    }
                    Text(monthTitle, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { onMonthDelta(1) }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                }

                val weekdays = if (state.lang == AppLanguage.EN) {
                    listOf("M", "T", "W", "T", "F", "S", "S")
                } else {
                    listOf("L", "M", "X", "J", "V", "S", "D")
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekdays.forEach { d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                val totalCells = ((firstDayOffset + daysInMonth + 6) / 7) * 7
                for (rowIdx in 0 until totalCells / 7) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (colIdx in 0 until 7) {
                            val cellNum = rowIdx * 7 + colIdx - firstDayOffset + 1
                            if (cellNum !in 1..daysInMonth) {
                                Spacer(Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val iso = ym.atDay(cellNum).toString()
                                val count = countsByDate[iso] ?: 0
                                val dayPhotos = photosByDate[iso].orEmpty()
                                val firstPhotoPath = dayPhotos.firstOrNull()?.photoPath
                                val dayThumb = remember(firstPhotoPath) {
                                    firstPhotoPath?.let { path ->
                                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                                        BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
                                    }
                                }
                                val selected = state.selectedDayIso == iso
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                selected -> MaterialTheme.colorScheme.primary
                                                count > 0 -> MaterialTheme.colorScheme.secondaryContainer
                                                else -> Color.Transparent
                                            }
                                        )
                                        .then(
                                            if (dayThumb != null) {
                                                Modifier.border(
                                                    width = if (selected) 2.5.dp else 1.5.dp,
                                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                    shape = CircleShape
                                                )
                                            } else Modifier
                                        )
                                        .clickable { onSelectDay(iso) }
                                ) {
                                    if (dayThumb != null) {
                                        Image(
                                            bitmap = dayThumb,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    if (selected) Color.Black.copy(alpha = 0.28f)
                                                    else Color.Black.copy(alpha = 0.40f)
                                                )
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = cellNum.toString(),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (dayThumb != null || selected) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                dayThumb != null -> Color.White
                                                selected -> MaterialTheme.colorScheme.onPrimary
                                                count > 0 -> MaterialTheme.colorScheme.onSecondaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (dayThumb == null && count > 0 && !selected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Monthly Balance Bars
        val monthPrefix = ym.toString()
        val monthLogs = remember(state.completions, monthPrefix) {
            state.completions.filter { it.dateIso.startsWith(monthPrefix) }
        }
        val totalM = monthLogs.size.coerceAtLeast(1)
        val qCount = monthLogs.count { it.category == GoalCategory.RAPIDO }
        val mCount = monthLogs.count { it.category == GoalCategory.MEDIO }
        val lCount = monthLogs.count { it.category == GoalCategory.LARGO }

        val isDark = GoalCardPalettes.isSplashDarkTheme()
        val quickAccent = GoalCardPalettes.expressiveTone(GoalCategory.RAPIDO, isDark).accent
        val medAccent = GoalCardPalettes.expressiveTone(GoalCategory.MEDIO, isDark).accent
        val longAccent = GoalCardPalettes.expressiveTone(GoalCategory.LARGO, isDark).accent

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(14.dp)) {
                Text(
                    text = SplashStrings.t(state.lang, "Equilibrio este mes", "Balance this month"),
                    style = MaterialTheme.typography.titleSmall
                )
                BalanceRow(SplashStrings.t(state.lang, "Flash (2 min)", "Flash (2 min)"), qCount, totalM, quickAccent)
                BalanceRow(SplashStrings.t(state.lang, "Medios (15 min)", "Medium (15 min)"), mCount, totalM, medAccent)
                BalanceRow(SplashStrings.t(state.lang, "Semanales", "Weekly"), lCount, totalM, longAccent)
            }
        }

        // Selected Day Memories (compact: only last 2 shown here, full list in Recuerdos menu)
        val dayLogs = remember(state.completions, state.selectedDayIso) {
            state.completions.filter { it.dateIso == state.selectedDayIso }
        }
        val previewLogs = remember(dayLogs) { dayLogs.take(2) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (dayLogs.isEmpty()) {
                    SplashStrings.t(state.lang, "Sin recuerdos guardados ese día", "No saved memories on that day")
                } else {
                    SplashStrings.t(state.lang, "Últimos recuerdos (${state.selectedDayIso})", "Recent memories (${state.selectedDayIso})")
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (dayLogs.isNotEmpty()) {
                TextButton(onClick = onOpenMemoriesTab) {
                    Text(SplashStrings.t(state.lang, "Ver todos (${dayLogs.size})", "See all (${dayLogs.size})"))
                }
            }
        }

        previewLogs.forEach { log ->
            MemoryLogRowCard(
                state = state,
                log = log,
                isDark = isDark,
                onOpenMemoryViewer = onOpenMemoryViewer
            )
        }
    }
}

@Composable
private fun MemoryLogRowCard(
    state: SplashUiState,
    log: CompletionLog,
    isDark: Boolean,
    onOpenMemoryViewer: (MemoryViewerTarget) -> Unit
) {
    val catColor = GoalCardPalettes.expressiveTone(log.category, isDark).accent
    val bmp = remember(log.photoPath) {
        log.photoPath?.let { p ->
            val f = File(p)
            if (f.exists()) BitmapFactory.decodeFile(p)?.asImageBitmap() else null
        }
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (bmp != null) Modifier.clickable {
                    onOpenMemoryViewer(MemoryViewerTarget.RealMemory(log))
                } else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(2.dp, catColor.copy(alpha = 0.55f), CircleShape)
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = catColor
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.lang == AppLanguage.EN) log.titleEn else log.titleEs,
                    style = MaterialTheme.typography.titleSmall
                )
                if (!log.note.isNullOrBlank()) {
                    Text(
                        text = "\"${log.note}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val bName = SplashCatalogData.BIOMES.firstOrNull { it.id == log.biomeId }?.shortName(state.lang) ?: log.biomeId
                Text(
                    text = "$bName · +${log.earnedXp} pts · ${log.dateIso}",
                    style = MaterialTheme.typography.labelMedium,
                    color = catColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MemoriesWallContent(
    state: SplashUiState,
    onTakeMemoryPhoto: () -> Unit,
    onPickGalleryPhoto: () -> Unit,
    onOpenMemoryViewer: (MemoryViewerTarget) -> Unit
) {
    val isDark = GoalCardPalettes.isSplashDarkTheme()
    val realMemories = remember(state.completions) {
        state.completions.filter { !it.photoPath.isNullOrBlank() && File(it.photoPath).exists() }
    }
    var viewMode by remember { mutableStateOf(MemoriesViewMode.PHOTOS) }
    var groupBy by remember { mutableStateOf(MemoriesGroupBy.DAY) }
    var filter by remember { mutableStateOf(MemoryFilter.ALL) }
    var inspectedLog by remember { mutableStateOf<CompletionLog?>(null) }

    val visibleItems = remember(realMemories, filter) {
        when (filter) {
            MemoryFilter.ALL ->
                realMemories.map { MemoryViewerTarget.RealMemory(it) } +
                    SAMPLE_MEMORIES.map { MemoryViewerTarget.SampleMemory(it) }
            MemoryFilter.REAL ->
                realMemories.map { MemoryViewerTarget.RealMemory(it) }
            MemoryFilter.SAMPLE ->
                SAMPLE_MEMORIES.map { MemoryViewerTarget.SampleMemory(it) }
        }
    }

    fun groupKeyForPhoto(target: MemoryViewerTarget): String {
        val dateIso = when (target) {
            is MemoryViewerTarget.RealMemory -> target.log.dateIso
            is MemoryViewerTarget.SampleMemory -> target.sample.dateLabel(state.lang)
        }
        val biomeId = when (target) {
            is MemoryViewerTarget.RealMemory -> target.log.biomeId
            is MemoryViewerTarget.SampleMemory -> target.sample.biomeId
        }
        return when (groupBy) {
            MemoriesGroupBy.DAY -> dateIso
            MemoriesGroupBy.MONTH -> {
                if (target is MemoryViewerTarget.RealMemory && target.log.dateIso.length >= 7) {
                    formatMemoryMonth(target.log.dateIso.substring(0, 7), state.lang)
                } else {
                    SplashStrings.t(state.lang, "Ejemplos", "Samples")
                }
            }
            MemoriesGroupBy.BIOME ->
                SplashCatalogData.BIOMES.firstOrNull { it.id == biomeId }?.name(state.lang) ?: biomeId
        }
    }

    fun groupKeyForLog(log: CompletionLog): String {
        return when (groupBy) {
            MemoriesGroupBy.DAY -> log.dateIso
            MemoriesGroupBy.MONTH -> {
                if (log.dateIso.length >= 7) formatMemoryMonth(log.dateIso.substring(0, 7), state.lang)
                else log.dateIso
            }
            MemoriesGroupBy.BIOME ->
                SplashCatalogData.BIOMES.firstOrNull { it.id == log.biomeId }?.name(state.lang) ?: log.biomeId
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                    Column {
                        Text(
                            text = SplashStrings.t(state.lang, "Fotos y Recuerdos", "Photos & Memories"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = SplashStrings.t(
                                state.lang,
                                "${realMemories.size} fotos · ${state.completions.size} recuerdos registrados",
                                "${realMemories.size} photos · ${state.completions.size} saved memories"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Switch between Photos and Full Memories Log
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        MemoriesViewMode.PHOTOS to SplashStrings.t(state.lang, "Fotos (${realMemories.size})", "Photos (${realMemories.size})"),
                        MemoriesViewMode.LOGS to SplashStrings.t(state.lang, "Recuerdos (${state.completions.size})", "Memories (${state.completions.size})")
                    ).forEach { (m, lbl) ->
                        ToggleButton(
                            checked = viewMode == m,
                            onCheckedChange = { viewMode = m },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(lbl, maxLines = 1)
                        }
                    }
                }

                // Shared ordering/grouping bar: Day / Month / Biome
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    listOf(
                        MemoriesGroupBy.DAY to SplashStrings.t(state.lang, "Por día", "By day"),
                        MemoriesGroupBy.MONTH to SplashStrings.t(state.lang, "Por mes", "By month"),
                        MemoriesGroupBy.BIOME to SplashStrings.t(state.lang, "Por bioma", "By biome")
                    ).forEach { (g, lbl) ->
                        FilterChip(
                            selected = groupBy == g,
                            onClick = { groupBy = g },
                            shape = CircleShape,
                            label = { Text(lbl, maxLines = 1) }
                        )
                    }
                }

                if (viewMode == MemoriesViewMode.PHOTOS) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            MemoryFilter.ALL to SplashStrings.t(state.lang, "Todas", "All"),
                            MemoryFilter.REAL to SplashStrings.t(state.lang, "Mis fotos", "My photos"),
                            MemoryFilter.SAMPLE to SplashStrings.t(state.lang, "Ejemplos", "Samples")
                        ).forEach { (f, lbl) ->
                            FilterChip(
                                selected = filter == f,
                                onClick = { filter = f },
                                shape = CircleShape,
                                label = { Text(lbl, maxLines = 1) }
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilledTonalButton(
                            onClick = onTakeMemoryPhoto,
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(SplashStrings.t(state.lang, "Hacer foto", "Take photo"), maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = onPickGalleryPhoto,
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(SplashStrings.t(state.lang, "Galería", "Gallery"), maxLines = 1)
                        }
                    }
                }
            }
        }

        if (viewMode == MemoriesViewMode.PHOTOS) {
            if (visibleItems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                        Text(
                            text = SplashStrings.t(state.lang, "Todavía no hay fotos reales", "No real photos yet"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = SplashStrings.t(
                                state.lang,
                                "Haz una foto o impórtala de tu galería para guardarla y organizarla aquí.",
                                "Take a photo or import from your gallery to organize it here."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val groupedPhotos = visibleItems.groupBy { groupKeyForPhoto(it) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    groupedPhotos.forEach { (sectionTitle, itemsInGroup) ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Text(
                                    text = "$sectionTitle (${itemsInGroup.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    itemsInGroup.forEach { target ->
                                        CircularMemoryTile(
                                            state = state,
                                            target = target,
                                            onClick = { onOpenMemoryViewer(target) },
                                            modifier = Modifier.width(84.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (state.completions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = SplashStrings.t(
                            state.lang,
                            "Todavía no hay recuerdos completados. Completa un objetivo en Hoy para verlo aquí.",
                            "No completed memories yet. Complete a goal on Today to see it here."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                val groupedLogs = state.completions.groupBy { groupKeyForLog(it) }
                groupedLogs.forEach { (sectionTitle, logsInGroup) ->
                    Text(
                        text = "$sectionTitle (${logsInGroup.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        logsInGroup.chunked(3).forEach { rowLogs ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowLogs.forEach { log ->
                                    val hasPhoto = !log.photoPath.isNullOrBlank() && File(log.photoPath).exists()
                                    MiniMemoryLogTile(
                                        state = state,
                                        log = log,
                                        isDark = isDark,
                                        hasPhoto = hasPhoto,
                                        onClick = {
                                            if (hasPhoto) {
                                                onOpenMemoryViewer(MemoryViewerTarget.RealMemory(log))
                                            } else {
                                                inspectedLog = log
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - rowLogs.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inspectedLog?.let { log ->
        MemoryLogDetailDialog(
            state = state,
            log = log,
            isDark = isDark,
            onDismiss = { inspectedLog = null }
        )
    }
}

@Composable
private fun MiniMemoryLogTile(
    state: SplashUiState,
    log: CompletionLog,
    isDark: Boolean,
    hasPhoto: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catColor = GoalCardPalettes.expressiveTone(log.category, isDark).accent
    val title = if (state.lang == AppLanguage.EN) log.titleEn else log.titleEs
    val bShort = SplashCatalogData.BIOMES.firstOrNull { it.id == log.biomeId }?.shortName(state.lang) ?: log.biomeId
    val shortDate = if (log.dateIso.length >= 10) log.dateIso.substring(5) else log.dateIso

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, catColor.copy(alpha = 0.28f)),
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(catColor)
                    )
                    Text(
                        text = "+${log.earnedXp}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = catColor,
                        maxLines = 1
                    )
                }
                if (hasPhoto) {
                    Icon(
                        imageVector = Icons.Rounded.Photo,
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(12.dp)
                    )
                } else if (!log.note.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Rounded.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$bShort · $shortDate",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MemoryLogDetailDialog(
    state: SplashUiState,
    log: CompletionLog,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val catColor = GoalCardPalettes.expressiveTone(log.category, isDark).accent
    val biomeName = SplashCatalogData.BIOMES.firstOrNull { it.id == log.biomeId }?.name(state.lang) ?: log.biomeId
    val title = if (state.lang == AppLanguage.EN) log.titleEn else log.titleEs

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = catColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "$biomeName · +${log.earnedXp} pts",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    FilledTonalIconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = SplashStrings.t(state.lang, "Cerrar", "Close"))
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = log.dateIso,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!log.note.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = SplashStrings.t(state.lang, "Mensaje para yo del futuro", "Message to my future self"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\"${log.note}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularMemoryTile(
    state: SplashUiState,
    target: MemoryViewerTarget,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringColor = when (target) {
        is MemoryViewerTarget.RealMemory -> {
            SplashCatalogData.BIOMES.firstOrNull { it.id == target.log.biomeId }?.accentHex
                ?: MaterialTheme.colorScheme.primary
        }
        is MemoryViewerTarget.SampleMemory -> target.sample.accentColor
    }

    val title = when (target) {
        is MemoryViewerTarget.RealMemory -> if (state.lang == AppLanguage.EN) target.log.titleEn else target.log.titleEs
        is MemoryViewerTarget.SampleMemory -> target.sample.title(state.lang)
    }

    val subtitle = when (target) {
        is MemoryViewerTarget.RealMemory -> target.log.dateIso
        is MemoryViewerTarget.SampleMemory -> target.sample.dateLabel(state.lang)
    }

    val rimBadge = when (target) {
        is MemoryViewerTarget.RealMemory ->
            SplashCatalogData.BIOMES.firstOrNull { it.id == target.log.biomeId }?.shortName(state.lang) ?: target.log.biomeId
        is MemoryViewerTarget.SampleMemory -> SplashStrings.t(state.lang, "Ejemplo", "Sample")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(1f)
        ) {
            Surface(
                shape = CircleShape,
                tonalElevation = 1.dp,
                border = BorderStroke(2.dp, ringColor.copy(alpha = 0.72f)),
                modifier = Modifier.fillMaxSize()
            ) {
                when (target) {
                    is MemoryViewerTarget.RealMemory -> {
                        val thumb = remember(target.log.photoPath) {
                            target.log.photoPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                        }
                        if (thumb != null) {
                            Image(
                                bitmap = thumb,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    is MemoryViewerTarget.SampleMemory -> {
                        SampleMemoryArtwork(
                            sampleId = target.sample.id,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.88f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = rimBadge,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MemoryLightboxDialog(
    state: SplashUiState,
    target: MemoryViewerTarget,
    onExportPhotoToGallery: (String, String) -> Unit,
    onDeleteMemoryPhoto: (Long) -> Unit,
    onUpdateMemoryBiome: (Long, String) -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var visible by remember(target) { mutableStateOf(false) }
        var confirmDelete by remember(target) { mutableStateOf(false) }
        LaunchedEffect(target) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.90f,
                animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn()
        ) {
            val accentColor = when (target) {
                is MemoryViewerTarget.RealMemory ->
                    SplashCatalogData.BIOMES.firstOrNull { it.id == target.log.biomeId }?.accentHex
                        ?: MaterialTheme.colorScheme.primary
                is MemoryViewerTarget.SampleMemory -> target.sample.accentColor
            }
            val biomeName = when (target) {
                is MemoryViewerTarget.RealMemory ->
                    SplashCatalogData.BIOMES.firstOrNull { it.id == target.log.biomeId }?.name(state.lang)
                        ?: target.log.biomeId
                is MemoryViewerTarget.SampleMemory ->
                    SplashCatalogData.BIOMES.firstOrNull { it.id == target.sample.biomeId }?.name(state.lang)
                        ?: target.sample.biomeId
            }
            val title = when (target) {
                is MemoryViewerTarget.RealMemory -> if (state.lang == AppLanguage.EN) target.log.titleEn else target.log.titleEs
                is MemoryViewerTarget.SampleMemory -> target.sample.title(state.lang)
            }
            val dateLabel = when (target) {
                is MemoryViewerTarget.RealMemory -> target.log.dateIso
                is MemoryViewerTarget.SampleMemory -> target.sample.dateLabel(state.lang)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(onClick = onClose)
                    .padding(20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = biomeName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            FilledTonalIconButton(
                                onClick = onClose,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = SplashStrings.t(state.lang, "Cerrar", "Close"))
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(4.dp, accentColor.copy(alpha = 0.72f)),
                            modifier = Modifier
                                .fillMaxWidth(0.84f)
                                .aspectRatio(1f)
                        ) {
                            when (target) {
                                is MemoryViewerTarget.RealMemory -> {
                                    val fullBmp = remember(target.log.photoPath) {
                                        target.log.photoPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                                    }
                                    if (fullBmp != null) {
                                        Image(
                                            bitmap = fullBmp,
                                            contentDescription = title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                is MemoryViewerTarget.SampleMemory -> {
                                    SampleMemoryArtwork(
                                        sampleId = target.sample.id,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (target is MemoryViewerTarget.RealMemory && !target.log.note.isNullOrBlank()) {
                                Text(
                                    text = "\"${target.log.note}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        if (target is MemoryViewerTarget.RealMemory && !target.log.photoPath.isNullOrBlank()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = SplashStrings.t(state.lang, "Organizar en bioma", "Organize by biome"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    SplashCatalogData.BIOMES.forEach { b ->
                                        FilterChip(
                                            selected = target.log.biomeId == b.id,
                                            onClick = { onUpdateMemoryBiome(target.log.id, b.id) },
                                            shape = CircleShape,
                                            label = { Text(b.shortName(state.lang), maxLines = 1) }
                                        )
                                    }
                                }
                            }

                            FilledTonalButton(
                                onClick = { onExportPhotoToGallery(target.log.photoPath, title) },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(SplashStrings.t(state.lang, "Guardar en galería del móvil", "Save to phone gallery"))
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!confirmDelete) {
                                        confirmDelete = true
                                    } else {
                                        onDeleteMemoryPhoto(target.log.id)
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (confirmDelete) {
                                        SplashStrings.t(state.lang, "Confirmar eliminar foto", "Confirm delete photo")
                                    } else {
                                        SplashStrings.t(state.lang, "Eliminar foto", "Delete photo")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceRow(label: String, count: Int, total: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(count.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        LinearWavyProgressIndicator(
            progress = { if (count == 0) 0f else (count.toFloat() / total.toFloat()).coerceIn(0.05f, 1f) },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CircleWallContent(state: SplashUiState) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Text(
                text = SplashStrings.t(state.lang, "Trabajando en Círculo", "Working on Circle"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = SplashStrings.t(
                    state.lang,
                    "Estamos diseñando nuevas ideas para cumplir objetivos reales con amigos y familiares, sin comparaciones ni presión.",
                    "We're working on new ideas to complete real-life goals with friends and family, without comparisons or pressure."
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    SplashStrings.t(state.lang, "Retos compartidos", "Shared goals"),
                    SplashStrings.t(state.lang, "Postales de bioma", "Biome postcards"),
                    SplashStrings.t(state.lang, "Amigos y familia", "Friends & family")
                ).forEach { tag ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}
