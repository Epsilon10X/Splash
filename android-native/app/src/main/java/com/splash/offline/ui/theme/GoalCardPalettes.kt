package com.splash.offline.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.splash.offline.data.CatalogFilter
import com.splash.offline.data.GoalCategory
import com.splash.offline.data.TimeBucket

data class ExpressiveCategoryTone(
    val container: Color,
    val onContainer: Color,
    val accent: Color,
    val onAccent: Color,
    val pillBg: Color,
    val pillFg: Color
)

data class CategoryCardStyle(
    val bgStart: Color,
    val bgMid: Color,
    val bgEnd: Color,
    val onCard: Color,
    val chipBg: Color,
    val buttonBg: Color,
    val buttonInk: Color,
    val pillActive: Color,
    val borderColor: Color
)

data class BiomePostcardStyle(
    val bg1: Color,
    val bg2: Color,
    val border: Color,
    val accent: Color,
    val accentSoft: Color,
    val ink: Color,
    val sub: Color,
    val buttonInk: Color,
    val c1: Color,
    val c2: Color,
    val c3: Color
)

object GoalCardPalettes {
    @Composable
    fun isSplashDarkTheme(): Boolean = MaterialTheme.colorScheme.background == Color.Black

    fun expressiveTone(category: GoalCategory, isDark: Boolean): ExpressiveCategoryTone = when (category) {
        GoalCategory.RAPIDO -> if (isDark) {
            ExpressiveCategoryTone(
                container = Color(0xFF1E3616),
                onContainer = Color(0xFFE0F5C4),
                accent = Color(0xFF9BE06B),
                onAccent = Color(0xFF143008),
                pillBg = Color(0xFF9BE06B),
                pillFg = Color(0xFF143008)
            )
        } else {
            ExpressiveCategoryTone(
                container = Color(0xFFE2F4C8),
                onContainer = Color(0xFF1C3D0C),
                accent = Color(0xFF4B9426),
                onAccent = Color.White,
                pillBg = Color(0xFF4B9426),
                pillFg = Color.White
            )
        }

        GoalCategory.MEDIO -> if (isDark) {
            ExpressiveCategoryTone(
                container = Color(0xFF422F0B),
                onContainer = Color(0xFFFFF0B8),
                accent = Color(0xFFFFCE44),
                onAccent = Color(0xFF3A2700),
                pillBg = Color(0xFFFFCE44),
                pillFg = Color(0xFF3A2700)
            )
        } else {
            ExpressiveCategoryTone(
                container = Color(0xFFFFF0B3),
                onContainer = Color(0xFF4A3000),
                accent = Color(0xFFE09E0C),
                onAccent = Color.White,
                pillBg = Color(0xFFE09E0C),
                pillFg = Color.White
            )
        }

        GoalCategory.LARGO -> if (isDark) {
            ExpressiveCategoryTone(
                container = Color(0xFF47181B),
                onContainer = Color(0xFFFFDAD6),
                accent = Color(0xFFF2847E),
                onAccent = Color(0xFF4B0E12),
                pillBg = Color(0xFFF2847E),
                pillFg = Color(0xFF4B0E12)
            )
        } else {
            ExpressiveCategoryTone(
                container = Color(0xFFFFD8D0),
                onContainer = Color(0xFF571216),
                accent = Color(0xFFB82E2E),
                onAccent = Color.White,
                pillBg = Color(0xFFB82E2E),
                pillFg = Color.White
            )
        }
    }

    fun biomeUnifiedTone(isDark: Boolean): ExpressiveCategoryTone = if (isDark) {
        ExpressiveCategoryTone(
            container = Color(0xFF3A2E10),
            onContainer = Color(0xFFFCEFC2),
            accent = Color(0xFFECC24E),
            onAccent = Color(0xFF362500),
            pillBg = Color(0xFFECC24E),
            pillFg = Color(0xFF362500)
        )
    } else {
        ExpressiveCategoryTone(
            container = Color(0xFFF9ECC2),
            onContainer = Color(0xFF453100),
            accent = Color(0xFFB68312),
            onAccent = Color.White,
            pillBg = Color(0xFFB68312),
            pillFg = Color.White
        )
    }

    fun catalogFilterTonalColors(filter: CatalogFilter, isDark: Boolean): Pair<Color, Color> = when (filter) {
        CatalogFilter.ALL -> if (isDark) {
            Color(0xFF133625) to Color(0xFFC2F0D6)
        } else {
            Color(0xFFD0ECDC) to Color(0xFF0E3D26)
        }
        CatalogFilter.RAPIDO -> expressiveTone(GoalCategory.RAPIDO, isDark).let { it.container to it.onContainer }
        CatalogFilter.MEDIO -> expressiveTone(GoalCategory.MEDIO, isDark).let { it.container to it.onContainer }
        CatalogFilter.LARGO -> expressiveTone(GoalCategory.LARGO, isDark).let { it.container to it.onContainer }
        CatalogFilter.BIOME -> biomeUnifiedTone(isDark).let { it.container to it.onContainer }
    }

    fun timeBucketTonalColors(bucket: TimeBucket, isDark: Boolean): Pair<Color, Color> = when (bucket) {
        TimeBucket.QUICK_2M -> expressiveTone(GoalCategory.RAPIDO, isDark).let { it.container to it.onContainer }
        TimeBucket.MEDIUM_15M -> expressiveTone(GoalCategory.MEDIO, isDark).let { it.container to it.onContainer }
        TimeBucket.WEEKLY -> expressiveTone(GoalCategory.LARGO, isDark).let { it.container to it.onContainer }
    }

    fun categoryCardStyle(category: GoalCategory, isDark: Boolean): CategoryCardStyle = when (category) {
        GoalCategory.RAPIDO -> if (isDark) {
            CategoryCardStyle(
                bgStart = Color(0xFF1E3616),
                bgMid = Color(0xFF182C11),
                bgEnd = Color(0xFF13230D),
                onCard = Color(0xFFE0F5C4),
                chipBg = Color(0xFF9BE06B).copy(alpha = 0.18f),
                buttonBg = Color(0xFF9BE06B),
                buttonInk = Color(0xFF143008),
                pillActive = Color(0xFF9BE06B),
                borderColor = Color(0xFF9BE06B).copy(alpha = 0.24f)
            )
        } else {
            CategoryCardStyle(
                bgStart = Color(0xFFECF9D6),
                bgMid = Color(0xFFE2F4C8),
                bgEnd = Color(0xFFD1EBAD),
                onCard = Color(0xFF1C3D0C),
                chipBg = Color(0xFF4B9426).copy(alpha = 0.14f),
                buttonBg = Color(0xFF4B9426),
                buttonInk = Color.White,
                pillActive = Color(0xFF4B9426),
                borderColor = Color(0xFF4B9426).copy(alpha = 0.22f)
            )
        }

        GoalCategory.MEDIO -> if (isDark) {
            CategoryCardStyle(
                bgStart = Color(0xFF422F0B),
                bgMid = Color(0xFF362608),
                bgEnd = Color(0xFF2B1E06),
                onCard = Color(0xFFFFF0B8),
                chipBg = Color(0xFFFFCE44).copy(alpha = 0.18f),
                buttonBg = Color(0xFFFFCE44),
                buttonInk = Color(0xFF3A2700),
                pillActive = Color(0xFFFFCE44),
                borderColor = Color(0xFFFFCE44).copy(alpha = 0.24f)
            )
        } else {
            CategoryCardStyle(
                bgStart = Color(0xFFFFF6CC),
                bgMid = Color(0xFFFFF0B3),
                bgEnd = Color(0xFFFFE18A),
                onCard = Color(0xFF4A3000),
                chipBg = Color(0xFFE09E0C).copy(alpha = 0.14f),
                buttonBg = Color(0xFFE09E0C),
                buttonInk = Color.White,
                pillActive = Color(0xFFE09E0C),
                borderColor = Color(0xFFE09E0C).copy(alpha = 0.22f)
            )
        }

        GoalCategory.LARGO -> if (isDark) {
            CategoryCardStyle(
                bgStart = Color(0xFF47181B),
                bgMid = Color(0xFF3B1316),
                bgEnd = Color(0xFF2F0F11),
                onCard = Color(0xFFFFDAD6),
                chipBg = Color(0xFFF2847E).copy(alpha = 0.18f),
                buttonBg = Color(0xFFF2847E),
                buttonInk = Color(0xFF4B0E12),
                pillActive = Color(0xFFF2847E),
                borderColor = Color(0xFFF2847E).copy(alpha = 0.24f)
            )
        } else {
            CategoryCardStyle(
                bgStart = Color(0xFFFFE3DD),
                bgMid = Color(0xFFFFD8D0),
                bgEnd = Color(0xFFF9C1B8),
                onCard = Color(0xFF571216),
                chipBg = Color(0xFFB82E2E).copy(alpha = 0.14f),
                buttonBg = Color(0xFFB82E2E),
                buttonInk = Color.White,
                pillActive = Color(0xFFB82E2E),
                borderColor = Color(0xFFB82E2E).copy(alpha = 0.22f)
            )
        }
    }

    fun biomePostcardStyle(biomeId: String, isDark: Boolean): BiomePostcardStyle {
        val cTriplet = when (biomeId) {
            "valle" -> Triple(Color(0xFF569B58), Color(0xFF6CC6DF), Color(0xFFDDEAF2))
            "jungla" -> Triple(Color(0xFF1B472C), Color(0xFF56AEBC), Color(0xFF2E6943))
            "costa" -> Triple(Color(0xFF77C4D4), Color(0xFFE6CFA5), Color(0xFFD97757))
            "kioto" -> Triple(Color(0xFFF7CED9), Color(0xFFE5BDD0), Color(0xFF6DA6AD))
            "desierto" -> Triple(Color(0xFFE2BA7A), Color(0xFF4EB2B0), Color(0xFFC38E4B))
            "arrozales" -> Triple(Color(0xFF4E9952), Color(0xFF5DC4D2), Color(0xFF74C16B))
            "otono" -> Triple(Color(0xFFD9342B), Color(0xFFF27B24), Color(0xFFF8C73A))
            "fiordo" -> Triple(Color(0xFF417FA3), Color(0xFFD8E7F2), Color(0xFFB84438))
            "atolon" -> Triple(Color(0xFF4CC7C4), Color(0xFFF4E5C8), Color(0xFFEE7A74))
            "toscana" -> Triple(Color(0xFF8DA45A), Color(0xFFDFB66A), Color(0xFFC96D4B))
            else -> Triple(Color(0xFF569B58), Color(0xFF6CC6DF), Color(0xFFDDEAF2))
        }

        val unified = biomeUnifiedTone(isDark)
        return if (isDark) {
            BiomePostcardStyle(
                bg1 = Color(0xFF3A2E10),
                bg2 = Color(0xFF2C220B),
                border = unified.accent.copy(alpha = 0.30f),
                accent = unified.accent,
                accentSoft = unified.accent.copy(alpha = 0.16f),
                ink = unified.onContainer,
                sub = unified.onContainer.copy(alpha = 0.78f),
                buttonInk = unified.onAccent,
                c1 = cTriplet.first,
                c2 = cTriplet.second,
                c3 = cTriplet.third
            )
        } else {
            BiomePostcardStyle(
                bg1 = Color(0xFFFDF3CE),
                bg2 = Color(0xFFF5E2A3),
                border = unified.accent.copy(alpha = 0.28f),
                accent = unified.accent,
                accentSoft = unified.accent.copy(alpha = 0.14f),
                ink = unified.onContainer,
                sub = unified.onContainer.copy(alpha = 0.78f),
                buttonInk = unified.onAccent,
                c1 = cTriplet.first,
                c2 = cTriplet.second,
                c3 = cTriplet.third
            )
        }
    }
}
