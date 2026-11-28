use crate::buffers::WorldBufferRaw;
use crate::environmental::{sigmoid, softplus};
use rayon::prelude::*;

#[inline(always)]
fn softmin_f64(a: f64, b: f64, k: f64) -> f64 {
    let m = a.min(b);
    let diff = (a - b).abs();
    m - ((-k * diff).exp().ln_1p()) / k
}

/// Updates Physical Capital Accumulation, Agglomeration, and Tainter Institutional Entropy.
/// Mathematically identical to `UrbanKernel.java` (Single-Responsibility Solow-Swan + Lavoisier).
pub unsafe fn process_urban(world: &mut WorldBufferRaw, dt_years: f32) {
    let n = world.capacity;
    if n == 0 {
        return;
    }

    let dt_in_years = dt_years.max(0.0001) as f64;
    let decay_capital = 0.95_f64.powf(dt_in_years);
    let decay_complexity = 0.90_f64.powf(dt_in_years);

    let pop = std::slice::from_raw_parts(world.human_population, n);
    let biomes = if !world.biomes.is_null() { std::slice::from_raw_parts(world.biomes, n) } else { &[] };
    let nat = if !world.biomass_natural.is_null() { std::slice::from_raw_parts(world.biomass_natural, n) } else { &[] };
    let livestock = if !world.biomass_livestock.is_null() { std::slice::from_raw_parts(world.biomass_livestock, n) } else { &[] };
    let agri = if !world.biomass_agriculture.is_null() { std::slice::from_raw_parts(world.biomass_agriculture, n) } else { &[] };
    let e_wind = if !world.energy_wind.is_null() { std::slice::from_raw_parts(world.energy_wind, n) } else { &[] };
    let e_solar = if !world.energy_solar.is_null() { std::slice::from_raw_parts(world.energy_solar, n) } else { &[] };
    let profile_basal_w = if !world.organism_profile.is_null() {
        (*world.organism_profile).basal_power_watts
    } else {
        4.24 * 65.0_f64.powf(0.75)
    };

    let cap_ptr = world.resource_capital as usize;
    let comp_ptr = world.institutional_complexity as usize;
    let wood_ptr = world.wood_resource as usize;
    let metal_ptr = world.metal_resource as usize;
    let clay_ptr = world.clay_resource as usize;

    const WATTS_PER_GJ_PER_YEAR: f64 = 1.0e9 / (365.25 * 86_400.0);

    (0..n).into_par_iter().for_each(|i| {
        if !biomes.is_empty() && biomes[i] <= 1 {
            return;
        }
        let p = pop[i];
        let cap_val = *((cap_ptr as *mut f32).add(i));
        let comp_val = *((comp_ptr as *mut f32).add(i));

        let active_settlement = sigmoid(p as f64, 12.0, 0.5);
        if p <= 0.001 || active_settlement < 1e-6 {
            if cap_val > 0.0 {
                *((cap_ptr as *mut f32).add(i)) = ((cap_val as f64) * decay_capital).max(0.0) as f32;
            }
            if comp_val > 0.0 {
                *((comp_ptr as *mut f32).add(i)) = ((comp_val as f64) * decay_complexity).max(0.0) as f32;
            }
            return;
        }

        let capital_per_capita = (cap_val / p) as f64;
        let p_f64 = p as f64;

        // 1. Institutional Complexity C (Tainter 1988, West-Bettencourt urban scaling)
        let target_complexity = (1.0 + capital_per_capita * 0.25 + (1.0 + p_f64 * 0.005).ln()).ln();
        let new_comp = (active_settlement * target_complexity
            + (1.0 - active_settlement) * (comp_val as f64) * decay_complexity) as f32;

        // 2. Institutional Maintenance Overhead (Tainter Law of Diminishing Marginal Returns)
        let capped_comp = softmin_f64(5.0, new_comp as f64, 4.0);
        let baseline_turnover = 0.04_f64;
        let complexity_overhead = 0.0008 * capped_comp.max(0.0).powf(1.5);
        let effective_depreciation_rate = softmin_f64(0.075, baseline_turnover + complexity_overhead, 40.0);

        // 3. Smil (2017) 5-Tier Somatic + Exosomatic Power & Solow-Swan Steady-State Convergence
        let nat_val = if !nat.is_empty() { nat[i] as f64 } else { 0.0 };
        let wood_val = if wood_ptr != 0 { (*((wood_ptr as *const f32).add(i))) as f64 } else { 0.0 };
        let agri_val = if !agri.is_empty() { agri[i] as f64 } else { 0.0 };
        let live_per_cap_t = if !livestock.is_empty() { (livestock[i] as f64 / p_f64).max(0.0) } else { 0.0 };
        let kpc_pos = capital_per_capita.max(0.0);

        let wood_biomass_kg = (nat_val.max(0.0) + wood_val.max(0.0)) * 1000.0;
        let wood_thermal_w = 212.0 * ((wood_biomass_kg + 50.0) / 50.0).tanh();
        let draft_animal_w = 65.0 * (live_per_cap_t / 0.25).tanh() * sigmoid(kpc_pos, 25.0, 12.0);
        let draft_tool_w = 25.0 * (softplus(kpc_pos - 80.0, 0.05) / 180.0).tanh();
        let explicit_wind_solar_w = if !e_wind.is_empty() && !e_solar.is_empty() {
            ((e_wind[i].max(0.0) as f64 + e_solar[i].max(0.0) as f64) * WATTS_PER_GJ_PER_YEAR) / p_f64
        } else {
            0.0
        };
        let mill_and_charcoal_w = 220.0 * (softplus(kpc_pos - 320.0, 0.05) / 420.0).tanh()
            + explicit_wind_solar_w.min(180.0);
        let steam_coal_w = 600.0 * (softplus(kpc_pos - 1000.0, 0.02) / 1800.0).tanh();
        let modern_grid_w = 1350.0 * (softplus(kpc_pos - 4000.0, 0.01) / 10000.0).tanh();
        let power_per_capita_w = (profile_basal_w + 39.0 + wood_thermal_w + draft_animal_w + draft_tool_w
            + mill_and_charcoal_w + steam_coal_w + modern_grid_w) as f32 as f64;

        let mut k_star = 5.0
            + 580.0 * (softplus(power_per_capita_w - 348.0, 0.2) / 38.0).tanh()
            + 62.0 * (softplus(power_per_capita_w - 400.0, 0.1) / 100.0).powf(1.95);

        let band_constraint = sigmoid(p_f64, 50.0, 15.0);
        k_star = 5.0 * (1.0 - band_constraint) + k_star * band_constraint;

        let agri_activation = sigmoid(agri_val, 150.0, 25.0);
        let industrial_activation = sigmoid(power_per_capita_w, 520.0, 25.0);
        let sedentary_capability = agri_activation.max(industrial_activation);
        let agrarian_masonry_stock = 145.0 * (agri_val.max(0.0) / (p_f64 * 180.0 + 1.0)).tanh() * band_constraint;
        k_star = 5.0 + (k_star - 5.0 + agrarian_masonry_stock) * sedentary_capability;

        let d_capital_per_capita = (baseline_turnover * k_star - effective_depreciation_rate * capital_per_capita) * dt_in_years;
        let target_capital = (capital_per_capita + d_capital_per_capita).max(0.0) * p_f64;
        let decayed_cap = (cap_val as f64) * decay_capital;
        let next_cap = (active_settlement * target_capital + (1.0 - active_settlement) * decayed_cap) as f32;

        // Lavoisier mass conservation on net positive capital formation
        let net_added_kg = (next_cap - cap_val) as f64;
        if net_added_kg > 0.0 {
            let consumed_tonnes = net_added_kg * 1.0e-3;
            let dt_clamped = dt_in_years.clamp(0.0001, 1.0);
            if metal_ptr != 0 {
                let m_val = *((metal_ptr as *mut f32).add(i)) as f64;
                if m_val > 0.0 {
                    *((metal_ptr as *mut f32).add(i)) = (m_val - (m_val * 0.05 * dt_clamped).min(consumed_tonnes * 0.05)).max(0.0) as f32;
                }
            }
            if wood_ptr != 0 {
                let w_val = *((wood_ptr as *mut f32).add(i)) as f64;
                if w_val > 0.0 {
                    *((wood_ptr as *mut f32).add(i)) = (w_val - (w_val * 0.05 * dt_clamped).min(consumed_tonnes * 0.07)).max(0.0) as f32;
                }
            }
            if clay_ptr != 0 {
                let c_val = *((clay_ptr as *mut f32).add(i)) as f64;
                if c_val > 0.0 {
                    *((clay_ptr as *mut f32).add(i)) = (c_val - (c_val * 0.05 * dt_clamped).min(consumed_tonnes * 0.10)).max(0.0) as f32;
                }
            }
        }
        *((cap_ptr as *mut f32).add(i)) = next_cap;

        // 4. Smooth Tainter Simplification: when complexity overextends beyond physical capital, complexity contracts
        let maintenance_drag = sigmoid(new_comp as f64, 4.0, 0.8);
        *((comp_ptr as *mut f32).add(i)) =
            (new_comp as f64 * (1.0 - 0.05 * maintenance_drag * dt_in_years)) as f32;
    });
}
