package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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

    var activeCategory by remember { mutableStateOf<String?>(null) }
    var hoverTile by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

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
        // 1. 3D Game Canvas with Multi-Touch Controls
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    viewModel.camera.viewportWidth = size.width.toFloat()
                    viewModel.camera.viewportHeight = size.height.toFloat()
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        if (zoom != 1.0f) {
                            viewModel.camera.zoomBy(zoom)
                        }
                        if (rotation != 0f) {
                            viewModel.camera.rotateBy(rotation * 0.8f, 0f)
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
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. Active Disaster Alert Banner
        uiState.activeDisaster?.let { disaster ->
            DisasterAlert(
                disaster = disaster,
                onLocate = { viewModel.goToDisaster(it) },
                onDismiss = { viewModel.dismissDisaster() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            )
        }

        // 4. Bottom Controls (Flyouts, Inspector, Bottom Bar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Inspector Sheet if a tile is selected
            uiState.selectedTile?.let { tile ->
                InspectorSheet(
                    tile = tile,
                    onDemolish = { viewModel.demolishTile(it) },
                    onDismiss = { viewModel.dismissInspector() }
                )
            }

            // Flyout Submenu for currently selected category
            ToolFlyoutMenu(
                activeCategory = activeCategory,
                activeTool = uiState.activeTool,
                activeOverlay = uiState.overlayMode,
                onSelectTool = { tool ->
                    viewModel.selectTool(tool)
                    activeCategory = null
                },
                onSelectOverlay = { overlay ->
                    viewModel.setOverlay(overlay)
                    activeCategory = null
                },
                onClose = { activeCategory = null }
            )

            // Primary Bottom Bar
            GameBottomBar(
                activeCategory = activeCategory,
                activeTool = uiState.activeTool,
                onCategoryClick = { cat ->
                    activeCategory = if (activeCategory == cat) null else cat
                },
                onEconomyClick = { viewModel.toggleEconomyDialog(true) },
                onStatsClick = { viewModel.toggleStatsDialog(true) },
                onSettingsClick = { viewModel.toggleSettingsDialog(true) },
                onDemolishClick = {
                    viewModel.selectTool(ActiveTool(mode = ToolMode.DEMOLISH))
                    activeCategory = null
                }
            )
        }

        // 5. Snackbar for quick toasts
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        )

        // 6. Dialogs
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
    }
}
