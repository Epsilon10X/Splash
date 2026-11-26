package com.splash.offline.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splash.offline.R
import com.splash.offline.data.AppLanguage
import com.splash.offline.data.BiomeCelebrationEvent
import com.splash.offline.data.GoalCategory
import com.splash.offline.ui.theme.GoalCardPalettes
import com.splash.offline.data.GoalItem
import com.splash.offline.data.SplashCatalogData
import com.splash.offline.data.SplashStrings
import com.splash.offline.data.SplashUiState
import com.splash.offline.data.TimeBucket
import com.splash.offline.ui.components.BiomeDioramaCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
    onDismissNotifiedGoal: () -> Unit,
    onStartGoal: (GoalItem, Boolean, Boolean, Boolean) -> Unit,
    onPhotoGoal: (GoalItem, Boolean, Boolean) -> Unit = { _, _, _ -> },
    onClearCelebration: (Long) -> Unit = {},
    onOpenCatalog: () -> Unit,
    onOpenBiomes: () -> Unit
) {
    val maxBiomeIdx = if (state.betaMode) SplashCatalogData.BIOMES.lastIndex else state.unlockedBiomeCount - 1
    val canPrev = state.activeBiomeIndex > 0
    val canNext = state.activeBiomeIndex < maxBiomeIdx
    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
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

        // Isolated 3D Biome Hero Stage Card (prevents full-screen recomposition during 120Hz animations)
        BiomeHeroStageCard(
            state = state,
            canPrev = canPrev,
            canNext = canNext,
            onOpen3D = onOpen3D,
            onSwitchBiome = onSwitchBiome,
            onRequestScrollTop = {
                if (scrollState.value > 0) {
                    scrollState.animateScrollTo(0)
                }
            },
            onClearCelebration = onClearCelebration
        )

        val isDark = GoalCardPalettes.isSplashDarkTheme()
        val notifiedGoal = state.notifiedGoalId?.let { nid ->
            state.catalog.firstOrNull { it.id == nid && !it.isHidden }
        }
        AnimatedVisibility(
            visible = notifiedGoal != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            notifiedGoal?.let { goal ->
                NotifiedGoalCard(
                    state = state,
                    goal = goal,
                    isDark = isDark,
                    onDismiss = onDismissNotifiedGoal,
                    onComplete = { onStartGoal(goal, goal.biomeExclusive != null, false, true) }
                )
            }
        }

        // Material 3 Expressive ToggleButton Segmented Switcher (Flash / 15 min / Semanal)
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
                label = "Flash",
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
                    "Flash (2 min) · Bioma hoy: ${state.quickUsedToday.coerceAtMost(2)}/2 (+2 pts)",
                    "Flash (2 min) · Biome today: ${state.quickUsedToday.coerceAtMost(2)}/2 (+2 pts)"
                )
                TimeBucket.MEDIUM_15M -> SplashStrings.t(
                    state.lang,
                    "15 min · Bioma hoy: ${state.mediumUsedToday.coerceAtMost(1)}/1 (+3 pts)",
                    "15 min · Biome today: ${state.mediumUsedToday.coerceAtMost(1)}/1 (+3 pts)"
                )
                TimeBucket.WEEKLY -> SplashStrings.t(
                    state.lang,
                    "Semanal · Bioma esta semana: ${state.weeklyUsedThisWeek.coerceAtMost(4)}/4 (+4 pts)",
                    "Weekly · Biome this week: ${state.weeklyUsedThisWeek.coerceAtMost(4)}/4 (+4 pts)"
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
                                onComplete = { onStartGoal(extraGoal, false, true, false) }
                            )
                        } else {
                            CompletedDailyBanner(
                                state = state,
                                title = SplashStrings.t(state.lang, "Objetivos Flash Completados (2/2)", "Flash Goals Completed (2/2)"),
                                subtitle = SplashStrings.t(
                                    state.lang,
                                    "Ya completaste todos los Flash de hoy. Si haces objetivos extra cuentan para tu racha y Muro Real, pero NO suman puntos al bioma.",
                                    "You finished all Flash goals for today. Extra goals count for your streak and Real Wall, NOT for biome XP."
                                ),
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
                    if (state.mediumUsedToday >= 1) {
                        val extraGoal = state.extraMediumGoalId?.let { id -> state.catalog.firstOrNull { it.id == id } }
                        if (extraGoal != null) {
                            ExtraGoalCard(
                                state = state,
                                goal = extraGoal,
                                onRerollExtra = { onRequestExtraGoal(TimeBucket.MEDIUM_15M) },
                                onDismiss = { onDismissExtraGoal(TimeBucket.MEDIUM_15M) },
                                onComplete = { onStartGoal(extraGoal, false, true, false) }
                            )
                        } else {
                            CompletedDailyBanner(
                                state = state,
                                title = SplashStrings.t(state.lang, "Objetivo Medio Completado (1/1)", "Medium Goal Completed (1/1)"),
                                subtitle = SplashStrings.t(
                                    state.lang,
                                    "Ya completaste tu objetivo Medio de hoy. Si haces objetivos extra cuentan para tu racha y Muro Real, pero NO suman puntos al bioma.",
                                    "You finished today's Medium goal. Extra goals count for your streak and Real Wall, NOT for biome XP."
                                ),
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
                    if (state.weeklyUsedThisWeek >= 4) {
                        val extraGoal = state.extraWeeklyGoalId?.let { id -> state.catalog.firstOrNull { it.id == id } }
                        if (extraGoal != null) {
                            ExtraGoalCard(
                                state = state,
                                goal = extraGoal,
                                onRerollExtra = { onRequestExtraGoal(TimeBucket.WEEKLY) },
                                onDismiss = { onDismissExtraGoal(TimeBucket.WEEKLY) },
                                onComplete = { onStartGoal(extraGoal, false, true, false) }
                            )
                        } else {
                            CompletedDailyBanner(
                                state = state,
                                title = SplashStrings.t(state.lang, "Objetivos Semanales Completados (4/4)", "Weekly Goals Completed (4/4)"),
                                subtitle = SplashStrings.t(
                                    state.lang,
                                    "Ya completaste tus 4 objetivos semanales de esta semana. Si haces objetivos extra cuentan para tu racha y Muro Real, pero NO suman puntos al bioma.",
                                    "You finished your 4 weekly goals this week. Extra goals count for your streak and Real Wall, NOT for biome XP."
                                ),
                                onDoExtra = { onRequestExtraGoal(TimeBucket.WEEKLY) },
                                onOpenCatalog = onOpenCatalog
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.currentWeeklyIds.mapNotNull { id -> state.catalog.firstOrNull { it.id == id } }
                                .forEach { goal ->
                                    WeeklyGoalCard(
                                        state = state,
                                        goal = goal,
                                        onPhoto = { onPhotoGoal(goal, false, false) },
                                        onComplete = { onStartGoal(goal, false, false, false) }
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
    onStartGoal: (GoalItem, Boolean, Boolean, Boolean) -> Unit
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
                    .height(214.dp)
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
                            if (!isBackFace) {
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
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                if (isBackFace) {
                                    // Back Face (full readable description, scrollable without clipping/overlap)
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                    ) {
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
                                                fontSize = 13.sp,
                                                lineHeight = 17.5.sp
                                            ),
                                            fontWeight = FontWeight.Medium,
                                            color = biomeStyle.ink.copy(alpha = 0.96f)
                                        )
                                    }
                                } else {
                                    // Front Face (Quick Biome & Medium Biome)
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
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
                                                            "Flash"
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
                                    onClick = { onStartGoal(bg, true, false, false) },
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
                    .height(214.dp)
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
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

                        // Prototype D (Medium General): Sideways accordion reveals full description without clipping
                        AnimatedVisibility(
                            visible = !isQuick && mediumGenExpanded,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = gg.displayTitle(state.lang),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 17.5.sp
                                ),
                                fontWeight = FontWeight.Medium,
                                color = catStyle.onCard.copy(alpha = 0.96f)
                            )
                        }
                    }

                    Button(
                        onClick = { onStartGoal(gg, false, false, false) },
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
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
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
                                            fontSize = 13.sp,
                                            lineHeight = 17.5.sp
                                        ),
                                        fontWeight = FontWeight.SemiBold,
                                        color = catStyle.onCard.copy(alpha = 0.96f)
                                    )
                                }

                                Button(
                                    onClick = { onStartGoal(gg, false, false, false) },
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
                            text = SplashStrings.t(state.lang, "Extra · +0 pts bioma", "Extra · +0 biome pts"),
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
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.22f)
            ) {
                Text(
                    text = SplashStrings.t(state.lang, "AVISO · LOS EXTRA NO SUMAN AL BIOMA", "NOTE · EXTRA GOALS DON'T ADD BIOME XP"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
                        text = "+4 pts",
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

            Button(
                onClick = onComplete,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = catStyle.buttonBg,
                    contentColor = catStyle.buttonInk
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(SplashStrings.t(state.lang, "Completar", "Complete"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NotifiedGoalCard(
    state: SplashUiState,
    goal: GoalItem,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val catStyle = GoalCardPalettes.categoryCardStyle(goal.category, isDark)
    val shape = RoundedCornerShape(24.dp)
    val canEarnSurpriseXp = state.surpriseUsedToday < 5
    val ptsLabel = when {
        !canEarnSurpriseXp -> SplashStrings.t(state.lang, "Muro Real (+0 pts)", "Real Wall (+0 pts)")
        goal.category == GoalCategory.MEDIO -> SplashStrings.t(state.lang, "+2 pts sorpresa", "+2 surprise pts")
        else -> SplashStrings.t(state.lang, "+1 pt sorpresa", "+1 surprise pt")
    }
    val catTag = if (goal.category == GoalCategory.MEDIO) "15 min · $ptsLabel" else "Flash · $ptsLabel"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(catStyle.bgStart, catStyle.bgMid, catStyle.bgEnd)))
            .border(BorderStroke(1.5.dp, catStyle.borderColor), shape)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = catStyle.onCard,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = SplashStrings.t(
                                state.lang,
                                "De tu notificación · $catTag",
                                "From notification · $catTag"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = catStyle.onCard
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = SplashStrings.t(state.lang, "Descartar", "Dismiss"),
                        tint = catStyle.onCard,
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
            Text(
                text = goal.displayTitle(state.lang),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                color = catStyle.onCard.copy(alpha = 0.94f)
            )
            Button(
                onClick = onComplete,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = catStyle.buttonBg,
                    contentColor = catStyle.buttonInk
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(SplashStrings.t(state.lang, "Completar objetivo", "Complete goal"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BiomeHeroStageCard(
    state: SplashUiState,
    canPrev: Boolean,
    canNext: Boolean,
    onOpen3D: () -> Unit,
    onSwitchBiome: (Int) -> Unit,
    onRequestScrollTop: suspend () -> Unit,
    onClearCelebration: (Long) -> Unit
) {
    val biome = state.activeBiome
    val stage = state.effectiveBiomeLevel
    val celebrationProgress = remember { Animatable(1f) }
    var activeCelebration by remember { mutableStateOf<BiomeCelebrationEvent?>(null) }

    LaunchedEffect(state.biomeCelebration?.id) {
        val ev = state.biomeCelebration ?: return@LaunchedEffect
        activeCelebration = ev
        onRequestScrollTop()
        val durationMs = when {
            ev.biomeUnlocked && ev.nextBiomeId != null -> 4600
            ev.biomeUnlocked -> 3400
            ev.category == GoalCategory.RAPIDO -> 1800
            ev.category == GoalCategory.MEDIO -> 2400
            else -> 3000
        }
        celebrationProgress.snapTo(0f)
        celebrationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
        )
        activeCelebration = null
        onClearCelebration(ev.id)
    }

    val ev = activeCelebration
    val isSlideUnlock = ev != null && ev.biomeUnlocked && ev.nextBiomeId != null
    val p = if (ev != null) celebrationProgress.value.coerceIn(0f, 1f) else 1f
    val smooth = p * p * (3f - 2f * p)

    val slideProgress = if (isSlideUnlock && p >= 0.76f) {
        FastOutSlowInEasing.transform(((p - 0.76f) / 0.24f).coerceIn(0f, 1f))
    } else 0f

    val celebrationLocalP = if (isSlideUnlock) {
        (p / 0.60f).coerceIn(0f, 1f)
    } else {
        smooth
    }

    val xpInterpT = if (ev != null) {
        val endT = if (isSlideUnlock) 0.36f else 0.58f
        val raw = ((p - 0.08f) / endT).coerceIn(0f, 1f)
        raw * raw * (3f - 2f * raw)
    } else 1f

    val animatedXp = if (ev != null) {
        if (isSlideUnlock && slideProgress >= 0.45f) 0
        else (ev.prevXp + (ev.nextXp - ev.prevXp) * xpInterpT).roundToInt()
    } else state.activeBiomeXp

    val animatedStage = if (ev != null && isSlideUnlock && slideProgress >= 0.45f) 1
    else ((animatedXp * 7) / SplashCatalogData.MAX_BIOME_XP + 1).coerceIn(1, 8)
    val displayStage = if (state.betaStageOverride != null) stage else animatedStage
    val xpFraction = (animatedXp / 100f).coerceIn(0f, 1f)

    val sproutProgress = if (ev != null) {
        val span = if (isSlideUnlock) 0.48f else 0.72f
        ((p - 0.10f) / span).coerceIn(0f, 1f)
    } else 1f

    val displayBiomeId = ev?.completedBiomeId ?: biome.id
    val displayBiomeName = if (isSlideUnlock && slideProgress >= 0.45f) {
        ev?.nextBiomeName ?: biome.name(state.lang)
    } else {
        ev?.completedBiomeName ?: biome.name(state.lang)
    }

    val canvasPoints = when {
        state.betaStageOverride != null -> null
        ev != null -> if (p < 0.10f) ev.prevXp else ev.nextXp
        else -> state.activeBiomeXp
    }
    val canvasPrevPoints = ev?.let { if (p >= 0.10f) it.prevXp else null }

    val celebBell = sin(celebrationLocalP * PI).toFloat().coerceIn(0f, 1f)
    val holdFade = if (isSlideUnlock && p > 0.58f) (1f - ((p - 0.58f) / 0.14f)).coerceIn(0f, 1f) else celebBell
    val cameraYaw = when {
        ev == null -> 45f
        ev.biomeUnlocked -> 45f + 54f * sin(celebrationLocalP * PI).toFloat().coerceIn(0f, 1f)
        ev.category == GoalCategory.RAPIDO -> 45f + 9f * celebBell
        ev.category == GoalCategory.MEDIO -> 45f + 20f * celebBell
        else -> 45f + 36f * celebBell
    }
    val cameraZoom = when {
        ev == null -> 1.04f
        ev.biomeUnlocked -> 1.04f + 0.14f * holdFade
        ev.category == GoalCategory.RAPIDO -> 1.04f + 0.08f * celebBell
        ev.category == GoalCategory.MEDIO -> 1.04f + 0.11f * celebBell
        else -> 1.04f + 0.14f * celebBell
    }
    val celebrationAccent = when {
        ev == null -> MaterialTheme.colorScheme.primary
        ev.biomeUnlocked -> Color(0xFFF59E0B)
        ev.category == GoalCategory.RAPIDO -> Color(0xFF22C55E)
        ev.category == GoalCategory.MEDIO -> Color(0xFF14B8A6)
        else -> Color(0xFFF59E0B)
    }

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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 52.dp)
                    .clipToBounds()
            ) {
                val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
                BiomeDioramaCanvas(
                    biomeId = displayBiomeId,
                    stage = if (ev != null && ev.biomeUnlocked) 8 else displayStage,
                    yawDeg = cameraYaw,
                    zoom = cameraZoom,
                    points = canvasPoints,
                    prevPoints = canvasPrevPoints,
                    sproutProgress = sproutProgress,
                    sproutAccentColor = celebrationAccent,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = -widthPx * slideProgress
                        }
                )

                val nextId = ev?.nextBiomeId
                if (isSlideUnlock && slideProgress > 0f && nextId != null) {
                    BiomeDioramaCanvas(
                        biomeId = nextId,
                        stage = 1,
                        yawDeg = 45f,
                        zoom = 1.04f,
                        points = 0,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = widthPx * (1f - slideProgress)
                            }
                    )
                }
            }

            ev?.let { activeEv ->
                BiomeCelebrationOverlay(
                    event = activeEv,
                    progress = p,
                    slideProgress = slideProgress,
                    lang = state.lang,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 52.dp)
                )
            }

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
                                enabled = canPrev && ev == null,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
                            }
                            Text(
                                text = displayBiomeName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(
                                onClick = { onSwitchBiome(state.activeBiomeIndex + 1) },
                                enabled = canNext && ev == null,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                            }
                        }

                        val chipScale = if (ev != null) 1f + 0.09f * holdFade else 1f
                        Surface(
                            shape = CircleShape,
                            color = if (ev != null) celebrationAccent.copy(alpha = 0.24f) else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.graphicsLayer {
                                scaleX = chipScale
                                scaleY = chipScale
                            }
                        ) {
                            val lvlPrefix = SplashStrings.t(state.lang, "Nv.", "Lvl.")
                            val mood = SplashStrings.t(state.lang, "En calma", "Serene")
                            Text(
                                text = "$lvlPrefix $displayStage · ${animatedXp}% · $mood",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (ev != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer,
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
}

@Composable
private fun BiomeCelebrationOverlay(
    event: BiomeCelebrationEvent,
    progress: Float,
    slideProgress: Float,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val rawP = progress.coerceIn(0f, 1f)
    val isUnlock = event.biomeUnlocked
    val effectP = if (isUnlock && event.nextBiomeId != null) {
        (rawP / 0.72f).coerceIn(0f, 1f)
    } else {
        rawP
    }
    val smooth = effectP * effectP * (3f - 2f * effectP)
    val env = sin(smooth * PI).toFloat().coerceIn(0f, 1f)
    val unlockHoldAlpha = if (isUnlock) {
        when {
            rawP < 0.10f -> rawP / 0.10f
            rawP > 0.90f -> (1f - rawP) / 0.10f
            else -> 1f
        }.coerceIn(0f, 1f)
    } else {
        when {
            rawP < 0.14f -> rawP / 0.14f
            rawP > 0.78f -> (1f - rawP) / 0.22f
            else -> 1f
        }.coerceIn(0f, 1f)
    }
    val pillScale = if (rawP < 0.18f) 0.80f + 0.24f * (rawP / 0.18f) else 1f + 0.03f * env

    val primaryColor = when {
        isUnlock && slideProgress >= 0.45f -> Color(0xFF10B981)
        isUnlock -> Color(0xFFF59E0B)
        event.category == GoalCategory.RAPIDO -> Color(0xFF22C55E)
        event.category == GoalCategory.MEDIO -> Color(0xFF14B8A6)
        else -> Color(0xFFF59E0B)
    }
    val secondaryColor = when {
        isUnlock -> Color(0xFFFDE047)
        event.category == GoalCategory.RAPIDO -> Color(0xFFFACC15)
        event.category == GoalCategory.MEDIO -> Color(0xFFFB923C)
        else -> Color(0xFFFB7185)
    }
    val labelText = when {
        isUnlock && slideProgress >= 0.45f && event.nextBiomeName != null ->
            SplashStrings.t(lang, "Nuevo bioma · ${event.nextBiomeName}", "New biome · ${event.nextBiomeName}")
        isUnlock ->
            SplashStrings.t(lang, "Bioma completado · 100%", "Biome completed · 100%")
        event.category == GoalCategory.RAPIDO -> if (event.isSurprise) {
            SplashStrings.t(lang, "+${event.earnedXp}% · Sorpresa Flash", "+${event.earnedXp}% · Surprise Flash")
        } else {
            SplashStrings.t(lang, "+${event.earnedXp}% · Flash", "+${event.earnedXp}% · Flash")
        }
        event.category == GoalCategory.MEDIO ->
            SplashStrings.t(lang, "+${event.earnedXp}% · Enfoque · Tu bioma crece", "+${event.earnedXp}% · Focus · Biome growing")
        else ->
            SplashStrings.t(lang, "+${event.earnedXp}% · Gran Hito Semanal", "+${event.earnedXp}% · Weekly Milestone")
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.50f
            val minDim = minOf(w, h)

            when {
                isUnlock -> {
                    val beamFade = if (slideProgress > 0f) (1f - slideProgress).coerceIn(0f, 1f) else env
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFEF08A).copy(alpha = 0.48f * beamFade),
                                primaryColor.copy(alpha = 0.30f * beamFade),
                                Color(0xFF34D399).copy(alpha = 0.14f * beamFade),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = minDim * 0.60f
                        ),
                        radius = minDim * 0.60f,
                        center = Offset(cx, cy)
                    )
                    for (b in 0 until 14) {
                        val angle = b * (2.0 * PI / 14.0) + smooth * 1.6
                        val start = Offset(cx + (cos(angle) * minDim * 0.06f).toFloat(), cy + (sin(angle) * minDim * 0.06f).toFloat())
                        val end = Offset(cx + (cos(angle) * minDim * 0.54f).toFloat(), cy + (sin(angle) * minDim * 0.54f).toFloat())
                        drawLine(
                            color = Color(0xFFFDE047).copy(alpha = 0.30f * beamFade),
                            start = start,
                            end = end,
                            strokeWidth = 7f * beamFade + 1f,
                            cap = StrokeCap.Round
                        )
                    }
                    for (wave in 0..3) {
                        val wp = ((smooth - wave * 0.12f) / 0.64f).coerceIn(0f, 1f)
                        if (wp > 0f && wp < 1f) {
                            val wc = if (wave % 2 == 0) Color(0xFFFDE047) else Color(0xFF34D399)
                            drawCircle(
                                color = wc.copy(alpha = 0.72f * (1f - wp) * beamFade),
                                radius = minDim * (0.05f + wp * 0.56f),
                                center = Offset(cx, cy),
                                style = Stroke(width = 7f * (1f - wp * 0.4f))
                            )
                        }
                    }
                    for (i in 0 until 44) {
                        val theta = (i * (2.0 * PI / 44.0)) + smooth * 3.4
                        val radial = minDim * (0.08f + smooth * (0.38f + (i % 5) * 0.04f))
                        val px = cx + (cos(theta) * radial).toFloat()
                        val py = cy + (sin(theta) * radial * 0.56f).toFloat() - smooth * (76f + (i % 6) * 16f)
                        val c = when (i % 3) {
                            0 -> Color(0xFFFDE047)
                            1 -> Color(0xFF34D399)
                            else -> Color(0xFFFB7185)
                        }
                        drawCircle(
                            color = c.copy(alpha = 0.94f * beamFade),
                            radius = (5.5f + (i % 4) * 2f) * beamFade + 1.5f,
                            center = Offset(px, py)
                        )
                    }
                }

                event.category == GoalCategory.RAPIDO -> {
                    val r = minDim * (0.10f + smooth * 0.44f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                secondaryColor.copy(alpha = 0.28f * env),
                                primaryColor.copy(alpha = 0.14f * env),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = minDim * 0.42f
                        ),
                        radius = minDim * 0.42f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.72f * (1f - smooth)),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 5f * (1f - smooth * 0.5f))
                    )
                    for (i in 0 until 14) {
                        val angle = -PI / 2.0 + ((i - 6.5) * 0.24)
                        val dist = minDim * (0.08f + smooth * (0.28f + (i % 3) * 0.08f))
                        val px = cx + (cos(angle) * dist * 0.85).toFloat()
                        val py = cy + (sin(angle) * dist * 1.15).toFloat() - smooth * 26f
                        val c = if (i % 2 == 0) secondaryColor else primaryColor
                        drawCircle(
                            color = c.copy(alpha = 0.90f * env),
                            radius = (4.5f + (i % 3) * 2f) * env + 1f,
                            center = Offset(px, py)
                        )
                    }
                }

                event.category == GoalCategory.MEDIO -> {
                    for (wave in 0..1) {
                        val wp = ((smooth - wave * 0.18f) / 0.82f).coerceIn(0f, 1f)
                        if (wp > 0f && wp < 1f) {
                            drawCircle(
                                color = (if (wave == 0) primaryColor else secondaryColor).copy(alpha = 0.65f * (1f - wp)),
                                radius = minDim * (0.08f + wp * 0.48f),
                                center = Offset(cx, cy),
                                style = Stroke(width = 6f * (1f - wp * 0.5f))
                            )
                        }
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.30f * env),
                                secondaryColor.copy(alpha = 0.14f * env),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = minDim * 0.48f
                        ),
                        radius = minDim * 0.48f,
                        center = Offset(cx, cy)
                    )
                    for (i in 0 until 22) {
                        val theta = (i * (2.0 * PI / 22.0)) + smooth * 2.2
                        val radial = minDim * (0.12f + smooth * 0.34f + (i % 4) * 0.03f)
                        val px = cx + (cos(theta) * radial).toFloat()
                        val py = cy + (sin(theta) * radial * 0.52f).toFloat() - smooth * (55f + (i % 5) * 14f)
                        val c = if (i % 3 == 0) secondaryColor else primaryColor
                        drawCircle(
                            color = c.copy(alpha = 0.88f * env),
                            radius = (5f + (i % 3) * 2.2f) * env + 1.2f,
                            center = Offset(px, py)
                        )
                    }
                }

                else -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFDE047).copy(alpha = 0.42f * env),
                                primaryColor.copy(alpha = 0.28f * env),
                                secondaryColor.copy(alpha = 0.12f * env),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = minDim * 0.56f
                        ),
                        radius = minDim * 0.56f,
                        center = Offset(cx, cy)
                    )
                    for (b in 0 until 10) {
                        val angle = b * (2.0 * PI / 10.0) + smooth * 1.4
                        val start = Offset(cx + (cos(angle) * minDim * 0.08f).toFloat(), cy + (sin(angle) * minDim * 0.08f).toFloat())
                        val end = Offset(cx + (cos(angle) * minDim * 0.50f).toFloat(), cy + (sin(angle) * minDim * 0.50f).toFloat())
                        drawLine(
                            color = Color(0xFFFDE047).copy(alpha = 0.28f * env),
                            start = start,
                            end = end,
                            strokeWidth = 7f * env,
                            cap = StrokeCap.Round
                        )
                    }
                    for (wave in 0..2) {
                        val wp = ((smooth - wave * 0.14f) / 0.72f).coerceIn(0f, 1f)
                        if (wp > 0f && wp < 1f) {
                            val wc = when (wave) {
                                0 -> Color(0xFFFDE047)
                                1 -> primaryColor
                                else -> secondaryColor
                            }
                            drawCircle(
                                color = wc.copy(alpha = 0.72f * (1f - wp)),
                                radius = minDim * (0.06f + wp * 0.52f),
                                center = Offset(cx, cy),
                                style = Stroke(width = 7f * (1f - wp * 0.4f))
                            )
                        }
                    }
                    for (i in 0 until 34) {
                        val theta = (i * (2.0 * PI / 34.0)) + smooth * 3.1
                        val radial = minDim * (0.08f + smooth * (0.36f + (i % 5) * 0.04f))
                        val px = cx + (cos(theta) * radial).toFloat()
                        val py = cy + (sin(theta) * radial * 0.58f).toFloat() - smooth * (72f + (i % 6) * 16f)
                        val c = when (i % 3) {
                            0 -> Color(0xFFFDE047)
                            1 -> primaryColor
                            else -> secondaryColor
                        }
                        drawCircle(
                            color = c.copy(alpha = 0.94f * env),
                            radius = (5.5f + (i % 4) * 2f) * env + 1.5f,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }

        Surface(
            shape = CircleShape,
            color = primaryColor,
            contentColor = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
                .graphicsLayer {
                    alpha = unlockHoldAlpha
                    scaleX = pillScale
                    scaleY = pillScale
                    translationY = if (isUnlock) 0f else -10f * smooth
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
