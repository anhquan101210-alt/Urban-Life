package com.example.game.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.model.*
import com.example.game.renderer.Game3DRenderer
import com.example.game.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val renderer = remember { Game3DRenderer(viewModel.camera) }
    val context = LocalContext.current

    var activeCategory by remember { mutableStateOf<String?>(null) }
    var hoverTile by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Android Back button handler
    BackHandler {
        if (activeCategory != null) {
            activeCategory = null
        } else {
            viewModel.handleBackPress()
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_screen_container")
    ) {
        // 1. Isometric City Canvas with Pan, Pinch-Zoom, and Tap-Selection
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    viewModel.camera.viewportWidth = size.width.toFloat()
                    viewModel.camera.viewportHeight = size.height.toFloat()
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1.0f) {
                            viewModel.camera.zoomBy(zoom)
                        }
                        if (pan != Offset.Zero) {
                            viewModel.camera.pan(pan.x, pan.y)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val groundCoords = viewModel.camera.screenToGround(offset.x, offset.y)
                            if (groundCoords != null) {
                                hoverTile = groundCoords
                                viewModel.onTileTapped(groundCoords.first, groundCoords.second)
                            }
                        }
                    )
                }
        ) {
            renderer.render(
                drawScope = this,
                world = viewModel.world,
                traffic = viewModel.traffic,
                stats = viewModel.sim.stats,
                overlay = uiState.overlayMode,
                graphicsQuality = uiState.graphicsQuality,
                hoverTile = hoverTile,
                activeTool = uiState.activeTool
            )
        }

        // 2. Top HUD Bar
        GameTopBar(
            stats = viewModel.sim.stats,
            camera = viewModel.camera,
            fps = uiState.fps,
            showFps = uiState.showFpsCounter,
            onSpeedChanged = { viewModel.setSimSpeed(it) },
            onOpenDemand = { viewModel.toggleDemandDialog(true) },
            onOpenCityInfo = { viewModel.toggleCityOverviewDialog(true) },
            onOpenSettings = { viewModel.toggleSettingsDialog(true) },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. Active Tool Floating Banner (When a build/zone/demolish tool is selected)
        if (uiState.activeTool.mode != ToolMode.INSPECT) {
            ActiveToolBanner(
                activeTool = uiState.activeTool,
                onCancel = { viewModel.selectTool(ActiveTool(mode = ToolMode.INSPECT)) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 44.dp)
            )
        }

        // 4. Disaster Emergency Alert Banner
        uiState.activeDisaster?.let { disaster ->
            DisasterAlert(
                disaster = disaster,
                onLocate = { viewModel.goToDisaster(it) },
                onDismiss = { viewModel.dismissDisaster() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (uiState.activeTool.mode != ToolMode.INSPECT) 80.dp else 46.dp)
            )
        }

        // 5. Functional Minimap Widget (Bottom-Right, above navigation dock)
        MinimapWidget(
            world = viewModel.world,
            camera = viewModel.camera,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 48.dp)
        )

        // 6. Bottom Controls: Inspector Sheet, Flyout Menu, and Dock
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Inspector Sheet for Selected Tile / Building
            uiState.selectedTile?.let { tile ->
                InspectorSheet(
                    tile = tile,
                    onUpgrade = { viewModel.upgradeTile(it) },
                    onDemolish = { viewModel.demolishTile(it) },
                    onDismiss = { viewModel.dismissInspector() }
                )
            }

            // Flyout Submenu for active Category
            ToolFlyoutMenu(
                activeCategory = activeCategory,
                activeTool = uiState.activeTool,
                activeOverlay = uiState.overlayMode,
                stats = viewModel.sim.stats,
                onSelectTool = { tool ->
                    viewModel.selectTool(tool)
                    activeCategory = null
                },
                onSelectOverlay = { overlay ->
                    viewModel.setOverlay(overlay)
                    activeCategory = null
                },
                onOpenCityOverview = {
                    viewModel.toggleCityOverviewDialog(true)
                    activeCategory = null
                },
                onOpenEconomy = {
                    viewModel.toggleEconomyDialog(true)
                    activeCategory = null
                },
                onOpenStats = {
                    viewModel.toggleStatsDialog(true)
                    activeCategory = null
                },
                onOpenSettings = {
                    viewModel.toggleSettingsDialog(true)
                    activeCategory = null
                },
                onClose = { activeCategory = null }
            )

            // Primary Bottom Dock (Zones, Roads, Services, Utilities, Transport, More)
            GameBottomBar(
                activeCategory = activeCategory,
                activeTool = uiState.activeTool,
                onCategoryClick = { cat ->
                    activeCategory = if (activeCategory == cat) null else cat
                }
            )
        }

        // 7. In-game Feedback SnackBar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        )

        // 8. Dialog Modals
        if (uiState.isDemandDialogOpen) {
            CityDemandDialog(
                stats = viewModel.sim.stats,
                onDismiss = { viewModel.toggleDemandDialog(false) }
            )
        }

        if (uiState.isCityOverviewDialogOpen) {
            CityOverviewDialog(
                stats = viewModel.sim.stats,
                onDismiss = { viewModel.toggleCityOverviewDialog(false) }
            )
        }

        if (uiState.isEconomyDialogOpen) {
            EconomyDialog(
                stats = viewModel.sim.stats,
                onTaxRatesChanged = { r, c, i -> viewModel.setTaxRates(r, c, i) },
                onDismiss = { viewModel.toggleEconomyDialog(false) }
            )
        }

        if (uiState.isStatsDialogOpen) {
            StatisticsDialog(
                stats = viewModel.sim.stats,
                history = viewModel.sim.history,
                onDismiss = { viewModel.toggleStatsDialog(false) }
            )
        }

        if (uiState.isSettingsDialogOpen) {
            SettingsDialog(
                graphicsQuality = uiState.graphicsQuality,
                showFps = uiState.showFpsCounter,
                soundManager = viewModel.soundManager,
                onGraphicsChanged = { viewModel.setGraphicsQuality(it) },
                onToggleFps = { viewModel.toggleFpsCounter() },
                onSaveCity = { viewModel.saveCity("City 1") },
                onLoadCity = { viewModel.loadCity("City 1") },
                onResetCity = { viewModel.resetCity() },
                onDismiss = { viewModel.toggleSettingsDialog(false) }
            )
        }

        if (uiState.isExitConfirmDialogOpen) {
            ExitConfirmDialog(
                onConfirmExit = {
                    (context as? Activity)?.finish()
                },
                onDismiss = { viewModel.toggleExitConfirmDialog(false) }
            )
        }
    }
}
