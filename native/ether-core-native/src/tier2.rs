use crate::buffers::WorldBufferRaw;
use crate::environmental::{sigmoid, softplus};
use rayon::prelude::*;

/// Advances Universal Tier 2 Cliodynamics, Macroeconomics, and Epidemiological Grids
/// directly over the Data-Oriented WorldBuffer.
///
/// Implements cross-epoch literature foundations:
/// 1. Kenneth Pomeranz (2000) Great Divergence: coal proximity + transoceanic commercial flux relief.
/// 2. Peter Turchin & Jack Goldstone (1991, 2016) Structural-Demographic Theory (SDT):
///    political stress index (PSI), elite overproduction, mass immiseration, and institutional decay.
/// 3. David Cutler & Grant Miller (2005) / Abdel Omran (1971):
///    urban crowding pathogen pressure mitigated by municipal sanitation infrastructure capital.
/// 4. Thomas Piketty (2014) & Branko Milanovic (2011):
///    Inequality Possibility Frontier (IPF) and dynamic r > g capital concentration ODE.
/// 5. Reiner Kümmel (2011) & Robert Ayres (2005):
///    Second-law thermodynamic Carnot efficiency bounded by combustion flame temperatures.
pub unsafe fn process_tier2(world: &mut WorldBufferRaw, dt_years: f32) {
    let n = world.capacity;
    if n == 0 {
        return;
    }

    let dt = dt_years.max(0.0001) as f64;

    let pop_slice = std::slice::from_raw_parts(world.biomass_human, n);
    let cap_ptr = world.resource_capital as usize;
    let comp_ptr = world.institutional_complexity as usize;
    let gini_ptr = world.gini_index as usize;
    let life_ptr = world.lifespan as usize;

    let coal_slice = if !world.resource_coal.is_null() {
        std::slice::from_raw_parts(world.resource_coal, n)
    } else {
        &[]
    };
    let oil_slice = if !world.resource_oil.is_null() {
        std::slice::from_raw_parts(world.resource_oil, n)
    } else {
        &[]
    };
    let gas_slice = if !world.resource_gas.is_null() {
        std::slice::from_raw_parts(world.resource_gas, n)
    } else {
        &[]
    };
    let comm_slice = if !world.commercial_flux_volume.is_null() {
        std::slice::from_raw_parts(world.commercial_flux_volume, n)
    } else {
        &[]
    };
    let water_slice = if !world.water_resource.is_null() {
        std::slice::from_raw_parts(world.water_resource, n)
    } else {
        &[]
    };
    let food_slice = if !world.food_resource.is_null() {
        std::slice::from_raw_parts(world.food_resource, n)
    } else {
        &[]
    };
    let temp_slice = if !world.temperature.is_null() {
        std::slice::from_raw_parts(world.temperature, n)
    } else {
        &[]
    };
    let agri_slice = if !world.biomass_agriculture.is_null() {
        std::slice::from_raw_parts(world.biomass_agriculture, n)
    } else {
        &[]
    };
    let livestock_slice = if !world.biomass_livestock.is_null() {
        std::slice::from_raw_parts(world.biomass_livestock, n)
    } else {
        &[]
    };
    let fire_slice = if !world.energy_fire.is_null() {
        std::slice::from_raw_parts(world.energy_fire, n)
    } else {
        &[]
    };
    let wind_slice = if !world.energy_wind.is_null() {
        std::slice::from_raw_parts(world.energy_wind, n)
    } else {
        &[]
    };
    let solar_slice = if !world.energy_solar.is_null() {
        std::slice::from_raw_parts(world.energy_solar, n)
    } else {
        &[]
    };
    let efood_slice = if !world.energy_food_consumed.is_null() {
        std::slice::from_raw_parts(world.energy_food_consumed, n)
    } else {
        &[]
    };
    let flux_ptr = world.flux_pressure as usize;

    (0..n).into_par_iter().for_each(|i| {
        let p = pop_slice[i] as f64;
        if p <= 0.001 {
            return;
        }

        let mut capital = *((cap_ptr as *mut f32).add(i)) as f64;
        let mut complexity = *((comp_ptr as *mut f32).add(i)) as f64;
        let mut gini = *((gini_ptr as *mut f32).add(i)) as f64;
        let mut lifespan = *((life_ptr as *mut f32).add(i)) as f64;
        if gini <= 0.01 {
            gini = 0.35;
        }
        if lifespan <= 5.0 {
            lifespan = 35.0;
        }

        let k_pc = capital / p;

        // -----------------------------------------------------------------
        // 1. Cutler-Miller (2005) & Omran (1971): Urban Pathogen Sanitation
        // -----------------------------------------------------------------
        let cell_area_km2 = 36.13_f64;
        let density = (p / 0.06) / cell_area_km2; // Convert somatic mass to headcount
        let crowding_pathogen = (density / 200.0).min(1.0);
        let sanitation_coverage = if k_pc > 200.0 {
            ((k_pc - 200.0) / 2000.0).min(0.98)
        } else {
            0.0
        };
        let water_avail = if !water_slice.is_empty() { water_slice[i] as f64 } else { 1.0 };
        let target_lifespan = 30.0 + 48.0 * sanitation_coverage * water_avail.min(1.0)
            - 8.0 * crowding_pathogen * (1.0 - sanitation_coverage);
        lifespan += (target_lifespan - lifespan) * (dt * 0.1).min(1.0);
        lifespan = lifespan.clamp(22.0, 85.0);

        // -----------------------------------------------------------------
        // 2. Milanovic (2011) & Piketty (2014): Dynamic Inequality Possibility Frontier
        // -----------------------------------------------------------------
        let s_min = 15.0_f64;
        let g_max = (1.0 - s_min / (s_min + 1.0).max(k_pc)).clamp(0.0, 0.85);
        let r_minus_g = 0.05 - 0.02 * (k_pc / 1000.0).tanh();
        let mu_extract = 0.20 + 0.70 * sigmoid(r_minus_g, 0.0, 0.02) * sigmoid(complexity, 0.25, 0.10);
        let target_gini = (0.20 + mu_extract * g_max).clamp(0.20, 0.75);
        gini += 0.04 * (target_gini - gini) * dt;
        gini = gini.clamp(0.18, 0.80);

        // -----------------------------------------------------------------
        // 3. Goldstone & Turchin (2016) Structural-Demographic Theory (SDT)
        // -----------------------------------------------------------------
        if p >= 5.0 {
            let food_val = if !food_slice.is_empty() { food_slice[i] as f64 } else { 500.0 };
            let food_per_cap = food_val / (p / 0.06).max(1.0);
            let mmp = (1.9 / food_per_cap.max(0.2)) * (density / 150.0).tanh();
            let emp = ((gini - 0.25).max(0.0) / 0.20).powi(2);
            let sf = 0.4 * (1.9 / food_per_cap.max(0.2)) * (k_pc / 800.0).tanh();
            let psi = mmp.max(0.2) * emp.max(0.2) * sf.max(0.2);

            let psi_stress = (psi * psi) / (25.0 + psi * psi);
            let unrest_damage = capital * 0.08 * psi_stress * dt;
            capital = (capital - unrest_damage).max(5.0);

            let purge = psi.powi(3) / (200.0 + psi.powi(3));
            gini = (gini - 0.04 * purge * (gini - 0.25) * dt).max(0.25);
            complexity = (complexity * (1.0 - 0.05 * psi_stress * dt)).max(0.0);
        }

        // -----------------------------------------------------------------
        // 4. Kenneth Pomeranz (2000) Great Divergence & Mineral Proximity
        // -----------------------------------------------------------------
        if k_pc >= 50.0 {
            let coal = if !coal_slice.is_empty() { coal_slice[i] as f64 } else { 0.0 };
            let trade = if !comm_slice.is_empty() { comm_slice[i] as f64 } else { 0.0 };
            let coal_avail = sigmoid(coal, 500.0, 100.0);
            let friction_relief = 1.0 - sigmoid(0.50, 0.70, 0.15); // Navigable water proximity
            let coal_proximity = coal_avail * friction_relief;
            let ghost_acreage = sigmoid(trade, 25.0, 5.0) * sigmoid(k_pc, 100.0, 30.0);
            let divergence_pot = coal_proximity * (1.0 + 0.80 * ghost_acreage);

            if divergence_pot > 0.15 {
                let takeoff = 0.055 * divergence_pot * dt;
                capital *= 1.0 + takeoff;
            } else if k_pc >= 80.0 && p >= 10.0 {
                let involution_drag = 0.015 * dt;
                capital = (capital * (1.0 - involution_drag * 0.40)).max(10.0);
            }
        }

        // -----------------------------------------------------------------
        // 5. Kümmel-Ayres Second-Law Carnot Exergy Production Function
        // -----------------------------------------------------------------
        let temp_c = if !temp_slice.is_empty() { temp_slice[i] as f64 } else { 15.0 };
        let t_cold_k = temp_c + 273.15;
        let coal_val = if !coal_slice.is_empty() { coal_slice[i] as f64 } else { 0.0 };
        let hydro_val = (if !oil_slice.is_empty() { oil_slice[i] as f64 } else { 0.0 })
            + (if !gas_slice.is_empty() { gas_slice[i] as f64 } else { 0.0 });

        let delta_t_flame = 450.0 + 700.0 * sigmoid(coal_val, 100.0, 30.0) + 500.0 * sigmoid(hydro_val, 100.0, 30.0);
        let t_hot_k = t_cold_k + delta_t_flame;
        let eta_carnot = 1.0 - (t_cold_k / t_hot_k);
        let recovery = 0.08 + (0.58 - 0.08) * sigmoid(k_pc, 1200.0, 400.0);
        let eta_exergy = (eta_carnot * recovery).clamp(0.05, 0.50);

        let primary_watts = 100.0 * p + 50.0 * capital;
        let useful_exergy = softplus(primary_watts * eta_exergy, 1.0);
        let labor = p * 0.5;
        let output = capital.powf(0.25) * labor.powf(0.25) * useful_exergy.powf(0.50);
        let reinvest = output * 0.05 * dt;
        let depr = capital * 0.04 * dt;
        capital = (capital + reinvest - depr).max(1.0);

        // -----------------------------------------------------------------
        // 6. Tainter (1988) Diminishing Marginal Returns on Complexity
        // -----------------------------------------------------------------
        let headcount = (p / 0.06).max(1.0);
        if capital >= 50.0 {
            let k_per_cap_t = capital / headcount;
            let surplus_flow = headcount * (0.05 + 0.020 * k_per_cap_t) * dt;
            let t_comp = (1.0 + 0.02 * k_per_cap_t).ln();
            let maint_cost = (0.015 * capital + headcount * t_comp.powf(2.2) * 0.08) * dt;
            if maint_cost > surplus_flow {
                let erosion = (maint_cost - surplus_flow) * 0.8;
                capital = (capital - erosion).max(10.0);
                complexity = (complexity - 0.01 * (erosion / capital.max(1.0)).min(0.5)).max(0.0);
            }
        }

        // -----------------------------------------------------------------
        // 7. Ricardo (1817) Differential Land Rent & White (1949) Exosomatic Energy
        // -----------------------------------------------------------------
        if headcount > 20.0 && !agri_slice.is_empty() {
            let agri = agri_slice[i] as f64;
            if agri > 50.0 && k_pc >= 15.0 {
                let margin_pressure = (headcount * 1.20) / (agri + 1.0);
                let margin_factor = sigmoid(margin_pressure - 1.0, 0.0, 0.25);
                if margin_factor > 0.05 {
                    capital = (capital * (1.0 - 0.035 * margin_factor * dt)).max(10.0);
                    gini = (gini + 0.02 * margin_factor * dt).min(0.85);
                }
            }
        }
        let e_fire = if !fire_slice.is_empty() { fire_slice[i] as f64 } else { 0.0 };
        let e_wind = if !wind_slice.is_empty() { wind_slice[i] as f64 } else { 0.0 };
        let e_solar = if !solar_slice.is_empty() { solar_slice[i] as f64 } else { 0.0 };
        let e_food = if !efood_slice.is_empty() { (efood_slice[i] as f64).max(3.5 * headcount) } else { 3.5 * headcount };
        let e_livestock = if !livestock_slice.is_empty() { livestock_slice[i] as f64 * 15.0 } else { 0.0 };
        let e_total = e_food + e_fire + e_wind + e_solar + e_livestock + coal_val * 24.0 + hydro_val * 33.0;
        let e_pc = e_total / headcount;
        capital += headcount * (e_pc / 50.0) * 0.005 * dt;

        // -----------------------------------------------------------------
        // 8. Schelling-Axelrod (1971, 1997) Polarization Friction
        // -----------------------------------------------------------------
        if flux_ptr != 0 {
            let pol_act = sigmoid(gini, 0.45, 1.0 / 12.0);
            if pol_act > 0.02 {
                let fp = *((flux_ptr as *mut f32).add(i)) as f64;
                let df = 0.05 * pol_act * (gini - 0.35).max(0.0) * dt;
                *((flux_ptr as *mut f32).add(i)) = (fp + df).min(10.0) as f32;
            }
        }

        // Write back mutated state observables
        *((cap_ptr as *mut f32).add(i)) = capital as f32;
        *((comp_ptr as *mut f32).add(i)) = complexity as f32;
        *((gini_ptr as *mut f32).add(i)) = gini as f32;
        *((life_ptr as *mut f32).add(i)) = lifespan as f32;
    });
}
