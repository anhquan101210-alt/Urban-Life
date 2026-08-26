package com.example.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.SoundManager
import com.example.game.engine.SimulationEngine
import com.example.game.engine.TrafficEngine
import com.example.game.engine.WorldMap
import com.example.game.model.*
import com.example.game.renderer.Camera3D
import com.example.game.save.CitySaveManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class GameUiState(
    val cityName: String = "Urban Life",
    val activeTool: ActiveTool = ActiveTool(mode = ToolMode.INSPECT),
    val selectedTile: GridTile? = null,
    val overlayMode: OverlayMode = OverlayMode.NORMAL,
    val isDemandDialogOpen: Boolean = false,
    val isCityOverviewDialogOpen: Boolean = false,
    val isEconomyDialogOpen: Boolean = false,
    val isStatsDialogOpen: Boolean = false,
    val isSettingsDialogOpen: Boolean = false,
    val isExitConfirmDialogOpen: Boolean = false,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val showFpsCounter: Boolean = true,
    val fps: Int = 60,
    val activeDisaster: Disaster? = null,
    val toastMessage: String? = null
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val world = WorldMap(36, 36)
    val camera = Camera3D(targetX = 18f, targetY = 18f)
    val soundManager = SoundManager(application)
    val saveManager = CitySaveManager(application)

    val sim = SimulationEngine(world)
    val traffic = TrafficEngine(world, sim.stats)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()

    init {
        startGameLoop()
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            while (isActive) {
                val startTime = System.currentTimeMillis()

                // Simulation tick
                sim.tick()
                traffic.update()

                // Active disaster banner
                val latestDisaster = sim.activeDisasters.firstOrNull()
                if (latestDisaster != null && _uiState.value.activeDisaster == null) {
                    soundManager.playSirenAlert()
                }

                // FPS calculation
                frameCount++
                val now = System.currentTimeMillis()
                if (now - lastFpsTimestamp >= 1000) {
                    val calculatedFps = (frameCount * 1000f / (now - lastFpsTimestamp)).toInt()
                    _uiState.value = _uiState.value.copy(
                        fps = calculatedFps,
                        activeDisaster = latestDisaster
                    )
                    frameCount = 0
                    lastFpsTimestamp = now
                }

                val elapsed = System.currentTimeMillis() - startTime
                val sleepTime = maxOf(5L, 33L - elapsed)
                delay(sleepTime)
            }
        }
    }

    fun selectTool(tool: ActiveTool) {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(
            activeTool = tool,
            selectedTile = null
        )
    }

    fun onTileTapped(x: Int, y: Int) {
        val tile = world.getTile(x, y) ?: return
        val currentTool = _uiState.value.activeTool

        when (currentTool.mode) {
            ToolMode.INSPECT -> {
                soundManager.playClick()
                _uiState.value = _uiState.value.copy(selectedTile = tile)
            }
            ToolMode.ROAD -> {
                placeRoad(tile, currentTool.roadType)
            }
            ToolMode.ZONE -> {
                placeZone(tile, currentTool.zoneType)
            }
            ToolMode.SERVICE -> {
                currentTool.serviceType?.let { placeService(tile, it) }
            }
            ToolMode.UTILITY -> {
                currentTool.utilityType?.let { placeUtility(tile, it) }
            }
            ToolMode.TRANSPORT -> {
                currentTool.transportType?.let { placeTransport(tile, it) }
            }
            ToolMode.DEMOLISH -> {
                demolishTile(tile)
            }
        }
    }

    private fun placeRoad(tile: GridTile, roadType: RoadType) {
        if (roadType == RoadType.NONE) return
        if (tile.terrain == TerrainType.WATER && roadType != RoadType.BRIDGE) {
            showToast("Cannot build road on water! Use Bridge.")
            return
        }
        if (tile.terrain != TerrainType.WATER && roadType == RoadType.BRIDGE) {
            showToast("Bridges must be built over water!")
            return
        }
        if (sim.stats.treasury < roadType.cost) {
            showToast("Not enough funds to build road!")
            return
        }

        sim.stats.treasury -= roadType.cost
        tile.road = roadType
        tile.building = null
        tile.zone = ZoneType.NONE
        tile.treeType = 0
        tile.hasPowerLine = true
        tile.hasWaterPipe = true
        soundManager.playBuildSound()
    }

    private fun placeZone(tile: GridTile, zoneType: ZoneType) {
        if (tile.terrain == TerrainType.WATER) {
            showToast("Cannot zone on water!")
            return
        }
        if (tile.road != RoadType.NONE) {
            showToast("Cannot zone over road!")
            return
        }
        if (sim.stats.treasury < zoneType.cost) {
            showToast("Not enough funds for zoning!")
            return
        }

        sim.stats.treasury -= zoneType.cost
        tile.zone = zoneType
        tile.treeType = 0
        soundManager.playBuildSound()
    }

    private fun placeService(tile: GridTile, serviceType: ServiceType) {
        if (tile.terrain == TerrainType.WATER || tile.road != RoadType.NONE) {
            showToast("Cannot place building here!")
            return
        }
        if (sim.stats.treasury < serviceType.cost) {
            showToast("Not enough funds for ${serviceType.displayName}!")
            return
        }

        sim.stats.treasury -= serviceType.cost
        tile.service = serviceType
        tile.building = null
        tile.zone = ZoneType.NONE
        tile.treeType = 0
        soundManager.playBuildSound()
    }

    private fun placeUtility(tile: GridTile, utilityType: UtilityType) {
        if (tile.road != RoadType.NONE) {
            showToast("Cannot place utility on road!")
            return
        }
        if (utilityType.requiresWaterfront && tile.terrain != TerrainType.SHORE && tile.terrain != TerrainType.WATER) {
            showToast("Water Pump must be placed near waterfront!")
            return
        }
        if (!utilityType.requiresWaterfront && tile.terrain == TerrainType.WATER) {
            showToast("Cannot place on water!")
            return
        }
        if (sim.stats.treasury < utilityType.cost) {
            showToast("Not enough funds for ${utilityType.displayName}!")
            return
        }

        sim.stats.treasury -= utilityType.cost
        tile.utility = utilityType
        tile.building = null
        tile.zone = ZoneType.NONE
        tile.treeType = 0
        soundManager.playBuildSound()
    }

    private fun placeTransport(tile: GridTile, transportType: TransportType) {
        if (tile.terrain == TerrainType.WATER) return
        if (sim.stats.treasury < transportType.cost) {
            showToast("Not enough funds for ${transportType.displayName}!")
            return
        }

        sim.stats.treasury -= transportType.cost
        tile.transport = transportType
        tile.treeType = 0
        soundManager.playBuildSound()
    }

    fun demolishTile(tile: GridTile) {
        val cost = 10L
        if (sim.stats.treasury < cost) {
            showToast("Not enough money to demolish!")
            return
        }
        sim.stats.treasury -= cost
        world.clearTile(tile.x, tile.y)
        soundManager.playDemolishSound()
        if (_uiState.value.selectedTile == tile) {
            _uiState.value = _uiState.value.copy(selectedTile = null)
        }
    }

    fun setSimSpeed(speed: Int) {
        soundManager.playClick()
        sim.stats.simSpeed = speed
    }

    fun setOverlay(overlay: OverlayMode) {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(overlayMode = overlay)
    }

    fun setTaxRates(res: Int, com: Int, ind: Int) {
        sim.stats.residentialTaxRate = res
        sim.stats.commercialTaxRate = com
        sim.stats.industrialTaxRate = ind
    }

    fun toggleDemandDialog(open: Boolean) {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(isDemandDialogOpen = open)
    }

    fun toggleCityOverviewDialog(open: Boolean) {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(isCityOverviewDialogOpen = open)
    }

    fun toggleEconomyDialog(open: Boolean) {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(isEconomyDialogOpen = open)
    }

    fun toggleStatsDialog(open: Boolean) {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(isStatsDialogOpen = open)
    }

    fun toggleSettingsDialog(open: Boolean) {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(isSettingsDialogOpen = open)
    }

    fun toggleExitConfirmDialog(open: Boolean) {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(isExitConfirmDialogOpen = open)
    }

    fun resetTool() {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(activeTool = ActiveTool(mode = ToolMode.INSPECT))
    }

    fun upgradeTile(tile: GridTile) {
        val b = tile.building
        if (b != null && b.level < 3) {
            val upgradeCost = b.level * 150L
            if (sim.stats.treasury < upgradeCost) {
                showToast("Not enough money for upgrade ($$upgradeCost required)")
                return
            }
            sim.stats.treasury -= upgradeCost
            b.level += 1
            b.population = (b.population * 1.5f).toInt()
            b.jobs = (b.jobs * 1.5f).toInt()
            b.landValue += 15
            soundManager.playCashChime()
            showToast("Upgraded ${b.buildingName} to Level ${b.level}!")
            // Trigger refresh of selection
            _uiState.value = _uiState.value.copy(selectedTile = tile)
        } else {
            showToast("Building is already at max level!")
        }
    }

    fun handleBackPress(): Boolean {
        val s = _uiState.value
        return when {
            s.isDemandDialogOpen -> { toggleDemandDialog(false); true }
            s.isCityOverviewDialogOpen -> { toggleCityOverviewDialog(false); true }
            s.isEconomyDialogOpen -> { toggleEconomyDialog(false); true }
            s.isStatsDialogOpen -> { toggleStatsDialog(false); true }
            s.isSettingsDialogOpen -> { toggleSettingsDialog(false); true }
            s.isExitConfirmDialogOpen -> { toggleExitConfirmDialog(false); true }
            s.selectedTile != null -> { dismissInspector(); true }
            s.activeTool.mode != ToolMode.INSPECT -> { resetTool(); true }
            s.overlayMode != OverlayMode.NORMAL -> { setOverlay(OverlayMode.NORMAL); true }
            else -> {
                toggleExitConfirmDialog(true)
                true
            }
        }
    }

    fun setGraphicsQuality(quality: GraphicsQuality) {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(graphicsQuality = quality)
    }

    fun toggleFpsCounter() {
        soundManager.playClick()
        _uiState.value = _uiState.value.copy(showFpsCounter = !_uiState.value.showFpsCounter)
    }

    fun dismissInspector() {
        _uiState.value = _uiState.value.copy(selectedTile = null)
    }

    fun saveCity(slotName: String = "City 1") {
        val success = saveManager.saveCity(slotName, world, sim)
        if (success) {
            soundManager.playCashChime()
            showToast("City saved successfully as '$slotName'")
        } else {
            showToast("Failed to save city.")
        }
    }

    fun loadCity(slotName: String = "City 1") {
        val success = saveManager.loadCity(slotName, world, sim)
        if (success) {
            soundManager.playCashChime()
            showToast("City '$slotName' loaded!")
        } else {
            showToast("Save file not found.")
        }
    }

    fun resetCity() {
        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                world.clearTile(x, y)
            }
        }
        sim.stats.population = 0
        sim.stats.treasury = 65000L
        sim.stats.happiness = 85
        sim.stats.dayCount = 1
        sim.stats.dayTime = 9.0f
        showToast("New city started!")
    }

    fun goToDisaster(disaster: Disaster) {
        camera.centerOn(disaster.x.toFloat(), disaster.y.toFloat())
        _uiState.value = _uiState.value.copy(activeDisaster = null)
    }

    fun dismissDisaster() {
        _uiState.value = _uiState.value.copy(activeDisaster = null)
    }

    private fun showToast(msg: String) {
        _uiState.value = _uiState.value.copy(toastMessage = msg)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
