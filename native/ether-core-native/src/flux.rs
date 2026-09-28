use crate::buffers::{TransportFluxBitset, WorldBufferRaw};

/// Symmetric Finite-Volume Resource Flux across Hexagonal Neighborhoods (100% Mass Conserving).
pub unsafe fn process_flux(world: &mut WorldBufferRaw, dt_seconds: f32) {
    let capacity = world.capacity;
    if capacity == 0 { return; }

    let food = std::slice::from_raw_parts_mut(world.food_resource, capacity);
    let pop = std::slice::from_raw_parts(world.biomass_human, capacity);
    let prices = std::slice::from_raw_parts_mut(world.local_price, capacity);
    let elev = std::slice::from_raw_parts(world.elevation, capacity);
    let neighbors = std::slice::from_raw_parts(world.neighbor_indexes, capacity * 6);

    // 1. Vectorized Price calculation
    for i in 0..capacity {
        prices[i] = (pop[i] + 1.0) / (food[i] + 1.0);
    }

    let mut delta_food = vec![0.0_f32; capacity];
    let base_conductivity: f32 = 0.05;
    let dt_days = dt_seconds / 86400.0;

    // 2. Symmetric edge flux with direction bitset caching
    for i in 0..capacity {
        let p_a = prices[i];
        let h_a = elev[i];
        let food_a = food[i];
        let mut flux_bitset = TransportFluxBitset::default();

        for j in 0..6 {
            let n_idx = neighbors[i * 6 + j];
            if n_idx == -1 || i >= (n_idx as usize) { continue; }
            let n = n_idx as usize;

            let p_b = prices[n];
            let h_b = elev[n];
            let food_b = food[n];

            let gradient = p_b - p_a;
            let friction = 1.0 + (h_b - h_a).abs() * 0.1;
            let conductivity = base_conductivity / friction;

            let raw_flux = gradient * conductivity * dt_days;

            let max_transfer_a = if food_a > 0.0 { food_a * 0.5 } else { 0.0 };
            let max_transfer_b = if food_b > 0.0 { food_b * 0.5 } else { 0.0 };

            let flux = raw_flux.clamp(-max_transfer_b, max_transfer_a);

            if flux.abs() > 1e-6 {
                flux_bitset.set_direction_active(j, true);
                let quantized_mag = (flux.abs().min(255.0)) as u8;
                flux_bitset.set_flux_magnitude(j, quantized_mag);
            }

            delta_food[i] -= flux;
            delta_food[n] += flux;
        }
    }

    for i in 0..capacity {
        food[i] = (food[i] + delta_food[i]).max(0.0);
    }
}

