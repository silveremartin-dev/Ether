/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvère Martin-Michiellot & Gemini AI
 */
package org.ether.society.data;

import org.ether.society.config.EtherPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * PAGES 2k Consortium (Nature Geoscience 2019 / Paleoceanography 2017) Paleoclimate Provider.
 *
 * <p>Provides programmatic access to the 2,000-year multi-proxy Common Era surface temperature
 * reconstruction and the 647 geographically distributed paleoclimate proxy archive sites
 * (tree rings, ice cores, speleothems, lake and marine sediments, corals, and boreholes).</p>
 *
 * <p>Physical equations and historical baseline reference:
 * <ul>
 *   <li>Global Mean Surface Temperature (GMST) anomalies are expressed in degrees Celsius (\(^\circ\text{C}\))
 *       relative to the 1961–1990 CE instrumental climatological normal:
 *       $$\Delta T(t) = T(t) - \overline{T}_{1961-1990}$$</li>
 *   <li>Ensemble statistics include the median (\(q_{0.50}\)), the 2.5th percentile (\(q_{0.025}\)),
 *       and the 97.5th percentile (\(q_{0.975}\)) across 7,000 reconstructions spanning diverse methods
 *       (BHM, CPS, M08, OIE, PAIR, CCA).</li>
 *   <li>Spatial geodesic distance between simulation cells and proxy sites uses the Haversine metric:
 *       $$d = 2 R_{\oplus} \arcsin \left( \sqrt{\sin^2\left(\frac{\Delta \phi}{2}\right) + \cos \phi_1 \cos \phi_2 \sin^2\left(\frac{\Delta \lambda}{2}\right)} \right)$$
 *       where \(R_{\oplus} \approx 6371.0\,\text{km}\).</li>
 * </ul>
 * </p>
 *
 * @author Silvère Martin-Michiellot &amp; Gemini AI
 * @version 1.0.0
 * @see org.ether.society.config.EtherPaths#resolveSourceFile(String)
 */
public final class Pages2kClimateProvider {

    private static final Logger logger = LoggerFactory.getLogger(Pages2kClimateProvider.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Immutable record representing a single year's global temperature reconstruction anomaly.
     *
     * @param yearCe year Common Era (1 to 2000 CE)
     * @param medianAnomalyC full ensemble median temperature anomaly (\(^\circ\text{C}\))
     * @param percentile2_5C ensemble 2.5th percentile lower bound (\(^\circ\text{C}\))
     * @param percentile97_5C ensemble 97.5th percentile upper bound (\(^\circ\text{C}\))
     * @param filtered31yrMedianC 31-year Butterworth filtered median anomaly (\(^\circ\text{C}\)), or {@code NaN} if unavailable
     */
    public record TemperatureAnomalyRecord(
            int yearCe,
            double medianAnomalyC,
            double percentile2_5C,
            double percentile97_5C,
            double filtered31yrMedianC
    ) {}

    /**
     * Immutable record representing an individual paleoclimate proxy record site.
     *
     * @param datasetId unique LiPD dataset identifier (e.g. {@code "Eur-SpannagelCave.Mangini.2005"})
     * @param siteName human-readable geographic site name
     * @param region continental or oceanic region (e.g. {@code "Europe"}, {@code "Asia"}, {@code "Antarctica"}, {@code "Ocean"})
     * @param archiveType paleoclimatic natural archive type (e.g. {@code "Tree ring"}, {@code "Speleothem"}, {@code "GlacierIce"})
     * @param latitude degrees north (\(-90.0\) to \(+90.0\))
     * @param longitude degrees east (\(-180.0\) to \(+180.0\))
     * @param elevationM altitude above sea level in meters
     * @param lipdFilename associated LiPD archive filename
     */
    public record ProxySite(
            String datasetId,
            String siteName,
            String region,
            String archiveType,
            double latitude,
            double longitude,
            double elevationM,
            String lipdFilename
    ) {}

    // In-memory indexed datasets
    private static final Map<Integer, TemperatureAnomalyRecord> ANOMALY_BY_YEAR = new HashMap<>(2048);
    private static final List<ProxySite> ALL_PROXIES = new ArrayList<>(700);
    private static boolean initialized = false;

    private Pages2kClimateProvider() {
        // Utility class
    }

    /**
     * Ensures datasets are loaded from {@code data/sources/pages2k/}.
     * Thread-safe lazy initialization.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        loadReconstructionSeries();
        loadProxySitesIndex();
        initialized = true;
    }

    /**
     * Retrieves the global mean temperature anomaly median in \(^\circ\text{C}\) for a given Common Era year.
     *
     * @param yearCe year in Common Era (1 to 2000 CE)
     * @return global temperature anomaly relative to 1961-1990, or 0.0 if year is outside the calibrated range
     */
    public static double getTemperatureAnomaly(int yearCe) {
        if (!initialized) {
            initialize();
        }
        TemperatureAnomalyRecord rec = ANOMALY_BY_YEAR.get(yearCe);
        if (rec != null) {
            return rec.medianAnomalyC();
        }
        return 0.0;
    }

    /**
     * Retrieves the full statistical reconstruction record for a given year.
     *
     * @param yearCe year in Common Era (1 to 2000 CE)
     * @return {@link Optional} containing the record with 95% confidence intervals and filtered trend, or empty if absent
     */
    public static Optional<TemperatureAnomalyRecord> getRecord(int yearCe) {
        if (!initialized) {
            initialize();
        }
        return Optional.ofNullable(ANOMALY_BY_YEAR.get(yearCe));
    }

    /**
     * Returns an unmodifiable list of all 647 PAGES 2k proxy sites.
     *
     * @return unmodifiable list of {@link ProxySite}
     */
    public static List<ProxySite> getAllProxySites() {
        if (!initialized) {
            initialize();
        }
        return Collections.unmodifiableList(ALL_PROXIES);
    }

    /**
     * Filters proxy sites by geographic region (case-insensitive match, supporting full names and standard PAGES abbreviations).
     *
     * @param region query region name (e.g. "Eur", "Europe", "Asia", "Africa", "Ant", "Antarctica", "Ocean", "NAm", "SAm", "Arc")
     * @return list of matching proxy sites
     */
    public static List<ProxySite> getProxiesByRegion(String region) {
        if (!initialized) {
            initialize();
        }
        if (region == null || region.isBlank()) {
            return Collections.emptyList();
        }
        String needle = region.trim().toLowerCase();
        // Canonical alias mapping
        if ("europe".equals(needle)) needle = "eur";
        else if ("antarctica".equals(needle)) needle = "ant";
        else if ("arctic".equals(needle)) needle = "arc";
        else if ("australia".equals(needle) || "australasia".equals(needle)) needle = "aus";
        else if ("north america".equals(needle)) needle = "nam";
        else if ("south america".equals(needle)) needle = "sam";

        List<ProxySite> matched = new ArrayList<>();
        for (ProxySite p : ALL_PROXIES) {
            String pReg = p.region().toLowerCase();
            if (pReg.contains(needle) || needle.contains(pReg)) {
                matched.add(p);
            }
        }
        return matched;
    }

    /**
     * Filters proxy sites by paleoclimatic archive type (case-insensitive substring match, with aliases for Tree ring / Wood).
     *
     * @param archiveType archive category (e.g. "Wood", "Tree", "Tree ring", "GlacierIce", "Speleothem", "LakeSediment", "Coral", "MarineSediment")
     * @return list of matching proxy sites
     */
    public static List<ProxySite> getProxiesByArchiveType(String archiveType) {
        if (!initialized) {
            initialize();
        }
        if (archiveType == null || archiveType.isBlank()) {
            return Collections.emptyList();
        }
        String needle = archiveType.trim().toLowerCase();
        if ("tree ring".equals(needle) || "tree".equals(needle) || "treerings".equals(needle) || "dendro".equals(needle)) {
            needle = "wood";
        }

        List<ProxySite> matched = new ArrayList<>();
        for (ProxySite p : ALL_PROXIES) {
            String pType = p.archiveType().toLowerCase();
            if (pType.contains(needle) || needle.contains(pType)) {
                matched.add(p);
            }
        }
        return matched;
    }

    /**
     * Finds the nearest paleoclimate proxy site to given latitude and longitude using Great-Circle Haversine distance.
     *
     * @param latitude degrees north (-90 to +90)
     * @param longitude degrees east (-180 to +180)
     * @return {@link Optional} containing the nearest {@link ProxySite}, or empty if no proxies are loaded
     */
    public static Optional<ProxySite> findNearestProxy(double latitude, double longitude) {
        if (!initialized) {
            initialize();
        }
        if (ALL_PROXIES.isEmpty()) {
            return Optional.empty();
        }

        ProxySite nearest = null;
        double minDistanceKm = Double.MAX_VALUE;

        for (ProxySite site : ALL_PROXIES) {
            double dist = haversineDistanceKm(latitude, longitude, site.latitude(), site.longitude());
            if (dist < minDistanceKm) {
                minDistanceKm = dist;
                nearest = site;
            }
        }

        return Optional.ofNullable(nearest);
    }

    /**
     * Calculates the Great-Circle Haversine distance in kilometers between two geographic coordinates.
     *
     * @param lat1 latitude of point 1 in degrees
     * @param lon1 longitude of point 1 in degrees
     * @param lat2 latitude of point 2 in degrees
     * @param lon2 longitude of point 2 in degrees
     * @return geodesic distance in kilometers
     */
    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2.0) * Math.sin(deltaPhi / 2.0)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2.0) * Math.sin(deltaLambda / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_KM * c;
    }

    // ── Internal File Parsing ─────────────────────────────────────────────────

    private static void loadReconstructionSeries() {
        File file = EtherPaths.resolveSourceFile("pages2k/pages2k_global_temperature_reconstruction_1_2000ce.tsv");
        if (!file.exists()) {
            logger.warn("PAGES 2k global reconstruction TSV not found at: {}", file.getAbsolutePath());
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("Year")) {
                    continue;
                }

                String[] parts = line.split("\t");
                if (parts.length >= 5) {
                    try {
                        int year = Integer.parseInt(parts[0].trim());
                        double median = parseDoubleSafe(parts[2]);
                        double p2_5 = parseDoubleSafe(parts[3]);
                        double p97_5 = parseDoubleSafe(parts[4]);
                        double filteredMedian = (parts.length >= 7) ? parseDoubleSafe(parts[6]) : Double.NaN;

                        ANOMALY_BY_YEAR.put(year, new TemperatureAnomalyRecord(year, median, p2_5, p97_5, filteredMedian));
                    } catch (NumberFormatException e) {
                        logger.debug("Skipping unparseable reconstruction line: {}", line);
                    }
                }
            }
            logger.info("Loaded {} PAGES 2k annual global temperature reconstruction records (1-2000 CE).", ANOMALY_BY_YEAR.size());
        } catch (IOException e) {
            logger.error("Failed to read PAGES 2k global temperature TSV: {}", file.getAbsolutePath(), e);
        }
    }

    private static void loadProxySitesIndex() {
        File file = EtherPaths.resolveSourceFile("pages2k/pages2k_proxy_sites_index.csv");
        if (!file.exists()) {
            logger.warn("PAGES 2k proxy sites index CSV not found at: {}", file.getAbsolutePath());
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // Header line
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length >= 8) {
                    try {
                        String datasetId = parts[0].trim();
                        String siteName = parts[1].trim();
                        String region = parts[2].trim();
                        String archiveType = parts[3].trim();
                        double lat = Double.parseDouble(parts[4].trim());
                        double lon = Double.parseDouble(parts[5].trim());
                        double elev = Double.parseDouble(parts[6].trim());
                        String lipd = parts[7].trim();

                        ALL_PROXIES.add(new ProxySite(datasetId, siteName, region, archiveType, lat, lon, elev, lipd));
                    } catch (NumberFormatException e) {
                        logger.debug("Skipping unparseable proxy site line: {}", line);
                    }
                }
            }
            logger.info("Loaded {} PAGES 2k multi-proxy observation sites.", ALL_PROXIES.size());
        } catch (IOException e) {
            logger.error("Failed to read PAGES 2k proxy sites index CSV: {}", file.getAbsolutePath(), e);
        }
    }

    private static double parseDoubleSafe(String val) {
        if (val == null || val.isBlank() || "NA".equalsIgnoreCase(val.trim())) {
            return Double.NaN;
        }
        return Double.parseDouble(val.trim());
    }
}
