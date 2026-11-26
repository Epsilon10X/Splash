package com.splash.offline.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.splash.offline.data.AppLanguage
import com.splash.offline.data.AppTab
import com.splash.offline.data.SplashStrings
import com.splash.offline.data.SplashUiState
import com.splash.offline.ui.modals.FeedbackSheet
import com.splash.offline.ui.modals.FocusTimerOverlay
import com.splash.offline.ui.modals.SettingsSheet
import com.splash.offline.ui.modals.UpdateDialog
import com.splash.offline.ui.screens.Biome3DExplorerOverlay
import com.splash.offline.ui.screens.BiomesScreen
import com.splash.offline.ui.screens.CatalogScreen
import com.splash.offline.ui.screens.TodayScreen
import com.splash.offline.ui.screens.WallScreen
import com.splash.offline.ui.theme.GoalCardPalettes
import com.splash.offline.ui.theme.SplashTheme
import com.splash.offline.update.AppUpdateManager
import com.splash.offline.update.InstallLaunchResult
import com.splash.offline.update.ReleaseUpdateInfo
import com.splash.offline.update.UpdateCheckResult
import com.splash.offline.viewmodel.SplashViewModel
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashApp(
    state: SplashUiState,
    viewModel: SplashViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val forceOled = state.activeSession != null
    val hazeState = rememberHazeState()
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val installedVersion = remember { AppUpdateManager.getInstalledVersionName(context) }
    var availableUpdate by remember { mutableStateOf<ReleaseUpdateInfo?>(null) }
    var dismissedUpdateVersion by remember { mutableStateOf<String?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var needsInstallPermHint by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        delay(1500L)
        if (availableUpdate == null && !isCheckingUpdate) {
            when (val res = AppUpdateManager.checkForUpdate(installedVersion)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    if (dismissedUpdateVersion != res.info.versionName) {
                        downloadedApk = null
                        downloadProgress = 0f
                        needsInstallPermHint = false
                        updateError = null
                        availableUpdate = res.info
                    }
                }
                else -> Unit
            }
        }
    }

    fun checkUpdatesManually() {
        if (isCheckingUpdate) return
        scope.launch {
            isCheckingUpdate = true
            val res = AppUpdateManager.checkForUpdate(installedVersion)
            isCheckingUpdate = false
            when (res) {
                is UpdateCheckResult.UpdateAvailable -> {
                    downloadedApk = null
                    downloadProgress = 0f
                    needsInstallPermHint = false
                    updateError = null
                    availableUpdate = res.info
                }
                is UpdateCheckResult.UpToDate -> viewModel.showToast(
                    SplashStrings.t(state.lang, "Ya tienes la última versión (v${res.currentVersion})", "You are on the latest version (v${res.currentVersion})")
                )
                UpdateCheckResult.NoReleaseYet -> viewModel.showToast(
                    SplashStrings.t(state.lang, "No hay actualizaciones publicadas todavía", "No published releases yet")
                )
                is UpdateCheckResult.Error -> viewModel.showToast(
                    SplashStrings.t(state.lang, "No se pudo comprobar actualizaciones", "Could not check for updates")
                )
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCaptureUri
        pendingCaptureUri = null
        if (success && uri != null) {
            viewModel.saveCameraMemoryUri(uri)
        } else {
            viewModel.dismissCameraCapture()
        }
    }

    val pickGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.saveGalleryMemoryUri(uri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val uri = pendingCaptureUri
        if (granted && uri != null) {
            takePictureLauncher.launch(uri)
        } else {
            pendingCaptureUri = null
            viewModel.dismissCameraCapture()
            viewModel.showToast(SplashStrings.t(state.lang, "Activa el permiso de cámara para hacer la foto", "Enable camera permission to take a photo"))
        }
    }

    LaunchedEffect(state.pendingCameraGoal) {
        if (state.pendingCameraGoal != null && pendingCaptureUri == null) {
            try {
                val outFile = File(context.cacheDir, "splash_cap_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
                pendingCaptureUri = uri
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    takePictureLauncher.launch(uri)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            } catch (_: Throwable) {
                pendingCaptureUri = null
                viewModel.dismissCameraCapture()
            }
        }
    }

    LaunchedEffect(state.toastMessage) {
        if (state.toastMessage != null) {
            delay(2800L)
            viewModel.clearToast()
        }
    }

    SplashTheme(
        themeMode = state.themeMode,
        forceAmoledBlack = forceOled
    ) {
        val isDark = GoalCardPalettes.isSplashDarkTheme()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Screen Tabs marked as Haze blur source
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .statusBarsPadding()
            ) {
                AnimatedContent(
                    targetState = state.activeTab,
                    transitionSpec = {
                        fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                            fadeOut(spring(stiffness = Spring.StiffnessMedium))
                    },
                    label = "mainTab"
                ) { tab ->
                    when (tab) {
                        AppTab.HOY -> TodayScreen(
                            state = state,
                            onOpenSettings = { viewModel.setSettingsOpen(true) },
                            onOpen3D = { viewModel.toggle3DExplorer(true) },
                            onSwitchBiome = { viewModel.switchBiome(it) },
                            onSelectBucket = { viewModel.selectTimeBucket(it) },
                            onReroll = { viewModel.rerollSuggestions() },
                            onRequestExtraGoal = { viewModel.requestExtraGoal(it) },
                            onDismissExtraGoal = { viewModel.dismissExtraGoal(it) },
                            onStartGoal = { g, isBiome, isExtra -> viewModel.startGoal(g, isBiome, isExtra) },
                            onPhotoGoal = { g, isBiome, isExtra -> viewModel.openGoalPhotoCapture(g, isBiome, isExtra) },
                            onOpenCatalog = { viewModel.selectTab(AppTab.CATALOGO) },
                            onOpenBiomes = { viewModel.selectTab(AppTab.BIOMAS) }
                        )

                        AppTab.MURO -> WallScreen(
                            state = state,
                            onSelectSubTab = { viewModel.selectWallSubTab(it) },
                            onSelectMapStyle = { viewModel.selectMapStyle(it) },
                            onToggleMapAllColor = { viewModel.toggleMapShowAllColor() },
                            onMonthDelta = { viewModel.selectCalendarMonth(it) },
                            onSelectDay = { viewModel.selectDayIso(it) },
                            onOpenBiome3D = { idx -> viewModel.toggle3DExplorer(true, idx) },
                            onTakeMemoryPhoto = { viewModel.openStandalonePhotoCapture() },
                            onPickGalleryPhoto = { pickGalleryLauncher.launch("image/*") },
                            onExportPhotoToGallery = { path, title -> viewModel.exportMemoryPhotoToGallery(path, title) }
                        )

                        AppTab.CATALOGO -> CatalogScreen(
                            state = state,
                            onSearchChange = { viewModel.updateCatalogSearch(it) },
                            onFilterChange = { viewModel.updateCatalogFilter(it) },
                            onOpenCreate = { viewModel.setCreateGoalOpen(true) },
                            onDismissCreate = { viewModel.setCreateGoalOpen(false) },
                            onCreateGoal = { t, cat, area -> viewModel.createCustomGoal(t, cat, area) },
                            onToggleFavorite = { viewModel.toggleFavoriteGoal(it) },
                            onStartGoal = { g -> viewModel.startGoal(g, false, false) }
                        )

                        AppTab.BIOMAS -> BiomesScreen(
                            state = state,
                            onSelectLayout = { viewModel.selectBiomeLayout(it) },
                            onEquipBiome = { idx ->
                                viewModel.switchBiome(idx)
                                viewModel.selectTab(AppTab.HOY)
                            },
                            onOpen3D = { idx -> viewModel.toggle3DExplorer(true, idx) }
                        )
                    }
                }
            }

            // Official Material 3 Expressive 1.5.0 HorizontalFloatingToolbar + Biomes FAB with real Haze backdrop blur
            val showNavBar = !state.is3DOpen && !state.isSettingsOpen && !state.isFeedbackOpen && state.activeSession == null
            val glassTintBase = if (isDark) Color(0xFF14171E) else MaterialTheme.colorScheme.surfaceContainerHigh
            val toolbarHazeStyle = HazeStyle(
                backgroundColor = glassTintBase,
                tint = HazeTint(glassTintBase.copy(alpha = if (isDark) 0.54f else 0.64f)),
                blurRadius = 24.dp
            )
            val fabActive = state.activeTab == AppTab.BIOMAS
            val fabGlassBase = if (fabActive) MaterialTheme.colorScheme.primary else glassTintBase
            val fabHazeStyle = HazeStyle(
                backgroundColor = fabGlassBase,
                tint = HazeTint(fabGlassBase.copy(alpha = if (fabActive) 0.78f else if (isDark) 0.56f else 0.66f)),
                blurRadius = 24.dp
            )

            AnimatedVisibility(
                visible = showNavBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp, start = 16.dp, end = 16.dp)
                    .wrapContentSize()
            ) {
                val barBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.36f else 0.52f)
                val (hoyNavTargetBg, hoyNavTargetFg) = if (state.activeTab == AppTab.HOY) {
                    GoalCardPalettes.timeBucketTonalColors(state.timeBucket, isDark)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                }
                val hoyNavBg by animateColorAsState(targetValue = hoyNavTargetBg, label = "hoyNavBg")
                val hoyNavFg by animateColorAsState(targetValue = hoyNavTargetFg, label = "hoyNavFg")
                val (catalogNavTargetBg, catalogNavTargetFg) = if (state.activeTab == AppTab.CATALOGO) {
                    GoalCardPalettes.catalogFilterTonalColors(state.catalogFilter, isDark)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                }
                val catalogNavBg by animateColorAsState(targetValue = catalogNavTargetBg, label = "catalogNavBg")
                val catalogNavFg by animateColorAsState(targetValue = catalogNavTargetFg, label = "catalogNavFg")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.wrapContentSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(60.dp)
                            .clip(CircleShape)
                            .hazeEffect(state = hazeState, style = toolbarHazeStyle)
                            .border(width = 1.dp, color = barBorder, shape = CircleShape)
                    ) {
                        HorizontalFloatingToolbar(
                            expanded = true,
                            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                                toolbarContainerColor = Color.Transparent,
                                toolbarContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            expandedShadowElevation = 0.dp,
                            collapsedShadowElevation = 0.dp
                        ) {
                            ToolbarNavItem(
                                selected = state.activeTab == AppTab.HOY,
                                icon = Icons.Rounded.Home,
                                label = SplashStrings.t(state.lang, "Hoy", "Today"),
                                selectedContainerColor = hoyNavBg,
                                selectedContentColor = hoyNavFg,
                                onClick = { viewModel.selectTab(AppTab.HOY) }
                            )
                            ToolbarNavItem(
                                selected = state.activeTab == AppTab.MURO,
                                icon = Icons.Rounded.PhotoLibrary,
                                label = SplashStrings.t(state.lang, "Muro", "Wall"),
                                onClick = { viewModel.selectTab(AppTab.MURO) }
                            )
                            ToolbarNavItem(
                                selected = state.activeTab == AppTab.CATALOGO,
                                icon = Icons.Rounded.MenuBook,
                                label = SplashStrings.t(state.lang, "Catálogo", "Catalog"),
                                selectedContainerColor = catalogNavBg,
                                selectedContentColor = catalogNavFg,
                                onClick = { viewModel.selectTab(AppTab.CATALOGO) }
                            )
                        }
                    }

                    Surface(
                        onClick = { viewModel.selectTab(AppTab.BIOMAS) },
                        shape = CircleShape,
                        color = Color.Transparent,
                        contentColor = if (fabActive) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .hazeEffect(state = hazeState, style = fabHazeStyle)
                            .border(width = 1.dp, color = barBorder, shape = CircleShape)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Terrain,
                                contentDescription = SplashStrings.t(state.lang, "Biomas", "Biomes")
                            )
                        }
                    }
                }
            }

            // Fullscreen Overlays
            AnimatedVisibility(
                visible = state.is3DOpen,
                enter = fadeIn(tween(320, easing = FastOutSlowInEasing)) + scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = fadeOut(tween(260, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.92f)
            ) {
                Biome3DExplorerOverlay(
                    state = state,
                    onClose = { viewModel.toggle3DExplorer(false) },
                    onSelectBiome = { viewModel.switchBiome(it) },
                    onSelectBetaStage = { viewModel.setBetaStageOverride(it) },
                    onGrantBetaXp = { viewModel.grantBetaXp(it) },
                    onToggleNight = { viewModel.toggleDioramaNightPreview() },
                    onToggleAudio = { viewModel.toggleAmbientAudio() }
                )
            }

            AnimatedVisibility(
                visible = state.isSettingsOpen,
                enter = slideInVertically { it / 3 } + fadeIn(),
                exit = slideOutVertically { it / 3 } + fadeOut()
            ) {
                SettingsSheet(
                    state = state,
                    onClose = { viewModel.setSettingsOpen(false) },
                    onOpenFeedback = { viewModel.setFeedbackOpen(true) },
                    isCheckingUpdate = isCheckingUpdate,
                    onCheckForUpdates = { checkUpdatesManually() },
                    onSaveName = { viewModel.setUserName(it) },
                    onSwitchLanguage = { viewModel.switchLanguage(it) },
                    onSelectTheme = { viewModel.setThemeMode(it) },
                    onSelectWidgetTheme = { viewModel.setWidgetThemePref(it) },
                    onToggleDayHour = { d, h -> viewModel.toggleDayHour(d, h) },
                    onApplyPreset = { viewModel.applySchedulePreset(it) },
                    onToggleBeta = { viewModel.setBetaMode(it) },
                    onCompleteBiomeBeta = { viewModel.completeActiveBiomeBeta() },
                    onResetProgress = { viewModel.resetAllProgress() },
                    onShowToast = { viewModel.showToast(it) }
                )
            }

            AnimatedVisibility(
                visible = state.isFeedbackOpen,
                enter = slideInVertically { it / 3 } + fadeIn(),
                exit = slideOutVertically { it / 3 } + fadeOut()
            ) {
                FeedbackSheet(
                    state = state,
                    onClose = { viewModel.setFeedbackOpen(false) },
                    onShowToast = { viewModel.showToast(it) }
                )
            }

            AnimatedVisibility(
                visible = state.activeSession != null,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.95f),
                exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) + scaleOut(targetScale = 0.95f)
            ) {
                state.activeSession?.let { session ->
                    FocusTimerOverlay(
                        session = session,
                        lang = state.lang,
                        isAudioPlaying = state.isAmbientAudioPlaying,
                        onToggleRunning = { viewModel.toggleActiveTimerRunning() },
                        onResetTimer = { viewModel.resetActiveTimer() },
                        onTick = { viewModel.tickActiveTimer() },
                        onToggleAudio = { viewModel.toggleAmbientAudio() },
                        onComplete = { viewModel.completeActiveSession() },
                        onClose = { viewModel.closeActiveSession() }
                    )
                }
            }

            // Direct camera launch is handled via LaunchedEffect(state.pendingCameraGoal)

            // Official Material 3 Expressive 1.5.0 LoadingIndicator overlay during Language Switch
            AnimatedVisibility(
                visible = state.isLanguageSwitching,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.88f))
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)
                        ) {
                            LoadingIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = if (state.lang == AppLanguage.EN) "English" else "Español",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            availableUpdate?.let { updateInfo ->
                UpdateDialog(
                    lang = state.lang,
                    currentVersion = installedVersion,
                    updateInfo = updateInfo,
                    isDownloading = isDownloadingUpdate,
                    downloadProgress = downloadProgress,
                    downloadedFile = downloadedApk,
                    needsPermissionHint = needsInstallPermHint,
                    errorMessage = updateError,
                    onStartDownload = {
                        if (!isDownloadingUpdate) {
                            scope.launch {
                                isDownloadingUpdate = true
                                downloadProgress = 0f
                                needsInstallPermHint = false
                                updateError = null
                                val result = AppUpdateManager.downloadApk(context, updateInfo) { p ->
                                    downloadProgress = p
                                }
                                isDownloadingUpdate = false
                                result.onSuccess { file ->
                                    downloadedApk = file
                                    when (AppUpdateManager.installApk(context, file)) {
                                        InstallLaunchResult.NEEDS_UNKNOWN_SOURCES_PERMISSION -> {
                                            needsInstallPermHint = true
                                        }
                                        InstallLaunchResult.FAILED -> {
                                            updateError = SplashStrings.t(
                                                state.lang,
                                                "No se pudo abrir el instalador de Android.",
                                                "Could not launch Android package installer."
                                            )
                                        }
                                        InstallLaunchResult.LAUNCHED -> Unit
                                    }
                                }.onFailure {
                                    updateError = SplashStrings.t(
                                        state.lang,
                                        "Error al descargar la actualización. Inténtalo de nuevo.",
                                        "Download failed. Please try again."
                                    )
                                }
                            }
                        }
                    },
                    onInstallDownloaded = {
                        val file = downloadedApk ?: return@UpdateDialog
                        updateError = null
                        needsInstallPermHint = false
                        when (AppUpdateManager.installApk(context, file)) {
                            InstallLaunchResult.NEEDS_UNKNOWN_SOURCES_PERMISSION -> {
                                needsInstallPermHint = true
                            }
                            InstallLaunchResult.FAILED -> {
                                updateError = SplashStrings.t(
                                    state.lang,
                                    "No se pudo abrir el instalador de Android.",
                                    "Could not launch Android package installer."
                                )
                            }
                            InstallLaunchResult.LAUNCHED -> Unit
                        }
                    },
                    onDismiss = {
                        dismissedUpdateVersion = updateInfo.versionName
                        availableUpdate = null
                    }
                )
            }

            // Toast / Snackbar
            AnimatedVisibility(
                visible = state.toastMessage != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 60.dp, start = 16.dp, end = 16.dp)
            ) {
                state.toastMessage?.let { msg ->
                    Snackbar(
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    ) {
                        Text(msg, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarNavItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) selectedContainerColor else Color.Transparent,
        modifier = Modifier.height(42.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
