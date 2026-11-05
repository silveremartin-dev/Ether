use crate::buffers::{AgentBufferRaw, WorldBufferRaw};
use crate::environmental::sigmoid;

#[inline(always)]
fn get_spatial_tensor_anchor(
    elevation: &[f32],
    inst_comp: &[f32],
    human_pop: &[f32],
    capital: &[f32],
    hex_idx: usize,
    dimension: usize,
) -> f32 {
    let e_val = if hex_idx < elevation.len() { elevation[hex_idx] } else { 0.0 };
    let inst = if hex_idx < inst_comp.len() { inst_comp[hex_idx] } else { 0.0 };
    let pop = if hex_idx < human_pop.len() { human_pop[hex_idx] } else { 0.0 };
    let cap = if hex_idx < capital.len() { capital[hex_idx] } else { 0.0 };
    let cap_per_cap = if pop > 0.001 { cap / pop } else { 0.0 };

    match dimension {
        0 => (0.5 + (((e_val as f64) / 3500.0).tanh() as f32) * 0.35).clamp(0.0, 1.0),
        1 => {
            let clan_persistence = 1.0 - (sigmoid(cap_per_cap as f64, 120.0, 45.0) as f32);
            (0.20 + 0.60 * clan_persistence).clamp(0.0, 1.0)
        }
        2 => {
            let rugged = sigmoid(e_val as f64, 1000.0, 333.333) as f32;
            (0.50 + 0.30 * rugged).clamp(0.0, 1.0)
        }
        3 => {
            let state_cent = inst / (15.0 + inst.max(0.0));
            (0.15 + 0.70 * state_cent).clamp(0.0, 1.0)
        }
        _ => 0.5,
    }
}

/// Langevin Cultural SDE Drift and Memetic Spatial Diffusion across 6 H3 neighbors.
/// Mathematically identical to `CultureKernel.java` (Zero state-level technology reads or writes).
pub unsafe fn process_culture(
    world: &mut WorldBufferRaw,
    agents: &mut AgentBufferRaw,
    dt_years: f32,
    diffusion_rate: f32,
    forcing_rate: f32,
) {
    process_culture_full(world, agents, dt_years, diffusion_rate, forcing_rate, 0.0, 42, 1);
}

pub unsafe fn process_culture_full(
    world: &mut WorldBufferRaw,
    agents: &mut AgentBufferRaw,
    dt_years: f32,
    diffusion_rate: f32,
    forcing_rate: f32,
    mutation_rate: f32,
    seed: i64,
    tick_counter: i64,
) {
    let a_cap = agents.capacity;
    let w_cap = world.capacity;
    if a_cap == 0 || w_cap == 0 {
        return;
    }

    let hex_ids = std::slice::from_raw_parts(agents.hex_ids, a_cap);
    let culture = std::slice::from_raw_parts_mut(agents.culture, 4 * a_cap);
    let elev = if !world.elevation.is_null() {
        std::slice::from_raw_parts(world.elevation, w_cap)
    } else {
        &[]
    };
    let inst_comp = if !world.institutional_complexity.is_null() {
        std::slice::from_raw_parts(world.institutional_complexity, w_cap)
    } else {
        &[]
    };
    let human_pop = if !world.biomass_human.is_null() {
        std::slice::from_raw_parts(world.biomass_human, w_cap)
    } else {
        &[]
    };
    let capital = if !world.resource_capital.is_null() {
        std::slice::from_raw_parts(world.resource_capital, w_cap)
    } else {
        &[]
    };
    let neighbors = if !world.neighbor_indexes.is_null() {
        std::slice::from_raw_parts(world.neighbor_indexes, w_cap * 6)
    } else {
        &[]
    };

    let dt = dt_years.max(0.0001);

    // Step 0: Langevin stochastic Wiener drift
    if mutation_rate > 0.0 {
        let noise_coeff = ((mutation_rate as f64) * (dt as f64).sqrt()) as f32;
        for i in 0..a_cap {
            if hex_ids[i] == -1 {
                continue;
            }
            for d in 0..4 {
                let gaussian_noise = crate::prng::next_gaussian(seed, tick_counter, ((i as i64) << 4) | (d as i64))
                    .clamp(-3.0, 3.0) as f32;
                let val = culture[d * a_cap + i] + gaussian_noise * noise_coeff;
                culture[d * a_cap + i] = val.clamp(0.0, 1.0);
            }
        }
    }

    // Step 1: Spatial aggregation into cells
    let mut hex_agent_count = vec![0_i32; w_cap];
    let mut hex_culture_sum = vec![[0.0_f32; 4]; w_cap];
    let mut hex_culture_mean = vec![[0.0_f32; 4]; w_cap];

    for i in 0..a_cap {
        let h = hex_ids[i];
        if h >= 0 && (h as usize) < w_cap {
            let u = h as usize;
            hex_agent_count[u] += 1;
            for d in 0..4 {
                hex_culture_sum[u][d] += culture[d * a_cap + i];
            }
        }
    }

    for u in 0..w_cap {
        let count = hex_agent_count[u];
        if count > 0 {
            let inv_count = 1.0 / (count as f32);
            for d in 0..4 {
                hex_culture_mean[u][d] = hex_culture_sum[u][d] * inv_count;
            }
        }
    }

    // Step 2: Neighbor memetic diffusion and regional forcing
    let diff_coeff = (diffusion_rate / 6.0) * dt;
    let force_coeff = forcing_rate * dt;

    for i in 0..a_cap {
        let h_idx = hex_ids[i];
        if h_idx < 0 || (h_idx as usize) >= w_cap {
            continue;
        }
        let my_hex = h_idx as usize;

        for d in 0..4 {
            let my_val = culture[d * a_cap + i];
            let mut delta = 0.0_f32;

            if !neighbors.is_empty() {
                let base_idx = my_hex * 6;
                for j in 0..6 {
                    let n_idx = neighbors[base_idx + j];
                    if n_idx >= 0 && (n_idx as usize) < w_cap {
                        let n_u = n_idx as usize;
                        let neighbor_mean = if hex_agent_count[n_u] > 0 {
                            hex_culture_mean[n_u][d]
                        } else {
                            get_spatial_tensor_anchor(elev, inst_comp, human_pop, capital, n_u, d)
                        };
                        delta += (neighbor_mean - my_val) * diff_coeff;
                    }
                }
            }

            let target_spatial_val = get_spatial_tensor_anchor(elev, inst_comp, human_pop, capital, my_hex, d);
            delta += (target_spatial_val - my_val) * force_coeff;

            culture[d * a_cap + i] = (my_val + delta).clamp(0.0, 1.0);
        }
    }
}
