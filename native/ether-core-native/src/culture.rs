use crate::buffers::{AgentBufferRaw, WorldBufferRaw};

#[inline(always)]
fn get_spatial_tensor_anchor(
    elevation: &[f32],
    tech: &[f32],
    hex_idx: usize,
    dimension: usize,
) -> f32 {
    let e_val = if hex_idx < elevation.len() { elevation[hex_idx] } else { 0.0 };
    let t_val = if hex_idx < tech.len() { tech[hex_idx] } else { 1.0 };

    match dimension {
        0 => (0.5 + (e_val / 4000.0) * 0.3).clamp(0.0, 1.0), // Isogloss
        1 => (0.8 - (t_val / 10.0) * 0.4).clamp(0.0, 1.0),   // Kinship
        2 => (0.6_f32 + if e_val > 1000.0 { 0.2_f32 } else { 0.0_f32 }).clamp(0.0, 1.0), // Rituals
        3 => ((t_val / 10.0) * 0.7 + 0.2).clamp(0.0, 1.0),    // Sovereignty
        _ => 0.5,
    }
}

/// Langevin Cultural SDE Drift and Memetic Spatial Diffusion across 6 H3 neighbors.
/// Mathematically and algorithmically identical to CultureKernel.java.
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
    if a_cap == 0 || w_cap == 0 { return; }

    let hex_ids = std::slice::from_raw_parts(agents.hex_ids, a_cap);
    let culture = std::slice::from_raw_parts_mut(agents.culture, 4 * a_cap);
    let tech = if !world.technology_level.is_null() {
        std::slice::from_raw_parts(world.technology_level, w_cap)
    } else {
        &[]
    };
    let elev = if !world.elevation.is_null() {
        std::slice::from_raw_parts(world.elevation, w_cap)
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
            if hex_ids[i] == -1 { continue; }
            for d in 0..4 {
                let gaussian_noise = crate::prng::next_gaussian(seed, tick_counter, ((i as i64) << 4) | (d as i64)).clamp(-3.0, 3.0) as f32;
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
        if h_idx < 0 || (h_idx as usize) >= w_cap { continue; }
        let my_hex = h_idx as usize;

        for d in 0..4 {
            let my_val = culture[d * a_cap + i];
            let mut delta = 0.0_f32;

            // 6-Neighbor diffusion
            if !neighbors.is_empty() {
                let base_idx = my_hex * 6;
                for j in 0..6 {
                    let n_idx = neighbors[base_idx + j];
                    if n_idx >= 0 && (n_idx as usize) < w_cap {
                        let n_u = n_idx as usize;
                        let neighbor_mean = if hex_agent_count[n_u] > 0 {
                            hex_culture_mean[n_u][d]
                        } else {
                            get_spatial_tensor_anchor(elev, tech, n_u, d)
                        };
                        delta += (neighbor_mean - my_val) * diff_coeff;
                    }
                }
            }

            // Regional tensor forcing
            let target_spatial_val = get_spatial_tensor_anchor(elev, tech, my_hex, d);
            delta += (target_spatial_val - my_val) * force_coeff;

            culture[d * a_cap + i] = (my_val + delta).clamp(0.0, 1.0);
        }
    }
}
