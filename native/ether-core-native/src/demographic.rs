use crate::buffers::{AgentBufferRaw, WorldBufferRaw};
use crate::constants::*;
use crate::environmental::{sigmoid, softplus};

#[inline(always)]
fn compute_smooth_trophic_ratio(cap_per_cap: f64, fish_biomass: f64) -> f64 {
    let coastal_share = (fish_biomass.max(0.0) / 250.0).tanh();
    let forager_ratio = 1.0 + 0.22222222 * coastal_share;
    let agrarian_draft_expansion = 1.22 * sigmoid(cap_per_cap, 45.0, 18.0);
    let industrial_cold_chain_expansion = 5.60 * sigmoid(cap_per_cap, 1200.0, 450.0);
    forager_ratio + agrarian_draft_expansion + industrial_cold_chain_expansion
}

/// Executes one complete demographic simulation cycle across all active cohorts with default seed 42.
pub unsafe fn process_demographics(
    world: &mut WorldBufferRaw,
    agents: &mut AgentBufferRaw,
    target_cohort_size: f32,
    dt_years: f32,
) {
    process_demographics_full(world, agents, target_cohort_size, dt_years, 42);
}

/// Executes one complete demographic simulation cycle across all active cohorts.
/// Mathematically identical to `DemographicKernel.java` (Zero state-level technology reads or writes).
pub unsafe fn process_demographics_full(
    world: &mut WorldBufferRaw,
    agents: &mut AgentBufferRaw,
    target_cohort_size: f32,
    dt_years: f32,
    seed: i64,
) {
    let a_cap = agents.capacity;
    let w_cap = world.capacity;
    if a_cap == 0 || w_cap == 0 {
        return;
    }

    let dt_in_years = dt_years.max(0.0001);
    let min_mass_threshold = (target_cohort_size * 0.001).clamp(0.0001, 0.05);

    let hex_ids = std::slice::from_raw_parts_mut(agents.hex_ids, a_cap);
    let mass = std::slice::from_raw_parts_mut(agents.mass, a_cap);
    let energy = std::slice::from_raw_parts_mut(agents.energy, a_cap);
    let sigma = std::slice::from_raw_parts_mut(agents.sigma_cost, a_cap);
    let age = std::slice::from_raw_parts_mut(agents.age, a_cap);
    let births = std::slice::from_raw_parts_mut(agents.births, a_cap);
    let deaths = std::slice::from_raw_parts_mut(agents.deaths, a_cap);
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
    let capital = std::slice::from_raw_parts_mut(world.resource_capital, w_cap);
    let water = std::slice::from_raw_parts(world.water_resource, w_cap);
    let food_consumed = std::slice::from_raw_parts_mut(world.energy_food_consumed, w_cap);
    let lifespan = if !world.lifespan.is_null() {
        std::slice::from_raw_parts(world.lifespan, w_cap)
    } else {
        &[]
    };

    for b in bio_human.iter_mut() {
        *b = 0.0;
    }

    let mut cell_demand = vec![0.0_f32; w_cap];
    let mut cell_pop = vec![0.0_f32; w_cap];
    let mut cell_satisfaction = vec![1.0_f32; w_cap];

    // Step 1: Accumulate active cohort population per cell
    for i in 0..a_cap {
        let h_idx = hex_ids[i];
        if h_idx < 0 || (h_idx as usize) >= w_cap {
            continue;
        }
        let m = mass[i];
        if m < min_mass_threshold {
            continue;
        }
        cell_pop[h_idx as usize] += m;
    }

    // Step 2: Cell-level metabolic demand and food satisfaction from physical observables
    for h in 0..w_cap {
        let pop_h = cell_pop[h];
        if pop_h > 0.0001 {
            let cap_per_cap = (capital[h] / pop_h) as f64;
            let fish_bio = if !bio_fish.is_empty() { bio_fish[h] } else { 0.0 };
            let demand_trophic_factor = 1.0 + 0.15 * (cap_per_cap / 120.0).tanh();
            let demand = ((pop_h as f64)
                * (HUMAN_ANNUAL_METABOLIC_ENERGY_GJ as f64)
                * demand_trophic_factor
                * (dt_in_years as f64)) as f32;
            cell_demand[h] = demand;

            let edible_agri = bio_agri[h] * 0.70 * (BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG * 1e-3);
            let wild_harvest_frac = 0.005 + 0.003 / (1.0 + pop_h / 1500.0);
            let edible_nat = bio_natural[h] * wild_harvest_frac * BIOMASS_ENERGY_DENSITY_FLORA_WILD_MJ_PER_KG;
            let edible_fish = fish_bio * 0.25 * (BIOMASS_ENERGY_DENSITY_MARINE_MJ_PER_KG * 1e-3);
            let total_annual_food_flux_gj = edible_agri + edible_nat + edible_fish;

            let food_harvest_tick_gj = total_annual_food_flux_gj * dt_in_years;
            let total_avail = food[h] + food_harvest_tick_gj;

            cell_satisfaction[h] = (total_avail / demand.max(1e-6)).clamp(0.0, 1.0);
            let consumed = total_avail.min(demand);
            let surplus = (total_avail - consumed).max(0.0);

            let storage_logistic = (cap_per_cap / 35.0).tanh() as f32;
            let storage_years = 0.083 + 1.417 * storage_logistic;
            let max_granary_cap = demand * (storage_years / dt_in_years);
            food[h] = surplus.min(max_granary_cap);
            food_consumed[h] += consumed;
        }
    }

    // Step 3: Cohort Evolution Pass
    for i in 0..a_cap {
        let h_idx = hex_ids[i];
        if h_idx < 0 || (h_idx as usize) >= w_cap {
            continue;
        }
        let h = h_idx as usize;

        let m = mass[i];
        if m < min_mass_threshold {
            hex_ids[i] = -1;
            continue;
        }

        let age_years = age[i];
        sigma[i] = ((m as f64).powf(1.05) as f32) * 0.01;

        let total_cell_pop = m.max(cell_pop[h]);
        let cap_per_cap = (capital[h] / total_cell_pop) as f64;
        let fish_bio = if !bio_fish.is_empty() { bio_fish[h] as f64 } else { 0.0 };
        let trophic_ratio = compute_smooth_trophic_ratio(cap_per_cap, fish_bio);

        let food_satisfaction = cell_satisfaction[h];
        let base_metabolic_need = (m as f64) * (HUMAN_ANNUAL_METABOLIC_ENERGY_GJ as f64) * (dt_in_years as f64);
        let food_taken = (base_metabolic_need * trophic_ratio * (food_satisfaction as f64)) as f32;

        let forager_weight = 1.0 - (cap_per_cap / 20.0).tanh();
        if food_taken > 0.0 {
            capital[h] += ((food_taken as f64) * (CARCASS_MATERIAL_BYPRODUCT_FRACTION as f64) * 0.05 * forager_weight) as f32;
        }

        // Smooth C^infty somatic energy store update
        let sat_gate = sigmoid(food_satisfaction as f64, 0.80, 0.05);
        let energy_gain = 5.0 * (food_satisfaction as f64);
        let energy_loss = 15.0 * (1.0 - food_satisfaction as f64);
        let delta_energy = sat_gate * energy_gain - (1.0 - sat_gate) * energy_loss;
        energy[i] = ((energy[i] as f64 + delta_energy) as f32).clamp(0.0, 100.0);

        // 1. Smooth Primiparity & Fecundity Curve
        let kinship_natalism = culture_slice[a_cap + i].clamp(0.0, 1.0);
        let delayed_marriage_shift = 6.0 * (cap_per_cap / 450.0).tanh();
        let primiparity_age = (HUMAN_MIN_PRIMIPARITY_AGE_YEARS as f64)
            + 10.0 * (1.0 - kinship_natalism as f64)
            + delayed_marriage_shift;
        let peak_fertility_age = primiparity_age + 8.0;
        let maturity_gate = sigmoid(age_years as f64, primiparity_age - 1.0, 0.75);
        let age_diff = (age_years as f64) - peak_fertility_age;
        let fecundity_age_factor = (maturity_gate * (-(age_diff * age_diff) / 338.0).exp()) as f32;

        let nutritional_factor = (0.15 + 0.85 * (energy[i] / 75.0) * food_satisfaction).clamp(0.05, 1.25);
        let cultural_fertility_mult = 0.40 + 1.20 * kinship_natalism;

        // 3. Malthusian density feedback
        let wild_harvest_frac = 0.005 + 0.003 * forager_weight;
        let edible_nat = (bio_natural[h] as f64) * wild_harvest_frac * (BIOMASS_ENERGY_DENSITY_FLORA_WILD_MJ_PER_KG as f64);
        let edible_agri = (bio_agri[h] as f64) * 0.70 * ((BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG as f64) * 1e-3);
        let edible_fish = fish_bio * 0.25 * ((BIOMASS_ENERGY_DENSITY_MARINE_MJ_PER_KG as f64) * 1e-3);
        let renewable_flow = edible_nat + edible_agri + edible_fish;
        let annual_cell_prod = renewable_flow + (renewable_flow * 0.20).min((food[h] as f64) * 0.15);

        let boserup_intensification = 1.0 + 0.36 * (cap_per_cap / 80.0).tanh();
        let synthetic_nitrogen_boost = 1.0 + 3.5 * sigmoid(cap_per_cap, 800.0, 250.0);
        let carrying_capacity = ((annual_cell_prod * boserup_intensification * synthetic_nitrogen_boost)
            / (HUMAN_ANNUAL_METABOLIC_ENERGY_GJ as f64))
            .max(1.0);
        let malthusian_pressure = (total_cell_pop as f64) / carrying_capacity;
        let density_feedback = (1.0 / (1.0 + malthusian_pressure * malthusian_pressure)) as f32;

        // 4. Bocquet-Appel Neolithic fertility transition from cultivated agrarian caloric share
        let agri_caloric_share = edible_agri / (renewable_flow + 1e-4);
        let neolithic_weight = sigmoid(agri_caloric_share + 0.35 * (cap_per_cap / 30.0).tanh(), 0.25, 0.10);
        let forager_fertility = 0.026_f64;
        let agrarian_fertility = 0.042 - 0.016 * sigmoid(cap_per_cap, 2400.0, 600.0);
        let max_bio_fertility = ((1.0 - neolithic_weight) * forager_fertility + neolithic_weight * agrarian_fertility) as f32;
        let fertility_modulation = fecundity_age_factor * nutritional_factor.min(1.0) * cultural_fertility_mult * density_feedback;

        let birth_rate = max_bio_fertility * fertility_modulation.clamp(0.0, 1.25);
        let new_births = m * birth_rate * dt_in_years;
        births[i] = m * birth_rate;

        // 5. Maternal reproductive load
        let repro_load = new_births * HUMAN_GESTATION_LACTATION_ENERGY_GJ;
        let repro_food_taken = food[h].min(repro_load);
        food[h] -= repro_food_taken;
        food_consumed[h] += repro_food_taken;

        // 6. Omran / Preston Sanitary Infant Mortality
        let cell_water = water[h] as f64;
        let water_security = 0.30 + 0.70 * (cell_water / (10.0 + cell_water.max(0.0)));
        let unsanitary_exposure = 1.0 - sigmoid(cap_per_cap, 320.0, 110.0);
        let baseline_infant_mort = 0.018 + 0.30 * unsanitary_exposure;
        let nutritional_stress = 1.5 - 0.5 * (food_satisfaction as f64);
        let water_stress = 1.5 - 0.5 * water_security;
        let mut infant_mortality_rate = ((baseline_infant_mort * nutritional_stress * water_stress).clamp(0.015, 0.55)) as f32;

        let mut lifespan_ratio = 1.0_f32;
        if !lifespan.is_empty() && lifespan[h] > 5.0 {
            lifespan_ratio = (40.0 / lifespan[h]).clamp(0.40, 2.50);
            infant_mortality_rate = (infant_mortality_rate * (0.85 + 0.15 * lifespan_ratio)).clamp(0.015, 0.55);
        }

        let surviving_newborns = new_births * (1.0 - infant_mortality_rate);
        let non_surviving_infants = new_births - surviving_newborns;

        // 7. Gompertz-Makeham Adult & Senescent Hazard Law
        let gompertz_senescence = (0.0001_f64 * (0.08_f64 * age_years as f64).exp()) as f32;
        let acute_deficit = softplus(1.0 - food_satisfaction as f64, 20.0);
        let deficit_mortality = 0.35 * sigmoid(acute_deficit, 0.40, 0.08);
        let energy_mortality = 0.45 * (1.0 - sigmoid(energy[i] as f64, 15.0, 4.0));
        let starvation_mortality = deficit_mortality + energy_mortality;
        let baseline_hazard = (0.008 + 0.016 * unsanitary_exposure) * (lifespan_ratio as f64);
        let adult_hazard_rate = (baseline_hazard + gompertz_senescence as f64 + starvation_mortality).max(0.008) as f32;

        let survival_fraction = ((-adult_hazard_rate as f64 * dt_in_years as f64).exp()) as f32;
        let adult_deaths = m * (1.0 - survival_fraction);
        deaths[i] = adult_deaths + non_surviving_infants;

        let new_mass = (m * survival_fraction) + surviving_newborns;
        mass[i] = new_mass.max(0.0);

        // 8. Continuous Lotka Age Renewal Dynamics
        if new_mass > min_mass_threshold {
            let senescent_deaths = adult_deaths.min(m * gompertz_senescence * dt_in_years);
            let mean_senescent_death_age = (age_years + 10.0).max(62.0);
            let age_sum = (age_years + dt_in_years) * m
                - age_years * (adult_deaths - senescent_deaths).max(0.0)
                - mean_senescent_death_age * senescent_deaths;
            let mut updated_mean_age = (age_sum / new_mass).max(16.0);

            let target_eq_age = (24.5 + 12.0 * (cap_per_cap / 450.0).tanh()) as f32;
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
    process_mitosis(agents, a_cap, target_cohort_size, seed);
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

unsafe fn process_mitosis(agents: &mut AgentBufferRaw, capacity: usize, target_cohort_size: f32, seed: i64) {
    let mass = std::slice::from_raw_parts_mut(agents.mass, capacity);
    let energy = std::slice::from_raw_parts_mut(agents.energy, capacity);
    let hex_ids = std::slice::from_raw_parts_mut(agents.hex_ids, capacity);
    let h3_indexes = std::slice::from_raw_parts_mut(agents.h3_indexes, capacity);
    let age = std::slice::from_raw_parts_mut(agents.age, capacity);
    let gen = std::slice::from_raw_parts_mut(agents.generation_count, capacity);
    let culture = std::slice::from_raw_parts_mut(agents.culture, 4 * capacity);
    let genetics = std::slice::from_raw_parts_mut(agents.genetics, 4 * capacity);

    let mut hint = 0;
    for i in 0..capacity {
        if hex_ids[i] == -1 {
            continue;
        }

        if mass[i] >= target_cohort_size * 1.25 && energy[i] >= 25.0 {
            if let Some(new_slot) = find_free_slot(hex_ids, capacity, &mut hint) {
                mass[new_slot] = mass[i] / 2.0;
                energy[new_slot] = energy[i];
                mass[i] /= 2.0;

                hex_ids[new_slot] = hex_ids[i];
                h3_indexes[new_slot] = h3_indexes[i];
                gen[new_slot] = gen[i] + 1;

                let age_rnd = crate::prng::next_double(seed, 0, new_slot as i64, 403) as f32;
                age[new_slot] = (18.0 + age_rnd * 8.0).max(16.0);

                for d in 0..4 {
                    let mut_g = ((crate::prng::next_double(seed, 0, new_slot as i64, 405 + d as i64) - 0.5) as f32) * 0.05_f32;
                    let mut_c = ((crate::prng::next_double(seed, 0, new_slot as i64, 415 + d as i64) - 0.5) as f32) * 0.05_f32;
                    genetics[d * capacity + new_slot] = (genetics[d * capacity + i] + mut_g).clamp(0.0, 1.0);
                    culture[d * capacity + new_slot] = (culture[d * capacity + i] + mut_c).clamp(0.0, 1.0);
                }
            } else {
                break;
            }
        }
    }
}
