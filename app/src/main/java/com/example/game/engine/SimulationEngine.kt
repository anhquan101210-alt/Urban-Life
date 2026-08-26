package com.example.game.engine

import com.example.game.model.*
import java.util.UUID
import kotlin.math.*
import kotlin.random.Random

class SimulationEngine(
    val world: WorldMap,
    val stats: CityStats = CityStats()
) {
    val history = mutableListOf<HistoryPoint>()
    val activeDisasters = mutableListOf<Disaster>()

    private val random = Random(System.currentTimeMillis())
    private var tickAccumulator = 0
    private var dailyTickCounter = 0

    init {
        // Initial snapshot
        recordHistory()
    }

    fun tick() {
        if (stats.simSpeed == 0) return

        val speedMultiplier = stats.simSpeed
        tickAccumulator += speedMultiplier

        // Advance Day / Night clock
        stats.dayTime += 0.05f * speedMultiplier
        if (stats.dayTime >= 24f) {
            stats.dayTime = 0f
            stats.dayCount++
            onNewDay()
        }

        // Run simulation sub-systems every 4 ticks
        if (tickAccumulator >= 4) {
            tickAccumulator = 0
            dailyTickCounter++

            updateUtilitiesGrid()
            updateServiceCoverageAndLandValue()
            updatePollution()
            updateZoningAndBuildingGrowth()
            updateRCIDemand()
            simulateDisasters()
            calculateCityMetrics()
        }
    }

    private fun onNewDay() {
        // Daily financial accounting
        collectTaxesAndPayMaintenance()
        recordHistory()
        checkRandomDisasterEvent()
    }

    private fun updateUtilitiesGrid() {
        var totalPowerCapacity = 0
        var totalWaterCapacity = 0
        var totalPowerDemand = 0
        var totalWaterDemand = 0

        // 1. Gather generation from power & water utility buildings
        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                val util = tile.utility
                if (util != null) {
                    totalPowerCapacity += util.outputPowerMW
                    totalWaterCapacity += util.outputWaterMG
                }
            }
        }

        // 2. Gather demand from services and buildings
        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]

                // Service demand
                tile.service?.let {
                    totalPowerDemand += it.powerDemand
                    totalWaterDemand += it.waterDemand
                }

                // Building demand
                tile.building?.let { b ->
                    if (b.stage == BuildingStage.BUILT || b.stage == BuildingStage.CONSTRUCTION) {
                        val densityFactor = when (b.zoneType.density) {
                            DensityLevel.LOW -> 1.0f
                            DensityLevel.MEDIUM -> 3.5f
                            DensityLevel.HIGH -> 12.0f
                            DensityLevel.NONE -> 0.0f
                        }
                        val levelFactor = 0.8f + (b.level * 0.4f)
                        b.powerDemand = (b.zoneType.basePowerDemand * densityFactor * levelFactor).roundToInt()
                        b.waterDemand = (b.zoneType.baseWaterDemand * densityFactor * levelFactor).roundToInt()

                        totalPowerDemand += b.powerDemand
                        totalWaterDemand += b.waterDemand
                    }
                }
            }
        }

        stats.powerCapacityMW = totalPowerCapacity
        stats.powerDemandMW = totalPowerDemand
        stats.waterCapacityMG = totalWaterCapacity
        stats.waterDemandMG = totalWaterDemand

        val powerSufficient = totalPowerCapacity >= totalPowerDemand || totalPowerDemand == 0
        val waterSufficient = totalWaterCapacity >= totalWaterDemand || totalWaterDemand == 0

        // Distribute power & water connectivity
        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                tile.building?.let { b ->
                    b.isPowered = powerSufficient || (totalPowerCapacity > 0 && random.nextFloat() < (totalPowerCapacity.toFloat() / totalPowerDemand.coerceAtLeast(1)))
                    b.isWatered = waterSufficient || (totalWaterCapacity > 0 && random.nextFloat() < (totalWaterCapacity.toFloat() / totalWaterDemand.coerceAtLeast(1)))
                    b.hasRoadAccess = world.hasAdjacentRoad(x, y)
                }
            }
        }
    }

    private fun updateServiceCoverageAndLandValue() {
        // Reset coverage arrays
        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                tile.policeCoverage = 0
                tile.fireCoverage = 0
                tile.healthCoverage = 0
                tile.educationCoverage = 0
                tile.parkCoverage = 0
                tile.publicTransportCoverage = 0
            }
        }

        // Radiate service coverage
        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                val s = tile.service
                if (s != null) {
                    val rad = s.radius
                    for (dx in -rad..rad) {
                        for (dy in -rad..rad) {
                            val nx = x + dx
                            val ny = y + dy
                            if (world.isInside(nx, ny)) {
                                val dist = hypot(dx.toFloat(), dy.toFloat())
                                if (dist <= rad) {
                                    val intensity = ((1f - dist / (rad + 1f)) * s.effectMagnitude).roundToInt()
                                    val target = world.tiles[nx][ny]
                                    when (s.category) {
                                        ServiceCategory.POLICE -> target.policeCoverage = max(target.policeCoverage, intensity)
                                        ServiceCategory.FIRE -> target.fireCoverage = max(target.fireCoverage, intensity)
                                        ServiceCategory.HEALTH -> target.healthCoverage = max(target.healthCoverage, intensity)
                                        ServiceCategory.EDUCATION -> target.educationCoverage = max(target.educationCoverage, intensity)
                                        ServiceCategory.PARK -> target.parkCoverage = max(target.parkCoverage, intensity)
                                        ServiceCategory.DEATH_CARE -> target.healthCoverage = max(target.healthCoverage, intensity / 2)
                                        ServiceCategory.GARBAGE -> target.healthCoverage = max(target.healthCoverage, intensity / 2)
                                    }
                                }
                            }
                        }
                    }
                }

                // Public transport radiation
                val tr = tile.transport
                if (tr != null) {
                    val rad = tr.trafficReliefRadius
                    for (dx in -rad..rad) {
                        for (dy in -rad..rad) {
                            val nx = x + dx
                            val ny = y + dy
                            if (world.isInside(nx, ny)) {
                                val dist = hypot(dx.toFloat(), dy.toFloat())
                                if (dist <= rad) {
                                    val intensity = ((1f - dist / (rad + 1f)) * 40).roundToInt()
                                    world.tiles[nx][ny].publicTransportCoverage = max(world.tiles[nx][ny].publicTransportCoverage, intensity)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Calculate Land Value per tile
        var landValueSum = 0
        var totalLandTiles = 0

        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                if (tile.terrain == TerrainType.WATER) continue
                totalLandTiles++

                var lv = 40

                // Waterfront bonus
                if (tile.terrain == TerrainType.SHORE) lv += 20

                // Service bonuses
                lv += (tile.parkCoverage * 0.4f).roundToInt()
                lv += (tile.educationCoverage * 0.3f).roundToInt()
                lv += (tile.healthCoverage * 0.25f).roundToInt()
                lv += (tile.policeCoverage * 0.2f).roundToInt()
                lv += (tile.publicTransportCoverage * 0.25f).roundToInt()

                // Road accessibility
                if (tile.road != RoadType.NONE || world.hasAdjacentRoad(x, y)) lv += 10

                // Pollution penalty
                lv -= (tile.airPollution * 0.4f).roundToInt()
                lv -= (tile.noisePollution * 0.3f).roundToInt()

                tile.landValue = lv.coerceIn(10, 100)
                landValueSum += tile.landValue
            }
        }

        if (totalLandTiles > 0) {
            stats.averageLandValue = (landValueSum / totalLandTiles).coerceIn(10, 100)
        }
    }

    private fun updatePollution() {
        var totalAirPollution = 0
        var tileCount = 0

        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                var air = 0
                var noise = 0

                // Industry pollution based on density
                tile.building?.let { b ->
                    if (b.stage == BuildingStage.BUILT) {
                        if (b.zoneType.category == ZoneCategory.INDUSTRIAL) {
                            when (b.zoneType.density) {
                                DensityLevel.LOW -> air += 15
                                DensityLevel.MEDIUM -> air += 45
                                DensityLevel.HIGH -> air += 110
                                DensityLevel.NONE -> {}
                            }
                        }
                    }
                }

                // Utility pollution (Coal/Gas)
                tile.utility?.let { u ->
                    air += u.pollution
                }

                // Traffic noise
                if (tile.road != RoadType.NONE) {
                    noise += (tile.trafficVolume * 50).roundToInt()
                    if (tile.road == RoadType.HIGHWAY) {
                        noise += 25
                        air += 15
                    }
                }

                tile.airPollution = air.coerceIn(0, 100)
                tile.noisePollution = noise.coerceIn(0, 100)

                totalAirPollution += tile.airPollution
                tileCount++
            }
        }

        if (tileCount > 0) {
            stats.airPollutionIndex = (totalAirPollution / tileCount).coerceIn(0, 100)
        }
    }

    private fun updateZoningAndBuildingGrowth() {
        var currentPop = 0
        var currentJobs = 0

        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                val zone = tile.zone

                if (zone == ZoneType.NONE) {
                    // If no zone and no service/road, demolish old building if any
                    if (tile.building != null) {
                        tile.building = null
                    }
                    continue
                }

                // If zone exists but no building yet
                if (tile.building == null && tile.road == RoadType.NONE && tile.service == null && tile.utility == null) {
                    // Check if ready to spawn foundation
                    val hasRoad = world.hasAdjacentRoad(x, y)
                    val demand = when (zone.category) {
                        ZoneCategory.RESIDENTIAL -> stats.residentialDemand
                        ZoneCategory.COMMERCIAL -> stats.commercialDemand
                        ZoneCategory.INDUSTRIAL -> stats.industrialDemand
                        ZoneCategory.NONE -> 0f
                    }

                    // High density requires decent land value to even start
                    val lvThreshold = when (zone.density) {
                        DensityLevel.LOW -> 15
                        DensityLevel.MEDIUM -> 35
                        DensityLevel.HIGH -> 55
                        DensityLevel.NONE -> 0
                    }

                    if (hasRoad && demand > 0.15f && tile.landValue >= lvThreshold && random.nextFloat() < (0.25f * demand)) {
                        val newBuilding = Building(
                            id = UUID.randomUUID().toString(),
                            gridX = x,
                            gridY = y,
                            zoneType = zone,
                            stage = BuildingStage.FOUNDATION,
                            level = 1,
                            constructionTicks = 0,
                            colorSeed = random.nextInt(100),
                            styleVariant = random.nextInt(4),
                            buildingName = generateBuildingName(zone, 1)
                        )
                        tile.building = newBuilding
                        tile.treeType = 0 // Clear trees for construction
                    }
                }

                // Existing building progression
                tile.building?.let { b ->
                    // Road access & utility check
                    b.hasRoadAccess = world.hasAdjacentRoad(x, y)

                    if (!b.hasRoadAccess) {
                        // Without road access, building abandons
                        b.happinessScore = max(0, b.happinessScore - 5)
                        if (b.happinessScore <= 10) b.stage = BuildingStage.ABANDONED
                    }

                    when (b.stage) {
                        BuildingStage.FOUNDATION -> {
                            b.constructionTicks++
                            if (b.constructionTicks >= 6) {
                                b.stage = BuildingStage.CONSTRUCTION
                                b.constructionTicks = 0
                            }
                        }
                        BuildingStage.CONSTRUCTION -> {
                            b.constructionTicks++
                            if (b.constructionTicks >= 8) {
                                b.stage = BuildingStage.BUILT
                                b.constructionTicks = 0
                                populateBuilding(b)
                            }
                        }
                        BuildingStage.BUILT -> {
                            // Check upgrade conditions
                            checkBuildingUpgrade(b, tile)
                            calculateBuildingHappiness(b, tile)
                        }
                        BuildingStage.ABANDONED -> {
                            // Can recover if road and utilities restored
                            if (b.hasRoadAccess && b.isPowered && b.isWatered && random.nextFloat() < 0.1f) {
                                b.stage = BuildingStage.BUILT
                                b.happinessScore = 50
                                populateBuilding(b)
                            }
                        }
                        BuildingStage.EMPTY -> {}
                    }

                    if (b.stage == BuildingStage.BUILT) {
                        currentPop += b.population
                        currentJobs += b.jobs
                    }
                }
            }
        }

        stats.population = currentPop
        stats.jobsTotal = currentJobs
        stats.jobsEmployed = (min(currentPop * 0.6f, currentJobs.toFloat())).roundToInt()
    }

    private fun populateBuilding(b: Building) {
        val lvl = b.level
        when (b.zoneType.category) {
            ZoneCategory.RESIDENTIAL -> {
                b.population = when (b.zoneType.density) {
                    DensityLevel.LOW -> when (lvl) {
                        1 -> random.nextInt(2, 5)
                        2 -> random.nextInt(4, 8)
                        else -> random.nextInt(7, 14)
                    }
                    DensityLevel.MEDIUM -> when (lvl) {
                        1 -> random.nextInt(20, 45)
                        2 -> random.nextInt(45, 70)
                        else -> random.nextInt(70, 95)
                    }
                    DensityLevel.HIGH -> when (lvl) {
                        1 -> random.nextInt(120, 220)
                        2 -> random.nextInt(220, 360)
                        else -> random.nextInt(360, 560)
                    }
                    DensityLevel.NONE -> 0
                }
                b.jobs = 0
            }
            ZoneCategory.COMMERCIAL -> {
                b.jobs = when (b.zoneType.density) {
                    DensityLevel.LOW -> when (lvl) {
                        1 -> random.nextInt(4, 10)
                        2 -> random.nextInt(10, 18)
                        else -> random.nextInt(18, 30)
                    }
                    DensityLevel.MEDIUM -> when (lvl) {
                        1 -> random.nextInt(30, 60)
                        2 -> random.nextInt(60, 100)
                        else -> random.nextInt(100, 160)
                    }
                    DensityLevel.HIGH -> when (lvl) {
                        1 -> random.nextInt(150, 260)
                        2 -> random.nextInt(260, 420)
                        else -> random.nextInt(420, 700)
                    }
                    DensityLevel.NONE -> 0
                }
                b.population = 0
            }
            ZoneCategory.INDUSTRIAL -> {
                b.jobs = when (b.zoneType.density) {
                    DensityLevel.LOW -> when (lvl) {
                        1 -> random.nextInt(6, 16)
                        2 -> random.nextInt(16, 30)
                        else -> random.nextInt(30, 48)
                    }
                    DensityLevel.MEDIUM -> when (lvl) {
                        1 -> random.nextInt(40, 80)
                        2 -> random.nextInt(80, 140)
                        else -> random.nextInt(140, 220)
                    }
                    DensityLevel.HIGH -> when (lvl) {
                        1 -> random.nextInt(120, 240)
                        2 -> random.nextInt(240, 400)
                        else -> random.nextInt(400, 650)
                    }
                    DensityLevel.NONE -> 0
                }
                b.population = 0
            }
            ZoneCategory.NONE -> {}
        }
        b.buildingName = generateBuildingName(b.zoneType, b.level)
    }

    private fun checkBuildingUpgrade(b: Building, tile: GridTile) {
        if (b.level >= 3) return

        val demand = when (b.zoneType.category) {
            ZoneCategory.RESIDENTIAL -> stats.residentialDemand
            ZoneCategory.COMMERCIAL -> stats.commercialDemand
            ZoneCategory.INDUSTRIAL -> stats.industrialDemand
            ZoneCategory.NONE -> 0f
        }

        // Requirements for upgrade
        val reqLandValue = when (b.zoneType.density) {
            DensityLevel.LOW -> if (b.level == 1) 45 else 65
            DensityLevel.MEDIUM -> if (b.level == 1) 55 else 75
            DensityLevel.HIGH -> if (b.level == 1) 70 else 85
            DensityLevel.NONE -> 999
        }

        val hasServices = when (b.zoneType.density) {
            DensityLevel.LOW -> tile.fireCoverage > 10 || tile.policeCoverage > 10 || tile.parkCoverage > 10
            DensityLevel.MEDIUM -> tile.healthCoverage > 15 && tile.educationCoverage > 15
            DensityLevel.HIGH -> tile.healthCoverage > 25 && tile.educationCoverage > 25 && tile.fireCoverage > 25 && tile.policeCoverage > 25
            DensityLevel.NONE -> false
        }

        if (b.hasRoadAccess && b.isPowered && b.isWatered &&
            tile.landValue >= reqLandValue && b.happinessScore >= 70 &&
            demand > 0.3f && hasServices && random.nextFloat() < 0.08f
        ) {
            b.level++
            populateBuilding(b)
        }
    }

    private fun calculateBuildingHappiness(b: Building, tile: GridTile) {
        var hap = 75

        // Utilities
        if (!b.isPowered) hap -= 35
        if (!b.isWatered) hap -= 35
        if (!b.hasRoadAccess) hap -= 40

        // Services
        hap += (tile.parkCoverage * 0.2f).roundToInt()
        hap += (tile.healthCoverage * 0.15f).roundToInt()
        hap += (tile.educationCoverage * 0.15f).roundToInt()
        hap += (tile.policeCoverage * 0.15f).roundToInt()
        hap += (tile.fireCoverage * 0.15f).roundToInt()

        // Negative factors
        hap -= (tile.airPollution * 0.35f).roundToInt()
        hap -= (tile.noisePollution * 0.25f).roundToInt()
        hap -= (tile.trafficVolume * 25).roundToInt()

        // Tax impact
        val taxRate = when (b.zoneType.category) {
            ZoneCategory.RESIDENTIAL -> stats.residentialTaxRate
            ZoneCategory.COMMERCIAL -> stats.commercialTaxRate
            ZoneCategory.INDUSTRIAL -> stats.industrialTaxRate
            ZoneCategory.NONE -> 10
        }
        if (taxRate > 12) hap -= (taxRate - 12) * 4
        else if (taxRate < 9) hap += (9 - taxRate) * 2

        b.happinessScore = hap.coerceIn(5, 100)
    }

    private fun updateRCIDemand() {
        // Residential demand driven by available jobs vs population
        val pop = stats.population.coerceAtLeast(1)
        val jobs = stats.jobsTotal
        val rRatio = (jobs.toFloat() / (pop * 0.6f).coerceAtLeast(1f))
        var rDemand = 0.5f + (rRatio - 1.0f) * 0.4f
        if (stats.happiness > 80) rDemand += 0.2f
        else if (stats.happiness < 60) rDemand -= 0.3f
        stats.residentialDemand = rDemand.coerceIn(0.05f, 1.0f)

        // Commercial demand driven by population shopping power
        val cRatio = (pop * 0.25f) / (jobs * 0.4f).coerceAtLeast(1f)
        var cDemand = 0.45f + (cRatio - 1.0f) * 0.3f
        stats.commercialDemand = cDemand.coerceIn(0.05f, 1.0f)

        // Industrial demand driven by regional trade & unemployment
        val iDemand = 0.5f + (stats.residentialDemand * 0.3f) - (stats.airPollutionIndex * 0.003f)
        stats.industrialDemand = iDemand.coerceIn(0.05f, 1.0f)
    }

    private fun collectTaxesAndPayMaintenance() {
        var dailyIncome = 0L
        var dailyExpenses = 0L

        // 1. Tax collection from buildings
        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                tile.building?.let { b ->
                    if (b.stage == BuildingStage.BUILT) {
                        val baseYield = b.zoneType.baseTaxYield
                        val levelMultiplier = when (b.level) {
                            1 -> 1.0f
                            2 -> 2.2f
                            else -> 4.5f
                        }
                        val taxRate = when (b.zoneType.category) {
                            ZoneCategory.RESIDENTIAL -> stats.residentialTaxRate
                            ZoneCategory.COMMERCIAL -> stats.commercialTaxRate
                            ZoneCategory.INDUSTRIAL -> stats.industrialTaxRate
                            ZoneCategory.NONE -> 10
                        }
                        val taxEarned = (baseYield * levelMultiplier * (taxRate / 10f) * (b.happinessScore / 100f)).roundToLong()
                        dailyIncome += max(1L, taxEarned)
                    }
                }

                // 2. Maintenance costs
                if (tile.road != RoadType.NONE) {
                    dailyExpenses += tile.road.maintenance
                }
                tile.service?.let {
                    dailyExpenses += it.maintenance
                }
                tile.utility?.let {
                    dailyExpenses += it.maintenance
                }
                tile.transport?.let {
                    dailyExpenses += it.maintenance
                }
            }
        }

        stats.dailyIncome = dailyIncome
        stats.dailyExpenses = dailyExpenses
        stats.treasury = (stats.treasury + dailyIncome - dailyExpenses).coerceAtLeast(-50000L)
    }

    private fun calculateCityMetrics() {
        var hapSum = 0
        var builtCount = 0

        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                tile.building?.let { b ->
                    if (b.stage == BuildingStage.BUILT) {
                        hapSum += b.happinessScore
                        builtCount++
                    }
                }
            }
        }

        if (builtCount > 0) {
            stats.happiness = (hapSum / builtCount).coerceIn(10, 100)
        } else {
            stats.happiness = 85
        }
    }

    private fun simulateDisasters() {
        val iterator = activeDisasters.iterator()
        while (iterator.hasNext()) {
            val d = iterator.next()
            d.durationTicks--

            if (d.type == DisasterType.FIRE) {
                val tile = world.getTile(d.x, d.y)
                tile?.building?.let { b ->
                    b.onFire = true
                    b.fireProgress += 0.05f
                    // If fire station covers this tile, extinguish fire faster
                    if (tile.fireCoverage > 20 || b.fireProgress >= 1.0f) {
                        b.onFire = false
                        b.fireProgress = 0f
                        d.durationTicks = 0
                        if (tile.fireCoverage <= 10) {
                            b.stage = BuildingStage.ABANDONED // burned down!
                        }
                    }
                }
            }

            if (d.durationTicks <= 0) {
                iterator.remove()
            }
        }
    }

    private fun checkRandomDisasterEvent() {
        if (activeDisasters.isNotEmpty()) return

        // 1. Fire disaster chance
        if (random.nextFloat() < 0.12f && stats.population > 50) {
            // Find a random built building
            val candidates = mutableListOf<GridTile>()
            for (x in 0 until world.width) {
                for (y in 0 until world.height) {
                    val tile = world.tiles[x][y]
                    if (tile.building?.stage == BuildingStage.BUILT && !tile.building!!.onFire) {
                        candidates.add(tile)
                    }
                }
            }
            if (candidates.isNotEmpty()) {
                val target = candidates[random.nextInt(candidates.size)]
                target.building?.onFire = true
                val d = Disaster(
                    id = UUID.randomUUID().toString(),
                    title = "Building on Fire!",
                    description = "A fire broke out at (${target.x}, ${target.y})! Fire trucks are en route.",
                    x = target.x,
                    y = target.y,
                    durationTicks = 20,
                    type = DisasterType.FIRE
                )
                activeDisasters.add(d)
            }
        }
    }

    private fun recordHistory() {
        history.add(
            HistoryPoint(
                day = stats.dayCount,
                population = stats.population,
                treasury = stats.treasury,
                trafficIndex = stats.trafficIndex,
                happiness = stats.happiness
            )
        )
        if (history.size > 50) {
            history.removeAt(0)
        }
    }

    private fun generateBuildingName(zone: ZoneType, level: Int): String {
        return when (zone.category) {
            ZoneCategory.RESIDENTIAL -> when (zone.density) {
                DensityLevel.LOW -> when (level) {
                    1 -> "Suburban Cottage"
                    2 -> "Family Home"
                    else -> "Luxury Villa"
                }
                DensityLevel.MEDIUM -> when (level) {
                    1 -> "Brick Townhouse"
                    2 -> "Parkside Flats"
                    else -> "Modern Residence"
                }
                DensityLevel.HIGH -> when (level) {
                    1 -> "High-Rise Tower"
                    2 -> "Metropolis Heights"
                    else -> "Pinnacle Skyscraper"
                }
                DensityLevel.NONE -> "Empty Lot"
            }
            ZoneCategory.COMMERCIAL -> when (zone.density) {
                DensityLevel.LOW -> when (level) {
                    1 -> "Corner Cafe"
                    2 -> "Neighborhood Market"
                    else -> "Boutique Arcade"
                }
                DensityLevel.MEDIUM -> when (level) {
                    1 -> "Business Plaza"
                    2 -> "Corporate Center"
                    else -> "Financial Block"
                }
                DensityLevel.HIGH -> when (level) {
                    1 -> "Commercial Tower"
                    2 -> "Grand Trade Plaza"
                    else -> "World Financial Center"
                }
                DensityLevel.NONE -> "Empty Shop"
            }
            ZoneCategory.INDUSTRIAL -> when (zone.density) {
                DensityLevel.LOW -> when (level) {
                    1 -> "Artisan Workshop"
                    2 -> "Storage Warehouse"
                    else -> "Precision Works"
                }
                DensityLevel.MEDIUM -> when (level) {
                    1 -> "Fabrication Plant"
                    2 -> "Logistics Hub"
                    else -> "Advanced Assembly"
                }
                DensityLevel.HIGH -> when (level) {
                    1 -> "Heavy Foundry"
                    2 -> "Chemical Complex"
                    else -> "Automated Megafactory"
                }
                DensityLevel.NONE -> "Empty Yard"
            }
            ZoneCategory.NONE -> "Wilderness"
        }
    }
}
