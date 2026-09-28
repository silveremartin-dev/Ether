use crate::buffers::{AgentBufferRaw, WorldBufferRaw};

/// Langevin Cultural SDE Drift and Memetic Spatial Diffusion.
pub unsafe fn process_culture(
    world: &mut WorldBufferRaw,
    agents: &mut AgentBufferRaw,
    dt_years: f32,
    _diffusion_rate: f32,
    forcing_rate: f32,
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
    let elev = std::slice::from_raw_parts(world.elevation, w_cap);

    // 1. Spatial Tensor Anchoring & Drift
    for i in 0..a_cap {
        let h_idx = hex_ids[i];
        if h_idx == -1 || (h_idx as usize) >= w_cap { continue; }
        let h = h_idx as usize;

        let t_val = if !tech.is_empty() { tech[h] } else { 0.0 };
        let e_val = elev[h];

        let anchors = [
            (0.5 + (e_val / 4000.0) * 0.3).clamp(0.0, 1.0), // Isoglosse
            (0.8 - (t_val / 10.0) * 0.4).clamp(0.0, 1.0),   // Kinship
            if e_val > 1000.0 { 0.8 } else { 0.6 },          // Rituals
            ((t_val / 10.0) * 0.7 + 0.2).clamp(0.0, 1.0),    // Sovereignty
        ];

        for d in 0..4 {
            let current = culture[d * a_cap + i];
            let diff = anchors[d] - current;
            let updated = current + diff * forcing_rate * dt_years;
            culture[d * a_cap + i] = updated.clamp(0.0, 1.0);
        }
    }
}
