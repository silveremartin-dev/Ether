/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Structured statistical attribution report quantifying the partial variance contribution
 * of individual Tier 2 cliodynamic engines on a simulation trajectory via deterministic
 * Leave-One-Out (LOO) ablation.
 *
 * <p>For each active Tier 2 engine \(E_i\), normalized trajectory deviations \(\Delta_{i,v}\)
 * between the full baseline run and the ablated run (\(E_i = \text{OFF}\)) are squared and
 * normalized across all tested engines to express variance share percentages:</p>
 * \[
 *   \%\,\text{Contrib}_{i,v} = \frac{\Delta_{i,v}^2}{\sum_{j} \Delta_{j,v}^2} \times 100
 * \]
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see Tier2EngineContributionAnalyzer
 */
public class EngineContributionReport {

    /**
     * Per-metric attribution breakdown for a single Tier 2 engine.
     *
     * @param metricKey        canonical telemetry metric identifier
     * @param varianceSharePct normalized share of total trajectory variance (%)
     * @param trajectoryRmsePct normalized root-mean-square trajectory deviation (%)
     * @param finalDeltaPct    percentage change at the final simulation year (%)
     */
    public record MetricContribution(
            String metricKey,
            double varianceSharePct,
            double trajectoryRmsePct,
            double finalDeltaPct
    ) {}

    /**
     * Complete attribution summary for a single Tier 2 engine module.
     *
     * @param engineId               unique Tier 2 engine identifier
     * @param wiringStatus           execution loop wiring classification
     * @param wiredInLoop            {@code true} if the engine executes in the current tick pipeline
     * @param globalVarianceSharePct mean variance share across active metrics (%)
     * @param divergenceYear         first year where ablating this engine shifts any core metric by \(\ge 1\%\), or {@code Integer.MIN_VALUE} if never
     * @param mapeDeltaVsReality     \(\text{MAPE}_{\text{ablated}} - \text{MAPE}_{\text{baseline}}\) against historical reality (&gt; 0 means engine improves empirical fit)
     * @param classification         effect tier ({@code Dominant}, {@code Moderate}, {@code Marginal}, {@code Inert})
     * @param metricContributions    per-metric variance shares
     */
    public record EngineEntry(
            String engineId,
            String wiringStatus,
            boolean wiredInLoop,
            double globalVarianceSharePct,
            int divergenceYear,
            double mapeDeltaVsReality,
            String classification,
            List<MetricContribution> metricContributions
    ) {
        /**
         * Resolves the variance share percentage for a specific metric key.
         *
         * @param key metric identifier
         * @return variance share in \([0, 100]\), or {@code 0.0} if not recorded
         */
        public double getMetricSharePct(String key) {
            if (metricContributions == null || key == null) return 0.0;
            for (MetricContribution mc : metricContributions) {
                if (key.equals(mc.metricKey())) {
                    return mc.varianceSharePct();
                }
            }
            return 0.0;
        }
    }

    private String scenarioName;
    private String runId;
    private long seed;
    private int startYear;
    private int endYear;
    private int h3Resolution;
    private int testedEngineCount;
    private int totalEnabledEngines;
    private List<EngineEntry> entries;

    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    public EngineContributionReport() {
        this.entries = Collections.emptyList();
    }

    /**
     * Constructs an immutable attribution report snapshot.
     *
     * @param scenarioName        scenario display name
     * @param runId               associated run identifier
     * @param seed                deterministic scenario seed
     * @param startYear           scenario start calendar year
     * @param endYear             scenario end calendar year
     * @param h3Resolution        H3 mesh resolution used for ablation
     * @param testedEngineCount   number of engines physically ablated
     * @param totalEnabledEngines total Tier 2 engines enabled on the scenario
     * @param entries             ordered list of per-engine contribution records
     */
    public EngineContributionReport(String scenarioName,
                                    String runId,
                                    long seed,
                                    int startYear,
                                    int endYear,
                                    int h3Resolution,
                                    int testedEngineCount,
                                    int totalEnabledEngines,
                                    List<EngineEntry> entries) {
        this.scenarioName = scenarioName;
        this.runId = runId;
        this.seed = seed;
        this.startYear = startYear;
        this.endYear = endYear;
        this.h3Resolution = h3Resolution;
        this.testedEngineCount = testedEngineCount;
        this.totalEnabledEngines = totalEnabledEngines;
        this.entries = entries != null ? List.copyOf(entries) : Collections.emptyList();
    }

    /** @return scenario name */
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    /** @return associated run identifier */
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    /** @return deterministic seed */
    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }

    /** @return start year */
    public int getStartYear() { return startYear; }
    public void setStartYear(int startYear) { this.startYear = startYear; }

    /** @return end year */
    public int getEndYear() { return endYear; }
    public void setEndYear(int endYear) { this.endYear = endYear; }

    /** @return H3 resolution */
    public int getH3Resolution() { return h3Resolution; }
    public void setH3Resolution(int h3Resolution) { this.h3Resolution = h3Resolution; }

    /** @return count of engines ablated */
    public int getTestedEngineCount() { return testedEngineCount; }
    public void setTestedEngineCount(int testedEngineCount) { this.testedEngineCount = testedEngineCount; }

    /** @return total enabled Tier 2 engines */
    public int getTotalEnabledEngines() { return totalEnabledEngines; }
    public void setTotalEnabledEngines(int totalEnabledEngines) { this.totalEnabledEngines = totalEnabledEngines; }

    /** @return ordered per-engine attribution entries */
    public List<EngineEntry> getEntries() { return entries != null ? entries : Collections.emptyList(); }
    public void setEntries(List<EngineEntry> entries) { this.entries = entries != null ? List.copyOf(entries) : Collections.emptyList(); }

    /**
     * Converts entries to an ordered lookup map keyed by engine ID.
     *
     * @return map of engineId to {@link EngineEntry}
     */
    public Map<String, EngineEntry> toEngineMap() {
        Map<String, EngineEntry> map = new LinkedHashMap<>();
        for (EngineEntry e : getEntries()) {
            map.put(e.engineId(), e);
        }
        return map;
    }

    /**
     * Renders this attribution report as a Markdown section suitable for embedding
     * into cliodynamic falsification and inter-scenario comparative reports.
     *
     * @return formatted Markdown section
     */
    public String toMarkdownSection() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
                "### ⚙️ Partial Contribution of Tier 2 Engines (`%s` — %d to %d, H3 Res %d, Seed %d)\n\n",
                scenarioName != null ? scenarioName : runId, startYear, endYear, h3Resolution, seed));
        sb.append(String.format(Locale.US,
                "> Deterministic Leave-One-Out (LOO) ablation across **%d** tested engines out of **%d** enabled Tier 2 engines. "
                        + "Variance shares are normalized squared trajectory deviations ($\\Delta^2 / \\sum \\Delta^2$); "
                        + "$\\Delta\\text{MAPE} = \\text{MAPE}_{\\text{ablated}} - \\text{MAPE}_{\\text{baseline}}$ (positive = engine improves empirical fit).\n\n",
                testedEngineCount, totalEnabledEngines));

        if (entries == null || entries.isEmpty()) {
            sb.append("- *No active Tier 2 engines were enabled in this scenario.*\n\n");
            return sb.toString();
        }

        sb.append("| Tier 2 Engine | Pipeline Status | Global Share (%) | Pop (%) | Capital/GDP (%) | Energy (%) | Gini (%) | Stability (%) | CO2 (%) | Onset ($T_{\\text{div}}$) | $\\Delta$MAPE vs Real | Classification |\n");
        sb.append("|---|---|---:|---:|---:|---:|---:|---:|---:|:---:|---:|---|\n");

        for (EngineEntry e : entries) {
            String divStr = e.divergenceYear() == Integer.MIN_VALUE
                    ? "—"
                    : (e.divergenceYear() < 0 ? String.format("%d BCE", -e.divergenceYear()) : String.format("%d CE", e.divergenceYear()));
            String deltaMapeStr = Double.isNaN(e.mapeDeltaVsReality())
                    ? "N/A"
                    : String.format(Locale.US, "%+.2f%%", e.mapeDeltaVsReality());

            sb.append(String.format(Locale.US,
                    "| `%s` | %s | **%.2f%%** | %.1f%% | %.1f%% | %.1f%% | %.1f%% | %.1f%% | %.1f%% | %s | %s | %s |\n",
                    e.engineId(),
                    e.wiringStatus(),
                    e.globalVarianceSharePct(),
                    e.getMetricSharePct("worldPopulation"),
                    e.getMetricSharePct("grossWorldProduct"),
                    e.getMetricSharePct("primaryEnergy"),
                    e.getMetricSharePct("giniInequality"),
                    e.getMetricSharePct("asabiyyahSocialCohesion"),
                    e.getMetricSharePct("co2Concentration"),
                    divStr,
                    deltaMapeStr,
                    e.classification()));
        }
        sb.append("\n");
        return sb.toString();
    }
}
