package com.splash.offline.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splash.offline.R
import com.splash.offline.data.GoalCategory
import com.splash.offline.ui.theme.GoalCardPalettes
import com.splash.offline.data.GoalItem
import com.splash.offline.data.SplashCatalogData
import com.splash.offline.data.SplashStrings
import com.splash.offline.data.SplashUiState
import com.splash.offline.data.TimeBucket
import com.splash.offline.ui.components.BiomeDioramaCanvas

@Composable
fun SplashLeafMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_splash_brand),
        contentDescription = "Splash",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
    )
}

@Composable
fun TodayScreen(
    state: SplashUiState,
    onOpenSettings: () -> Unit,
    onOpen3D: () -> Unit,
    onSwitchBiome: (Int) -> Unit,
    onSelectBucket: (TimeBucket) -> Unit,
    onReroll: () -> Unit,
    onRequestExtraGoal: (TimeBucket) -> Unit,
    onDismissExtraGoal: (TimeBucket) -> Unit,
    onStartGoal: (GoalItem, Boolean, Boolean) -> Unit,
    onPhotoGoal: (GoalItem, Boolean, Boolean) -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenBiomes: () -> Unit
) {
    val biome = state.activeBiome
    val maxBiomeIdx = if (state.betaMode) SplashCatalogData.BIOMES.lastIndex else state.unlockedBiomeCount - 1
    val canPrev = state.activeBiomeIndex > 0
    val canNext = state.activeBiomeIndex < maxBiomeIdx
    val stage = state.effectiveBiomeLevel
    val stageXpInLevel = state.activeBiomeXp % 10
    val xpFraction = if (state.activeBiomeXp >= 80) 1f else (stageXpInLevel / 10f).coerceIn(0.06f, 1f)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 96.dp)
    ) {
        // Minimalist M3 Expressive Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SplashLeafMark()
                Text(
                    text = "Splash",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = SplashStrings.streakLabel(state.lang, state.streakDays),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.clickable { onOpenBiomes() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Terrain,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${state.unlockedBiomeCount}/10",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = SplashStrings.t(state.lang, "Ajustes", "Settings"),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // 3D Biome Diorama Stage Card
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(385.dp)
                    .clickable { onOpen3D() }
            ) {
                BiomeDioramaCanvas(
                    biomeId = biome.id,
                    stage = stage,
                    points = if (state.betaStageOverride != null) null else state.activeBiomeXp,
                    zoom = 1.04f,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 52.dp)
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.90f),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clickable { onOpen3D() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RotateRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "3D",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Bottom HUD inside Diorama Card with official M3E LinearWavyProgressIndicator
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = { onSwitchBiome(state.activeBiomeIndex - 1) },
                                    enabled = canPrev,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
                                }
                                Text(
                                    text = biome.name(state.lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                IconButton(
                                    onClick = { onSwitchBiome(state.activeBiomeIndex + 1) },
                                    enabled = canNext,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                val lvlPrefix = SplashStrings.t(state.lang, "Nv.", "Lvl.")
                                val mood = SplashStrings.t(state.lang, "En calma", "Serene")
                                Text(
                                    text = "$lvlPrefix $stage · $stageXpInLevel/10 · $mood",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        LinearWavyProgressIndicator(
                            progress = { xpFraction },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }
            }
        }

        // Material 3 Expressive ToggleButton Segmented Switcher (2 min / 15 min / Semanal)
        val isDark = GoalCardPalettes.isSplashDarkTheme()
        val quickTone = GoalCardPalettes.expressiveTone(GoalCategory.RAPIDO, isDark)
        val medTone = GoalCardPalettes.expressiveTone(GoalCategory.MEDIO, isDark)
        val longTone = GoalCardPalettes.expressiveTone(GoalCategory.LARGO, isDark)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TimeBucketChip(
                selected = state.timeBucket == TimeBucket.QUICK_2M,
                onClick = { onSelectBucket(TimeBucket.QUICK_2M) },
                icon = Icons.Rounded.Bolt,
                label = "2 min",
                activeContainer = quickTone.pillBg,
                activeContent = quickTone.pillFg,
                modifier = Modifier.weight(1f)
            )
            TimeBucketChip(
                selected = state.timeBucket == TimeBucket.MEDIUM_15M,
                onClick = { onSelectBucket(TimeBucket.MEDIUM_15M) },
                icon = Icons.Rounded.Timer,
                label = "15 min",
                activeContainer = medTone.pillBg,
                activeContent = medTone.pillFg,
                modifier = Modifier.weight(1f)
            )
            TimeBucketChip(
                selected = state.timeBucket == TimeBucket.WEEKLY,
                onClick = { onSelectBucket(TimeBucket.WEEKLY) },
                icon = Icons.Rounded.LocalFireDepartment,
                label = SplashStrings.t(state.lang, "Semanal", "Weekly"),
                activeContainer = longTone.pillBg,
                activeContent = longTone.pillFg,
                modifier = Modifier.weight(1f)
            )
        }

        // Deck Status Row + Reroll
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            val statusText = when (state.timeBucket) {
                TimeBucket.QUICK_2M -> SplashStrings.t(
                    state.lang,
                    "2 min · Bioma hoy: ${state.quickUsedToday.coerceAtMost(2)}/2 (+1 pt)",
                    "2 min · Biome today: ${state.quickUsedToday.coerceAtMost(2)}/2 (+1 pt)"
                )
                TimeBucket.MEDIUM_15M -> SplashStrings.t(
                    state.lang,
                    "15 min · Bioma hoy: ${state.mediumUsedToday.coerceAtMost(2)}/2 (+4 pts)",
                    "15 min · Biome today: ${state.mediumUsedToday.coerceAtMost(2)}/2 (+4 pts)"
                )
                TimeBucket.WEEKLY -> SplashStrings.t(
                    state.lang,
                    "Semanal · Bioma esta semana: ${state.weeklyUsedThisWeek.coerceAtMost(2)}/2 (+8 pts)",
                    "Weekly · Biome this week: ${state.weeklyUsedThisWeek.coerceAtMost(2)}/2 (+8 pts)"
                )
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onReroll, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = SplashStrings.t(state.lang, "Nuevas sugerencias", "New suggestions"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Animated Mission Deck
        AnimatedContent(
            targetState = state.timeBucket,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                    fadeOut(spring(stiffness = Spring.StiffnessMedium))
            },
            label = "deck"
        ) { bucket ->
            when (bucket) {
                TimeBucket.QUICK_2M -> {
                    if (state.quickUsedToday >= 2) {
                        val extraGoal = state.extraQuickGoalId?.let { id -> state.catalog.firstOrNull { it.id == id } }
                        if (extraGoal != null) {
                            ExtraGoalCard(
                                state = state,
                                goal = extraGoal,
                                onRerollExtra = { onRequestExtraGoal(TimeBucket.QUICK_2M) },
                                onDismiss = { onDismissExtraGoal(TimeBucket.QUICK_2M) },
                                onComplete = { onStartGoal(extraGoal, false, true) }
                            )
                        } else {
                            CompletedDailyBanner(
                                state = state,
                                title = SplashStrings.t(state.lang, "Objetivos Flash Completados", "Flash Goals Completed"),
                                subtitle = SplashStrings.t(state.lang, "Los Objetivos extras no mejoran el Bioma.", "Extra Goals don't upgrade your Biome."),
                                onDoExtra = { onRequestExtraGoal(TimeBucket.QUICK_2M) },
                                onOpenCatalog = onOpenCatalog
                            )
                        }
                    } else {
                        val biomeGoal = state.catalog.firstOrNull { it.id == state.currentQuickIds.second }
                        val genGoal = state.catalog.firstOrNull { it.id == state.currentQuickIds.first }
                        TwoMissionDeckRow(
                            state = state,
                            biomeGoal = biomeGoal,
                            generalGoal = genGoal,
                            onStartGoal = onStartGoal
                        )
                    }
                }

                TimeBucket.MEDIUM_15M -> {
                    if (state.mediumUsedToday >= 2) {
                        val extraGoal = state.extraMediumGoalId?.let { id -> state.catalog.firstOrNull { it.id == id } }
                        if (extraGoal != null) {
                            ExtraGoalCard(
                                state = state,
                                goal = extraGoal,
                                onRerollExtra = { onRequestExtraGoal(TimeBucket.MEDIUM_15M) },
                                onDismiss = { onDismissExtraGoal(TimeBucket.MEDIUM_15M) },
                                onComplete = { onStartGoal(extraGoal, false, true) }
                            )
                        } else {
                            CompletedDailyBanner(
                                state = state,
                                title = SplashStrings.t(state.lang, "Objetivos Medios Completados", "Medium Goals Completed"),
                                subtitle = SplashStrings.t(state.lang, "Los Objetivos extras no mejoran el Bioma.", "Extra Goals don't upgrade your Biome."),
                                onDoExtra = { onRequestExtraGoal(TimeBucket.MEDIUM_15M) },
                                onOpenCatalog = onOpenCatalog
                            )
                        }
                    } else {
                        val biomeGoal = state.catalog.firstOrNull { it.id == state.currentMediumIds.second }
                        val genGoal = state.catalog.firstOrNull { it.id == state.currentMediumIds.first }
                        TwoMissionDeckRow(
                            state = state,
                            biomeGoal = biomeGoal,
                            generalGoal = genGoal,
                            onStartGoal = onStartGoal
                        )
                    }
                }

                TimeBucket.WEEKLY -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.currentWeeklyIds.mapNotNull { id -> state.catalog.firstOrNull { it.id == id } }
                            .forEach { goal ->
                                WeeklyGoalCard(
                                    state = state,
                                    goal = goal,
                                    onPhoto = { onPhotoGoal(goal, false, false) },
                                    onComplete = { onStartGoal(goal, false, false) }
                                )
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeBucketChip(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    activeContainer: Color,
    activeContent: Color,
    modifier: Modifier = Modifier
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        colors = ToggleButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = activeContainer,
            checkedContentColor = activeContent
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        modifier = modifier
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun TwoMissionDeckRow(
    state: SplashUiState,
    biomeGoal: GoalItem?,
    generalGoal: GoalItem?,
    onStartGoal: (GoalItem, Boolean, Boolean) -> Unit
) {
    val isDark = GoalCardPalettes.isSplashDarkTheme()
    val biomeStyle = GoalCardPalettes.biomePostcardStyle(state.activeBiome.id, isDark)
    val isQuick = state.timeBucket == TimeBucket.QUICK_2M
    val density = LocalDensity.current.density

    var mediumGenExpanded by remember(generalGoal?.id, isQuick) { mutableStateOf(false) }

    val biomeWeight by animateFloatAsState(
        targetValue = if (!isQuick && mediumGenExpanded) 0.24f else 1f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow),
        label = "biomeAccordionWeight"
    )
    val generalWeight = 1f

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        biomeGoal?.let { bg ->
            val shape = RoundedCornerShape(24.dp)
            var flippedQuickBiome by remember(bg.id) { mutableStateOf(false) }
            var flippedMediumBiome by remember(bg.id, isQuick) { mutableStateOf(false) }
            val flipDeg by animateFloatAsState(
                targetValue = if (isQuick && flippedQuickBiome) 180f else 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                label = "quickBiomeFlip"
            )
            val flipXDeg by animateFloatAsState(
                targetValue = if (!isQuick && !mediumGenExpanded && flippedMediumBiome) 180f else 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                label = "medBiomeFlipX"
            )
            val isQuickBackFace = isQuick && flipDeg > 90f
            val isMedBackFace = !isQuick && !mediumGenExpanded && flipXDeg > 90f
            val isBackFace = isQuickBackFace || isMedBackFace
            val isCompactRail = !isQuick && mediumGenExpanded

            Box(
                modifier = Modifier
                    .weight(biomeWeight)
                    .height(196.dp)
                    .graphicsLayer {
                        rotationY = flipDeg
                        rotationX = flipXDeg
                        cameraDistance = 14f * density
                    }
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(biomeStyle.bg1, biomeStyle.bg2)))
                    .border(BorderStroke(1.5.dp, biomeStyle.border), shape)
                    .clickable {
                        if (isQuick) flippedQuickBiome = !flippedQuickBiome
                        else if (mediumGenExpanded) mediumGenExpanded = false
                        else flippedMediumBiome = !flippedMediumBiome
                    }
            ) {
                AnimatedContent(
                    targetState = isCompactRail,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "biomeRailMode",
                    modifier = Modifier.fillMaxSize()
                ) { compactRail ->
                    if (compactRail) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 18.dp, horizontal = 6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = biomeStyle.accentSoft,
                                border = BorderStroke(1.dp, biomeStyle.border),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = SplashStrings.iconFor(bg.iconName),
                                    contentDescription = null,
                                    tint = biomeStyle.accent,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = biomeStyle.accent.copy(alpha = 0.14f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = biomeStyle.ink,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    if (isQuickBackFace) rotationY = 180f
                                    if (isMedBackFace) rotationX = 180f
                                }
                        ) {
                            // Decorative orb & large icon watermark ("interior de dibujitos")
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 22.dp, y = (-22).dp)
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) Color.White.copy(alpha = 0.09f)
                                        else biomeStyle.accent.copy(alpha = 0.14f)
                                    )
                            )
                            Icon(
                                imageVector = SplashStrings.iconFor(bg.iconName),
                                contentDescription = null,
                                tint = if (isDark) Color.White.copy(alpha = 0.14f) else biomeStyle.accent.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 10.dp, bottom = 46.dp)
                                    .size(56.dp)
                            )

                            Column(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                            ) {
                                if (isBackFace) {
                                    // Back Face (Prototype A 3D Horizontal Flip & Medium Biome 3D Vertical Flip)
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = biomeStyle.accentSoft,
                                                border = BorderStroke(1.dp, biomeStyle.border)
                                            ) {
                                                Text(
                                                    text = state.activeBiome.shortName(state.lang),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = biomeStyle.accent,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = biomeStyle.accentSoft,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = SplashStrings.t(state.lang, "Volver", "Back"),
                                                    tint = biomeStyle.ink,
                                                    modifier = Modifier.padding(4.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = bg.displayTitle(state.lang),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 14.sp,
                                                lineHeight = 19.sp
                                            ),
                                            fontWeight = FontWeight.Medium,
                                            color = biomeStyle.ink.copy(alpha = 0.96f),
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    // Front Face (Quick Biome & Medium Biome)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = biomeStyle.accentSoft,
                                                border = BorderStroke(1.dp, biomeStyle.border)
                                            ) {
                                                Text(
                                                    text = state.activeBiome.shortName(state.lang),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = biomeStyle.accent,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = biomeStyle.accentSoft
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                GoalCardPalettes.expressiveTone(
                                                                    if (isQuick) GoalCategory.RAPIDO else GoalCategory.MEDIO,
                                                                    isDark
                                                                ).accent
                                                            )
                                                    )
                                                    Text(
                                                        text = if (isQuick) {
                                                            SplashStrings.t(state.lang, "Rápido", "Quick")
                                                        } else {
                                                            SplashStrings.t(state.lang, "Medio", "Medium")
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = biomeStyle.ink
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = bg.displayShort(state.lang),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontSize = 17.sp,
                                                    lineHeight = 22.sp
                                                ),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = biomeStyle.ink,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.RotateRight,
                                                contentDescription = null,
                                                tint = biomeStyle.ink.copy(alpha = 0.78f),
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { onStartGoal(bg, true, false) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = biomeStyle.accent,
                                        contentColor = biomeStyle.buttonInk
                                    )
                                ) {
                                    Text(SplashStrings.t(state.lang, "Completar", "Complete"), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        generalGoal?.let { gg ->
            val catStyle = GoalCardPalettes.categoryCardStyle(gg.category, isDark)
            val shape = RoundedCornerShape(24.dp)
            var curtainOpen by remember(gg.id) { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .weight(generalWeight)
                    .height(196.dp)
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(catStyle.bgStart, catStyle.bgMid, catStyle.bgEnd)))
                    .border(BorderStroke(1.dp, catStyle.borderColor), shape)
                    .clickable {
                        if (isQuick) curtainOpen = !curtainOpen
                        else mediumGenExpanded = !mediumGenExpanded
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 22.dp, y = (-22).dp)
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.09f)
                            else catStyle.buttonBg.copy(alpha = 0.15f)
                        )
                )
                Icon(
                    imageVector = SplashStrings.iconFor(gg.iconName),
                    contentDescription = null,
                    tint = if (isDark) Color.White.copy(alpha = 0.14f) else catStyle.buttonBg.copy(alpha = 0.17f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 46.dp)
                        .size(56.dp)
                )

                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = catStyle.chipBg,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = gg.displaySub(state.lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = catStyle.onCard,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            if (!isQuick && mediumGenExpanded) {
                                Surface(
                                    shape = CircleShape,
                                    color = catStyle.chipBg,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = SplashStrings.t(state.lang, "Cerrar", "Close"),
                                        tint = catStyle.onCard,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = catStyle.onCard.copy(alpha = 0.78f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = gg.displayShort(state.lang),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                lineHeight = 22.sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = catStyle.onCard,
                            maxLines = if (!isQuick && mediumGenExpanded) 2 else 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Prototype D (Medium General): Sideways accordion reveals description right in place
                        AnimatedVisibility(
                            visible = !isQuick && mediumGenExpanded,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = gg.displayTitle(state.lang),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp
                                ),
                                fontWeight = FontWeight.Medium,
                                color = catStyle.onCard.copy(alpha = 0.96f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = { onStartGoal(gg, false, false) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = catStyle.buttonBg,
                            contentColor = catStyle.buttonInk
                        )
                    ) {
                        Text(SplashStrings.t(state.lang, "Completar", "Complete"), fontWeight = FontWeight.Bold)
                    }
                }

                // Prototype B (Quick General): In-Card Sliding Curtain / Peek Overlay
                if (isQuick) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = curtainOpen,
                        enter = slideInVertically(
                            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                        ) { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier.matchParentSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            catStyle.bgEnd.copy(alpha = 0.98f),
                                            catStyle.bgStart.copy(alpha = 0.99f)
                                        )
                                    )
                                )
                                .border(BorderStroke(1.dp, catStyle.borderColor), shape)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = catStyle.chipBg,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(
                                                text = gg.displaySub(state.lang),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = catStyle.onCard,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = catStyle.chipBg,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = SplashStrings.t(state.lang, "Cerrar", "Close"),
                                                tint = catStyle.onCard,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = gg.displayTitle(state.lang),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.5.sp,
                                            lineHeight = 18.sp
                                        ),
                                        fontWeight = FontWeight.SemiBold,
                                        color = catStyle.onCard.copy(alpha = 0.96f),
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = { onStartGoal(gg, false, false) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = catStyle.buttonBg,
                                        contentColor = catStyle.buttonInk
                                    )
                                ) {
                                    Text(SplashStrings.t(state.lang, "Completar", "Complete"), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraGoalCard(
    state: SplashUiState,
    goal: GoalItem,
    onRerollExtra: () -> Unit,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val isDark = GoalCardPalettes.isSplashDarkTheme()
    val catStyle = GoalCardPalettes.categoryCardStyle(goal.category, isDark)
    val shape = RoundedCornerShape(24.dp)
    var expanded by remember(goal.id) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(catStyle.bgStart, catStyle.bgMid, catStyle.bgEnd)))
            .border(BorderStroke(1.dp, catStyle.borderColor), shape)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .clickable { expanded = !expanded }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-24).dp)
                .size(84.dp)
                .clip(CircleShape)
                .background(
                    if (isDark) Color.White.copy(alpha = 0.09f)
                    else catStyle.buttonBg.copy(alpha = 0.15f)
                )
        )
        Icon(
            imageVector = SplashStrings.iconFor(goal.iconName),
            contentDescription = null,
            tint = if (isDark) Color.White.copy(alpha = 0.14f) else catStyle.buttonBg.copy(alpha = 0.17f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 52.dp)
                .size(64.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = catStyle.buttonBg
                    ) {
                        Text(
                            text = SplashStrings.t(state.lang, "Extra · 0 XP", "Extra · 0 XP"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = catStyle.buttonInk,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = catStyle.chipBg
                    ) {
                        Text(
                            text = goal.displaySub(state.lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = catStyle.onCard,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = catStyle.onCard.copy(alpha = 0.72f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = SplashStrings.t(state.lang, "Cerrar", "Dismiss"),
                            tint = catStyle.onCard.copy(alpha = 0.72f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Text(
                text = goal.displayShort(state.lang),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp),
                fontWeight = FontWeight.ExtraBold,
                color = catStyle.onCard
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = goal.displayTitle(state.lang),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 20.sp),
                    fontWeight = FontWeight.Medium,
                    color = catStyle.onCard.copy(alpha = 0.96f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onRerollExtra,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = catStyle.chipBg,
                        contentColor = catStyle.onCard
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(SplashStrings.t(state.lang, "Otro", "Another"), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onComplete,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = catStyle.buttonBg,
                        contentColor = catStyle.buttonInk
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(SplashStrings.t(state.lang, "Completar", "Complete"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CompletedDailyBanner(
    state: SplashUiState,
    title: String,
    subtitle: String,
    onDoExtra: () -> Unit,
    onOpenCatalog: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF3E7D28), Color(0xFF5C9E3A))
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.90f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDoExtra,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F4F14)
                    )
                ) {
                    Text(SplashStrings.t(state.lang, "Hacer uno extra", "Do an extra one"))
                }
                OutlinedButton(
                    onClick = onOpenCatalog,
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(SplashStrings.t(state.lang, "Catálogo", "Catalog"))
                }
            }
        }
    }
}

@Composable
private fun WeeklyGoalCard(
    state: SplashUiState,
    goal: GoalItem,
    onPhoto: () -> Unit,
    onComplete: () -> Unit
) {
    val isDark = GoalCardPalettes.isSplashDarkTheme()
    val catStyle = GoalCardPalettes.categoryCardStyle(GoalCategory.LARGO, isDark)
    val shape = RoundedCornerShape(24.dp)
    var expanded by remember(goal.id) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(catStyle.bgStart, catStyle.bgMid, catStyle.bgEnd)))
            .border(BorderStroke(1.dp, catStyle.borderColor), shape)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .clickable { expanded = !expanded }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-24).dp)
                .size(84.dp)
                .clip(CircleShape)
                .background(
                    if (isDark) Color.White.copy(alpha = 0.09f)
                    else catStyle.buttonBg.copy(alpha = 0.15f)
                )
        )
        Icon(
            imageVector = SplashStrings.iconFor(goal.iconName),
            contentDescription = null,
            tint = if (isDark) Color.White.copy(alpha = 0.14f) else catStyle.buttonBg.copy(alpha = 0.17f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 52.dp)
                .size(64.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = catStyle.chipBg
                ) {
                    Text(
                        text = goal.displaySub(state.lang),
                        style = MaterialTheme.typography.labelMedium,
                        color = catStyle.onCard,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "+8 pts",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = catStyle.onCard
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = catStyle.onCard.copy(alpha = 0.72f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = goal.displayShort(state.lang),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp),
                fontWeight = FontWeight.ExtraBold,
                color = catStyle.onCard
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = goal.displayTitle(state.lang),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 20.sp),
                    fontWeight = FontWeight.Medium,
                    color = catStyle.onCard.copy(alpha = 0.96f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onPhoto,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = catStyle.chipBg,
                        contentColor = catStyle.onCard
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(SplashStrings.t(state.lang, "Foto", "Photo"), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onComplete,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = catStyle.buttonBg,
                        contentColor = catStyle.buttonInk
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(SplashStrings.t(state.lang, "Completar", "Complete"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
