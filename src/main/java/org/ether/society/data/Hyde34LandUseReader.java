/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.config.EtherPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <h1>Hyde34LandUseReader</h1>
 * <p>
 * Empirical reader and spherical-area-conservative regridder for HYDE 3.4 anthropogenic land-use
 * archives ({@code *_lu.zip} and urban {@code uopp_*.asc} from {@code *_pop.zip}, 5-arc-minute
 * $4320 \times 2160$ Esri ASCII grids covering $10000\,\text{BCE}$ to $2024\,\text{CE}$).
 * </p>
 *
 * <p>All extensive land-use area layers ($\text{km}^2$ per 5-arc-minute cell) are transferred onto
 * the standard $2048 \times 1024$ equirectangular grid via {@link ConservativeRasterResampler#resampleExtensive}
 * and divided by exact spherical pixel area $A_{\text{px}}(y)$ to yield dimensionless fractional
 * cover fields $\in [0, 1]$, guaranteeing strict global area conservation across latitudes.</p>
 *
 * <h2>Scientific Applications</h2>
 * <ul>
 *   <li><b>Anthropogenic Ecological Footprint Tensor (Anthromes)</b>: Ellis &amp; Ramankutty (2008) /
 *       Sanderson et al. (2002) land-transformation intensity replacing synthetic Gaussian blobs.</li>
 *   <li><b>Bioclimatic Canopy Conversion</b>: Historical deforestation (forest/jungle clearance into
 *       cultivated plains) and alluvial oasis irrigation (Pongratz et al. 2008; Kaplan et al. 2009).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see Hyde34GridReader
 * @see ConservativeRasterResampler
 */
public final class Hyde34LandUseReader {
    private static final Logger logger = LoggerFactory.getLogger(Hyde34LandUseReader.class);

    public static final int ETHER_WIDTH = Hyde34GridReader.ETHER_WIDTH;
    public static final int ETHER_HEIGHT = Hyde34GridReader.ETHER_HEIGHT;

    /**
     * Immutable $2048 \times 1024$ fractional land-cover container ($\text{km}^2_{\text{use}} / \text{km}^2_{\text{pixel}} \in [0, 1]$).
     */
    public record LandUseFields(
            long effectiveYear,
            float[][] croplandFrac,
            float[][] irrigatedFrac,
            float[][] pastureFrac,
            float[][] rangelandFrac,
            float[][] urbanFrac) {}

    private static final Map<Long, LandUseFields> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(4, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, LandUseFields> eldest) {
                    return size() > 2;
                }
            });

    private Hyde34LandUseReader() {
        // Pure static reader
    }

    /** Clears the bounded epoch land-use cache. */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Loads and conservatively downsamples HYDE 3.4 land-use fractional fields for the given year.
     * Returns {@code null} for deep pre-agricultural epochs ({@code year < -10000}, where anthropogenic
     * agriculture is identically $0.0\%$ per {@code AGENTS.md} §8.2) or if local archives are absent.
     *
     * @param year historical simulation year
     * @return downsampled {@link LandUseFields}, or {@code null} if pre-Holocene or unavailable
     */
    public static LandUseFields loadForYear(long year) {
        if (year < -10000L) {
            return null;
        }
        long effectiveYear = Hyde34GridReader.findNearestHydeYear(year);
        LandUseFields cached = CACHE.get(effectiveYear);
        if (cached != null) {
            return cached;
        }

        String yearTag = DataDownloaderService.getHydeYearTag(effectiveYear);
        File luZip = resolveHydeArchive(yearTag + "_lu.zip");
        if (luZip == null && effectiveYear == 0L) {
            yearTag = "0AD";
            luZip = resolveHydeArchive("0AD_lu.zip");
        } else if (luZip == null && effectiveYear == 1780L) {
            yearTag = DataDownloaderService.getHydeYearTag(1770L);
            luZip = resolveHydeArchive(yearTag + "_lu.zip");
        } else if (luZip == null && effectiveYear == 1930L) {
            yearTag = DataDownloaderService.getHydeYearTag(1920L);
            luZip = resolveHydeArchive(yearTag + "_lu.zip");
        }
        if (luZip == null) {
            return null;
        }

        File popZip = resolveHydeArchive(yearTag + "_pop.zip");
        if (popZip == null && effectiveYear == 0L) {
            popZip = resolveHydeArchive("0AD_pop.zip");
        }

        float[][] crop = null;
        float[][] irri = null;
        float[][] pasture = null;
        float[][] rangeland = null;

        try (ZipFile zf = new ZipFile(luZip)) {
            crop = loadLayerFraction(zf, "cropland");
            irri = loadLayerFraction(zf, "tot_irri");
            pasture = loadLayerFraction(zf, "pasture");
            rangeland = loadLayerFraction(zf, "rangeland");
        } catch (Exception e) {
            logger.warn("Failed reading HYDE 3.4 land-use zip '{}': {}", luZip.getName(), e.getMessage());
        }

        float[][] urban = null;
        if (popZip != null) {
            try (ZipFile pzf = new ZipFile(popZip)) {
                urban = loadLayerFraction(pzf, "uopp_");
            } catch (Exception e) {
                logger.warn("Failed reading HYDE 3.4 urban population from '{}': {}", popZip.getName(), e.getMessage());
            }
        }

        if (crop == null && pasture == null && rangeland == null) {
            return null;
        }
        if (crop == null) crop = new float[ETHER_HEIGHT][ETHER_WIDTH];
        if (irri == null) irri = new float[ETHER_HEIGHT][ETHER_WIDTH];
        if (pasture == null) pasture = new float[ETHER_HEIGHT][ETHER_WIDTH];
        if (rangeland == null) rangeland = new float[ETHER_HEIGHT][ETHER_WIDTH];
        if (urban == null) urban = new float[ETHER_HEIGHT][ETHER_WIDTH];

        LandUseFields fields = new LandUseFields(effectiveYear, crop, irri, pasture, rangeland, urban);
        CACHE.put(effectiveYear, fields);
        logger.info("Ingested HYDE 3.4 conservative land-use fractions (cropland, irri, pasture, rangeland, urban) for epoch {}.", effectiveYear);
        return fields;
    }

    private static File resolveHydeArchive(String zipName) {
        File f = new File("user_data/maps/hyde34/" + zipName);
        if (f.exists() && f.isFile() && f.length() > 10000L) return f;
        f = EtherPaths.resolveSourceFile("hyde34/" + zipName);
        if (f.exists() && f.isFile() && f.length() > 10000L) return f;
        return null;
    }

    private static float[][] loadLayerFraction(ZipFile zf, String prefix) {
        try {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            ZipEntry match = null;
            String pLower = prefix.toLowerCase(Locale.ROOT);
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName().toLowerCase(Locale.ROOT);
                if (name.startsWith(pLower) && name.endsWith(".asc")) {
                    match = e;
                    break;
                }
            }
            if (match == null) return null;

            try (InputStream is = zf.getInputStream(match)) {
                Hyde34GridReader.ParsedAsciiGrid parsed = Hyde34GridReader.parseAsciiGrid(is);
                if (parsed == null) return null;

                Hyde34GridReader.HydeGridMetadata meta = parsed.meta();
                float[][] areaKm2 = ConservativeRasterResampler.resampleExtensive(
                        parsed.data(), meta.ncols, meta.nrows, (float) meta.nodataValue, ETHER_WIDTH, ETHER_HEIGHT);

                float[][] frac = new float[ETHER_HEIGHT][ETHER_WIDTH];
                for (int y = 0; y < ETHER_HEIGHT; y++) {
                    double pxArea = Math.max(1e-6, ConservativeRasterResampler.sphericalPixelAreaKm2(y, ETHER_WIDTH, ETHER_HEIGHT));
                    float[] srcRow = areaKm2[y];
                    float[] dstRow = frac[y];
                    for (int x = 0; x < ETHER_WIDTH; x++) {
                        dstRow[x] = (float) Math.clamp(srcRow[x] / pxArea, 0.0, 1.0);
                    }
                }
                return frac;
            }
        } catch (Exception e) {
            logger.warn("Failed reading HYDE land-use layer '{}' from '{}': {}", prefix, zf.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Computes a continuous anthropogenic ecological transformation intensity $\in [0, 1]$
     * calibrated to Ellis &amp; Ramankutty (2008) Anthromes and Sanderson et al. (2002) Human Footprint
     * biome weights:
     * $$S_{\text{eco}} = \text{clamp}\!\big(1.0\,f_{\text{urb}} + 0.85\,f_{\text{crop}} + 0.15\,f_{\text{irri}} + 0.50\,f_{\text{past}} + 0.20\,f_{\text{range}},\; 0,\; 1\big)$$
     *
     * @param cropFrac  cropland fractional area $[0, 1]$
     * @param irriFrac  irrigated cropland fractional area $[0, 1]$
     * @param pastFrac  intensive pasture fractional area $[0, 1]$
     * @param rangeFrac extensive rangeland fractional area $[0, 1]$
     * @param urbanFrac built-up urban fractional area $[0, 1]$
     * @return continuous ecological strain index in $[0.0, 1.0]$
     */
    public static double computeAnthropogenicStrainIndex(
            double cropFrac, double irriFrac, double pastFrac, double rangeFrac, double urbanFrac) {
        double raw = urbanFrac
                + 0.85 * cropFrac
                + 0.15 * irriFrac
                + 0.50 * pastFrac
                + 0.20 * rangeFrac;
        return Math.clamp(raw, 0.0, 1.0);
    }

    /**
     * Modulates a natural Holdridge/Whittaker biome RGB color code using empirical HYDE 3.4
     * agricultural land-cover transformation (deforestation &amp; alluvial irrigation).
     * Never alters oceanic basins, polar glaciers, alpine snow, mountains, or tundra.
     *
     * @param naturalBiomeRgb 24-bit RGB biome code from {@link WorldClimEmpiricalRasterLoader}
     * @param x               pixel X coordinate in $[0, \text{ETHER\_WIDTH}-1]$
     * @param y               pixel Y coordinate in $[0, \text{ETHER\_HEIGHT}-1]$
     * @param fields          loaded {@link LandUseFields} for the epoch (may be {@code null})
     * @return anthropogenically modulated 24-bit RGB biome code
     */
    public static int modulateBiomeWithLandUse(int naturalBiomeRgb, int x, int y, LandUseFields fields) {
        if (fields == null || x < 0 || x >= ETHER_WIDTH || y < 0 || y >= ETHER_HEIGHT) {
            return naturalBiomeRgb;
        }
        if (naturalBiomeRgb == WorldClimEmpiricalRasterLoader.BIOME_GLACIER
                || naturalBiomeRgb == WorldClimEmpiricalRasterLoader.BIOME_SNOW
                || naturalBiomeRgb == WorldClimEmpiricalRasterLoader.BIOME_MOUNTAINS
                || naturalBiomeRgb == WorldClimEmpiricalRasterLoader.BIOME_TUNDRA
                || naturalBiomeRgb == PaleoClimateMapRasterizer.BIOME_DEEP_OCEAN
                || naturalBiomeRgb == PaleoClimateMapRasterizer.BIOME_OCEAN) {
            return naturalBiomeRgb;
        }

        double crop = fields.croplandFrac()[y][x];
        double irri = fields.irrigatedFrac()[y][x];
        double past = fields.pastureFrac()[y][x];
        double urb = fields.urbanFrac()[y][x];

        double clearedFraction = crop + urb + 0.65 * past;

        // Forest / jungle canopy clearance into open cultivated plains/pasture mosaic
        if ((naturalBiomeRgb == WorldClimEmpiricalRasterLoader.BIOME_FOREST
                || naturalBiomeRgb == WorldClimEmpiricalRasterLoader.BIOME_JUNGLE)
                && clearedFraction >= 0.42) {
            return WorldClimEmpiricalRasterLoader.BIOME_PLAINS;
        }

        // Irrigated arid floodplains (e.g., Nile, Mesopotamia, Indus) into cultivated plains
        if (naturalBiomeRgb == WorldClimEmpiricalRasterLoader.BIOME_DESERT
                && (irri >= 0.20 || crop >= 0.38)) {
            return WorldClimEmpiricalRasterLoader.BIOME_PLAINS;
        }

        // Heavily terraced/cultivated hillslopes
        if (naturalBiomeRgb == WorldClimEmpiricalRasterLoader.BIOME_HILLS
                && (crop + urb) >= 0.55) {
            return WorldClimEmpiricalRasterLoader.BIOME_PLAINS;
        }

        return naturalBiomeRgb;
    }
}
