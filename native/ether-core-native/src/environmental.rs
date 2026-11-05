use crate::buffers::WorldBufferRaw;
use crate::constants::*;
use rayon::prelude::*;
use std::sync::LazyLock;

const LUT_TEMP_MIN: i32 = -10;
const LUT_TEMP_STEPS: usize = 56;
const LUT_CO2_MIN: i32 = 150;
const LUT_CO2_STEP_SIZE: i32 = 25;
const LUT_CO2_STEPS: usize = 35;
const PET_LUT_MIN: i32 = -5;

const H3_AVG_AREA_KM2: [f64; 16] = [
    4357449.416, 609788.441, 86801.780, 12393.434,
    1770.347, 252.903, 36.129, 5.161,
    0.737, 0.105, 0.015, 0.0021,
    0.0003, 0.00004, 0.000006, 0.0000009,
];

#[inline(always)]
pub(crate) fn sigmoid(x: f64, x0: f64, scale: f64) -> f64 {
    let z = -((x - x0) / scale.max(1e-12));
    if z > 40.0 {
        0.0
    } else if z < -40.0 {
        1.0
    } else {
        1.0 / (1.0 + z.exp())
    }
}

#[inline(always)]
pub(crate) fn softplus(x: f64, sharpness: f64) -> f64 {
    let k = sharpness.max(1e-12);
    let kx = k * x;
    if kx > 40.0 {
        x
    } else if kx < -40.0 {
        0.0
    } else {
        (1.0 + kx.exp()).ln() / k
    }
}

#[inline(always)]
pub(crate) fn softmax(a: f64, b: f64, sharpness: f64) -> f64 {
    let k = sharpness.max(1e-12);
    let m = a.max(b);
    let diff = -k * (a - b).abs();
    if diff < -40.0 {
        m
    } else {
        m + (1.0 + diff.exp()).ln() / k
    }
}

#[inline(always)]
pub(crate) fn softmin(a: f64, b: f64, sharpness: f64) -> f64 {
    -softmax(-a, -b, sharpness)
}

#[inline(always)]
pub(crate) fn smooth_clamp(x: f64, min: f64, max: f64, sharpness: f64) -> f64 {
    softmin(max, softmax(min, x, sharpness), sharpness)
}

static FVCB_LUT: LazyLock<[[f32; LUT_CO2_STEPS]; LUT_TEMP_STEPS]> = LazyLock::new(|| {
    let mut table = [[0.0_f32; LUT_CO2_STEPS]; LUT_TEMP_STEPS];
    for t_idx in 0..LUT_TEMP_STEPS {
        let temp_c = (LUT_TEMP_MIN + t_idx as i32) as f64;
        let temp_k = temp_c + 273.15;
        let chill_activation = sigmoid(temp_c, -1.0, 1.8);
        let heat_preservation = 1.0 - sigmoid(temp_c, 39.5, 2.5);
        let inv_diff = (1.0 / 298.15) - (1.0 / temp_k);

        let vcmax = 60.0 * ((65000.0 / (R_GAS_CONSTANT as f64)) * inv_diff).exp();
        let gamma_star = 42.75 * ((37830.0 / (R_GAS_CONSTANT as f64)) * inv_diff).exp();
        let kc = 404.9 * ((79430.0 / (R_GAS_CONSTANT as f64)) * inv_diff).exp();
        let ko = 278.4 * ((36380.0 / (R_GAS_CONSTANT as f64)) * inv_diff).exp();
        let km = kc * (1.0 + 210.0 / ko);

        for c_idx in 0..LUT_CO2_STEPS {
            let co2_ppm = (LUT_CO2_MIN + c_idx as i32 * LUT_CO2_STEP_SIZE) as f64;
            let ci = co2_ppm * 0.70;
            let net_ci = softplus(ci - gamma_star, 1.0);
            let ac = vcmax * net_ci / softmax(1.0, ci + km, 2.0);
            let aj = 0.5 * vcmax * net_ci / softmax(1.0, ci + 2.0 * gamma_star, 2.0);
            let anet = softmin(ac, aj, 0.5) * chill_activation * heat_preservation;
            table[t_idx][c_idx] = ((0.05 + anet / 25.0).clamp(0.05, 1.50)) as f32;
        }
    }
    table
});

static PET_LUT: LazyLock<[f32; 111]> = LazyLock::new(|| {
    let mut table = [0.0_f32; 111];
    for i in 0..111 {
        let t = (PET_LUT_MIN as f64) + (i as f64) * 0.5;
        let delta = (4098.0 * (0.6108 * (17.27 * t / (t + 237.3)).exp())) / ((t + 237.3) * (t + 237.3));
        let gamma_psy = 0.066;
        let alpha_pt = 1.26;
        let net_solar = softmax(100.0, (t + 10.0) * 35.0, 0.05);
        let pet = alpha_pt * (delta / (delta + gamma_psy)) * net_solar;
        table[i] = pet as f32;
    }
    table
});

#[inline(always)]
fn evaluate_farquhar_yield(temp_c: f32, co2_ppm: f32) -> f32 {
    let t_pos = (temp_c - LUT_TEMP_MIN as f32).clamp(0.0, (LUT_TEMP_STEPS - 1) as f32);
    let c_pos = ((co2_ppm - LUT_CO2_MIN as f32) / (LUT_CO2_STEP_SIZE as f32)).clamp(0.0, (LUT_CO2_STEPS - 1) as f32);
    let t0 = (t_pos as usize).min(LUT_TEMP_STEPS - 2);
    let c0 = (c_pos as usize).min(LUT_CO2_STEPS - 2);
    let ft = t_pos - t0 as f32;
    let fc = c_pos - c0 as f32;
    let v00 = FVCB_LUT[t0][c0];
    let v10 = FVCB_LUT[t0 + 1][c0];
    let v01 = FVCB_LUT[t0][c0 + 1];
    let v11 = FVCB_LUT[t0 + 1][c0 + 1];
    let v0 = v00 + ft * (v10 - v00);
    let v1 = v01 + ft * (v11 - v01);
    v0 + fc * (v1 - v0)
}

#[inline(always)]
fn calculate_priestley_taylor_pet(temp_c: f32) -> f32 {
    if temp_c <= -5.0 {
        return PET_LUT[0];
    }
    if temp_c >= 50.0 {
        return PET_LUT[110];
    }
    let idx_f = (temp_c - (PET_LUT_MIN as f32)) * 2.0;
    let idx = (idx_f as usize).min(109);
    let frac = idx_f - (idx as f32);
    PET_LUT[idx] + frac * (PET_LUT[idx + 1] - PET_LUT[idx])
}

#[inline(always)]
fn calculate_miami_npp(temp_c: f32, effective_precip_mm: f32) -> f32 {
    let npp_temp = 3000.0 / (1.0 + (1.315 - 0.119 * (temp_c as f64)).exp());
    let pos_precip = softplus(effective_precip_mm as f64, 0.2);
    let npp_precip = 3000.0 * (1.0 - (-0.000664 * pos_precip).exp());
    (softmin(npp_temp, npp_precip, 0.02).clamp(15.0, 3000.0)) as f32
}

/// Complete Photosynthetic Farquhar FvCB, Lieth Miami NPP & Priestley-Taylor Agronomic Kernel.
/// Mathematically identical to `EnvironmentalKernel.java` (Zero discrete biome switches, zero technology state).
pub unsafe fn process_environmental(world: &mut WorldBufferRaw, dt_years: f32) {
    let n = world.capacity;
    if n == 0 {
        return;
    }

    let dt = dt_years.max(0.0001);
    let food_retention_factor = ((-0.03_f64 * (dt as f64)).exp()) as f32;

    let temp = std::slice::from_raw_parts(world.temperature, n);
    let rain = std::slice::from_raw_parts(world.rainfall, n);
    let biomes = std::slice::from_raw_parts(world.biomes, n);
    let elev = if !world.elevation.is_null() {
        std::slice::from_raw_parts(world.elevation, n)
    } else {
        &[]
    };
    let water = if !world.water_resource.is_null() {
        std::slice::from_raw_parts(world.water_resource, n)
    } else {
        &[]
    };
    let human_pop = if !world.biomass_human.is_null() {
        std::slice::from_raw_parts(world.biomass_human, n)
    } else {
        &[]
    };
    let cap_arr = if !world.resource_capital.is_null() {
        std::slice::from_raw_parts(world.resource_capital, n)
    } else {
        &[]
    };
    let livestock_arr = if !world.biomass_livestock.is_null() {
        std::slice::from_raw_parts(world.biomass_livestock, n)
    } else {
        &[]
    };

    let mut area_scale = 1.0_f32;
    if !world.h3_indexes.is_null() {
        let first_h3 = *world.h3_indexes as u64;
        if first_h3 != 0 {
            let res = ((first_h3 >> 52) & 0x0F) as usize;
            if res <= 15 {
                area_scale = ((H3_AVG_AREA_KM2[res] / 1770.49).clamp(0.01, 20.0)) as f32;
            }
        }
    }

    let food_ptr = world.food_resource as usize;
    let bio_nat_ptr = world.biomass_natural as usize;
    let bio_agri_ptr = world.biomass_agriculture as usize;
    let baseline_co2 = 280.0_f32;

    (0..n).into_par_iter().for_each(|i| {
        let b = biomes[i];
        if b <= 1 {
            return;
        }

        let temp_c = temp[i];
        let elevation_m = if !elev.is_empty() { elev[i] } else { 0.0 };
        let water_avail = if !water.is_empty() { water[i] } else { 0.0 };
        let effective_moisture = softmax(
            rain[i] as f64,
            softmin(1500.0, (rain[i] as f64) + (water_avail as f64) * 0.8, 0.02),
            0.05,
        ) as f32;

        let npp = calculate_miami_npp(temp_c, effective_moisture);
        let frost_factor = sigmoid(temp_c as f64, -4.0, 2.5) as f32;
        let pos_elev = elevation_m.max(0.0);
        let elev_norm = pos_elev / 1800.0;
        let slope_retention = 1.0 / (1.0 + elev_norm * elev_norm);

        let base_prod = (150.0 + npp * 1.95 * frost_factor).clamp(150.0, 3800.0) * area_scale;
        let fvcb_factor = evaluate_farquhar_yield(temp_c, baseline_co2);
        let pet = calculate_priestley_taylor_pet(temp_c);
        let moisture_aridity_ratio = smooth_clamp(
            (effective_moisture / pet.max(100.0)) as f64,
            0.40,
            1.25,
            8.0,
        ) as f32;

        let pop = if !human_pop.is_empty() { human_pop[i] } else { 0.0 };
        let capital = if !cap_arr.is_empty() { cap_arr[i] } else { 0.0 };
        let cap_per_cap = if pop > 0.001 { capital / pop } else { 0.0 };

        let bio_dom = if !livestock_arr.is_empty() { livestock_arr[i] } else { 0.0 };
        let animal_traction_ratio = if pop > 0.0 {
            (((bio_dom / (pop * 0.35 + 1.0)) as f64).tanh()) as f32
        } else {
            0.0
        };
        let fodder_factor = 1.0 - PREINDUSTRIAL_FODDER_LAND_FRACTION * animal_traction_ratio;

        let industrial_boost = 1.0 + 2.5 * (((cap_per_cap / 600.0) as f64).tanh() as f32);
        let agro_climatic_factor = smooth_clamp(
            (fvcb_factor * moisture_aridity_ratio * fodder_factor * industrial_boost) as f64,
            0.40,
            2.0,
            6.0,
        ) as f32;

        let arable_land_cap = 0.08 + 0.92 * (((cap_per_cap / 80.0) as f64).tanh() as f32);
        let base_ag_potential = (npp * 85.0 * slope_retention * frost_factor).clamp(500.0, 180000.0);
        let max_ag_potential = base_ag_potential
            * area_scale
            * agro_climatic_factor
            * arable_land_cap
            * (1.0 + 0.20 * (((1.0 + (cap_per_cap as f64) / 50.0).ln()) as f32));
        let ag_demand_potential = pop * 6.5;
        let ag_ratio = if max_ag_potential > 0.0 {
            ag_demand_potential / max_ag_potential
        } else {
            0.0
        };
        let ag_prod = if pop > 0.0 && max_ag_potential > 0.0 {
            max_ag_potential * (1.0 - ((-2.0 * ag_ratio as f64).exp() as f32)) * dt
        } else {
            0.0
        };
        let natural_prod = base_prod * agro_climatic_factor * dt;

        let growth = natural_prod + ag_prod;
        let food_val = *((food_ptr as *mut f32).add(i));
        let retained_food = food_val * food_retention_factor;
        *((food_ptr as *mut f32).add(i)) = (retained_food + growth).clamp(0.0, 50000.0 * area_scale);

        if bio_nat_ptr != 0 {
            let bio_val = *((bio_nat_ptr as *mut f32).add(i));
            let forest_affinity = sigmoid(effective_moisture as f64, 450.0, 166.67)
                * sigmoid(temp_c as f64, 2.0, 4.0);
            let residence_years = (25.0 * forest_affinity + 4.0 * (1.0 - forest_affinity)) as f32;
            let analytical_cap_tons = (10000.0 * area_scale)
                .max(npp * 1770.49 * area_scale * residence_years * 0.025);
            let forager_dep = 1.0 - (((cap_per_cap / 25.0) as f64).tanh() as f32);
            let min_forager_tons = pop * 48.0 * forager_dep;
            let nat_ceiling = bio_val.max(analytical_cap_tons.max(min_forager_tons));
            *((bio_nat_ptr as *mut f32).add(i)) = (bio_val + natural_prod * 0.5).min(nat_ceiling);
        }

        if bio_agri_ptr != 0 {
            let bio_agri_val = *((bio_agri_ptr as *mut f32).add(i));
            let cultivation_share = (((cap_per_cap / 15.0) as f64).tanh()) as f32;
            let per_capita_ag_target = pop * (250.0 + 370.0 * cultivation_share) * cultivation_share;
            let annual_ag_flux_kg = if ag_prod > 0.0 {
                per_capita_ag_target.max((ag_prod / dt.max(0.0001)) / 0.0105 * cultivation_share)
            } else {
                per_capita_ag_target
            };
            *((bio_agri_ptr as *mut f32).add(i)) = (bio_agri_val * food_retention_factor).max(annual_ag_flux_kg);
        }
    });
}
