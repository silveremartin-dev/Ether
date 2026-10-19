use crate::buffers::WorldBufferRaw;
use crate::constants::*;
use rayon::prelude::*;

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

fn evaluate_farquhar_yield(temp_c: f32, co2_ppm: f32) -> f32 {
    if temp_c < -2.0 {
        return 0.05;
    }
    let temp_k = (temp_c + KELVIN_ZERO_CELSIUS) as f64;
    let inv_diff = (1.0 / 298.15) - (1.0 / temp_k);

    let vcmax = 60.0 * ((65000.0 / (R_GAS_CONSTANT as f64)) * inv_diff).exp();
    let gamma_star = 42.75 * ((37830.0 / (R_GAS_CONSTANT as f64)) * inv_diff).exp();
    let kc = 404.9 * ((79430.0 / (R_GAS_CONSTANT as f64)) * inv_diff).exp();
    let ko = 278.4 * ((36380.0 / (R_GAS_CONSTANT as f64)) * inv_diff).exp();
    let km = kc * (1.0 + 210.0 / ko);

    let ci = (co2_ppm as f64) * 0.70;
    let ac = vcmax * (ci - gamma_star).max(0.0) / (ci + km).max(1.0);
    let aj = 0.5 * vcmax * (ci - gamma_star).max(0.0) / (ci + 2.0 * gamma_star).max(1.0);

    let mut anet = ac.min(aj);
    if temp_c > 38.0 {
        anet *= (-((temp_c as f64 - 38.0) * 0.15)).exp();
    }
    (anet / 25.0).clamp(0.05, 1.50) as f32
}

fn calculate_priestley_taylor_pet(temp_c: f32) -> f32 {
    if temp_c <= -5.0 { return 50.0; }
    let t = temp_c as f64;
    let delta = (4098.0 * (0.6108 * (17.27 * t / (t + 237.3)).exp())) / ((t + 237.3) * (t + 237.3));
    let gamma_psy = 0.066;
    let alpha_pt = 1.26;
    let net_solar = (t + 10.0).max(0.0) * 35.0;
    let pet = alpha_pt * (delta / (delta + gamma_psy)) * net_solar.max(100.0);
    pet.max(50.0) as f32
}

/// Complete Photosynthetic Farquhar FvCB & Priestley-Taylor Agronomic Kernel.
/// Mathematically and algorithmically identical to EnvironmentalKernel.java.
pub unsafe fn process_environmental(world: &mut WorldBufferRaw, dt_years: f32) {
    let n = world.capacity;
    if n == 0 { return; }

    let dt = dt_years.max(0.0001);
    let food_retention_factor = (-0.03 * dt).exp();

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
    let area_scale = 1.0_f32; // Standard macro-cell baseline

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

        // Biophysical agricultural ceiling (Boserup-Geertz involution on finite arable land):
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
