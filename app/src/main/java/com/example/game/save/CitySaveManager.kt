package com.example.game.save

import android.content.Context
import com.example.game.engine.SimulationEngine
import com.example.game.engine.WorldMap
import com.example.game.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CitySaveManager(private val context: Context) {

    fun listSaveSlots(): List<String> {
        val saveDir = File(context.filesDir, "cities")
        if (!saveDir.exists()) saveDir.mkdirs()
        val files = saveDir.listFiles { _, name -> name.endsWith(".json") }
        return files?.map { it.nameWithoutExtension } ?: listOf("Slot 1")
    }

    fun saveCity(slotName: String, world: WorldMap, sim: SimulationEngine): Boolean {
        return try {
            val saveDir = File(context.filesDir, "cities")
            if (!saveDir.exists()) saveDir.mkdirs()
            val file = File(saveDir, "$slotName.json")

            val root = JSONObject()
            root.put("cityName", "Urban Life")
            root.put("timestamp", System.currentTimeMillis())

            // Stats
            val statsObj = JSONObject().apply {
                put("population", sim.stats.population)
                put("treasury", sim.stats.treasury)
                put("happiness", sim.stats.happiness)
                put("dayCount", sim.stats.dayCount)
                put("dayTime", sim.stats.dayTime.toDouble())
                put("resTax", sim.stats.residentialTaxRate)
                put("comTax", sim.stats.commercialTaxRate)
                put("indTax", sim.stats.industrialTaxRate)
            }
            root.put("stats", statsObj)

            // Tiles
            val tilesArray = JSONArray()
            for (x in 0 until world.width) {
                for (y in 0 until world.height) {
                    val t = world.tiles[x][y]
                    if (t.zone != ZoneType.NONE || t.road != RoadType.NONE || t.service != null || t.utility != null || t.transport != null || t.building != null) {
                        val tObj = JSONObject().apply {
                            put("x", x)
                            put("y", y)
                            put("zone", t.zone.name)
                            put("road", t.road.name)
                            put("service", t.service?.name ?: "")
                            put("utility", t.utility?.name ?: "")
                            put("transport", t.transport?.name ?: "")
                            t.building?.let { b ->
                                val bObj = JSONObject().apply {
                                    put("id", b.id)
                                    put("zoneType", b.zoneType.name)
                                    put("stage", b.stage.name)
                                    put("level", b.level)
                                    put("pop", b.population)
                                    put("jobs", b.jobs)
                                    put("name", b.buildingName)
                                    put("hap", b.happinessScore)
                                }
                                put("building", bObj)
                            }
                        }
                        tilesArray.put(tObj)
                    }
                }
            }
            root.put("tiles", tilesArray)

            file.writeText(root.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadCity(slotName: String, world: WorldMap, sim: SimulationEngine): Boolean {
        return try {
            val file = File(File(context.filesDir, "cities"), "$slotName.json")
            if (!file.exists()) return false

            val jsonStr = file.readText()
            val root = JSONObject(jsonStr)

            // Reset world first
            for (x in 0 until world.width) {
                for (y in 0 until world.height) {
                    world.clearTile(x, y)
                }
            }

            // Load Stats
            val statsObj = root.optJSONObject("stats")
            if (statsObj != null) {
                sim.stats.population = statsObj.optInt("population", 0)
                sim.stats.treasury = statsObj.optLong("treasury", 50000L)
                sim.stats.happiness = statsObj.optInt("happiness", 80)
                sim.stats.dayCount = statsObj.optInt("dayCount", 1)
                sim.stats.dayTime = statsObj.optDouble("dayTime", 10.0).toFloat()
                sim.stats.residentialTaxRate = statsObj.optInt("resTax", 10)
                sim.stats.commercialTaxRate = statsObj.optInt("comTax", 10)
                sim.stats.industrialTaxRate = statsObj.optInt("indTax", 10)
            }

            // Load Tiles
            val tilesArray = root.optJSONArray("tiles")
            if (tilesArray != null) {
                for (i in 0 until tilesArray.length()) {
                    val tObj = tilesArray.getJSONObject(i)
                    val x = tObj.getInt("x")
                    val y = tObj.getInt("y")
                    if (world.isInside(x, y)) {
                        val tile = world.tiles[x][y]
                        val zStr = tObj.optString("zone", "NONE")
                        tile.zone = try { ZoneType.valueOf(zStr) } catch (_: Exception) { ZoneType.NONE }

                        val rStr = tObj.optString("road", "NONE")
                        tile.road = try { RoadType.valueOf(rStr) } catch (_: Exception) { RoadType.NONE }

                        val sStr = tObj.optString("service", "")
                        tile.service = if (sStr.isNotEmpty()) try { ServiceType.valueOf(sStr) } catch (_: Exception) { null } else null

                        val uStr = tObj.optString("utility", "")
                        tile.utility = if (uStr.isNotEmpty()) try { UtilityType.valueOf(uStr) } catch (_: Exception) { null } else null

                        val trStr = tObj.optString("transport", "")
                        tile.transport = if (trStr.isNotEmpty()) try { TransportType.valueOf(trStr) } catch (_: Exception) { null } else null

                        val bObj = tObj.optJSONObject("building")
                        if (bObj != null) {
                            val bZone = try { ZoneType.valueOf(bObj.getString("zoneType")) } catch (_: Exception) { ZoneType.RESIDENTIAL_LOW }
                            val bStage = try { BuildingStage.valueOf(bObj.getString("stage")) } catch (_: Exception) { BuildingStage.BUILT }
                            val b = Building(
                                id = bObj.optString("id", java.util.UUID.randomUUID().toString()),
                                gridX = x,
                                gridY = y,
                                zoneType = bZone,
                                stage = bStage,
                                level = bObj.optInt("level", 1),
                                population = bObj.optInt("pop", 0),
                                jobs = bObj.optInt("jobs", 0),
                                buildingName = bObj.optString("name", "Building"),
                                happinessScore = bObj.optInt("hap", 75)
                            )
                            tile.building = b
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteCity(slotName: String): Boolean {
        val file = File(File(context.filesDir, "cities"), "$slotName.json")
        return if (file.exists()) file.delete() else false
    }
}
