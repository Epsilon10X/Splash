package com.splash.offline.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.splash.offline.data.BiomeDef
import com.splash.offline.data.BiomeLayoutMode
import com.splash.offline.data.SplashCatalogData
import com.splash.offline.data.SplashStrings
import com.splash.offline.data.SplashUiState
import com.splash.offline.ui.components.BiomeDioramaCanvas

@Composable
fun BiomesScreen(
    state: SplashUiState,
    onSelectLayout: (BiomeLayoutMode) -> Unit,
    onEquipBiome: (Int) -> Unit,
    onOpen3D: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
    ) {
        Text(
            text = SplashStrings.t(state.lang, "Tus Biomas", "Your Biomes"),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = SplashStrings.t(
                state.lang,
                "10 mundos apilados en orden. Toca uno para equiparlo o ábrelo en 3D.",
                "10 worlds stacked in order. Tap one to equip it or open it in 3D."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                BiomeLayoutMode.MINI to SplashStrings.t(state.lang, "Mini", "Mini"),
                BiomeLayoutMode.STACK to SplashStrings.t(state.lang, "Apilado", "Stack"),
                BiomeLayoutMode.LARGE to SplashStrings.t(state.lang, "Grande", "Large")
            ).forEach { (mode, lbl) ->
                ToggleButton(
                    checked = state.biomeLayoutMode == mode,
                    onCheckedChange = { onSelectLayout(mode) },
                    colors = ToggleButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(lbl, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        AnimatedContent(
            targetState = state.biomeLayoutMode,
            transitionSpec = {
                (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    scaleIn(initialScale = 0.96f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                    (fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                        scaleOut(targetScale = 0.96f, animationSpec = spring(stiffness = Spring.StiffnessMedium)))
            },
            modifier = Modifier.weight(1f),
            label = "biomeLayout"
        ) { layout ->
            when (layout) {
                BiomeLayoutMode.MINI -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 124.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(SplashCatalogData.BIOMES, key = { _, b -> b.id }) { idx, biome ->
                            val unlocked = state.betaMode || idx < state.unlockedBiomeCount
                            val isActive = idx == state.activeBiomeIndex
                            val xp = state.biomeXpById[biome.id] ?: 0
                            BiomeGridTile(
                                state = state,
                                biome = biome,
                                unlocked = unlocked,
                                isActive = isActive,
                                xp = xp,
                                onTap = { if (unlocked) onEquipBiome(idx) }
                            )
                        }
                    }
                }

                BiomeLayoutMode.STACK -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 124.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(SplashCatalogData.BIOMES, key = { _, b -> b.id }) { idx, biome ->
                            val unlocked = state.betaMode || idx < state.unlockedBiomeCount
                            val isActive = idx == state.activeBiomeIndex
                            val xp = state.biomeXpById[biome.id] ?: 0
                            BiomeStackCard(
                                state = state,
                                biome = biome,
                                unlocked = unlocked,
                                isActive = isActive,
                                xp = xp,
                                large = false,
                                onEquip = { onEquipBiome(idx) },
                                onOpen3D = { onOpen3D(idx) }
                            )
                        }
                    }
                }

                BiomeLayoutMode.LARGE -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 124.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(SplashCatalogData.BIOMES, key = { _, b -> b.id }) { idx, biome ->
                            val unlocked = state.betaMode || idx < state.unlockedBiomeCount
                            val isActive = idx == state.activeBiomeIndex
                            val xp = state.biomeXpById[biome.id] ?: 0
                            BiomeStackCard(
                                state = state,
                                biome = biome,
                                unlocked = unlocked,
                                isActive = isActive,
                                xp = xp,
                                large = true,
                                onEquip = { onEquipBiome(idx) },
                                onOpen3D = { onOpen3D(idx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BiomeGridTile(
    state: SplashUiState,
    biome: BiomeDef,
    unlocked: Boolean,
    isActive: Boolean,
    xp: Int,
    onTap: () -> Unit
) {
    val stage = ((xp / 10) + 1).coerceIn(1, 8)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (isActive) 3.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (unlocked) 1f else 0.56f)
            .clickable(enabled = unlocked, onClick = onTap)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
            ) {
                BiomeDioramaCanvas(
                    biomeId = biome.id,
                    stage = stage,
                    points = xp,
                    zoom = 0.98f,
                    modifier = Modifier.fillMaxSize()
                )
                if (!unlocked) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp).size(18.dp)
                        )
                    }
                }
            }
            Text(
                text = biome.shortName(state.lang),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            LinearWavyProgressIndicator(
                progress = { (xp / 80f).coerceIn(0.04f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BiomeStackCard(
    state: SplashUiState,
    biome: BiomeDef,
    unlocked: Boolean,
    isActive: Boolean,
    xp: Int,
    large: Boolean,
    onEquip: () -> Unit,
    onOpen3D: () -> Unit
) {
    val stage = ((xp / 10) + 1).coerceIn(1, 8)
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (isActive) 3.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (unlocked) 1f else 0.62f)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            if (large) {
                BiomeDioramaCanvas(
                    biomeId = biome.id,
                    stage = stage,
                    points = xp,
                    zoom = 0.96f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!large) {
                    BiomeDioramaCanvas(
                        biomeId = biome.id,
                        stage = stage,
                        points = xp,
                        zoom = 0.98f,
                        modifier = Modifier.size(78.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = biome.name(state.lang),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!unlocked) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    }
                    Text(
                        text = biome.subtitle(state.lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearWavyProgressIndicator(
                        progress = { (xp / 80f).coerceIn(0.05f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (unlocked) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isActive) {
                        FilledTonalButton(
                            onClick = onEquip,
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(SplashStrings.t(state.lang, "Equipado", "Equipped"))
                        }
                    } else {
                        Button(
                            onClick = onEquip,
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(SplashStrings.t(state.lang, "Equipar", "Equip"))
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpen3D,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(SplashStrings.t(state.lang, "Explorar 3D", "3D View"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
