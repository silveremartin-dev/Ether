use crate::buffers::WorldBufferRaw;
use crate::constants::*;
use rayon::prelude::*;
use std::sync::LazyLock;

const LUT_TEMP_MIN: i32 = -10;
const _LUT_TEMP_MAX: i32 = 45;
const LUT_CO2_MIN: i32 = 150;
const LUT_CO2_STEP_SIZE: i32 = 25;
const PET_LUT_MIN: i32 = -5;

static FVCB_LUT: LazyLock<[[f32; 35]; 56]> = LazyLock::new(|| {
    let mut table = [[0.0_f32; 35]; 56];
    for t_idx in 0..56 {
        let temp_c = (LUT_TEMP_MIN + t_idx as i32) as f64;
        let temp_k = temp_c + 273.15;
        for c_idx in 0..35 {
            let co2_ppm = (LUT_CO2_MIN + c_idx as i32 * LUT_CO2_STEP_SIZE) as f64;
            if temp_c < -2.0 {
                table[t_idx][c_idx] = 0.05;
            } else {
                let inv_diff = (1.0 / 298.15) - (1.0 / temp_k);
                let vcmax = 60.0 * ((65000.0 / (R_GAS_CONSTANT as f64)) * inv_diff).exp();
                let gamma_star = 42.75 * ((37830.0 / (R_GAS_CONSTANT as f64)) * inv_diff).exp();
                let kc = 404.9 * ((79430.0 / (R_GAS_CONSTANT as f64)) * inv_diff).exp();
                let ko = 278.4 * ((36380.0 / (R_GAS_CONSTANT as f64)) * inv_diff).exp();
                let km = kc * (1.0 + 210.0 / ko);

                let ci = co2_ppm * 0.70;
                let ac = vcmax * (ci - gamma_star).max(0.0) / (ci + km).max(1.0);
                let aj = 0.5 * vcmax * (ci - gamma_star).max(0.0) / (ci + 2.0 * gamma_star).max(1.0);

                let mut anet = ac.min(aj);
                if temp_c > 38.0 {
                    anet *= (-((temp_c - 38.0) * 0.15)).exp();
                }
                table[t_idx][c_idx] = ((anet / 25.0).clamp(0.05, 1.50)) as f32;
            }
        }
    }
    table
});

static PET_LUT: LazyLock<[f32; 111]> = LazyLock::new(|| {
    let mut table = [0.0_f32; 111];
    for i in 0..111 {
        let t = (PET_LUT_MIN as f64) + (i as f64) * 0.5;
        let delta = (4098.0 * (0.6108 * (17.27 * t / (t + 237.3)).exp())) / ((t + 237.3) * (t + 237.3));
        let gamma_psy = 0.066;
        let alpha_pt = 1.26;
        let net_solar = (t + 10.0).max(0.0) * 35.0;
        let pet = alpha_pt * (delta / (delta + gamma_psy)) * net_solar.max(100.0);
        table[i] = pet as f32;
    }
    table
});

#[inline(always)]
fn evaluate_farquhar_yield(temp_c: f32, co2_ppm: f32) -> f32 {
    let t_idx = (temp_c.round() as i32 - LUT_TEMP_MIN).clamp(0, 55) as usize;
    let c_idx = (((co2_ppm - LUT_CO2_MIN as f32) / (LUT_CO2_STEP_SIZE as f32)).round() as i32).clamp(0, 34) as usize;
    FVCB_LUT[t_idx][c_idx]
}

#[inline(always)]
fn calculate_priestley_taylor_pet(temp_c: f32) -> f32 {
    if temp_c <= -5.0 { return 50.0; }
    if temp_c >= 50.0 { return PET_LUT[110]; }
    let idx_f = (temp_c - (PET_LUT_MIN as f32)) * 2.0;
    let idx = idx_f as usize;
    let frac = idx_f - (idx as f32);
    PET_LUT[idx] + frac * (PET_LUT[idx + 1] - PET_LUT[idx])
}

fn get_biome_production_rate(b: u8) -> f32 {
    match b {
        5 => 3500.0, // JUNGLE
        4 => 2800.0, // FOREST
        12 => 3000.0, // SAVANNAH
        3 => 2500.0, // PLAINS
        7 => 1800.0, // HILLS
        2 | 13 => 1500.0, // BEACH, LAKE
        8 => 800.0,  // MOUNTAINS
        0 | 1 => 1200.0, // OCEAN, DEEP_OCEAN
        10 => 600.0, // TUNDRA
        6 => 150.0,  // DESERT
        _ => 300.0,  // SNOW, GLACIER, DEFAULT
    }
}

fn get_biome_agricultural_potential(b: u8) -> f32 {
    match b {
        3 => 150000.0, // PLAINS
        12 => 120000.0, // SAVANNAH
        4 => 80000.0,  // FOREST
        5 => 70000.0,  // JUNGLE
        7 => 40000.0,  // HILLS
        2 | 13 => 25000.0, // BEACH, LAKE
        8 => 15000.0,  // MOUNTAINS
        10 => 5000.0,  // TUNDRA
        6 => 2000.0,   // DESERT
        _ => 1000.0,   // DEFAULT
    }
}

/// Complete Photosynthetic Farquhar FvCB & Priestley-Taylor Agronomic Kernel.
/// Mathematically and algorithmically identical to EnvironmentalKernel.java.
pub unsafe fn process_environmental(world: &mut WorldBufferRaw, dt_years: f32) {
    let n = world.capacity;
    if n == 0 { return; }

    let dt = dt_years.max(0.0001);
    let food_retention_factor = ((-0.03_f64 * (dt as f64)).exp()) as f32;

    let temp = std::slice::from_raw_parts(world.temperature, n);
    let rain = std::slice::from_raw_parts(world.rainfall, n);
    let biomes = std::slice::from_raw_parts(world.biomes, n);
    let tech = if !world.technology_level.is_null() {
        std::slice::from_raw_parts(world.technology_level, n)
    } else {
        &[]
    };
    let water = if !world.water_resource.is_null() {
        std::slice::from_raw_parts(world.water_resource, n)
    } else {
        &[]
    };
    let human_pop = if !world.biomass_human.is_null() {
        std::slice::from_raw_parts(world.biomass_human, n)
    } else {
        &[]
    };

    let food_ptr = world.food_resource as usize;
    let bio_nat_ptr = world.biomass_natural as usize;
    let bio_agri_ptr = world.biomass_agriculture as usize;

    let baseline_co2 = 280.0_f32;
    let area_scale = 1.0_f32;

    (0..n).into_par_iter().for_each(|i| {
        let b = biomes[i];
        if b <= 1 { return; } // OCEAN (0) & DEEP_OCEAN (1) are skipped in land pass

        let t = temp[i];
        let r = rain[i];
        let w = if !water.is_empty() { water[i] } else { 0.0 };
        let pop = if !human_pop.is_empty() { human_pop[i] } else { 0.0 };
        let t_lvl = if !tech.is_empty() { tech[i] } else { 0.0 };

        let base_prod = get_biome_production_rate(b) * area_scale;
        let fvcb = evaluate_farquhar_yield(t, baseline_co2);
        let pet = calculate_priestley_taylor_pet(t);
        let effective_moisture = r.max((r + w * 0.8).min(1500.0));
        let moisture_aridity_ratio = (effective_moisture / pet.max(100.0)).clamp(0.40, 1.25);

        let fodder_factor = if t_lvl >= 4.0 && t_lvl < 50.0 {
            1.0 - PREINDUSTRIAL_FODDER_LAND_FRACTION
        } else {
            1.0
        };

        let industrial_boost = if t_lvl >= 50.0 {
            (1.0 + (t_lvl - 50.0) * 0.03).min(3.5)
        } else {
            1.0
        };

        let agro_climatic_factor = (fvcb * moisture_aridity_ratio * fodder_factor * industrial_boost).clamp(0.40, 2.0);

        let max_ag_potential = get_biome_agricultural_potential(b) * area_scale * agro_climatic_factor * (1.0 + 0.05 * t_lvl.min(100.0));
        let ag_demand_potential = pop * 4.6;
        let ag_prod = if pop > 0.0 {
            max_ag_potential * (ag_demand_potential / (max_ag_potential + ag_demand_potential)) * dt
        } else {
            0.0
        };

        let natural_prod = base_prod * agro_climatic_factor * dt;
        let growth = natural_prod + ag_prod;

        let food_val = *( (food_ptr as *mut f32).add(i) );
        let retained_food = food_val * food_retention_factor;
        *( (food_ptr as *mut f32).add(i) ) = (retained_food + growth).clamp(0.0, 2000000.0 * area_scale);

        if bio_nat_ptr != 0 {
            let bio_val = *( (bio_nat_ptr as *mut f32).add(i) );
            *( (bio_nat_ptr as *mut f32).add(i) ) = (bio_val + natural_prod * 0.5).min(10000.0 * area_scale);
        }

        if bio_agri_ptr != 0 && ag_prod > 0.0 && t_lvl >= 1.5 {
            let bio_agri_val = *( (bio_agri_ptr as *mut f32).add(i) );
            if bio_agri_val <= 0.0 {
                *( (bio_agri_ptr as *mut f32).add(i) ) = (pop * 0.6).min(250.0 * area_scale);
            }
        }
    });
}
