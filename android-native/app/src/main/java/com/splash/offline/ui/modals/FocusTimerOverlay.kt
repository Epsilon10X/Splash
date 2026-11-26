package com.splash.offline.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splash.offline.data.ActiveSession
import com.splash.offline.data.AppLanguage
import com.splash.offline.data.SplashStrings
import com.splash.offline.data.VerificationType
import kotlinx.coroutines.delay

@Composable
fun FocusTimerOverlay(
    session: ActiveSession,
    lang: AppLanguage,
    isAudioPlaying: Boolean,
    onToggleRunning: () -> Unit,
    onResetTimer: () -> Unit,
    onTick: () -> Unit,
    onToggleAudio: () -> Unit,
    onComplete: () -> Unit,
    onClose: () -> Unit
) {
    LaunchedEffect(session.sessionKey, session.isRunning) {
        while (session.isRunning) {
            onTick()
            delay(250L)
        }
    }

    val rem = session.remainingSeconds.coerceAtLeast(0)
    val mins = rem / 60
    val secs = rem % 60
    val clockText = String.format("%02d:%02d", mins, secs)
    val progress = (rem.toFloat() / session.totalSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val isAnalog = session.goal.verification == VerificationType.PUENTE_ANALOGICO
    val hasPartialProgress = rem < session.totalSeconds || session.isFinished

    val statusText = when {
        session.isFinished -> SplashStrings.t(lang, "Tiempo completado", "Time completed")
        session.isRunning -> SplashStrings.t(lang, "En curso · puedes apagar la pantalla", "Running · screen can turn off")
        session.pausedByAppExit -> SplashStrings.t(lang, "Pausado al salir · progreso guardado", "Paused on exit · progress saved")
        hasPartialProgress -> SplashStrings.t(lang, "En pausa · progreso guardado", "Paused · progress saved")
        else -> SplashStrings.t(lang, "Listo · pulsa Activar", "Ready · tap Start")
    }

    val startButtonLabel = when {
        session.isRunning -> SplashStrings.t(lang, "Pausar temporizador", "Pause timer")
        hasPartialProgress -> SplashStrings.t(lang, "Reanudar temporizador", "Resume timer")
        else -> SplashStrings.t(lang, "Activar temporizador", "Start timer")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        FilledTonalIconButton(
            onClick = onClose,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = Color(0xFF181B20),
                contentColor = Color(0xFFF2F1EE)
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(46.dp)
        ) {
            Icon(Icons.Rounded.Close, contentDescription = SplashStrings.t(lang, "Cerrar", "Close"))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF234717)
            ) {
                Text(
                    text = if (isAnalog) {
                        SplashStrings.t(lang, "Puente Analógico", "Offline Bridge")
                    } else {
                        SplashStrings.t(lang, "Modo OLED · Sin Pantalla", "OLED Mode · Screen Free")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFE2F4C8),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isAnalog) {
                    SplashStrings.t(lang, "Deja el móvil boca abajo", "Place your phone face down")
                } else {
                    session.goal.displayTitle(lang)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            if (isAnalog) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = session.goal.displayTitle(lang),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFA7AEA9),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(28.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(236.dp)
            ) {
                CircularWavyProgressIndicator(
                    progress = { progress },
                    color = if (session.isFinished) Color(0xFF66E39C) else Color(0xFF9BE06B),
                    trackColor = Color(0xFF1E232A),
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 18.dp)
                ) {
                    Text(
                        text = clockText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (session.pausedByAppExit) Color(0xFFF6C46D) else Color(0xFFA7AEA9),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            // Explicit Start / Pause / Resume Timer button
            if (!session.isFinished) {
                Button(
                    onClick = onToggleRunning,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (session.isRunning) Color(0xFF1D242C) else Color(0xFF9BE06B),
                        contentColor = if (session.isRunning) Color.White else Color(0xFF143008)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = if (session.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = startButtonLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasPartialProgress) {
                    FilledTonalIconButton(
                        onClick = onResetTimer,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFF181B20),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = SplashStrings.t(lang, "Reiniciar", "Reset")
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onToggleAudio,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isAudioPlaying) Color(0xFF1E3616) else Color(0xFF181B20),
                        contentColor = if (isAudioPlaying) Color(0xFFE2F4C8) else Color.White
                    ),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(SplashStrings.t(lang, "Sonido suave", "Soft sound"))
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onComplete,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (session.isFinished) Color(0xFF9BE06B) else Color(0xFF233224),
                    contentColor = if (session.isFinished) Color(0xFF143008) else Color(0xFFE2F4C8)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = if (isAnalog) {
                        SplashStrings.t(lang, "Ya terminé este reto", "I finished this challenge")
                    } else {
                        SplashStrings.t(lang, "Completar", "Complete")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
