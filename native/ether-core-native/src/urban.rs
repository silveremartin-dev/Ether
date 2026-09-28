use crate::buffers::WorldBufferRaw;
use crate::constants::*;
use rayon::prelude::*;

/// Updates Capital Accumulation, Agglomeration, and Tainter Institutional Entropy.
pub unsafe fn process_urban(world: &mut WorldBufferRaw, dt_years: f32) {
    let n = world.capacity;
    if n == 0 { return; }

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
            // 1. Institutional complexity logarithmic scaling
            let new_comp = (1.0 + cap_val * 0.05).ln();
            *( (comp_ptr as *mut f32).add(i) ) = new_comp;

            // 2. Super-linear Tainter coordination entropy: Sigma = k * C^1.15
            let sigma = new_comp.powf(TAINTER_COMPLEXITY_EXPONENT) * 50.0;

            // 3. Gross production
            let price_factor = if !prices.is_empty() { prices[i].max(0.1) } else { 1.0 };
            let gross_prod = price_factor * p * 0.05;
            let net_acc = (gross_prod - sigma) * dt_years;

            let updated_cap = (cap_val + net_acc).max(0.0);
            *( (cap_ptr as *mut f32).add(i) ) = updated_cap;

            // 4. Agglomeration & Romer technological spillover
            if updated_cap > 50.0 && tech_ptr != 0 {
                let tech_val = *( (tech_ptr as *mut f32).add(i) );
                *( (tech_ptr as *mut f32).add(i) ) = tech_val + updated_cap.sqrt() * 0.0005 * dt_years;
            }

            // 5. Tainter Institutional Collapse threshold
            if sigma > gross_prod {
                *( (comp_ptr as *mut f32).add(i) ) = new_comp * (0.95_f32).powf(dt_years);
            }
        } else {
            *( (cap_ptr as *mut f32).add(i) ) = (cap_val * (0.95_f32).powf(dt_years)).max(0.0);
            *( (comp_ptr as *mut f32).add(i) ) = (comp_val * (0.90_f32).powf(dt_years)).max(0.0);
        }
    });
}
