/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;

/**
 * Authoritative spatial surface area and areal density helper across H3 hexagonal meshes and planets.
 *
 * <p>Provides a single zero-interpretation source of truth for cellular surface areas ($\text{km}^2$),
 * planetary spherical scaling $(R / R_{\oplus})^2$, and intensive areal densities ($\text{individuals/km}^2$,
 * $\text{t/km}^2$, $\text{kg/km}^2$), mirroring {@link PhysicalUnitsHelper} for SI dimensions and
 * {@link org.ether.society.core.SimulationTimeStep} for temporal integration.</p>
 *
 * <p>Hardcoded cell areas (such as {@code 36.13}, {@code 100.0}, {@code 1770.49}, or {@code 12000.0})
 * are strictly forbidden inside simulation engines and telemetry evaluators.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public final class SpatialSurfaceHelper {

    /** Canonical Earth H3 Resolution 4 reference hexagon area ($\text{km}^2$). */
    public static final double CANONICAL_EARTH_RES4_AREA_KM2 = 1770.49;

    /** Minimum numerical cell area guard ($\text{km}^2$) preventing division-by-zero on synthetic micro-buffers. */
    public static final double MIN_SAFE_CELL_AREA_KM2 = 0.001;

    private SpatialSurfaceHelper() {
        // Pure static spatial surface authority
    }

    /**
     * Extracts the embedded Uber H3 resolution level ($0 \dots 15$) from a 64-bit H3 index.
     * Defaults to canonical Resolution 4 when the index is uninitialized ({@code 0L}), e.g. in unit micro-buffers.
     *
     * @param h3Index 64-bit H3 index
     * @return H3 resolution in $[0, 15]$
     */
    public static int extractH3Resolution(long h3Index) {
        if (h3Index == 0L) {
            return 4;
        }
        int res = (int) ((h3Index >>> 52) & 0x0FL);
        return (res >= 0 && res <= 15) ? res : 4;
    }

    /**
     * Computes the spherical surface scaling factor $(R / R_{\oplus})^2$ for a planetary body.
     *
     * @param planetRadiusKm planetary mean radius in kilometers ($\le 0$ defaults to Earth $6\,371\text{ km}$)
     * @return dimensionless spherical surface area ratio relative to Earth
     */
    public static double computePlanetarySurfaceScale(double planetRadiusKm) {
        if (planetRadiusKm <= 0.0 || Double.isNaN(planetRadiusKm) || Double.isInfinite(planetRadiusKm)) {
            return 1.0;
        }
        double ratio = planetRadiusKm / PhysicalConstants.EARTH_RADIUS_KM;
        return Math.max(1.0e-4, ratio * ratio);
    }

    /**
     * Resolves the planar H3 cell surface area in $\text{km}^2$ for a given H3 index and planetary radius:
     * $$A_{\text{cell}}(h, R) = A_{\text{H3},\oplus}(\text{res}(h)) \cdot \left(\frac{R}{R_{\oplus}}\right)^2$$
     *
     * @param h3Index        64-bit H3 cell index
     * @param planetRadiusKm planetary radius in kilometers
     * @return cell planar surface area in $\text{km}^2$
     */
    public static double resolvePlanarCellAreaKm2(long h3Index, double planetRadiusKm) {
        int res = extractH3Resolution(h3Index);
        double earthHexKm2 = H3CellPhysicsAndStateHelper.getAvgHexagonAreaKm2(res);
        return Math.max(MIN_SAFE_CELL_AREA_KM2, earthHexKm2 * computePlanetarySurfaceScale(planetRadiusKm));
    }

    /**
     * Resolves the canonical Earth H3 cell surface area in $\text{km}^2$ for a given H3 resolution ($0 \dots 15$).
     *
     * @param h3Resolution H3 resolution level
     * @return mean planar hexagon area in $\text{km}^2$
     */
    public static double resolveCellAreaKm2(int h3Resolution) {
        return Math.max(MIN_SAFE_CELL_AREA_KM2, H3CellPhysicsAndStateHelper.getAvgHexagonAreaKm2(h3Resolution));
    }

    /**
     * Resolves the effective 3D surface area in $\text{km}^2$ for an {@link H3Cell} scaled to the target planet radius.
     *
     * @param cell           source H3 cell
     * @param planetRadiusKm planetary radius in kilometers
     * @return effective 3D cell surface area in $\text{km}^2$
     */
    public static double resolveEffectiveCellAreaKm2(H3Cell cell, double planetRadiusKm) {
        if (cell == null) {
            return CANONICAL_EARTH_RES4_AREA_KM2 * computePlanetarySurfaceScale(planetRadiusKm);
        }
        double earthEffectiveKm2 = H3CellPhysicsAndStateHelper.getEffectiveSurfaceAreaKm2(cell);
        return Math.max(MIN_SAFE_CELL_AREA_KM2, earthEffectiveKm2 * computePlanetarySurfaceScale(planetRadiusKm));
    }

    /**
     * Resolves the authoritative surface area ($\text{km}^2$) of cell {@code index} in {@link WorldBuffer}.
     * Uses the precomputed {@code cellAreaKm2} array when populated, falling back deterministically
     * to the H3 index resolution and buffer planetary radius.
     *
     * @param buffer source world buffer
     * @param index  cell slot index
     * @return strictly positive cell surface area in $\text{km}^2$
     */
    public static double resolveCellAreaKm2(WorldBuffer buffer, int index) {
        if (buffer == null || index < 0 || index >= buffer.getCapacity()) {
            return CANONICAL_EARTH_RES4_AREA_KM2;
        }
        float[] areas = buffer.getCellAreaKm2Array();
        if (areas != null && index < areas.length && areas[index] > 0.0f && !Float.isNaN(areas[index])) {
            return Math.max(MIN_SAFE_CELL_AREA_KM2, areas[index]);
        }
        long[] h3 = buffer.getH3Indexes();
        long idx = (h3 != null && index < h3.length) ? h3[index] : 0L;
        return resolvePlanarCellAreaKm2(idx, buffer.getPlanetRadiusKm());
    }

    /**
     * Resolves the mean cell surface area ($\text{km}^2$) across a {@link WorldBuffer}.
     *
     * @param buffer source world buffer
     * @return mean cell surface area in $\text{km}^2$
     */
    public static double resolveMeanCellAreaKm2(WorldBuffer buffer) {
        if (buffer == null || buffer.getCapacity() <= 0) {
            return CANONICAL_EARTH_RES4_AREA_KM2;
        }
        float[] areas = buffer.getCellAreaKm2Array();
        if (areas != null && areas.length > 0 && areas[0] > 0.0f) {
            double sum = 0.0;
            int valid = 0;
            for (float a : areas) {
                if (a > 0.0f && !Float.isNaN(a)) {
                    sum += a;
                    valid++;
                }
            }
            if (valid > 0) {
                return Math.max(MIN_SAFE_CELL_AREA_KM2, sum / valid);
            }
        }
        long[] h3 = buffer.getH3Indexes();
        long first = (h3 != null && h3.length > 0) ? h3[0] : 0L;
        return resolvePlanarCellAreaKm2(first, buffer.getPlanetRadiusKm());
    }

    /**
     * Computes living human population density ($\text{individuals/km}^2$) for cell {@code index}.
     *
     * @param buffer source world buffer
     * @param index  cell slot index
     * @return human population density in $\text{individuals/km}^2$
     */
    public static double resolvePopulationDensityPerKm2(WorldBuffer buffer, int index) {
        if (buffer == null || buffer.getHumanPopulation() == null || index < 0 || index >= buffer.getCapacity()) {
            return 0.0;
        }
        double pop = Math.max(0.0, buffer.getHumanPopulation()[index]);
        return pop / resolveCellAreaKm2(buffer, index);
    }

    /**
     * Computes intensive areal density ($X / A_i$) of an extensive cell quantity over cell {@code index}.
     *
     * @param buffer            source world buffer
     * @param index             cell slot index
     * @param extensiveQuantity extensive cell stock or flux
     * @return areal density per $\text{km}^2$
     */
    public static double resolveArealDensityPerKm2(WorldBuffer buffer, int index, double extensiveQuantity) {
        return extensiveQuantity / resolveCellAreaKm2(buffer, index);
    }

    /**
     * Computes the dimensionless cell area ratio relative to canonical Earth H3 Resolution 4 ($1\,770.49\text{ km}^2$).
     *
     * @param buffer source world buffer
     * @param index  cell slot index
     * @return dimensionless area scale factor $A_i / 1770.49$
     */
    public static double resolveAreaScaleVsRes4(WorldBuffer buffer, int index) {
        return resolveCellAreaKm2(buffer, index) / CANONICAL_EARTH_RES4_AREA_KM2;
    }
}
