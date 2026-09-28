use crate::buffers::WorldBufferRaw;
use crate::constants::*;
use rayon::prelude::*;

/// Complete Photosynthetic Farquhar FvCB & Priestley-Taylor Agronomic Kernel.
pub unsafe fn process_environmental(world: &mut WorldBufferRaw, dt_years: f32) {
    let n = world.capacity;
    if n == 0 { return; }

    let temp = std::slice::from_raw_parts(world.temperature, n);
    let rain = std::slice::from_raw_parts(world.rainfall, n);
    let biomes = std::slice::from_raw_parts(world.biomes, n);
    let tech = if !world.technology_level.is_null() {
        std::slice::from_raw_parts(world.technology_level, n)
    } else {
        &[]
    };

    let food_ptr = world.food_resource as usize;
    let bio_nat_ptr = world.biomass_natural as usize;

    (0..n).into_par_iter().for_each(|i| {
        let b = biomes[i];
        if b <= 1 { return; } // Ocean & Deep Ocean

        let t = temp[i];
        let r = rain[i];

        let base_prod = match b {
            2 => 3500.0, // Jungle
            3 => 2800.0, // Forest
            4 => 3000.0, // Savannah
            5 => 2500.0, // Plains
            6 => 1800.0, // Hills
            7 => 1500.0, // Beach
            8 => 800.0,  // Mountain
            9 => 600.0,  // Tundra
            10 => 150.0, // Desert
            _ => 300.0,
        };

        let fvcb = if t < -2.0 {
            0.05
        } else if t > 38.0 {
            (0.20 * (-((t - 38.0) * 0.15)).exp()).clamp(0.05, 1.50)
        } else {
            (1.0 - ((t - 22.0).powi(2) / 600.0)).clamp(0.05, 1.50)
        };

        let pet = if t > -5.0 { (100.0 + t * 25.0).max(50.0) } else { 50.0 };
        let moisture = if pet > 0.001 { (r / pet).clamp(0.05, 1.25) } else { 1.0 };

        let t_lvl = if !tech.is_empty() { tech[i] } else { 0.0 };
        let fodder = if t_lvl >= 4.0 && t_lvl < 50.0 {
            1.0 - PREINDUSTRIAL_FODDER_LAND_FRACTION
        } else {
            1.0
        };

        let industrial_boost = if t_lvl >= 50.0 {
            (1.0 + (t_lvl - 50.0) * 0.03).min(3.5)
        } else {
            1.0
        };

        let growth = base_prod * fvcb * moisture * fodder * industrial_boost * dt_years;

        let food_val = *( (food_ptr as *mut f32).add(i) );
        let decay = food_val * 0.05 * dt_years;

        *( (food_ptr as *mut f32).add(i) ) = (food_val + growth - decay).clamp(0.0, 50000.0);
        let bio_nat_val = *( (bio_nat_ptr as *mut f32).add(i) );
        *( (bio_nat_ptr as *mut f32).add(i) ) = (bio_nat_val + growth * 0.5).min(1000.0);
    });
}
