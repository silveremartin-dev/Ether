/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;

import java.io.Serializable;
import java.util.*;

/**
 * <h1>SpatialPhaseTransitionChronicleObserver</h1>
 * <p>
 * Evaluates spatial and macrosociological phase transitions across the active simulation domain
 * to enrich generative historical chronicles. Detects emergent urban concentrations, acute
 * demographic collapses, local golden ages of material capital accumulation, and systemic
 * institutional bifurcations.
 * </p>
 * <p>
 * Calibrated against peer-reviewed cliodynamic models (Zipf's urban rank-size law, Bairoch 1988
 * historical urbanization thresholds, Peter Turchin 2016 Structural-Demographic Theory PSI indices,
 * and Garrett 2011 thermodynamic capital-exergy coupling).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class SpatialPhaseTransitionChronicleObserver implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Evaluation cadence: observe phase transitions every N simulation years. */
    public static final int OBSERVATION_INTERVAL_YEARS = 1;

    /** Maximum chronicle events emitted per annual observation cycle to prevent spam. */
    public static final int MAX_EVENTS_PER_CYCLE = 2;

    /** Canonical urban agglomeration threshold tiers (Bairoch 1988, Zipf 1949). */
    private static final int[] CANONICAL_URBAN_TIERS = {
        5_000,       // Neolithic / Proto-urban settlement (Jericho, Çatalhöyük)
        25_000,      // Early Bronze Age city (Uruk, Ur, Eridu)
        100_000,     // Classical metropolis (Athens, Alexandria, Chang'an)
        500_000,     // Imperial megacity (Rome, Baghdad, Kaifeng)
        1_000_000,   // Early modern giant (Beijing, London, Edo)
        5_000_000,   // Industrial super-conurbation
        10_000_000   // Modern hyper-metropolis
    };

    private int lastEvaluationYear = Integer.MIN_VALUE;
    private final HashSet<Integer> firedUrbanTiers = new HashSet<>();
    private final HashMap<Integer, Integer> previousPopulations = new HashMap<>();
    private final HashMap<Integer, Integer> cellCollapseCooldowns = new HashMap<>();
    private final HashSet<String> firedPolityCrises = new HashSet<>();
    private double highestRecordedCapitalPerCapita = 150.0;
    private int highestDomainPopulation = 0;

    /**
     * Resets all internal caches, cooldowns, and historical tracking state.
     */
    public void reset() {
        lastEvaluationYear = Integer.MIN_VALUE;
        firedUrbanTiers.clear();
        previousPopulations.clear();
        cellCollapseCooldowns.clear();
        firedPolityCrises.clear();
        highestRecordedCapitalPerCapita = 150.0;
        highestDomainPopulation = 0;
    }

    /**
     * Evaluates spatial phase transitions across active domain cells and emits calibrated chronicle events.
     *
     * @param year simulation calendar year
     * @param month simulation calendar month (1-12)
     * @param cells active simulation mesh
     * @param eventSystem parent event coordinator
     * @param scenario active scenario definition
     * @param random seeded pseudo-random generator
     */
    public void evaluatePhaseTransitions(
            int year,
            int month,
            List<H3Cell> cells,
            EventSystem eventSystem,
            Scenario scenario,
            Random random) {

        if (cells == null || cells.isEmpty() || eventSystem == null) {
            return;
        }

        // Only evaluate on the first month (0 or 1) of each designated observation year
        if (month > 1) {
            return;
        }
        if (lastEvaluationYear != Integer.MIN_VALUE && (year - lastEvaluationYear) < OBSERVATION_INTERVAL_YEARS) {
            return;
        }
        lastEvaluationYear = year;

        // Clip domain to scenario bounding box if clipping is active
        List<H3Cell> domainCells = filterDomainCells(cells, scenario);
        if (domainCells.isEmpty()) {
            return;
        }

        List<ActiveEvent> candidateEvents = new ArrayList<>();

        // 1. Urban Agglomeration Transitions (Bairoch 1988)
        evaluateUrbanTransitions(year, month, domainCells, candidateEvents);

        // 2. Local Demographic Collapses
        evaluateDemographicCollapses(year, month, domainCells, candidateEvents);

        // 3. Regional Golden Ages (Capital per Capita spikes)
        evaluateGoldenAges(year, month, domainCells, candidateEvents);

        // 4. Institutional & Elite Overproduction Crises (Turchin PSI)
        evaluateInstitutionalCrises(year, month, domainCells, candidateEvents);

        // Update baseline populations for collapse detection
        updatePopulationBaselines(domainCells);

        // Limit rate of chronicle events to prevent log spam
        int countToEmit = Math.min(MAX_EVENTS_PER_CYCLE, candidateEvents.size());
        for (int i = 0; i < countToEmit; i++) {
            eventSystem.recordSpatialEvent(candidateEvents.get(i));
        }
    }

    /**
     * Backward-compatible alias for evaluatePhaseTransitions.
     */
    public void evaluateSingularities(
            int year,
            int month,
            List<H3Cell> cells,
            EventSystem eventSystem,
            Scenario scenario,
            Random random) {
        evaluatePhaseTransitions(year, month, cells, eventSystem, scenario, random);
    }

    private List<H3Cell> filterDomainCells(List<H3Cell> cells, Scenario scenario) {
        if (scenario == null || !scenario.isClippingEnabled()) {
            return cells;
        }
        double minLat = scenario.getMinLat();
        double maxLat = scenario.getMaxLat();
        double minLng = scenario.getMinLng();
        double maxLng = scenario.getMaxLng();

        List<H3Cell> filtered = new ArrayList<>();
        for (H3Cell c : cells) {
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            if (lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng) {
                filtered.add(c);
            }
        }
        return filtered;
    }

    private void evaluateUrbanTransitions(
            int year,
            int month,
            List<H3Cell> cells,
            List<ActiveEvent> candidates) {

        H3Cell maxCityCell = null;
        int maxPop = 0;

        for (H3Cell c : cells) {
            if (c == null || c.getBiome() == Biome.OCEAN || c.getBiome() == Biome.DEEP_OCEAN) continue;
            Integer pop = c.getPopulation();
            if (pop != null && pop > maxPop) {
                maxPop = pop;
                maxCityCell = c;
            }
        }

        if (maxCityCell == null || maxPop < CANONICAL_URBAN_TIERS[0]) {
            return;
        }

        int highestCrossedTier = -1;
        for (int tier : CANONICAL_URBAN_TIERS) {
            if (maxPop >= tier && !firedUrbanTiers.contains(tier)) {
                highestCrossedTier = tier;
            }
        }

        if (highestCrossedTier > 0) {
            for (int tier : CANONICAL_URBAN_TIERS) {
                if (tier <= highestCrossedTier) {
                    firedUrbanTiers.add(tier);
                }
            }
            double magnitude = Math.min(9.0, 5.0 + Math.log10(highestCrossedTier / 1000.0) * 1.2);
            String title = String.format(Locale.ROOT,
                    "🏛️ ÉMERGENCE URBAINE : Une métropole franchit le seuil de %,d habitants!", highestCrossedTier);
            candidates.add(new ActiveEvent(
                    "URBAN_TRANSITION_" + highestCrossedTier + "_" + year,
                    title,
                    "CHRONICLE_URBAN_EMERGENCE",
                    maxCityCell.getLatitude(),
                    maxCityCell.getLongitude(),
                    year, month, 1,
                    25.0,
                    magnitude
            ));
        }
    }

    private void evaluateDemographicCollapses(
            int year,
            int month,
            List<H3Cell> cells,
            List<ActiveEvent> candidates) {

        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            if (c == null) continue;
            Integer currentPop = c.getPopulation();
            if (currentPop == null) continue;

            Integer prevPop = previousPopulations.get(i);
            if (prevPop == null || prevPop < 800) continue;

            // Check if cell is in cooldown (avoid repeating collapse every year for ongoing crisis)
            Integer lastCollapse = cellCollapseCooldowns.get(i);
            if (lastCollapse != null && (year - lastCollapse) < 15) {
                continue;
            }

            double relativeDelta = (double) (currentPop - prevPop) / prevPop;
            // Phase transition drop: loss of 40% or more of population in observation period
            if (relativeDelta <= -0.40) {
                cellCollapseCooldowns.put(i, year);
                double lossPct = Math.abs(relativeDelta) * 100.0;
                double magnitude = Math.min(8.8, 5.5 + (lossPct / 100.0) * 2.5);
                String title = String.format(Locale.ROOT,
                        "📉 EFFONDREMENT DÉMOGRAPHIQUE : Décroissance brutale de -%.0f%% de la population locale!", lossPct);
                candidates.add(new ActiveEvent(
                        "COLLAPSE_TRANSITION_" + i + "_" + year,
                        title,
                        "CHRONICLE_CRISIS",
                        c.getLatitude(),
                        c.getLongitude(),
                        year, month, 1,
                        25.0,
                        magnitude
                ));
                break; // At most one acute collapse candidate per cycle
            }
        }
    }

    private void evaluateGoldenAges(
            int year,
            int month,
            List<H3Cell> cells,
            List<ActiveEvent> candidates) {

        H3Cell peakCell = null;
        double highestLocalCapitalPerCapita = 0.0;

        for (H3Cell c : cells) {
            if (c == null || c.getPopulation() == null || c.getPopulation() < 500) continue;
            Double capital = c.getResourceCapital();
            if (capital == null || capital <= 0) continue;
            double capPerCapita = capital / c.getPopulation();
            if (capPerCapita > highestLocalCapitalPerCapita) {
                highestLocalCapitalPerCapita = capPerCapita;
                peakCell = c;
            }
        }

        // New landmark record: exceeds previous historical record by at least 50%
        if (peakCell != null && highestLocalCapitalPerCapita >= highestRecordedCapitalPerCapita * 1.50) {
            highestRecordedCapitalPerCapita = highestLocalCapitalPerCapita;
            double magnitude = Math.min(8.5, 5.5 + Math.log10(highestLocalCapitalPerCapita / 100.0));
            String title = String.format(Locale.ROOT,
                    "✨ ÂGE D'OR RÉGIONAL : Concentration record de capital matériel (%.0f unités/hab)!",
                    highestLocalCapitalPerCapita);
            candidates.add(new ActiveEvent(
                    "GOLDEN_AGE_TRANSITION_" + year,
                    title,
                    "CHRONICLE_GOLDEN_AGE",
                    peakCell.getLatitude(),
                    peakCell.getLongitude(),
                    year, month, 1,
                    25.0,
                    magnitude
            ));
        }
    }

    private void evaluateInstitutionalCrises(
            int year,
            int month,
            List<H3Cell> cells,
            List<ActiveEvent> candidates) {

        Set<Polity> evaluatedPolities = new HashSet<>();
        for (H3Cell c : cells) {
            if (c == null) continue;
            Polity owner = c.getOwner();
            if (owner != null && evaluatedPolities.add(owner)) {
                double psi = owner.getPoliticalInstability();
                if (psi >= 0.80 && !firedPolityCrises.contains(owner.getId() + "_" + (year / 20))) {
                    firedPolityCrises.add(owner.getId() + "_" + (year / 20));
                    double lat = c.getLatitude();
                    double lng = c.getLongitude();
                    String title = String.format(Locale.ROOT,
                            "⚔️ CRISE INSTITUTIONNELLE : Fracture politique et surproduction des élites (PSI: %.2f) dans %s!",
                            psi, owner.getName());
                    candidates.add(new ActiveEvent(
                            "INSTITUTIONAL_CRISIS_" + owner.getId() + "_" + year,
                            title,
                            "CHRONICLE_INSTITUTIONAL",
                            lat, lng,
                            year, month, 1,
                            25.0,
                            7.2
                    ));
                    break;
                }
            }
        }
    }

    private void updatePopulationBaselines(List<H3Cell> cells) {
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            if (c != null && c.getPopulation() != null) {
                previousPopulations.put(i, c.getPopulation());
            }
        }
    }

    public boolean isTierFired(int tier) {
        return firedUrbanTiers.contains(tier);
    }
}
