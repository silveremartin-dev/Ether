use crate::buffers::WorldBufferRaw;
use crate::constants::*;
use rayon::prelude::*;

/// Updates Capital Accumulation, Agglomeration, and Tainter Institutional Entropy.
/// Mathematically and bitwise identical to UrbanKernel.java.
pub unsafe fn process_urban(world: &mut WorldBufferRaw, dt_years: f32) {
    let n = world.capacity;
    if n == 0 { return; }

    let dt_in_years = dt_years.max(0.0001);
    let decay_capital = (0.95_f64.powf(dt_in_years as f64)) as f32;
    let decay_complexity = (0.90_f64.powf(dt_in_years as f64)) as f32;

    let pop = std::slice::from_raw_parts(world.biomass_human, n);
    let prices = if !world.local_price.is_null() {
        std::slice::from_raw_parts(world.local_price, n)
    } else {
        &[]
    };

    let cap_ptr = world.resource_capital as usize;
    let comp_ptr = world.institutional_complexity as usize;
    let tech_ptr = world.technology_level as usize;

    (0..n).into_par_iter().for_each(|i| {
        let p = pop[i];
        let cap_val = *( (cap_ptr as *mut f32).add(i) );
        let comp_val = *( (comp_ptr as *mut f32).add(i) );

        if p > 10.0 {
            let effective_tech = if tech_ptr != 0 {
                let t = *( (tech_ptr as *mut f32).add(i) );
                t.max(0.5)
            } else {
                1.0
            };
            let capital_per_capita = if p > 0.001 { cap_val / p } else { 0.0 };

            // 1. Institutional Complexity C (Tainter 1988, West-Bettencourt urban scaling):
            // complexity[i] = (float) Math.log1p(capitalPerCapita * 0.25f + Math.log1p(pop[i] * 0.005f));
            let inner_arg = (capital_per_capita as f64) * 0.25 + (1.0 + (p as f64) * 0.005).ln();
            let mut new_comp = ((1.0 + inner_arg).ln()) as f32;
            *( (comp_ptr as *mut f32).add(i) ) = new_comp;

            // 2. Institutional Maintenance Overhead:
            let baseline_depreciation = 0.02_f32;
            let complexity_overhead = 0.012_f32 * ((new_comp as f64).powf(TAINTER_COMPLEXITY_EXPONENT as f64) as f32);
            let intensive_maintenance_drag = 0.0015_f32 * (capital_per_capita / effective_tech.max(1.0));
            let depreciation_rate = (baseline_depreciation + complexity_overhead + intensive_maintenance_drag).min(0.85);
            let maintenance_cost = cap_val * depreciation_rate;

            // 3. Gross Capital Formation:
            let price_factor = if !prices.is_empty() { prices[i].clamp(0.5, 2.0) } else { 1.0 };
            let surplus_coeff = if effective_tech < 3.0 { 0.06_f32 } else { 0.12_f32 };
            let gross_prod = price_factor * p * surplus_coeff * ((effective_tech as f64).powf(0.85) as f32);
            let net_acc = (gross_prod - maintenance_cost) * dt_in_years;

            let updated_cap = (cap_val + net_acc).max(0.0);
            *( (cap_ptr as *mut f32).add(i) ) = updated_cap;

            // 4. Endogenous Urban Agglomeration Innovation:
            if p >= 250.0 && capital_per_capita > 10.0 && tech_ptr != 0 {
                let urban_scale_factor = (((p as f64 / 100.0).log10()).min(2.0)) as f32;
                let intensive_surplus = ((1.0 + (capital_per_capita as f64 - 10.0)).ln()) as f32;
                let knowledge_burden = 1.0 + effective_tech * effective_tech * 0.5;
                let annual_tech_gain = (0.0015 * urban_scale_factor * intensive_surplus) / knowledge_burden;
                let tech_val = *( (tech_ptr as *mut f32).add(i) );
                *( (tech_ptr as *mut f32).add(i) ) = tech_val + annual_tech_gain * dt_in_years;
            }

            // 5. Tainter Simplification:
            if maintenance_cost > gross_prod {
                new_comp *= decay_capital;
                *( (comp_ptr as *mut f32).add(i) ) = new_comp;
            }
        } else {
            if cap_val > 0.0 {
                *( (cap_ptr as *mut f32).add(i) ) = (cap_val * decay_capital).max(0.0);
            }
            if comp_val > 0.0 {
                *( (comp_ptr as *mut f32).add(i) ) = (comp_val * decay_complexity).max(0.0);
            }
        }
    });
}
