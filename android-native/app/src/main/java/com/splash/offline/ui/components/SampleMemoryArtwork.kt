package com.splash.offline.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.splash.offline.data.AppLanguage

data class SampleMemoryItem(
    val id: String,
    val titleEs: String,
    val titleEn: String,
    val biomeId: String,
    val dateLabelEs: String,
    val dateLabelEn: String,
    val accentColor: Color
) {
    fun title(lang: AppLanguage): String = if (lang == AppLanguage.EN) titleEn else titleEs
    fun dateLabel(lang: AppLanguage): String = if (lang == AppLanguage.EN) dateLabelEn else dateLabelEs
}

val SAMPLE_MEMORIES: List<SampleMemoryItem> = listOf(
    SampleMemoryItem(
        id = "valle_sunrise",
        titleEs = "Amanecer alpino",
        titleEn = "Alpine sunrise",
        biomeId = "valle",
        dateLabelEs = "Ejemplo · Valle",
        dateLabelEn = "Sample · Valley",
        accentColor = Color(0xFF569B58)
    ),
    SampleMemoryItem(
        id = "costa_sunset",
        titleEs = "Atardecer sin móvil",
        titleEn = "Phone-free sunset",
        biomeId = "costa",
        dateLabelEs = "Ejemplo · Costa",
        dateLabelEn = "Sample · Coast",
        accentColor = Color(0xFFE0855B)
    ),
    SampleMemoryItem(
        id = "kioto_sakura",
        titleEs = "Paseo entre cerezos",
        titleEn = "Sakura stroll",
        biomeId = "kioto",
        dateLabelEs = "Ejemplo · Kioto",
        dateLabelEn = "Sample · Kyoto",
        accentColor = Color(0xFFE08AA2)
    ),
    SampleMemoryItem(
        id = "jungla_waterfall",
        titleEs = "Cascada escondida",
        titleEn = "Hidden waterfall",
        biomeId = "jungla",
        dateLabelEs = "Ejemplo · Jungla",
        dateLabelEn = "Sample · Jungle",
        accentColor = Color(0xFF3E8C5B)
    ),
    SampleMemoryItem(
        id = "toscana_picnic",
        titleEs = "Merienda dorada",
        titleEn = "Golden hill picnic",
        biomeId = "toscana",
        dateLabelEs = "Ejemplo · Toscana",
        dateLabelEn = "Sample · Tuscany",
        accentColor = Color(0xFFD1A447)
    ),
    SampleMemoryItem(
        id = "atolon_stars",
        titleEs = "Noche de estrellas",
        titleEn = "Stargazing night",
        biomeId = "atolon",
        dateLabelEs = "Ejemplo · Atolón",
        dateLabelEn = "Sample · Atoll",
        accentColor = Color(0xFF4CC7C4)
    )
)

@Composable
fun SampleMemoryArtwork(
    sampleId: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (sampleId) {
            "valle_sunrise" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF9AD5F5), Color(0xFFFDE3B8), Color(0xFFE6F0D8))
                    )
                )
                drawCircle(
                    color = Color(0xFFFFD87A).copy(alpha = 0.45f),
                    radius = w * 0.24f,
                    center = Offset(w * 0.54f, h * 0.44f)
                )
                drawCircle(
                    color = Color(0xFFFFF2B8),
                    radius = w * 0.14f,
                    center = Offset(w * 0.54f, h * 0.44f)
                )
                val backPeak = Path().apply {
                    moveTo(0f, h * 0.72f)
                    lineTo(w * 0.30f, h * 0.34f)
                    lineTo(w * 0.60f, h * 0.62f)
                    lineTo(w * 0.84f, h * 0.38f)
                    lineTo(w, h * 0.64f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(backPeak, Color(0xFF6B9377))
                val snowCap = Path().apply {
                    moveTo(w * 0.30f, h * 0.34f)
                    lineTo(w * 0.21f, h * 0.46f)
                    lineTo(w * 0.28f, h * 0.44f)
                    lineTo(w * 0.33f, h * 0.48f)
                    lineTo(w * 0.39f, h * 0.44f)
                    close()
                }
                drawPath(snowCap, Color(0xFFF7FBFF))
                val meadow = Path().apply {
                    moveTo(0f, h * 0.72f)
                    quadraticTo(w * 0.45f, h * 0.60f, w, h * 0.74f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(meadow, Color(0xFF569B58))
                // Pines
                listOf(0.18f to 0.74f, 0.78f to 0.72f).forEach { (px, py) ->
                    val pine = Path().apply {
                        moveTo(w * px, h * (py - 0.14f))
                        lineTo(w * (px + 0.06f), h * py)
                        lineTo(w * (px - 0.06f), h * py)
                        close()
                    }
                    drawPath(pine, Color(0xFF2F5E38))
                }
            }

            "costa_sunset" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF8397E8), Color(0xFFF69E7B), Color(0xFFFED38F))
                    )
                )
                drawCircle(
                    color = Color(0xFFFFB26B).copy(alpha = 0.45f),
                    radius = w * 0.23f,
                    center = Offset(w * 0.50f, h * 0.54f)
                )
                drawCircle(
                    color = Color(0xFFFFE59E),
                    radius = w * 0.15f,
                    center = Offset(w * 0.50f, h * 0.54f)
                )
                // Sea
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF4BA6BD), Color(0xFF28728F))
                    ),
                    topLeft = Offset(0f, h * 0.60f),
                    size = Size(w, h * 0.40f)
                )
                // Sun reflection stripes
                listOf(0.65f to 0.22f, 0.72f to 0.16f, 0.79f to 0.10f).forEach { (ry, rw) ->
                    drawRoundRect(
                        color = Color(0xFFFFECB3).copy(alpha = 0.72f),
                        topLeft = Offset(w * (0.50f - rw * 0.5f), h * ry),
                        size = Size(w * rw, h * 0.025f),
                        cornerRadius = CornerRadius(20f, 20f)
                    )
                }
                // Warm cliff & tiny lighthouse
                val cliff = Path().apply {
                    moveTo(w * 0.66f, h * 0.70f)
                    quadraticTo(w * 0.80f, h * 0.58f, w, h * 0.64f)
                    lineTo(w, h)
                    lineTo(w * 0.58f, h)
                    close()
                }
                drawPath(cliff, Color(0xFFD8B88A))
                drawRoundRect(
                    color = Color(0xFFFAF2E4),
                    topLeft = Offset(w * 0.78f, h * 0.44f),
                    size = Size(w * 0.07f, h * 0.17f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawCircle(
                    color = Color(0xFFFFF0A6),
                    radius = w * 0.03f,
                    center = Offset(w * 0.815f, h * 0.43f)
                )
            }

            "kioto_sakura" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFE5EE), Color(0xFFFED8E4), Color(0xFFCAE2C6))
                    )
                )
                // Distant hill
                val hill = Path().apply {
                    moveTo(0f, h * 0.72f)
                    quadraticTo(w * 0.50f, h * 0.50f, w, h * 0.68f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(hill, Color(0xFF8DBA85))
                // Torii pillars & crossbeam
                drawRoundRect(
                    color = Color(0xFFD14F43),
                    topLeft = Offset(w * 0.36f, h * 0.44f),
                    size = Size(w * 0.045f, h * 0.30f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color(0xFFD14F43),
                    topLeft = Offset(w * 0.595f, h * 0.44f),
                    size = Size(w * 0.045f, h * 0.30f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color(0xFFC13E33),
                    topLeft = Offset(w * 0.28f, h * 0.40f),
                    size = Size(w * 0.44f, h * 0.05f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = Color(0xFFD14F43),
                    topLeft = Offset(w * 0.32f, h * 0.49f),
                    size = Size(w * 0.36f, h * 0.032f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Sakura canopies
                listOf(
                    Triple(0.18f, 0.38f, 0.16f),
                    Triple(0.28f, 0.29f, 0.14f),
                    Triple(0.80f, 0.36f, 0.17f),
                    Triple(0.71f, 0.27f, 0.13f)
                ).forEachIndexed { idx, (cx, cy, r) ->
                    drawCircle(
                        color = if (idx % 2 == 0) Color(0xFFF6A5BF) else Color(0xFFFBC4D6),
                        radius = w * r,
                        center = Offset(w * cx, h * cy)
                    )
                }
            }

            "jungla_waterfall" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF9AE0CE), Color(0xFF459A71), Color(0xFF1F5D3D))
                    )
                )
                // Lush hills
                drawCircle(Color(0xFF2B7048), radius = w * 0.36f, center = Offset(w * 0.18f, h * 0.62f))
                drawCircle(Color(0xFF215D3B), radius = w * 0.38f, center = Offset(w * 0.84f, h * 0.64f))
                // Waterfall
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFA9F1FC), Color(0xFF5DC7DB))),
                    topLeft = Offset(w * 0.42f, h * 0.26f),
                    size = Size(w * 0.16f, h * 0.56f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                // Mist & plunge pool
                drawOval(
                    color = Color(0xFF63CEDC),
                    topLeft = Offset(w * 0.20f, h * 0.72f),
                    size = Size(w * 0.60f, h * 0.22f)
                )
                drawOval(
                    color = Color(0xFFE6FCFF).copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.36f, h * 0.74f),
                    size = Size(w * 0.28f, h * 0.09f)
                )
                // Foliage frame
                drawCircle(Color(0xFF3E8E58), radius = w * 0.14f, center = Offset(w * 0.18f, h * 0.32f))
                drawCircle(Color(0xFF4BA263), radius = w * 0.13f, center = Offset(w * 0.82f, h * 0.34f))
            }

            "toscana_picnic" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFEE5B2), Color(0xFFF6C478), Color(0xFFE5D290))
                    )
                )
                drawCircle(
                    color = Color(0xFFFFF5CC),
                    radius = w * 0.13f,
                    center = Offset(w * 0.74f, h * 0.28f)
                )
                val hill1 = Path().apply {
                    moveTo(0f, h * 0.62f)
                    quadraticTo(w * 0.35f, h * 0.42f, w, h * 0.60f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(hill1, Color(0xFF9CB460))
                val hill2 = Path().apply {
                    moveTo(0f, h * 0.74f)
                    quadraticTo(w * 0.65f, h * 0.56f, w, h * 0.76f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(hill2, Color(0xFFDFB565))
                // Cypress silhouettes
                listOf(0.24f to 0.46f, 0.31f to 0.49f, 0.78f to 0.54f).forEach { (cx, cy) ->
                    drawOval(
                        color = Color(0xFF476838),
                        topLeft = Offset(w * (cx - 0.03f), h * (cy - 0.16f)),
                        size = Size(w * 0.06f, h * 0.22f)
                    )
                }
                // Warm villa
                drawRoundRect(
                    color = Color(0xFFFAF0DA),
                    topLeft = Offset(w * 0.44f, h * 0.45f),
                    size = Size(w * 0.15f, h * 0.12f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color(0xFFC86848),
                    topLeft = Offset(w * 0.42f, h * 0.42f),
                    size = Size(w * 0.19f, h * 0.04f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            else -> {
                // atolon_stars
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0E233A), Color(0xFF1A4363), Color(0xFF236E7E))
                    )
                )
                // Crescent moon & stars
                drawCircle(
                    color = Color(0xFFFFF4C2),
                    radius = w * 0.09f,
                    center = Offset(w * 0.72f, h * 0.27f)
                )
                drawCircle(
                    color = Color(0xFF122C47),
                    radius = w * 0.075f,
                    center = Offset(w * 0.75f, h * 0.245f)
                )
                listOf(
                    0.22f to 0.22f, 0.38f to 0.16f, 0.54f to 0.28f,
                    0.30f to 0.38f, 0.84f to 0.40f, 0.16f to 0.45f
                ).forEachIndexed { idx, (sx, sy) ->
                    drawCircle(
                        color = Color.White.copy(alpha = if (idx % 2 == 0) 0.92f else 0.65f),
                        radius = w * (if (idx % 2 == 0) 0.015f else 0.010f),
                        center = Offset(w * sx, h * sy)
                    )
                }
                // Lagoon & sand cay
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF287C8D), Color(0xFF3DAEB2))),
                    topLeft = Offset(0f, h * 0.62f),
                    size = Size(w, h * 0.38f)
                )
                drawOval(
                    color = Color(0xFFF3E2BE),
                    topLeft = Offset(w * 0.16f, h * 0.70f),
                    size = Size(w * 0.68f, h * 0.20f)
                )
                // Palm arc
                drawLine(
                    color = Color(0xFF7B573C),
                    start = Offset(w * 0.34f, h * 0.76f),
                    end = Offset(w * 0.28f, h * 0.56f),
                    strokeWidth = w * 0.028f,
                    cap = StrokeCap.Round
                )
                drawArc(
                    color = Color(0xFF478E54),
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(w * 0.19f, h * 0.51f),
                    size = Size(w * 0.18f, h * 0.11f),
                    style = Stroke(width = w * 0.032f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
