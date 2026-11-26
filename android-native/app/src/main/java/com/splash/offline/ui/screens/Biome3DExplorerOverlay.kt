package com.splash.offline.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.splash.offline.data.AppLanguage
import com.splash.offline.data.SplashCatalogData
import com.splash.offline.data.SplashStrings
import com.splash.offline.data.SplashUiState
import com.splash.offline.ui.components.BiomeDioramaCanvas

@Composable
fun Biome3DExplorerOverlay(
    state: SplashUiState,
    onClose: () -> Unit,
    onSelectBiome: (Int) -> Unit,
    onSelectBetaStage: (Int?) -> Unit,
    onGrantBetaXp: (Int) -> Unit,
    onToggleNight: () -> Unit,
    onToggleAudio: () -> Unit
) {
    var yaw by remember(state.activeBiome.id) { mutableFloatStateOf(45f) }
    var zoom by remember { mutableFloatStateOf(1.04f) }
    val entranceScale = remember { Animatable(0.88f) }
    LaunchedEffect(state.activeBiome.id) {
        entranceScale.snapTo(0.88f)
        entranceScale.animateTo(
            1.0f,
            spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    val biome = state.activeBiome
    val stage = state.effectiveBiomeLevel
    val stageDef = SplashCatalogData.STAGES.getOrElse(stage - 1) { SplashCatalogData.STAGES.first() }
    val bgTop = if (state.dioramaNightPreview) Color(0xFF05080F) else MaterialTheme.colorScheme.background
    val bgBot = if (state.dioramaNightPreview) Color(0xFF0C1422) else MaterialTheme.colorScheme.surfaceContainerLow

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBot)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Fullscreen 3D interactive canvas
        BiomeDioramaCanvas(
            biomeId = biome.id,
            stage = stage,
            yawDeg = yaw,
            zoom = zoom * entranceScale.value,
            nightMode = state.dioramaNightPreview,
            interactive = true,
            points = if (state.betaStageOverride != null) null else state.activeBiomeXp,
            onRotateDelta = { delta -> yaw = (yaw + delta) % 360f },
            onZoomDelta = { factor -> zoom = (zoom * factor).coerceIn(0.65f, 2.20f) },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (state.betaMode) 150.dp else 40.dp)
        )

        // Top HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = biome.name(state.lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stageDef.title(state.lang)} · ${state.activeBiomeXp}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FilledTonalIconButton(
                onClick = onClose,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = SplashStrings.t(state.lang, "Cerrar", "Close")
                )
            }
        }

        // Right floating tools
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
        ) {
            FilledTonalIconButton(onClick = { zoom = (zoom + 0.16f).coerceAtMost(2.20f) }) {
                Icon(Icons.Rounded.Add, contentDescription = "Zoom in")
            }
            FilledTonalIconButton(onClick = { zoom = (zoom - 0.16f).coerceAtLeast(0.65f) }) {
                Icon(Icons.Rounded.Remove, contentDescription = "Zoom out")
            }
            FilledTonalIconButton(onClick = { yaw = 45f; zoom = 1.04f }) {
                Icon(Icons.Rounded.CenterFocusStrong, contentDescription = "Reset")
            }
            FilledTonalIconButton(onClick = onToggleNight) {
                Icon(
                    imageVector = if (state.dioramaNightPreview) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = "Day/Night"
                )
            }
            FilledTonalIconButton(onClick = onToggleAudio) {
                Icon(
                    imageVector = if (state.isAmbientAudioPlaying) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    contentDescription = "Audio"
                )
            }
        }

        // Rotation hint or Beta Lab panel
        if (state.betaMode) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        SplashCatalogData.BIOMES.forEachIndexed { idx, b ->
                            FilterChip(
                                selected = idx == state.activeBiomeIndex,
                                onClick = { onSelectBiome(idx) },
                                label = { Text(b.shortName(state.lang)) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = SplashStrings.t(state.lang, "Mejora", "Stage"),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        for (s in 1..8) {
                            FilterChip(
                                selected = state.betaStageOverride == s,
                                onClick = { onSelectBetaStage(s) },
                                label = { Text(s.toString()) }
                            )
                        }
                        FilterChip(
                            selected = state.betaStageOverride == null,
                            onClick = { onSelectBetaStage(null) },
                            label = { Text(SplashStrings.t(state.lang, "Real", "Real")) }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onGrantBetaXp(2) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text("+2 Flash")
                        }
                        Button(
                            onClick = { onGrantBetaXp(4) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text(SplashStrings.t(state.lang, "+4 Medio", "+4 Med"))
                        }
                        Button(
                            onClick = { onGrantBetaXp(8) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text(SplashStrings.t(state.lang, "+8 Largo", "+8 Long"))
                        }
                    }
                }
            }
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.86f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = SplashStrings.t(state.lang, "Desliza para girar 360° · Pellizca para ampliar", "Swipe to rotate 360° · Pinch to zoom"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
    }
}
