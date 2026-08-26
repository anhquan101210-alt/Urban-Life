package com.example.game.engine

import com.example.game.model.*
import java.util.LinkedList
import java.util.Queue
import java.util.UUID
import kotlin.math.*
import kotlin.random.Random

class TrafficEngine(
    private val world: WorldMap,
    private val stats: CityStats
) {
    val vehicles = mutableListOf<Vehicle>()
    private val random = Random(System.currentTimeMillis())
    private var spawnTimer = 0

    fun update() {
        if (stats.simSpeed == 0) return

        val speedMult = stats.simSpeed.toFloat()
        spawnTimer++

        // Spawn vehicles based on city population & traffic demand
        val maxVehicles = (stats.population / 15).coerceIn(4, 55)
        if (spawnTimer >= 15 && vehicles.size < maxVehicles) {
            spawnTimer = 0
            spawnRandomVehicle()
        }

        // Move existing vehicles
        val iterator = vehicles.iterator()
        while (iterator.hasNext()) {
            val v = iterator.next()
            if (v.path.isEmpty() || v.pathIndex >= v.path.size) {
                iterator.remove()
                continue
            }

            val targetCoord = v.path[v.pathIndex]
            val tx = targetCoord.first.toFloat() + 0.5f
            val ty = targetCoord.second.toFloat() + 0.5f

            val dx = tx - v.x
            val dy = ty - v.y
            val dist = hypot(dx, dy)

            val moveStep = v.type.speed * speedMult
            if (dist <= moveStep * 1.5f) {
                v.x = tx
                v.y = ty
                v.pathIndex++
                if (v.pathIndex >= v.path.size) {
                    iterator.remove()
                }
            } else {
                v.angle = atan2(dy, dx)
                v.x += (dx / dist) * moveStep
                v.y += (dy / dist) * moveStep
            }
        }

        // Calculate Road Traffic Heatmap & Overall Traffic Index
        updateRoadCongestion()
    }

    fun dispatchEmergencyVehicle(targetX: Int, targetY: Int, type: VehicleType) {
        // Find nearest fire/police/hospital station
        var nearestX = -1
        var nearestY = -1
        var minDist = Float.MAX_VALUE

        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                val s = tile.service
                val matches = when (type) {
                    VehicleType.FIRE_TRUCK -> s?.category == ServiceCategory.FIRE
                    VehicleType.POLICE -> s?.category == ServiceCategory.POLICE
                    VehicleType.AMBULANCE -> s?.category == ServiceCategory.HEALTH
                    else -> false
                }
                if (matches) {
                    val d = hypot((x - targetX).toFloat(), (y - targetY).toFloat())
                    if (d < minDist) {
                        minDist = d
                        nearestX = x
                        nearestY = y
                    }
                }
            }
        }

        val startX = if (nearestX != -1) nearestX else world.width / 2
        val startY = if (nearestY != -1) nearestY else 2

        val path = findRoadPath(startX, startY, targetX, targetY)
        if (path.size >= 2) {
            val v = Vehicle(
                id = UUID.randomUUID().toString(),
                type = type,
                x = path[0].first + 0.5f,
                y = path[0].second + 0.5f,
                targetX = targetX,
                targetY = targetY,
                path = path,
                pathIndex = 1,
                isEmergencyMission = true
            )
            vehicles.add(v)
        }
    }

    private fun spawnRandomVehicle() {
        val roadTiles = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                if (world.tiles[x][y].road != RoadType.NONE) {
                    roadTiles.add(Pair(x, y))
                }
            }
        }

        if (roadTiles.size < 4) return

        val start = roadTiles[random.nextInt(roadTiles.size)]
        val end = roadTiles[random.nextInt(roadTiles.size)]
        if (start == end) return

        val path = findRoadPath(start.first, start.second, end.first, end.second)
        if (path.size >= 3) {
            val vType = when (random.nextInt(10)) {
                0, 1 -> VehicleType.BUS
                2, 3 -> VehicleType.TRUCK
                4 -> VehicleType.POLICE
                else -> VehicleType.CAR
            }

            val v = Vehicle(
                id = UUID.randomUUID().toString(),
                type = vType,
                x = path[0].first + 0.5f,
                y = path[0].second + 0.5f,
                targetX = end.first,
                targetY = end.second,
                path = path,
                pathIndex = 1
            )
            vehicles.add(v)
        }
    }

    private fun findRoadPath(startX: Int, startY: Int, endX: Int, endY: Int): List<Pair<Int, Int>> {
        if (!world.isInside(startX, startY) || !world.isInside(endX, endY)) return emptyList()

        val queue: Queue<Pair<Int, Int>> = LinkedList()
        val cameFrom = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()
        val visited = Array(world.width) { BooleanArray(world.height) }

        val start = Pair(startX, startY)
        queue.add(start)
        visited[startX][startY] = true

        var found = false
        val target = Pair(endX, endY)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            if (current == target) {
                found = true
                break
            }

            val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
            for ((dx, dy) in dirs) {
                val nx = current.first + dx
                val ny = current.second + dy
                if (world.isInside(nx, ny) && !visited[nx][ny]) {
                    val tile = world.tiles[nx][ny]
                    if (tile.road != RoadType.NONE || (nx == endX && ny == endY)) {
                        visited[nx][ny] = true
                        val nextNode = Pair(nx, ny)
                        cameFrom[nextNode] = current
                        queue.add(nextNode)
                    }
                }
            }
        }

        if (!found && !visited[endX][endY]) return emptyList()

        // Reconstruct path
        val path = mutableListOf<Pair<Int, Int>>()
        var curr: Pair<Int, Int>? = target
        while (curr != null) {
            path.add(curr)
            curr = cameFrom[curr]
        }
        path.reverse()
        return path
    }

    private fun updateRoadCongestion() {
        var totalRoadTiles = 0
        var totalCongestionScore = 0f

        // Count vehicles per road tile
        val vehicleCounts = mutableMapOf<Pair<Int, Int>, Int>()
        for (v in vehicles) {
            val rx = v.x.toInt()
            val ry = v.y.toInt()
            val k = Pair(rx, ry)
            vehicleCounts[k] = (vehicleCounts[k] ?: 0) + 1
        }

        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                if (tile.road != RoadType.NONE) {
                    totalRoadTiles++
                    val vCount = vehicleCounts[Pair(x, y)] ?: 0

                    // Surrounding building density pressure
                    var surroundingDensityScore = 0f
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (world.isInside(nx, ny)) {
                                val neighbor = world.tiles[nx][ny]
                                neighbor.building?.let { b ->
                                    surroundingDensityScore += when (b.zoneType.density) {
                                        DensityLevel.LOW -> 0.05f
                                        DensityLevel.MEDIUM -> 0.18f
                                        DensityLevel.HIGH -> 0.55f
                                        DensityLevel.NONE -> 0.0f
                                    }
                                }
                            }
                        }
                    }

                    // Public transport relief
                    val transitRelief = tile.publicTransportCoverage * 0.005f

                    val roadCapacity = when (tile.road) {
                        RoadType.SMALL_2L -> 1.0f
                        RoadType.MEDIUM_4L -> 2.2f
                        RoadType.LARGE_6L -> 4.5f
                        RoadType.HIGHWAY -> 8.0f
                        RoadType.BRIDGE -> 2.0f
                        RoadType.NONE -> 1.0f
                    }

                    val trafficLoad = ((vCount * 0.35f + surroundingDensityScore) - transitRelief) / roadCapacity
                    tile.trafficVolume = trafficLoad.coerceIn(0.05f, 1.0f)
                    totalCongestionScore += tile.trafficVolume
                } else {
                    tile.trafficVolume = 0f
                }
            }
        }

        if (totalRoadTiles > 0) {
            stats.trafficIndex = ((totalCongestionScore / totalRoadTiles) * 100).roundToInt().coerceIn(5, 100)
        } else {
            stats.trafficIndex = 10
        }
    }
}
