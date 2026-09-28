use crate::buffers::{AgentBufferRaw, WorldBufferRaw};
use crate::constants::*;
use std::ffi::c_float;

/// Executes one demographic cycle: bioenergetic metabolism, Gompertz survival, and cohort mitosis.
pub unsafe fn process_demographics(
    world: &mut WorldBufferRaw,
    agents: &mut AgentBufferRaw,
    target_cohort_size: f32,
    dt_years: f32,
) {
    let a_cap = agents.capacity;
    let w_cap = world.capacity;
    if a_cap == 0 || w_cap == 0 { return; }

    let hex_ids = std::slice::from_raw_parts_mut(agents.hex_ids, a_cap);
    let mass = std::slice::from_raw_parts_mut(agents.mass, a_cap);
    let energy = std::slice::from_raw_parts_mut(agents.energy, a_cap);
    let sigma = std::slice::from_raw_parts_mut(agents.sigma_cost, a_cap);
    let age = std::slice::from_raw_parts_mut(agents.age, a_cap);
    let births = std::slice::from_raw_parts_mut(agents.births, a_cap);
    let deaths = std::slice::from_raw_parts_mut(agents.deaths, a_cap);
    let tech = std::slice::from_raw_parts(agents.tech_level, a_cap);
    let culture_slice = std::slice::from_raw_parts(agents.culture, 4 * a_cap);

    let food = std::slice::from_raw_parts_mut(world.food_resource, w_cap);
    let bio_human = std::slice::from_raw_parts_mut(world.biomass_human, w_cap);
    let bio_fish = std::slice::from_raw_parts(world.biomass_fish, w_cap);
    let biomes = std::slice::from_raw_parts(world.biomes, w_cap);
    let capital = std::slice::from_raw_parts_mut(world.resource_capital, w_cap);
    let water = std::slice::from_raw_parts(world.water_resource, w_cap);
    let food_consumed = std::slice::from_raw_parts_mut(world.energy_food_consumed, w_cap);

    // Reset accumulated human biomass
    for b in bio_human.iter_mut() {
        *b = 0.0;
    }

    let min_mass_threshold = (target_cohort_size * 0.001).clamp(0.0001, 0.05);

    // --- Step 1: Bioenergetic Metabolism and Lotka-Gompertz Dynamics ---
    for i in 0..a_cap {
        let h_idx = hex_ids[i];
        if h_idx < 0 || (h_idx as usize) >= w_cap { continue; }
        let h = h_idx as usize;

        let m = mass[i];
        if m < min_mass_threshold {
            hex_ids[i] = -1;
            continue;
        }

        age[i] += dt_years;
        let age_years = age[i];

        // Structural Cost Sigma
        sigma[i] = m.powf(1.05) * 0.01;

        // Trophic Footprint determination
        let t_lvl = tech[i];
        let trophic_mult = if t_lvl < 1.5 {
            let is_coastal = biomes[h] == 7 || bio_fish[h] > 100.0; // Beach or rich fish
            if is_coastal { TROPHIC_MULTIPLIER_COASTAL_FORAGER } else { TROPHIC_MULTIPLIER_HUNTER_GATHERER }
        } else if t_lvl < 4.0 {
            TROPHIC_MULTIPLIER_NEOLITHIC_EARLY_AGRARIAN
        } else if t_lvl < 50.0 {
            TROPHIC_MULTIPLIER_PREINDUSTRIAL_ADVANCED_AGRARIAN
        } else if t_lvl < 120.0 {
            TROPHIC_MULTIPLIER_INDUSTRIAL_WORKER
        } else {
            TROPHIC_MULTIPLIER_POST_INDUSTRIAL
        };

        let base_metabolic_need = m * HUMAN_ANNUAL_METABOLIC_ENERGY_GJ * dt_years;
        let raw_biomass_mobilized = base_metabolic_need * (trophic_mult / TROPHIC_MULTIPLIER_HUNTER_GATHERER);

        let food_taken = food[h].min(raw_biomass_mobilized);
        food[h] -= food_taken;
        food_consumed[h] += food_taken;

        // Carcass Capital byproduct in Hunter-Gatherer era
        if t_lvl < 1.5 && food_taken > 0.0 {
            capital[h] += food_taken * CARCASS_MATERIAL_BYPRODUCT_FRACTION * 0.05;
        }

        let food_satisfaction = if raw_biomass_mobilized > 0.0001 {
            food_taken / raw_biomass_mobilized
        } else {
            1.0
        };

        if food_satisfaction >= 0.8 {
            energy[i] = (energy[i] + 5.0 * food_satisfaction).min(100.0);
        } else {
            energy[i] = (energy[i] - 15.0 * (1.0 - food_satisfaction)).max(0.0);
        }

        // Natalism & Primiparité (Cultural Tensor dimension 1)
        let kinship_natalism = culture_slice[1 * a_cap + i].clamp(0.0, 1.0);
        let primiparity_age = HUMAN_MIN_PRIMIPARITY_AGE_YEARS 
            + 10.0 * (1.0 - kinship_natalism) 
            + 6.0 * (t_lvl / 80.0).tanh();
        let peak_fertility_age = primiparity_age + 8.0;

        let fecundity_age_factor = if age_years < primiparity_age - 1.5 {
            0.0
        } else {
            (-((age_years - peak_fertility_age).powi(2)) / (2.0 * 13.0_f32.powi(2))).exp()
        };

        let nutritional_factor = (0.15 + 0.85 * (energy[i] / 75.0) * food_satisfaction).clamp(0.05, 1.25);
        let cultural_fertility_mult = 0.35 + 1.30 * kinship_natalism;

        let carrying_cap = (food[h] / HUMAN_ANNUAL_METABOLIC_ENERGY_GJ).max(0.05);
        let malthusian_pressure = m / carrying_cap;
        let density_feedback = 1.0 / (1.0 + malthusian_pressure.powi(2));

        let max_bio_fertility = 0.065;
        let birth_rate = max_bio_fertility * fecundity_age_factor * nutritional_factor * cultural_fertility_mult * density_feedback;
        let new_births = m * birth_rate * dt_years;
        births[i] = new_births;

        // Gestation metabolic load
        let repro_load = new_births * HUMAN_GESTATION_LACTATION_ENERGY_GJ;
        let repro_food_taken = food[h].min(repro_load);
        food[h] -= repro_food_taken;
        food_consumed[h] += repro_food_taken;

        // Infant & Adult Gompertz-Makeham Mortality
        let capital_per_cap = if m > 0.001 { capital[h] / m } else { 0.0 };
        let water_security = if water[h] > 10.0 { 1.0 } else { 0.3 };
        let hygiene_factor = (-capital_per_cap / 50.0).exp();
        let infant_mortality = (0.015 + 0.25 * hygiene_factor * (1.5 - 0.5 * food_satisfaction) * (1.5 - 0.5 * water_security)).clamp(0.015, 0.50);

        let surviving_infants = new_births * (1.0 - infant_mortality);

        let gompertz_senescence = 0.0001 * (0.08 * age_years).exp();
        let acute_deficit = (1.0 - food_satisfaction).max(0.0);
        let starvation_mortality = if acute_deficit > 0.3 { 0.35 * (acute_deficit - 0.3) } else { 0.0 }
            + if energy[i] < 25.0 { 0.45 * (1.0 - energy[i] / 25.0) } else { 0.0 };

        let adult_mortality = (0.012 + gompertz_senescence + starvation_mortality).clamp(0.01, 0.98);
        let adult_deaths = m * adult_mortality * dt_years;
        deaths[i] = adult_deaths + (new_births - surviving_infants);

        let new_mass = (m + surviving_infants - adult_deaths).max(0.0);
        mass[i] = new_mass;

        if new_mass > min_mass_threshold {
            let surviving_mass = (m - adult_deaths).max(0.0);
            age[i] = ((age_years * surviving_mass) / new_mass).max(0.0);
        }

        bio_human[h] += mass[i];
        if mass[i] < min_mass_threshold {
            hex_ids[i] = -1;
        }
    }

    // --- Step 2: Cohort Fission & Mitosis ---
    process_mitosis(agents, a_cap, target_cohort_size);
}

unsafe fn process_mitosis(agents: &mut AgentBufferRaw, capacity: usize, target_cohort_size: f32) {
    let mass = std::slice::from_raw_parts_mut(agents.mass, capacity);
    let energy = std::slice::from_raw_parts_mut(agents.energy, capacity);
    let hex_ids = std::slice::from_raw_parts_mut(agents.hex_ids, capacity);
    let h3_indexes = std::slice::from_raw_parts_mut(agents.h3_indexes, capacity);
    let tech = std::slice::from_raw_parts_mut(agents.tech_level, capacity);
    let age = std::slice::from_raw_parts_mut(agents.age, capacity);
    let gen = std::slice::from_raw_parts_mut(agents.generation_count, capacity);
    let culture = std::slice::from_raw_parts_mut(agents.culture, 4 * capacity);
    let genetics = std::slice::from_raw_parts_mut(agents.genetics, 4 * capacity);

    for i in 0..capacity {
        if hex_ids[i] == -1 { continue; }

        if mass[i] >= target_cohort_size * 2.0 && energy[i] >= 50.0 {
            // Find free slot
            if let Some(free_idx) = (0..capacity).find(|&k| hex_ids[k] == -1) {
                mass[free_idx] = mass[i] / 2.0;
                energy[free_idx] = energy[i] / 2.0;
                mass[i] /= 2.0;
                energy[i] /= 2.0;

                hex_ids[free_idx] = hex_ids[i];
                h3_indexes[free_idx] = h3_indexes[i];
                tech[free_idx] = tech[i];
                gen[free_idx] = gen[i] + 1;
                age[free_idx] = 15.0; // New young adult cohort pivot

                for d in 0..4 {
                    genetics[d * capacity + free_idx] = genetics[d * capacity + i];
                    culture[d * capacity + free_idx] = culture[d * capacity + i];
                }
            }
        }
    }
}
