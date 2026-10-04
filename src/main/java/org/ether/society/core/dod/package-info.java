/**
 * <h1>Data-Oriented Design (DoD) Memory Layout &amp; Computational Kernels</h1>
 * <p>
 * Implements structure-of-arrays (SoA) contiguous native memory buffers and cache-friendly
 * data structures for high-performance cellular and demographic state transitions.
 * </p>
 * <p>
 * Core responsibilities:
 * <ul>
 *   <li>{@link org.ether.society.core.dod.WorldBuffer}: Contiguous off-heap / primitive array storage for all cell attributes.</li>
 *   <li>{@link org.ether.society.core.dod.DemographicKernel}: Analytical demographic, mortality, and population dynamics updates.</li>
 *   <li>{@link org.ether.society.core.dod.EnvironmentalKernel}: Soil, water, biomass, climate, and pollution mass conservation updates.</li>
 *   <li>{@link org.ether.society.core.dod.FormulaEvaluator}: AST-based &amp; bytecode-accelerated formula evaluation engine.</li>
 * </ul>
 * </p>
 */
package org.ether.society.core.dod;
