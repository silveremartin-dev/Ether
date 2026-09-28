use std::ffi::c_float;

/// Zero-Copy Data-Oriented World Grid Buffer (AOSOA/SoA layout).
#[repr(C)]
pub struct WorldBufferRaw {
    pub capacity: usize,
    
    // Mapping & Topology (6 neighbor indexes per hexagon)
    pub h3_indexes: *mut i64,
    pub neighbor_indexes: *mut i32, // Flattened [capacity * 6]
    
    // Terrain & Climate
    pub elevation: *mut c_float,
    pub temperature: *mut c_float,
    pub rainfall: *mut c_float,
    pub biomes: *mut u8,
    
    // Resources
    pub food_resource: *mut c_float,
    pub water_resource: *mut c_float,
    pub wood_resource: *mut c_float,
    pub metal_resource: *mut c_float,
    pub clay_resource: *mut c_float,
    
    // Biomass
    pub biomass_human: *mut c_float,
    pub biomass_livestock: *mut c_float,
    pub biomass_fish: *mut c_float,
    pub biomass_agriculture: *mut c_float,
    pub biomass_natural: *mut c_float,
    
    // Energy
    pub energy_wind: *mut c_float,
    pub energy_solar: *mut c_float,
    pub energy_fire: *mut c_float,
    pub energy_slaves: *mut c_float,
    pub energy_food_consumed: *mut c_float,
    
    // Socio-Economic
    pub lifespan: *mut c_float,
    pub fertility: *mut c_float,
    pub gini_index: *mut c_float,
    pub technology_level: *mut c_float,
    pub resource_capital: *mut c_float,
    
    // Logistics & Institutions
    pub flux_pressure: *mut c_float,
    pub local_price: *mut c_float,
    pub storage: *mut c_float,
    pub institutional_complexity: *mut c_float,
}

unsafe impl Send for WorldBufferRaw {}
unsafe impl Sync for WorldBufferRaw {}

/// High-Performance Demographic Agent Buffer (Cohorts SoA layout).
#[repr(C)]
pub struct AgentBufferRaw {
    pub capacity: usize,
    
    pub hex_ids: *mut i32,
    pub h3_indexes: *mut i64,
    
    pub mass: *mut c_float,
    pub energy: *mut c_float,
    pub sigma_cost: *mut c_float,
    pub tech_level: *mut c_float,
    pub age: *mut c_float,
    pub births: *mut c_float,
    pub deaths: *mut c_float,
    pub generation_count: *mut i32,
    
    // 4-Dimensional Genetic & Cultural Tensors (flattened [4 * capacity])
    pub genetics: *mut c_float,
    pub culture: *mut c_float,
}

unsafe impl Send for AgentBufferRaw {}
unsafe impl Sync for AgentBufferRaw {}

/// 64-byte Cache-Line Aligned Bit-Packed Demographic Cohort Arena Unit.
/// Fits exactly into a single L1 CPU cache line (64 bytes).
#[repr(C, align(64))]
#[derive(Clone, Copy, Debug)]
pub struct AgentCohortPacked {
    pub hex_id: i32,               // 4 bytes
    pub generation: i16,           // 2 bytes
    pub flags_and_status: u16,     // 2 bytes (bit-packed active/migrating/crisis)
    pub mass: f32,                 // 4 bytes
    pub energy: f32,               // 4 bytes
    pub sigma_cost: f32,           // 4 bytes
    pub tech_level: f32,           // 4 bytes
    pub age_years: f32,            // 4 bytes
    pub births: f32,               // 4 bytes
    pub deaths: f32,               // 4 bytes
    pub genetics_quantized: [u8; 4], // 4 bytes (8-bit fixed point [0, 1])
    pub culture_quantized: [u8; 4],  // 4 bytes (8-bit fixed point [0, 1])
    pub padding: [u8; 20],         // 20 bytes -> Exactly 64 bytes total
}

/// Pre-computed CSR Sparse Topology & Spherical Distance Look-Up Table (LUT).
#[repr(C)]
pub struct SparseTopologyLUT {
    pub num_cells: usize,
    pub neighbor_indices: *const i32,    // [num_cells * 6]
    pub geodesic_distances: *const f32,  // [num_cells * 6]
    pub coriolis_factors: *const f32,    // [num_cells]
    pub area_weights: *const f32,        // [num_cells]
}

/// 64-bit Bitset Ring-Buffer for 6-Directional Hexagonal Logistics & Flux Routing.
/// [6 bits: active directions | 6x8 = 48 bits: quantized flux magnitude | 10 bits: flags]
#[repr(C)]
#[derive(Clone, Copy, Debug, Default)]
pub struct TransportFluxBitset(pub u64);

impl TransportFluxBitset {
    #[inline(always)]
    pub fn is_active_direction(&self, dir: usize) -> bool {
        (self.0 & (1 << (dir & 0x7))) != 0
    }

    #[inline(always)]
    pub fn set_direction_active(&mut self, dir: usize, active: bool) {
        if active {
            self.0 |= 1 << (dir & 0x7);
        } else {
            self.0 &= !(1 << (dir & 0x7));
        }
    }

    #[inline(always)]
    pub fn get_flux_magnitude(&self, dir: usize) -> u8 {
        ((self.0 >> (6 + (dir * 8))) & 0xFF) as u8
    }

    #[inline(always)]
    pub fn set_flux_magnitude(&mut self, dir: usize, val: u8) {
        let shift = 6 + (dir * 8);
        self.0 = (self.0 & !(0xFF_u64 << shift)) | ((val as u64) << shift);
    }
}

