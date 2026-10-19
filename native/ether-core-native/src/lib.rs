pub mod buffers;
pub mod constants;
pub mod culture;
pub mod demographic;
pub mod environmental;
pub mod flux;
pub mod prng;
pub mod urban;

use buffers::{AgentBufferRaw, WorldBufferRaw};
use std::ffi::c_float;

/// Complete FFI Export: Advances the full demographic & metabolic cycle in Rust.
#[no_mangle]
pub unsafe extern "C" fn ether_demographic_tick(
    world_ptr: *mut WorldBufferRaw,
    agents_ptr: *mut AgentBufferRaw,
    target_cohort_size: c_float,
    dt_years: c_float,
) {
    if world_ptr.is_null() || agents_ptr.is_null() { return; }
    demographic::process_demographics(&mut *world_ptr, &mut *agents_ptr, target_cohort_size, dt_years);
}

/// Complete FFI Export: Advances the Photosynthetic Farquhar FvCB & Biomass simulation.
#[no_mangle]
pub unsafe extern "C" fn ether_environmental_tick(world_ptr: *mut WorldBufferRaw, dt_years: c_float) {
    if world_ptr.is_null() { return; }
    environmental::process_environmental(&mut *world_ptr, dt_years);
}

/// Complete FFI Export: Advances Urban Agglomeration & Tainter Institutional Entropy.
#[no_mangle]
pub unsafe extern "C" fn ether_urban_tick(world_ptr: *mut WorldBufferRaw, dt_years: c_float) {
    if world_ptr.is_null() { return; }
    urban::process_urban(&mut *world_ptr, dt_years);
}

/// Complete FFI Export: Advances Finite-Volume Hexagonal Trade & Resource Diffusion.
#[no_mangle]
pub unsafe extern "C" fn ether_flux_tick(world_ptr: *mut WorldBufferRaw, dt_seconds: c_float) {
    if world_ptr.is_null() { return; }
    flux::process_flux(&mut *world_ptr, dt_seconds);
}

/// Complete FFI Export: Advances Cultural Langevin SDE Drift and Tensor Anchoring.
#[no_mangle]
pub unsafe extern "C" fn ether_culture_tick(
    world_ptr: *mut WorldBufferRaw,
    agents_ptr: *mut AgentBufferRaw,
    dt_years: c_float,
    diffusion_rate: c_float,
    forcing_rate: c_float,
) {
    if world_ptr.is_null() || agents_ptr.is_null() { return; }
    culture::process_culture(&mut *world_ptr, &mut *agents_ptr, dt_years, diffusion_rate, forcing_rate);
}

/// Complete FFI Export: Advances Cultural Langevin SDE Drift and Tensor Anchoring with explicit stochastic parameters.
#[no_mangle]
pub unsafe extern "C" fn ether_culture_tick_full(
    world_ptr: *mut WorldBufferRaw,
    agents_ptr: *mut AgentBufferRaw,
    dt_years: c_float,
    diffusion_rate: c_float,
    forcing_rate: c_float,
    mutation_rate: c_float,
    seed: i64,
    tick_counter: i64,
) {
    if world_ptr.is_null() || agents_ptr.is_null() { return; }
    culture::process_culture_full(&mut *world_ptr, &mut *agents_ptr, dt_years, diffusion_rate, forcing_rate, mutation_rate, seed, tick_counter);
}
