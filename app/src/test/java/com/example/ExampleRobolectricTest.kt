package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.engine.SimulationEngine
import com.example.game.engine.TrafficEngine
import com.example.game.engine.WorldMap
import com.example.game.model.*
import com.example.game.save.CitySaveManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Urban Life", appName)
    }

    @Test
    fun `test world map and terrain generation`() {
        val world = WorldMap(36, 36)
        assertEquals(36, world.width)
        assertEquals(36, world.height)
        assertNotNull(world.getTile(18, 18))
        assertNull(world.getTile(-1, 50))

        val tile = world.getTile(10, 10)!!
        assertTrue(tile.terrain in TerrainType.values())
    }

    @Test
    fun `test zoning and building growth simulation`() {
        val world = WorldMap(36, 36)
        val sim = SimulationEngine(world)

        // Setup road, power, water
        val rx = 15; val ry = 15
        world.tiles[rx][ry].road = RoadType.SMALL_2L
        world.tiles[rx][ry].terrain = TerrainType.PLAINS

        val zx = 15; val zy = 16
        world.tiles[zx][zy].terrain = TerrainType.PLAINS
        world.tiles[zx][zy].zone = ZoneType.RESIDENTIAL_HIGH

        // Place power plant and water pump
        world.tiles[14][15].terrain = TerrainType.PLAINS
        world.tiles[14][15].utility = UtilityType.WIND_TURBINE

        world.tiles[14][16].terrain = TerrainType.SHORE
        world.tiles[14][16].utility = UtilityType.WATER_PUMP

        // Run multiple ticks
        for (i in 0..120) {
            sim.tick()
        }

        assertTrue(sim.stats.powerCapacityMW > 0)
        assertTrue(sim.stats.waterCapacityMG > 0)
    }

    @Test
    fun `test save and load city`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val saveManager = CitySaveManager(context)

        val world = WorldMap(36, 36)
        val sim = SimulationEngine(world)

        world.tiles[5][5].road = RoadType.MEDIUM_4L
        world.tiles[5][6].zone = ZoneType.COMMERCIAL_HIGH
        sim.stats.treasury = 88000L
        sim.stats.population = 150

        val saved = saveManager.saveCity("UnitTestSlot", world, sim)
        assertTrue(saved)

        // Create new world and load
        val loadedWorld = WorldMap(36, 36)
        val loadedSim = SimulationEngine(loadedWorld)
        val loaded = saveManager.loadCity("UnitTestSlot", loadedWorld, loadedSim)

        assertTrue(loaded)
        assertEquals(RoadType.MEDIUM_4L, loadedWorld.tiles[5][5].road)
        assertEquals(ZoneType.COMMERCIAL_HIGH, loadedWorld.tiles[5][6].zone)
        assertEquals(88000L, loadedSim.stats.treasury)
    }
}

