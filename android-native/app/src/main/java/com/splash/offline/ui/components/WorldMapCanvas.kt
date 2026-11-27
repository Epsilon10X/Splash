package com.splash.offline.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as ACanvas
import android.graphics.Color as AColor
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint as APaint
import android.graphics.Path as APath
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.splash.offline.data.AppLanguage
import com.splash.offline.data.MapStyle
import com.splash.offline.data.SplashCatalogData
import com.splash.offline.data.SplashStrings
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAP_W = 960f
private const val MAP_H = 680f
private const val BITMAP_SCALE = 2.0f
private const val HIT_STEP = 5
private val HIT_W = ceil(MAP_W / HIT_STEP).toInt()
private val HIT_H = ceil(MAP_H / HIT_STEP).toInt()

private const val U_MIN = -25
private const val U_MAX = 25
private const val V_MIN = 8
private const val V_MAX = 64
private const val HALF_W = 16.4f
private const val HALF_H = 8.95f
private const val ORIGIN_X = 480f
private const val ORIGIN_Y = 24f

private val SUN_LEN = sqrt(0.55f * 0.55f + (-0.45f) * (-0.45f) + 0.70f * 0.70f)
private val SUN_X = 0.55f / SUN_LEN
private val SUN_Y = -0.45f / SUN_LEN
private val SUN_Z = 0.70f / SUN_LEN

private data class MapZone(
    val id: String,
    val labelEs: String,
    val labelEn: String,
    val nx: Float,
    val ny: Float,
    val smoothCx: Float,
    val smoothCy: Float,
    var gxCenter: Int = 18,
    var gyCenter: Int = 18,
    var wzCenter: Float = 1.4f,
    var isoCx: Float = 0f,
    var isoCy: Float = 0f
) {
    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) labelEn else labelEs
}

private data class RoadSpec(val fromId: String, val toId: String, val hasBridge: Boolean)

private data class WorldCell(
    val u: Int,
    val v: Int,
    val gx: Float,
    val gy: Float,
    val nx: Float,
    val ny: Float,
    val bid: String,
    val margin: Float,
    val shoreDepth: Float,
    val isWater: Boolean,
    val isCoastalEdge: Boolean,
    val wz: Float,
    val colorHex: String
)

private object WorldMapCache {
    val zones = listOf(
        MapZone("valle", "VALLE ALPINO", "VALLEY", -0.58f, -0.48f, 235f, 185f),
        MapZone("fiordo", "FIORDO", "FJORD", -0.06f, -0.56f, 450f, 148f),
        MapZone("desierto", "OASIS", "OASIS", 0.36f, -0.50f, 638f, 176f),
        MapZone("toscana", "TOSCANA", "TUSCANY", 0.68f, -0.26f, 766f, 248f),
        MapZone("jungla", "JUNGLA", "JUNGLE", -0.64f, 0.04f, 202f, 330f),
        MapZone("kioto", "KIOTO", "KYOTO", -0.24f, -0.04f, 378f, 312f),
        MapZone("arrozales", "BALI", "BALI", 0.25f, 0.02f, 588f, 330f),
        MapZone("otono", "OTOÑO", "AUTUMN", 0.68f, 0.18f, 762f, 388f),
        MapZone("costa", "COSTA", "COAST", -0.34f, 0.50f, 338f, 480f),
        MapZone("atolon", "ATOLÓN", "ATOLL", 0.34f, 0.50f, 632f, 480f)
    )

    val zoneById = zones.associateBy { it.id }
    val zoneIndexById = zones.mapIndexed { idx, z -> z.id to idx }.toMap()

    val roads = listOf(
        RoadSpec("valle", "fiordo", false),
        RoadSpec("fiordo", "desierto", true),
        RoadSpec("desierto", "toscana", false),
        RoadSpec("valle", "jungla", false),
        RoadSpec("valle", "kioto", false),
        RoadSpec("fiordo", "kioto", true),
        RoadSpec("kioto", "arrozales", true),
        RoadSpec("desierto", "arrozales", false),
        RoadSpec("toscana", "otono", false),
        RoadSpec("arrozales", "otono", false),
        RoadSpec("jungla", "costa", false),
        RoadSpec("kioto", "costa", true),
        RoadSpec("arrozales", "atolon", false),
        RoadSpec("costa", "atolon", true)
    )

    val progressionOrder = listOf(
        "valle", "jungla", "costa", "kioto", "desierto",
        "arrozales", "otono", "fiordo", "atolon", "toscana"
    )

    val cells: List<WorldCell> by lazy { buildWorldTerrain() }

    private fun hash2D(x: Float, y: Float, seed: Int = 17): Float {
        val n = sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453123
        return (n - floor(n)).toFloat()
    }

    private fun riverCenterNx(ny: Float): Float = -0.04f + 0.085f * sin(ny * 2.5f + 0.35f)
    private fun tribStreamNy(nx: Float): Float = -0.15f + 0.07f * sin(nx * 4.2f + 0.6f)

    private fun continentBoundaryRadius(nx: Float, ny: Float): Float {
        val ang = atan2(ny, nx)
        val base = 0.95f +
            0.06f * cos(ang * 2f - 0.35f) +
            0.045f * sin(ang * 3f + 0.75f) +
            0.03f * cos(ang * 5f - 1.0f)
        val fjordInlet = 0.20f *
            exp(-((nx + 0.04f) / 0.14f).pow(2)) *
            ((-ny - 0.46f) / 0.48f).coerceIn(0f, 1f)
        val southBay = 0.19f *
            exp(-((nx - 0.04f) / 0.19f).pow(2)) *
            ((ny - 0.46f) / 0.50f).coerceIn(0f, 1f)
        return base - fjordInlet - southBay
    }

    private fun classifyBiome(nx: Float, ny: Float): Pair<MapZone, Float> {
        val wx = nx + 0.035f * sin(ny * 6.2f)
        val wy = ny + 0.035f * cos(nx * 5.8f)
        var best = zones[0]
        var bestD = Float.POSITIVE_INFINITY
        var secondD = Float.POSITIVE_INFINITY
        for (z in zones) {
            val d = hypot((wx - z.nx) * 0.96f, (wy - z.ny) * 1.06f)
            if (d < bestD) {
                secondD = bestD
                bestD = d
                best = z
            } else if (d < secondD) {
                secondD = d
            }
        }
        return best to (secondD - bestD)
    }

    private fun buildWorldTerrain(): List<WorldCell> {
        val list = ArrayList<WorldCell>(1500)
        val sumX = HashMap<String, Float>()
        val sumY = HashMap<String, Float>()
        val sumZ = HashMap<String, Float>()
        val counts = HashMap<String, Int>()

        for (v in V_MIN..V_MAX) {
            for (u in U_MIN..U_MAX) {
                if (((u + v) and 1) != 0) continue
                val gx = (u + v) * 0.5f
                val gy = (v - u) * 0.5f
                val nx = u / 22.8f
                val ny = (v - 36f) / 25.5f
                val eDist = hypot(nx / 1.02f, ny / 0.95f)
                val rim = continentBoundaryRadius(nx, ny)
                val shoreDepth = rim - eDist

                if (shoreDepth < -0.10f) continue

                val (zone, margin) = classifyBiome(nx, ny)
                val bid = zone.id
                val nVal = (hash2D(gx, gy, 31) - 0.5f) * 0.24f
                val localDist = hypot(nx - zone.nx, ny - zone.ny)

                val isAtolonShallow = shoreDepth < 0f && bid == "atolon" && shoreDepth > -0.095f
                if (shoreDepth < 0f && !isAtolonShallow) continue

                val rNx = riverCenterNx(ny)
                val riverDist = abs(nx - rNx)
                val inFjordBay = bid == "fiordo" && ny < -0.60f && abs(nx + 0.04f) < 0.14f
                val isCentralRiver = riverDist < 0.058f && ny in -0.82f..0.85f

                val streamDist = abs(ny - tribStreamNy(nx))
                val isWestStream = nx > -0.74f && nx < rNx && streamDist < 0.046f && (bid == "jungla" || bid == "kioto")
                val isRiver = isCentralRiver || isWestStream || inFjordBay

                var wz = 0.85f
                var colorHex = "#8EC37C"

                if (isAtolonShallow) {
                    wz = 0.34f
                    colorHex = if (shoreDepth > -0.048f) "#72DCD6" else "#4BBCC0"
                } else if (isRiver) {
                    val isWaterfallStep = isWestStream && abs(nx + 0.34f) < 0.08f
                    wz = if (isWaterfallStep) 0.56f else 0.26f
                    colorHex = when {
                        isWaterfallStep -> "#B4F0FA"
                        inFjordBay -> "#4383A6"
                        else -> "#53B7CE"
                    }
                } else {
                    var rawH = 1.05f + max(0f, 0.38f - shoreDepth) * 0.15f
                    when (bid) {
                        "valle" -> {
                            val peak = max(0f, 1f - hypot(nx + 0.72f, ny + 0.62f) * 2.5f)
                            val lake = hypot(nx + 0.44f, ny + 0.40f) < 0.11f
                            if (lake) {
                                rawH = 0.44f
                                colorHex = "#6CC6DF"
                            } else {
                                rawH = 1.05f + peak * 2.35f + (if (ny < -0.52f) 0.42f else 0f) + nVal
                                colorHex = when {
                                    rawH > 2.45f -> "#F4F8FC"
                                    rawH > 1.85f -> "#487B50"
                                    rawH > 1.35f -> "#569B58"
                                    else -> "#74B46A"
                                }
                            }
                        }
                        "fiordo" -> {
                            val peakW = max(0f, 1f - hypot(nx + 0.24f, ny + 0.56f) * 2.9f)
                            val peakE = max(0f, 1f - hypot(nx + 0.06f - 0.18f, ny + 0.60f) * 2.9f)
                            rawH = 1.28f + max(peakW, peakE) * 2.45f + nVal
                            colorHex = when {
                                rawH > 2.48f -> "#EFF6FB"
                                rawH > 1.75f -> "#8FAABE"
                                else -> "#6F8B9C"
                            }
                        }
                        "desierto" -> {
                            val oasisPool = hypot(nx - 0.40f, ny + 0.46f) < 0.105f
                            if (oasisPool) {
                                rawH = 0.38f
                                colorHex = "#4DB8B6"
                            } else {
                                val duneWave = sin(nx * 14f + ny * 9f) * 0.34f
                                rawH = 1.02f + max(0f, 0.32f - localDist) * 1.8f + duneWave + nVal * 0.6f
                                colorHex = when {
                                    rawH > 1.78f -> "#E8C587"
                                    rawH > 1.28f -> "#DCAD6A"
                                    else -> "#C38E4B"
                                }
                            }
                        }
                        "toscana" -> {
                            val hill = max(0f, 1f - localDist * 2.1f)
                            rawH = 1.08f + hill * 1.42f + sin(nx * 10f) * 0.14f + nVal
                            val stripe = ((gx.toInt() + gy.toInt()) and 1) == 0
                            colorHex = when {
                                rawH > 1.85f -> "#C7AE64"
                                stripe -> "#9EB466"
                                else -> "#DFB66A"
                            }
                        }
                        "jungla" -> {
                            val mesa = max(0f, 1f - hypot(nx + 0.72f, ny - 0.04f) * 2.3f)
                            rawH = 1.24f + mesa * 1.55f + abs(sin(nx * 8f + ny * 8f)) * 0.25f + nVal
                            colorHex = when {
                                rawH > 2.25f -> "#1B472C"
                                rawH > 1.65f -> "#2E6943"
                                else -> "#4E8F5B"
                            }
                        }
                        "kioto" -> {
                            val shrineHill = max(0f, 1f - hypot(nx + 0.22f, ny + 0.10f) * 2.6f)
                            rawH = 1.06f + shrineHill * 1.28f + nVal
                            val sakuraPatch = hash2D(gx, gy, 77) > 0.62f
                            colorHex = when {
                                sakuraPatch -> "#F6CBD7"
                                rawH > 1.75f -> "#88AE7D"
                                else -> "#6DA370"
                            }
                        }
                        "arrozales" -> {
                            val rings = floor(localDist * 22f).toInt()
                            val isFloodStep = rings % 2 == 1 && localDist < 0.34f
                            rawH = 0.82f + max(0f, 0.36f - localDist) * 3.1f + (if (isFloodStep) -0.14f else 0.12f)
                            colorHex = when {
                                isFloodStep -> "#5AC2CF"
                                rawH > 1.58f -> "#4E9952"
                                else -> "#74C16B"
                            }
                        }
                        "otono" -> {
                            val ridge = max(0f, 1f - localDist * 2.3f)
                            rawH = 1.12f + ridge * 1.25f + nVal
                            val roll = hash2D(gx, gy, 53)
                            colorHex = when {
                                roll > 0.66f -> "#DA603A"
                                roll > 0.33f -> "#EA933E"
                                else -> "#859E56"
                            }
                        }
                        "costa" -> {
                            val cliff = max(0f, 0.42f - shoreDepth) * 1.1f + max(0f, 1f - localDist * 2.4f) * 1.1f
                            rawH = 1.15f + cliff + nVal * 0.5f
                            colorHex = when {
                                shoreDepth < 0.08f -> "#F3E3C2"
                                rawH > 1.8f -> "#DEC293"
                                else -> "#EEDBB8"
                            }
                        }
                        else -> {
                            val innerLagoon = hypot(nx - 0.34f, ny - 0.50f) < 0.11f
                            if (innerLagoon) {
                                rawH = 0.36f
                                colorHex = "#4CC7C4"
                            } else {
                                rawH = 0.78f + max(0f, 0.28f - localDist) * 0.85f + nVal * 0.4f
                                colorHex = "#F4E5C8"
                            }
                        }
                    }
                    val edgeDrop = when {
                        shoreDepth < 0.06f -> 0.55f
                        shoreDepth < 0.13f -> 0.84f
                        else -> 1.0f
                    }
                    val stepped = (rawH * edgeDrop * 3.2f).roundToInt() / 3.2f
                    wz = max(0.42f, stepped)
                }

                val cell = WorldCell(
                    u = u,
                    v = v,
                    gx = gx,
                    gy = gy,
                    nx = nx,
                    ny = ny,
                    bid = bid,
                    margin = margin,
                    shoreDepth = shoreDepth,
                    isWater = isRiver || isAtolonShallow,
                    isCoastalEdge = shoreDepth < 0.065f,
                    wz = wz,
                    colorHex = colorHex
                )
                list.add(cell)

                if (!cell.isWater || bid == "fiordo" || bid == "atolon") {
                    sumX[bid] = (sumX[bid] ?: 0f) + gx
                    sumY[bid] = (sumY[bid] ?: 0f) + gy
                    sumZ[bid] = (sumZ[bid] ?: 0f) + wz
                    counts[bid] = (counts[bid] ?: 0) + 1
                }
            }
        }

        for (z in zones) {
            val c = counts[z.id] ?: 0
            if (c > 0) {
                z.gxCenter = ((sumX[z.id] ?: 0f) / c).roundToInt()
                z.gyCenter = ((sumY[z.id] ?: 0f) / c).roundToInt()
                z.wzCenter = (sumZ[z.id] ?: 0f) / c
            }
            val pt = projectIso(z.gxCenter.toFloat(), z.gyCenter.toFloat(), z.wzCenter, 13.0f)
            z.isoCx = pt.first
            z.isoCy = pt.second
        }
        return list
    }

    fun projectIso(gx: Float, gy: Float, wz: Float, elevPx: Float): Pair<Float, Float> =
        (ORIGIN_X + (gx - gy) * HALF_W) to (ORIGIN_Y + (gx + gy) * HALF_H - wz * elevPx)

    private val renderedMapCache = LruCache<String, RenderedMapHolder>(6)

    fun getCachedMap(key: String): RenderedMapHolder? = renderedMapCache.get(key)

    fun putCachedMap(key: String, holder: RenderedMapHolder) {
        renderedMapCache.put(key, holder)
    }
}

@Composable
fun WorldMapCanvas(
    lang: AppLanguage,
    mapStyle: MapStyle,
    showAllColor: Boolean,
    activeBiomeIndex: Int,
    activeBiomeLevel: Int,
    unlockedCount: Int,
    betaMode: Boolean,
    biomeXpById: Map<String, Int>,
    onBiomeSelected: (Int) -> Unit,
    onLockedBiomeTapped: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val activeBiome = SplashCatalogData.BIOMES.getOrElse(activeBiomeIndex) { SplashCatalogData.BIOMES.first() }
    val activeId = activeBiome.id
    val mapKey = "${mapStyle.name}|$showAllColor|$activeId|$activeBiomeLevel|$unlockedCount|$betaMode|$isDark|${lang.code}"

    var renderedMap by remember {
        mutableStateOf(
            WorldMapCache.getCachedMap(mapKey) ?: renderContinentBitmap(
                lang = lang,
                style = mapStyle,
                showAllColor = showAllColor,
                activeBiomeId = activeId,
                activeLevel = activeBiomeLevel,
                unlockedCount = unlockedCount,
                betaMode = betaMode,
                isDark = isDark
            ).also { WorldMapCache.putCachedMap(mapKey, it) }
        )
    }

    LaunchedEffect(mapKey) {
        val cached = WorldMapCache.getCachedMap(mapKey)
        if (cached != null) {
            renderedMap = cached
        } else {
            val fresh = withContext(Dispatchers.Default) {
                renderContinentBitmap(
                    lang = lang,
                    style = mapStyle,
                    showAllColor = showAllColor,
                    activeBiomeId = activeId,
                    activeLevel = activeBiomeLevel,
                    unlockedCount = unlockedCount,
                    betaMode = betaMode,
                    isDark = isDark
                )
            }
            WorldMapCache.putCachedMap(mapKey, fresh)
            renderedMap = fresh
        }
    }

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val zoomAnim = remember { Animatable(1.0f) }
    val panXAnim = remember { Animatable(0f) }
    val panYAnim = remember { Animatable(0f) }
    val enterRingProgress = remember { Animatable(0f) }
    val mapAlphaAnim = remember { Animatable(1f) }
    var enteringBiomeMapPos by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var isLockedPulse by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val latestRenderedMap = rememberUpdatedState(renderedMap)
    val latestMapStyle = rememberUpdatedState(mapStyle)
    val latestUnlockedCount = rememberUpdatedState(unlockedCount)
    val latestBetaMode = rememberUpdatedState(betaMode)
    val latestLang = rememberUpdatedState(lang)
    val latestOnBiomeSelected = rememberUpdatedState(onBiomeSelected)
    val latestOnLockedBiomeTapped = rememberUpdatedState(onLockedBiomeTapped)
    val latestViewportSize = rememberUpdatedState(viewportSize)

    fun clampPanForSize(px: Float, py: Float, nextZoom: Float, size: IntSize): Pair<Float, Float> {
        if (size.width <= 0 || size.height <= 0) return 0f to 0f
        val fitScale = min(size.width / MAP_W, size.height / MAP_H)
        val totalScale = fitScale * nextZoom
        val drawW = MAP_W * totalScale
        val drawH = MAP_H * totalScale
        val maxPanX = max(0f, (drawW - size.width) * 0.5f)
        val maxPanY = max(0f, (drawH - size.height) * 0.5f)
        return px.coerceIn(-maxPanX, maxPanX) to py.coerceIn(-maxPanY, maxPanY)
    }

    fun zoneMapCenter(zone: MapZone, style: MapStyle): Pair<Float, Float> {
        val isSmooth = style == MapStyle.PRUEBA_3 || style == MapStyle.PRUEBA_4
        val elev = if (style == MapStyle.PRUEBA_1) 14.0f else 13.0f
        return if (isSmooth) zone.smoothCx to zone.smoothCy
        else WorldMapCache.projectIso(zone.gxCenter.toFloat(), zone.gyCenter.toFloat(), zone.wzCenter, elev)
    }

    fun animateZoomTo(targetZoom: Float) {
        if (enteringBiomeMapPos != null) return
        val clampedZoom = targetZoom.coerceIn(1.0f, 3.6f)
        val oldZoom = zoomAnim.value.coerceAtLeast(1.0f)
        val ratio = clampedZoom / oldZoom
        val (cxPan, cyPan) = if (clampedZoom <= 1.01f) {
            0f to 0f
        } else {
            clampPanForSize(panXAnim.value * ratio, panYAnim.value * ratio, clampedZoom, viewportSize)
        }
        val animSpec = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        scope.launch {
            launch { zoomAnim.animateTo(clampedZoom, animSpec) }
            launch { panXAnim.animateTo(cxPan, animSpec) }
            launch { panYAnim.animateTo(cyPan, animSpec) }
        }
    }

    val showResetZoom by remember {
        derivedStateOf { zoomAnim.value > 1.06f && enteringBiomeMapPos == null }
    }

    val oceanBg = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0C2434), Color(0xFF07151F)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF4AA8CA), Color(0xFF2A7E9E)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(oceanBg)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.4f else 0.55f),
                shape = RoundedCornerShape(28.dp)
            )
            .onSizeChanged { viewportSize = it }
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                val tapSlop = max(touchSlop * 1.5f, 22f)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (enteringBiomeMapPos != null && !isLockedPulse) return@awaitEachGesture

                    val downPos = down.position
                    val downTimeMs = System.currentTimeMillis()
                    var maxPointers = 1
                    var maxMoveDist = 0f
                    var pastTouchSlop = false
                    var wasCanceledByParent = false
                    var zoomAccum = 1f
                    var panAccum = Offset.Zero
                    var curZoom = zoomAnim.value
                    var curPanX = panXAnim.value
                    var curPanY = panYAnim.value

                    do {
                        val event = awaitPointerEvent()
                        val consumedByOther = event.changes.any { it.isConsumed }
                        if (consumedByOther) {
                            wasCanceledByParent = true
                            break
                        }

                        val pressedChanges = event.changes.filter { it.pressed }
                        val pressedCount = pressedChanges.size
                        if (pressedCount > maxPointers) maxPointers = pressedCount
                        if (pressedCount == 1 && maxPointers == 1) {
                            val dist = (pressedChanges[0].position - downPos).getDistance()
                            if (dist > maxMoveDist) maxMoveDist = dist
                        }

                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val canSinglePan = curZoom > 1.02f

                        if (!pastTouchSlop && (pressedCount >= 2 || canSinglePan)) {
                            zoomAccum *= zoomChange
                            panAccum += panChange
                            val zoomMotion = abs(1f - zoomAccum) * event.calculateCentroidSize(useCurrent = false)
                            val panMotion = panAccum.getDistance()
                            if (pressedCount >= 2 || panMotion > touchSlop || zoomMotion > touchSlop) {
                                pastTouchSlop = true
                            }
                        }

                        if (pastTouchSlop && (pressedCount >= 2 || canSinglePan)) {
                            val vp = latestViewportSize.value
                            if (vp.width > 0 && vp.height > 0 && (zoomChange != 1f || panChange != Offset.Zero)) {
                                val nextZoom = (curZoom * zoomChange).coerceIn(1.0f, 3.6f)
                                val centroid = event.calculateCentroid(useCurrent = false)
                                val halfW = vp.width * 0.5f
                                val halfH = vp.height * 0.5f
                                val ratio = nextZoom / curZoom
                                val rawPanX = if (centroid != Offset.Unspecified) {
                                    (centroid.x - halfW) * (1f - ratio) + curPanX * ratio + panChange.x
                                } else {
                                    curPanX + panChange.x
                                }
                                val rawPanY = if (centroid != Offset.Unspecified) {
                                    (centroid.y - halfH) * (1f - ratio) + curPanY * ratio + panChange.y
                                } else {
                                    curPanY + panChange.y
                                }
                                val (cxPan, cyPan) = clampPanForSize(rawPanX, rawPanY, nextZoom, vp)
                                curZoom = nextZoom
                                curPanX = cxPan
                                curPanY = cyPan
                                scope.launch {
                                    zoomAnim.snapTo(nextZoom)
                                    panXAnim.snapTo(cxPan)
                                    panYAnim.snapTo(cyPan)
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (pressedCount >= 2) {
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (pastTouchSlop) {
                        if (zoomAnim.value < 1.05f) {
                            val resetSpec = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                            scope.launch {
                                launch { zoomAnim.animateTo(1.0f, resetSpec) }
                                launch { panXAnim.animateTo(0f, resetSpec) }
                                launch { panYAnim.animateTo(0f, resetSpec) }
                            }
                        }
                    } else if (!wasCanceledByParent && maxPointers == 1 && maxMoveDist <= tapSlop &&
                        (System.currentTimeMillis() - downTimeMs) < 500L
                    ) {
                        val vp = latestViewportSize.value
                        if (vp.width > 0 && vp.height > 0) {
                            val fitScale = min(vp.width / MAP_W, vp.height / MAP_H)
                            val totalScale = fitScale * zoomAnim.value
                            val mapX = MAP_W * 0.5f + (downPos.x - vp.width * 0.5f - panXAnim.value) / totalScale
                            val mapY = MAP_H * 0.5f + (downPos.y - vp.height * 0.5f - panYAnim.value) / totalScale
                            val style = latestMapStyle.value
                            val hitId = latestRenderedMap.value.hitTest(mapX, mapY, style)
                            if (hitId != null) {
                                val biomeIdx = SplashCatalogData.BIOMES.indexOfFirst { it.id == hitId }
                                if (biomeIdx >= 0) {
                                    val zone = WorldMapCache.zoneById[hitId]
                                    val (zx, zy) = if (zone != null) zoneMapCenter(zone, style) else (mapX to mapY)
                                    val isUnlocked = latestBetaMode.value || biomeIdx < latestUnlockedCount.value
                                    if (isUnlocked) {
                                        val targetZoom = 3.1f
                                        val totalTargetScale = fitScale * targetZoom
                                        val rawPanX = (MAP_W * 0.5f - zx) * totalTargetScale
                                        val rawPanY = (MAP_H * 0.5f - zy) * totalTargetScale
                                        val (cPanX, cPanY) = clampPanForSize(rawPanX, rawPanY, targetZoom, vp)

                                        scope.launch {
                                            isLockedPulse = false
                                            enteringBiomeMapPos = zx to zy
                                            val diveEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
                                            val diveSpec = tween<Float>(durationMillis = 340, easing = diveEasing)
                                            launch {
                                                enterRingProgress.snapTo(0f)
                                                enterRingProgress.animateTo(1f, tween(340, easing = diveEasing))
                                            }
                                            launch { zoomAnim.animateTo(targetZoom, diveSpec) }
                                            launch { panXAnim.animateTo(cPanX, diveSpec) }
                                            launch { panYAnim.animateTo(cPanY, diveSpec) }
                                            launch {
                                                delay(180L)
                                                mapAlphaAnim.animateTo(0.82f, tween(160))
                                            }
                                            delay(210L)
                                            latestOnBiomeSelected.value(biomeIdx)
                                            delay(340L)
                                            zoomAnim.snapTo(1.0f)
                                            panXAnim.snapTo(0f)
                                            panYAnim.snapTo(0f)
                                            enterRingProgress.snapTo(0f)
                                            mapAlphaAnim.snapTo(1f)
                                            enteringBiomeMapPos = null
                                        }
                                    } else {
                                        val biomeDef = SplashCatalogData.BIOMES[biomeIdx]
                                        val curLang = latestLang.value
                                        latestOnLockedBiomeTapped.value(
                                            SplashStrings.t(
                                                curLang,
                                                "${biomeDef.name(curLang)} está bloqueado · Completa el bioma anterior al 100% para desbloquearlo",
                                                "${biomeDef.name(curLang)} is locked · Reach 100% on the previous biome to unlock"
                                            )
                                        )
                                        scope.launch {
                                            isLockedPulse = true
                                            enteringBiomeMapPos = zx to zy
                                            enterRingProgress.snapTo(0f)
                                            enterRingProgress.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
                                            enterRingProgress.snapTo(0f)
                                            enteringBiomeMapPos = null
                                            isLockedPulse = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Image(
            bitmap = renderedMap.imageBitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.Medium,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val z = zoomAnim.value
                    scaleX = z
                    scaleY = z
                    translationX = panXAnim.value
                    translationY = panYAnim.value
                    alpha = mapAlphaAnim.value
                }
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pulsePos = enteringBiomeMapPos ?: return@Canvas
            val (zx, zy) = pulsePos
            val fitScale = min(size.width / MAP_W, size.height / MAP_H)
            val totalScale = fitScale * zoomAnim.value
            val screenPos = Offset(
                x = size.width * 0.5f + (zx - MAP_W * 0.5f) * totalScale + panXAnim.value,
                y = size.height * 0.5f + (zy - MAP_H * 0.5f) * totalScale + panYAnim.value
            )
            val progress = enterRingProgress.value
            val ringRadius = (18f + 68f * progress) * fitScale
            val ringAlpha = (1f - progress).coerceIn(0f, 1f) * 0.90f
            if (isLockedPulse) {
                drawCircle(
                    color = Color(0xFFFFB74D).copy(alpha = ringAlpha),
                    radius = ringRadius * 0.78f,
                    center = screenPos,
                    style = Stroke(width = 4.5f)
                )
            } else {
                drawCircle(
                    color = Color.White.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = screenPos,
                    style = Stroke(width = 4.8f)
                )
                drawCircle(
                    color = Color(0xFF42A86E).copy(alpha = ringAlpha * 0.80f),
                    radius = ringRadius * 0.70f,
                    center = screenPos,
                    style = Stroke(width = 3.2f)
                )
            }
        }

        // Top floating instruction pill matching the original look
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = if (isDark) 0.88f else 0.92f),
            tonalElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = SplashStrings.t(
                        lang,
                        "Pellizca para ampliar · Toca un bioma para entrar",
                        "Pinch to zoom · Tap a biome to enter"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Bottom-right reset zoom button when zoomed in
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
        ) {
            AnimatedVisibility(
                visible = showResetZoom,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                FilledTonalIconButton(
                    onClick = { animateZoomTo(1.0f) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CenterFocusStrong,
                        contentDescription = SplashStrings.t(lang, "Centrar mapa", "Reset zoom"),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private class RenderedMapHolder(
    val bitmap: Bitmap,
    val hitGrid: ByteArray
) {
    val imageBitmap = bitmap.asImageBitmap()

    fun hitTest(mapX: Float, mapY: Float, style: MapStyle): String? {
        val ix = floor(mapX / HIT_STEP).toInt()
        val iy = floor(mapY / HIT_STEP).toInt()
        if (ix in 0 until HIT_W && iy in 0 until HIT_H) {
            val code = hitGrid[iy * HIT_W + ix].toInt() and 0xFF
            if (code in 1..WorldMapCache.zones.size) {
                return WorldMapCache.zones[code - 1].id
            }
            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = ix + dx
                    val ny = iy + dy
                    if (nx in 0 until HIT_W && ny in 0 until HIT_H) {
                        val nCode = hitGrid[ny * HIT_W + nx].toInt() and 0xFF
                        if (nCode in 1..WorldMapCache.zones.size) {
                            return WorldMapCache.zones[nCode - 1].id
                        }
                    }
                }
            }
        }
        // Soft proximity fallback near zone anchors
        val isSmooth = style == MapStyle.PRUEBA_3 || style == MapStyle.PRUEBA_4
        val elev = if (style == MapStyle.PRUEBA_1) 14.0f else 13.0f
        var bestId: String? = null
        var bestDist = 96f
        for (z in WorldMapCache.zones) {
            val (zx, zy) = if (isSmooth) z.smoothCx to z.smoothCy else WorldMapCache.projectIso(z.gxCenter.toFloat(), z.gyCenter.toFloat(), z.wzCenter, elev)
            val d = hypot(mapX - zx, mapY - zy)
            if (d < bestDist) {
                bestDist = d
                bestId = z.id
            }
        }
        return bestId
    }
}

private fun parseColor(hex: String): Int = try {
    AColor.parseColor(hex)
} catch (_: Throwable) {
    AColor.rgb(200, 208, 200)
}

private fun mixColor(colA: Int, colB: Int, t: Float): Int {
    val k = t.coerceIn(0f, 1f)
    val r = (AColor.red(colA) + (AColor.red(colB) - AColor.red(colA)) * k).roundToInt().coerceIn(0, 255)
    val g = (AColor.green(colA) + (AColor.green(colB) - AColor.green(colA)) * k).roundToInt().coerceIn(0, 255)
    val b = (AColor.blue(colA) + (AColor.blue(colB) - AColor.blue(colA)) * k).roundToInt().coerceIn(0, 255)
    return AColor.rgb(r, g, b)
}

private fun toLockedGrey(col: Int, isDark: Boolean): Int {
    val lum = 0.299f * AColor.red(col) + 0.587f * AColor.green(col) + 0.114f * AColor.blue(col)
    val s = (if (isDark) (lum * 0.36f + 22f) else (lum * 0.54f + 98f)).roundToInt()
    return AColor.rgb(s.coerceIn(0, 255), s.coerceIn(0, 255), (s + 2).coerceIn(0, 255))
}

private fun lambertShade(col: Int, nx: Float, ny: Float, nz: Float, boost: Float = 0f, shadowBias: Float = 0f): Int {
    val dot = nx * SUN_X + ny * SUN_Y + nz * SUN_Z
    val pct = dot * 26f - 10f + boost - shadowBias
    val f = 1f + pct / 100f
    val warm = if (dot > 0.2f) 9f else 0f
    val r = (AColor.red(col) * f + warm).roundToInt().coerceIn(0, 255)
    val g = (AColor.green(col) * f + warm * 0.72f).roundToInt().coerceIn(0, 255)
    val b = (AColor.blue(col) * f + warm * 0.25f).roundToInt().coerceIn(0, 255)
    return AColor.rgb(r, g, b)
}

private fun markHitRect(hitGrid: ByteArray, x0: Float, y0: Float, x1: Float, y1: Float, code: Int) {
    val ix0 = floor(x0 / HIT_STEP).toInt().coerceIn(0, HIT_W - 1)
    val ix1 = ceil(x1 / HIT_STEP).toInt().coerceIn(0, HIT_W - 1)
    val iy0 = floor(y0 / HIT_STEP).toInt().coerceIn(0, HIT_H - 1)
    val iy1 = ceil(y1 / HIT_STEP).toInt().coerceIn(0, HIT_H - 1)
    val b = code.toByte()
    for (iy in iy0..iy1) {
        val row = iy * HIT_W
        for (ix in ix0..ix1) {
            hitGrid[row + ix] = b
        }
    }
}

private fun renderContinentBitmap(
    lang: AppLanguage,
    style: MapStyle,
    showAllColor: Boolean,
    activeBiomeId: String,
    activeLevel: Int,
    unlockedCount: Int,
    betaMode: Boolean,
    isDark: Boolean
): RenderedMapHolder {
    val bw = (MAP_W * BITMAP_SCALE).roundToInt()
    val bh = (MAP_H * BITMAP_SCALE).roundToInt()
    val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
    val canvas = ACanvas(bitmap)
    canvas.scale(BITMAP_SCALE, BITMAP_SCALE)

    val hitGrid = ByteArray(HIT_W * HIT_H)
    val unlockedSet = HashSet<String>(10)
    SplashCatalogData.BIOMES.forEachIndexed { idx, b ->
        if (idx < unlockedCount) unlockedSet.add(b.id)
    }

    // Keep Bitmap background transparent so the seamless Compose ocean backdrop fills edge-to-edge without clipping seams
    when (style) {
        MapStyle.PRUEBA_1 -> renderVoxelContinent(
            canvas, hitGrid, lang, isFort = false, showAllColor, unlockedSet, activeBiomeId, activeLevel, isDark
        )
        MapStyle.PRUEBA_2 -> renderVoxelContinent(
            canvas, hitGrid, lang, isFort = true, showAllColor, unlockedSet, activeBiomeId, activeLevel, isDark
        )
        MapStyle.PRUEBA_3 -> renderSmoothContinent(
            canvas, hitGrid, lang, isFortniteHybrid = false, showAllColor, unlockedSet, activeBiomeId, activeLevel, isDark
        )
        MapStyle.PRUEBA_4 -> renderSmoothContinent(
            canvas, hitGrid, lang, isFortniteHybrid = true, showAllColor, unlockedSet, activeBiomeId, activeLevel, isDark
        )
    }

    return RenderedMapHolder(bitmap, hitGrid)
}

private fun renderVoxelContinent(
    canvas: ACanvas,
    hitGrid: ByteArray,
    lang: AppLanguage,
    isFort: Boolean,
    showAllColor: Boolean,
    unlockedSet: Set<String>,
    activeBiomeId: String,
    activeLevel: Int,
    isDark: Boolean
) {
    val elevPx = if (isFort) 13.0f else 14.0f
    val cliffHex = if (isDark) "#262C28" else if (isFort) "#5C6C72" else "#584E43"
    val cliffCol = parseColor(cliffHex)
    val greyCliffCol = parseColor(if (isDark) "#2A2D32" else "#84888E")

    val fillPaint = APaint(APaint.ANTI_ALIAS_FLAG).apply { style = APaint.Style.FILL }
    val strokePaint = APaint(APaint.ANTI_ALIAS_FLAG).apply {
        style = APaint.Style.STROKE
        strokeJoin = APaint.Join.ROUND
        strokeCap = APaint.Cap.ROUND
    }
    val reusablePath = APath()

    fun drawIsoPrism(
        gx: Float,
        gy: Float,
        widthFrac: Float,
        zBot: Float,
        zTop: Float,
        baseCol: Int,
        cliffBaseCol: Int = baseCol,
        contourStroke: Int? = null,
        hitCode: Int = 0
    ) {
        val m = (1f - widthFrac) * 0.5f
        val x0 = gx - 0.5f + m
        val x1 = gx + 0.5f - m
        val y0 = gy - 0.5f + m
        val y1 = gy + 0.5f - m

        val (pnX, pnY) = WorldMapCache.projectIso(x0, y0, zTop, elevPx)
        val (peX, peY) = WorldMapCache.projectIso(x1, y0, zTop, elevPx)
        val (psX, psY) = WorldMapCache.projectIso(x1, y1, zTop, elevPx)
        val (pwX, pwY) = WorldMapCache.projectIso(x0, y1, zTop, elevPx)

        val (beX, beY) = WorldMapCache.projectIso(x1, y0, zBot, elevPx)
        val (bsX, bsY) = WorldMapCache.projectIso(x1, y1, zBot, elevPx)
        val (bwX, bwY) = WorldMapCache.projectIso(x0, y1, zBot, elevPx)

        // Left face
        reusablePath.reset()
        reusablePath.moveTo(pwX, pwY); reusablePath.lineTo(psX, psY); reusablePath.lineTo(bsX, bsY); reusablePath.lineTo(bwX, bwY); reusablePath.close()
        fillPaint.color = lambertShade(cliffBaseCol, 0f, 1f, 0f, 0f, 4f)
        canvas.drawPath(reusablePath, fillPaint)

        // Right face
        reusablePath.reset()
        reusablePath.moveTo(psX, psY); reusablePath.lineTo(peX, peY); reusablePath.lineTo(beX, beY); reusablePath.lineTo(bsX, bsY); reusablePath.close()
        fillPaint.color = lambertShade(cliffBaseCol, 1f, 0f, 0f, 0f, -2f)
        canvas.drawPath(reusablePath, fillPaint)

        // Top face
        reusablePath.reset()
        reusablePath.moveTo(pnX, pnY); reusablePath.lineTo(peX, peY); reusablePath.lineTo(psX, psY); reusablePath.lineTo(pwX, pwY); reusablePath.close()
        fillPaint.color = lambertShade(baseCol, 0f, 0f, 1f, 4f, 0f)
        canvas.drawPath(reusablePath, fillPaint)

        if (contourStroke != null) {
            strokePaint.color = contourStroke
            strokePaint.strokeWidth = 0.85f
            canvas.drawPath(reusablePath, strokePaint)
        } else {
            // Subtle hairline seal
            strokePaint.color = fillPaint.color
            strokePaint.strokeWidth = 0.55f
            canvas.drawPath(reusablePath, strokePaint)
        }

        if (hitCode > 0) {
            markHitRect(hitGrid, min(pwX, pnX), min(pnY, peY), max(peX, psX), bsY, hitCode)
        }
    }

    // 1. Tactical Ocean Grid / Decorative Sea Waves
    if (isFort) {
        strokePaint.color = if (isDark) AColor.argb(20, 130, 196, 224) else AColor.argb(36, 255, 255, 255)
        strokePaint.strokeWidth = 1.0f
        var x = 48f
        while (x < MAP_W) {
            canvas.drawLine(x, 0f, x, MAP_H, strokePaint)
            x += 64f
        }
        var y = 48f
        while (y < MAP_H) {
            canvas.drawLine(0f, y, MAP_W, y, strokePaint)
            y += 64f
        }
    } else {
        strokePaint.color = if (isDark) AColor.argb(40, 155, 215, 238) else AColor.argb(76, 255, 255, 255)
        strokePaint.strokeWidth = 1.8f
        val waves = listOf(52f to 82f, 820f to 74f, 58f to 594f, 818f to 608f)
        for ((wx, wy) in waves) {
            reusablePath.reset()
            reusablePath.moveTo(wx, wy)
            reusablePath.quadTo(wx + 18f, wy - 7f, wx + 36f, wy)
            reusablePath.quadTo(wx + 54f, wy - 7f, wx + 72f, wy)
            canvas.drawPath(reusablePath, strokePaint)
        }
    }

    // 2. Continent coastal drop shadow
    val cells = WorldMapCache.cells
    fillPaint.color = if (isDark) AColor.argb(82, 0, 0, 0) else AColor.argb(56, 10, 46, 62)
    for (c in cells) {
        if (!c.isCoastalEdge) continue
        val (spx, spy) = WorldMapCache.projectIso(c.gx, c.gy, -0.82f, elevPx)
        canvas.drawOval(RectF(spx + 3f - 22f, spy + 12f - 11f, spx + 3f + 22f, spy + 12f + 11f), fillPaint)
    }

    // 3. Back-to-Front 3D Isometric Voxel Terrain + Landmarks
    val heroDrawn = HashSet<String>(10)
    val activeEdgeCol = AColor.rgb(33, 138, 84)
    val fortContourCol = AColor.argb(66, 255, 255, 255)

    fun cOf(hex: String, greyOut: Boolean): Int {
        val raw = parseColor(hex)
        return if (greyOut) toLockedGrey(raw, isDark) else raw
    }

    for (c in cells) {
        val unlocked = c.bid in unlockedSet
        val greyOut = !showAllColor && !unlocked
        val isUserBiome = c.bid == activeBiomeId
        val zBot = if (c.isCoastalEdge) -0.82f else max(-0.35f, c.wz - 0.95f)
        val topCol = cOf(c.colorHex, greyOut)
        val contour = when {
            c.isCoastalEdge && isUserBiome -> activeEdgeCol
            isFort && !c.isWater -> fortContourCol
            else -> null
        }
        val hitCode = (WorldMapCache.zoneIndexById[c.bid] ?: 0) + 1
        drawIsoPrism(
            gx = c.gx,
            gy = c.gy,
            widthFrac = 1.0f,
            zBot = zBot,
            zTop = c.wz,
            baseCol = topCol,
            cliffBaseCol = if (greyOut) greyCliffCol else cliffCol,
            contourStroke = contour,
            hitCode = hitCode
        )

        // Hero monuments + scattered trees/ships
        val zone = WorldMapCache.zoneById[c.bid] ?: continue
        val isCenter = hypot(c.gx - zone.gxCenter, c.gy - zone.gyCenter) < 1.25f
        val r = hash2DSimple(c.gx, c.gy, 91)
        val z0 = c.wz

        if (c.isWater) {
            if (c.bid == "fiordo" && c.shoreDepth < 0.08f && r > 0.90f) {
                drawIsoPrism(c.gx, c.gy, 0.55f, z0, z0 + 0.35f, cOf("#704C34", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.14f, z0 + 0.35f, z0 + 1.05f, cOf("#C75145", greyOut), hitCode = hitCode)
            } else if (c.bid == "atolon" && r > 0.84f) {
                drawIsoPrism(c.gx, c.gy, 0.52f, z0, z0 + 0.32f, cOf("#8B6544", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.44f, z0 + 0.32f, z0 + 0.72f, cOf("#FAF0DA", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.56f, z0 + 0.72f, z0 + 0.96f, cOf("#B97B55", greyOut), hitCode = hitCode)
            } else if (c.bid == "costa" && r > 0.88f) {
                drawIsoPrism(c.gx, c.gy, 0.44f, z0, z0 + 0.22f, cOf("#FAF7F0", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.12f, z0 + 0.22f, z0 + 0.86f, cOf("#FFFFFF", greyOut), hitCode = hitCode)
            }
            continue
        }

        if (isCenter && c.bid !in heroDrawn) {
            heroDrawn.add(c.bid)
            when (c.bid) {
                "costa" -> {
                    drawIsoPrism(c.gx, c.gy, 0.76f, z0, z0 + 0.55f, cOf("#E4CFA9", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.52f, z0 + 0.55f, z0 + 1.55f, cOf("#F7EBD5", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.56f, z0 + 1.10f, z0 + 1.32f, cOf("#D97757", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.40f, z0 + 1.55f, z0 + 1.92f, cOf("#FFF2AA", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.48f, z0 + 1.92f, z0 + 2.15f, cOf("#D97757", greyOut), hitCode = hitCode)
                }
                "kioto" -> {
                    drawIsoPrism(c.gx, c.gy, 0.78f, z0, z0 + 0.45f, cOf("#C95240", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.62f, z0 + 0.45f, z0 + 0.95f, cOf("#FAF0E4", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.70f, z0 + 0.95f, z0 + 1.25f, cOf("#B94232", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.44f, z0 + 1.25f, z0 + 1.72f, cOf("#C95240", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.18f, z0 + 1.72f, z0 + 2.10f, cOf("#E8C068", greyOut), hitCode = hitCode)
                }
                "jungla" -> {
                    drawIsoPrism(c.gx, c.gy, 0.86f, z0, z0 + 0.48f, cOf("#6D7D72", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.64f, z0 + 0.48f, z0 + 0.96f, cOf("#7E8E82", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.42f, z0 + 0.96f, z0 + 1.48f, cOf("#92A395", greyOut), hitCode = hitCode)
                }
                "valle" -> {
                    drawIsoPrism(c.gx, c.gy, 0.68f, z0, z0 + 0.56f, cOf("#FAF0DC", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.78f, z0 + 0.56f, z0 + 0.92f, cOf("#C46747", greyOut), hitCode = hitCode)
                }
                "fiordo" -> {
                    drawIsoPrism(c.gx, c.gy, 0.72f, z0, z0 + 0.54f, cOf("#B54236", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.80f, z0 + 0.54f, z0 + 0.96f, cOf("#EEF6FC", greyOut), hitCode = hitCode)
                }
                "desierto" -> {
                    drawIsoPrism(c.gx, c.gy, 0.74f, z0, z0 + 0.68f, cOf("#DFB16D", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.46f, z0 + 0.68f, z0 + 1.15f, cOf("#FAF0DA", greyOut), hitCode = hitCode)
                }
                "toscana" -> {
                    drawIsoPrism(c.gx, c.gy, 0.76f, z0, z0 + 0.66f, cOf("#F7E8CC", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.84f, z0 + 0.66f, z0 + 0.96f, cOf("#C86848", greyOut), hitCode = hitCode)
                }
                "arrozales" -> {
                    drawIsoPrism(c.gx, c.gy, 0.72f, z0, z0 + 0.42f, cOf("#D6BA8D", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.58f, z0 + 0.42f, z0 + 0.85f, cOf("#3C3834", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.42f, z0 + 0.85f, z0 + 1.32f, cOf("#4E4842", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.24f, z0 + 1.32f, z0 + 1.65f, cOf("#322E2A", greyOut), hitCode = hitCode)
                }
                "otono" -> {
                    drawIsoPrism(c.gx, c.gy, 0.70f, z0, z0 + 0.56f, cOf("#8B593B", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.78f, z0 + 0.56f, z0 + 0.94f, cOf("#C4492D", greyOut), hitCode = hitCode)
                }
                else -> {
                    drawIsoPrism(c.gx, c.gy, 0.68f, z0, z0 + 0.48f, cOf("#FAF0DC", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.78f, z0 + 0.48f, z0 + 0.82f, cOf("#D98752", greyOut), hitCode = hitCode)
                }
            }
            continue
        }

        if (c.margin < 0.09f) continue
        when {
            c.bid == "valle" && r > 0.76f -> {
                drawIsoPrism(c.gx, c.gy, 0.22f, z0, z0 + 0.26f, cOf("#6D523A", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.52f, z0 + 0.26f, z0 + 0.72f, cOf("#3C7046", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.32f, z0 + 0.72f, z0 + 1.04f, cOf("#4D8856", greyOut), hitCode = hitCode)
            }
            c.bid == "fiordo" && r > 0.78f -> {
                drawIsoPrism(c.gx, c.gy, 0.22f, z0, z0 + 0.24f, cOf("#5E4A3A", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.50f, z0 + 0.24f, z0 + 0.68f, cOf("#48685E", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.34f, z0 + 0.68f, z0 + 0.96f, cOf("#F2F7FB", greyOut), hitCode = hitCode)
            }
            c.bid == "jungla" && r > 0.66f -> {
                drawIsoPrism(c.gx, c.gy, 0.22f, z0, z0 + 0.32f, cOf("#5A422E", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.66f, z0 + 0.32f, z0 + 0.78f, cOf(if (r > 0.84f) "#28623D" else "#3C7E4F", greyOut), hitCode = hitCode)
            }
            c.bid == "kioto" && r > 0.72f -> {
                drawIsoPrism(c.gx, c.gy, 0.20f, z0, z0 + 0.26f, cOf("#684C3D", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.60f, z0 + 0.26f, z0 + 0.66f, cOf(if (r > 0.86f) "#F6B6C9" else "#FAD0DC", greyOut), hitCode = hitCode)
            }
            c.bid == "desierto" && r > 0.82f -> {
                drawIsoPrism(c.gx, c.gy, 0.18f, z0, z0 + 0.52f, cOf("#8B6342", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.54f, z0 + 0.52f, z0 + 0.74f, cOf("#558E52", greyOut), hitCode = hitCode)
            }
            c.bid == "toscana" && r > 0.75f -> {
                drawIsoPrism(c.gx, c.gy, 0.28f, z0, z0 + 1.05f, cOf("#4D6D3B", greyOut), hitCode = hitCode)
            }
            c.bid == "otono" && r > 0.68f -> {
                drawIsoPrism(c.gx, c.gy, 0.20f, z0, z0 + 0.26f, cOf("#6D4E38", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.60f, z0 + 0.26f, z0 + 0.72f, cOf(when {
                    r > 0.88f -> "#D84030"
                    r > 0.78f -> "#EC822E"
                    else -> "#F4BF42"
                }, greyOut), hitCode = hitCode)
            }
            c.bid == "costa" && r > 0.77f -> {
                if (r > 0.86f) {
                    drawIsoPrism(c.gx, c.gy, 0.56f, z0, z0 + 0.44f, cOf("#F7EBD5", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.40f, z0 + 0.44f, z0 + 0.64f, cOf("#3574B5", greyOut), hitCode = hitCode)
                } else {
                    drawIsoPrism(c.gx, c.gy, 0.18f, z0, z0 + 0.22f, cOf("#6E563F", greyOut), hitCode = hitCode)
                    drawIsoPrism(c.gx, c.gy, 0.48f, z0 + 0.22f, z0 + 0.52f, cOf("#789468", greyOut), hitCode = hitCode)
                }
            }
            c.bid == "arrozales" && r > 0.80f -> {
                drawIsoPrism(c.gx, c.gy, 0.20f, z0, z0 + 0.46f, cOf("#6BA65C", greyOut), hitCode = hitCode)
            }
            c.bid == "atolon" && r > 0.78f -> {
                drawIsoPrism(c.gx, c.gy, 0.18f, z0, z0 + 0.48f, cOf("#8B6544", greyOut), hitCode = hitCode)
                drawIsoPrism(c.gx, c.gy, 0.52f, z0 + 0.48f, z0 + 0.70f, cOf("#539856", greyOut), hitCode = hitCode)
            }
        }
    }

    // 4. Roads + 3D Bridges
    val dashEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    for (road in WorldMapCache.roads) {
        val a = WorldMapCache.zoneById[road.fromId] ?: continue
        val b = WorldMapCache.zoneById[road.toId] ?: continue
        val (ax, ay) = WorldMapCache.projectIso(a.gxCenter.toFloat(), a.gyCenter.toFloat(), a.wzCenter, elevPx)
        val (bx, by) = WorldMapCache.projectIso(b.gxCenter.toFloat(), b.gyCenter.toFloat(), b.wzCenter, elevPx)
        val unlockedBoth = showAllColor || (road.fromId in unlockedSet && road.toId in unlockedSet)

        strokePaint.pathEffect = dashEffect
        strokePaint.strokeWidth = 2.1f
        strokePaint.color = when {
            !unlockedBoth -> AColor.argb(106, 130, 140, 148)
            isFort -> AColor.argb(200, 255, 255, 255)
            else -> AColor.argb(190, 250, 245, 228)
        }
        canvas.drawLine(ax, ay, bx, by, strokePaint)

        if (road.hasBridge) {
            val mx = (ax + bx) * 0.5f
            val my = (ay + by) * 0.5f
            val angDeg = (atan2(by - ay, bx - ax) * 180.0 / PI).toFloat()
            canvas.save()
            canvas.translate(mx, my)
            canvas.rotate(angDeg)
            fillPaint.color = AColor.rgb(201, 172, 130)
            canvas.drawRoundRect(RectF(-14f, -5f, 14f, 5f), 3f, 3f, fillPaint)
            strokePaint.pathEffect = null
            strokePaint.color = AColor.rgb(110, 81, 56)
            strokePaint.strokeWidth = 1.4f
            canvas.drawRoundRect(RectF(-14f, -5f, 14f, 5f), 3f, 3f, strokePaint)
            canvas.drawLine(-5f, -5f, -5f, 5f, strokePaint)
            canvas.drawLine(0f, -5f, 0f, 5f, strokePaint)
            canvas.drawLine(5f, -5f, 5f, 5f, strokePaint)
            canvas.restore()
        }
    }
    strokePaint.pathEffect = null

    // 5. Soft Fluffy Clouds for Fortnite style (Prueba 2)
    if (isFort) {
        drawFortniteClouds(canvas, isDark, listOf(Triple(106f, 98f, 1.0f), Triple(838f, 128f, 0.92f), Triple(124f, 526f, 1.1f)))
    }

    // 6. Zone Labels + "Tú · Nv. X" Badge
    drawZoneLabelsAndBadges(
        canvas = canvas,
        hitGrid = hitGrid,
        lang = lang,
        isFortnitePoi = isFort,
        isSmoothCoords = false,
        elevPx = elevPx,
        showAllColor = showAllColor,
        unlockedSet = unlockedSet,
        activeBiomeId = activeBiomeId,
        activeLevel = activeLevel,
        isDark = isDark
    )

    // 7. Compass Rose
    drawCompassRose(canvas, 888f, 608f, if (isDark) 0.40f else 0.60f, AColor.rgb(250, 248, 243))
}

private fun renderSmoothContinent(
    canvas: ACanvas,
    hitGrid: ByteArray,
    lang: AppLanguage,
    isFortniteHybrid: Boolean,
    showAllColor: Boolean,
    unlockedSet: Set<String>,
    activeBiomeId: String,
    activeLevel: Int,
    isDark: Boolean
) {
    val fillPaint = APaint(APaint.ANTI_ALIAS_FLAG).apply { style = APaint.Style.FILL }
    val strokePaint = APaint(APaint.ANTI_ALIAS_FLAG).apply {
        style = APaint.Style.STROKE
        strokeCap = APaint.Cap.ROUND
        strokeJoin = APaint.Join.ROUND
    }

    fun isGreyBiome(bid: String): Boolean = !showAllColor && bid !in unlockedSet
    fun colOf(bid: String, hex: String): Int {
        val raw = parseColor(hex)
        return if (isGreyBiome(bid)) toLockedGrey(raw, isDark) else raw
    }

    // 1. Tactical Ocean Grid or Soft Sea Waves
    if (isFortniteHybrid) {
        strokePaint.color = if (isDark) AColor.argb(24, 130, 196, 224) else AColor.argb(38, 255, 255, 255)
        strokePaint.strokeWidth = 1.0f
        var x = 48f
        while (x < MAP_W) { canvas.drawLine(x, 0f, x, MAP_H, strokePaint); x += 64f }
        var y = 48f
        while (y < MAP_H) { canvas.drawLine(0f, y, MAP_W, y, strokePaint); y += 64f }

        val gridText = APaint(APaint.ANTI_ALIAS_FLAG).apply {
            textSize = 9.5f
            typeface = Typeface.DEFAULT_BOLD
            color = if (isDark) AColor.argb(72, 170, 215, 236) else AColor.argb(92, 255, 255, 255)
        }
        listOf("A1", "B1", "C1", "D1").forEachIndexed { idx, lbl ->
            canvas.drawText(lbl, 80f + idx * 224f, 22f, gridText)
        }
    } else {
        strokePaint.color = if (isDark) AColor.argb(36, 155, 215, 238) else AColor.argb(86, 255, 255, 255)
        strokePaint.strokeWidth = 2.0f
        val p = APath()
        listOf(54f to 82f, 818f to 72f, 58f to 596f, 790f to 610f).forEach { (wx, wy) ->
            p.reset()
            p.moveTo(wx, wy)
            p.quadTo(wx + 20f, wy - 8f, wx + 40f, wy)
            p.quadTo(wx + 60f, wy - 8f, wx + 80f, wy)
            canvas.drawPath(p, strokePaint)
        }
    }

    // 2. Concentric soft coastal ocean ripples
    val rippleSpecs = if (isFortniteHybrid) {
        listOf(
            1.068f to (if (isDark) AColor.argb(46, 78, 162, 196) else AColor.argb(62, 182, 236, 252)),
            1.036f to (if (isDark) AColor.argb(62, 110, 194, 224) else AColor.argb(102, 220, 248, 255)),
            1.014f to (if (isDark) AColor.argb(76, 156, 224, 244) else AColor.argb(164, 255, 255, 255))
        )
    } else {
        listOf(
            1.078f to (if (isDark) AColor.argb(36, 84, 160, 188) else AColor.argb(62, 218, 246, 254)),
            1.046f to (if (isDark) AColor.argb(52, 108, 186, 212) else AColor.argb(98, 235, 251, 255)),
            1.018f to (if (isDark) AColor.argb(72, 142, 212, 234) else AColor.argb(144, 255, 255, 255))
        )
    }
    for ((sc, col) in rippleSpecs) {
        fillPaint.color = col
        canvas.drawPath(buildSmoothContinentPath(sc, 0f, 8f), fillPaint)
    }

    // 3. Sculpted 3D coastal cliff plinth & shadow
    val slabLayers = if (isFortniteHybrid) {
        listOf(
            Triple(28f, 1.012f, if (isDark) AColor.argb(96, 0, 0, 0) else AColor.argb(66, 10, 38, 54)),
            Triple(20f, 1.003f, parseColor(if (isDark) "#1D262B" else "#566973")),
            Triple(12f, 1.001f, parseColor(if (isDark) "#2B373E" else "#738A94")),
            Triple(5f, 1.000f, parseColor(if (isDark) "#3B4850" else "#AFC1C8"))
        )
    } else {
        listOf(
            Triple(28f, 1.012f, if (isDark) AColor.argb(88, 0, 0, 0) else AColor.argb(56, 10, 44, 58)),
            Triple(20f, 1.003f, parseColor(if (isDark) "#21272A" else "#967B5E")),
            Triple(12f, 1.001f, parseColor(if (isDark) "#2E3639" else "#B79A75")),
            Triple(5f, 1.000f, parseColor(if (isDark) "#3D4649" else "#D9C298"))
        )
    }
    for ((dy, sc, col) in slabLayers) {
        fillPaint.color = col
        canvas.drawPath(buildSmoothContinentPath(sc, if (dy > 24f) 5f else 0f, dy), fillPaint)
    }

    val topPoly = buildSmoothContinentPath(1.0f, 0f, 0f)
    canvas.save()
    canvas.clipPath(topPoly)

    fillPaint.shader = null
    fillPaint.color = parseColor(if (isDark) "#233027" else "#A9CF92")
    canvas.drawRect(0f, 0f, MAP_W, MAP_H, fillPaint)

    val smoothPal = mapOf(
        "valle" to ("#76B56E" to "#589C5B"),
        "fiordo" to ("#DCEAF2" to "#B7CFDF"),
        "desierto" to ("#ECC68A" to "#D6A45E"),
        "toscana" to ("#DFCD8E" to "#C4AE6A"),
        "jungla" to ("#4D935E" to "#2F6F45"),
        "kioto" to ("#9EC388" to "#F2CBD7"),
        "arrozales" to ("#74C28E" to "#53AC96"),
        "otono" to ("#E39C62" to "#C96D45"),
        "costa" to ("#EFE0BE" to "#DAC090"),
        "atolon" to ("#92DFD8" to "#68CAC3")
    )

    WorldMapCache.zones.forEachIndexed { idx, z ->
        val (h1, h2) = smoothPal[z.id] ?: ("#76B56E" to "#589C5B")
        val c1 = parseColor(h1)
        val c2 = parseColor(h2)
        val grey = isGreyBiome(z.id)
        val colA = if (grey) toLockedGrey(c1, isDark) else if (isDark) mixColor(c1, parseColor("#121A17"), 0.45f) else c1
        val colB = if (grey) toLockedGrey(c2, isDark) else if (isDark) mixColor(c2, parseColor("#101815"), 0.50f) else c2
        fillPaint.shader = RadialGradient(
            z.smoothCx - 20f, z.smoothCy - 18f, 195f,
            intArrayOf(colA, colB, AColor.TRANSPARENT),
            floatArrayOf(0f, 0.68f, 1f),
            Shader.TileMode.CLAMP
        )
        val blob = buildOrganicMoundPath(z.smoothCx, z.smoothCy, 195f, 138f, if (idx % 2 == 1) 0.2f else -0.2f, 11 + idx * 7, 0.14f)
        canvas.drawPath(blob, fillPaint)
    }
    fillPaint.shader = null

    // Stacked rounded 3D biome plateaus
    drawSmooth3DTerrace(canvas, 215f, 185f, 118f, 72f, -0.18f, 14f, "#6CAE67", isGreyBiome("valle"), isDark, 17, 0.09f)
    drawSmooth3DTerrace(canvas, 185f, 165f, 76f, 48f, -0.22f, 13f, "#4B8854", isGreyBiome("valle"), isDark, 29, 0.10f)
    fillPaint.color = colOf("valle", "#6CC6DF")
    canvas.drawOval(RectF(275f - 26f, 206f - 13f, 275f + 26f, 206f + 13f), fillPaint)
    drawSmoothPeak(canvas, 165f, 154f, 72f, 46f, 0.44f, "#5E7C68", "#F8FAFC", isGreyBiome("valle"), isDark)
    drawSmoothPeak(canvas, 210f, 144f, 58f, 38f, 0.46f, "#587562", "#FFFFFF", isGreyBiome("valle"), isDark)

    drawSmooth3DTerrace(canvas, 452f, 148f, 120f, 62f, 0.05f, 15f, "#BBD2E2", isGreyBiome("fiordo"), isDark, 43, 0.09f)
    drawSmoothPeak(canvas, 398f, 136f, 68f, 46f, 0.62f, "#6E8EA4", "#F3F8FC", isGreyBiome("fiordo"), isDark)
    drawSmoothPeak(canvas, 504f, 134f, 72f, 48f, 0.64f, "#6E8EA4", "#EBF3FA", isGreyBiome("fiordo"), isDark)

    drawSmooth3DTerrace(canvas, 640f, 176f, 116f, 68f, 0.15f, 13f, "#E8C284", isGreyBiome("desierto"), isDark, 57, 0.08f)
    drawSmooth3DTerrace(canvas, 620f, 156f, 66f, 36f, -0.24f, 11f, "#E0AD65", isGreyBiome("desierto"), isDark, 63, 0.11f)
    fillPaint.color = colOf("desierto", "#4EB8B6")
    canvas.drawOval(RectF(672f - 24f, 184f - 11f, 672f + 24f, 184f + 11f), fillPaint)

    drawSmooth3DTerrace(canvas, 766f, 250f, 108f, 66f, 0.25f, 12f, "#C8B86E", isGreyBiome("toscana"), isDark, 71, 0.08f)
    drawSmooth3DTerrace(canvas, 756f, 232f, 64f, 38f, -0.10f, 10f, "#8FA65C", isGreyBiome("toscana"), isDark, 79, 0.09f)

    drawSmooth3DTerrace(canvas, 198f, 336f, 116f, 76f, 0.12f, 15f, "#3E8251", isGreyBiome("jungla"), isDark, 83, 0.10f)
    drawSmooth3DTerrace(canvas, 182f, 314f, 72f, 46f, -0.15f, 12f, "#225937", isGreyBiome("jungla"), isDark, 97, 0.10f)
    fillPaint.color = colOf("jungla", "#90E4F2")
    canvas.drawRoundRect(RectF(216f, 304f, 228f, 332f), 6f, 6f, fillPaint)
    fillPaint.color = colOf("jungla", "#E8FCFF")
    canvas.drawOval(RectF(209f, 327f, 235f, 337f), fillPaint)

    drawSmooth3DTerrace(canvas, 376f, 312f, 108f, 68f, -0.08f, 12f, "#7FAE78", isGreyBiome("kioto"), isDark, 101, 0.08f)
    drawSmooth3DTerrace(canvas, 368f, 294f, 66f, 40f, 0.18f, 9f, "#F3CBD7", isGreyBiome("kioto"), isDark, 109, 0.09f)
    fillPaint.color = colOf("kioto", "#6DA6AD")
    canvas.drawOval(RectF(412f - 20f, 326f - 9f, 412f + 20f, 326f + 9f), fillPaint)

    drawSmooth3DTerrace(canvas, 592f, 332f, 118f, 72f, 0.08f, 13f, "#58AE68", isGreyBiome("arrozales"), isDark, 113, 0.07f)
    drawSmooth3DTerrace(canvas, 590f, 322f, 92f, 54f, 0.08f, 10f, "#59C2CF", isGreyBiome("arrozales"), isDark, 127, 0.06f)
    drawSmooth3DTerrace(canvas, 589f, 312f, 66f, 38f, 0.08f, 8f, "#74C776", isGreyBiome("arrozales"), isDark, 131, 0.06f)
    drawSmooth3DTerrace(canvas, 588f, 302f, 40f, 22f, 0.08f, 7f, "#64CBD7", isGreyBiome("arrozales"), isDark, 137, 0.05f)

    drawSmooth3DTerrace(canvas, 764f, 390f, 108f, 68f, -0.22f, 13f, "#DC7B46", isGreyBiome("otono"), isDark, 139, 0.09f)
    drawSmooth3DTerrace(canvas, 754f, 372f, 66f, 40f, 0.12f, 9f, "#EB9F45", isGreyBiome("otono"), isDark, 149, 0.08f)

    drawSmooth3DTerrace(canvas, 338f, 480f, 126f, 70f, 0.12f, 13f, "#EAD5AD", isGreyBiome("costa"), isDark, 157, 0.08f)
    drawSmooth3DTerrace(canvas, 324f, 464f, 80f, 44f, -0.10f, 10f, "#DFBF8D", isGreyBiome("costa"), isDark, 167, 0.08f)

    drawSmooth3DTerrace(canvas, 634f, 482f, 118f, 64f, -0.14f, 9f, "#EFE0BE", isGreyBiome("atolon"), isDark, 179, 0.08f)
    fillPaint.color = colOf("atolon", "#4CC7C4")
    canvas.drawOval(RectF(638f - 54f, 488f - 24f, 638f + 54f, 488f + 24f), fillPaint)
    strokePaint.color = colOf("atolon", "#FAF4E4")
    strokePaint.strokeWidth = 2.2f
    canvas.drawOval(RectF(638f - 54f, 488f - 24f, 638f + 54f, 488f + 24f), strokePaint)

    if (isFortniteHybrid) {
        strokePaint.color = if (isDark) AColor.argb(42, 255, 255, 255) else AColor.argb(88, 255, 255, 255)
        strokePaint.strokeWidth = 1.25f
        WorldMapCache.zones.forEachIndexed { idx, z ->
            canvas.drawPath(
                buildOrganicMoundPath(z.smoothCx, z.smoothCy - 8f, 102f, 62f, if (idx % 2 == 1) 0.15f else -0.15f, 41 + idx * 5, 0.09f),
                strokePaint
            )
        }
    }

    // Carved meandering river & jungle waterfall stream
    val riverCol = parseColor(if (isDark) "#2B7B93" else "#53BBD2")
    val bankCol = parseColor(if (isDark) "#19353F" else "#35798C")
    val rPath = APath().apply {
        moveTo(468f, 92f)
        cubicTo(445f, 195f, 502f, 268f, 468f, 368f)
        cubicTo(440f, 452f, 500f, 520f, 496f, 598f)
    }
    canvas.save(); canvas.translate(0f, -4f)
    strokePaint.color = bankCol; strokePaint.strokeWidth = 28f; canvas.drawPath(rPath, strokePaint)
    canvas.restore()
    strokePaint.color = riverCol; strokePaint.strokeWidth = 21f; canvas.drawPath(rPath, strokePaint)

    val tribPath = APath().apply {
        moveTo(222f, 328f)
        quadTo(342f, 314f, 468f, 344f)
    }
    canvas.save(); canvas.translate(0f, -3f)
    strokePaint.color = bankCol; strokePaint.strokeWidth = 15f; canvas.drawPath(tribPath, strokePaint)
    canvas.restore()
    strokePaint.color = riverCol; strokePaint.strokeWidth = 10f; canvas.drawPath(tribPath, strokePaint)

    // White shimmer along central river
    strokePaint.color = AColor.argb(108, 255, 255, 255)
    strokePaint.strokeWidth = 2.4f
    strokePaint.pathEffect = DashPathEffect(floatArrayOf(12f, 16f), 0f)
    canvas.drawPath(rPath, strokePaint)
    strokePaint.pathEffect = null

    // Highlight active biome oval
    WorldMapCache.zoneById[activeBiomeId]?.let { z ->
        strokePaint.color = AColor.rgb(33, 138, 84)
        strokePaint.strokeWidth = 3.2f
        canvas.drawOval(RectF(z.smoothCx - 86f, z.smoothCy - 60f, z.smoothCx + 86f, z.smoothCy + 44f), strokePaint)
    }

    canvas.restore() // end clip

    // Coastal top contour outline
    strokePaint.color = if (isFortniteHybrid) {
        if (isDark) AColor.argb(72, 255, 255, 255) else AColor.argb(184, 255, 255, 255)
    } else {
        if (isDark) AColor.argb(42, 255, 255, 255) else AColor.argb(144, 255, 255, 255)
    }
    strokePaint.strokeWidth = 2.1f
    canvas.drawPath(topPoly, strokePaint)

    // Populate organic hitGrid
    val hitMidX = 482f
    val hitMidY = 336f
    for (iy in 0 until HIT_H) {
        val py = iy * HIT_STEP + HIT_STEP * 0.5f
        for (ix in 0 until HIT_W) {
            val px = ix * HIT_STEP + HIT_STEP * 0.5f
            val dx = (px - hitMidX) / 398f
            val dy = (py - hitMidY) / 248f
            val t = atan2(dy, dx)
            val wave = 0.95f + 0.055f * cos(t * 2f - 0.35f) + 0.042f * sin(t * 3f + 0.75f) + 0.025f * cos(t * 5f - 1.0f)
            val fjordNotch = if (sin(t) < -0.45f) 0.18f * exp(-((cos(t) + 0.04f) / 0.16f).pow(2)) else 0f
            val southBay = if (sin(t) > 0.45f) 0.16f * exp(-((cos(t) - 0.05f) / 0.22f).pow(2)) else 0f
            if (hypot(dx, dy) <= wave - fjordNotch - southBay + 0.02f) {
                var bestIdx = 0
                var bestD = Float.POSITIVE_INFINITY
                WorldMapCache.zones.forEachIndexed { k, z ->
                    val d = hypot(px - z.smoothCx, (py - z.smoothCy) * 1.15f)
                    if (d < bestD) { bestD = d; bestIdx = k }
                }
                hitGrid[iy * HIT_W + ix] = (bestIdx + 1).toByte()
            }
        }
    }

    // Smooth curved landmarks north -> south
    WorldMapCache.zones.sortedBy { it.smoothCy }.forEach { z ->
        drawSmoothLandmark(canvas, z.id, z.smoothCx, z.smoothCy - 18f, isGreyBiome(z.id), isDark)
    }

    // Dashed tactical roads + rounded bridges
    strokePaint.pathEffect = if (isFortniteHybrid) DashPathEffect(floatArrayOf(7f, 6f), 0f) else DashPathEffect(floatArrayOf(5f, 6f), 0f)
    for (road in WorldMapCache.roads) {
        val a = WorldMapCache.zoneById[road.fromId] ?: continue
        val b = WorldMapCache.zoneById[road.toId] ?: continue
        val unlockedBoth = showAllColor || (road.fromId in unlockedSet && road.toId in unlockedSet)
        val mx = (a.smoothCx + b.smoothCx) * 0.5f
        val my = (a.smoothCy + b.smoothCy) * 0.5f - 10f
        strokePaint.color = when {
            !unlockedBoth -> AColor.argb(92, 130, 140, 148)
            isFortniteHybrid -> AColor.argb(214, 255, 255, 255)
            isDark -> AColor.argb(108, 255, 255, 255)
            else -> AColor.argb(184, 255, 252, 244)
        }
        strokePaint.strokeWidth = 2.3f
        val qPath = APath().apply {
            moveTo(a.smoothCx, a.smoothCy)
            quadTo(mx, my, b.smoothCx, b.smoothCy)
        }
        canvas.drawPath(qPath, strokePaint)

        if (road.hasBridge) {
            val by = (a.smoothCy + b.smoothCy) * 0.5f - 5f
            val ang = (atan2(b.smoothCy - a.smoothCy, b.smoothCx - a.smoothCx) * 180.0 / PI).toFloat()
            canvas.save()
            canvas.translate(mx, by)
            canvas.rotate(ang)
            fillPaint.color = parseColor(if (isDark) "#7E6850" else "#D5BF96")
            canvas.drawRoundRect(RectF(-14f, -5f, 14f, 5f), 5f, 5f, fillPaint)
            strokePaint.pathEffect = null
            strokePaint.color = parseColor(if (isDark) "#2B221A" else "#7C5D40")
            strokePaint.strokeWidth = 1.4f
            canvas.drawRoundRect(RectF(-14f, -5f, 14f, 5f), 5f, 5f, strokePaint)
            strokePaint.pathEffect = if (isFortniteHybrid) DashPathEffect(floatArrayOf(7f, 6f), 0f) else DashPathEffect(floatArrayOf(5f, 6f), 0f)
            canvas.restore()
        }
    }
    strokePaint.pathEffect = null

    if (isFortniteHybrid) {
        drawFortniteClouds(
            canvas, isDark,
            listOf(Triple(102f, 92f, 1.0f), Triple(846f, 122f, 0.94f), Triple(118f, 534f, 1.08f), Triple(832f, 488f, 0.90f))
        )
    }

    drawZoneLabelsAndBadges(
        canvas = canvas,
        hitGrid = hitGrid,
        lang = lang,
        isFortnitePoi = isFortniteHybrid,
        isSmoothCoords = true,
        elevPx = 13f,
        showAllColor = showAllColor,
        unlockedSet = unlockedSet,
        activeBiomeId = activeBiomeId,
        activeLevel = activeLevel,
        isDark = isDark
    )

    drawCompassRose(canvas, 890f, 610f, if (isDark) 0.42f else 0.65f, AColor.rgb(250, 248, 243))
}

private fun drawZoneLabelsAndBadges(
    canvas: ACanvas,
    hitGrid: ByteArray,
    lang: AppLanguage,
    isFortnitePoi: Boolean,
    isSmoothCoords: Boolean,
    elevPx: Float,
    showAllColor: Boolean,
    unlockedSet: Set<String>,
    activeBiomeId: String,
    activeLevel: Int,
    isDark: Boolean
) {
    val fillPaint = APaint(APaint.ANTI_ALIAS_FLAG).apply { style = APaint.Style.FILL }
    val strokePaint = APaint(APaint.ANTI_ALIAS_FLAG).apply { style = APaint.Style.STROKE }
    val textPaint = APaint(APaint.ANTI_ALIAS_FLAG).apply {
        textAlign = APaint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    WorldMapCache.zones.forEachIndexed { idx, z ->
        val (cx, cy) = if (isSmoothCoords) {
            z.smoothCx to z.smoothCy
        } else {
            WorldMapCache.projectIso(z.gxCenter.toFloat(), z.gyCenter.toFloat(), z.wzCenter, elevPx)
        }
        val isUser = z.id == activeBiomeId
        val unlocked = z.id in unlockedSet
        val grey = !showAllColor && !unlocked
        val lx = cx
        val ly = cy + if (isSmoothCoords && !isFortnitePoi) 18f else 21f
        val lbl = z.label(lang)

        if (isFortnitePoi) {
            fillPaint.color = if (grey) AColor.rgb(206, 210, 216) else AColor.WHITE
            canvas.drawCircle(cx, cy + 4f, 5.4f, fillPaint)
            strokePaint.color = when {
                isUser -> AColor.rgb(33, 138, 84)
                grey -> AColor.rgb(92, 98, 106)
                else -> AColor.rgb(24, 36, 44)
            }
            strokePaint.strokeWidth = if (isUser) 2.6f else 2.0f
            canvas.drawCircle(cx, cy + 4f, 5.4f, strokePaint)

            textPaint.textSize = 12f
            textPaint.style = APaint.Style.STROKE
            textPaint.strokeWidth = 3.6f
            textPaint.color = if (grey) AColor.argb(175, 42, 46, 52) else AColor.argb(210, 14, 24, 30)
            canvas.drawText(lbl, lx, ly + 6f, textPaint)

            textPaint.style = APaint.Style.FILL
            textPaint.color = if (grey) AColor.rgb(198, 203, 210) else AColor.WHITE
            canvas.drawText(lbl, lx, ly + 6f, textPaint)
        } else {
            val pillW = 94f
            val pillH = 21f
            val rect = RectF(lx - pillW * 0.5f, ly - pillH * 0.5f, lx + pillW * 0.5f, ly + pillH * 0.5f)
            fillPaint.color = when {
                grey && isDark -> AColor.argb(205, 34, 37, 42)
                grey -> AColor.argb(215, 228, 231, 236)
                isDark -> AColor.argb(228, 22, 25, 30)
                else -> AColor.argb(236, 255, 253, 248)
            }
            canvas.drawRoundRect(rect, 10.5f, 10.5f, fillPaint)
            strokePaint.color = when {
                isUser -> AColor.rgb(31, 122, 76)
                grey -> AColor.argb(72, 118, 124, 132)
                isDark -> AColor.argb(48, 255, 255, 255)
                else -> AColor.argb(34, 40, 38, 34)
            }
            strokePaint.strokeWidth = if (isUser) 2.2f else 1.1f
            canvas.drawRoundRect(rect, 10.5f, 10.5f, strokePaint)

            textPaint.style = APaint.Style.FILL
            textPaint.textSize = 9.6f
            textPaint.color = when {
                grey && isDark -> AColor.rgb(142, 147, 154)
                grey -> AColor.rgb(110, 116, 124)
                isDark -> AColor.rgb(241, 242, 244)
                else -> AColor.rgb(30, 42, 34)
            }
            canvas.drawText(lbl, lx, ly + 3.4f, textPaint)
        }

        if (isUser) {
            val tag = SplashStrings.t(lang, "Tú · Nv. $activeLevel", "You · Lvl. $activeLevel")
            val tagW = 86f
            val tagH = 19f
            val tagY = cy - 34f
            val tagRect = RectF(lx - tagW * 0.5f, tagY - tagH * 0.5f, lx + tagW * 0.5f, tagY + tagH * 0.5f)
            fillPaint.color = AColor.rgb(31, 122, 76)
            canvas.drawRoundRect(tagRect, 9.5f, 9.5f, fillPaint)
            strokePaint.color = AColor.WHITE
            strokePaint.strokeWidth = 1.4f
            canvas.drawRoundRect(tagRect, 9.5f, 9.5f, strokePaint)
            textPaint.style = APaint.Style.FILL
            textPaint.textSize = 9.5f
            textPaint.color = AColor.WHITE
            canvas.drawText(tag, lx, tagY + 3.3f, textPaint)
            markHitRect(hitGrid, tagRect.left - 4f, tagRect.top - 4f, tagRect.right + 4f, tagRect.bottom + 4f, idx + 1)
        }

        markHitRect(hitGrid, cx - 44f, cy - 42f, cx + 44f, cy + 36f, idx + 1)
        markHitRect(hitGrid, lx - 52f, ly - 16f, lx + 52f, ly + 16f, idx + 1)
    }
}

private fun drawFortniteClouds(canvas: ACanvas, isDark: Boolean, clouds: List<Triple<Float, Float, Float>>) {
    val fill = APaint(APaint.ANTI_ALIAS_FLAG).apply { style = APaint.Style.FILL }
    for ((cx, cy, s) in clouds) {
        fill.color = AColor.argb(40, 10, 24, 34)
        canvas.drawOval(RectF(cx + 8f - 34f * s, cy + 20f - 12f * s, cx + 8f + 34f * s, cy + 20f + 12f * s), fill)
        fill.color = if (isDark) AColor.argb(92, 220, 232, 244) else AColor.argb(194, 255, 255, 255)
        canvas.drawOval(RectF(cx - 32f * s, cy - 13f * s, cx + 32f * s, cy + 13f * s), fill)
        canvas.drawOval(RectF(cx + 20f * s - 21f * s, cy - 5f * s - 11f * s, cx + 20f * s + 21f * s, cy - 5f * s + 11f * s), fill)
        canvas.drawOval(RectF(cx - 18f * s - 18f * s, cy + 2f * s - 9f * s, cx - 18f * s + 18f * s, cy + 2f * s + 9f * s), fill)
    }
}

private fun drawCompassRose(canvas: ACanvas, cx: Float, cy: Float, alphaFrac: Float, col: Int) {
    val a = (alphaFrac * 255).roundToInt().coerceIn(0, 255)
    val fill = APaint(APaint.ANTI_ALIAS_FLAG).apply { style = APaint.Style.FILL }
    val stroke = APaint(APaint.ANTI_ALIAS_FLAG).apply { style = APaint.Style.STROKE; strokeWidth = 1.4f }
    canvas.save()
    canvas.translate(cx, cy)
    fill.color = AColor.argb((a * 0.25f).roundToInt(), 255, 255, 255)
    canvas.drawCircle(0f, 0f, 20f, fill)
    stroke.color = AColor.argb(a, AColor.red(col), AColor.green(col), AColor.blue(col))
    canvas.drawCircle(0f, 0f, 20f, stroke)

    val p = APath().apply {
        moveTo(0f, -15f); lineTo(4f, -3.5f); lineTo(0f, -1f); lineTo(-4f, -3.5f); close()
    }
    fill.color = AColor.argb(a, AColor.red(col), AColor.green(col), AColor.blue(col))
    canvas.drawPath(p, fill)
    canvas.restore()
}

private fun buildSmoothContinentPath(scale: Float, offsetX: Float, offsetY: Float): APath {
    val steps = 120
    val midX = 482f
    val midY = 336f
    val rxBase = 398f * scale
    val ryBase = 248f * scale
    val pts = ArrayList<Pair<Float, Float>>(steps)
    for (i in 0 until steps) {
        val t = (i.toFloat() / steps) * (2f * PI.toFloat())
        val cosT = cos(t)
        val sinT = sin(t)
        val wave = 0.95f + 0.055f * cos(t * 2f - 0.35f) + 0.042f * sin(t * 3f + 0.75f) + 0.025f * cos(t * 5f - 1.0f)
        val fjordNotch = if (sinT < -0.45f) 0.18f * exp(-((cosT + 0.04f) / 0.16f).pow(2)) else 0f
        val southBay = if (sinT > 0.45f) 0.16f * exp(-((cosT - 0.05f) / 0.22f).pow(2)) else 0f
        val r = wave - fjordNotch - southBay
        pts.add((midX + cosT * rxBase * r + offsetX) to (midY + sinT * ryBase * r + offsetY))
    }
    return traceClosedLoop(pts)
}

private fun buildOrganicMoundPath(cx: Float, cy: Float, rx: Float, ry: Float, rot: Float, seed: Int, wobble: Float): APath {
    val n = 20
    val cosR = cos(rot)
    val sinR = sin(rot)
    val pts = ArrayList<Pair<Float, Float>>(n)
    for (i in 0 until n) {
        val a = (i.toFloat() / n) * (2f * PI.toFloat())
        val mod = 1f + wobble * sin(a * 3f + seed) + wobble * 0.6f * cos(a * 2f - seed * 0.7f)
        val lx = cos(a) * rx * mod
        val ly = sin(a) * ry * mod
        pts.add((cx + lx * cosR - ly * sinR) to (cy + lx * sinR + ly * cosR))
    }
    return traceClosedLoop(pts)
}

private fun traceClosedLoop(pts: List<Pair<Float, Float>>): APath {
    val p = APath()
    if (pts.size < 3) return p
    val sx = (pts.last().first + pts.first().first) * 0.5f
    val sy = (pts.last().second + pts.first().second) * 0.5f
    p.moveTo(sx, sy)
    for (i in pts.indices) {
        val cur = pts[i]
        val next = pts[(i + 1) % pts.size]
        p.quadTo(cur.first, cur.second, (cur.first + next.first) * 0.5f, (cur.second + next.second) * 0.5f)
    }
    p.close()
    return p
}

private fun drawSmooth3DTerrace(
    canvas: ACanvas,
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    rot: Float,
    elevPx: Float,
    baseHex: String,
    isGrey: Boolean,
    isDark: Boolean,
    seed: Int,
    wobble: Float
) {
    val raw = parseColor(baseHex)
    val main = if (isGrey) toLockedGrey(raw, isDark) else raw
    val fill = APaint(APaint.ANTI_ALIAS_FLAG).apply { style = APaint.Style.FILL }
    val stroke = APaint(APaint.ANTI_ALIAS_FLAG).apply {
        style = APaint.Style.STROKE
        strokeWidth = 1.25f
        color = if (isDark) AColor.argb(36, 255, 255, 255) else AColor.argb(122, 255, 255, 255)
    }

    fill.color = if (isDark) AColor.argb(72, 0, 0, 0) else AColor.argb(40, 20, 42, 48)
    canvas.drawPath(buildOrganicMoundPath(cx + 4f, cy + max(4f, elevPx * 0.35f), rx * 1.03f, ry * 1.03f, rot, seed, wobble), fill)

    fill.shader = LinearGradient(
        cx - rx, cy, cx + rx, cy + elevPx,
        mixColor(main, parseColor("#1E2A26"), if (isDark) 0.45f else 0.28f),
        mixColor(main, parseColor("#0E1714"), if (isDark) 0.62f else 0.42f),
        Shader.TileMode.CLAMP
    )
    canvas.drawPath(buildOrganicMoundPath(cx, cy, rx, ry, rot, seed, wobble), fill)
    fill.shader = null

    if (elevPx > 4f) {
        fill.color = mixColor(main, parseColor("#182420"), if (isDark) 0.38f else 0.24f)
        canvas.drawPath(buildOrganicMoundPath(cx, cy - elevPx * 0.5f, rx, ry, rot, seed, wobble), fill)
    }

    val topPath = buildOrganicMoundPath(cx, cy - elevPx, rx, ry, rot, seed, wobble)
    fill.shader = LinearGradient(
        cx - rx, cy - elevPx - ry, cx + rx, cy - elevPx + ry,
        intArrayOf(
            mixColor(main, AColor.WHITE, if (isDark) 0.10f else 0.22f),
            main,
            mixColor(main, parseColor("#2A3632"), if (isDark) 0.25f else 0.12f)
        ),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawPath(topPath, fill)
    fill.shader = null
    canvas.drawPath(topPath, stroke)
}

private fun drawSmoothPeak(
    canvas: ACanvas,
    cx: Float,
    cy: Float,
    baseW: Float,
    h: Float,
    capRatio: Float,
    rockHex: String,
    snowHex: String,
    isGrey: Boolean,
    isDark: Boolean
) {
    fun cOf(hex: String) = if (isGrey) toLockedGrey(parseColor(hex), isDark) else parseColor(hex)
    val rCol = cOf(rockHex)
    val sCol = cOf(snowHex)
    val hw = baseW * 0.5f
    val fill = APaint(APaint.ANTI_ALIAS_FLAG).apply { style = APaint.Style.FILL }

    fill.color = if (isDark) AColor.argb(72, 0, 0, 0) else AColor.argb(40, 18, 38, 44)
    canvas.drawOval(RectF(cx + 6f - hw * 1.04f, cy + 4f - hw * 0.36f, cx + 6f + hw * 1.04f, cy + 4f + hw * 0.36f), fill)

    val pLeft = APath().apply {
        moveTo(cx - hw, cy)
        quadTo(cx - hw * 0.38f, cy - h * 0.65f, cx, cy - h)
        quadTo(cx - hw * 0.04f, cy - h * 0.35f, cx, cy + hw * 0.24f)
        close()
    }
    fill.color = mixColor(rCol, AColor.WHITE, if (isDark) 0.08f else 0.18f)
    canvas.drawPath(pLeft, fill)

    val pRight = APath().apply {
        moveTo(cx + hw, cy)
        quadTo(cx + hw * 0.38f, cy - h * 0.65f, cx, cy - h)
        quadTo(cx - hw * 0.04f, cy - h * 0.35f, cx, cy + hw * 0.24f)
        close()
    }
    fill.color = mixColor(rCol, parseColor("#1C262E"), if (isDark) 0.42f else 0.25f)
    canvas.drawPath(pRight, fill)

    if (capRatio > 0f) {
        val sw = hw * capRatio
        val sh = h * capRatio
        val sLeft = APath().apply {
            moveTo(cx - sw, cy - h + sh)
            quadTo(cx - sw * 0.4f, cy - h + sh * 0.35f, cx, cy - h)
            lineTo(cx, cy - h + sh * 1.12f)
            quadTo(cx - sw * 0.45f, cy - h + sh * 0.90f, cx - sw, cy - h + sh)
            close()
        }
        fill.color = sCol
        canvas.drawPath(sLeft, fill)

        val sRight = APath().apply {
            moveTo(cx + sw, cy - h + sh)
            quadTo(cx + sw * 0.4f, cy - h + sh * 0.35f, cx, cy - h)
            lineTo(cx, cy - h + sh * 1.12f)
            quadTo(cx + sw * 0.45f, cy - h + sh * 0.90f, cx + sw, cy - h + sh)
            close()
        }
        fill.color = mixColor(sCol, parseColor("#8AA8C0"), if (isDark) 0.36f else 0.22f)
        canvas.drawPath(sRight, fill)
    }
}

private fun drawSmoothLandmark(canvas: ACanvas, bid: String, x: Float, y: Float, isGrey: Boolean, isDark: Boolean) {
    fun cOf(hex: String) = if (isGrey) toLockedGrey(parseColor(hex), isDark) else parseColor(hex)
    val fill = APaint(APaint.ANTI_ALIAS_FLAG).apply { style = APaint.Style.FILL }
    val stroke = APaint(APaint.ANTI_ALIAS_FLAG).apply { style = APaint.Style.STROKE; strokeCap = APaint.Cap.ROUND }

    fill.color = if (isDark) AColor.argb(82, 0, 0, 0) else AColor.argb(46, 18, 34, 30)
    canvas.drawOval(RectF(x - 15f, y - 0.5f, x + 19f, y + 12.5f), fill)

    when (bid) {
        "costa" -> {
            fill.color = cOf("#F7EBD5")
            canvas.drawRoundRect(RectF(x - 15f, y - 26f, x - 3f, y + 3f), 3f, 3f, fill)
            fill.color = cOf("#D97757")
            canvas.drawRoundRect(RectF(x - 14.5f, y - 16f, x - 3.5f, y - 11f), 2.5f, 2.5f, fill)
            canvas.drawRoundRect(RectF(x - 14f, y - 29f, x - 4f, y - 24.5f), 2f, 2f, fill)
            fill.color = cOf("#FFF2AA")
            canvas.drawCircle(x - 9f, y - 33f, 4.2f, fill)
            fill.color = cOf("#F7EBD5")
            canvas.drawRoundRect(RectF(x + 2f, y - 9f, x + 17f, y + 2f), 4f, 4f, fill)
            fill.color = cOf("#3574B5")
            canvas.drawArc(RectF(x + 3.5f, y - 15f, x + 15.5f, y - 3f), 180f, 180f, true, fill)
        }
        "kioto" -> {
            listOf(Triple(-24f, -4f, 9f), Triple(-14f, -10f, 8f), Triple(21f, -5f, 9.5f)).forEachIndexed { i, (dx, dy, r) ->
                fill.color = cOf(if (i % 2 == 0) "#F6B6C9" else "#FAD0DC")
                canvas.drawCircle(x + dx, y + dy, r, fill)
            }
            fill.color = cOf("#FAF0E4")
            canvas.drawRoundRect(RectF(x + 5f, y - 16f, x + 17f, y + 1f), 2.5f, 2.5f, fill)
            fill.color = cOf("#C95240")
            val roof = APath().apply { moveTo(x + 2f, y - 8f); quadTo(x + 11f, y - 15f, x + 20f, y - 8f); close() }
            canvas.drawPath(roof, fill)
            canvas.drawRoundRect(RectF(x - 16f, y - 12f, x - 12.5f, y + 4f), 1.5f, 1.5f, fill)
            canvas.drawRoundRect(RectF(x - 2f, y - 12f, x + 1.5f, y + 4f), 1.5f, 1.5f, fill)
            canvas.drawRoundRect(RectF(x - 20f, y - 14f, x + 4f, y - 10f), 2f, 2f, fill)
        }
        "valle" -> {
            listOf(Triple(-22f, 2f, 17f), Triple(18f, 2f, 18f)).forEach { (dx, dy, h) ->
                fill.color = cOf("#3E7347")
                val tree = APath().apply {
                    moveTo(x + dx, y + dy - h)
                    lineTo(x + dx + 6.5f, y + dy + 3f)
                    lineTo(x + dx - 6.5f, y + dy + 3f)
                    close()
                }
                canvas.drawPath(tree, fill)
            }
            fill.color = cOf("#FAF0DC")
            canvas.drawRoundRect(RectF(x - 9f, y - 9f, x + 9f, y + 3f), 3f, 3f, fill)
            fill.color = cOf("#C46747")
            val cr = APath().apply { moveTo(x - 12f, y - 8f); quadTo(x, y - 20f, x + 12f, y - 8f); close() }
            canvas.drawPath(cr, fill)
        }
        "fiordo" -> {
            fill.color = cOf("#B84438")
            canvas.drawRoundRect(RectF(x - 19f, y - 9f, x - 3f, y + 2f), 3f, 3f, fill)
            fill.color = cOf("#F2F7FC")
            val r1 = APath().apply { moveTo(x - 22f, y - 8f); lineTo(x - 11f, y - 18f); lineTo(x, y - 8f); close() }
            canvas.drawPath(r1, fill)
            fill.color = cOf("#704C34")
            canvas.drawOval(RectF(x + 12f, y + 1f, x + 32f, y + 8f), fill)
            fill.color = cOf("#D65B4B")
            val sail = APath().apply { moveTo(x + 22f, y + 2f); quadTo(x + 30f, y - 6f, x + 22f, y - 14f); close() }
            canvas.drawPath(sail, fill)
        }
        "desierto" -> {
            fill.color = cOf("#DFB16D")
            canvas.drawRoundRect(RectF(x - 10f, y - 10f, x + 10f, y + 2f), 4f, 4f, fill)
            fill.color = cOf("#FAF0DA")
            canvas.drawArc(RectF(x - 8f, y - 18f, x + 8f, y - 2f), 180f, 180f, true, fill)
            stroke.color = cOf("#8B6342"); stroke.strokeWidth = 2.4f
            canvas.drawLine(x - 20f, y + 4f, x - 17f, y - 15f, stroke)
            canvas.drawLine(x + 19f, y + 5f, x + 22f, y - 13f, stroke)
            fill.color = cOf("#558E52")
            canvas.drawCircle(x - 17f, y - 16f, 5.2f, fill)
            canvas.drawCircle(x + 22f, y - 14f, 5.2f, fill)
        }
        "toscana" -> {
            fill.color = cOf("#F7E8CC")
            canvas.drawRoundRect(RectF(x - 11f, y - 11f, x + 11f, y + 3f), 4f, 4f, fill)
            fill.color = cOf("#C86848")
            val tr = APath().apply { moveTo(x - 14f, y - 10f); quadTo(x, y - 20f, x + 14f, y - 10f); close() }
            canvas.drawPath(tr, fill)
            fill.color = cOf("#4D6D3B")
            listOf(Triple(-23f, -4f, 10f), Triple(-17f, -9f, 12f), Triple(18f, -8f, 12f)).forEach { (dx, dy, ry) ->
                canvas.drawOval(RectF(x + dx - 3.6f, y + dy - ry, x + dx + 3.6f, y + dy + ry), fill)
            }
        }
        "jungla" -> {
            listOf(Triple(-22f, 1f, 9f), Triple(-14f, -5f, 10f), Triple(15f, -4f, 10f)).forEach { (dx, dy, r) ->
                fill.color = cOf("#2B6C42")
                canvas.drawCircle(x + dx, y + dy, r, fill)
            }
            fill.color = cOf("#7D8F82")
            canvas.drawRoundRect(RectF(x - 12f, y - 8f, x + 12f, y + 4f), 4f, 4f, fill)
            canvas.drawRoundRect(RectF(x - 8f, y - 16f, x + 8f, y - 8f), 3f, 3f, fill)
            canvas.drawRoundRect(RectF(x - 4.5f, y - 22f, x + 4.5f, y - 16f), 2.5f, 2.5f, fill)
        }
        "arrozales" -> {
            fill.color = cOf("#D6BA8D")
            canvas.drawRoundRect(RectF(x - 9f, y - 7f, x + 9f, y + 3f), 3f, 3f, fill)
            fill.color = cOf("#3E3A36")
            val r1 = APath().apply { moveTo(x - 13f, y - 7f); quadTo(x, y - 17f, x + 13f, y - 7f); close() }
            val r2 = APath().apply { moveTo(x - 9f, y - 14f); quadTo(x, y - 23f, x + 9f, y - 14f); close() }
            canvas.drawPath(r1, fill)
            canvas.drawPath(r2, fill)
        }
        "otono" -> {
            listOf(Triple(-22f, -1f, "#C83B2B"), Triple(-14f, -7f, "#EC7628"), Triple(16f, -6f, "#F3B438")).forEach { (dx, dy, hc) ->
                fill.color = cOf(hc)
                canvas.drawCircle(x + dx, y + dy, 8.5f, fill)
            }
            fill.color = cOf("#8B593B")
            canvas.drawRoundRect(RectF(x - 9f, y - 9f, x + 9f, y + 3f), 3f, 3f, fill)
            fill.color = cOf("#C4492D")
            val ar = APath().apply { moveTo(x - 12f, y - 8f); quadTo(x, y - 18f, x + 12f, y - 8f); close() }
            canvas.drawPath(ar, fill)
        }
        else -> {
            stroke.color = cOf("#9B764F"); stroke.strokeWidth = 2.6f
            canvas.drawLine(x - 18f, y + 3f, x + 18f, y + 3f, stroke)
            listOf(-14f to 0f, 4f to -4f).forEach { (dx, dy) ->
                fill.color = cOf("#FAF0DC")
                canvas.drawRoundRect(RectF(x + dx - 7f, y + dy - 7f, x + dx + 7f, y + dy + 2f), 3f, 3f, fill)
                fill.color = cOf("#D98752")
                val hr = APath().apply { moveTo(x + dx - 10f, y + dy - 6f); quadTo(x + dx, y + dy - 14f, x + dx + 10f, y + dy - 6f); close() }
                canvas.drawPath(hr, fill)
            }
        }
    }
}

private fun hash2DSimple(x: Float, y: Float, seed: Int): Float {
    val n = sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453123
    return (n - floor(n)).toFloat()
}
