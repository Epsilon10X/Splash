package com.splash.offline.ui.modals

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.splash.offline.data.AppLanguage
import com.splash.offline.data.GoalCategory
import com.splash.offline.data.PendingNoteCompletion
import com.splash.offline.data.SplashStrings
import java.io.File

@Composable
fun PastSelfMessageDialog(
    lang: AppLanguage,
    pending: PendingNoteCompletion,
    pointsPreviewLabel: String,
    onTakePhoto: () -> Unit = {},
    onPickGalleryPhoto: () -> Unit = {},
    onRemovePhoto: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    val goal = pending.goal
    val isMandatory = goal.category == GoalCategory.MEDIO || goal.category == GoalCategory.LARGO
    var note by remember(goal.id) { mutableStateOf("") }
    val trimmed = note.trim()
    val canConfirm = !isMandatory || trimmed.isNotEmpty()
    val attachedThumb = remember(pending.photoPath) {
        pending.photoPath?.let { path ->
            if (File(path).exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = pointsPreviewLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Text(
                    text = SplashStrings.t(lang, "Mensaje para yo del futuro", "Message to my future self"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = goal.displayShort(lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp),
                    label = {
                        Text(
                            if (isMandatory) {
                                SplashStrings.t(lang, "Escribe cómo fue o qué te llevas (obligatorio)", "Write how it went or your takeaway (required)")
                            } else {
                                SplashStrings.t(lang, "Nota rápida (opcional)", "Quick note (optional)")
                            }
                        )
                    },
                    placeholder = {
                        Text(
                            SplashStrings.t(
                                lang,
                                "Ej. Qué leí, cómo me sentí o qué pensé hoy...",
                                "e.g. What I read, felt, or realized today..."
                            )
                        )
                    },
                    supportingText = {
                        Text(
                            if (isMandatory) {
                                SplashStrings.t(
                                    lang,
                                    "Obligatorio en objetivos Medios y Semanales para guardarlo en Recuerdos.",
                                    "Required for Medium & Weekly goals to save into Memories."
                                )
                            } else {
                                SplashStrings.t(
                                    lang,
                                    "Opcional en objetivos Flash.",
                                    "Optional on Flash goals."
                                )
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = SplashStrings.t(lang, "Foto recuerdo (opcional)", "Memory photo (optional)"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (attachedThumb != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Image(
                                    bitmap = attachedThumb,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                )
                                Text(
                                    text = SplashStrings.t(lang, "Foto añadida al recuerdo", "Photo attached to memory"),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = onRemovePhoto,
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(SplashStrings.t(lang, "Quitar", "Remove"))
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = onTakePhoto,
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(SplashStrings.t(lang, "Cámara", "Camera"), maxLines = 1)
                                }
                                FilledTonalButton(
                                    onClick = onPickGalleryPhoto,
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(SplashStrings.t(lang, "Galería", "Gallery"), maxLines = 1)
                                }
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(SplashStrings.t(lang, "Cancelar", "Cancel"))
                    }
                    Button(
                        onClick = { onConfirm(trimmed.ifEmpty { null }) },
                        enabled = canConfirm,
                        shape = CircleShape,
                        modifier = Modifier.weight(1.35f)
                    ) {
                        Text(SplashStrings.t(lang, "Completar", "Complete"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
