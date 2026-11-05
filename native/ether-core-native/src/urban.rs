use crate::buffers::WorldBufferRaw;
use crate::environmental::{sigmoid, smooth_clamp, softmax, softmin, softplus};
use rayon::prelude::*;

/// Updates Physical Capital Accumulation, Agglomeration, and Tainter Institutional Entropy.
/// Mathematically identical to `UrbanKernel.java` (Zero state-level technology reads or writes).
pub unsafe fn process_urban(world: &mut WorldBufferRaw, dt_years: f32) {
    let n = world.capacity;
    if n == 0 {
        return;
    }

    let dt_in_years = dt_years.max(0.0001);
    let decay_capital = (0.95_f64.powf(dt_in_years as f64)) as f32;
    let decay_complexity = (0.90_f64.powf(dt_in_years as f64)) as f32;

    let pop = std::slice::from_raw_parts(world.biomass_human, n);
    let biomes = if !world.biomes.is_null() {
        std::slice::from_raw_parts(world.biomes, n)
    } else {
        &[]
    };
    let prices = if !world.local_price.is_null() {
        std::slice::from_raw_parts(world.local_price, n)
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
                *((cap_ptr as *mut f32).add(i)) = (cap_val * decay_capital).max(0.0);
            }
            if comp_val > 0.0 {
                *((comp_ptr as *mut f32).add(i)) = (comp_val * decay_complexity).max(0.0);
            }
            return;
        }

        let capital_per_capita = (cap_val / p) as f64;
        let p_f64 = p as f64;

        // 1. Institutional Complexity C (Tainter 1988, West-Bettencourt urban scaling)
        let target_complexity = (1.0 + capital_per_capita * 0.25 + (1.0 + p_f64 * 0.005).ln()).ln();
        let new_comp = (active_settlement * target_complexity
            + (1.0 - active_settlement) * (comp_val as f64) * (decay_complexity as f64)) as f32;

        // 2. Institutional Maintenance Overhead
        let capped_comp = softmin(5.0, new_comp as f64, 4.0);
        let baseline_depreciation = 0.02_f64;
        let complexity_overhead = 0.001_f64 * capped_comp.max(0.0).powf(1.5);
        let depreciation_rate = softmin(0.08, baseline_depreciation + complexity_overhead, 40.0);

        // 3. Gross Physical Capital Formation
        let price_raw = if !prices.is_empty() && prices[i] > 0.01 {
            prices[i] as f64
        } else {
            1.0
        };
        let price_factor = smooth_clamp(price_raw, 0.95, 1.15, 25.0);
        let intensive_kpc = softmax(1.0, capital_per_capita, 2.0);
        let cobb_douglas_out = 12.0
            * price_factor
            * intensive_kpc.powf(0.30)
            * (1.0 + 0.25 * (capital_per_capita / 150.0).tanh());
        let agglomeration_bonus = 1.0 + 0.15 * ((1.0 + p_f64 / 250.0).ln() * 0.35).tanh();
        let savings_rate = 0.08 + 0.06 * (capital_per_capita / 300.0).tanh();
        let gross_investment = p_f64 * savings_rate * cobb_douglas_out * agglomeration_bonus;

        // 4. Smooth Complexity Overextension & Conflict Drag
        let excess_comp = softplus((new_comp as f64) - 3.8, 8.0);
        let war_damage_rate = 0.012 * excess_comp;
        let total_depreciation_rate = softmin(0.09, depreciation_rate + war_damage_rate, 40.0);
        let actual_maintenance_cost = (cap_val as f64) * total_depreciation_rate;

        let raw_net_acc = (gross_investment - actual_maintenance_cost) * (dt_in_years as f64);
        let war_drag = softmin(0.016, 0.008 * excess_comp, 100.0);
        let max_annual_rate = softmax(0.004, 0.020 - war_drag, 200.0);
        let max_growth_cap = max_annual_rate * (cap_val as f64) * (dt_in_years as f64);
        let bounded_net_acc = softmin(raw_net_acc, max_growth_cap, 10.0);

        let active_updated_cap = ((cap_val as f64) + bounded_net_acc).max(0.0);
        let decayed_cap = (cap_val as f64) * (decay_capital as f64);
        *((cap_ptr as *mut f32).add(i)) =
            (active_settlement * active_updated_cap + (1.0 - active_settlement) * decayed_cap) as f32;

        // 5. Smooth Tainter Simplification
        let deficit_share = sigmoid(
            actual_maintenance_cost - gross_investment,
            0.0,
            (0.1 * gross_investment + 0.1).max(1.0),
        );
        *((comp_ptr as *mut f32).add(i)) =
            ((new_comp as f64) * (1.0 - deficit_share * (1.0 - decay_capital as f64))) as f32;
    });
}
