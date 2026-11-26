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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onExportPhotoToGallery: (String, String) -> Unit
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
        MemoryLightboxDialog(
            state = state,
            target = target,
            onExportPhotoToGallery = onExportPhotoToGallery,
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
                                        .clickable { onSelectDay(iso) }
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = cellNum.toString(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = when {
                                                selected -> MaterialTheme.colorScheme.onPrimary
                                                count > 0 -> MaterialTheme.colorScheme.onSecondaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (count > 0 && !selected) {
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
                BalanceRow(SplashStrings.t(state.lang, "Rápidos (2 min)", "Quick (2 min)"), qCount, totalM, quickAccent)
                BalanceRow(SplashStrings.t(state.lang, "Medios (15 min)", "Medium (15 min)"), mCount, totalM, medAccent)
                BalanceRow(SplashStrings.t(state.lang, "Semanales", "Weekly"), lCount, totalM, longAccent)
            }
        }

        // Photo Memories Circular Shortcut Card
        val photoMemories = remember(state.completions) {
            state.completions.filter { !it.photoPath.isNullOrBlank() && File(it.photoPath).exists() }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable { onOpenMemoriesTab() }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Column {
                            Text(
                                text = SplashStrings.t(state.lang, "Fotos recuerdos", "Photo memories"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = SplashStrings.t(
                                    state.lang,
                                    "${photoMemories.size} fotos reales · ${SAMPLE_MEMORIES.size} ejemplos",
                                    "${photoMemories.size} real photos · ${SAMPLE_MEMORIES.size} samples"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenMemoriesTab,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(SplashStrings.t(state.lang, "Abrir menú", "Open menu"))
                    }
                }

                // 4 circular preview thumbnails (real photos first, padded with illustrated samples)
                val realPreviewCount = photoMemories.size.coerceAtMost(4)
                val samplePreviewCount = 4 - realPreviewCount
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    photoMemories.take(4).forEach { mem ->
                        val thumb = remember(mem.photoPath) {
                            mem.photoPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                        }
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    SAMPLE_MEMORIES.take(samplePreviewCount).forEach { sample ->
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(2.dp, sample.accentColor.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            SampleMemoryArtwork(
                                sampleId = sample.id,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Selected Day Memories
        val dayLogs = remember(state.completions, state.selectedDayIso) {
            state.completions.filter { it.dateIso == state.selectedDayIso }
        }
        Text(
            text = if (dayLogs.isEmpty()) {
                SplashStrings.t(state.lang, "Sin recuerdos guardados ese día", "No saved memories on that day")
            } else {
                SplashStrings.t(state.lang, "Recuerdos del ${state.selectedDayIso}", "Memories on ${state.selectedDayIso}")
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        dayLogs.forEach { log ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.lang == AppLanguage.EN) log.titleEn else log.titleEs,
                            style = MaterialTheme.typography.titleSmall
                        )
                        val bName = SplashCatalogData.BIOMES.firstOrNull { it.id == log.biomeId }?.shortName(state.lang) ?: log.biomeId
                        Text(
                            text = "$bName · +${log.earnedXp} XP",
                            style = MaterialTheme.typography.bodyMedium,
                            color = catColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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
    val realMemories = remember(state.completions) {
        state.completions.filter { !it.photoPath.isNullOrBlank() && File(it.photoPath).exists() }
    }
    var filter by remember { mutableStateOf(MemoryFilter.ALL) }

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
                            text = SplashStrings.t(state.lang, "Fotos recuerdos", "Photo memories"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = SplashStrings.t(
                                state.lang,
                                "${realMemories.size} fotos reales · ${SAMPLE_MEMORIES.size} ejemplos ilustrados",
                                "${realMemories.size} real photos · ${SAMPLE_MEMORIES.size} illustrated samples"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                        text = SplashStrings.t(
                            state.lang,
                            "Todavía no hay fotos reales",
                            "No real photos yet"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = SplashStrings.t(
                            state.lang,
                            "Pulsa el botón Foto en un objetivo semanal para guardar aquí tus momentos reales.",
                            "Tap Photo on any weekly goal to save your real memories here."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Non-nested 2-column circular grid (safe inside WallScreen verticalScroll)
            visibleItems.chunked(2).forEach { pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    pair.forEach { target ->
                        CircularMemoryTile(
                            state = state,
                            target = target,
                            onClick = { onOpenMemoryViewer(target) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .aspectRatio(1f)
        ) {
            Surface(
                shape = CircleShape,
                tonalElevation = 2.dp,
                border = BorderStroke(3.dp, ringColor.copy(alpha = 0.72f)),
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
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = rimBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
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
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var visible by remember(target) { mutableStateOf(false) }
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
                        }

                        if (target is MemoryViewerTarget.RealMemory && !target.log.photoPath.isNullOrBlank()) {
                            FilledTonalButton(
                                onClick = { onExportPhotoToGallery(target.log.photoPath, title) },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(SplashStrings.t(state.lang, "Guardar en galería del móvil", "Save to phone gallery"))
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
            progress = { if (count == 0) 0.03f else (count.toFloat() / total.toFloat()).coerceIn(0.05f, 1f) },
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
