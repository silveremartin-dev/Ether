// =========================================================================
// Fundamental Physical, Thermodynamic, Biological, and Cliodynamic Constants
// =========================================================================

pub const R_GAS_CONSTANT: f32 = 8.314462;
pub const KELVIN_ZERO_CELSIUS: f32 = 273.15;
pub const SOLAR_CONSTANT_WPM2: f32 = 1361.0;
pub const SECONDS_PER_JULIAN_YEAR: f32 = 31557600.0;
pub const SECONDS_PER_DAY: f32 = 86400.0;

// Metabolic & Demographic Constants
pub const HUMAN_ANNUAL_METABOLIC_ENERGY_GJ: f32 = 3.362;
pub const HUMAN_GESTATION_LACTATION_ENERGY_GJ: f32 = 0.80;
pub const HUMAN_MIN_PRIMIPARITY_AGE_YEARS: f32 = 14.0;
pub const CARCASS_MATERIAL_BYPRODUCT_FRACTION: f32 = 0.20;

// Biomass Energy Densities (MJ/kg fresh weight)
pub const BIOMASS_ENERGY_DENSITY_FAUNA_MJ_PER_KG: f32 = 8.0;
pub const BIOMASS_ENERGY_DENSITY_FLORA_WILD_MJ_PER_KG: f32 = 4.5;
pub const BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG: f32 = 15.0;
pub const BIOMASS_ENERGY_DENSITY_MARINE_MJ_PER_KG: f32 = 7.0;
pub const BIOMASS_ENERGY_DENSITY_HERBAGE_MJ_PER_KG: f32 = 4.0;

// Trophic Footprints (PhysicalConstants.java)
pub const TROPHIC_MULTIPLIER_HUNTER_GATHERER: f32 = 2.25;
pub const TROPHIC_MULTIPLIER_COASTAL_FORAGER: f32 = 2.75;
pub const TROPHIC_MULTIPLIER_NOMADIC_PASTORALIST: f32 = 12.5;
pub const TROPHIC_MULTIPLIER_NEOLITHIC_EARLY_AGRARIAN: f32 = 2.25;
pub const TROPHIC_MULTIPLIER_PREINDUSTRIAL_ADVANCED_AGRARIAN: f32 = 5.0;
pub const TROPHIC_MULTIPLIER_INDUSTRIAL_WORKER: f32 = 6.0;
pub const TROPHIC_MULTIPLIER_POST_INDUSTRIAL: f32 = 20.0;

// Allometric & Entropy Scaling
pub const TAINTER_COMPLEXITY_EXPONENT: f32 = 1.15;
pub const PREINDUSTRIAL_FODDER_LAND_FRACTION: f32 = 0.35;
