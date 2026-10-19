use crate::buffers::{AgentBufferRaw, WorldBufferRaw};
use crate::constants::*;

/// Executes one complete demographic simulation cycle across all active cohorts.
/// Mathematically and algorithmically identical to DemographicKernel.java.
pub unsafe fn process_demographics(
    world: &mut WorldBufferRaw,
    agents: &mut AgentBufferRaw,
    target_cohort_size: f32,
    dt_years: f32,
) {
    let a_cap = agents.capacity;
    let w_cap = world.capacity;
    if a_cap == 0 || w_cap == 0 { return; }

    let dt_in_years = dt_years.max(0.0001);
    let min_mass_threshold = (target_cohort_size * 0.001).clamp(0.0001, 0.05);

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
    let bio_natural = std::slice::from_raw_parts(world.biomass_natural, w_cap);
    let bio_agri = std::slice::from_raw_parts(world.biomass_agriculture, w_cap);
    let bio_fish = if !world.biomass_fish.is_null() {
        std::slice::from_raw_parts(world.biomass_fish, w_cap)
    } else {
        &[]
    };
    let biomes = std::slice::from_raw_parts(world.biomes, w_cap);
    let capital = std::slice::from_raw_parts_mut(world.resource_capital, w_cap);
    let water = std::slice::from_raw_parts(world.water_resource, w_cap);
    let food_consumed = std::slice::from_raw_parts_mut(world.energy_food_consumed, w_cap);
    let cell_tech = if !world.technology_level.is_null() {
        std::slice::from_raw_parts(world.technology_level, w_cap)
    } else {
        &[]
    };
    let lifespan = if !world.lifespan.is_null() {
        std::slice::from_raw_parts(world.lifespan, w_cap)
    } else {
        &[]
    };

    // Reset accumulated human biomass
    for b in bio_human.iter_mut() {
        *b = 0.0;
    }

    // Step 1: Pre-pass to accumulate cell-level demand & human population
    let mut cell_demand = vec![0.0_f32; w_cap];
    let mut cell_pop = vec![0.0_f32; w_cap];
    let mut cell_satisfaction = vec![1.0_f32; w_cap];

    for i in 0..a_cap {
        let h_idx = hex_ids[i];
        if h_idx < 0 || (h_idx as usize) >= w_cap { continue; }
        let h = h_idx as usize;
        let m = mass[i];
        if m < min_mass_threshold { continue; }

        let t = tech[i];
        let trophic_mult = if t < 1.5 {
            TROPHIC_MULTIPLIER_HUNTER_GATHERER
        } else {
            TROPHIC_MULTIPLIER_NEOLITHIC_EARLY_AGRARIAN
        };
        let base_metabolic_need = m * HUMAN_ANNUAL_METABOLIC_ENERGY_GJ * dt_in_years;
        let added_demand = base_metabolic_need * (trophic_mult / TROPHIC_MULTIPLIER_HUNTER_GATHERER);

        cell_pop[h] += m;
        cell_demand[h] += added_demand;
    }

    // Step 2: Food Consumption on Populated Cells
    for h in 0..w_cap {
        if cell_demand[h] > 0.0001 {
            let ct = if !cell_tech.is_empty() { cell_tech[h] } else { 1.0 };
            let edible_agri = bio_agri[h] * 0.70 * BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG;
            let edible_nat = bio_natural[h] * (if ct < 1.5 { 0.008 } else { 0.005 }) * BIOMASS_ENERGY_DENSITY_FLORA_WILD_MJ_PER_KG;
            let edible_fish = (if !bio_fish.is_empty() { bio_fish[h] } else { 0.0 }) * 0.25 * BIOMASS_ENERGY_DENSITY_MARINE_MJ_PER_KG;
            let total_annual_food_flux_gj = edible_agri + edible_nat + edible_fish;

            let food_harvest_tick_gj = total_annual_food_flux_gj * dt_in_years;
            let total_avail = food[h] + food_harvest_tick_gj;

            cell_satisfaction[h] = (total_avail / cell_demand[h]).clamp(0.0, 1.0);
            let consumed = total_avail.min(cell_demand[h]);
            let surplus = (total_avail - consumed).max(0.0);

            let storage_years = if ct < 1.5 { 0.083_f32 } else { 1.5_f32 };
            let max_granary_cap = cell_demand[h] * (storage_years / dt_in_years);
            food[h] = surplus.min(max_granary_cap);
            food_consumed[h] += consumed;
        } else {
            cell_satisfaction[h] = 1.0;
        }
    }

    // Step 3: Cohort Evolution Pass
    for i in 0..a_cap {
        let h_idx = hex_ids[i];
        if h_idx < 0 || (h_idx as usize) >= w_cap { continue; }
        let h = h_idx as usize;

        let m = mass[i];
        if m < min_mass_threshold {
            hex_ids[i] = -1;
            continue;
        }

        let age_years = age[i];
        sigma[i] = ((m as f64).powf(1.05) as f32) * 0.01;

        let t = tech[i];
        let trophic_mult = if t < 1.5 {
            // Beach ordinal in Biome.java is 2
            let is_coastal = biomes[h] == 2 || (!bio_fish.is_empty() && bio_fish[h] > 100.0);
            if is_coastal { TROPHIC_MULTIPLIER_COASTAL_FORAGER } else { TROPHIC_MULTIPLIER_HUNTER_GATHERER }
        } else if t < 4.0 {
            TROPHIC_MULTIPLIER_NEOLITHIC_EARLY_AGRARIAN
        } else if t < 50.0 {
            TROPHIC_MULTIPLIER_PREINDUSTRIAL_ADVANCED_AGRARIAN
        } else if t < 120.0 {
            TROPHIC_MULTIPLIER_INDUSTRIAL_WORKER
        } else {
            TROPHIC_MULTIPLIER_POST_INDUSTRIAL
        };

        let food_satisfaction = cell_satisfaction[h];
        let base_metabolic_need = m * HUMAN_ANNUAL_METABOLIC_ENERGY_GJ * dt_in_years;
        let raw_biomass_mobilized = base_metabolic_need * (trophic_mult / TROPHIC_MULTIPLIER_HUNTER_GATHERER);
        let food_taken = raw_biomass_mobilized * food_satisfaction;

        // Hunter-gatherer carcass capital byproduct
        if t < 1.5 && food_taken > 0.0 {
            capital[h] += food_taken * CARCASS_MATERIAL_BYPRODUCT_FRACTION * 0.05;
        }

        // Energy store update
        if food_satisfaction >= 0.8 {
            energy[i] = (energy[i] + 5.0 * food_satisfaction).min(100.0);
        } else {
            energy[i] = (energy[i] - 15.0 * (1.0 - food_satisfaction)).max(0.0);
        }

        // 1. Primiparity & Fecundity
        let kinship_natalism = culture_slice[1 * a_cap + i].clamp(0.0, 1.0);
        let primiparity_age = HUMAN_MIN_PRIMIPARITY_AGE_YEARS 
            + 10.0 * (1.0 - kinship_natalism) 
            + 6.0 * ((t as f64 / 80.0).tanh() as f32);
        let peak_fertility_age = primiparity_age + 8.0;

        let fecundity_age_factor = if age_years < primiparity_age - 1.5 {
            0.0
        } else {
            let age_diff = age_years - peak_fertility_age;
            ((-((age_diff * age_diff) as f64) / 338.0).exp()) as f32
        };

        let nutritional_factor = (0.15 + 0.85 * (energy[i] / 75.0) * food_satisfaction).clamp(0.05, 1.25);
        let cultural_fertility_mult = 0.40 + 1.20 * kinship_natalism;

        // 4. Malthusian density feedback
        let edible_nat = bio_natural[h] * (if t < 1.5 { 0.008 } else { 0.005 }) * BIOMASS_ENERGY_DENSITY_FLORA_WILD_MJ_PER_KG;
        let edible_agri = bio_agri[h] * 0.70 * BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG;
        let edible_fish = (if !bio_fish.is_empty() { bio_fish[h] } else { 0.0 }) * 0.25 * BIOMASS_ENERGY_DENSITY_MARINE_MJ_PER_KG;
        let renewable_flow = edible_nat + edible_agri + edible_fish;
        let annual_cell_productivity = renewable_flow + (renewable_flow * 0.20).min(food[h] * 0.15);
        let carrying_capacity = (annual_cell_productivity / HUMAN_ANNUAL_METABOLIC_ENERGY_GJ).max(1.0);
        let total_cell_pop = m.max(cell_pop[h]);
        let malthusian_pressure = total_cell_pop / carrying_capacity;
        let density_feedback = 1.0 / (1.0 + malthusian_pressure.powi(2));

        // 5. Birth Rate
        let max_bio_fertility = if t < 1.5 {
            (0.024_f64 + 0.004_f64 * (t as f64).tanh()) as f32
        } else {
            (0.0375_f64 + 0.012_f64 * (((t - 1.5) / 1.5) as f64).tanh()) as f32
        };
        let fertility_modulation = fecundity_age_factor * nutritional_factor.min(1.0) * cultural_fertility_mult * density_feedback;
        let birth_rate = max_bio_fertility * fertility_modulation.clamp(0.0, 1.25);
        let new_births = m * birth_rate * dt_in_years;
        births[i] = new_births;

        // 6. Maternal reproductive load
        let repro_load = new_births * HUMAN_GESTATION_LACTATION_ENERGY_GJ;
        let repro_food_taken = food[h].min(repro_load);
        food[h] -= repro_food_taken;
        food_consumed[h] += repro_food_taken;

        // 7. Infant mortality
        let capital_per_cap = if m > 0.001 { capital[h] / m } else { 0.0 };
        let water_security = if water[h] > 10.0 { 1.0 } else { 0.3 };
        let hygiene_factor = ((-capital_per_cap as f64 / 50.0).exp()) as f32;
        let mut infant_mortality_rate = (0.015 + 0.20 * hygiene_factor * (1.5 - 0.5 * food_satisfaction) * (1.5 - 0.5 * water_security)).clamp(0.015, 0.50);

        let mut lifespan_ratio = 1.0_f32;
        if !lifespan.is_empty() && lifespan[h] > 5.0 {
            lifespan_ratio = (40.0 / lifespan[h]).clamp(0.40, 2.50);
            if lifespan_ratio > 1.0 {
                infant_mortality_rate = (infant_mortality_rate * (0.85 + 0.15 * lifespan_ratio)).clamp(0.015, 0.55);
            }
        }

        let surviving_newborns = new_births * (1.0 - infant_mortality_rate);
        let non_surviving_infants = new_births - surviving_newborns;

        // 8. Adult & Senescent mortality
        let gompertz_senescence = (0.0001_f64 * (0.08_f64 * age_years as f64).exp()) as f32;
        let acute_deficit = (1.0 - food_satisfaction).max(0.0);
        let starvation_mortality = (if acute_deficit > 0.3 { 0.35 * (acute_deficit - 0.3) } else { 0.0 })
            + (if energy[i] < 25.0 { 0.45 * (1.0 - energy[i] / 25.0) } else { 0.0 });
        let baseline_hazard = 0.010 * lifespan_ratio;
        let adult_hazard_rate = (baseline_hazard + gompertz_senescence + starvation_mortality).max(0.008);

        let survival_fraction = ((-adult_hazard_rate as f64 * dt_in_years as f64).exp()) as f32;
        let adult_deaths = m * (1.0 - survival_fraction);
        let total_deaths = adult_deaths + non_surviving_infants;
        deaths[i] = total_deaths;

        let new_mass = (m * survival_fraction) + surviving_newborns;
        mass[i] = new_mass.max(0.0);

        // 10. Continuous age renewal dynamics
        if new_mass > min_mass_threshold {
            let senescent_deaths = adult_deaths.min(m * gompertz_senescence * dt_in_years);
            let mean_senescent_death_age = (age_years + 10.0).max(62.0);
            let age_sum = (age_years + dt_in_years) * m
                - age_years * (adult_deaths - senescent_deaths).max(0.0)
                - mean_senescent_death_age * senescent_deaths;
            let mut updated_mean_age = (age_sum / new_mass).max(16.0);

            let target_eq_age = 24.5 + 12.0 * ((t as f64 / 80.0).tanh() as f32);
            let gen_turnover = (0.08 * dt_in_years).min(1.0);
            updated_mean_age += (target_eq_age - updated_mean_age) * gen_turnover;
            age[i] = updated_mean_age.clamp(18.0, 50.0);
        }

        bio_human[h] += mass[i];
        if mass[i] < min_mass_threshold {
            hex_ids[i] = -1;
        }
    }

    // Step 4: Dunbar Mitosis
    process_mitosis(agents, a_cap, target_cohort_size);
}

fn find_free_slot(hex_ids: &[i32], capacity: usize, hint: &mut usize) -> Option<usize> {
    for idx in *hint..capacity {
        if hex_ids[idx] == -1 {
            *hint = if idx + 1 < capacity { idx + 1 } else { 0 };
            return Some(idx);
        }
    }
    for idx in 0..*hint {
        if hex_ids[idx] == -1 {
            *hint = if idx + 1 < capacity { idx + 1 } else { 0 };
            return Some(idx);
        }
    }
    None
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

    let mut hint = 0;
    for i in 0..capacity {
        if hex_ids[i] == -1 { continue; }

        if mass[i] >= target_cohort_size * 1.25 && energy[i] >= 25.0 {
            if let Some(new_slot) = find_free_slot(hex_ids, capacity, &mut hint) {
                mass[new_slot] = mass[i] / 2.0;
                energy[new_slot] = energy[i]; // Preserves per-capita energy status
                mass[i] /= 2.0;

                hex_ids[new_slot] = hex_ids[i];
                h3_indexes[new_slot] = h3_indexes[i];
                tech[new_slot] = tech[i];
                gen[new_slot] = gen[i] + 1;

                let age_rnd = crate::prng::next_double(42, 0, new_slot as i64, 403) as f32;
                age[new_slot] = (18.0 + age_rnd * 8.0).max(16.0);

                for d in 0..4 {
                    let mut_g = ((crate::prng::next_double(42, 0, new_slot as i64, 405 + d as i64) - 0.5) as f32) * 0.05_f32;
                    let mut_c = ((crate::prng::next_double(42, 0, new_slot as i64, 415 + d as i64) - 0.5) as f32) * 0.05_f32;
                    genetics[d * capacity + new_slot] = (genetics[d * capacity + i] + mut_g).clamp(0.0, 1.0);
                    culture[d * capacity + new_slot] = (culture[d * capacity + i] + mut_c).clamp(0.0, 1.0);
                }
            } else {
                break;
            }
        }
    }
}
