package com.splash.offline.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splash.offline.data.CatalogFilter
import com.splash.offline.data.GoalCategory
import com.splash.offline.data.GoalItem
import com.splash.offline.data.SplashCatalogData
import com.splash.offline.data.SplashStrings
import com.splash.offline.data.SplashUiState
import com.splash.offline.ui.theme.GoalCardPalettes

@Composable
fun CatalogScreen(
    state: SplashUiState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (CatalogFilter) -> Unit,
    onOpenCreate: () -> Unit,
    onDismissCreate: () -> Unit,
    onCreateGoal: (String, GoalCategory, String) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onStartGoal: (GoalItem) -> Unit
) {
    val activeBiomeId = state.activeBiome.id

    // Exact sorting: Rápidos -> Medios -> Semanales -> Bioma
    val filteredList = remember(state.catalog, state.catalogSearch, state.catalogFilter, activeBiomeId, state.lang) {
        val q = state.catalogSearch.trim().lowercase()
        state.catalog
            .filter { !it.isHidden }
            .filter { g ->
                val matchesSearch = q.isEmpty() ||
                    g.displayTitle(state.lang).lowercase().contains(q) ||
                    g.displaySub(state.lang).lowercase().contains(q)
                val matchesFilter = when (state.catalogFilter) {
                    CatalogFilter.ALL -> true
                    CatalogFilter.RAPIDO -> g.category == GoalCategory.RAPIDO && g.biomeExclusive == null
                    CatalogFilter.MEDIO -> g.category == GoalCategory.MEDIO && g.biomeExclusive == null
                    CatalogFilter.LARGO -> g.category == GoalCategory.LARGO && g.biomeExclusive == null
                    CatalogFilter.BIOME -> g.biomeExclusive != null
                }
                matchesSearch && matchesFilter
            }
            .sortedWith(
                compareBy<GoalItem>(
                    { if (it.biomeExclusive != null) 3 else when (it.category) {
                        GoalCategory.RAPIDO -> 0
                        GoalCategory.MEDIO -> 1
                        GoalCategory.LARGO -> 2
                    } },
                    { it.biomeExclusive != activeBiomeId },
                    { !it.isFavorite },
                    { it.id }
                )
            )
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = SplashStrings.t(state.lang, "Catálogo", "Catalog"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                val isDark = GoalCardPalettes.isSplashDarkTheme()
                val (createTargetBg, createTargetFg) = GoalCardPalettes.catalogFilterTonalColors(state.catalogFilter, isDark)
                val createBg by animateColorAsState(targetValue = createTargetBg, label = "createBtnBg")
                val createFg by animateColorAsState(targetValue = createTargetFg, label = "createBtnFg")
                FilledTonalButton(
                    onClick = onOpenCreate,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = createBg,
                        contentColor = createFg
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(SplashStrings.t(state.lang, "Crear propio", "Create custom"))
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.catalogSearch,
                onValueChange = onSearchChange,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = {
                    Text(SplashStrings.t(state.lang, "Buscar entre los 67+ objetivos...", "Search across 67+ goals..."))
                },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                val isDark = GoalCardPalettes.isSplashDarkTheme()
                val chips = listOf(
                    CatalogFilter.ALL to SplashStrings.t(state.lang, "Todos", "All"),
                    CatalogFilter.RAPIDO to SplashStrings.t(state.lang, "Rápidos", "Quick"),
                    CatalogFilter.MEDIO to SplashStrings.t(state.lang, "Medios", "Medium"),
                    CatalogFilter.LARGO to SplashStrings.t(state.lang, "Largos", "Long"),
                    CatalogFilter.BIOME to SplashStrings.t(state.lang, "Del bioma", "Biome")
                )
                chips.forEach { (filter, label) ->
                    val checkedBg = when (filter) {
                        CatalogFilter.ALL -> if (isDark) Color(0xFF66C494) else Color(0xFF1B5E3F)
                        CatalogFilter.RAPIDO -> GoalCardPalettes.expressiveTone(GoalCategory.RAPIDO, isDark).pillBg
                        CatalogFilter.MEDIO -> GoalCardPalettes.expressiveTone(GoalCategory.MEDIO, isDark).pillBg
                        CatalogFilter.LARGO -> GoalCardPalettes.expressiveTone(GoalCategory.LARGO, isDark).pillBg
                        CatalogFilter.BIOME -> GoalCardPalettes.biomeUnifiedTone(isDark).pillBg
                    }
                    val checkedFg = when (filter) {
                        CatalogFilter.ALL -> if (isDark) Color(0xFF073320) else Color.White
                        CatalogFilter.RAPIDO -> GoalCardPalettes.expressiveTone(GoalCategory.RAPIDO, isDark).pillFg
                        CatalogFilter.MEDIO -> GoalCardPalettes.expressiveTone(GoalCategory.MEDIO, isDark).pillFg
                        CatalogFilter.LARGO -> GoalCardPalettes.expressiveTone(GoalCategory.LARGO, isDark).pillFg
                        CatalogFilter.BIOME -> GoalCardPalettes.biomeUnifiedTone(isDark).pillFg
                    }
                    ToggleButton(
                        checked = state.catalogFilter == filter,
                        onCheckedChange = { onFilterChange(filter) },
                        colors = ToggleButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = checkedBg,
                            checkedContentColor = checkedFg
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }

        items(filteredList, key = { it.id }) { goal ->
            CatalogGoalRow(
                state = state,
                goal = goal,
                onToggleFavorite = { onToggleFavorite(goal.id) }
            )
        }
    }

    if (state.isCreateGoalOpen) {
        CreateCustomGoalSheet(
            state = state,
            onDismiss = onDismissCreate,
            onSave = onCreateGoal
        )
    }
}

@Composable
private fun CatalogGoalRow(
    state: SplashUiState,
    goal: GoalItem,
    onToggleFavorite: () -> Unit
) {
    val isDark = GoalCardPalettes.isSplashDarkTheme()
    val isBiomeGoal = goal.biomeExclusive != null
    val tone = if (isBiomeGoal) {
        GoalCardPalettes.biomeUnifiedTone(isDark)
    } else {
        GoalCardPalettes.expressiveTone(goal.category, isDark)
    }
    val contentColor = tone.onContainer
    val iconBadgeBg = tone.accent
    val iconBadgeFg = tone.onAccent

    val richGradient = if (isBiomeGoal) {
        val bs = GoalCardPalettes.biomePostcardStyle(goal.biomeExclusive ?: state.activeBiome.id, isDark)
        Brush.linearGradient(listOf(bs.bg1, bs.bg2))
    } else {
        val cs = GoalCardPalettes.categoryCardStyle(goal.category, isDark)
        Brush.linearGradient(listOf(cs.bgStart, cs.bgMid, cs.bgEnd))
    }

    val catLabel = when {
        goal.biomeExclusive != null -> {
            val b = SplashCatalogData.BIOMES.firstOrNull { it.id == goal.biomeExclusive }?.shortName(state.lang)
                ?: goal.biomeExclusive
            SplashStrings.t(state.lang, "Bioma $b", "$b Biome")
        }
        goal.category == GoalCategory.RAPIDO -> "2 min"
        goal.category == GoalCategory.MEDIO -> "15 min"
        else -> SplashStrings.t(state.lang, "Semanal", "Weekly")
    }

    var expanded by remember(goal.id) { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = contentColor
        ),
        border = BorderStroke(1.dp, iconBadgeBg.copy(alpha = 0.24f)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .clickable { expanded = !expanded }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(richGradient)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-24).dp)
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.08f)
                        else iconBadgeBg.copy(alpha = 0.14f)
                    )
            )
            Icon(
                imageVector = SplashStrings.iconFor(goal.iconName),
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.12f) else iconBadgeBg.copy(alpha = 0.16f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 56.dp)
                    .size(62.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 86.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconBadgeBg,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = SplashStrings.iconFor(goal.iconName),
                        contentDescription = null,
                        tint = iconBadgeFg,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = goal.displayShort(state.lang),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.5.sp,
                            lineHeight = 23.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor,
                        maxLines = if (expanded) 3 else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    AnimatedVisibility(visible = expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = goal.displayTitle(state.lang),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.5.sp,
                                    lineHeight = 20.sp
                                ),
                                fontWeight = FontWeight.Medium,
                                color = contentColor.copy(alpha = 0.96f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isBiomeGoal) {
                                    Surface(
                                        shape = CircleShape,
                                        color = iconBadgeBg.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = catLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = iconBadgeBg,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = goal.displaySub(state.lang),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = contentColor.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "${goal.displaySub(state.lang)} · $catLabel",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = contentColor.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                FilledTonalIconToggleButton(
                    checked = goal.isFavorite,
                    onCheckedChange = { onToggleFavorite() },
                    shapes = IconButtonDefaults.toggleableShapes(),
                    colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                        containerColor = contentColor.copy(alpha = 0.12f),
                        contentColor = contentColor,
                        checkedContainerColor = iconBadgeBg,
                        checkedContentColor = iconBadgeFg
                    )
                ) {
                    Icon(
                        imageVector = if (goal.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (goal.isFavorite) {
                            SplashStrings.t(state.lang, "Quitar de favoritos", "Remove from favorites")
                        } else {
                            SplashStrings.t(state.lang, "Añadir a favorito", "Add to favorites")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateCustomGoalSheet(
    state: SplashUiState,
    onDismiss: () -> Unit,
    onSave: (String, GoalCategory, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(GoalCategory.RAPIDO) }
    var areaEs by remember { mutableStateOf(SplashCatalogData.DEFAULT_AREAS_ES.first()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = SplashStrings.t(state.lang, "Crear objetivo propio", "Create custom goal"),
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(SplashStrings.t(state.lang, "¿Qué quieres hacer fuera de la pantalla?", "What do you want to do off-screen?")) },
                placeholder = { Text(SplashStrings.t(state.lang, "Ej. Regar las plantas del balcón", "e.g. Water balcony plants")) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = SplashStrings.t(state.lang, "Tiempo", "Duration"),
                style = MaterialTheme.typography.labelLarge
            )
            val isDark = GoalCardPalettes.isSplashDarkTheme()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    GoalCategory.RAPIDO to SplashStrings.t(state.lang, "Rápido", "Quick"),
                    GoalCategory.MEDIO to SplashStrings.t(state.lang, "Medio", "Medium"),
                    GoalCategory.LARGO to SplashStrings.t(state.lang, "Largo", "Long")
                ).forEach { (c, lbl) ->
                    val tone = GoalCardPalettes.expressiveTone(c, isDark)
                    ToggleButton(
                        checked = category == c,
                        onCheckedChange = { category = c },
                        colors = ToggleButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            checkedContainerColor = tone.pillBg,
                            checkedContentColor = tone.pillFg
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(lbl)
                    }
                }
            }

            Text(
                text = SplashStrings.t(state.lang, "Área", "Area"),
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                SplashCatalogData.DEFAULT_AREAS_ES.forEach { a ->
                    ToggleButton(
                        checked = areaEs == a,
                        onCheckedChange = { areaEs = a }
                    ) {
                        Text(SplashStrings.areaFor(state.lang, a), maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onSave(title, category, areaEs) },
                enabled = title.trim().isNotEmpty(),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(SplashStrings.t(state.lang, "Guardar en Catálogo", "Save to Catalog"))
            }
        }
    }
}
