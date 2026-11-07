/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.data.DODDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.engines.tier1.NuclearWarfareClimateEngine;
import org.ether.society.engines.tier1.ThermodynamicMigrationEngine;
import org.ether.society.events.HistoricalIntervention;
import org.ether.society.events.LeaderArchetype;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;
import org.ether.society.model.ClimateEvent;
import org.ether.society.model.HabitatType;
import org.ether.society.database.Polity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * <h1>EventPhysicalImpactExecutor</h1>
 * <p>
 * Centralized, fully headless physical execution engine for planetary events,
 * astronomical forcings, cataclysms, and ecological shocks.
 * </p>
 * <p>
 * <b>Epistemic Modeling Principle — Exogenous Boundary Forcings vs. Emergence:</b><br>
 * In macroeconomic and Earth System Models (ESMs, CMIP6, IPCC), external shocks such as
 * explosive volcanic eruptions (e.g., 1815 Tambora, 1257 Samalas) or extraterrestrial impacts
 * are not artificial ad-hoc overrides of internal dynamics; they constitute authentic
 * <i>exogenous boundary conditions</i>. Ether strictly routes these external events through
 * biophysical conservation channels:
 * <ul>
 *   <li>Atmospheric aerosol optical depth \(\tau\) attenuating top-of-atmosphere solar irradiance:
 *       \(S = S_0 \cdot e^{-\tau}\) (Beer-Lambert law).</li>
 *   <li>Thermodynamic surface cooling and precipitation reduction impacting net primary productivity (NPP).</li>
 *   <li>Physical infrastructure and agricultural capital degradation through exponential survival curves:
 *       \(K(t^+) = K(t^-) \cdot e^{-\alpha \cdot \text{magnitude}}\).</li>
 * </ul>
 * Governs both scheduled scenario shocks and live exploratory sandbox interventions,
 * ensuring complete bitwise and mathematical parity between GUI and headless executions
 * ({@code AGENTS.md} §1, §7, §10).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class EventPhysicalImpactExecutor {

    private static final Logger logger = LoggerFactory.getLogger(EventPhysicalImpactExecutor.class);

    private EventPhysicalImpactExecutor() {
        // Static headless utility class
    }

    /**
     * Finds the nearest simulation cell to the specified latitude and longitude coordinates.
     *
     * @param cells cell list
     * @param lat target latitude in degrees
     * @param lng target longitude in degrees
     * @return nearest H3Cell, or {@code null} if list is empty
     */
    public static H3Cell findNearestCell(List<H3Cell> cells, double lat, double lng) {
        if (cells == null || cells.isEmpty()) return null;
        H3Cell nearest = null;
        double minD = Double.MAX_VALUE;
        for (H3Cell c : cells) {
            if (c == null) continue;
            double d = Math.hypot(c.getLatitudeOr(0.0) - lat, c.getLongitudeOr(0.0) - lng);
            if (d < minD) {
                minD = d;
                nearest = c;
            }
        }
        return nearest;
    }

    /**
     * Checks if the event type is strictly terrestrial (cannot originate in open ocean).
     */
    public static boolean isTerrestrialEventType(String type) {
        if (type == null) return false;
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "FLOOD", "DROUGHT", "FAMINE", "VOLCANO", "EARTHQUAKE", "PANDEMIC",
                 "RENAISSANCE_BOOM", "TECH_SINGULARITY", "CYBER_ATTACK", "ECONOMIC_CRASH",
                 "BIODIVERSITY_COLLAPSE" -> true;
            default -> false;
        };
    }

    /**
     * Checks if the event type requires oceanic or coastal coordinates.
     */
    public static boolean isOceanicEventType(String type) {
        if (type == null) return false;
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "TSUNAMI" -> true;
            default -> false;
        };
    }

    /**
     * Executes physical, climatic, epidemiological, or infrastructural forcing across cells.
     *
     * @param engine simulation engine (may be headless or GUI)
     * @param type event typology
     * @param name event descriptive name
     * @param mag intensity or magnitude [0.0, 10.0]
     * @param lat epicenter latitude
     * @param lng epicenter longitude
     * @param depth hypocentral depth in km (for seismic/volcanic events)
     * @param radiusKm influence radius in km
     * @param durationDays event active duration in days (-1 or 0 for permanent)
     */
    public static void executeEventForcing(
            H3SimulationEngine engine,
            String type,
            String name,
            double mag,
            double lat,
            double lng,
            double depth,
            double radiusKm,
            int durationDays) {
        if (engine == null || engine.getCells() == null || engine.getCells().isEmpty() || type == null) {
            return;
        }

        applyPhysicalForcingToGrid(
                engine.getCells(),
                engine.getWorldBuffer(),
                engine.getAgentBuffer(),
                engine.getNations(),
                type,
                name,
                mag,
                lat,
                lng,
                depth,
                radiusKm,
                durationDays
        );
    }

    /**
     * Applies physical forcing directly to cellular collections and DOD buffers.
     * Fully headless: zero dependency on JavaFX, GUI, or audio rendering.
     *
     * @param cells contiguous cell list
     * @param worldBuffer DOD world buffer (optional, may be null)
     * @param agentBuffer DOD agent buffer (optional, may be null)
     * @param nations active polities list (optional, may be null)
     * @param type canonical event typology
     * @param name descriptive event title
     * @param mag normalized magnitude or intensity [0.0, 10.0]
     * @param lat epicenter latitude [-90, +90]
     * @param lng epicenter longitude [-180, +180]
     * @param depth hypocentral depth in km
     * @param radiusKm geographical impact radius in km
     * @param durationDays active duration in days (<= 0 for permanent)
     */
    public static void applyPhysicalForcingToGrid(
            List<H3Cell> cells,
            WorldBuffer worldBuffer,
            AgentBuffer agentBuffer,
            List<Polity> nations,
            String type,
            String name,
            double mag,
            double lat,
            double lng,
            double depth,
            double radiusKm,
            int durationDays) {

        if (cells == null || cells.isEmpty() || type == null) return;

        String upperType = type.toUpperCase(Locale.ROOT);
        String lowerName = name != null ? name.toLowerCase(Locale.ROOT) : "";
        int totalCells = cells.size();

        // 1. Effective influence radius calculation (Haversine km)
        double effectiveRadiusKm = radiusKm > 0.0 ? radiusKm : calculateDefaultRadiusKm(upperType, lowerName, mag);

        // 2. Global Atmospheric / Radiative Forcing Shocks
        applyAtmosphericGlobalForcing(upperType, mag, durationDays);

        float[] cellSurvival = null;
        boolean anyAgentMortality = false;

        // 3. Local Spatial Cellular Shocks across H3 mesh
        for (int i = 0; i < totalCells; i++) {
            H3Cell cell = cells.get(i);
            if (cell == null) continue;

            double cellLat = cell.getLatitudeOr(0.0);
            double cellLng = cell.getLongitudeOr(0.0);
            double distKm = ThermodynamicMigrationEngine.calculateHaversineDistance(cellLat, cellLng, lat, lng);

            // Hypocentral 3D distance for seismic/volcanic depth calculations: R = sqrt(dist^2 + depth^2)
            double distance3dKm = Math.hypot(distKm, Math.max(0.0, depth));

            if (distance3dKm <= effectiveRadiusKm) {
                double attenuation = Math.max(0.0, 1.0 - (distance3dKm / effectiveRadiusKm));
                int prevPop = cell.getPopulationOr(0);
                int lostPop = 0;

                switch (upperType) {
                    case "VOLCANO" -> {
                        // Explosive tephra fallout, pyroclastic density currents and localized sulfate cooling
                        double localTempDrop = 0.85 * (mag / 10.0) * attenuation;
                        cell.setTemperature(Math.max(-50.0, cell.getTemperatureOr(15.0) - localTempDrop));

                        // Tephra burial destroying edible biomass and capital infrastructure
                        if (cell.getFoodResource() != null) {
                            cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-0.45 * (mag / 10.0) * attenuation)));
                        }
                        if (cell.getResourceCapital() != null) {
                            cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.08 * (mag / 10.0) * attenuation)));
                        }
                        if (prevPop > 0 && attenuation > 0.7) {
                            lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.05 * (mag / 10.0) * attenuation)));
                        }
                    }

                    case "EARTHQUAKE" -> {
                        // Seismic rupture: structural collapse of unreinforced buildings (Omori / Gutenberg-Richter)
                        double seismicIntensity = (mag / 10.0) * attenuation;
                        if (cell.getResourceCapital() != null) {
                            cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.25 * seismicIntensity)));
                        }
                        if (prevPop > 0) {
                            lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.08 * seismicIntensity)));
                        }
                    }

                    case "TSUNAMI" -> {
                        // Tsunami run-up strictly damages coastal or low-elevation marine zones (<= 25m)
                        boolean isCoastalOrLow = (cell.getIsCoastal() != null && cell.getIsCoastal())
                                || cell.getBiome() == Biome.OCEAN
                                || cell.getBiome() == Biome.DEEP_OCEAN
                                || (cell.getElevation() != null && cell.getElevation() <= 25.0);

                        if (isCoastalOrLow) {
                            double waveIntensity = (mag / 10.0) * attenuation;
                            if (cell.getResourceCapital() != null) {
                                cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.40 * waveIntensity)));
                            }
                            if (cell.getFoodResource() != null) {
                                cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-0.60 * waveIntensity)));
                            }
                            if (prevPop > 0) {
                                lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.15 * waveIntensity)));
                            }
                        }
                    }

                    case "FLOOD" -> {
                        // Riverine inundation: wash-out of crops and alluvial settlements
                        double floodIntensity = (mag / 10.0) * attenuation;
                        if (cell.getFoodResource() != null) {
                            cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.45 * floodIntensity)));
                        }
                        if (cell.getResourceCapital() != null) {
                            cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.10 * floodIntensity)));
                        }
                        if (prevPop > 0) {
                            lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.03 * floodIntensity)));
                        }
                    }

                    case "DROUGHT", "HEATWAVE", "FAMINE" -> {
                        // Hydrological soil water deficit and crop desiccation
                        double droughtIntensity = (mag / 10.0) * attenuation;
                        if (cell.getFreshwaterAquifer() != null) {
                            cell.setFreshwaterAquifer(Math.max(0.0, cell.getFreshwaterAquifer() * (1.0 - 0.50 * droughtIntensity)));
                        }
                        if (cell.getFoodResource() != null) {
                            cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * (1.0 - 0.65 * droughtIntensity)));
                        }
                        if (upperType.equals("HEATWAVE")) {
                            cell.setTemperature(cell.getTemperatureOr(15.0) + 1.2 * mag * attenuation);
                        }
                        if (prevPop > 0 && upperType.equals("FAMINE")) {
                            lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-0.06 * droughtIntensity)));
                        }
                    }

                    case "PANDEMIC", "EPIDEMIC", "PLAGUE" -> {
                        // Bio-molecular epidemiological transmission: differential virulence (Kermack-McKendrick SIR)
                        double virulence = 0.12;
                        if (lowerName.contains("grippe") || lowerName.contains("influenza") || lowerName.contains("flu")) {
                            virulence = 0.035;
                        } else if (lowerName.contains("peste") || lowerName.contains("black death") || lowerName.contains("plague")) {
                            virulence = 0.35;
                        } else if (lowerName.contains("covid") || lowerName.contains("sars")) {
                            virulence = 0.055;
                        }

                        if (prevPop > 0) {
                            lostPop = (int) Math.round(prevPop * (1.0 - Math.exp(-virulence * (mag / 10.0) * attenuation)));
                        }
                        if (cell.getResourceCapital() != null) {
                            cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-0.04 * attenuation)));
                        }
                    }

                    case "NUCLEAR_STRIKE", "NUCLEAR_WINTER" -> {
                        // Prompt thermal radiation, blast overpressure and ionizing gamma flash
                        double blast = (mag / 10.0) * attenuation;
                        if (prevPop > 0) {
                            lostPop = (int) Math.round(prevPop * Math.min(0.98, 0.90 * blast));
                        }
                        if (cell.getResourceCapital() != null) {
                            cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-1.5 * blast)));
                        }
                        if (cell.getFoodResource() != null) {
                            cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-1.8 * blast)));
                        }
                        cell.setPollutionLevel(Math.min(1.0, cell.getPollutionLevelOr(0.0) + 0.85 * blast));
                        cell.setTemperature(Math.max(-50.0, cell.getTemperatureOr(15.0) - 1.5 * blast));
                    }

                    case "METEOR" -> {
                        // Hypervelocity impact cratering and ejecta curtain shockwave
                        double impactBlast = (mag / 10.0) * attenuation;
                        if (prevPop > 0) {
                            lostPop = (int) Math.round(prevPop * Math.min(0.99, 0.95 * impactBlast));
                        }
                        if (cell.getResourceCapital() != null) {
                            cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * Math.exp(-2.0 * impactBlast)));
                        }
                        if (cell.getFoodResource() != null) {
                            cell.setFoodResource(Math.max(0.0, cell.getFoodResource() * Math.exp(-2.2 * impactBlast)));
                        }
                        cell.setElevation(Math.max(-500.0, cell.getElevationOr(0.0) - 150.0 * impactBlast));
                    }

                    case "SOLAR_EMP", "CYBER_ATTACK", "ECONOMIC_CRASH" -> {
                        // Grid disruption and capital devaluation
                        double disruption = (mag / 10.0) * attenuation;
                        if (cell.getResourceCapital() != null) {
                            cell.setResourceCapital(Math.max(0.0, cell.getResourceCapital() * (1.0 - 0.40 * disruption)));
                        }
                    }

                    // ==========================================
                    // POSITIVE BREAKTHROUGHS & RATCHET MILESTONES
                    // ==========================================

                    case "TECH_SINGULARITY", "FUSION", "milestone_technology" -> {
                        // Permanent clean exergy grid expansion & deep physical decarbonization
                        double techBonus = (mag / 10.0) * attenuation;
                        double currentCap = cell.getResourceCapitalOr(1000.0);
                        // Upgrade structural machine capital K
                        cell.setResourceCapital(currentCap + (5000.0 * techBonus * Math.max(1, prevPop)));
                        // Purge atmospheric pollution and industrial toxins
                        cell.setPollutionLevel(Math.max(0.0, cell.getPollutionLevelOr(0.0) * (1.0 - 0.80 * techBonus)));
                        // Reduce transport friction via high-speed magnetic/automated infrastructure
                        double curFriction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
                        cell.setMovementFriction(Math.max(0.15, curFriction * (1.0 - 0.50 * techBonus)));
                    }

                    case "RENAISSANCE_BOOM", "milestone_golden_age" -> {
                        // Institutional Enlightenment, reduction in transport friction & Asabiyyah reinforcement
                        double boomBonus = (mag / 10.0) * attenuation;
                        double curCap = cell.getResourceCapitalOr(500.0);
                        cell.setResourceCapital(curCap * (1.0 + 0.35 * boomBonus));
                        double curFric = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
                        cell.setMovementFriction(Math.max(0.20, curFric * (1.0 - 0.30 * boomBonus)));

                        Polity owner = cell.getOwner();
                        if (owner != null) {
                            owner.setStateCapacity(Math.min(1.0, owner.getStateCapacity() + 0.15 * boomBonus));
                            owner.setAsabiyyah(Math.min(1.0, owner.getAsabiyyah() + 0.10 * boomBonus));
                        }
                    }

                    case "GEOENGINEERING" -> {
                        // Controlled solar radiation management (SRM) cooling with monsoon precipitation side-effects
                        double geoMag = (mag / 10.0) * attenuation;
                        cell.setTemperature(cell.getTemperatureOr(15.0) - 1.5 * geoMag);
                        if (cell.getRainfall() != null) {
                            cell.setRainfall(Math.max(50.0, cell.getRainfall() * (1.0 - 0.15 * geoMag)));
                        }
                    }

                    case "ALIEN_CONTACT" -> {
                        // Exogenous civilizational catalyst: massive physical capital upgrade
                        double alienBonus = (mag / 10.0) * attenuation;
                        double cap = cell.getResourceCapitalOr(1000.0);
                        cell.setResourceCapital(cap * (1.0 + 1.2 * alienBonus));
                        cell.setPollutionLevel(0.0);
                    }

                    // Extraterrestrial Pressurized Habitat Hazards
                    case "DOME_BREACH" -> {
                        double breachBlast = (mag / 10.0) * attenuation;
                        cell.setHabitatIntegrity(0.0);
                        if (prevPop > 0) {
                            lostPop = (int) Math.round(prevPop * (1.0 - 0.90 * breachBlast));
                        }
                    }
                    case "ECLSS_FAILURE" -> {
                        double eclssBlast = (mag / 10.0) * attenuation;
                        double curInteg = cell.getHabitatIntegrity() != null ? cell.getHabitatIntegrity() : 1.0;
                        cell.setHabitatIntegrity(Math.max(0.05, curInteg - 0.70 * eclssBlast));
                        if (prevPop > 0) {
                            lostPop = (int) Math.round(prevPop * (1.0 - 0.65 * eclssBlast));
                        }
                    }
                    case "AEROSTAT_PUNCTURE" -> {
                        double aeroBlast = (mag / 10.0) * attenuation;
                        cell.setHabitatIntegrity(0.0);
                        cell.setHabitatType(HabitatType.NONE);
                        if (prevPop > 0) {
                            lostPop = (int) Math.round(prevPop * (1.0 - 0.95 * aeroBlast));
                        }
                    }
                    case "LAVA_TUBE_COLLAPSE" -> {
                        double collapseBlast = (mag / 10.0) * attenuation;
                        cell.setHabitatIntegrity(0.0);
                        if (prevPop > 0) {
                            lostPop = (int) Math.round(prevPop * (1.0 - 0.85 * collapseBlast));
                        }
                    }

                    default -> {
                        // Generic historical leader archetypes or custom interventions
                        if (upperType.startsWith("LEADER_")) {
                            applyLeaderArchetypeForcing(cell, upperType, mag, attenuation);
                        }
                    }
                }

                // Apply cohort and population loss synchronization
                if (lostPop > 0 && prevPop > 0) {
                    int remaining = Math.max(0, prevPop - lostPop);
                    H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(cell, remaining);
                    cell.setBiomassHuman((double) remaining);

                    if (agentBuffer != null) {
                        if (cellSurvival == null) {
                            cellSurvival = new float[totalCells];
                            Arrays.fill(cellSurvival, 1.0f);
                        }
                        cellSurvival[i] = (float) remaining / (float) prevPop;
                        anyAgentMortality = true;
                    }
                }

                // Synchronize cell states into DOD WorldBuffer if present
                if (worldBuffer != null && i < worldBuffer.getCapacity()) {
                    if (cell.getTemperature() != null) worldBuffer.getTemperature()[i] = cell.getTemperature().floatValue();
                    if (cell.getResourceCapital() != null) worldBuffer.getResourceCapital()[i] = cell.getResourceCapital().floatValue();
                    if (cell.getFoodResource() != null) worldBuffer.getFoodResource()[i] = cell.getFoodResource().floatValue();
                    if (cell.getPopulation() != null) worldBuffer.getBiomassHuman()[i] = cell.getPopulation().floatValue();
                }
            }
        }

        // Synchronize AgentBuffer if mortality occurred
        if (anyAgentMortality && agentBuffer != null && cellSurvival != null) {
            int cap = agentBuffer.getCapacity();
            int[] hexIds = agentBuffer.getHexIds();
            float[] mass = agentBuffer.getMass();
            float[] deaths = agentBuffer.getDeaths();
            for (int a = 0; a < cap; a++) {
                int hId = hexIds[a];
                if (hId >= 0 && hId < totalCells && mass[a] > 0.0f) {
                    float surv = cellSurvival[hId];
                    if (surv < 1.0f) {
                        float oldM = mass[a];
                        float newM = oldM * surv;
                        mass[a] = newM;
                        deaths[a] += (oldM - newM);
                    }
                }
            }
        }
    }

    /**
     * Calculates default impact radius in kilometers from typology and magnitude.
     */
    private static double calculateDefaultRadiusKm(String type, String name, double mag) {
        if (type.contains("PANDEMIC") || type.contains("PLAGUE") || type.contains("EPIDEMIC")) {
            return (name.contains("grippe") || name.contains("flu")) ? (180.0 * 111.32) : (85.0 * 111.32);
        }
        if (type.contains("VOLCANO") || type.contains("NUCLEAR")) {
            return Math.max(250.0, mag * 200.0);
        }
        if (type.contains("EARTHQUAKE") || type.contains("TSUNAMI")) {
            return Math.max(150.0, mag * 120.0);
        }
        if (type.contains("TECH_SINGULARITY") || type.contains("RENAISSANCE")) {
            return 3000.0;
        }
        return Math.max(50.0, mag * 100.0);
    }

    /**
     * Applies atmospheric stratospheric aerosol or soot optical depth to global climate engines.
     */
    private static void applyAtmosphericGlobalForcing(String type, double mag, int durationDays) {
        switch (type) {
            case "VOLCANO" -> {
                // Stratospheric sulfate aerosol optical depth injection: tau_aod += 0.05 * mag
                double addedTau = 0.05 * mag;
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(
                        NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() + addedTau);
            }
            case "NUCLEAR_STRIKE", "NUCLEAR_WINTER" -> {
                double addedSoot = 0.20 * mag;
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(
                        NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() + addedSoot);
            }
            case "METEOR" -> {
                double addedEjecta = 0.35 * mag;
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(
                        NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() + addedEjecta);
            }
            case "GEOENGINEERING" -> {
                // Mitigation of aerosol optical depth or controlled planetary cooling
                NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(
                        Math.max(0.0, NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() - 0.10 * mag));
            }
            default -> {}
        }
    }

    /**
     * Applies leadership archetype forcing on institutional and demographic cell parameters.
     */
    private static void applyLeaderArchetypeForcing(H3Cell cell, String type, double mag, double attenuation) {
        Polity owner = cell.getOwner();
        if (owner == null) return;
        double effect = (mag / 10.0) * attenuation;

        switch (type) {
            case "LEADER_MILITARY_CONQUEROR" -> {
                owner.setAsabiyyah(Math.min(1.0, owner.getAsabiyyah() + 0.15 * effect));
            }
            case "LEADER_INFRASTRUCTURE_BUILDER" -> {
                double cap = cell.getResourceCapitalOr(100.0);
                cell.setResourceCapital(cap + 1500.0 * effect);
                double fric = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
                cell.setMovementFriction(Math.max(0.2, fric * (1.0 - 0.25 * effect)));
            }
            case "LEADER_INSTITUTIONAL_REFORMER" -> {
                owner.setStateCapacity(Math.min(1.0, owner.getStateCapacity() + 0.20 * effect));
            }
            case "LEADER_HYDRAULIC_AGRARIAN_INNOVATOR" -> {
                double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
                cell.setFoodResource(food * (1.0 + 0.30 * effect));
            }
            case "LEADER_MORAL_RELIGIOUS_SAGE" -> {
                owner.setAsabiyyah(Math.min(1.0, owner.getAsabiyyah() + 0.12 * effect));
            }
            case "LEADER_TOTALITARIAN_PURGER" -> {
                owner.setStateCapacity(Math.min(1.0, owner.getStateCapacity() + 0.15 * effect));
                owner.setAsabiyyah(Math.max(0.0, owner.getAsabiyyah() - 0.10 * effect));
            }
            default -> {}
        }
    }

    /**
     * Executes localized terraforming brush outcome across cells in headless mode.
     */
    public record HeadlessBrushOutcome(H3Cell cell, String type, String title, String description, double magnitude) {}

    public static HeadlessBrushOutcome applyTerraformBrush(List<H3Cell> cells, WorldBuffer worldBuffer, int selectedIdx, double lat, double lng) {
        H3Cell nearest = findNearestCell(cells, lat, lng);
        if (nearest == null) return null;

        String brushType;
        String brushTitle;
        String brushDesc;
        double brushMag;

        switch (selectedIdx) {
            case 0 -> {
                nearest.setPopulation(nearest.getPopulationOr(0) + 50000);
                brushType = "POP_BOOST";
                brushTitle = I18n.getOrDefault("godmode.brush.pop", "👥 Boost Population (+50 000 hab)");
                brushDesc = String.format(Locale.ROOT, "Ajout de +50 000 habitants à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitudeOr(0.0), nearest.getLongitudeOr(0.0));
                brushMag = 50000.0;
            }
            case 1 -> {
                nearest.setFoodResource((nearest.getFoodResource() != null ? nearest.getFoodResource() : 0.0) + 500.0);
                brushType = "AGRI_BOOST";
                brushTitle = I18n.getOrDefault("godmode.brush.agri", "🌾 Injection Agricole & Silos (+500 t)");
                brushDesc = String.format(Locale.ROOT, "Apport de +500 t de céréales à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitudeOr(0.0), nearest.getLongitudeOr(0.0));
                brushMag = 500.0;
            }
            case 2 -> {
                nearest.setFreshwaterAquifer((nearest.getFreshwaterAquifer() != null ? nearest.getFreshwaterAquifer() : 0.0) + 2000.0);
                brushType = "WATER_RECHARGE";
                brushTitle = I18n.getOrDefault("godmode.brush.water", "🚰 Recharge Aquifère (+2 000 m³)");
                brushDesc = String.format(Locale.ROOT, "Recharge de nappe phréatique (+2 000 m³) à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitudeOr(0.0), nearest.getLongitudeOr(0.0));
                brushMag = 2000.0;
            }
            case 3 -> {
                nearest.setTemperature(nearest.getTemperatureOr(15.0) + 10.0);
                brushType = "HEAT_INJECTION";
                brushTitle = I18n.getOrDefault("godmode.brush.heat", "🔥 Vague de Chaleur Locale (+10,0°C)");
                brushDesc = String.format(Locale.ROOT, "Hausse thermique locale (+10.0°C) à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitudeOr(0.0), nearest.getLongitudeOr(0.0));
                brushMag = 10.0;
            }
            case 4 -> {
                nearest.setTemperature(nearest.getTemperatureOr(15.0) - 10.0);
                brushType = "COLD_INJECTION";
                brushTitle = I18n.getOrDefault("godmode.brush.cold", "❄️ Refroidissement Local (-10,0°C)");
                brushDesc = String.format(Locale.ROOT, "Refroidissement local (-10.0°C) à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitudeOr(0.0), nearest.getLongitudeOr(0.0));
                brushMag = 10.0;
            }
            default -> {
                nearest.setPollutionLevel(0.0);
                brushType = "CLEAN_ECO";
                brushTitle = I18n.getOrDefault("godmode.brush.clean", "🧼 Nettoyage Écologique Intégral");
                brushDesc = String.format(Locale.ROOT, "Dépollution intégrale à (Lat: %.2f°, Lng: %.2f°)", nearest.getLatitudeOr(0.0), nearest.getLongitudeOr(0.0));
                brushMag = 1.0;
            }
        }

        if (worldBuffer != null && cells != null) {
            DODDataGenerator.populateWorldBuffer(cells, worldBuffer);
        }

        return new HeadlessBrushOutcome(nearest, brushType, brushTitle, brushDesc, brushMag);
    }
}
