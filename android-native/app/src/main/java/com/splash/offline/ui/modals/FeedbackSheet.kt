package com.splash.offline.ui.modals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.splash.offline.data.GoalItem
import com.splash.offline.data.SplashCatalogData
import com.splash.offline.data.SplashStrings
import com.splash.offline.data.SplashUiState
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NETLIFY_FEEDBACK_ENDPOINT = "https://splash-beta-feedback.netlify.app/"

@Composable
fun FeedbackSheet(
    state: SplashUiState,
    onClose: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var respondentName by remember(state.userName) { mutableStateOf(state.userName) }
    var disconnectHelp by remember { mutableStateOf("") }
    var notifControlAgree by remember { mutableStateOf("") }
    var favoriteMap by remember { mutableStateOf("") }
    var favoriteFlip by remember { mutableStateOf("") }
    var favoriteBiomeOrder by remember { mutableStateOf("") }
    var proofMethods by remember { mutableStateOf(setOf<String>()) }
    var customProofIdea by remember { mutableStateOf("") }
    var favGoalIds by remember { mutableStateOf(listOf<Int>()) }
    var favCustomGoals by remember { mutableStateOf(listOf<String>()) }
    var favCustomInput by remember { mutableStateOf("") }
    var dislikeGoalIds by remember { mutableStateOf(listOf<Int>()) }
    var dislikeCustomGoals by remember { mutableStateOf(listOf<String>()) }
    var dislikeCustomInput by remember { mutableStateOf("") }
    var favBiomeIds by remember { mutableStateOf(listOf<String>()) }
    var dislikeBiomeIds by remember { mutableStateOf(listOf<String>()) }
    var comments by remember { mutableStateOf("") }

    var showFavPicker by remember { mutableStateOf(false) }
    var favSearch by remember { mutableStateOf("") }
    var showDislikePicker by remember { mutableStateOf(false) }
    var dislikeSearch by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val availableGoals = remember(state.catalog) {
        state.catalog.filter { !it.isHidden }.sortedBy { it.id }
    }

    fun formatGoalList(ids: List<Int>, custom: List<String>): String {
        val entries = buildList {
            ids.mapNotNull { id ->
                availableGoals.firstOrNull { it.id == id }?.let { "${it.id}. ${it.displayShort(state.lang)}" }
            }.forEach { add(it) }
            custom.forEach { entry -> add("Escrito: $entry") }
        }
        return entries.joinToString(" | ").ifBlank { "Ninguno" }
    }

    fun formatBiomeList(ids: List<String>): String =
        ids.mapNotNull { id ->
            SplashCatalogData.BIOMES.firstOrNull { it.id == id }?.name(state.lang)
        }.joinToString(" | ").ifBlank { "Ninguno" }

    fun buildSummary(): String {
        val favNames = buildList {
            favGoalIds.mapNotNull { id ->
                availableGoals.firstOrNull { it.id == id }?.let { "${it.id}. ${it.displayShort(state.lang)} (${it.displayTitle(state.lang)})" }
            }.forEach { add(it) }
            favCustomGoals.forEach { add("Escrito: $it") }
        }
        val dislikeNames = buildList {
            dislikeGoalIds.mapNotNull { id ->
                availableGoals.firstOrNull { it.id == id }?.let { "${it.id}. ${it.displayShort(state.lang)} (${it.displayTitle(state.lang)})" }
            }.forEach { add(it) }
            dislikeCustomGoals.forEach { add("Escrito: $it") }
        }
        val favBiomeNames = favBiomeIds.mapNotNull { id ->
            SplashCatalogData.BIOMES.firstOrNull { it.id == id }?.name(state.lang)
        }
        val dislikeBiomeNames = dislikeBiomeIds.mapNotNull { id ->
            SplashCatalogData.BIOMES.firstOrNull { it.id == id }?.name(state.lang)
        }
        val proofSummary = buildList {
            addAll(proofMethods)
            if (customProofIdea.isNotBlank()) add("Otra idea: ${customProofIdea.trim()}")
        }.joinToString(", ").ifBlank { "Sin respuesta" }

        val userLabel = respondentName.trim().ifBlank { "Sin nombre" }
        return buildString {
            appendLine("=== INFORME SPLASH BETA v2.0.24 ===")
            appendLine("1. Nombre: $userLabel")
            appendLine("2. Ayuda a desconectar / dejar de scrollear: ${disconnectHelp.ifBlank { "Sin respuesta" }}")
            appendLine("3. Control aleatoriedad notificaciones: ${notifControlAgree.ifBlank { "Sin respuesta" }}")
            appendLine("4. Top 5 objetivos favoritos:")
            if (favNames.isEmpty()) appendLine("   - Ninguno")
            else favNames.forEach { appendLine("   - $it") }
            appendLine("5. Top 5 objetivos que menos gustan:")
            if (dislikeNames.isEmpty()) appendLine("   - Ninguno")
            else dislikeNames.forEach { appendLine("   - $it") }
            appendLine("6. Top 3 biomas favoritos:")
            if (favBiomeNames.isEmpty()) appendLine("   - Ninguno")
            else favBiomeNames.forEach { appendLine("   - $it") }
            appendLine("7. Top 3 biomas que menos gustan:")
            if (dislikeBiomeNames.isEmpty()) appendLine("   - Ninguno")
            else dislikeBiomeNames.forEach { appendLine("   - $it") }
            appendLine("8. Mapa favorito: ${favoriteMap.ifBlank { "Sin respuesta" }}")
            appendLine("9. Estilo de volteo favorito: ${favoriteFlip.ifBlank { "Sin respuesta" }}")
            appendLine("10. Diseño biomas favorito: ${favoriteBiomeOrder.ifBlank { "Sin respuesta" }}")
            appendLine("11. Cómo demostrar cumplido: $proofSummary")
            appendLine("12. Comentarios: ${comments.trim().ifBlank { "Ninguno" }}")
        }
    }

    fun submitToNetlify() {
        val hasAnyAnswer = respondentName.isNotBlank() ||
            disconnectHelp.isNotBlank() ||
            notifControlAgree.isNotBlank() ||
            favGoalIds.isNotEmpty() ||
            favCustomGoals.isNotEmpty() ||
            dislikeGoalIds.isNotEmpty() ||
            dislikeCustomGoals.isNotEmpty() ||
            favBiomeIds.isNotEmpty() ||
            dislikeBiomeIds.isNotEmpty() ||
            favoriteMap.isNotBlank() ||
            favoriteFlip.isNotBlank() ||
            favoriteBiomeOrder.isNotBlank() ||
            proofMethods.isNotEmpty() ||
            customProofIdea.isNotBlank() ||
            comments.isNotBlank()

        if (!hasAnyAnswer) {
            errorMessage = SplashStrings.t(
                state.lang,
                "Responde al menos una pregunta antes de enviar el informe.",
                "Answer at least one question before sending."
            )
            return
        }

        val proofSummary = buildList {
            addAll(proofMethods)
            if (customProofIdea.isNotBlank()) add("Otra idea: ${customProofIdea.trim()}")
        }.joinToString(", ").ifBlank { "Sin respuesta" }

        val fields = linkedMapOf(
            "form-name" to "splash-beta-feedback",
            "bot-field" to "",
            "usuario" to respondentName.trim().ifBlank { "Sin nombre" },
            "version" to "2.0.24",
            "ayuda_desconectar" to disconnectHelp.ifBlank { "Sin respuesta" },
            "control_notis" to notifControlAgree.ifBlank { "Sin respuesta" },
            "mapa_favorito" to favoriteMap.ifBlank { "Sin respuesta" },
            "estilo_volteo" to favoriteFlip.ifBlank { "Sin respuesta" },
            "diseno_biomas" to favoriteBiomeOrder.ifBlank { "Sin respuesta" },
            "como_demostrar" to proofSummary,
            "top5_favoritos" to formatGoalList(favGoalIds, favCustomGoals),
            "top5_menos" to formatGoalList(dislikeGoalIds, dislikeCustomGoals),
            "top3_biomas_favoritos" to formatBiomeList(favBiomeIds),
            "top3_biomas_menos" to formatBiomeList(dislikeBiomeIds),
            "resumen_informe" to buildSummary(),
            "comentarios" to comments.trim().ifBlank { "Ninguno" }
        )

        isSubmitting = true
        errorMessage = null

        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val encoded = fields.entries.joinToString("&") { (k, v) ->
                        "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
                    }
                    val conn = (URL(NETLIFY_FEEDBACK_ENDPOINT).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 12_000
                        readTimeout = 12_000
                        doOutput = true
                        instanceFollowRedirects = true
                        setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        setRequestProperty("Accept", "text/html,application/json")
                    }
                    conn.outputStream.use { it.write(encoded.toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    conn.disconnect()
                    code in 200..399
                } catch (_: Throwable) {
                    false
                }
            }
            isSubmitting = false
            if (ok) {
                isSubmitted = true
                onShowToast(SplashStrings.t(state.lang, "Informe enviado correctamente", "Report sent successfully"))
            } else {
                errorMessage = SplashStrings.t(
                    state.lang,
                    "No se pudo enviar. Revisa tu conexión a internet e inténtalo de nuevo.",
                    "Could not send. Check your internet connection and try again."
                )
            }
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = SplashStrings.t(state.lang, "Formulario Beta", "Beta Feedback Form"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = SplashStrings.t(
                            state.lang,
                            "Tu opinión decide el diseño final antes del lanzamiento.",
                            "Your feedback shapes the final launch design."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = SplashStrings.t(state.lang, "Cerrar", "Close"))
                }
            }

            if (isSubmitted) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                        Text(
                            text = SplashStrings.t(state.lang, "Informe enviado", "Report sent"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = SplashStrings.t(
                                state.lang,
                                "Gracias por ayudar a pulir Splash antes del lanzamiento. Tu informe ha llegado correctamente.",
                                "Thanks for helping polish Splash before launch. Your report was delivered."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onClose,
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(SplashStrings.t(state.lang, "Volver a Ajustes", "Back to Settings"))
                        }
                    }
                }
            } else {
                // 1. Nombre
                FeedbackSectionCard(
                    title = SplashStrings.t(state.lang, "1. Tu nombre o apodo", "1. Your name or nickname")
                ) {
                    OutlinedTextField(
                        value = respondentName,
                        onValueChange = { respondentName = it },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = {
                            Text(SplashStrings.t(state.lang, "Escribe tu nombre...", "Type your name..."))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Cuanto ayuda a dejar de scrollear o desconectar
                FeedbackSectionCard(
                    title = SplashStrings.t(
                        state.lang,
                        "2. ¿Cuánto te ayuda Splash a dejar de scrollear o desconectar?",
                        "2. How much does Splash help you stop scrolling or unplug?"
                    )
                ) {
                    val disconnectOptions = listOf(
                        "1 · Nada" to SplashStrings.t(state.lang, "1 · Nada", "1 · Not at all"),
                        "2 · Poco" to SplashStrings.t(state.lang, "2 · Poco", "2 · A little"),
                        "3 · Algo" to SplashStrings.t(state.lang, "3 · Algo", "3 · Somewhat"),
                        "4 · Bastante" to SplashStrings.t(state.lang, "4 · Bastante", "4 · Quite a lot"),
                        "5 · Muchísimo" to SplashStrings.t(state.lang, "5 · Muchísimo", "5 · A huge help")
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        disconnectOptions.forEach { (value, label) ->
                            FilterChip(
                                selected = disconnectHelp == value,
                                onClick = { disconnectHelp = if (disconnectHelp == value) "" else value },
                                shape = CircleShape,
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // 3. Control del maximo de notificaciones aleatorias
                FeedbackSectionCard(
                    title = SplashStrings.t(
                        state.lang,
                        "3. ¿Estás de acuerdo con poder elegir el máximo de notificaciones aleatorias al día?",
                        "3. Do you like choosing the daily max of random notifications?"
                    )
                ) {
                    val notifControlOptions = listOf(
                        "Sí, prefiero controlar el máximo" to SplashStrings.t(state.lang, "Sí, mejor elegir máximo", "Yes, pick daily max"),
                        "No, prefiero 100% sorpresa" to SplashStrings.t(state.lang, "No, prefiero 100% sorpresa", "No, 100% surprise"),
                        "Me da igual" to SplashStrings.t(state.lang, "Me da igual", "No preference")
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        notifControlOptions.forEach { (value, label) ->
                            FilterChip(
                                selected = notifControlAgree == value,
                                onClick = { notifControlAgree = if (notifControlAgree == value) "" else value },
                                shape = CircleShape,
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // 4. Tus 5 objetivos favoritos
                GoalMultiPickerCard(
                    title = SplashStrings.t(
                        state.lang,
                        "4. Elige tus 5 objetivos favoritos (${favGoalIds.size + favCustomGoals.size}/5)",
                        "4. Pick your top 5 favorite goals (${favGoalIds.size + favCustomGoals.size}/5)"
                    ),
                    lang = state.lang,
                    allGoals = availableGoals,
                    selectedIds = favGoalIds,
                    customEntries = favCustomGoals,
                    customInput = favCustomInput,
                    onCustomInputChange = { favCustomInput = it },
                    onAddCustom = {
                        val trimmed = favCustomInput.trim()
                        val total = favGoalIds.size + favCustomGoals.size
                        if (trimmed.isNotEmpty()) {
                            if (total >= 5) {
                                onShowToast(SplashStrings.t(state.lang, "Máximo 5 objetivos favoritos", "Maximum 5 favorite goals"))
                            } else if (favCustomGoals.any { it.equals(trimmed, ignoreCase = true) }) {
                                onShowToast(SplashStrings.t(state.lang, "Ese objetivo ya está añadido", "That goal is already added"))
                            } else {
                                favCustomGoals = favCustomGoals + trimmed
                                favCustomInput = ""
                            }
                        }
                    },
                    onRemoveCustom = { entry -> favCustomGoals = favCustomGoals - entry },
                    blockedIds = dislikeGoalIds.toSet(),
                    expanded = showFavPicker,
                    onToggleExpanded = { showFavPicker = !showFavPicker },
                    searchQuery = favSearch,
                    onSearchChange = { favSearch = it },
                    onToggleGoal = { id ->
                        favGoalIds = if (id in favGoalIds) {
                            favGoalIds - id
                        } else if (favGoalIds.size + favCustomGoals.size < 5) {
                            favGoalIds + id
                        } else {
                            onShowToast(SplashStrings.t(state.lang, "Máximo 5 objetivos favoritos", "Maximum 5 favorite goals"))
                            favGoalIds
                        }
                    },
                    icon = Icons.Rounded.Favorite
                )

                // 5. Tus 5 objetivos menos favoritos
                GoalMultiPickerCard(
                    title = SplashStrings.t(
                        state.lang,
                        "5. Elige los 5 objetivos que menos te gustan (${dislikeGoalIds.size + dislikeCustomGoals.size}/5)",
                        "5. Pick the 5 goals you like least (${dislikeGoalIds.size + dislikeCustomGoals.size}/5)"
                    ),
                    lang = state.lang,
                    allGoals = availableGoals,
                    selectedIds = dislikeGoalIds,
                    customEntries = dislikeCustomGoals,
                    customInput = dislikeCustomInput,
                    onCustomInputChange = { dislikeCustomInput = it },
                    onAddCustom = {
                        val trimmed = dislikeCustomInput.trim()
                        val total = dislikeGoalIds.size + dislikeCustomGoals.size
                        if (trimmed.isNotEmpty()) {
                            if (total >= 5) {
                                onShowToast(SplashStrings.t(state.lang, "Máximo 5 objetivos menos votados", "Maximum 5 least favorite goals"))
                            } else if (dislikeCustomGoals.any { it.equals(trimmed, ignoreCase = true) }) {
                                onShowToast(SplashStrings.t(state.lang, "Ese objetivo ya está añadido", "That goal is already added"))
                            } else {
                                dislikeCustomGoals = dislikeCustomGoals + trimmed
                                dislikeCustomInput = ""
                            }
                        }
                    },
                    onRemoveCustom = { entry -> dislikeCustomGoals = dislikeCustomGoals - entry },
                    blockedIds = favGoalIds.toSet(),
                    expanded = showDislikePicker,
                    onToggleExpanded = { showDislikePicker = !showDislikePicker },
                    searchQuery = dislikeSearch,
                    onSearchChange = { dislikeSearch = it },
                    onToggleGoal = { id ->
                        dislikeGoalIds = if (id in dislikeGoalIds) {
                            dislikeGoalIds - id
                        } else if (dislikeGoalIds.size + dislikeCustomGoals.size < 5) {
                            dislikeGoalIds + id
                        } else {
                            onShowToast(SplashStrings.t(state.lang, "Máximo 5 objetivos menos votados", "Maximum 5 least favorite goals"))
                            dislikeGoalIds
                        }
                    },
                    icon = Icons.Rounded.ThumbDown
                )

                // 6. Tus 3 biomas favoritos
                FeedbackSectionCard(
                    title = SplashStrings.t(
                        state.lang,
                        "6. Elige tus 3 biomas favoritos (${favBiomeIds.size}/3)",
                        "6. Pick your top 3 favorite biomes (${favBiomeIds.size}/3)"
                    )
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SplashCatalogData.BIOMES.forEach { biome ->
                            val selected = biome.id in favBiomeIds
                            FilterChip(
                                selected = selected,
                                enabled = biome.id !in dislikeBiomeIds,
                                onClick = {
                                    favBiomeIds = if (selected) {
                                        favBiomeIds - biome.id
                                    } else if (favBiomeIds.size < 3) {
                                        favBiomeIds + biome.id
                                    } else {
                                        onShowToast(SplashStrings.t(state.lang, "Máximo 3 biomas favoritos", "Maximum 3 favorite biomes"))
                                        favBiomeIds
                                    }
                                },
                                shape = CircleShape,
                                label = { Text(biome.shortName(state.lang)) }
                            )
                        }
                    }
                }

                // 7. Tus 3 biomas menos favoritos
                FeedbackSectionCard(
                    title = SplashStrings.t(
                        state.lang,
                        "7. Elige los 3 biomas que menos te gustan (${dislikeBiomeIds.size}/3)",
                        "7. Pick the 3 biomes you like least (${dislikeBiomeIds.size}/3)"
                    )
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SplashCatalogData.BIOMES.forEach { biome ->
                            val selected = biome.id in dislikeBiomeIds
                            FilterChip(
                                selected = selected,
                                enabled = biome.id !in favBiomeIds,
                                onClick = {
                                    dislikeBiomeIds = if (selected) {
                                        dislikeBiomeIds - biome.id
                                    } else if (dislikeBiomeIds.size < 3) {
                                        dislikeBiomeIds + biome.id
                                    } else {
                                        onShowToast(SplashStrings.t(state.lang, "Máximo 3 biomas menos votados", "Maximum 3 least favorite biomes"))
                                        dislikeBiomeIds
                                    }
                                },
                                shape = CircleShape,
                                label = { Text(biome.shortName(state.lang)) }
                            )
                        }
                    }
                }

                // 8. Que mapa mola mas
                FeedbackSectionCard(
                    title = SplashStrings.t(state.lang, "8. ¿Qué mapa mola más?", "8. Which map style is best?")
                ) {
                    val mapOptions = listOf("1", "2", "3", "4")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        mapOptions.forEach { num ->
                            FilterChip(
                                selected = favoriteMap == num,
                                onClick = { favoriteMap = if (favoriteMap == num) "" else num },
                                shape = CircleShape,
                                label = { Text(num) }
                            )
                        }
                    }
                }

                // 9. Estilo de volteo de flashcard
                FeedbackSectionCard(
                    title = SplashStrings.t(state.lang, "9. ¿Qué estilo de volteo de flashcard prefieres?", "9. Which flashcard reveal style do you prefer?")
                ) {
                    val flipOptions = listOf(
                        "Flash izq · Giro 3D horizontal" to SplashStrings.t(state.lang, "Giro 3D horizontal (Flash izq)", "3D Horizontal Flip (Flash L)"),
                        "Flash der · Cortina deslizante" to SplashStrings.t(state.lang, "Cortina deslizante (Flash der)", "Sliding Curtain (Flash R)"),
                        "Medio izq · Giro 3D vertical" to SplashStrings.t(state.lang, "Giro 3D vertical (Medio izq)", "3D Vertical Flip (Med L)"),
                        "Medio der · Acordeón lateral" to SplashStrings.t(state.lang, "Acordeón lateral (Medio der)", "Side Accordion (Med R)")
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        flipOptions.forEach { (value, label) ->
                            FilterChip(
                                selected = favoriteFlip == value,
                                onClick = { favoriteFlip = if (favoriteFlip == value) "" else value },
                                shape = CircleShape,
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // 10. Diseno para ordenar los biomas
                FeedbackSectionCard(
                    title = SplashStrings.t(state.lang, "10. ¿Qué diseño te gusta más para ordenar los biomas?", "10. Which biome layout order do you prefer?")
                ) {
                    val layoutOptions = listOf(
                        "Mini (cuadrícula)" to SplashStrings.t(state.lang, "Mini (cuadrícula 2 col.)", "Mini (2-col grid)"),
                        "Apilado (lista)" to SplashStrings.t(state.lang, "Apilado (lista)", "Stacked (list)"),
                        "Grande (tarjetas)" to SplashStrings.t(state.lang, "Grande (tarjetas 3D)", "Large (3D cards)")
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        layoutOptions.forEach { (value, label) ->
                            FilterChip(
                                selected = favoriteBiomeOrder == value,
                                onClick = { favoriteBiomeOrder = if (favoriteBiomeOrder == value) "" else value },
                                shape = CircleShape,
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // 11. Como demostrar que has cumplido un objetivo
                FeedbackSectionCard(
                    title = SplashStrings.t(state.lang, "11. ¿Cómo te gustaría demostrar que has cumplido un objetivo?", "11. How should you prove goal completion?")
                ) {
                    val proofOptions = listOf(
                        "Temporizador sin salir (pantalla apagada)" to SplashStrings.t(state.lang, "Temporizador sin salir (pantalla apagada)", "Focus timer without leaving"),
                        "Foto rápida (ej. página del libro)" to SplashStrings.t(state.lang, "Foto rápida (ej. página del libro)", "Quick photo (e.g. book page)"),
                        "Mini-resumen de 1 frase" to SplashStrings.t(state.lang, "Mini-resumen de 1 frase", "1-sentence quick takeaway"),
                        "Toque rápido (confianza)" to SplashStrings.t(state.lang, "Toque rápido (confianza)", "Quick tap (honor)"),
                        "Mixto según el objetivo" to SplashStrings.t(state.lang, "Mixto según el reto", "Mixed per challenge")
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        proofOptions.forEach { (value, label) ->
                            val active = value in proofMethods
                            FilterChip(
                                selected = active,
                                onClick = {
                                    proofMethods = if (active) proofMethods - value else proofMethods + value
                                },
                                shape = CircleShape,
                                label = { Text(label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customProofIdea,
                        onValueChange = { customProofIdea = it },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = {
                            Text(SplashStrings.t(state.lang, "Otra forma de demostrarlo...", "Another idea to verify completion..."))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 12. Comentarios
                FeedbackSectionCard(
                    title = SplashStrings.t(state.lang, "12. Otros detalles o sugerencias", "12. Other suggestions or details")
                ) {
                    OutlinedTextField(
                        value = comments,
                        onValueChange = { comments = it },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = {
                            Text(
                                SplashStrings.t(
                                    state.lang,
                                    "Cuéntanos qué cambiarías antes de lanzar...",
                                    "Tell us what to polish before launch..."
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Button(
                    onClick = { submitToNetlify() },
                    enabled = !isSubmitting,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = SplashStrings.t(state.lang, "Enviando informe...", "Sending report..."),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = SplashStrings.t(state.lang, "Enviar informe", "Send report"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeedbackSectionCard(
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun GoalMultiPickerCard(
    title: String,
    lang: com.splash.offline.data.AppLanguage,
    allGoals: List<GoalItem>,
    selectedIds: List<Int>,
    customEntries: List<String>,
    customInput: String,
    onCustomInputChange: (String) -> Unit,
    onAddCustom: () -> Unit,
    onRemoveCustom: (String) -> Unit,
    blockedIds: Set<Int>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onToggleGoal: (Int) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val totalSelected = selectedIds.size + customEntries.size

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onToggleExpanded)
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null
                )
            }

            if (selectedIds.isNotEmpty() || customEntries.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectedIds.forEach { gid ->
                        val g = allGoals.firstOrNull { it.id == gid }
                        if (g != null) {
                            InputChip(
                                selected = true,
                                onClick = { onToggleGoal(gid) },
                                shape = CircleShape,
                                label = {
                                    Text(
                                        text = g.displayShort(lang),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingIcon = {
                                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            )
                        }
                    }
                    customEntries.forEach { entry ->
                        InputChip(
                            selected = true,
                            onClick = { onRemoveCustom(entry) },
                            shape = CircleShape,
                            label = {
                                Text(
                                    text = entry,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                            }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = onCustomInputChange,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = {
                        Text(
                            text = SplashStrings.t(lang, "Si no lo encuentras, escríbelo aquí...", "Can't find it? Type it here..."),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onAddCustom,
                    enabled = customInput.trim().isNotEmpty() && totalSelected < 5,
                    shape = CircleShape
                ) {
                    Text(SplashStrings.t(lang, "Añadir", "Add"))
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        shape = CircleShape,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        placeholder = { Text(SplashStrings.t(lang, "Buscar objetivo...", "Search goal...")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    val q = searchQuery.trim().lowercase()
                    val filtered = remember(allGoals, q, blockedIds) {
                        allGoals.filter { g ->
                            g.id !in blockedIds && (
                                q.isEmpty() ||
                                    g.displayShort(lang).lowercase().contains(q) ||
                                    g.displayTitle(lang).lowercase().contains(q) ||
                                    g.displaySub(lang).lowercase().contains(q)
                                )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        filtered.forEach { g ->
                            val picked = g.id in selectedIds
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (picked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onToggleGoal(g.id) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = g.displayShort(lang),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = g.displayTitle(lang),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (picked) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
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
}
