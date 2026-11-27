package com.splash.offline.ui.modals

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.splash.offline.SplashBiomeWidgetProvider
import com.splash.offline.SplashNotificationReceiver
import com.splash.offline.SplashNotificationScheduler
import com.splash.offline.SplashQuickWidgetProvider
import com.splash.offline.SplashWidgetProvider
import com.splash.offline.data.AppLanguage
import com.splash.offline.data.GoalCategory
import com.splash.offline.data.SplashStrings
import com.splash.offline.data.SplashUiState
import com.splash.offline.data.ThemeMode
import com.splash.offline.data.WidgetThemePref

@Composable
fun SettingsSheet(
    state: SplashUiState,
    onClose: () -> Unit,
    onOpenFeedback: () -> Unit,
    isCheckingUpdate: Boolean = false,
    onCheckForUpdates: () -> Unit = {},
    onSaveName: (String) -> Unit,
    onSwitchLanguage: (AppLanguage) -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onSelectWidgetTheme: (WidgetThemePref) -> Unit,
    onToggleDayHour: (String, Int) -> Unit,
    onApplyPreset: (String) -> Unit,
    onSetDailyNotifCount: (Int) -> Unit = {},
    onToggleBeta: (Boolean) -> Unit,
    onCompleteBiomeBeta: () -> Unit,
    onPreviewBiomeCelebration: (GoalCategory) -> Unit = {},
    onResetProgress: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember(state.userName) { mutableStateOf(state.userName) }
    var selectedDay by remember { mutableStateOf("mon") }
    var selectedWidgetTab by remember { mutableStateOf(0) }

    fun fireTestNotification() {
        try {
            val intent = Intent(context, SplashNotificationReceiver::class.java).apply {
                action = SplashNotificationScheduler.ACTION_RANDOM_PING
                putExtra(SplashNotificationReceiver.EXTRA_FORCE_TEST, true)
            }
            context.sendBroadcast(intent)
            onShowToast(SplashStrings.t(state.lang, "Notificación aleatoria enviada", "Random notification sent"))
        } catch (_: Throwable) {
        }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fireTestNotification()
        } else {
            onShowToast(SplashStrings.t(state.lang, "Activa notificaciones en Ajustes del sistema", "Enable notifications in system settings"))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = SplashStrings.t(state.lang, "Ajustes", "Settings"),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Splash v2.0.26 · Material 3 Expressive 1.5",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                }
            }

            // Beta Feedback Form Entry
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onOpenFeedback)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RateReview,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = SplashStrings.t(state.lang, "Enviar feedback Beta", "Send Beta feedback"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = SplashStrings.t(
                                state.lang,
                                "Mapa, volteo, orden y tus objetivos y biomas favoritos/menos.",
                                "Vote map style, card flip, layout & favorite/least goals & biomes."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Auto-Update via GitHub Releases
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = SplashStrings.t(state.lang, "Actualizaciones automáticas", "Automatic updates"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = SplashStrings.t(
                                state.lang,
                                "Busca nuevas versiones de Splash en GitHub e instala en 1 toque.",
                                "Check for new Splash versions on GitHub and install in 1 tap."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = onCheckForUpdates,
                        enabled = !isCheckingUpdate,
                        shape = CircleShape
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(SplashStrings.t(state.lang, "Buscar", "Check"))
                        }
                    }
                }
            }

            // Profile Name
            SettingSection(title = SplashStrings.t(state.lang, "Tu nombre en Splash", "Your name in Splash")) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = {
                            Text(SplashStrings.t(state.lang, "Tu nombre (opcional)", "Your name (optional)"))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onSaveName(nameInput) },
                        shape = CircleShape
                    ) {
                        Text(SplashStrings.t(state.lang, "Guardar", "Save"))
                    }
                }
            }

            // Language ES / EN
            SettingSection(title = SplashStrings.t(state.lang, "Idioma", "Language")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ToggleButton(
                        checked = state.lang == AppLanguage.ES,
                        onCheckedChange = { onSwitchLanguage(AppLanguage.ES) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Español")
                    }
                    ToggleButton(
                        checked = state.lang == AppLanguage.EN,
                        onCheckedChange = { onSwitchLanguage(AppLanguage.EN) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("English")
                    }
                }
            }

            // Theme Mode (System / Light / AMOLED Dark)
            SettingSection(title = SplashStrings.t(state.lang, "Apariencia (AMOLED #000000)", "Appearance (AMOLED #000000)")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        ThemeMode.SYSTEM to SplashStrings.t(state.lang, "Sistema", "System"),
                        ThemeMode.LIGHT to SplashStrings.t(state.lang, "Claro", "Light"),
                        ThemeMode.DARK to SplashStrings.t(state.lang, "Oscuro", "Dark")
                    ).forEach { (mode, lbl) ->
                        ToggleButton(
                            checked = state.themeMode == mode,
                            onCheckedChange = { onSelectTheme(mode) },
                            colors = ToggleButtonDefaults.colors(
                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(lbl, maxLines = 1)
                        }
                    }
                }
            }

            // Notification Scheduler
            SettingSection(title = SplashStrings.t(state.lang, "Notificaciones Flash", "Flash notifications")) {
                Text(
                    text = SplashStrings.t(
                        state.lang,
                        "Elige día y horas en las que recibir avisos aleatorios:",
                        "Pick days and hours to receive gentle random pings:"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val dayDefs = if (state.lang == AppLanguage.EN) {
                    listOf("mon" to "Mon", "tue" to "Tue", "wed" to "Wed", "thu" to "Thu", "fri" to "Fri", "sat" to "Sat", "sun" to "Sun")
                } else {
                    listOf("mon" to "Lun", "tue" to "Mar", "wed" to "Mié", "thu" to "Jue", "fri" to "Vie", "sat" to "Sáb", "sun" to "Dom")
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    dayDefs.forEach { (k, lbl) ->
                        ToggleButton(
                            checked = selectedDay == k,
                            onCheckedChange = { selectedDay = k }
                        ) {
                            Text(lbl)
                        }
                    }
                }

                val daySched = state.notifSchedule[selectedDay]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    for (hr in 8..22) {
                        val active = daySched?.hours?.contains(hr) == true
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleDayHour(selectedDay, hr) }
                        ) {
                            Text(
                                text = "${hr}h",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    FilledTonalButton(onClick = { onApplyPreset("default") }) {
                        Text(SplashStrings.t(state.lang, "L–V sin lun. tarde", "Mon–Fri no Mon PM"))
                    }
                    FilledTonalButton(onClick = { onApplyPreset("weekdays") }) {
                        Text(SplashStrings.t(state.lang, "L–V 9–21h", "Mon–Fri 9a–9p"))
                    }
                    FilledTonalButton(onClick = { onApplyPreset("all") }) {
                        Text(SplashStrings.t(state.lang, "Todos 9–21h", "Daily 9a–9p"))
                    }
                    OutlinedButton(onClick = { onApplyPreset("mute") }) {
                        Text(SplashStrings.t(state.lang, "Silenciar", "Mute"))
                    }
                }

                Text(
                    text = SplashStrings.t(
                        state.lang,
                        "Máximo de avisos al día: ${state.dailyNotifCount} (recibirás entre 2 y ${state.dailyNotifCount} al azar)",
                        "Max notifications per day: ${state.dailyNotifCount} (random 2–${state.dailyNotifCount})"
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (2..7).forEach { count ->
                        ToggleButton(
                            checked = state.dailyNotifCount == count,
                            onCheckedChange = { onSetDailyNotifCount(count) },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(count.toString(), maxLines = 1)
                        }
                    }
                }

                val nextLabel = remember(state.notifSchedule, state.dailyNotifCount, state.lang) {
                    SplashNotificationScheduler.getNextScheduledLabel(context, state.lang == AppLanguage.EN)
                }
                Text(
                    text = SplashStrings.t(state.lang, "Próximo aviso programado: $nextLabel", "Next scheduled ping: $nextLabel"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (state.betaMode) {
                    FilledTonalButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                fireTestNotification()
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SplashStrings.t(state.lang, "Probar notificación aleatoria ahora", "Test random notification now"))
                    }
                }
            }

            // Widgets Center
            SettingSection(title = SplashStrings.t(state.lang, "Widgets de inicio", "Home Widgets")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        0 to "4×2 Flash",
                        1 to SplashStrings.t(state.lang, "2×2 Bioma", "2×2 Biome"),
                        2 to SplashStrings.t(state.lang, "Temporizador", "Timer")
                    ).forEach { (idx, lbl) ->
                        ToggleButton(
                            checked = selectedWidgetTab == idx,
                            onCheckedChange = { selectedWidgetTab = idx },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(lbl, maxLines = 1)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        WidgetThemePref.AUTO to SplashStrings.t(state.lang, "Auto", "Auto"),
                        WidgetThemePref.LIGHT to SplashStrings.t(state.lang, "Claro", "Light"),
                        WidgetThemePref.DARK to SplashStrings.t(state.lang, "Oscuro", "Dark")
                    ).forEach { (pref, lbl) ->
                        ToggleButton(
                            checked = state.widgetThemePref == pref,
                            onCheckedChange = { onSelectWidgetTheme(pref) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(lbl, maxLines = 1)
                        }
                    }
                }

                Button(
                    onClick = {
                        val cls = when (selectedWidgetTab) {
                            1 -> SplashBiomeWidgetProvider::class.java
                            2 -> SplashQuickWidgetProvider::class.java
                            else -> SplashWidgetProvider::class.java
                        }
                        pinWidget(context, cls) { ok ->
                            onShowToast(
                                if (ok) SplashStrings.t(state.lang, "Solicitud enviada al launcher", "Widget request sent")
                                else SplashStrings.t(state.lang, "Añádelo desde la galería de widgets de tu móvil", "Add it from your home screen widget picker")
                            )
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(SplashStrings.t(state.lang, "Añadir widget a la pantalla de inicio", "Add widget to home screen"))
                }
            }

            // Beta Mode & Tests
            SettingSection(title = SplashStrings.t(state.lang, "Modo Beta y Pruebas", "Beta Mode & Tests")) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                text = SplashStrings.t(state.lang, "Modo Beta", "Beta Mode"),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = SplashStrings.t(state.lang, "Desbloquea biomas y selector de nivel en 3D", "Unlock biomes and stage preview in 3D"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(checked = state.betaMode, onCheckedChange = onToggleBeta)
                }

                if (state.betaMode) {
                    FilledTonalButton(
                        onClick = onCompleteBiomeBeta,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(SplashStrings.t(state.lang, "Completar bioma actual (100/100)", "Complete current biome (100/100)"))
                    }

                    Text(
                        text = SplashStrings.t(state.lang, "Probar animaciones de crecimiento en Hoy", "Test biome growth animations on Today"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilledTonalButton(
                            onClick = { onPreviewBiomeCelebration(GoalCategory.RAPIDO) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Flash (+2)", maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { onPreviewBiomeCelebration(GoalCategory.MEDIO) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(SplashStrings.t(state.lang, "Medio (+3)", "Med (+3)"), maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { onPreviewBiomeCelebration(GoalCategory.LARGO) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(SplashStrings.t(state.lang, "Semanal (+4)", "Week (+4)"), maxLines = 1)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onResetProgress,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(SplashStrings.t(state.lang, "Reiniciar progreso (desde 0)", "Reset progress (from 0)"))
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

private fun pinWidget(context: Context, providerCls: Class<*>, onResult: (Boolean) -> Unit) {
    try {
        val awm = AppWidgetManager.getInstance(context)
        if (awm != null && awm.isRequestPinAppWidgetSupported) {
            val ok = awm.requestPinAppWidget(ComponentName(context, providerCls), null, null)
            onResult(ok)
        } else {
            onResult(false)
        }
    } catch (_: Throwable) {
        onResult(false)
    }
}
