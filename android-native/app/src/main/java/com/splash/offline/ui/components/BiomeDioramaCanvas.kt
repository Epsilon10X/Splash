package com.splash.offline.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val GRID_N = 18
private const val HALF_N = 9f
private const val FRAME_PAD = 0.85f
private const val FRAME_BOTTOM = -1.1f
private const val RIM_HEIGHT = 0.75f
private const val MAX_POINTS = 80
private const val PITCH_RAD = 0.60f
private const val PERSP_K = 0.0105f

private val STAGE_PRESET_POINTS = intArrayOf(0, 15, 26, 36, 46, 57, 68, 80)

private val SUN_LEN = sqrt(0.55f * 0.55f + (-0.45f) * (-0.45f) + 0.70f * 0.70f)
private val SUN_X = 0.55f / SUN_LEN
private val SUN_Y = -0.45f / SUN_LEN
private val SUN_Z = 0.70f / SUN_LEN

private data class GridSortEntry(
    val ix: Int,
    val iy: Int,
    val wx0: Float,
    val wx1: Float,
    val wy0: Float,
    val wy1: Float,
    val isFrame: Boolean
)

private data class CellPos(val x: Int, val y: Int)

private data class SceneCellData(
    val ix: Int,
    val iy: Int,
    val wz: Float,
    val color: Color,
    val isWater: Boolean,
    val isWaterfall: Boolean = false,
    val hasPotentialPath: Boolean = false,
    val isPath: Boolean = false,
    val isFloodedPaddy: Boolean = false,
    val isOasisRim: Boolean = false,
    val isVineyard: Boolean = false,
    val isLagoon: Boolean = false,
    val riverDist: Float = 1f,
    val rawH: Float = 0.8f
)

private data class BiomeLayout(
    val treeRanks: Map<Int, Int>,
    val treeTotal: Int,
    val rockRanks: Map<Int, Int>,
    val rockTotal: Int,
    val pathRanks: Map<Int, Int>,
    val pathTotal: Int,
    val tentRanks: Map<Int, Int>,
    val tentTotal: Int,
    val houseRanks: Map<Int, Int>,
    val houseTotal: Int,
    val overwaterRanks: Map<Int, Int>,
    val overwaterTotal: Int,
    val lanternRanks: Map<Int, Int>,
    val lanternTotal: Int,
    val shimmerRanks: Map<Int, Int>,
    val shimmerTotal: Int,
    val landmarkCell: CellPos?,
    val toriiCell: CellPos,
    val secondToriiCell: CellPos,
    val costaWindmillCell: CellPos,
    val desertRuinsCell: CellPos,
    val toscanaFountains: List<CellPos>,
    val secondaryCell: CellPos?
)

private data class BiomeBudgets(
    val trees: Int,
    val rocks: Int,
    val paths: Int,
    val showBridge: Boolean,
    val tents: Int,
    val houses: Int,
    val overwater: Int,
    val lanterns: Int,
    val boats: Int,
    val showMistAndPetals: Boolean,
    val monumentPhase: Int,
    val shimmers: Int,
    val showSecondary: Boolean,
    val showLegendGlow: Boolean
)

private data class BiomeSceneData(
    val biomeId: String,
    val points: Int,
    val layout: BiomeLayout,
    val budgets: BiomeBudgets,
    val cells: Array<SceneCellData>
)

@Composable
fun BiomeDioramaCanvas(
    biomeId: String,
    stage: Int,
    modifier: Modifier = Modifier,
    yawDeg: Float = 45f,
    zoom: Float = 1f,
    nightMode: Boolean = false,
    interactive: Boolean = false,
    points: Int? = null,
    onRotateDelta: ((Float) -> Unit)? = null,
    onZoomDelta: ((Float) -> Unit)? = null
) {
    val safeStage = stage.coerceIn(1, 8)
    val resolvedPoints = (points ?: STAGE_PRESET_POINTS[safeStage - 1]).coerceIn(0, MAX_POINTS)

    val scene = remember(biomeId, resolvedPoints) {
        buildBiomeScene(biomeId, resolvedPoints)
    }

    val sortedEntries = remember(yawDeg) {
        buildSortedGrid(yawDeg)
    }

    val reusablePath = remember { Path() }

    val inf = rememberInfiniteTransition(label = "diorama")
    val shimmerPhase by inf.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4600, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    val dragMod = if (interactive && (onRotateDelta != null || onZoomDelta != null)) {
        modifier.pointerInput(biomeId) {
            detectTransformGestures { _, pan, zoomChange, _ ->
                if (zoomChange != 1f) {
                    onZoomDelta?.invoke(zoomChange)
                }
                if (pan.x != 0f) {
                    onRotateDelta?.invoke(pan.x * 0.45f)
                }
            }
        }
    } else {
        modifier
    }

    Canvas(modifier = dragMod) {
        val w = size.width
        val h = size.height
        if (w <= 2f || h <= 2f) return@Canvas

        val yawRad = (yawDeg * (PI / 180.0)).toFloat()
        val cosY = cos(yawRad)
        val sinY = sin(yawRad)
        val cosP = cos(PITCH_RAD)
        val sinP = sin(PITCH_RAD)

        val baseScale = min(w * 0.0315f, h * 0.052f)
        val unitScale = baseScale * zoom
        val cx = w * 0.5f
        val cy = h * 0.52f

        fun project(wx: Float, wy: Float, wz: Float = 0f): Offset {
            val rx = wx * cosY - wy * sinY
            val ry = wx * sinY + wy * cosY
            val depth = (ry * cosP + wz * sinP).coerceIn(-26f, 26f)
            val persp = 1f / (1f - depth * PERSP_K)
            return Offset(
                x = cx + rx * unitScale * persp,
                y = cy + (ry * sinP - wz * cosP) * unitScale * persp
            )
        }

        // Ambient sky halo & subtle rainbow arc
        drawBackdropAtmosphere(
            w = w,
            h = h,
            biomeId = biomeId,
            zoom = zoom,
            nightMode = nightMode,
            stage = safeStage
        )

        // Soft 3-layer studio diagonal drop shadow under the outer architectural frame
        val shQuad = listOf(
            project(-HALF_N - FRAME_PAD, -HALF_N - FRAME_PAD, FRAME_BOTTOM),
            project(HALF_N + FRAME_PAD, -HALF_N - FRAME_PAD, FRAME_BOTTOM),
            project(HALF_N + FRAME_PAD, HALF_N + FRAME_PAD, FRAME_BOTTOM),
            project(-HALF_N - FRAME_PAD, HALF_N + FRAME_PAD, FRAME_BOTTOM)
        )
        val shadowScale = (unitScale / 11f).coerceIn(0.6f, 1.8f)
        drawPoly(reusablePath, shQuad.offsetAll(-15f * shadowScale, 11f * shadowScale), Color(0xFF1C1A17).copy(alpha = 0.05f), null)
        drawPoly(reusablePath, shQuad.offsetAll(-11f * shadowScale, 8f * shadowScale), Color(0xFF1C1A17).copy(alpha = 0.065f), null)
        drawPoly(reusablePath, shQuad.offsetAll(-7f * shadowScale, 6f * shadowScale), Color(0xFF1C1A17).copy(alpha = 0.08f), null)

        val budgets = scene.budgets
        val layout = scene.layout
        val isAwake = safeStage >= 2

        var fjordShipTargetX = 0f
        var fjordShipTargetY = 0f
        var fjordShipTargetZ = 0f
        var fjordShipSpanCount = 0

        for (entry in sortedEntries) {
            if (entry.isFrame) {
                draw3DPrism(
                    path = reusablePath,
                    project = ::project,
                    x0 = entry.wx0,
                    x1 = entry.wx1,
                    y0 = entry.wy0,
                    y1 = entry.wy1,
                    zBot = FRAME_BOTTOM,
                    zTop = RIM_HEIGHT,
                    baseColor = Color(0xFF262624),
                    isAwake = false,
                    nightMode = nightMode
                )
                continue
            }

            val ix = entry.ix
            val iy = entry.iy
            val cell = scene.cells[ix * GRID_N + iy]
            val key = cellKey(ix, iy)
            val wx0 = entry.wx0
            val wx1 = entry.wx1
            val wy0 = entry.wy0
            val wy1 = entry.wy1

            draw3DPrism(
                path = reusablePath,
                project = ::project,
                x0 = wx0,
                x1 = wx1,
                y0 = wy0,
                y1 = wy1,
                zBot = FRAME_BOTTOM + 0.08f,
                zTop = cell.wz,
                baseColor = cell.color,
                isAwake = isAwake,
                nightMode = nightMode
            )

            if (cell.isWater) {
                if (budgets.showBridge) {
                    val isBridgeTile = when (biomeId) {
                        "valle" -> ix == 9 && iy == 8
                        "kioto" -> iy == 11 && ix == 7
                        "jungla" -> ix == 10 && cell.riverDist < 0.06f
                        "costa" -> ix == 11 && iy == 8
                        "desierto" -> ix == 8 && iy == 10
                        "arrozales" -> ix == 10 && iy == 12
                        "otono" -> ix == 8 && iy == 10
                        "fiordo" -> ix == 4 && iy == 9
                        "atolon" -> ix == 9 && iy == 8
                        "toscana" -> ix == 9 && iy == 9
                        else -> false
                    }
                    if (isBridgeTile) {
                        drawBridge3D(reusablePath, ::project, biomeId, wx0, wy0, cell.wz, isAwake, nightMode)
                    }
                }

                if (biomeId == "fiordo" && resolvedPoints >= 30 && iy == 10 && ix in 10..12) {
                    if (ix == 11) {
                        fjordShipTargetX = wx0
                        fjordShipTargetY = wy0
                        fjordShipTargetZ = cell.wz
                    }
                    fjordShipSpanCount++
                    if (fjordShipSpanCount == 3) {
                        drawLargeFjordShip3D(
                            reusablePath,
                            ::project,
                            unitScale,
                            fjordShipTargetX,
                            fjordShipTargetY,
                            fjordShipTargetZ,
                            isAwake,
                            nightMode
                        )
                    }
                    continue
                }

                val isBoat1 = when (biomeId) {
                    "valle" -> ix == 9 && iy == 12
                    "arrozales" -> ix == 14 && iy == 12
                    "otono" -> ix == 10 && iy == 11
                    "atolon" -> ix == 11 && iy == 11
                    "toscana" -> ix == 13 && iy == 8
                    "jungla", "costa", "kioto" -> ix == 13 && iy == 11
                    else -> false
                }
                val isBoat2 = when (biomeId) {
                    "valle" -> ix == 8 && iy == 13
                    "otono" -> ix == 12 && iy == 11
                    "fiordo" -> ix == 15 && iy == 11
                    "atolon" -> ix == 14 && iy == 13
                    "toscana" -> ix == 5 && iy == 11
                    "jungla", "costa", "kioto" -> ix == 14 && iy == 6
                    else -> false
                }
                if ((budgets.boats >= 1 && isBoat1) || (budgets.boats >= 2 && isBoat2)) {
                    drawBoat3D(reusablePath, ::project, biomeId, wx0, wy0, cell.wz, isAwake, nightMode)
                }

                if (biomeId == "atolon" && (layout.overwaterRanks[key] ?: Int.MAX_VALUE) <= budgets.overwater) {
                    drawOverwaterBungalow3D(reusablePath, ::project, wx0, wy0, cell.wz, isAwake, nightMode)
                    continue
                }

                if ((layout.shimmerRanks[key] ?: Int.MAX_VALUE) <= budgets.shimmers) {
                    val pulse = 0.65f + 0.35f * sin(shimmerPhase + ix * 0.7f + iy * 0.5f)
                    val w1 = project(wx0 + 0.25f, wy0 + 0.50f, cell.wz + 0.02f)
                    val w2 = project(wx0 + 0.72f, wy0 + 0.50f, cell.wz + 0.02f)
                    drawLine(
                        color = Color.White.copy(alpha = (0.48f * pulse).coerceIn(0.18f, 0.75f)),
                        start = w1,
                        end = w2,
                        strokeWidth = max(0.9f, unitScale * 0.08f),
                        cap = StrokeCap.Round
                    )
                }
                continue
            }

            if (cell.isWaterfall || cell.isPath) continue

            val isLandmarkPos = layout.landmarkCell?.let { it.x == ix && it.y == iy } == true
            val isToriiPos = biomeId == "kioto" && ix == layout.toriiCell.x && iy == layout.toriiCell.y
            val isSecondTorii = biomeId == "kioto" && budgets.showSecondary && ix == layout.secondToriiCell.x && iy == layout.secondToriiCell.y
            val isCostaWindmill = biomeId == "costa" && budgets.showSecondary && ix == layout.costaWindmillCell.x && iy == layout.costaWindmillCell.y
            val isDesertRuins = biomeId == "desierto" && budgets.showSecondary && ix == layout.desertRuinsCell.x && iy == layout.desertRuinsCell.y
            val isCustomSecondary = budgets.showSecondary && !isCostaWindmill && !isDesertRuins &&
                layout.secondaryCell?.let { it.x == ix && it.y == iy } == true

            if (budgets.monumentPhase >= 1 && isLandmarkPos) {
                drawLandmark3D(
                    reusablePath,
                    ::project,
                    unitScale,
                    biomeId,
                    wx0,
                    wy0,
                    cell.wz,
                    budgets.monumentPhase,
                    budgets.showLegendGlow,
                    isAwake,
                    nightMode
                )
                continue
            }
            if (isCostaWindmill) {
                drawWindmill3D(reusablePath, ::project, unitScale, wx0, wy0, cell.wz, isAwake, nightMode)
                continue
            }
            if (isDesertRuins) {
                drawDesertRuins3D(reusablePath, ::project, wx0, wy0, cell.wz, isAwake, nightMode)
                continue
            }
            if (isCustomSecondary) {
                drawSecondaryLandmark3D(reusablePath, ::project, unitScale, biomeId, wx0, wy0, cell.wz, isAwake, nightMode)
                continue
            }
            if ((budgets.monumentPhase >= 2 && isToriiPos) || isSecondTorii) {
                drawTorii3D(::project, unitScale, wx0, wy0, cell.wz)
                continue
            }
            if (biomeId == "toscana") {
                val isFountain1 = ix == layout.toscanaFountains[0].x && iy == layout.toscanaFountains[0].y && resolvedPoints >= 24
                val isFountain2 = ix == layout.toscanaFountains[1].x && iy == layout.toscanaFountains[1].y && resolvedPoints >= 46
                if (isFountain1 || isFountain2) {
                    drawTuscanFountain3D(reusablePath, ::project, unitScale, wx0, wy0, cell.wz, isAwake, nightMode)
                    continue
                }
            }

            if ((layout.tentRanks[key] ?: Int.MAX_VALUE) <= budgets.tents) {
                drawDesertTent3D(reusablePath, ::project, wx0, wy0, cell.wz, ix + iy, isAwake, nightMode)
                continue
            }
            if ((layout.houseRanks[key] ?: Int.MAX_VALUE) <= budgets.houses) {
                val bh = 0.36f + cellHash(ix, iy, 7) * 0.38f
                drawSettlementHouse3D(reusablePath, ::project, biomeId, wx0, wy0, cell.wz, 0.56f, bh, isAwake, nightMode)
                continue
            }
            if ((layout.lanternRanks[key] ?: Int.MAX_VALUE) <= budgets.lanterns) {
                drawLantern3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, isAwake || nightMode)
                continue
            }
            if ((layout.rockRanks[key] ?: Int.MAX_VALUE) <= budgets.rocks) {
                val rockColor = when (biomeId) {
                    "jungla" -> Color(0xFF637563)
                    "desierto" -> Color(0xFFD4B284)
                    "fiordo" -> Color(0xFF7C8C99)
                    "toscana" -> Color(0xFFDCD2BE)
                    "arrozales" -> Color(0xFF797E71)
                    "otono" -> Color(0xFF8C8074)
                    "atolon" -> Color(0xFFDCD6C8)
                    else -> Color(0xFFC9C4B8)
                }
                drawMicroRock3D(reusablePath, ::project, wx0, wy0, cell.wz, 0.85f, rockColor, isAwake, nightMode)
                continue
            }

            if ((layout.treeRanks[key] ?: Int.MAX_VALUE) <= budgets.trees) {
                when (biomeId) {
                    "kioto" -> {
                        val scale = 0.78f + cellHash(ix, iy, 63) * 0.24f
                        drawBlossomTree3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, scale, ix + iy, isAwake, nightMode)
                    }
                    "jungla" -> {
                        val jKind = cellHash(ix, iy, 83)
                        if (jKind > 0.72f) {
                            drawBamboo3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, 0.96f, isAwake, nightMode)
                        } else {
                            val scale = 0.88f + cellHash(ix, iy, 63) * 0.24f
                            drawJungleTree3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, scale, isAwake, nightMode)
                        }
                    }
                    "valle" -> {
                        drawMicroPine3D(reusablePath, ::project, wx0 + 0.5f, wy0 + 0.5f, cell.wz, 0.90f, Color(0xFF3A7042), isAwake, nightMode)
                    }
                    "costa" -> {
                        val kind = cellHash(ix, iy, 61)
                        when {
                            kind > 0.56f -> drawBougainvillea3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, isAwake, nightMode)
                            kind > 0.28f -> drawCypress3D(reusablePath, ::project, wx0 + 0.5f, wy0 + 0.5f, cell.wz, 0.95f, isAwake, nightMode)
                            else -> drawMicroCanopy3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, 0.21f, Color(0xFF809472), isAwake, nightMode)
                        }
                    }
                    "desierto" -> {
                        if (cell.isOasisRim) {
                            drawPalmTree3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, 0.88f, isAwake, nightMode, Color(0xFFC89F63))
                        }
                    }
                    "arrozales" -> {
                        val kind = cellHash(ix, iy, 65)
                        when {
                            kind > 0.25f -> drawBamboo3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, 1.02f, isAwake, nightMode)
                            kind > 0.10f -> drawPalmTree3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, 0.90f, isAwake, nightMode, Color(0xFF4B9650))
                            else -> drawFrangipani3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, 0.92f, isAwake, nightMode)
                        }
                    }
                    "otono" -> {
                        drawMapleTree3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, ix * 3 + iy, isAwake, nightMode)
                    }
                    "fiordo" -> {
                        drawSnowPine3D(reusablePath, ::project, wx0 + 0.5f, wy0 + 0.5f, cell.wz, 0.94f, isAwake, nightMode, Color(0xFF5B7B8F))
                    }
                    "atolon" -> {
                        if (cellHash(ix, iy, 69) > 0.36f) {
                            drawPalmTree3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, 0.96f, isAwake, nightMode, Color(0xFF568F5A))
                        } else {
                            drawHibiscus3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, isAwake, nightMode)
                        }
                    }
                    "toscana" -> {
                        when {
                            cell.isVineyard -> drawVineyardTuft3D(reusablePath, ::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, isAwake, nightMode)
                            cellHash(ix, iy, 71) > 0.44f -> drawCypress3D(reusablePath, ::project, wx0 + 0.5f, wy0 + 0.5f, cell.wz, 1.02f, isAwake, nightMode)
                            else -> drawMicroCanopy3D(::project, unitScale, wx0 + 0.5f, wy0 + 0.5f, cell.wz, 0.27f, Color(0xFF758E4F), isAwake, nightMode)
                        }
                    }
                }
            }
        }

        // Crisp upper architectural frame bevel hairline
        val r00 = project(-HALF_N - FRAME_PAD, -HALF_N - FRAME_PAD, RIM_HEIGHT)
        val r10 = project(HALF_N + FRAME_PAD, -HALF_N - FRAME_PAD, RIM_HEIGHT)
        val r11 = project(HALF_N + FRAME_PAD, HALF_N + FRAME_PAD, RIM_HEIGHT)
        val r01 = project(-HALF_N - FRAME_PAD, HALF_N + FRAME_PAD, RIM_HEIGHT)
        reusablePath.reset()
        reusablePath.moveTo(r00.x, r00.y)
        reusablePath.lineTo(r10.x, r10.y)
        reusablePath.lineTo(r11.x, r11.y)
        reusablePath.lineTo(r01.x, r01.y)
        reusablePath.close()
        drawPath(
            path = reusablePath,
            color = Color.White.copy(alpha = 0.16f),
            style = Stroke(width = 1.0f, join = StrokeJoin.Round)
        )

        if (biomeId == "jungla") {
            drawJungleWaterfallDetail(
                unitScale = unitScale,
                phase = shimmerPhase,
                nightMode = nightMode,
                project = ::project
            )
        }

        // Subtle atmospheric particles / night fireflies
        drawAtmosphericParticles(
            w = w,
            h = h,
            biomeId = biomeId,
            resolvedPoints = resolvedPoints,
            budgets = budgets,
            zoom = zoom,
            unitScale = unitScale,
            nightMode = nightMode,
            phase = shimmerPhase,
            project = ::project
        )
    }
}

private fun DrawScope.drawBackdropAtmosphere(
    w: Float,
    h: Float,
    biomeId: String,
    zoom: Float,
    nightMode: Boolean,
    stage: Int
) {
    val minDim = min(w, h)
    val glowColor = if (nightMode) {
        Color(0xFF688CD6)
    } else {
        when (biomeId) {
            "valle", "costa", "toscana" -> Color(0xFFF8E4B6)
            "kioto" -> Color(0xFFFADEE8)
            "desierto" -> Color(0xFFFADAAE)
            "jungla" -> Color(0xFFB6E0D6)
            "fiordo" -> Color(0xFFAAE8E0)
            "otono" -> Color(0xFFF6CEA8)
            "arrozales" -> Color(0xFFCEEEC6)
            "atolon" -> Color(0xFFB6F2F2)
            else -> Color(0xFFF8E4B6)
        }
    }

    val auraCenter = Offset(w * 0.5f, h * 0.45f)
    val auraRadius = minDim * 0.48f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = if (nightMode) 0.24f else 0.28f),
                glowColor.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = auraCenter,
            radius = auraRadius
        ),
        radius = auraRadius,
        center = auraCenter
    )

    if (nightMode) {
        for (i in 0 until 14) {
            val sx = w * (0.08f + cellHash(i, 1, 131) * 0.84f)
            val sy = h * (0.06f + cellHash(i, 2, 137) * 0.30f)
            drawCircle(
                color = Color(0xFFE8F0FF).copy(alpha = 0.45f),
                radius = 1.4f,
                center = Offset(sx, sy)
            )
        }
    } else {
        // Soft pastel rainbow halo behind the diorama (visible at all stages cleanly, richer when evolved)
        val cx = w * 0.5f
        val cy = h * 0.56f
        val baseR = minDim * 0.44f * min(1.18f, zoom)
        val bands = listOf(
            Color(0xFFEB8676),
            Color(0xFFF5C66C),
            Color(0xFF7ECC92),
            Color(0xFF78B8E8)
        )
        val alpha = if (stage >= 2) 0.17f else 0.12f
        val bandGap = 3.6f * zoom
        bands.forEachIndexed { idx, col ->
            val r = (baseR - idx * bandGap).coerceAtLeast(16f)
            drawArc(
                color = col.copy(alpha = alpha),
                startAngle = 204f,
                sweepAngle = 132f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = max(2.2f, 3.2f * zoom), cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawAtmosphericParticles(
    w: Float,
    h: Float,
    biomeId: String,
    resolvedPoints: Int,
    budgets: BiomeBudgets,
    zoom: Float,
    unitScale: Float,
    nightMode: Boolean,
    phase: Float,
    project: (Float, Float, Float) -> Offset
) {
    val time = phase / (2f * PI.toFloat())
    if (nightMode) {
        for (i in 0 until 14) {
            val fx = (cellHash(i, 3, 41) - 0.5f) * 14.5f
            val fy = (cellHash(i, 7, 53) - 0.5f) * 14.5f
            val fz = 1.1f + cellHash(i, 11, 61) * 1.8f
            val p = project(fx, fy, fz)
            val pulse = 0.55f + 0.45f * sin(phase + i * 1.4f)
            val r = max(3.5f, unitScale * 0.55f) * (0.85f + pulse * 0.35f)
            drawCircle(
                color = Color(0xFFFFEE9E).copy(alpha = 0.28f * pulse),
                radius = r * 1.9f,
                center = p
            )
            drawCircle(
                color = Color(0xFFFFEE9E).copy(alpha = 0.85f * pulse),
                radius = r * 0.45f,
                center = p
            )
        }
        return
    }

    if (biomeId == "jungla") {
        val dropCount = if (budgets.showMistAndPetals) 22 else 12 + resolvedPoints / 8
        for (i in 0 until dropCount) {
            val sx = cellHash(i, 1, 77)
            val sy = cellHash(i, 2, 88)
            val speed = 0.85f + (i % 4) * 0.18f
            val prog = (sy + time * speed) % 1f
            val rx = w * (0.20f + sx * 0.60f) - prog * 14f * zoom
            val ry = h * (0.14f + prog * 0.66f)
            val len = (5f + (i % 2) * 2.5f) * min(1.25f, zoom)
            drawLine(
                color = Color(0xFFD2EEF3).copy(alpha = 0.34f),
                start = Offset(rx, ry),
                end = Offset(rx - len * 0.22f, ry + len),
                strokeWidth = 1.0f,
                cap = StrokeCap.Round
            )
        }
    } else if (biomeId == "fiordo") {
        val snowCount = if (budgets.showMistAndPetals) 22 else 10 + resolvedPoints / 8
        for (i in 0 until snowCount) {
            val sx = cellHash(i, 9, 91)
            val sy = cellHash(i, 10, 97)
            val prog = (sy + time * (0.6f + (i % 3) * 0.2f)) % 1f
            val fx = w * (0.16f + sx * 0.68f) + sin(phase + i) * 6f * zoom
            val fy = h * (0.14f + prog * 0.68f)
            drawCircle(
                color = Color(0xFFF4F9FF).copy(alpha = 0.78f),
                radius = 1.6f * min(1.3f, zoom),
                center = Offset(fx, fy)
            )
        }
    } else {
        // Gentle floating motes of sunlight across all other biomes
        val moteCount = if (budgets.showMistAndPetals) 12 else 6
        val moteColor = when (biomeId) {
            "kioto" -> Color(0xFFF7CED9)
            "otono" -> Color(0xFFEBA844)
            "atolon" -> Color.White
            else -> Color(0xFFFFE892)
        }
        for (i in 0 until moteCount) {
            val sx = cellHash(i, 5, 101)
            val sy = cellHash(i, 8, 107)
            val pulse = 0.45f + 0.55f * sin(phase + i * 1.7f)
            val px = w * (0.20f + sx * 0.60f) + sin(phase + i) * 5f * zoom
            val py = h * (0.18f + sy * 0.46f) + cos(phase * 0.8f + i) * 4f * zoom
            drawCircle(
                color = moteColor.copy(alpha = 0.68f * pulse),
                radius = (1.6f + pulse * 1.1f) * min(1.25f, zoom),
                center = Offset(px, py)
            )
        }
    }
}

private fun List<Offset>.offsetAll(dx: Float, dy: Float): List<Offset> =
    List(size) { i -> Offset(this[i].x + dx, this[i].y + dy) }

private fun buildSortedGrid(yawDeg: Float): List<GridSortEntry> {
    val yawRad = (yawDeg * (PI / 180.0)).toFloat()
    val cosY = cos(yawRad)
    val sinY = sin(yawRad)
    val cosP = cos(PITCH_RAD)
    val camDistY = (1f / PERSP_K) * cosP

    val items = ArrayList<Pair<GridSortEntry, Float>>((GRID_N + 2) * (GRID_N + 2))
    for (ix in -1..GRID_N) {
        val wx0 = when (ix) {
            -1 -> -HALF_N - FRAME_PAD
            GRID_N -> HALF_N
            else -> ix - HALF_N
        }
        val wx1 = when (ix) {
            -1 -> -HALF_N
            GRID_N -> HALF_N + FRAME_PAD
            else -> ix - HALF_N + 1f
        }
        val mx = (wx0 + wx1) * 0.5f
        for (iy in -1..GRID_N) {
            val wy0 = when (iy) {
                -1 -> -HALF_N - FRAME_PAD
                GRID_N -> HALF_N
                else -> iy - HALF_N
            }
            val wy1 = when (iy) {
                -1 -> -HALF_N
                GRID_N -> HALF_N + FRAME_PAD
                else -> iy - HALF_N + 1f
            }
            val my = (wy0 + wy1) * 0.5f
            val rx = mx * cosY - my * sinY
            val ry = mx * sinY + my * cosY
            val distSq = rx * rx + (ry - camDistY) * (ry - camDistY)
            val isFrame = ix == -1 || ix == GRID_N || iy == -1 || iy == GRID_N
            items.add(GridSortEntry(ix, iy, wx0, wx1, wy0, wy1, isFrame) to distSq)
        }
    }
    items.sortByDescending { it.second }
    return items.map { it.first }
}

private fun cellKey(x: Int, y: Int): Int = (x shl 8) or (y and 0xFF)

private fun cellHash(x: Int, y: Int, seed: Int = 17): Float {
    val n = sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453123
    return (n - floor(n)).toFloat()
}

private fun shadeColor(base: Color, percent: Float, warmBoost: Float, nightMode: Boolean): Color {
    val factor = (1f + percent / 100f) * (if (nightMode) 0.72f else 1f)
    val wb = warmBoost / 255f
    return Color(
        red = (base.red * factor + wb).coerceIn(0f, 1f),
        green = (base.green * factor + wb * 0.8f).coerceIn(0f, 1f),
        blue = (base.blue * factor + wb * 0.35f).coerceIn(0f, 1f),
        alpha = base.alpha
    )
}

private fun lambertShade(base: Color, nx: Float, ny: Float, nz: Float, isAwake: Boolean, nightMode: Boolean): Color {
    val dot = nx * SUN_X + ny * SUN_Y + nz * SUN_Z
    val pct = dot * 26f - 10f
    val warm = if (isAwake && dot > 0.2f && !nightMode) 9f else 0f
    return shadeColor(base, pct, warm, nightMode)
}

private fun screenSignedArea(pts: List<Offset>): Float {
    var area = 0f
    for (i in pts.indices) {
        val a = pts[i]
        val b = pts[(i + 1) % pts.size]
        area += a.x * b.y - b.x * a.y
    }
    return area
}

private fun DrawScope.drawPoly(
    path: Path,
    pts: List<Offset>,
    fill: Color,
    strokeColor: Color?,
    lineWidth: Float = 0.85f
) {
    path.reset()
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
        path.lineTo(pts[i].x, pts[i].y)
    }
    path.close()
    drawPath(path = path, color = fill)
    if (strokeColor != null) {
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = lineWidth, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.draw3DPrism(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    x0: Float,
    x1: Float,
    y0: Float,
    y1: Float,
    zBot: Float,
    zTop: Float,
    baseColor: Color,
    isAwake: Boolean,
    nightMode: Boolean,
    strokeJoin: Boolean = true
) {
    val t00 = project(x0, y0, zTop)
    val t10 = project(x1, y0, zTop)
    val t11 = project(x1, y1, zTop)
    val t01 = project(x0, y1, zTop)

    val b00 = project(x0, y0, zBot)
    val b10 = project(x1, y0, zBot)
    val b11 = project(x1, y1, zBot)
    val b01 = project(x0, y1, zBot)

    drawFaceIfVisible(path, listOf(t10, b10, b11, t11), 1f, 0f, 0f, baseColor, isAwake, nightMode, strokeJoin)
    drawFaceIfVisible(path, listOf(t01, b01, b00, t00), -1f, 0f, 0f, baseColor, isAwake, nightMode, strokeJoin)
    drawFaceIfVisible(path, listOf(t11, b11, b01, t01), 0f, 1f, 0f, baseColor, isAwake, nightMode, strokeJoin)
    drawFaceIfVisible(path, listOf(t00, b00, b10, t10), 0f, -1f, 0f, baseColor, isAwake, nightMode, strokeJoin)
    drawFaceIfVisible(path, listOf(t00, t10, t11, t01), 0f, 0f, 1f, baseColor, isAwake, nightMode, strokeJoin)
}

private fun DrawScope.drawFaceIfVisible(
    path: Path,
    pts: List<Offset>,
    nx: Float,
    ny: Float,
    nz: Float,
    baseColor: Color,
    isAwake: Boolean,
    nightMode: Boolean,
    strokeJoin: Boolean
) {
    if (screenSignedArea(pts) > 0.02f) {
        val col = lambertShade(baseColor, nx, ny, nz, isAwake, nightMode)
        drawPoly(path, pts, col, if (strokeJoin) col else null, 0.9f)
    }
}

private fun DrawScope.drawArchBlock3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    wx0: Float,
    wy0: Float,
    zBase: Float,
    frac: Float,
    heightUnits: Float,
    wallColor: Color,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val pad = (1f - frac) * 0.5f
    draw3DPrism(
        path = path,
        project = project,
        x0 = wx0 + pad,
        x1 = wx0 + 1f - pad,
        y0 = wy0 + pad,
        y1 = wy0 + 1f - pad,
        zBot = zBase,
        zTop = zBase + heightUnits,
        baseColor = wallColor,
        isAwake = isAwake,
        nightMode = nightMode,
        strokeJoin = true
    )
}

private fun DrawScope.drawPyramidRoof(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    cx: Float,
    cy: Float,
    zBase: Float,
    radius: Float,
    height: Float,
    baseColor: Color,
    strokeOverride: Color? = null,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val apex = project(cx, cy, zBase + height)
    val c0 = project(cx - radius, cy - radius, zBase)
    val c1 = project(cx + radius, cy - radius, zBase)
    val c2 = project(cx + radius, cy + radius, zBase)
    val c3 = project(cx - radius, cy + radius, zBase)
    val sides = listOf(
        Triple(listOf(apex, c0, c1), 0f, -0.78f),
        Triple(listOf(apex, c1, c2), 0.78f, 0f),
        Triple(listOf(apex, c2, c3), 0f, 0.78f),
        Triple(listOf(apex, c3, c0), -0.78f, 0f)
    )
    for ((pts, nx, ny) in sides) {
        if (screenSignedArea(pts) > 0.01f) {
            val col = lambertShade(baseColor, nx, ny, 0.60f, isAwake, nightMode)
            drawPoly(path, pts, col, strokeOverride ?: col, 0.55f)
        }
    }
}

private fun DrawScope.drawMicroRock3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    wx0: Float,
    wy0: Float,
    zBase: Float,
    scale: Float,
    rockColor: Color,
    isAwake: Boolean,
    nightMode: Boolean
) {
    draw3DPrism(
        path = path,
        project = project,
        x0 = wx0 + 0.26f,
        x1 = wx0 + 0.72f,
        y0 = wy0 + 0.28f,
        y1 = wy0 + 0.70f,
        zBot = zBase,
        zTop = zBase + 0.22f * scale,
        baseColor = rockColor,
        isAwake = isAwake,
        nightMode = nightMode
    )
}

private fun DrawScope.drawMicroPine3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    cx: Float,
    cy: Float,
    zBase: Float,
    scale: Float,
    baseColor: Color,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val sh = project(cx - 0.1f, cy + 0.1f, zBase + 0.02f)
    drawOval(
        color = Color(0xFF1C1A17).copy(alpha = 0.14f),
        topLeft = Offset(sh.x - 4.0f * scale, sh.y - 2.0f * scale),
        size = Size(8.0f * scale, 4.0f * scale)
    )
    drawPyramidRoof(
        path = path,
        project = project,
        cx = cx,
        cy = cy,
        zBase = zBase + 0.06f,
        radius = 0.35f * scale,
        height = 0.96f * scale,
        baseColor = baseColor,
        isAwake = isAwake,
        nightMode = nightMode
    )
}

private fun DrawScope.drawSnowPine3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    cx: Float,
    cy: Float,
    zBase: Float,
    scale: Float,
    isAwake: Boolean,
    nightMode: Boolean,
    baseColor: Color
) {
    drawMicroPine3D(path, project, cx, cy, zBase, scale, baseColor, isAwake, nightMode)
    drawPyramidRoof(
        path = path,
        project = project,
        cx = cx,
        cy = cy,
        zBase = zBase + 0.48f * scale,
        radius = 0.26f * scale,
        height = 0.70f * scale,
        baseColor = Color(0xFFF2F7FB),
        strokeOverride = Color(0xFFDCE8F2),
        isAwake = isAwake,
        nightMode = nightMode
    )
}

private fun DrawScope.drawCypress3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    cx: Float,
    cy: Float,
    zBase: Float,
    scale: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    drawPyramidRoof(
        path = path,
        project = project,
        cx = cx,
        cy = cy,
        zBase = zBase + 0.12f,
        radius = 0.20f * scale,
        height = 1.13f * scale,
        baseColor = Color(0xFF3E6242),
        isAwake = isAwake,
        nightMode = nightMode
    )
}

private fun DrawScope.drawMicroCanopy3D(
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    cx: Float,
    cy: Float,
    zBase: Float,
    rad: Float,
    color: Color,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val base = project(cx, cy, zBase)
    val top = project(cx, cy, zBase + rad * 1.9f)
    val pxR = max(2.0f, rad * unitScale)

    drawOval(
        color = Color(0xFF1C1A17).copy(alpha = 0.12f),
        topLeft = Offset(base.x - 1f - pxR * 0.9f, base.y + 0.5f - pxR * 0.45f),
        size = Size(pxR * 1.8f, pxR * 0.9f)
    )
    drawLine(
        color = Color(0xFF6E5947),
        start = base,
        end = top,
        strokeWidth = max(1.2f, pxR * 0.34f),
        cap = StrokeCap.Round
    )
    drawCircle(
        color = lambertShade(color, 0f, 0f, 1f, isAwake, nightMode),
        radius = pxR,
        center = top
    )
    drawCircle(
        color = shadeColor(color, 12f, if (isAwake) 8f else 0f, nightMode),
        radius = pxR * 0.5f,
        center = Offset(top.x + pxR * 0.25f, top.y - pxR * 0.25f)
    )
}

private fun DrawScope.drawBlossomTree3D(
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    cx: Float,
    cy: Float,
    zBase: Float,
    scale: Float,
    variant: Int,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val base = project(cx, cy, zBase)
    val top = project(cx, cy, zBase + 0.62f * scale)
    val pxR = max(2.2f, 0.29f * scale * unitScale)

    drawOval(
        color = Color(0xFF1C1A17).copy(alpha = 0.13f),
        topLeft = Offset(base.x - 1f - pxR * 1.15f, base.y + 0.5f - pxR * 0.55f),
        size = Size(pxR * 2.3f, pxR * 1.1f)
    )
    drawLine(
        color = Color(0xFF634E42),
        start = base,
        end = top,
        strokeWidth = max(1.3f, pxR * 0.36f),
        cap = StrokeCap.Round
    )

    val pal = when (abs(variant) % 3) {
        0 -> Triple(Color(0xFFE495AB), Color(0xFFF7CED9), Color(0xFFFDE6EC))
        1 -> Triple(Color(0xFFDC89A1), Color(0xFFEEB0C1), Color(0xFFFBE0E8))
        else -> Triple(Color(0xFFE69EB3), Color(0xFFF5C4D2), Color(0xFFFFF0F4))
    }
    drawCircle(
        color = lambertShade(pal.first, -0.4f, 0.3f, 0.85f, isAwake, nightMode),
        radius = pxR * 0.82f,
        center = Offset(top.x - pxR * 0.48f, top.y + pxR * 0.14f)
    )
    drawCircle(
        color = lambertShade(pal.second, 0.3f, -0.2f, 0.90f, isAwake, nightMode),
        radius = pxR * 0.80f,
        center = Offset(top.x + pxR * 0.45f, top.y + pxR * 0.08f)
    )
    drawCircle(
        color = lambertShade(pal.second, 0f, 0f, 1f, isAwake, nightMode),
        radius = pxR * 0.96f,
        center = Offset(top.x, top.y - pxR * 0.24f)
    )
    drawCircle(
        color = pal.third,
        radius = pxR * 0.44f,
        center = Offset(top.x + pxR * 0.22f, top.y - pxR * 0.44f)
    )
}

private fun DrawScope.drawPalmTree3D(
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    cx: Float,
    cy: Float,
    zBase: Float,
    scale: Float,
    isAwake: Boolean,
    nightMode: Boolean,
    frondColor: Color
) {
    val base = project(cx, cy, zBase)
    val mid = project(cx + 0.10f, cy - 0.05f, zBase + 0.48f * scale)
    val crown = project(cx + 0.18f, cy - 0.08f, zBase + 0.92f * scale)
    val trunkW = max(1.3f, unitScale * 0.11f * scale)
    drawLine(Color(0xFF8B6C4E), base, mid, strokeWidth = trunkW, cap = StrokeCap.Round)
    drawLine(Color(0xFF8B6C4E), mid, crown, strokeWidth = trunkW, cap = StrokeCap.Round)

    val litFrond = lambertShade(frondColor, 0.1f, -0.1f, 0.95f, isAwake, nightMode)
    val frondW = max(1.4f, unitScale * 0.10f * scale)
    val dirs = listOf(
        Triple(0.38f, 0.12f, -0.14f),
        Triple(-0.34f, 0.22f, -0.16f),
        Triple(0.14f, -0.36f, -0.14f),
        Triple(-0.18f, -0.32f, -0.12f)
    )
    for ((dx, dy, dz) in dirs) {
        val tip = project(cx + 0.18f + dx * scale, cy - 0.08f + dy * scale, zBase + (0.92f + dz) * scale)
        drawLine(litFrond, crown, tip, strokeWidth = frondW, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawJungleWaterfallDetail(
    unitScale: Float,
    phase: Float,
    nightMode: Boolean,
    project: (Float, Float, Float) -> Offset
) {
    // Three silky animated cascading water ribbons down the diagonal chute (-7.5..-2.0, -7.5..-2.0)
    val ribbons = floatArrayOf(-0.24f, 0f, 0.24f)
    ribbons.forEachIndexed { idx, offset ->
        val topPos = project(-6.2f + offset, -6.2f - offset, 2.35f)
        val midPos = project(-4.2f + offset, -4.2f - offset, 1.45f)
        val poolPos = project(-2.1f + offset * 0.8f, -2.1f - offset * 0.8f, 0.42f)
        val pulse = 0.65f + 0.35f * sin(phase * 1.4f + idx * 1.1f)
        val ribbonCol = if (idx == 1) {
            Color(0xFFF2FDFF).copy(alpha = (if (nightMode) 0.50f else 0.78f) * pulse)
        } else {
            Color(0xFF9BEBF5).copy(alpha = (if (nightMode) 0.40f else 0.62f) * pulse)
        }
        val widthPx = max(1.6f, unitScale * (if (idx == 1) 0.22f else 0.14f))
        drawLine(ribbonCol, topPos, midPos, strokeWidth = widthPx, cap = StrokeCap.Round)
        drawLine(ribbonCol, midPos, poolPos, strokeWidth = widthPx, cap = StrokeCap.Round)
    }

    // Concentric gentle plunge pool ripples
    val plungeCenter = project(-1.7f, -1.7f, 0.39f)
    val ripplePulse = (sin(phase * 1.1f) + 1f) * 0.5f
    for (rIdx in 0..1) {
        val rr = max(3.5f, unitScale * (0.48f + rIdx * 0.34f + ripplePulse * 0.14f))
        drawOval(
            color = Color(0xFFE6FAFD).copy(alpha = if (nightMode) 0.22f else (0.36f - rIdx * 0.12f)),
            topLeft = Offset(plungeCenter.x - rr, plungeCenter.y - rr * 0.56f),
            size = Size(rr * 2f, rr * 1.12f),
            style = Stroke(width = max(1.1f, unitScale * 0.08f))
        )
    }

    // Soft translucent mist puffs at the base of the cascade
    val mistOffsets = listOf(-0.45f to 0.25f, 0.15f to -0.35f, 0.35f to 0.35f)
    mistOffsets.forEachIndexed { idx, (dx, dy) ->
        val drift = sin(phase * 0.8f + idx * 1.8f) * 0.14f
        val mp = project(-2.1f + dx + drift, -2.1f + dy - drift, 0.54f + idx * 0.08f)
        val mr = max(3.6f, unitScale * (0.38f + (idx % 2) * 0.12f))
        drawCircle(
            color = Color(0xFFF0FAFC).copy(alpha = if (nightMode) 0.14f else 0.30f),
            radius = mr,
            center = mp
        )
    }
}

private fun DrawScope.drawJungleTree3D(
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    cx: Float,
    cy: Float,
    zBase: Float,
    scale: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val base = project(cx, cy, zBase)
    val top1 = project(cx, cy, zBase + 0.62f * scale)
    val top2 = project(cx + 0.05f, cy - 0.04f, zBase + 0.90f * scale)
    val pxR = max(2.6f, 0.35f * scale * unitScale)

    drawLine(Color(0xFF665140), base, top2, strokeWidth = max(1.4f, pxR * 0.28f), cap = StrokeCap.Round)
    drawCircle(
        color = lambertShade(Color(0xFF286944), -0.2f, 0.2f, 0.9f, isAwake, nightMode),
        radius = pxR * 0.96f,
        center = top1
    )
    drawCircle(
        color = lambertShade(Color(0xFF3E8C5D), 0f, 0f, 1f, isAwake, nightMode),
        radius = pxR * 0.82f,
        center = top2
    )
    drawCircle(
        color = shadeColor(Color(0xFF63B882), 10f, if (isAwake) 7f else 0f, nightMode),
        radius = pxR * 0.46f,
        center = Offset(top2.x + pxR * 0.18f, top2.y - pxR * 0.16f)
    )
}

private fun DrawScope.drawBamboo3D(
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    cx: Float,
    cy: Float,
    zBase: Float,
    scale: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val lw = max(1.3f, unitScale * 0.10f)
    val stalks = listOf(
        Triple(-0.14f, -0.06f, 1.05f * scale),
        Triple(0.12f, 0.08f, 1.24f * scale),
        Triple(-0.02f, 0.15f, 0.90f * scale)
    )
    stalks.forEachIndexed { idx, st ->
        val p0 = project(cx + st.first, cy + st.second, zBase)
        val p1 = project(cx + st.first * 1.15f, cy + st.second * 1.15f, zBase + st.third)
        val stalkCol = lambertShade(
            if (idx % 2 == 0) Color(0xFF74B25C) else Color(0xFF5E9E4B),
            0.1f, -0.1f, 0.9f, isAwake, nightMode
        )
        drawLine(stalkCol, p0, p1, strokeWidth = lw, cap = StrokeCap.Round)
        drawCircle(
            color = lambertShade(Color(0xFF8CCF72), 0f, 0f, 1f, isAwake, nightMode),
            radius = max(1.6f, 0.14f * unitScale * scale),
            center = p1
        )
    }
}

private fun DrawScope.drawFrangipani3D(
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    cx: Float,
    cy: Float,
    zBase: Float,
    scale: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val trunkB = project(cx, cy, zBase)
    val trunkT = project(cx, cy, zBase + 0.44f * scale)
    drawLine(Color(0xFF7C5D43), trunkB, trunkT, strokeWidth = max(1.5f, unitScale * 0.12f), cap = StrokeCap.Round)
    val r = max(2.6f, 0.30f * unitScale * scale)
    drawCircle(lambertShade(Color(0xFF5EA152), 0.05f, -0.05f, 0.95f, isAwake, nightMode), r, trunkT)
    drawCircle(Color(0xFFFCF9EE), r * 0.36f, Offset(trunkT.x - r * 0.32f, trunkT.y - r * 0.18f))
    drawCircle(Color(0xFFFCF9EE), r * 0.30f, Offset(trunkT.x + r * 0.34f, trunkT.y - r * 0.08f))
    drawCircle(Color(0xFFF3CF55), r * 0.14f, Offset(trunkT.x - r * 0.32f, trunkT.y - r * 0.18f))
}

private fun DrawScope.drawMapleTree3D(
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    cx: Float,
    cy: Float,
    zBase: Float,
    variant: Int,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val palette = listOf(
        Color(0xFFD9342B),
        Color(0xFFF27B24),
        Color(0xFFF8C73A),
        Color(0xFFE84F32),
        Color(0xFFF5A623)
    )
    val leaf = palette[abs(variant) % palette.size]
    val scale = 0.88f + (abs(variant) % 3) * 0.11f
    val b = project(cx, cy, zBase)
    val t = project(cx, cy, zBase + 0.46f * scale)
    drawLine(Color(0xFF6A4C38), b, t, strokeWidth = max(1.4f, unitScale * 0.11f), cap = StrokeCap.Round)
    val r = max(2.6f, 0.33f * unitScale * scale)
    drawCircle(lambertShade(leaf, 0f, 0f, 1f, isAwake, nightMode), r, t)
    drawCircle(shadeColor(leaf, 14f, if (isAwake) 8f else 0f, nightMode), r * 0.56f, Offset(t.x - r * 0.25f, t.y - r * 0.28f))
}

private fun DrawScope.drawBougainvillea3D(
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    cx: Float,
    cy: Float,
    zBase: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val p = project(cx, cy, zBase + 0.18f)
    val r = max(1.8f, 0.22f * unitScale)
    drawCircle(lambertShade(Color(0xFFD45D86), 0f, 0f, 1f, isAwake, nightMode), r, p)
    drawCircle(Color(0xFFE57EA0), r * 0.56f, Offset(p.x + r * 0.28f, p.y - r * 0.25f))
}

private fun DrawScope.drawHibiscus3D(
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    cx: Float,
    cy: Float,
    zBase: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val p = project(cx, cy, zBase + 0.16f)
    val r = max(2.0f, 0.24f * unitScale)
    drawCircle(lambertShade(Color(0xFF3F8D59), 0f, 0f, 1f, isAwake, nightMode), r, p)
    drawCircle(Color(0xFFEE7A74), r * 0.44f, Offset(p.x + r * 0.28f, p.y - r * 0.22f))
    drawCircle(Color(0xFFEE7A74), r * 0.34f, Offset(p.x - r * 0.26f, p.y - r * 0.12f))
}

private fun DrawScope.drawVineyardTuft3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    cx: Float,
    cy: Float,
    zBase: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    draw3DPrism(path, project, cx - 0.30f, cx + 0.30f, cy - 0.11f, cy + 0.11f, zBase, zBase + 0.24f, Color(0xFF6E8F45), isAwake, nightMode)
    val p = project(cx + 0.1f, cy, zBase + 0.26f)
    drawCircle(Color(0xFF7D4B82), max(1.2f, unitScale * 0.09f), p)
}

private fun DrawScope.drawLantern3D(
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    cx: Float,
    cy: Float,
    zBase: Float,
    litWarm: Boolean
) {
    val base = project(cx, cy, zBase)
    val top = project(cx, cy, zBase + 0.42f)
    drawLine(Color(0xFF8C867B), base, top, strokeWidth = max(1.2f, unitScale * 0.11f), cap = StrokeCap.Round)
    drawCircle(
        color = if (litWarm) Color(0xFFFFD573) else Color(0xFFF4C150),
        radius = max(1.6f, unitScale * 0.12f),
        center = top
    )
}

private fun DrawScope.drawDesertTent3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    wx0: Float,
    wy0: Float,
    zBase: Float,
    variant: Int,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val tentColor = if (variant % 2 == 0) Color(0xFFC6583A) else Color(0xFF2F6878)
    drawPyramidRoof(
        path = path,
        project = project,
        cx = wx0 + 0.5f,
        cy = wy0 + 0.5f,
        zBase = zBase + 0.04f,
        radius = 0.38f,
        height = 0.58f,
        baseColor = tentColor,
        isAwake = isAwake,
        nightMode = nightMode
    )
}

private fun DrawScope.drawOverwaterBungalow3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    wx0: Float,
    wy0: Float,
    zWater: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    draw3DPrism(path, project, wx0 + 0.24f, wx0 + 0.76f, wy0 + 0.24f, wy0 + 0.76f, zWater, zWater + 0.22f, Color(0xFF6A4C35), isAwake, nightMode)
    draw3DPrism(path, project, wx0 + 0.14f, wx0 + 0.86f, wy0 + 0.14f, wy0 + 0.86f, zWater + 0.22f, zWater + 0.30f, Color(0xFFDFC8A4), isAwake, nightMode)
    drawArchBlock3D(path, project, wx0, wy0, zWater + 0.30f, 0.56f, 0.36f, Color(0xFFF4EAD6), isAwake, nightMode)
    drawPyramidRoof(path, project, wx0 + 0.5f, wy0 + 0.5f, zWater + 0.65f, 0.42f, 0.37f, Color(0xFFCBA464), isAwake = isAwake, nightMode = nightMode)
}

private fun DrawScope.drawSettlementHouse3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    biomeId: String,
    wx0: Float,
    wy0: Float,
    zBase: Float,
    frac: Float,
    hUnits: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    when (biomeId) {
        "kioto" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, frac, hUnits * 0.72f, Color(0xFFE8E2D5), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + hUnits * 0.72f, frac + 0.12f, hUnits * 0.30f, Color(0xFF46433F), isAwake, nightMode)
        }
        "jungla" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.34f, 0.24f, Color(0xFF6E5138), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.24f, frac, hUnits * 0.68f, Color(0xFFA9825A), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.24f + hUnits * 0.68f, frac + 0.10f, 0.20f, Color(0xFF8A6744), isAwake, nightMode)
        }
        "desierto" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, frac, hUnits, Color(0xFFE8D8BE), isAwake, nightMode)
        }
        "valle" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, frac, hUnits * 0.75f, Color(0xFFF5F2EB), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + hUnits * 0.75f, frac + 0.06f, 0.22f, Color(0xFFB58965), isAwake, nightMode)
        }
        "arrozales" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, frac, 0.14f, Color(0xFF8A847B), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.14f, frac * 0.86f, hUnits * 0.58f, Color(0xFFA97F54), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.14f + hUnits * 0.58f, frac + 0.10f, 0.24f, Color(0xFF4B463F), isAwake, nightMode)
        }
        "otono" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, frac, hUnits * 0.72f, Color(0xFFEFE6D8), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + hUnits * 0.72f, frac + 0.08f, 0.26f, Color(0xFFB85D3E), isAwake, nightMode)
        }
        "fiordo" -> {
            val isOchre = cellHash((wx0 + 10f).roundToInt(), (wy0 + 10f).roundToInt(), 53) > 0.68f
            drawArchBlock3D(path, project, wx0, wy0, zBase, frac, hUnits * 0.72f, if (isOchre) Color(0xFFD49442) else Color(0xFFA94235), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + hUnits * 0.72f, frac + 0.08f, 0.22f, Color(0xFFE9EFF4), isAwake, nightMode)
        }
        "atolon" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, frac, hUnits * 0.66f, Color(0xFFF2E7D2), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + hUnits * 0.66f, frac + 0.12f, 0.26f, Color(0xFFCDA462), isAwake, nightMode)
        }
        "toscana" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, frac, hUnits * 0.76f, Color(0xFFF2E5C9), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + hUnits * 0.76f, frac + 0.08f, 0.20f, Color(0xFFC96D4B), isAwake, nightMode)
        }
        else -> {
            val styleRoll = cellHash((wx0 + 10f).roundToInt(), (wy0 + 10f).roundToInt(), 47)
            drawArchBlock3D(path, project, wx0, wy0, zBase, frac, hUnits, Color(0xFFF2E2C4), isAwake, nightMode)
            if (styleRoll > 0.68f) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + hUnits, frac * 0.72f, 0.22f, Color(0xFF3574B5), isAwake, nightMode)
            } else if (styleRoll > 0.36f) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + hUnits, frac + 0.04f, 0.14f, Color(0xFFC96D4B), isAwake, nightMode)
            }
        }
    }
}

private fun DrawScope.drawBridge3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    biomeId: String,
    wx0: Float,
    wy0: Float,
    zWater: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    when (biomeId) {
        "kioto" -> {
            draw3DPrism(path, project, wx0 + 0.12f, wx0 + 0.88f, wy0 - 0.10f, wy0 + 1.10f, zWater, zWater + 0.24f, Color(0xFFC95D4B), isAwake, nightMode)
            draw3DPrism(path, project, wx0 + 0.22f, wx0 + 0.78f, wy0 + 0.12f, wy0 + 0.88f, zWater + 0.24f, zWater + 0.38f, Color(0xFFD66E5C), isAwake, nightMode)
        }
        "jungla" -> {
            draw3DPrism(path, project, wx0 + 0.15f, wx0 + 0.85f, wy0 - 0.12f, wy0 + 1.12f, zWater + 0.14f, zWater + 0.28f, Color(0xFF9C7853), isAwake, nightMode)
        }
        "costa" -> {
            draw3DPrism(path, project, wx0 - 0.20f, wx0 + 1.10f, wy0 + 0.26f, wy0 + 0.74f, zWater, zWater + 0.20f, Color(0xFFB7926A), isAwake, nightMode)
        }
        "desierto" -> {
            draw3DPrism(path, project, wx0 + 0.25f, wx0 + 0.75f, wy0 + 0.22f, wy0 + 0.78f, zWater, zWater + 0.14f, Color(0xFFE8CCA0), isAwake, nightMode)
        }
        "arrozales" -> {
            draw3DPrism(path, project, wx0 + 0.16f, wx0 + 0.84f, wy0 - 0.08f, wy0 + 1.08f, zWater + 0.08f, zWater + 0.24f, Color(0xFFC2A373), isAwake, nightMode)
        }
        "otono" -> {
            draw3DPrism(path, project, wx0 + 0.14f, wx0 + 0.86f, wy0 - 0.10f, wy0 + 1.10f, zWater, zWater + 0.24f, Color(0xFF8E6242), isAwake, nightMode)
            draw3DPrism(path, project, wx0 + 0.22f, wx0 + 0.78f, wy0 + 0.16f, wy0 + 0.84f, zWater + 0.24f, zWater + 0.34f, Color(0xFFA67550), isAwake, nightMode)
        }
        "fiordo" -> {
            draw3DPrism(path, project, wx0 + 0.16f, wx0 + 0.84f, wy0 - 0.12f, wy0 + 1.12f, zWater + 0.10f, zWater + 0.28f, Color(0xFF66584D), isAwake, nightMode)
        }
        "atolon" -> {
            draw3DPrism(path, project, wx0 - 0.18f, wx0 + 1.12f, wy0 + 0.24f, wy0 + 0.76f, zWater + 0.10f, zWater + 0.24f, Color(0xFFDFC8A4), isAwake, nightMode)
        }
        "toscana" -> {
            draw3DPrism(path, project, wx0 + 0.14f, wx0 + 0.86f, wy0 - 0.10f, wy0 + 1.10f, zWater, zWater + 0.26f, Color(0xFFEAE1D0), isAwake, nightMode)
            draw3DPrism(path, project, wx0 + 0.22f, wx0 + 0.78f, wy0 + 0.10f, wy0 + 0.90f, zWater + 0.26f, zWater + 0.36f, Color(0xFFC96D4B), isAwake, nightMode)
        }
        else -> {
            draw3DPrism(path, project, wx0 - 0.18f, wx0 + 1.18f, wy0 + 0.20f, wy0 + 0.80f, zWater, zWater + 0.24f, Color(0xFFB58B65), isAwake, nightMode)
        }
    }
}

private fun DrawScope.drawBoat3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    biomeId: String,
    wx0: Float,
    wy0: Float,
    zWater: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    when (biomeId) {
        "costa", "atolon" -> {
            val hull = if (biomeId == "atolon") Color(0xFF4CC7C4) else Color(0xFFF5F2EB)
            draw3DPrism(path, project, wx0 + 0.26f, wx0 + 0.74f, wy0 + 0.34f, wy0 + 0.66f, zWater, zWater + 0.14f, hull, isAwake, nightMode)
            val mB = project(wx0 + 0.50f, wy0 + 0.50f, zWater + 0.14f)
            val mT = project(wx0 + 0.50f, wy0 + 0.50f, zWater + 0.78f)
            val mS = project(wx0 + 0.74f, wy0 + 0.50f, zWater + 0.22f)
            drawPoly(path, listOf(mT, mB, mS), Color.White, Color(0xFFE5E0D5), 0.5f)
        }
        "fiordo" -> {
            draw3DPrism(path, project, wx0 + 0.22f, wx0 + 0.78f, wy0 + 0.36f, wy0 + 0.64f, zWater, zWater + 0.15f, Color(0xFF594435), isAwake, nightMode)
            val mB = project(wx0 + 0.50f, wy0 + 0.50f, zWater + 0.15f)
            val mT = project(wx0 + 0.50f, wy0 + 0.50f, zWater + 0.74f)
            val mS = project(wx0 + 0.72f, wy0 + 0.50f, zWater + 0.24f)
            drawPoly(path, listOf(mT, mB, mS), Color(0xFFB6463B), Color(0xFFEFE7DC), 0.5f)
        }
        "arrozales" -> {
            draw3DPrism(path, project, wx0 + 0.24f, wx0 + 0.76f, wy0 + 0.38f, wy0 + 0.62f, zWater, zWater + 0.12f, Color(0xFFC8B08E), isAwake, nightMode)
            val mB = project(wx0 + 0.50f, wy0 + 0.50f, zWater + 0.12f)
            val mT = project(wx0 + 0.50f, wy0 + 0.50f, zWater + 0.64f)
            val mS = project(wx0 + 0.70f, wy0 + 0.50f, zWater + 0.20f)
            drawPoly(path, listOf(mT, mB, mS), Color(0xFFFAF7F0), Color(0xFFDDD5C4), 0.45f)
        }
        else -> {
            draw3DPrism(path, project, wx0 + 0.24f, wx0 + 0.76f, wy0 + 0.38f, wy0 + 0.62f, zWater, zWater + 0.12f, Color(0xFF8F6A48), isAwake, nightMode)
        }
    }
}

private fun DrawScope.drawLargeFjordShip3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    wx0: Float,
    wy0: Float,
    zWater: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    draw3DPrism(path, project, wx0 - 0.55f, wx0 + 1.55f, wy0 + 0.14f, wy0 + 0.86f, zWater, zWater + 0.26f, Color(0xFF5A402E), isAwake, nightMode)
    draw3DPrism(path, project, wx0 - 0.55f, wx0 - 0.18f, wy0 + 0.22f, wy0 + 0.78f, zWater + 0.26f, zWater + 0.52f, Color(0xFF71513A), isAwake, nightMode)
    draw3DPrism(path, project, wx0 + 1.18f, wx0 + 1.55f, wy0 + 0.22f, wy0 + 0.78f, zWater + 0.26f, zWater + 0.48f, Color(0xFF71513A), isAwake, nightMode)
    draw3DPrism(path, project, wx0 - 0.14f, wx0 + 1.14f, wy0 + 0.22f, wy0 + 0.78f, zWater + 0.26f, zWater + 0.34f, Color(0xFFCDB99C), isAwake, nightMode)
    draw3DPrism(path, project, wx0 - 0.10f, wx0 + 0.32f, wy0 + 0.28f, wy0 + 0.72f, zWater + 0.34f, zWater + 0.64f, Color(0xFFA83F33), isAwake, nightMode)
    draw3DPrism(path, project, wx0 - 0.12f, wx0 + 0.34f, wy0 + 0.26f, wy0 + 0.74f, zWater + 0.64f, zWater + 0.74f, Color(0xFFEEF4F8), isAwake, nightMode)

    val mBase = project(wx0 + 0.62f, wy0 + 0.50f, zWater + 0.34f)
    val mTop = project(wx0 + 0.62f, wy0 + 0.50f, zWater + 1.92f)
    drawLine(Color(0xFF4A3425), mBase, mTop, strokeWidth = max(1.8f, unitScale * 0.12f), cap = StrokeCap.Round)
    val yL = project(wx0 + 0.62f, wy0 + 0.04f, zWater + 1.66f)
    val yR = project(wx0 + 0.62f, wy0 + 0.96f, zWater + 1.66f)
    drawLine(Color(0xFF4A3425), yL, yR, strokeWidth = max(1.4f, unitScale * 0.09f), cap = StrokeCap.Round)

    val sTL = project(wx0 + 0.62f, wy0 + 0.08f, zWater + 1.66f)
    val sTR = project(wx0 + 0.62f, wy0 + 0.92f, zWater + 1.66f)
    val sBR = project(wx0 + 0.76f, wy0 + 0.90f, zWater + 0.62f)
    val sBL = project(wx0 + 0.76f, wy0 + 0.10f, zWater + 0.62f)
    drawPoly(path, listOf(sTL, sTR, sBR, sBL), Color(0xFFFAF6EE), Color(0xFFDDD5C7), 0.5f)

    val stTL = project(wx0 + 0.62f, wy0 + 0.36f, zWater + 1.66f)
    val stTR = project(wx0 + 0.62f, wy0 + 0.64f, zWater + 1.66f)
    val stBR = project(wx0 + 0.76f, wy0 + 0.62f, zWater + 0.62f)
    val stBL = project(wx0 + 0.76f, wy0 + 0.38f, zWater + 0.62f)
    drawPoly(path, listOf(stTL, stTR, stBR, stBL), Color(0xFFB84438), null)

    val fTip = project(wx0 + 0.92f, wy0 + 0.50f, zWater + 1.84f)
    val fLow = project(wx0 + 0.62f, wy0 + 0.50f, zWater + 1.76f)
    drawPoly(path, listOf(mTop, fTip, fLow), Color(0xFFB84438), null)
}

private fun DrawScope.drawWindmill3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    wx0: Float,
    wy0: Float,
    zBase: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    drawArchBlock3D(path, project, wx0, wy0, zBase, 0.64f, 0.95f, Color.White, isAwake, nightMode)
    drawArchBlock3D(path, project, wx0, wy0, zBase + 0.95f, 0.68f, 0.26f, Color(0xFF3574B5), isAwake, nightMode)
    val hub = project(wx0 + 0.82f, wy0 + 0.50f, zBase + 0.82f)
    val arm = max(4.5f, unitScale * 0.44f)
    val angles = floatArrayOf(0.45f, 2.02f, 3.59f, 5.16f)
    for (ang in angles) {
        drawLine(
            color = Color(0xFF8F6A48),
            start = hub,
            end = Offset(hub.x + cos(ang) * arm, hub.y + sin(ang) * arm),
            strokeWidth = max(1.4f, unitScale * 0.11f),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawDesertRuins3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    wx0: Float,
    wy0: Float,
    zBase: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    drawArchBlock3D(path, project, wx0, wy0, zBase, 0.84f, 0.16f, Color(0xFFDECDB5), isAwake, nightMode)
    draw3DPrism(path, project, wx0 + 0.14f, wx0 + 0.34f, wy0 + 0.20f, wy0 + 0.40f, zBase + 0.16f, zBase + 0.88f, Color(0xFFE9CFA9), isAwake, nightMode)
    draw3DPrism(path, project, wx0 + 0.64f, wx0 + 0.84f, wy0 + 0.58f, wy0 + 0.78f, zBase + 0.16f, zBase + 0.88f, Color(0xFFE9CFA9), isAwake, nightMode)
    draw3DPrism(path, project, wx0 + 0.12f, wx0 + 0.86f, wy0 + 0.18f, wy0 + 0.80f, zBase + 0.88f, zBase + 1.06f, Color(0xFFD5BA92), isAwake, nightMode)
}

private fun DrawScope.drawTuscanFountain3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    wx0: Float,
    wy0: Float,
    zBase: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val cx = wx0 + 0.5f
    val cy = wy0 + 0.5f
    drawArchBlock3D(path, project, wx0, wy0, zBase, 0.86f, 0.22f, Color(0xFFEFE7D8), isAwake, nightMode)
    drawArchBlock3D(path, project, wx0, wy0, zBase + 0.22f, 0.64f, 0.10f, Color(0xFF63B8D4), isAwake, nightMode)
    drawArchBlock3D(path, project, wx0, wy0, zBase + 0.32f, 0.24f, 0.42f, Color(0xFFE5DCCB), isAwake, nightMode)
    drawArchBlock3D(path, project, wx0, wy0, zBase + 0.74f, 0.44f, 0.12f, Color(0xFFEFE7D8), isAwake, nightMode)
    val jet = project(cx, cy, zBase + 0.94f)
    drawCircle(Color(0xFFA8E4F5), max(1.6f, unitScale * 0.13f), jet)
    drawCircle(Color.White, max(1.0f, unitScale * 0.06f), Offset(jet.x, jet.y - unitScale * 0.04f))
}

private fun DrawScope.drawSecondaryLandmark3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    biomeId: String,
    wx0: Float,
    wy0: Float,
    zBase: Float,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val cx = wx0 + 0.5f
    val cy = wy0 + 0.5f
    when (biomeId) {
        "arrozales" -> {
            draw3DPrism(path, project, wx0 + 0.12f, wx0 + 0.88f, wy0 + 0.16f, wy0 + 0.84f, zBase, zBase + 0.16f, Color(0xFF8D877E), isAwake, nightMode)
            draw3DPrism(path, project, wx0 + 0.16f, wx0 + 0.40f, wy0 + 0.26f, wy0 + 0.56f, zBase + 0.16f, zBase + 0.92f, Color(0xFF777168), isAwake, nightMode)
            draw3DPrism(path, project, wx0 + 0.60f, wx0 + 0.84f, wy0 + 0.44f, wy0 + 0.74f, zBase + 0.16f, zBase + 0.92f, Color(0xFF777168), isAwake, nightMode)
            draw3DPrism(path, project, wx0 + 0.20f, wx0 + 0.36f, wy0 + 0.30f, wy0 + 0.52f, zBase + 0.92f, zBase + 1.12f, Color(0xFF5D5750), isAwake, nightMode)
            draw3DPrism(path, project, wx0 + 0.64f, wx0 + 0.80f, wy0 + 0.48f, wy0 + 0.70f, zBase + 0.92f, zBase + 1.12f, Color(0xFF5D5750), isAwake, nightMode)
        }
        "otono" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.72f, 0.18f, Color(0xFF8E7D6F), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.18f, 0.52f, 0.46f, Color(0xFF7C5438), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.64f, 0.84f, 0.24f, Color(0xFFC95D4B), isAwake, nightMode)
        }
        "fiordo" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.74f, 0.28f, Color(0xFF667380), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.28f, 0.46f, 0.88f, Color(0xFFE9F0F5), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 1.16f, 0.52f, 0.22f, Color(0xFFB6463B), isAwake, nightMode)
            drawCircle(Color(0xFFFFE082), max(2.0f, unitScale * 0.17f), project(cx, cy, zBase + 1.50f))
        }
        "atolon" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.80f, 0.16f, Color(0xFFE8D8BE), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.16f, 0.56f, 0.44f, Color(0xFFB08458), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.60f, 0.82f, 0.26f, Color(0xFFD4AE6A), isAwake, nightMode)
        }
        "toscana" -> {
            drawTuscanFountain3D(path, project, unitScale, wx0, wy0, zBase, isAwake, nightMode)
        }
    }
}

private fun DrawScope.drawTorii3D(
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    wx0: Float,
    wy0: Float,
    zBase: Float
) {
    val pL = project(wx0 + 0.20f, wy0 + 0.50f, zBase)
    val pR = project(wx0 + 0.80f, wy0 + 0.50f, zBase)
    val tL = project(wx0 + 0.20f, wy0 + 0.50f, zBase + 0.65f)
    val tR = project(wx0 + 0.80f, wy0 + 0.50f, zBase + 0.65f)
    val lw = max(1.8f, unitScale * 0.13f)
    val toriiRed = Color(0xFFC95D4B)
    drawLine(toriiRed, pL, tL, strokeWidth = lw, cap = StrokeCap.Round)
    drawLine(toriiRed, pR, tR, strokeWidth = lw, cap = StrokeCap.Round)
    drawLine(toriiRed, tL, tR, strokeWidth = lw, cap = StrokeCap.Round)
}

private fun DrawScope.drawLandmark3D(
    path: Path,
    project: (Float, Float, Float) -> Offset,
    unitScale: Float,
    biomeId: String,
    wx0: Float,
    wy0: Float,
    zBase: Float,
    monumentPhase: Int,
    showLegendGlow: Boolean,
    isAwake: Boolean,
    nightMode: Boolean
) {
    val cx = wx0 + 0.5f
    val cy = wy0 + 0.5f

    when (biomeId) {
        "valle" -> {
            if (monumentPhase == 1) {
                drawArchBlock3D(path, project, wx0, wy0, zBase, 0.86f, 0.24f, Color(0xFFD5CFC4), isAwake, nightMode)
                return
            }
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.82f, 0.88f, Color(0xFFF5F2EB), isAwake, nightMode)
            if (monumentPhase >= 3) {
                drawPyramidRoof(path, project, cx, cy, zBase + 0.86f, 0.46f, 0.58f, Color(0xFFB8896B), isAwake = isAwake, nightMode = nightMode)
                if (showLegendGlow) {
                    val glow = project(cx + 0.15f, cy - 0.10f, zBase + 1.54f)
                    drawCircle(Color(0xFFFFD573), max(1.8f, unitScale * 0.14f), glow)
                }
            }
        }
        "costa" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.84f, 0.44f, Color(0xFFE4CFA9), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.44f, 0.54f, 0.68f, Color(0xFFF5E9D3), isAwake, nightMode)
            if (monumentPhase >= 2) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 1.12f, 0.50f, 0.88f, Color(0xFFF5E9D3), isAwake, nightMode)
                drawArchBlock3D(path, project, wx0, wy0, zBase + 1.38f, 0.52f, 0.18f, Color(0xFFD97757), isAwake, nightMode)
            }
            if (monumentPhase >= 3) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 2.00f, 0.58f, 0.24f, Color(0xFFD97757), isAwake, nightMode)
                drawArchBlock3D(path, project, wx0, wy0, zBase + 2.24f, 0.42f, 0.18f, Color(0xFFFFF4C4), isAwake, nightMode)
                val top = project(cx, cy, zBase + 2.52f)
                drawCircle(Color(0xFFFFE082).copy(alpha = 0.38f), max(3.4f, unitScale * (if (showLegendGlow) 0.36f else 0.28f)), top)
                drawCircle(Color(0xFFFFD573), max(2.2f, unitScale * (if (showLegendGlow) 0.22f else 0.17f)), top)
            }
        }
        "kioto" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.84f, 0.36f, Color(0xFFE8E2D5), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.36f, 0.94f, 0.16f, Color(0xFF3D3A37), isAwake, nightMode)
            if (monumentPhase >= 2) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 0.52f, 0.62f, 0.34f, Color(0xFFE8E2D5), isAwake, nightMode)
                drawArchBlock3D(path, project, wx0, wy0, zBase + 0.86f, 0.74f, 0.15f, Color(0xFF3D3A37), isAwake, nightMode)
            }
            if (monumentPhase >= 3) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 1.01f, 0.42f, 0.28f, Color(0xFFC95D4B), isAwake, nightMode)
                drawArchBlock3D(path, project, wx0, wy0, zBase + 1.29f, 0.52f, 0.14f, Color(0xFF3D3A37), isAwake, nightMode)
                val spireB = project(cx, cy, zBase + 1.43f)
                val spireT = project(cx, cy, zBase + 1.82f)
                drawLine(Color(0xFFE5B24C), spireB, spireT, strokeWidth = max(1.5f, unitScale * 0.11f), cap = StrokeCap.Round)
            }
        }
        "jungla" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.92f, 0.42f, Color(0xFF6E756B), isAwake, nightMode)
            if (monumentPhase >= 2) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 0.42f, 0.68f, 0.42f, Color(0xFF838A7F), isAwake, nightMode)
            }
            if (monumentPhase >= 3) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 0.84f, 0.44f, 0.36f, Color(0xFF5C735B), isAwake, nightMode)
                if (showLegendGlow) {
                    drawCircle(Color(0xFFFFB74D), max(2.0f, unitScale * 0.15f), project(cx, cy, zBase + 1.32f))
                }
            }
        }
        "arrozales" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.86f, 0.34f, Color(0xFF8A847B), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.34f, 0.92f, 0.18f, Color(0xFF3B3834), isAwake, nightMode)
            if (monumentPhase >= 2) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 0.52f, 0.64f, 0.30f, Color(0xFFA0998E), isAwake, nightMode)
                drawArchBlock3D(path, project, wx0, wy0, zBase + 0.82f, 0.72f, 0.18f, Color(0xFF3B3834), isAwake, nightMode)
            }
            if (monumentPhase >= 3) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 1.00f, 0.44f, 0.26f, Color(0xFF3B3834), isAwake, nightMode)
                drawArchBlock3D(path, project, wx0, wy0, zBase + 1.26f, 0.26f, 0.20f, Color(0xFF3B3834), isAwake, nightMode)
                drawCircle(Color(0xFFF5CF6B), max(1.8f, unitScale * (if (showLegendGlow) 0.17f else 0.12f)), project(cx, cy, zBase + 1.56f))
            }
        }
        "otono" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.84f, 0.28f, Color(0xFF8E8073), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.28f, 0.76f, 0.58f, Color(0xFFEFE6D8), isAwake, nightMode)
            if (monumentPhase >= 2) {
                draw3DPrism(path, project, wx0 + 0.04f, wx0 + 0.20f, wy0 + 0.24f, wy0 + 0.76f, zBase + 0.14f, zBase + 0.76f, Color(0xFF6E482C), isAwake, nightMode)
            }
            if (monumentPhase >= 3) {
                drawPyramidRoof(path, project, cx, cy, zBase + 0.86f, 0.46f, 0.58f, Color(0xFFBA4E3B), isAwake = isAwake, nightMode = nightMode)
                if (showLegendGlow) {
                    drawCircle(Color(0xFFFFD573), max(2.0f, unitScale * 0.15f), project(cx + 0.18f, cy, zBase + 1.52f))
                }
            }
        }
        "fiordo" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.86f, 0.28f, Color(0xFF6B7682), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.28f, 0.72f, 0.46f, Color(0xFF48382E), isAwake, nightMode)
            if (monumentPhase >= 2) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 0.74f, 0.82f, 0.22f, Color(0xFF342720), isAwake, nightMode)
                drawArchBlock3D(path, project, wx0, wy0, zBase + 0.96f, 0.50f, 0.34f, Color(0xFF48382E), isAwake, nightMode)
            }
            if (monumentPhase >= 3) {
                drawPyramidRoof(path, project, cx, cy, zBase + 1.30f, 0.34f, 0.58f, Color(0xFF382B23), Color(0xFFDFE8EE), isAwake, nightMode)
                if (showLegendGlow) {
                    drawCircle(Color(0xFF8CF2D8), max(2.0f, unitScale * 0.16f), project(cx, cy, zBase + 1.98f))
                }
            }
        }
        "atolon" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.88f, 0.24f, Color(0xFFEFE7D8), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.24f, 0.68f, 0.56f, Color(0xFF966A43), isAwake, nightMode)
            if (monumentPhase >= 2) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 0.80f, 0.86f, 0.24f, Color(0xFFD6AE66), isAwake, nightMode)
            }
            if (monumentPhase >= 3) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 1.04f, 0.52f, 0.34f, Color(0xFFC49A54), isAwake, nightMode)
                drawCircle(
                    if (showLegendGlow) Color(0xFFFF9A8B) else Color(0xFFFFF1D0),
                    max(2.1f, unitScale * (if (showLegendGlow) 0.18f else 0.14f)),
                    project(cx, cy, zBase + 1.50f)
                )
            }
        }
        "toscana" -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.88f, 0.34f, Color(0xFFEFE5D2), isAwake, nightMode)
            drawArchBlock3D(path, project, wx0, wy0, zBase + 0.34f, 0.72f, 0.50f, Color(0xFFE9D3AA), isAwake, nightMode)
            if (monumentPhase >= 2) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 0.84f, 0.42f, 0.74f, Color(0xFFDFB478), isAwake, nightMode)
            }
            if (monumentPhase >= 3) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 1.58f, 0.48f, 0.24f, Color(0xFFC96D4B), isAwake, nightMode)
                if (showLegendGlow) {
                    drawCircle(Color(0xFFFFE082), max(2.0f, unitScale * 0.16f), project(cx, cy, zBase + 1.92f))
                }
            }
        }
        else -> {
            drawArchBlock3D(path, project, wx0, wy0, zBase, 0.80f, 0.70f, Color(0xFFF4EDE1), isAwake, nightMode)
            if (monumentPhase >= 2) {
                drawArchBlock3D(path, project, wx0, wy0, zBase + 0.70f, 0.46f, 0.62f, Color(0xFFEFE7D8), isAwake, nightMode)
            }
            if (monumentPhase >= 3) {
                drawCircle(Color(0xFFE5A03D), max(2.0f, unitScale * (if (showLegendGlow) 0.19f else 0.15f)), project(cx, cy, zBase + 1.46f))
            }
        }
    }
}

private fun getCellData(biomeId: String, x: Int, y: Int, points: Int, showPathHere: Boolean): SceneCellData {
    val nx = x.toFloat() / (GRID_N - 1)
    val ny = y.toFloat() / (GRID_N - 1)
    val noise = (cellHash(x, y, 3) - 0.5f) * 0.17f
    val hScale = 0.50f + (points.coerceIn(0, MAX_POINTS).toFloat() / MAX_POINTS) * 0.72f

    when (biomeId) {
        "valle" -> {
            val riverCenter = 0.50f + 0.08f * sin(ny * 4.6f)
            val distRiver = abs(nx - riverCenter)
            val lakeDist = hypot(nx - 0.50f, ny - 0.72f)
            val isWater = (ny > 0.34f && distRiver < 0.064f) || lakeDist < 0.145f

            val hasPotentialPath = !isWater && ny > 0.42f && ny < 0.88f && abs(nx - 0.28f) < 0.042f
            val isPath = hasPotentialPath && showPathHere

            val valleyWalls = (abs(nx - 0.50f) * 1.85f).pow(1.55f) * 0.95f
            val backSlope = max(0f, 1f - ny) * 0.65f
            val mainPeak = max(0f, 1f - hypot((nx - 0.50f) * 1.15f, (ny - 0.12f) * 1.35f) * 1.85f)
            var rawH = 0.68f + valleyWalls + backSlope + mainPeak.pow(1.25f) * 2.55f + noise * 0.42f
            if (lakeDist < 0.20f) rawH = min(rawH, 0.92f)

            val stepped = (rawH * 4.4f).roundToInt() / 4.4f
            val wz = if (isWater) 0.34f else max(0.54f, stepped * hScale)

            val color = when {
                isWater -> if (lakeDist < 0.085f) Color(0xFF46A4C2) else Color(0xFF6CC6DF)
                isPath -> Color(0xFFDFD6C3)
                rawH > 3.12f && points >= 10 -> Color(0xFFF8FAFC)
                rawH > 2.75f -> Color(0xFF9AA7AE)
                rawH > 1.85f -> Color(0xFF44824A)
                rawH > 1.30f -> Color(0xFF569B58)
                lakeDist < 0.21f || distRiver < 0.12f -> Color(0xFF82C67C)
                else -> Color(0xFF6AB168)
            }
            return SceneCellData(x, y, wz, color, isWater, hasPotentialPath = hasPotentialPath, isPath = isPath, riverDist = distRiver, rawH = rawH)
        }

        "costa" -> {
            val coastLine = 0.44f + 0.14f * sin(ny * PI.toFloat() * 1.4f) + 0.08f * ny
            val isWater = nx > coastLine
            val depth = nx - coastLine
            val hasPotentialPath = !isWater && abs(nx - 0.18f) < 0.048f && ny > 0.14f && ny < 0.84f
            val isPath = hasPotentialPath && showPathHere

            val rawH = 0.95f + max(0f, coastLine - nx) * 3.4f + (1f - ny) * 0.72f + noise
            val stepped = (rawH * 4.2f).roundToInt() / 4.2f
            val wz = if (isWater) max(0.22f, 0.40f - depth * 0.32f) else max(0.54f, stepped * hScale)

            val color = when {
                isWater -> when {
                    depth > 0.28f -> Color(0xFF4B9FB5)
                    depth > 0.12f -> Color(0xFF77C4D4)
                    else -> Color(0xFF9FDCE6)
                }
                isPath -> Color(0xFFEAD6B1)
                depth > -0.11f -> Color(0xFFEEDBB8)
                rawH > 2.25f -> Color(0xFFE6CFA5)
                rawH > 1.65f -> Color(0xFFD9BC8D)
                else -> Color(0xFFCCA976)
            }
            return SceneCellData(x, y, wz, color, isWater, hasPotentialPath = hasPotentialPath, isPath = isPath, rawH = rawH)
        }

        "kioto" -> {
            val streamCenter = 0.36f + 0.15f * cos(ny * PI.toFloat() * 1.25f)
            val pondDist = hypot(nx - 0.32f, ny - 0.68f)
            val isWater = abs(nx - streamCenter) < 0.072f || pondDist < 0.142f
            val pathCenter = 0.66f - 0.12f * sin(nx * PI.toFloat())
            val hasPotentialPath = !isWater && abs(ny - pathCenter) < 0.052f
            val isPath = hasPotentialPath && showPathHere

            val hillBack = hypot(nx - 0.78f, ny - 0.18f)
            val hillLeft = hypot(nx - 0.14f, ny - 0.24f)
            val rawH = 0.85f + max(0f, 1f - hillBack * 1.68f) * 2.35f + max(0f, 1f - hillLeft * 2.2f) * 1.05f + (1f - ny) * 0.4f + noise * 0.6f
            val stepped = (rawH * 4.4f).roundToInt() / 4.4f
            val wz = if (isWater) 0.34f else max(0.54f, stepped * hScale)

            val color = when {
                isWater -> Color(0xFF6DA6AD)
                isPath -> Color(0xFFF1E9E2)
                rawH > 2.28f -> Color(0xFFC99FB1)
                rawH > 1.72f -> Color(0xFFE5BDD0)
                cellHash(x, y, 29) > 0.46f -> Color(0xFFF9E7EE)
                else -> Color(0xFFF3D9E3)
            }
            return SceneCellData(x, y, wz, color, isWater, hasPotentialPath = hasPotentialPath, isPath = isPath, rawH = rawH)
        }

        "jungla" -> {
            // Smooth M3 Expressive terraced rainforest canyon with clean cascading falls & lagoon
            val fallAxis = abs(nx - ny)
            val diag = nx + ny
            val inPlateau = nx < 0.44f && ny < 0.44f
            val isWaterfall = nx < 0.43f && ny < 0.43f && fallAxis < 0.085f && diag in 0.22f..0.70f
            val riverCurve = 0.50f + 0.11f * sin(nx * PI.toFloat() * 1.25f)
            val poolDist = hypot(nx - 0.42f, ny - 0.42f)
            val inPool = poolDist < 0.165f
            val inRiver = nx >= 0.34f && abs(ny - riverCurve) < 0.072f
            val isWater = inPool || inRiver

            val plateauDist = hypot(max(0f, nx - 0.10f), max(0f, ny - 0.10f))
            val cliffHeight = if (inPlateau) max(0f, 1f - plateauDist * 1.55f) * 2.55f else 0f
            val canyonL = max(0f, 1f - hypot(nx - 0.80f, ny - 0.22f) * 1.55f) * 0.95f
            val canyonR = max(0f, 1f - hypot(nx - 0.24f, ny - 0.82f) * 1.65f) * 0.78f
            var rawH = 0.74f + cliffHeight + canyonL + canyonR + noise * 0.18f
            if (isWaterfall) {
                rawH = when {
                    diag < 0.38f -> 2.18f
                    diag < 0.54f -> 1.48f
                    else -> 0.84f
                }
            }

            val stepped = (rawH * 4.0f).roundToInt() / 4.0f
            val wz = when {
                isWater -> 0.34f
                isWaterfall -> max(0.48f, stepped * hScale)
                else -> max(0.56f, stepped * hScale)
            }

            val color = when {
                isWaterfall -> when {
                    diag < 0.36f -> Color(0xFF7CD5E4)
                    diag < 0.54f -> Color(0xFFB3EEF6)
                    else -> Color(0xFFE4FAFD)
                }
                isWater -> when {
                    poolDist < 0.075f -> Color(0xFF8BE2EE)
                    inPool -> Color(0xFF2D8FA0)
                    else -> Color(0xFF4AB4C2)
                }
                rawH > 2.05f -> Color(0xFF1F5036)
                rawH > 1.55f -> Color(0xFF2C6B47)
                rawH > 1.12f -> Color(0xFF458B5B)
                else -> Color(0xFF5FA874)
            }
            return SceneCellData(x, y, wz, color, isWater, isWaterfall = isWaterfall, riverDist = abs(ny - riverCurve), rawH = rawH)
        }

        "arrozales" -> {
            val poolDist = hypot(nx - 0.72f, ny - 0.70f)
            val templePlinth = hypot(nx - 12f / 17f, ny - 12f / 17f) < 0.068f
            val isCanal = nx in 0.28f..0.56f && abs(ny - nx) < 0.055f
            val isWater = !templePlinth && (poolDist < 0.23f || isCanal)
            val hasPotentialPath = !isWater && !templePlinth && nx < 0.54f && abs(ny - 0.70f) < 0.042f
            val isPath = hasPotentialPath && showPathHere

            val rSlope = hypot(nx * 0.94f, ny * 0.94f)
            var rawH = 0.76f + max(0f, 1.15f - rSlope) * 1.92f
            if (templePlinth) rawH = 1.04f

            val stepped = (rawH * 2.8f).roundToInt() / 2.8f
            val ringPhase = (rSlope * 3.4f) % 1f
            val isFloodedPaddy = !isWater && !templePlinth && !hasPotentialPath && rSlope > 0.14f && rSlope < 0.96f && ringPhase > 0.36f
            val wz = if (isWater) 0.34f else max(0.50f, (if (isFloodedPaddy) stepped - 0.05f else stepped) * hScale)

            val color = when {
                isWater -> if (poolDist < 0.13f) Color(0xFF3FA4B5) else Color(0xFF5EC2D0)
                isPath -> Color(0xFFA6BC9E)
                templePlinth -> Color(0xFF7E867E)
                isFloodedPaddy -> if (cellHash(x, y, 21) > 0.45f) Color(0xFF5BC1CF) else Color(0xFF70CFDC)
                rawH > 1.88f -> Color(0xFF3E8745)
                rawH > 1.38f -> Color(0xFF529E54)
                rawH > 0.98f -> Color(0xFF68B464)
                else -> Color(0xFF7AC372)
            }
            return SceneCellData(x, y, wz, color, isWater, hasPotentialPath = hasPotentialPath, isPath = isPath, isFloodedPaddy = isFloodedPaddy, rawH = rawH)
        }

        "otono" -> {
            val streamCenter = 0.48f + 0.14f * sin(nx * 4.1f)
            val distStream = abs(ny - streamCenter)
            val lakeDist = hypot(nx - 0.64f, ny - 0.66f)
            val isWater = distStream < 0.078f || lakeDist < 0.19f
            val hasPotentialPath = !isWater && abs(nx - 0.32f - ny * 0.28f) < 0.045f
            val isPath = hasPotentialPath && showPathHere

            val peak1 = hypot(nx - 0.22f, ny - 0.18f)
            val peak2 = hypot(nx - 0.84f, ny - 0.22f)
            var rawH = 0.88f + max(0f, 1f - peak1 * 1.65f) * 2.15f + max(0f, 1f - peak2 * 1.9f) * 1.55f
            rawH += sin(nx * 4.5f) * 0.18f + noise * 0.55f

            val stepped = (rawH * 4.2f).roundToInt() / 4.2f
            val wz = if (isWater) 0.34f else max(0.54f, stepped * hScale)

            val h31 = cellHash(x, y, 31)
            val color = when {
                isWater -> if (lakeDist < 0.11f) Color(0xFF3F7FA3) else Color(0xFF5C9EC0)
                isPath -> Color(0xFFEFE0C8)
                rawH > 2.25f -> Color(0xFFD44735)
                rawH > 1.72f -> Color(0xFFEB7E30)
                h31 > 0.52f -> Color(0xFFF4C348)
                h31 > 0.24f -> Color(0xFFEE9A3A)
                else -> Color(0xFFE05D38)
            }
            return SceneCellData(x, y, wz, color, isWater, hasPotentialPath = hasPotentialPath, isPath = isPath, rawH = rawH)
        }

        "fiordo" -> {
            val isWaterfall = nx >= 0.04f && nx < 0.15f && abs(ny - 0.54f) < 0.072f
            val fjordCenter = 0.54f + 0.06f * sin(nx * 3.4f)
            val fjordWidth = 0.20f + 0.06f * nx
            val isWater = !isWaterfall && nx >= 0.14f && abs(ny - fjordCenter) < fjordWidth
            val hasPotentialPath = !isWater && !isWaterfall && ny < 0.28f && abs(ny - 0.20f) < 0.040f
            val isPath = hasPotentialPath && showPathHere

            val northBank = max(0f, (fjordCenter - fjordWidth - ny) * 3.4f)
            val southBank = max(0f, (ny - (fjordCenter + fjordWidth)) * 3.0f)
            val backWall = max(0f, (0.20f - nx) * 4.5f)
            var rawH = 0.78f + northBank + southBank + backWall + sin(nx * 3.2f) * 0.14f + noise * 0.28f
            if (isWaterfall) rawH = max(0.62f, 2.55f - nx * 13.0f)

            val stepped = (rawH * 3.6f).roundToInt() / 3.6f
            val wz = when {
                isWater -> 0.34f
                isWaterfall -> max(0.45f, stepped * hScale)
                else -> max(0.54f, stepped * hScale)
            }

            val color = when {
                isWaterfall -> if (cellHash(x, y, 13) > 0.42f) Color(0xFFEFF9FD) else Color(0xFFA4D8EB)
                isWater -> {
                    val dF = abs(ny - fjordCenter)
                    when {
                        dF < fjordWidth * 0.46f -> Color(0xFF2B6284)
                        dF < fjordWidth * 0.78f -> Color(0xFF417FA3)
                        else -> Color(0xFF7DB3CE)
                    }
                }
                isPath -> Color(0xFFD4E0E9)
                rawH > 1.75f -> Color(0xFFF4F8FC)
                rawH > 1.25f -> Color(0xFFE3EDF5)
                cellHash(x, y, 33) > 0.52f -> Color(0xFFCBDCE8)
                else -> Color(0xFF9FB2C2)
            }
            return SceneCellData(x, y, wz, color, isWater, isWaterfall = isWaterfall, hasPotentialPath = hasPotentialPath, isPath = isPath, rawH = rawH)
        }

        "atolon" -> {
            val lagDist = hypot(nx - 0.58f, ny - 0.56f)
            val isLagoon = lagDist < 0.275f
            val isChannel = nx > 0.72f && ny > 0.68f && abs(nx - ny) < 0.16f
            val isOuterSea = lagDist > 0.53f && !(nx < 0.40f && ny < 0.40f)
            val isWater = isLagoon || isOuterSea || isChannel
            val hasPotentialPath = !isWater && abs(lagDist - 0.34f) < 0.042f && ny < 0.62f
            val isPath = hasPotentialPath && showPathHere

            val volcPeak = max(0f, 1f - hypot(nx - 0.24f, ny - 0.26f) * 2.15f) * 2.25f
            val eastKnoll = max(0f, 1f - hypot(nx - 0.80f, ny - 0.24f) * 2.7f) * 0.85f
            val rawH = 0.68f + volcPeak + eastKnoll + noise * 0.35f
            val stepped = (rawH * 4.4f).roundToInt() / 4.4f
            val wz = if (isWater) 0.34f else max(0.54f, stepped * hScale)

            val color = when {
                isLagoon -> if (lagDist < 0.14f) Color(0xFF3AC0BD) else Color(0xFF5BDAD3)
                isOuterSea || isChannel -> Color(0xFF318BA6)
                isPath -> Color(0xFFE5D5BA)
                rawH > 1.85f -> Color(0xFF55875A)
                rawH > 1.05f || volcPeak > 0.45f -> Color(0xFF6AA35D)
                else -> Color(0xFFF4E5C8)
            }
            return SceneCellData(x, y, wz, color, isWater, hasPotentialPath = hasPotentialPath, isPath = isPath, isLagoon = isLagoon, rawH = rawH)
        }

        "toscana" -> {
            val riverCenter = 0.60f + 0.12f * cos(nx * 4.2f)
            val distRiver = abs(ny - riverCenter)
            val isWater = distRiver < 0.072f
            val hasPotentialPath = !isWater && ny < 0.54f && abs(nx - 0.30f - ny * 0.18f) < 0.044f
            val isPath = hasPotentialPath && showPathHere

            val hill1 = hypot(nx - 0.28f, ny - 0.26f)
            val hill2 = hypot(nx - 0.78f, ny - 0.32f)
            var rawH = 0.88f + max(0f, 1f - hill1 * 1.45f) * 2.15f + max(0f, 1f - hill2 * 1.65f) * 1.5f
            rawH += sin(nx * 3.8f + ny * 2.5f) * 0.18f + noise * 0.4f

            val stepped = (rawH * 4.2f).roundToInt() / 4.2f
            val wz = if (isWater) 0.34f else max(0.54f, stepped * hScale)
            val isVineyard = !isWater && !hasPotentialPath && rawH > 1.1f && rawH < 2.25f && ((x + y) % 2 == 0) && cellHash(x, y, 63) > 0.42f

            val color = when {
                isWater -> if (distRiver < 0.035f) Color(0xFF5BA8C2) else Color(0xFF74BAD1)
                isPath -> Color(0xFFEFE7D6)
                isVineyard -> Color(0xFF7C964A)
                rawH > 1.85f -> Color(0xFF97AD5B)
                cellHash(x, y, 27) > 0.5f -> Color(0xFFC9B362)
                else -> Color(0xFFDFB66A)
            }
            return SceneCellData(x, y, wz, color, isWater, hasPotentialPath = hasPotentialPath, isPath = isPath, isVineyard = isVineyard, rawH = rawH)
        }

        else -> {
            // 'desierto' — Oasis y Dunas
            val oasisDist = hypot((nx - 0.52f) * 0.92f, (ny - 0.56f) * 1.02f)
            val isWater = oasisDist < 0.235f
            val isOasisRim = !isWater && oasisDist < 0.31f
            val hasPotentialPath = !isWater && ny < 0.56f && abs(nx - ny * 0.82f - 0.08f) < 0.045f
            val isPath = hasPotentialPath && showPathHere

            val backFactor = max(0f, 1f - (nx * 0.52f + ny * 0.58f))
            val northDune = max(0f, 1f - hypot((nx - 0.22f) * 1.1f, (ny - 0.16f) * 1.35f) * 1.45f) * 1.65f
            val northEastDune = max(0f, 1f - hypot((nx - 0.78f) * 1.25f, (ny - 0.14f) * 1.55f) * 1.65f) * 1.15f
            val gentleWave = sin(nx * 4.2f + ny * 2.2f) * 0.22f + cos(ny * 3.8f - nx * 1.4f) * 0.16f
            var rawH = 0.74f + backFactor * 0.95f + northDune + northEastDune + gentleWave + noise * 0.16f
            val frontOpen = max(0f, (nx + ny - 0.95f) * 0.65f)
            rawH = max(0.68f, rawH - frontOpen * 0.60f)
            if (ny > 0.58f || nx > 0.74f) rawH = min(rawH, 1.16f)
            if (isOasisRim) rawH = min(rawH, 0.78f)

            val stepped = (rawH * 4.2f).roundToInt() / 4.2f
            val wz = if (isWater) 0.34f else max(0.54f, stepped * hScale)

            val color = when {
                isWater -> if (oasisDist < 0.125f) Color(0xFF359CA0) else Color(0xFF55C4C2)
                isPath -> Color(0xFFEED3A1)
                isOasisRim -> Color(0xFFD6AA69)
                rawH > 2.35f -> Color(0xFFC38E4B)
                rawH > 1.75f -> Color(0xFFD4A45E)
                rawH > 1.25f -> Color(0xFFE2BA7A)
                else -> Color(0xFFEACA90)
            }
            return SceneCellData(x, y, wz, color, isWater, hasPotentialPath = hasPotentialPath, isPath = isPath, isOasisRim = isOasisRim, rawH = rawH)
        }
    }
}

private data class RankItem(val key: Int, val score: Float, val x: Int, val y: Int)

private fun toRankMap(arr: MutableList<RankItem>, maxCount: Int = Int.MAX_VALUE, minDist: Float = 0f): Pair<Map<Int, Int>, Int> {
    arr.sortBy { it.score }
    val picked = ArrayList<RankItem>()
    if (minDist > 0f) {
        for (item in arr) {
            if (picked.size >= maxCount) break
            val tooClose = picked.any { hypot((it.x - item.x).toFloat(), (it.y - item.y).toFloat()) < minDist }
            if (!tooClose) picked.add(item)
        }
    } else {
        for (i in 0 until min(arr.size, maxCount)) {
            picked.add(arr[i])
        }
    }
    val map = HashMap<Int, Int>(picked.size)
    picked.forEachIndexed { idx, item ->
        map[item.key] = idx + 1
    }
    return map to picked.size
}

private fun getBiomeLayout(biomeId: String): BiomeLayout {
    val landmarkCell = when (biomeId) {
        "jungla" -> CellPos(12, 4)
        "costa" -> CellPos(6, 3)
        "kioto" -> CellPos(13, 4)
        "valle" -> CellPos(4, 12)
        "desierto" -> CellPos(13, 12)
        "arrozales" -> CellPos(12, 12)
        "otono" -> CellPos(5, 5)
        "fiordo" -> CellPos(13, 2)
        "atolon" -> CellPos(5, 5)
        "toscana" -> CellPos(5, 4)
        else -> null
    }

    val toriiCell = CellPos(8, 11)
    val secondToriiCell = CellPos(5, 13)
    val costaWindmillCell = CellPos(2, 4)
    val desertRuinsCell = CellPos(3, 3)
    val toscanaFountains = listOf(CellPos(12, 4), CellPos(4, 14))
    val secondaryCell = when (biomeId) {
        "costa" -> costaWindmillCell
        "desierto" -> desertRuinsCell
        "arrozales" -> CellPos(4, 4)
        "otono" -> CellPos(4, 12)
        "fiordo" -> CellPos(4, 15)
        "atolon" -> CellPos(14, 4)
        "toscana" -> CellPos(13, 12)
        else -> null
    }

    val treeList = ArrayList<RankItem>()
    val rockList = ArrayList<RankItem>()
    val pathList = ArrayList<RankItem>()
    val tentList = ArrayList<RankItem>()
    val houseList = ArrayList<RankItem>()
    val overwaterList = ArrayList<RankItem>()
    val lanternList = ArrayList<RankItem>()
    val shimmerList = ArrayList<RankItem>()

    val maxHouseH = if (biomeId == "costa" || biomeId == "desierto" || biomeId == "atolon" || biomeId == "toscana") 3.8f else 2.25f
    val houseBaseThresh = when (biomeId) {
        "costa" -> 0.58f
        "toscana" -> 0.77f
        "atolon" -> 0.85f
        else -> 0.83f
    }
    val rockThresh = if (biomeId == "desierto" || biomeId == "fiordo") 0.90f else 0.92f

    for (x in 0 until GRID_N) {
        for (y in 0 until GRID_N) {
            val key = cellKey(x, y)
            val cell = getCellData(biomeId, x, y, MAX_POINTS, true)

            if (cell.isWater) {
                val isBridgeTile = when (biomeId) {
                    "valle" -> x == 9 && y == 8
                    "kioto" -> y == 11 && x == 7
                    "jungla" -> x == 10 && cell.riverDist < 0.06f
                    "costa" -> x == 11 && y == 8
                    "desierto" -> x == 8 && y == 10
                    "arrozales" -> x == 10 && y == 12
                    "otono" -> x == 8 && y == 10
                    "fiordo" -> x == 4 && y == 9
                    "atolon" -> x == 9 && y == 8
                    "toscana" -> x == 9 && y == 9
                    else -> false
                }
                val isBoatTile = when (biomeId) {
                    "valle" -> (x == 9 && y == 12) || (x == 8 && y == 13)
                    "arrozales" -> x == 14 && y == 12
                    "otono" -> (x == 10 && y == 11) || (x == 12 && y == 11)
                    "fiordo" -> (y == 10 && x in 10..12) || (x == 15 && y == 11)
                    "atolon" -> (x == 11 && y == 11) || (x == 14 && y == 13)
                    "toscana" -> (x == 13 && y == 8) || (x == 5 && y == 11)
                    "jungla", "costa", "kioto" -> (x == 13 && y == 11) || (x == 14 && y == 6)
                    else -> false
                }
                if (!isBridgeTile && !isBoatTile && biomeId == "atolon" && cell.isLagoon && cellHash(x, y, 73) > 0.72f) {
                    overwaterList.add(RankItem(key, cellHash(x, y, 74), x, y))
                    continue
                }
                if (!isBridgeTile && !isBoatTile && cellHash(x, y, 41) > 0.56f) {
                    shimmerList.add(RankItem(key, cellHash(x, y, 42), x, y))
                }
                continue
            }

            if (cell.isWaterfall) continue

            val isLandmark = landmarkCell?.let { it.x == x && it.y == y } == true
            val isTorii = biomeId == "kioto" && ((x == toriiCell.x && y == toriiCell.y) || (x == secondToriiCell.x && y == secondToriiCell.y))
            val isSecondary = secondaryCell?.let { it.x == x && it.y == y } == true
            val isExtraFountain = biomeId == "toscana" && toscanaFountains.any { it.x == x && it.y == y }

            if (isLandmark || isTorii || isSecondary || isExtraFountain) continue

            if (cell.hasPotentialPath) {
                val orderScore = when (biomeId) {
                    "costa", "toscana" -> y.toFloat()
                    "kioto", "fiordo" -> x.toFloat()
                    else -> (x + y).toFloat()
                }
                pathList.add(RankItem(key, orderScore, x, y))
                continue
            }

            val hVal = cellHash(x, y, 99)
            if (biomeId == "arrozales" && cell.isFloodedPaddy) continue

            if (biomeId == "desierto" && !cell.isOasisRim && cellHash(x, y, 59) > 0.92f) {
                tentList.add(RankItem(key, cellHash(x, y, 60), x, y))
                continue
            }

            val inCostaVillage = biomeId != "costa" ||
                (x / 17f < 0.44f && y / 17f > 0.14f && y / 17f < 0.86f && hypot((x - 6).toFloat(), (y - 3).toFloat()) >= 1.8f)
            if (inCostaVillage && hVal > houseBaseThresh && cell.rawH < maxHouseH) {
                houseList.add(RankItem(key, cellHash(x, y, 107), x, y))
                continue
            }

            if (cellHash(x, y, 37) > 0.92f && cell.rawH < maxHouseH) {
                lanternList.add(RankItem(key, cellHash(x, y, 38), x, y))
                continue
            }

            if (cellHash(x, y, 19) > rockThresh && (cell.rawH > 1.35f || biomeId == "desierto" || biomeId == "atolon")) {
                rockList.add(RankItem(key, cellHash(x, y, 20), x, y))
                continue
            }

            val isTreeEligible = when (biomeId) {
                "kioto" -> hVal > 0.28f
                "jungla" -> hVal > 0.30f && !(x <= 5 && y <= 5 && abs(x - y) <= 2)
                "valle" -> hVal > 0.46f && cell.rawH < 2.65f
                "costa" -> hVal > 0.62f
                "desierto" -> cell.isOasisRim && hVal > 0.54f
                "arrozales" -> !cell.isFloodedPaddy && hVal > 0.36f
                "otono" -> hVal > 0.33f
                "fiordo" -> hVal > 0.50f && cell.rawH < 2.60f
                "atolon" -> hVal > 0.42f
                "toscana" -> if (cell.isVineyard) hVal > 0.58f else hVal > 0.52f
                else -> false
            }
            if (isTreeEligible) {
                treeList.add(RankItem(key, cellHash(x, y, 201), x, y))
            }
        }
    }

    val treeCap = when (biomeId) {
        "kioto" -> 28
        "jungla" -> 30
        "otono" -> 34
        "valle", "toscana" -> 24
        "fiordo" -> 16
        "arrozales" -> 18
        "costa" -> 10
        "desierto" -> 5
        else -> 36
    }
    val treeMinDist = when (biomeId) {
        "kioto" -> 1.65f
        "jungla" -> 1.48f
        "valle", "arrozales" -> 1.45f
        else -> 0f
    }
    val houseCap = when (biomeId) {
        "costa" -> 11
        "toscana" -> 7
        "jungla" -> 5
        "atolon" -> 8
        "valle", "otono" -> 6
        "fiordo" -> 5
        "arrozales" -> 4
        "desierto" -> 3
        else -> 10
    }
    val houseMinDist = when (biomeId) {
        "jungla" -> 2.2f
        "kioto" -> 1.9f
        "costa" -> 1.65f
        else -> 0f
    }
    val rockCap = when (biomeId) {
        "desierto", "arrozales" -> 3
        "toscana", "fiordo", "otono" -> 4
        "costa", "jungla" -> 5
        else -> 10
    }
    val rockMinDist = if (biomeId == "jungla") 2.0f else 0f
    val lanternCap = when (biomeId) {
        "desierto" -> 3
        "arrozales", "toscana", "fiordo", "jungla" -> 4
        "otono", "costa" -> 5
        else -> 8
    }
    val lanternMinDist = if (biomeId == "jungla") 2.4f else 0f

    val (treeRanks, treeTotal) = toRankMap(treeList, treeCap, treeMinDist)
    val (rockRanks, rockTotal) = toRankMap(rockList, rockCap, rockMinDist)
    val (pathRanks, pathTotal) = toRankMap(pathList)
    val (tentRanks, tentTotal) = toRankMap(tentList, if (biomeId == "desierto") 2 else 6)
    val (houseRanks, houseTotal) = toRankMap(houseList, houseCap, houseMinDist)
    val (overwaterRanks, overwaterTotal) = toRankMap(overwaterList, 6)
    val (lanternRanks, lanternTotal) = toRankMap(lanternList, lanternCap, lanternMinDist)
    val (shimmerRanks, shimmerTotal) = toRankMap(shimmerList, 12)

    return BiomeLayout(
        treeRanks = treeRanks,
        treeTotal = treeTotal,
        rockRanks = rockRanks,
        rockTotal = rockTotal,
        pathRanks = pathRanks,
        pathTotal = pathTotal,
        tentRanks = tentRanks,
        tentTotal = tentTotal,
        houseRanks = houseRanks,
        houseTotal = houseTotal,
        overwaterRanks = overwaterRanks,
        overwaterTotal = overwaterTotal,
        lanternRanks = lanternRanks,
        lanternTotal = lanternTotal,
        shimmerRanks = shimmerRanks,
        shimmerTotal = shimmerTotal,
        landmarkCell = landmarkCell,
        toriiCell = toriiCell,
        secondToriiCell = secondToriiCell,
        costaWindmillCell = costaWindmillCell,
        desertRuinsCell = desertRuinsCell,
        toscanaFountains = toscanaFountains,
        secondaryCell = secondaryCell
    )
}

private fun getBudgetsForPoints(points: Int, layout: BiomeLayout): BiomeBudgets {
    val p = points.coerceIn(0, MAX_POINTS)
    val trees = if (p == 0) 0 else min(layout.treeTotal, max(1, ((p / 74f) * layout.treeTotal).roundToInt()))
    val rocks = if (p == 0) 0 else min(layout.rockTotal, ((p / 56f) * layout.rockTotal).roundToInt())
    val paths = if (p < 18) 0 else (min(1f, (p - 18) / 24f) * layout.pathTotal).roundToInt()
    val showBridge = p >= 32
    val tents = if (p < 28) 0 else min(layout.tentTotal, (min(1f, (p - 28) / 36f) * layout.tentTotal).roundToInt())
    val houses = if (p < 36) 0 else min(layout.houseTotal, (min(1f, (p - 36) / 36f) * layout.houseTotal).roundToInt())
    val overwater = if (p < 30) 0 else min(layout.overwaterTotal, (min(1f, (p - 30) / 36f) * layout.overwaterTotal).roundToInt())
    val lanterns = if (p < 46) 0 else min(layout.lanternTotal, (min(1f, (p - 46) / 30f) * layout.lanternTotal).roundToInt())
    val boats = when {
        p >= 72 -> 2
        p >= 52 -> 1
        else -> 0
    }
    val showMistAndPetals = p >= 56
    val monumentPhase = when {
        p < 58 -> 0
        p < 64 -> 1
        p < 70 -> 2
        else -> 3
    }
    val shimmers = if (p < 66) 0 else min(layout.shimmerTotal, (min(1f, (p - 66) / 14f) * layout.shimmerTotal).roundToInt())
    val showSecondary = p >= 74
    val showLegendGlow = p >= 78

    return BiomeBudgets(
        trees = trees,
        rocks = rocks,
        paths = paths,
        showBridge = showBridge,
        tents = tents,
        houses = houses,
        overwater = overwater,
        lanterns = lanterns,
        boats = boats,
        showMistAndPetals = showMistAndPetals,
        monumentPhase = monumentPhase,
        shimmers = shimmers,
        showSecondary = showSecondary,
        showLegendGlow = showLegendGlow
    )
}

private fun buildBiomeScene(biomeId: String, points: Int): BiomeSceneData {
    val layout = getBiomeLayout(biomeId)
    val budgets = getBudgetsForPoints(points, layout)
    val cells = Array(GRID_N * GRID_N) { idx ->
        val ix = idx / GRID_N
        val iy = idx % GRID_N
        val key = cellKey(ix, iy)
        val showPath = (layout.pathRanks[key] ?: Int.MAX_VALUE) <= budgets.paths
        getCellData(biomeId, ix, iy, points, showPath)
    }
    return BiomeSceneData(
        biomeId = biomeId,
        points = points,
        layout = layout,
        budgets = budgets,
        cells = cells
    )
}
