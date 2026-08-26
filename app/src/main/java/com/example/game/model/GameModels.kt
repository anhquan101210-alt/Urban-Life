package com.example.game.model

enum class TerrainType(val displayName: String) {
    GRASS("Grass"),
    DARK_GRASS("Forest Grass"),
    DIRT("Dirt"),
    SAND("Sand"),
    ROCK("Rock"),
    WATER("Water"),
    SHORE("Shore Beach"),
    PLAINS("Plains"),
    HILL("Hills"),
    FOREST("Forest")
}

enum class ZoneType(
    val displayName: String,
    val category: ZoneCategory,
    val density: DensityLevel,
    val cost: Long,
    val basePowerDemand: Int,
    val baseWaterDemand: Int,
    val baseGarbageOutput: Int,
    val basePollution: Int,
    val baseTaxYield: Int
) {
    NONE("None", ZoneCategory.NONE, DensityLevel.NONE, 0, 0, 0, 0, 0, 0),

    // Residential (3 Densities)
    RESIDENTIAL_LOW("Low Res (Houses)", ZoneCategory.RESIDENTIAL, DensityLevel.LOW, 20, 4, 3, 2, 0, 8),
    RESIDENTIAL_MED("Med Res (Flats)", ZoneCategory.RESIDENTIAL, DensityLevel.MEDIUM, 60, 18, 15, 10, 1, 35),
    RESIDENTIAL_HIGH("High Res (Towers)", ZoneCategory.RESIDENTIAL, DensityLevel.HIGH, 150, 75, 70, 40, 3, 140),

    // Commercial (3 Densities)
    COMMERCIAL_LOW("Low Com (Cafes)", ZoneCategory.COMMERCIAL, DensityLevel.LOW, 30, 7, 5, 4, 1, 15),
    COMMERCIAL_MED("Med Com (Plazas)", ZoneCategory.COMMERCIAL, DensityLevel.MEDIUM, 90, 28, 22, 16, 2, 60),
    COMMERCIAL_HIGH("High Com (Skyscrapers)", ZoneCategory.COMMERCIAL, DensityLevel.HIGH, 220, 95, 85, 55, 4, 220),

    // Industrial (3 Densities)
    INDUSTRIAL_LOW("Low Ind (Workshops)", ZoneCategory.INDUSTRIAL, DensityLevel.LOW, 25, 10, 8, 8, 12, 18),
    INDUSTRIAL_MED("Med Ind (Factories)", ZoneCategory.INDUSTRIAL, DensityLevel.MEDIUM, 75, 40, 30, 30, 40, 70),
    INDUSTRIAL_HIGH("High Ind (Refineries)", ZoneCategory.INDUSTRIAL, DensityLevel.HIGH, 180, 130, 100, 85, 105, 260)
}

enum class ZoneCategory {
    NONE,
    RESIDENTIAL,
    COMMERCIAL,
    INDUSTRIAL
}

enum class DensityLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}

enum class RoadType(
    val displayName: String,
    val lanes: Int,
    val cost: Long,
    val maintenance: Long,
    val speedLimit: Float,
    val capacity: Int
) {
    NONE("None", 0, 0, 0, 0f, 0),
    SMALL_2L("Small Road (2L)", 2, 25, 1, 1.0f, 100),
    MEDIUM_4L("Avenue (4L)", 4, 60, 2, 1.4f, 250),
    LARGE_6L("Boulevard (6L)", 6, 120, 4, 1.8f, 500),
    HIGHWAY("Highway", 4, 220, 7, 2.5f, 1000),
    BRIDGE("Bridge (Water)", 2, 90, 3, 1.2f, 200)
}

enum class ServiceType(
    val displayName: String,
    val category: ServiceCategory,
    val cost: Long,
    val maintenance: Long,
    val radius: Int,
    val effectMagnitude: Int,
    val powerDemand: Int,
    val waterDemand: Int
) {
    POLICE_STATION("Police Station", ServiceCategory.POLICE, 600, 20, 8, 40, 15, 10),
    FIRE_STATION("Fire Station", ServiceCategory.FIRE, 600, 20, 8, 50, 15, 15),
    HOSPITAL("Hospital / Clinic", ServiceCategory.HEALTH, 900, 35, 10, 45, 25, 25),
    ELEMENTARY_SCHOOL("Elementary School", ServiceCategory.EDUCATION, 500, 15, 6, 30, 10, 10),
    HIGH_SCHOOL("High School", ServiceCategory.EDUCATION, 1100, 40, 9, 45, 20, 20),
    UNIVERSITY("University Campus", ServiceCategory.EDUCATION, 2800, 90, 15, 70, 50, 40),
    PARK_SMALL("Small Park", ServiceCategory.PARK, 150, 5, 4, 25, 2, 5),
    PARK_LARGE("Central Park", ServiceCategory.PARK, 650, 18, 9, 60, 5, 15),
    CEMETERY("Cemetery", ServiceCategory.DEATH_CARE, 400, 12, 7, 20, 5, 5),
    GARBAGE_PLANT("Garbage Facility", ServiceCategory.GARBAGE, 850, 30, 10, 60, 30, 20)
}

enum class ServiceCategory {
    POLICE,
    FIRE,
    HEALTH,
    EDUCATION,
    PARK,
    DEATH_CARE,
    GARBAGE
}

enum class UtilityType(
    val displayName: String,
    val category: UtilityCategory,
    val cost: Long,
    val maintenance: Long,
    val outputPowerMW: Int,
    val outputWaterMG: Int,
    val pollution: Int,
    val requiresWaterfront: Boolean
) {
    WIND_TURBINE("Wind Turbine", UtilityCategory.POWER, 450, 10, 45, 0, 0, false),
    SOLAR_PLANT("Solar Farm", UtilityCategory.POWER, 950, 18, 110, 0, 0, false),
    GAS_PLANT("Gas Power Plant", UtilityCategory.POWER, 2000, 45, 280, 0, 25, false),
    COAL_PLANT("Coal Power Plant", UtilityCategory.POWER, 3500, 75, 550, 0, 85, false),

    WATER_PUMP("Water Pump (River/Lake)", UtilityCategory.WATER, 600, 15, 0, 180, 0, true),
    WATER_TOWER("Water Tower", UtilityCategory.WATER, 350, 8, 0, 80, 0, false),
    SEWAGE_PLANT("Sewage Treatment", UtilityCategory.WATER, 800, 25, 0, 220, 20, false)
}

enum class UtilityCategory {
    POWER,
    WATER
}

enum class TransportType(
    val displayName: String,
    val cost: Long,
    val maintenance: Long,
    val trafficReliefRadius: Int
) {
    BUS_STOP("Bus Stop", 120, 4, 4),
    BUS_DEPOT("Bus Depot", 900, 30, 12),
    METRO_STATION("Metro Station", 4000, 120, 12)
}

enum class BuildingStage {
    EMPTY,
    FOUNDATION,
    CONSTRUCTION,
    BUILT,
    ABANDONED
}

data class Building(
    val id: String,
    val gridX: Int,
    val gridY: Int,
    val zoneType: ZoneType,
    var stage: BuildingStage = BuildingStage.FOUNDATION,
    var level: Int = 1, // 1 to 3
    var constructionTicks: Int = 0,
    var population: Int = 0,
    var jobs: Int = 0,
    var powerDemand: Int = 0,
    var waterDemand: Int = 0,
    var garbageOutput: Int = 0,
    var pollutionOutput: Int = 0,
    var happinessScore: Int = 75,
    var landValue: Int = 50,
    var onFire: Boolean = false,
    var fireProgress: Float = 0f,
    var isPowered: Boolean = true,
    var isWatered: Boolean = true,
    var hasRoadAccess: Boolean = true,
    var colorSeed: Int = 0,
    var styleVariant: Int = 0, // 0..4 for rich visual variations
    var buildingName: String = ""
)

data class GridTile(
    val x: Int,
    val y: Int,
    var elevation: Float = 0f,
    var terrain: TerrainType = TerrainType.GRASS,
    var zone: ZoneType = ZoneType.NONE,
    var road: RoadType = RoadType.NONE,
    var service: ServiceType? = null,
    var utility: UtilityType? = null,
    var transport: TransportType? = null,
    var building: Building? = null,
    var treeType: Int = 0, // 0 = none, 1..6 = tree & nature types

    // Simulation fields
    var landValue: Int = 40,
    var airPollution: Int = 0,
    var noisePollution: Int = 0,
    var crimeRate: Int = 5,
    var fireRisk: Int = 5,
    var healthCoverage: Int = 0,
    var educationCoverage: Int = 0,
    var policeCoverage: Int = 0,
    var fireCoverage: Int = 0,
    var parkCoverage: Int = 0,
    var publicTransportCoverage: Int = 0,
    var trafficVolume: Float = 0f, // 0.0 to 1.0 (Green to Red)

    var hasPowerLine: Boolean = false,
    var hasWaterPipe: Boolean = false
)

enum class VehicleType(
    val speed: Float,
    val colorHex: Long,
    val isEmergency: Boolean = false
) {
    CAR(0.045f, 0xFF4A90E2),
    TAXI(0.046f, 0xFFFFD600),
    SUV(0.042f, 0xFF43A047),
    BUS(0.032f, 0xFFFB8C00),
    TRUCK(0.028f, 0xFF8D6E63),
    POLICE(0.062f, 0xFF1E88E5, true),
    FIRE_TRUCK(0.056f, 0xFFE53935, true),
    AMBULANCE(0.060f, 0xFFFFFFFF, true)
}

data class Vehicle(
    val id: String,
    val type: VehicleType,
    var x: Float,
    var y: Float,
    var targetX: Int,
    var targetY: Int,
    var angle: Float = 0f,
    var pathIndex: Int = 0,
    var path: List<Pair<Int, Int>> = emptyList(),
    var isEmergencyMission: Boolean = false
)

data class Pedestrian(
    val id: String,
    var x: Float,
    var y: Float,
    var targetX: Int,
    var targetY: Int,
    var walkAnimFrame: Int = 0,
    var colorIndex: Int = 0,
    var path: List<Pair<Int, Int>> = emptyList(),
    var pathIndex: Int = 0,
    var hasUmbrella: Boolean = false
)

enum class WeatherType(val displayName: String, val icon: String) {
    SUNNY("Sunny", "☀"),
    CLOUDY("Cloudy", "☁"),
    RAIN("Rain", "🌧"),
    STORM("Thunderstorm", "⛈")
}

enum class OverlayMode(val label: String) {
    NORMAL("Normal 2D"),
    ZONES("Zoning"),
    TRAFFIC("Traffic Flow"),
    POLLUTION("Pollution"),
    LAND_VALUE("Land Value"),
    POWER("Power Grid"),
    WATER("Water Grid"),
    SERVICES("Service Coverage")
}

data class Disaster(
    val id: String,
    val title: String,
    val description: String,
    val x: Int,
    val y: Int,
    var durationTicks: Int,
    val type: DisasterType
)

enum class DisasterType {
    FIRE,
    POWER_OUTAGE,
    WATER_SHORTAGE,
    TRAFFIC_JAM,
    SMOG_CRISIS
}

data class CityStats(
    var population: Int = 0,
    var jobsTotal: Int = 0,
    var jobsEmployed: Int = 0,
    var happiness: Int = 80,
    var treasury: Long = 65000L,
    var dailyIncome: Long = 0L,
    var dailyExpenses: Long = 0L,
    var powerCapacityMW: Int = 0,
    var powerDemandMW: Int = 0,
    var waterCapacityMG: Int = 0,
    var waterDemandMG: Int = 0,
    var residentialTaxRate: Int = 10,
    var commercialTaxRate: Int = 10,
    var industrialTaxRate: Int = 10,
    var residentialDemand: Float = 0.6f, // 0.0 to 1.0
    var commercialDemand: Float = 0.4f,
    var industrialDemand: Float = 0.5f,
    var trafficIndex: Int = 15,
    var averageLandValue: Int = 45,
    var airPollutionIndex: Int = 5,
    var dayTime: Float = 9.0f, // 0.0 to 24.0 hours
    var dayCount: Int = 1,
    var simSpeed: Int = 1, // 0 = Pause, 1 = 1x, 2 = 2x, 4 = 4x
    var weather: WeatherType = WeatherType.SUNNY
)

data class HistoryPoint(
    val day: Int,
    val population: Int,
    val treasury: Long,
    val trafficIndex: Int,
    val happiness: Int
)

enum class ToolMode {
    INSPECT,
    ROAD,
    ZONE,
    SERVICE,
    UTILITY,
    TRANSPORT,
    DEMOLISH
}

data class ActiveTool(
    val mode: ToolMode = ToolMode.INSPECT,
    val roadType: RoadType = RoadType.NONE,
    val zoneType: ZoneType = ZoneType.NONE,
    val serviceType: ServiceType? = null,
    val utilityType: UtilityType? = null,
    val transportType: TransportType? = null
)

enum class GraphicsQuality(val label: String) {
    LOW("Low (Best FPS)"),
    MEDIUM("Medium (Balanced)"),
    HIGH("High (Full Pixel Details)")
}
