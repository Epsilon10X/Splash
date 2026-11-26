package com.splash.offline.data

import androidx.compose.ui.graphics.Color

enum class GoalCategory {
    RAPIDO, MEDIO, LARGO
}

enum class VerificationType {
    TOQUE, TEMPORIZADOR, SOCIAL, PUENTE_ANALOGICO
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppLanguage(val code: String) {
    ES("es"), EN("en")
}

enum class AppTab {
    HOY, MURO, CATALOGO, BIOMAS
}

enum class TimeBucket {
    QUICK_2M, MEDIUM_15M, WEEKLY
}

enum class WallSubTab {
    MY_WALL, MEMORIES, CIRCLE, MAP
}

enum class BiomeLayoutMode {
    MINI, STACK, LARGE
}

enum class CatalogFilter {
    ALL, RAPIDO, MEDIO, LARGO, BIOME
}

enum class WidgetThemePref(val wire: String) {
    AUTO("auto"), LIGHT("light"), DARK("dark")
}

enum class MapStyle {
    PRUEBA_1, PRUEBA_2, PRUEBA_3, PRUEBA_4
}

data class GoalItem(
    val id: Int,
    val titleShortEs: String,
    val titleEs: String,
    val subEs: String,
    val titleShortEn: String,
    val titleEn: String,
    val subEn: String,
    val category: GoalCategory,
    val iconName: String,
    val verification: VerificationType,
    val biomeExclusive: String? = null,
    val isBiomeSecret: Boolean = false,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isCustom: Boolean = false
) {
    fun displayTitle(lang: AppLanguage): String = if (lang == AppLanguage.EN) titleEn else titleEs
    fun displayShort(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) titleShortEn.ifBlank { titleEn } else titleShortEs.ifBlank { titleEs }
    fun displaySub(lang: AppLanguage): String = if (lang == AppLanguage.EN) subEn else subEs
}

data class BiomeDef(
    val id: String,
    val nameEs: String,
    val shortEs: String,
    val subtitleEs: String,
    val nameEn: String,
    val shortEn: String,
    val subtitleEn: String,
    val accentHex: Color,
    val secondaryHex: Color,
    val tertiaryHex: Color,
    val mapX: Float,
    val mapY: Float
) {
    fun name(lang: AppLanguage) = if (lang == AppLanguage.EN) nameEn else nameEs
    fun shortName(lang: AppLanguage) = if (lang == AppLanguage.EN) shortEn else shortEs
    fun subtitle(lang: AppLanguage) = if (lang == AppLanguage.EN) subtitleEn else subtitleEs
}

data class StageDef(
    val stage: Int,
    val titleEs: String,
    val descEs: String,
    val titleEn: String,
    val descEn: String
) {
    fun title(lang: AppLanguage) = if (lang == AppLanguage.EN) titleEn else titleEs
    fun desc(lang: AppLanguage) = if (lang == AppLanguage.EN) descEn else descEs
}

data class CompletionLog(
    val id: Long,
    val dateIso: String,
    val timestampMs: Long,
    val goalId: Int,
    val titleEs: String,
    val titleEn: String,
    val category: GoalCategory,
    val biomeId: String,
    val photoPath: String? = null,
    val earnedXp: Int = 0
)

data class DaySchedule(
    val enabled: Boolean,
    val hours: Set<Int>
)

data class ActiveSession(
    val sessionKey: String,
    val goal: GoalItem,
    val durationMinutes: Int,
    val totalSeconds: Int = (durationMinutes * 60).coerceAtLeast(30),
    val remainingSeconds: Int = (durationMinutes * 60).coerceAtLeast(30),
    val isRunning: Boolean = false,
    val runningEndElapsedMs: Long? = null,
    val pausedByAppExit: Boolean = false,
    val isFinished: Boolean = false,
    val isBiomeMission: Boolean = false,
    val isExtra: Boolean = false,
    val isFromWidget: Boolean = false
)

data class PendingCameraGoal(
    val goal: GoalItem? = null,
    val isBiomeMission: Boolean = false,
    val isExtra: Boolean = false,
    val isStandaloneMemory: Boolean = false
)

data class SplashUiState(
    val lang: AppLanguage = AppLanguage.ES,
    val isLanguageSwitching: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val widgetThemePref: WidgetThemePref = WidgetThemePref.AUTO,
    val userName: String = "Explorador",
    val activeTab: AppTab = AppTab.HOY,
    val timeBucket: TimeBucket = TimeBucket.QUICK_2M,
    val wallSubTab: WallSubTab = WallSubTab.MY_WALL,
    val mapStyle: MapStyle = MapStyle.PRUEBA_1,
    val mapShowAllColor: Boolean = true,
    val biomeLayoutMode: BiomeLayoutMode = BiomeLayoutMode.STACK,
    val activeBiomeIndex: Int = 0,
    val unlockedBiomeCount: Int = 1,
    val biomeXpById: Map<String, Int> = mapOf("valle" to 0),
    val betaMode: Boolean = false,
    val betaStageOverride: Int? = null,
    val todayDateIso: String = "",
    val todayWeekIso: String = "",
    val quickUsedToday: Int = 0,
    val mediumUsedToday: Int = 0,
    val weeklyUsedThisWeek: Int = 0,
    val currentQuickIds: Pair<Int, Int> = 1 to 101,
    val currentMediumIds: Pair<Int, Int> = 21 to 102,
    val currentWeeklyIds: List<Int> = listOf(38, 39, 40),
    val extraQuickGoalId: Int? = null,
    val extraMediumGoalId: Int? = null,
    val savedTimerRemainingSec: Map<String, Int> = emptyMap(),
    val catalog: List<GoalItem> = emptyList(),
    val completions: List<CompletionLog> = emptyList(),
    val selectedMonthOffset: Int = 0,
    val selectedDayIso: String = "",
    val catalogSearch: String = "",
    val catalogFilter: CatalogFilter = CatalogFilter.ALL,
    val notifSchedule: Map<String, DaySchedule> = emptyMap(),
    val activeSession: ActiveSession? = null,
    val is3DOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isFeedbackOpen: Boolean = false,
    val isCreateGoalOpen: Boolean = false,
    val pendingCameraGoal: PendingCameraGoal? = null,
    val isAmbientAudioPlaying: Boolean = false,
    val dioramaNightPreview: Boolean = false,
    val toastMessage: String? = null
) {
    val activeBiome: BiomeDef
        get() = SplashCatalogData.BIOMES.getOrElse(activeBiomeIndex) { SplashCatalogData.BIOMES.first() }

    val activeBiomeXp: Int
        get() = (biomeXpById[activeBiome.id] ?: 0).coerceIn(0, 80)

    val effectiveBiomeLevel: Int
        get() = betaStageOverride?.coerceIn(1, 8)
            ?: ((activeBiomeXp / 10) + 1).coerceIn(1, 8)

    val streakDays: Int
        get() = completions.map { it.dateIso }.distinct().size
}

object SplashCatalogData {
    val BIOMES: List<BiomeDef> = listOf(
        BiomeDef(
            id = "valle",
            nameEs = "Valle Alpino",
            shortEs = "Valle",
            subtitleEs = "Valle verde, lago alpino y cima nevada",
            nameEn = "Alpine Valley",
            shortEn = "Valley",
            subtitleEn = "Green valley, alpine lake & snowy peaks",
            accentHex = Color(0xFF569B58),
            secondaryHex = Color(0xFF6CC6DF),
            tertiaryHex = Color(0xFFF8FAFC),
            mapX = 0.24f,
            mapY = 0.24f
        ),
        BiomeDef(
            id = "jungla",
            nameEs = "Jungla Lluviosa",
            shortEs = "Jungla",
            subtitleEs = "Cañón esmeralda, cascada, templos y lluvia",
            nameEn = "Rainy Jungle",
            shortEn = "Jungle",
            subtitleEn = "Emerald canyon, waterfall, temples & rain",
            accentHex = Color(0xFF2E6943),
            secondaryHex = Color(0xFF56AEBC),
            tertiaryHex = Color(0xFF1B472C),
            mapX = 0.25f,
            mapY = 0.63f
        ),
        BiomeDef(
            id = "costa",
            nameEs = "Costa Mediterránea",
            shortEs = "Costa",
            subtitleEs = "Acantilados crema, cala y faro",
            nameEn = "Mediterranean Coast",
            shortEn = "Coast",
            subtitleEn = "Warm cliffs, blue cove & lighthouse",
            accentHex = Color(0xFF52AEC2),
            secondaryHex = Color(0xFFE6CFA5),
            tertiaryHex = Color(0xFFD97757),
            mapX = 0.79f,
            mapY = 0.45f
        ),
        BiomeDef(
            id = "kioto",
            nameEs = "Valle de Kioto",
            shortEs = "Kioto",
            subtitleEs = "Almendros rosas, puente y pagoda",
            nameEn = "Kyoto Valley",
            shortEn = "Kyoto",
            subtitleEn = "Pink blossoms, wooden bridge & pagoda",
            accentHex = Color(0xFFE08AA2),
            secondaryHex = Color(0xFF6DA6AD),
            tertiaryHex = Color(0xFFF7CED9),
            mapX = 0.76f,
            mapY = 0.25f
        ),
        BiomeDef(
            id = "desierto",
            nameEs = "Oasis y Dunas",
            shortEs = "Oasis",
            subtitleEs = "Dunas esculpidas, poza turquesa y arco",
            nameEn = "Oasis & Dunes",
            shortEn = "Oasis",
            subtitleEn = "Sculpted dunes, turquoise pool & stone arch",
            accentHex = Color(0xFFD89D4E),
            secondaryHex = Color(0xFF4EB2B0),
            tertiaryHex = Color(0xFFC38E4B),
            mapX = 0.53f,
            mapY = 0.59f
        ),
        BiomeDef(
            id = "arrozales",
            nameEs = "Terrazas de Bali",
            shortEs = "Bali",
            subtitleEs = "Arrozales inundados, bambú y templo",
            nameEn = "Bali Terraces",
            shortEn = "Bali",
            subtitleEn = "Flooded rice terraces, bamboo & temple",
            accentHex = Color(0xFF4E9952),
            secondaryHex = Color(0xFF5DC4D2),
            tertiaryHex = Color(0xFF74C16B),
            mapX = 0.75f,
            mapY = 0.67f
        ),
        BiomeDef(
            id = "otono",
            nameEs = "Bosque de Otoño",
            shortEs = "Otoño",
            subtitleEs = "Arces rojos, naranjas, dorados y lago",
            nameEn = "Autumn Woods",
            shortEn = "Autumn",
            subtitleEn = "Red, orange & golden maples by the lake",
            accentHex = Color(0xFFD95A2B),
            secondaryHex = Color(0xFFF8C73A),
            tertiaryHex = Color(0xFF5A9CC4),
            mapX = 0.17f,
            mapY = 0.43f
        ),
        BiomeDef(
            id = "fiordo",
            nameEs = "Fiordo Nórdico",
            shortEs = "Fiordo",
            subtitleEs = "Fiordo amplio, hielo, abetos nevados y navío",
            nameEn = "Nordic Fjord",
            shortEn = "Fjord",
            subtitleEn = "Wide fjord, sea ice, snowy pines & longship",
            accentHex = Color(0xFF417FA3),
            secondaryHex = Color(0xFFE8F1F8),
            tertiaryHex = Color(0xFFB84438),
            mapX = 0.48f,
            mapY = 0.13f
        ),
        BiomeDef(
            id = "atolon",
            nameEs = "Atolón de Coral",
            shortEs = "Atolón",
            subtitleEs = "Laguna turquesa, palafitos y arrecife",
            nameEn = "Coral Atoll",
            shortEn = "Atoll",
            subtitleEn = "Turquoise lagoon, stilt huts & coral reef",
            accentHex = Color(0xFF4CC7C4),
            secondaryHex = Color(0xFFF4E5C8),
            tertiaryHex = Color(0xFFEE7A74),
            mapX = 0.46f,
            mapY = 0.83f
        ),
        BiomeDef(
            id = "toscana",
            nameEs = "Colinas de Toscana",
            shortEs = "Toscana",
            subtitleEs = "Cipreses, fuentes romanas y villa",
            nameEn = "Tuscan Hills",
            shortEn = "Tuscany",
            subtitleEn = "Cypresses, Roman fountains & sunlit villa",
            accentHex = Color(0xFF8DA45A),
            secondaryHex = Color(0xFFDFB66A),
            tertiaryHex = Color(0xFFC96D4B),
            mapX = 0.48f,
            mapY = 0.37f
        )
    )

    val STAGES: List<StageDef> = listOf(
        StageDef(1, "Nv. 1 · Terreno base", "Relieve suave inicial y cuenca de agua", "Lvl. 1 · Base Terrain", "Initial soft relief and water basin"),
        StageDef(2, "Nv. 2 · Primeros brotes", "Más relieve, rocas y primeros árboles", "Lvl. 2 · First Sprouts", "Richer terrain, boulders & first trees"),
        StageDef(3, "Nv. 3 · Flora abundante", "Arboledas densas y floración viva", "Lvl. 3 · Lush Flora", "Dense groves and blooming trees"),
        StageDef(4, "Nv. 4 · Caminos y puentes", "Senderos y puentes sobre el agua", "Lvl. 4 · Paths & Bridges", "Cobble trails and bridges over water"),
        StageDef(5, "Nv. 5 · Aldea 1:5.500", "Primeras construcciones en miniatura", "Lvl. 5 · Mini Village", "First miniature cottages appear"),
        StageDef(6, "Nv. 6 · Asentamiento vivo", "Más vida, barcas y farolillos cálidos", "Lvl. 6 · Living Settlement", "Boats on the water & warm lanterns"),
        StageDef(7, "Nv. 7 · Gran monumento", "Monumento principal 3D del bioma", "Lvl. 7 · Grand Landmark", "Hero 3D monument of the biome"),
        StageDef(8, "Nv. 8 · Bioma legendario", "Elevación máxima, brillos y detalles extra", "Lvl. 8 · Legendary Biome", "Maximum elevation, shimmers & details")
    )

    val DEFAULT_AREAS_ES = listOf("Cuerpo y energía", "Calma mental", "Conexión social", "Orden y entorno")
    val DEFAULT_AREAS_EN = listOf("Body & Energy", "Mental Calm", "Social Connection", "Order & Space")
}
