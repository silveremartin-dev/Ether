// =========================================================================
// DeterministicPRNG: Bit-identical 64-bit SplitMix64 PRNG matching DeterministicPRNG.java
// =========================================================================

const GAMMA: i64 = -7046029254386353131; // 0x9E3779B97F4A7C15L
const DOUBLE_UNIT: f64 = 1.0 / ((1_i64 << 53) as f64);

#[inline(always)]
pub fn split_mix_64(seed: i64, tick: i64, cell_index: i64, stream_id: i64) -> i64 {
    let mut z = seed
        .wrapping_add(tick.wrapping_mul(-4658895280553007687)) // 0xBF58476D1CE4E5B9L
        .wrapping_add(cell_index.wrapping_mul(GAMMA))
        .wrapping_add(stream_id.wrapping_mul(-7723594293311929621)); // 0x94D049BB133111EBL
    z = (z ^ ((z as u64 >> 30) as i64)).wrapping_mul(-4658895280553007687);
    z = (z ^ ((z as u64 >> 27) as i64)).wrapping_mul(-7723594293311929621);
    z ^ ((z as u64 >> 31) as i64)
}

#[inline(always)]
pub fn next_double(seed: i64, tick: i64, cell_index: i64, stream_id: i64) -> f64 {
    let bits = (split_mix_64(seed, tick, cell_index, stream_id) as u64 >> 11) as i64;
    (bits as f64) * DOUBLE_UNIT
}

#[inline(always)]
pub fn next_gaussian(seed: i64, tick: i64, cell_index: i64) -> f64 {
    let u1 = next_double(seed, tick, cell_index, 1).max(1e-15);
    let u2 = next_double(seed, tick, cell_index, 2);
    (-2.0 * u1.ln()).sqrt() * (2.0 * std::f64::consts::PI * u2).cos()
}
