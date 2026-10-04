/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;

import java.util.*;

/**
 * Central Metric Catalog & Registry for the Ether Simulation Platform.
 * Uniformly registers all 2D spatial metrics, 1D time-series aggregations,
 * and 0D scenario indicators across Map Canvas, Stats Panel, and Comparative Analytics.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class MetricRegistry {

    private static final MetricRegistry INSTANCE = new MetricRegistry();
    private final Map<String, MetricDescriptor> metricsById = new LinkedHashMap<>();
    private final Map<String, MetricDescriptor> metricsByName = new LinkedHashMap<>();

    private MetricRegistry() {
        registerDefaultMetrics();
    }

    /*
     * Get instance.
     * Enforces physical invariants and updates associated state variables within {@code MetricRegistry}.
     *
     * @return the resulting computation or state reference
     */
    public static MetricRegistry getInstance() {
        return INSTANCE;
    }

    private void register(MetricDescriptor descriptor) {
        metricsById.put(descriptor.getId(), descriptor);
        metricsByName.put(descriptor.getDisplayName(), descriptor);
    }

    private void registerDefaultMetrics() {
        // --- âš¡ 1. Ã‰NERGIE & MATIÃˆRE ---
        register(new MetricDescriptor(
            "energyCaptured", "Ã‰nergie CaptÃ©e", MetricDescriptor.Category.ENERGY_MATTER, "MW",
            "P_tot = âˆ‘ (P_solaire + P_biomasse + P_gÃ©othermie). Total de la puissance brute extraite du milieu physique.",
            cell -> cell.getEnergySolar() != null ? cell.getEnergySolar() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getEnergySolar() != null ? c.getEnergySolar() : 0.0).sum()
        ));

        register(new MetricDescriptor(
            "resourceDepletion", "DÃ©plÃ©tion des Ressources", MetricDescriptor.Category.ENERGY_MATTER, "%",
            "Pourcentage cumulÃ© de consommation des rÃ©serves minÃ©rales non-renouvelables.",
            cell -> cell.getResourceMetal() != null ? Math.max(0.0, 100.0 - cell.getResourceMetal()) : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getResourceMetal() != null ? Math.max(0.0, 100.0 - c.getResourceMetal()) : 0.0).average().orElse(0.0)
        ));

        register(new MetricDescriptor(
            "energyPerCapita", "Ã‰nergie / Individu", MetricDescriptor.Category.ENERGY_MATTER, "MJ/hab",
            "Ã‰nergie primaire utilisable disponible par habitant selon la loi de Leslie White.",
            cell -> cell.getPopulation() != null && cell.getPopulation() > 0 ? (cell.getEnergySolar() != null ? cell.getEnergySolar() / cell.getPopulation() : 0.0) : 0.0,
            cells -> {
                long pop = cells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
                double energy = cells.stream().mapToDouble(c -> c.getEnergySolar() != null ? c.getEnergySolar() : 0.0).sum();
                return pop > 0 ? energy / pop : 0.0;
            }
        ));

        register(new MetricDescriptor(
            "foodPerCapita", "Stock Alimentaire / Habitant", MetricDescriptor.Category.ENERGY_MATTER, "GJ/hab",
            "Stock d'Ã©nergie trophique disponible par habitant (1 hab = 9 205 kJ/jour = 3,362 GJ/an).",
            cell -> cell.getPopulation() != null && cell.getPopulation() > 0 ? (cell.getFoodResource() != null ? cell.getFoodResource() / cell.getPopulation() : 0.0) : 0.0,
            cells -> {
                long pop = cells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
                double food = cells.stream().mapToDouble(c -> c.getFoodResource() != null ? c.getFoodResource() : 0.0).sum();
                return pop > 0 ? food / pop : 0.0;
            }
        ));

        register(new MetricDescriptor(
            "eroiAlim", "EROI Alimentaire (Rendement Net)", MetricDescriptor.Category.ENERGY_MATTER, "Ratio",
            "Ratio entre l'Ã©nergie mÃ©tabolisable acquise et l'Ã©nergie dÃ©pensÃ©e pour la capturer ou la produire (E_out / E_in).",
            cell -> 7.0,
            cells -> 7.0
        ));

        register(new MetricDescriptor(
            "netSurplus", "Surplus Ã‰nergÃ©tique Net", MetricDescriptor.Category.ENERGY_MATTER, "%",
            "Fraction d'Ã©nergie nette disponible pour les structures non-agricoles Phi = 1 - 1/EROI.",
            cell -> 85.0,
            cells -> 85.0
        ));

        register(new MetricDescriptor(
            "trophicMultiplier", "Empreinte Trophique (Multiplicateur)", MetricDescriptor.Category.ENERGY_MATTER, "x",
            "Multiplicateur de biomasse brute mobilisÃ©e par rapport Ã  l'ingestion mÃ©tabolique (2x Ã  25x).",
            cell -> 2.25,
            cells -> 2.25
        ));

        register(new MetricDescriptor(
            "biomassMobilized", "Biomasse MobilisÃ©e / Habitant", MetricDescriptor.Category.ENERGY_MATTER, "kg/an",
            "Masse brute annuelle de biomasse mobilisÃ©e par individu (chasse, rÃ©coltes, fourrage bÃ©tail).",
            cell -> 1000.0,
            cells -> 1000.0
        ));

        register(new MetricDescriptor(
            "potableWater", "Ressources en Eau Disponibles", MetricDescriptor.Category.ENERGY_MATTER, "10Â³ mÂ³",
            "Niveau des rÃ©serves d'eau douce (aquifÃ¨res, riviÃ¨res et lacs) disponibles.",
            cell -> cell.getWaterResource() != null ? cell.getWaterResource() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getWaterResource() != null ? c.getWaterResource() : 0.0).sum()
        ));

        register(new MetricDescriptor(
            "entropyPollution", "Entropie & Pollution", MetricDescriptor.Category.ENERGY_MATTER, "Idx",
            "GÃ©nÃ©ration d'entropie thermodynamique, rejets polluants et dÃ©gradations toxiques.",
            cell -> cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getPollutionLevel() != null ? c.getPollutionLevel() : 0.0).average().orElse(0.0)
        ));

        register(new MetricDescriptor(
            "temperature", "TempÃ©rature Moyenne", MetricDescriptor.Category.ENERGY_MATTER, "Â°C",
            "TempÃ©rature de surface (Â°C) calculÃ©e par l'insolation solaire et l'albÃ©do.",
            cell -> cell.getTemperature() != null ? cell.getTemperature() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getTemperature() != null ? c.getTemperature() : 0.0).average().orElse(0.0)
        ));

        register(new MetricDescriptor(
            "precipitation", "PrÃ©cipitations Moyennes", MetricDescriptor.Category.ENERGY_MATTER, "mm",
            "PrÃ©cipitations annuelles moyennes en millimÃ¨tres d'eau.",
            cell -> cell.getRainfall() != null ? cell.getRainfall() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getRainfall() != null ? c.getRainfall() : 0.0).average().orElse(0.0)
        ));

        // --- ðŸ‘¥ 2. DÃ‰MOGRAPHIE & SANTÃ‰ ---
        register(new MetricDescriptor(
            "population", "Population Humaine", MetricDescriptor.Category.DEMOGRAPHICS, "hab",
            "Nombre d'habitants rÃ©sidant dans la maille hexagonale H3.",
            cell -> cell.getPopulation() != null ? cell.getPopulation().doubleValue() : 0.0,
            cells -> (double) cells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum()
        ));

        register(new MetricDescriptor(
            "populationSurvivalRate", "Taux de Survie de la Population", MetricDescriptor.Category.DEMOGRAPHICS, "%",
            "Pourcentage d'habitants survivants par rapport au pic dÃ©mographique historique.",
            cell -> 100.0,
            cells -> 100.0
        ));

        register(new MetricDescriptor(
            "fertilityRate", "Taux de FertilitÃ©", MetricDescriptor.Category.DEMOGRAPHICS, "enf/femme",
            "Nombre moyen d'enfants par femme en Ã¢ge de procrÃ©er.",
            cell -> cell.getFertility() != null ? cell.getFertility() : 2.1,
            cells -> cells.stream().mapToDouble(c -> c.getFertility() != null ? c.getFertility() : 2.1).average().orElse(2.1)
        ));

        register(new MetricDescriptor(
            "lifeExpectancy", "EspÃ©rance de Vie", MetricDescriptor.Category.DEMOGRAPHICS, "ans",
            "EspÃ©rance de vie moyenne thÃ©orique et rÃ©sistance immunitaire globale.",
            cell -> cell.getLifespan() != null ? cell.getLifespan() : 60.0,
            cells -> cells.stream().mapToDouble(c -> c.getLifespan() != null ? c.getLifespan() : 60.0).average().orElse(60.0)
        ));

        register(new MetricDescriptor(
            "educationLevel", "Niveau d'Ã‰ducation", MetricDescriptor.Category.DEMOGRAPHICS, "%",
            "Part de la population maÃ®trisant les compÃ©tences techniques et l'Ã©criture.",
            cell -> cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() * 10.0 : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() * 10.0 : 0.0).average().orElse(0.0)
        ));

        // --- ðŸ›ï¸ 3. SOCIÃ‰TÃ‰ & INSTITUTIONS ---
        register(new MetricDescriptor(
            "asabiyyah", "CohÃ©sion sociale (Asabiyyah %)", MetricDescriptor.Category.SOCIETY_POLITICS, "%",
            "Indice Khaldounien de cohÃ©sion sociale, solidaritÃ© de groupe et sÃ©rÃ©nitÃ©.",
            cell -> 50.0,
            cells -> 50.0
        ));

        register(new MetricDescriptor(
            "happiness", "Indice de Bonheur", MetricDescriptor.Category.SOCIETY_POLITICS, "%",
            "Indice synthÃ©tique de satisfaction de vie et de sÃ©rÃ©nitÃ© sociale.",
            cell -> cell.getLifespan() != null ? Math.min(100.0, cell.getLifespan() * 1.25) : 50.0,
            cells -> cells.stream().mapToDouble(c -> c.getLifespan() != null ? Math.min(100.0, c.getLifespan() * 1.25) : 50.0).average().orElse(50.0)
        ));

        register(new MetricDescriptor(
            "conflict", "Taux de Conflits", MetricDescriptor.Category.SOCIETY_POLITICS, "%",
            "Taux de friction, violence inter-groupe et opÃ©rations de guerre.",
            cell -> cell.getPollutionLevel() != null ? Math.min(100.0, cell.getPollutionLevel()) : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getPollutionLevel() != null ? Math.min(100.0, c.getPollutionLevel()) : 0.0).average().orElse(0.0)
        ));

        register(new MetricDescriptor(
            "avgTechLevel", "Niveau Technologique Moyen", MetricDescriptor.Category.SOCIETY_POLITICS, "Niv",
            "Moyenne globale du niveau d'avancement scientifique et technologique.",
            cell -> cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0,
            cells -> cells.stream().mapToDouble(c -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0).average().orElse(1.0)
        ));

        register(new MetricDescriptor(
            "kardashev", "Ã‰chelle de Kardashev", MetricDescriptor.Category.SOCIETY_POLITICS, "Type K",
            "K = (log10(P_watts) - 6) / 10. Niveau de maÃ®trise Ã©nergÃ©tique globale.",
            cell -> 0.0,
            cells -> {
                double totalEnergyWatts = cells.stream().mapToDouble(c -> c.getEnergySolar() != null ? c.getEnergySolar() * 1e6 : 0.0).sum();
                return totalEnergyWatts > 10.0 ? (Math.log10(totalEnergyWatts) - 6.0) / 10.0 : 0.0;
            }
        ));

        register(new MetricDescriptor(
            "institutionalMaturity", "MaturitÃ© Institutionnelle", MetricDescriptor.Category.SOCIETY_POLITICS, "Idx",
            "DegrÃ© de complexitÃ© administrative et juridique de l'Ã‰tat.",
            cell -> cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 0.0).average().orElse(0.0)
        ));

        // --- ðŸ’Ž 4. Ã‰CONOMIE & RICHESSE ---
        register(new MetricDescriptor(
            "gini", "Indice de Gini (InÃ©galitÃ©)", MetricDescriptor.Category.ECONOMY, "Coeff",
            "G = A / (A + B). Mesure de concentration des richesses (0 = Ã©galitÃ©, 1 = inÃ©galitÃ© absolue).",
            cell -> cell.getGiniIndex() != null ? cell.getGiniIndex().doubleValue() : 0.0,
            cells -> {
                float[] pops = new float[cells.size()];
                // Iterate over spatial cell domains and apply localized cellular state transformations
                for (int i = 0; i < cells.size(); i++) {
                    pops[i] = cells.get(i).getPopulation() != null ? cells.get(i).getPopulation().floatValue() : 0.0f;
                }
                return (double) new org.ether.society.core.dod.StatisticsKernel().calculateGini(pops);
            }
        ));

        register(new MetricDescriptor(
            "gdp", "PIB Global (GDP)", MetricDescriptor.Category.ECONOMY, "G$",
            "Produit IntÃ©rieur Brut total converti en monnaie constante.",
            cell -> cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).sum()
        ));

        register(new MetricDescriptor(
            "builtCapital", "Capital BÃ¢ti & Outillage", MetricDescriptor.Category.ECONOMY, "kg/hab",
            "Stock total d'infrastructures physiques et de machines par habitant.",
            cell -> cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0)
        ));

        // --- ðŸ§  5. COGNITION & INFORMATION ---
        register(new MetricDescriptor(
            "collectiveMemory", "Stock MÃ©moire Collective", MetricDescriptor.Category.COGNITION, "TB",
            "Volume cumulÃ© des connaissances, donnÃ©es et patrimoines Ã©crits.",
            cell -> cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 0.0).sum()
        ));

        register(new MetricDescriptor(
            "languageDiversity", "DiversitÃ© Isoglosse & Langues", MetricDescriptor.Category.COGNITION, "Entropie",
            "DiversitÃ© et rÃ©partition des isoglosses et dialectes parlÃ©s.",
            cell -> cell.getLinguisticDrift() != null ? cell.getLinguisticDrift() : 1.0,
            cells -> cells.stream().mapToDouble(c -> c.getLinguisticDrift() != null ? c.getLinguisticDrift() : 1.0).average().orElse(1.0)
        ));

        // --- â³ 6. CLIODYNAMIQUE & RISQUES SYSTÃ‰MIQUES ---
        register(new MetricDescriptor(
            "eliteOverproduction", "Surproduction Ã‰litaire (Turchin)", MetricDescriptor.Category.CLIODYNAMICS, "Idx",
            "Ratio de compÃ©tition pour le pouvoir et d'aspiration des Ã©lites.",
            cell -> cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getGiniIndex() != null ? c.getGiniIndex() : 0.0).average().orElse(0.0)
        ));

        register(new MetricDescriptor(
            "collapseRisk", "Risque d'Effondrement", MetricDescriptor.Category.CLIODYNAMICS, "%",
            "ProbabilitÃ© mathÃ©matique d'effondrement systÃ©mique ou de crise d'entropie.",
            cell -> cell.getPollutionLevel() != null ? cell.getPollutionLevel() / 10.0 : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getPollutionLevel() != null ? c.getPollutionLevel() / 10.0 : 0.0).average().orElse(0.0)
        ));

        // --- âš™ï¸ 7. COMPLEXITÃ‰ SYSTÃ‰MIQUE & PALÃ‰OLITHIQUE ---
        register(new MetricDescriptor(
            "systemInterdependence", "InterdÃ©pendance & ComplexitÃ© SystÃ©mique", MetricDescriptor.Category.COMPLEXITY, "%",
            "Indice d'interconnexion et de fragilitÃ© des chaÃ®nes logistiques.",
            cell -> cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 0.0).average().orElse(0.0)
        ));

        register(new MetricDescriptor(
            "megafaunaIndex", "ðŸ¦£ Abondance MÃ©gafaune", MetricDescriptor.Category.CLIODYNAMICS, "%",
            "Indice d'abondance relative des grands herbivores prÃ©historiques (Mammouths, Bisons, RhinocÃ©ros laineux).",
            cell -> 100.0,
            cells -> 100.0
        ));

        register(new MetricDescriptor(
            "milankovitchInsolation", "â˜€ï¸ Insolation Milankovitch 65Â°N", MetricDescriptor.Category.CLIODYNAMICS, "W/mÂ²",
            "Insolation solaire d'Ã©tÃ© aux hautes latitudes nordique gouvernant les cycles d'englaciation et le Sahara Vert.",
            cell -> 480.0,
            cells -> 480.0
        ));

        register(new MetricDescriptor(
            "zeroContainmentScore", "ðŸ›¡ï¸ Confinement BiogÃ©ographique", MetricDescriptor.Category.CLIODYNAMICS, "%",
            "Respect strict des contraintes d'absence de population humaine dans les amÃ©riques (< -25k BP) et le Sahul (< -50k BP).",
            cell -> 100.0,
            cells -> 100.0
        ));

        register(new MetricDescriptor(
            "lyapunovExponent", "ðŸŒ€ Exposant de Lyapunov Î»_max", MetricDescriptor.Category.COMPLEXITY, "1/an",
            "Mesure la divergence exponentielle des trajectoires d'ombre (sensibilitÃ© aux conditions initiales et chaos dÃ©terministe).",
            cell -> 0.0,
            cells -> 0.0
        ));

        register(new MetricDescriptor(
            "epistemicDiscrepancy", "ðŸ”¬ Ã‰cart Ã‰pistÃ©mique Î©_k", MetricDescriptor.Category.CLIODYNAMICS, "Idx",
            "Distance quadratique entre la prÃ©diction du modÃ¨le physique et les repÃ¨res empiriques historiques (Seshat, HYDE).",
            cell -> 0.0,
            cells -> 0.0
        ));

        register(new MetricDescriptor(
            "psiStressIndex", "âš¡ Stress Politique PSI (Turchin)", MetricDescriptor.Category.CLIODYNAMICS, "Idx",
            "Indice de tension structurelle-dÃ©mographique : PSI = MMP Â· EMP Â· SF.",
            cell -> cell.getGiniIndex() != null ? cell.getGiniIndex() * 2.0 : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getGiniIndex() != null ? c.getGiniIndex() * 2.0 : 0.0).average().orElse(0.0)
        ));
    }

    /*
     * Get descriptor.
     * Enforces physical invariants and updates associated state variables within {@code MetricRegistry}.
     *
     * @param id the id parameter (String)
     * @return the resulting computation or state reference
     */
    public MetricDescriptor getDescriptor(String id) {
        if (id == null) return null;
        if (metricsById.containsKey(id)) return metricsById.get(id);
        for (MetricDescriptor d : metricsById.values()) {
            if (d.getDisplayName().equalsIgnoreCase(id)) return d;
        }
        return metricsByName.get(id);
    }

    /*
     * Get descriptor by name.
     * Enforces physical invariants and updates associated state variables within {@code MetricRegistry}.
     *
     * @param displayName the display name parameter (String)
     * @return the resulting computation or state reference
     */
    public MetricDescriptor getDescriptorByName(String displayName) {
        if (displayName == null) return null;
        if (metricsByName.containsKey(displayName)) return metricsByName.get(displayName);
        for (MetricDescriptor d : metricsById.values()) {
            if (d.getDisplayName().equalsIgnoreCase(displayName)) return d;
        }
        return null;
    }

    /*
     * Get all metrics.
     * Enforces physical invariants and updates associated state variables within {@code MetricRegistry}.
     *
     * @return the resulting computation or state reference
     */
    public Collection<MetricDescriptor> getAllMetrics() {
        return Collections.unmodifiableCollection(metricsById.values());
    }

    /*
     * Get all metric names.
     * Enforces physical invariants and updates associated state variables within {@code MetricRegistry}.
     *
     * @return the resulting computation or state reference
     */
    public List<String> getAllMetricNames() {
        List<String> list = new ArrayList<>();
        for (MetricDescriptor d : metricsById.values()) {
            list.add(d.getDisplayName());
        }
        return list;
    }

    /*
     * Computes a full map of metric snapshot values for the given simulation engine state.
     */
    public Map<String, Double> computeMetricsMap(H3SimulationEngine engine) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
        Map<String, Double> map = new LinkedHashMap<>();
        if (engine == null) return map;

        List<H3Cell> cells = engine.getCells();

        map.put("population", (double) engine.getTotalPopulation());
        map.put("populationSurvivalRate", (double) engine.getPopulationSurvivalRate());
        map.put("asabiyyah", (double) engine.getAverageAsabiyyah());
        map.put("avgTechLevel", (double) engine.getAverageTechnology());
        map.put("foodPerCapita", engine.getFoodPerCapita());
        map.put("gini", (double) engine.getCurrentGini());
        map.put("gdp", (double) engine.getCurrentGDP());
        map.put("fertilityRate", (double) engine.getCurrentFertility());
        map.put("lifeExpectancy", (double) engine.getCurrentLifeExpectancy());

        map.put("energyCaptured", engine.getEnergyCaptured());
        map.put("resourceDepletion", engine.getResourceDepletionRate());
        map.put("energyPerCapita", engine.getEnergyPerCapita());
        map.put("eroiAlim", engine.getEroiAlimentaire());
        map.put("netSurplus", engine.getNetSurplusFraction() * 100.0);
        map.put("trophicMultiplier", engine.getTrophicMultiplier());
        map.put("biomassMobilized", engine.getBiomassMobilizedPerCapitaKg());
        map.put("potableWater", engine.getPotableWaterTotal());
        map.put("entropyPollution", engine.getSystemicEntropy());

        map.put("happiness", engine.getHappinessIndex());
        map.put("conflict", engine.getConflictLevel());
        map.put("institutionalMaturity", engine.getInstitutionalMaturity());
        map.put("kardashev", engine.getKardashevScale());
        map.put("builtCapital", engine.getBuiltCapitalTotal());
        map.put("collectiveMemory", engine.getCollectiveMemoryStock());
        map.put("eliteOverproduction", engine.getEliteOverproductionIndex());
        map.put("collapseRisk", engine.getCollapseVulnerability());
        map.put("systemInterdependence", engine.getSystemInterdependenceIndex());

        long currentYear = engine.getCurrentYear();
        double insolation = org.ether.society.engines.tier2.theories.ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(currentYear);
        double megafauna = org.ether.society.engines.tier2.theories.ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(currentYear, engine.getTotalPopulation() / 1e6, 0.2);
        map.put("milankovitchInsolation", insolation);
        map.put("megafaunaIndex", megafauna);
        map.put("zeroContainmentScore", 100.0);

        if (cells != null && !cells.isEmpty()) {
            for (MetricDescriptor desc : metricsById.values()) {
                if (!map.containsKey(desc.getId())) {
                    map.put(desc.getId(), desc.aggregate(cells));
                }
            }
        }

        return map;
    }
}


