use crate::buffers::WorldBufferRaw;
use crate::environmental::{sigmoid, softplus};
use rayon::prelude::*;

/// Updates Physical Capital Accumulation, Agglomeration, and Tainter Institutional Entropy.
/// Mathematically identical to `UrbanKernel.java` (Zero state-level technology reads or writes).
pub unsafe fn process_urban(world: &mut WorldBufferRaw, dt_years: f32) {
    let n = world.capacity;
    if n == 0 {
        return;
    }

    let dt_in_years = dt_years.max(0.0001) as f64;
    let decay_capital = 0.95_f64.powf(dt_in_years);
    let decay_complexity = 0.90_f64.powf(dt_in_years);

    let pop = std::slice::from_raw_parts(world.biomass_human, n);
    let biomes = if !world.biomes.is_null() {
        std::slice::from_raw_parts(world.biomes, n)
    } else {
        &[]
    };
    let nat = if !world.biomass_natural.is_null() {
        std::slice::from_raw_parts(world.biomass_natural, n)
    } else {
        &[]
    };
    let wood = if !world.wood_resource.is_null() {
        std::slice::from_raw_parts(world.wood_resource, n)
    } else {
        &[]
    };
    let agri = if !world.biomass_agriculture.is_null() {
        std::slice::from_raw_parts(world.biomass_agriculture, n)
    } else {
        &[]
    };

    let cap_ptr = world.resource_capital as usize;
    let comp_ptr = world.institutional_complexity as usize;

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
            + (1.0 - active_settlement) * (comp_val as f64) * (decay_complexity as f64)) as f32;

        // 2. Physical Capital Steady-State Convergence (Solow-Swan mechanical power constraint, Solow 1956, Garrett 2011, Krausmann et al. 2017)
        let nat_val = if !nat.is_empty() { nat[i] } else { 0.0 };
        let wood_val = if !wood.is_empty() { wood[i] } else { 0.0 };
        let agri_val = if !agri.is_empty() { agri[i] } else { 0.0 };

        let basal_w = 4.24 * 65.0_f64.powf(0.75); // Matches OrganismBiophysicalProfile.HOMO_SAPIENS (97.06238 W)
        let wood_thermal_w = 212.0 * ((nat_val as f64 + wood_val as f64 + 50.0) / 50.0).tanh();
        let draft_w = 25.0 * (softplus(capital_per_capita - 150.0, 0.05) / 250.0).tanh();
        let mill_and_charcoal_w = 220.0 * (softplus(capital_per_capita - 520.0, 0.05) / 420.0).tanh();
        let steam_coal_w = 600.0 * (softplus(capital_per_capita - 1000.0, 0.02) / 1800.0).tanh();
        let modern_grid_w = 1350.0 * (softplus(capital_per_capita - 4000.0, 0.01) / 10000.0).tanh();
        let power_per_capita_w = (basal_w + 39.0 + wood_thermal_w + draft_w + mill_and_charcoal_w + steam_coal_w + modern_grid_w) as f32 as f64;

        let mut k_star = 5.0
            + 580.0 * (softplus(power_per_capita_w - 348.0, 0.2) / 38.0).tanh()
            + 62.0 * (softplus(power_per_capita_w - 400.0, 0.1) / 100.0).powf(1.95);

        let band_constraint = sigmoid(p_f64, 50.0, 15.0);
        k_star = 5.0 * (1.0 - band_constraint) + k_star * band_constraint;

        let agri_activation = sigmoid(agri_val as f64, 100.0, 30.0);
        let industrial_activation = sigmoid(power_per_capita_w, 480.0, 80.0);
        let sedentary_capability = agri_activation.max(industrial_activation);
        k_star = 5.0 + (k_star - 5.0) * sedentary_capability;

        let delta_annual = 0.04_f64;
        let d_capital_per_capita = delta_annual * (k_star - capital_per_capita) * (dt_in_years as f64);
        let target_capital = (capital_per_capita + d_capital_per_capita).max(0.0) * p_f64;
        let decayed_cap = (cap_val as f64) * (decay_capital as f64);
        *((cap_ptr as *mut f32).add(i)) =
            (active_settlement * target_capital + (1.0 - active_settlement) * decayed_cap) as f32;

        // 3. Smooth Tainter Simplification: when complexity overextends beyond physical capital, complexity contracts
        let maintenance_drag = sigmoid(new_comp as f64, 4.0, 0.8);
        *((comp_ptr as *mut f32).add(i)) =
            (new_comp as f64 * (1.0 - 0.05 * maintenance_drag * (dt_in_years as f64))) as f32;
    });
}
