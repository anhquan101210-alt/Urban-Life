package com.example.game.engine

import com.example.game.model.*
import kotlin.math.*
import kotlin.random.Random

class WorldMap(
    val width: Int = 36,
    val height: Int = 36
) {
    val tiles: Array<Array<GridTile>> = Array(width) { x ->
        Array(height) { y ->
            GridTile(x = x, y = y)
        }
    }

    init {
        generateProceduralTerrain()
    }

    fun getTile(x: Int, y: Int): GridTile? {
        if (x !in 0 until width || y !in 0 until height) return null
        return tiles[x][y]
    }

    fun isInside(x: Int, y: Int): Boolean {
        return x in 0 until width && y in 0 until height
    }

    private fun generateProceduralTerrain() {
        val random = Random(42) // Consistent initial seed for beautiful world

        // 1. Generate river winding through map
        var riverX = width * 0.72f
        val riverPoints = mutableListOf<Pair<Int, Int>>()
        for (y in 0 until height) {
            val rx = (riverX + sin(y * 0.28f) * 3.5f + cos(y * 0.12f) * 2.0f).roundToInt().coerceIn(4, width - 5)
            riverPoints.add(Pair(rx, y))
            riverX -= 0.15f // slight diagonal slant
        }

        for ((rx, ry) in riverPoints) {
            // River width 2-3 tiles
            for (dx in -1..1) {
                val tx = rx + dx
                if (isInside(tx, ry)) {
                    tiles[tx][ry].terrain = TerrainType.WATER
                    tiles[tx][ry].elevation = 0f
                }
            }
        }

        // 2. Scenic Lake in top-right or bottom-left
        val lakeCenterX = (width * 0.25f).roundToInt()
        val lakeCenterY = (height * 0.28f).roundToInt()
        val lakeRadius = 3.8f
        for (x in 0 until width) {
            for (y in 0 until height) {
                val dist = hypot((x - lakeCenterX).toFloat(), (y - lakeCenterY).toFloat())
                if (dist < lakeRadius) {
                    tiles[x][y].terrain = TerrainType.WATER
                    tiles[x][y].elevation = 0f
                }
            }
        }

        // 3. Shoreline & Hills & Forests
        for (x in 0 until width) {
            for (y in 0 until height) {
                val tile = tiles[x][y]
                if (tile.terrain == TerrainType.WATER) continue

                // Check water proximity for shoreline
                var isNearWater = false
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (isInside(nx, ny) && tiles[nx][ny].terrain == TerrainType.WATER) {
                            isNearWater = true
                            break
                        }
                    }
                }

                if (isNearWater) {
                    tile.terrain = TerrainType.SHORE
                    tile.landValue = 65 // Scenic waterfront has higher land value!
                } else {
                    // Hills on upper left & bottom right
                    val hillNoise = sin(x * 0.35f) * cos(y * 0.35f) + sin((x + y) * 0.18f)
                    if (hillNoise > 1.15f && x > 2 && y > 2) {
                        tile.terrain = TerrainType.HILL
                        tile.elevation = 0.65f
                    } else {
                        tile.terrain = TerrainType.PLAINS
                        tile.elevation = 0f
                        // Plant trees in clusters
                        val treeNoise = sin(x * 0.7f + 1.2f) * cos(y * 0.6f + 2.5f)
                        if (treeNoise > 0.45f && random.nextFloat() > 0.3f) {
                            tile.treeType = random.nextInt(1, 4)
                        }
                    }
                }
            }
        }

        // 4. Initial Regional Highway Connection (Entry road into the map from south edge)
        val entryX = width / 2
        for (y in 0..4) {
            val tile = tiles[entryX][y]
            tile.road = RoadType.MEDIUM_4L
            tile.terrain = TerrainType.PLAINS
            tile.elevation = 0f
            tile.treeType = 0
            tile.hasPowerLine = true
            tile.hasWaterPipe = true
        }

        // Starter small intersection
        tiles[entryX - 1][4].road = RoadType.SMALL_2L
        tiles[entryX - 2][4].road = RoadType.SMALL_2L
        tiles[entryX + 1][4].road = RoadType.SMALL_2L
        tiles[entryX + 2][4].road = RoadType.SMALL_2L
        tiles[entryX][4].hasPowerLine = true
        tiles[entryX - 1][4].hasPowerLine = true
        tiles[entryX - 2][4].hasPowerLine = true
        tiles[entryX + 1][4].hasPowerLine = true
        tiles[entryX + 2][4].hasPowerLine = true
    }

    fun getNeighbors(x: Int, y: Int, includeDiagonals: Boolean = false): List<GridTile> {
        val result = mutableListOf<GridTile>()
        val dirs = if (includeDiagonals) {
            listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, 1 to -1, -1 to 1, 1 to 1)
        } else {
            listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        }
        for ((dx, dy) in dirs) {
            val nx = x + dx
            val ny = y + dy
            if (isInside(nx, ny)) {
                result.add(tiles[nx][ny])
            }
        }
        return result
    }

    fun hasAdjacentRoad(x: Int, y: Int): Boolean {
        for ((dx, dy) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            val nx = x + dx
            val ny = y + dy
            if (isInside(nx, ny) && tiles[nx][ny].road != RoadType.NONE) {
                return true
            }
        }
        return false
    }

    fun clearTile(x: Int, y: Int) {
        val tile = getTile(x, y) ?: return
        tile.zone = ZoneType.NONE
        tile.road = RoadType.NONE
        tile.service = null
        tile.utility = null
        tile.transport = null
        tile.building = null
        tile.treeType = 0
    }
}
