package org.ether.society.analytics;

/**
 * <h1>History Snapshot</h1>
 * <p>
 * Provides statistical analytics, empirical validation harnesses, and parameter calibration kernels.<br>
 * Evaluates simulation trajectories using multi-metric error metrics (Mean Absolute Percentage Error, Pearson correlation, and spatial centroid divergence).
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public record HistorySnapshot(
        int year,
        int month,
        long totalPopulation,
        double totalFood,
        double totalWealth,
        double avgLifespan,
        double globalGini,
        double avgTechnology,
        double energyCaptured,
        double kardashevScale,
        double happinessIndex,
        double conflictLevel,
        double gdpTotal,
        double divisionLabor,
        double systemComplexity,
        double collectiveMemory,
        double carbonFootprint,
        double collapseVulnerability,
        double tps,
        double resourceDepletion,
        double naturalBiomass,
        double potableWater,
        double fertilityRate,
        int cityStates,
        double eliteOverproduction) {

    /**
     * Backward-compatible 8-parameter constructor.
     */
    public HistorySnapshot(int year, int month, long totalPopulation, double totalFood, double totalWealth,
                           double avgLifespan, double globalGini, double avgTechnology) {
        this(year, month, totalPopulation, totalFood, totalWealth, avgLifespan, globalGini, avgTechnology,
             totalPopulation * (10.0 + avgTechnology * 0.5), (Math.log10(Math.max(1.0, totalPopulation * 1000.0)) - 6) / 10.0,
             75.0, 5.0, totalWealth * 1.5, 15.0, 20.0, 100.0, 0.5, 5.0, 60.0, 0.0, 5000.0, 20000.0, 3.5, 1, 0.1);
    }

    /**
     * Extracts numerical value for the given metric label or key.
     */
    public double getMetricValue(String metric) {
        if (metric == null) return totalPopulation;
        return switch (metric) {
            case "Human Population", "Population Humaine", "population" -> totalPopulation;
            case "Food Stock per Capita", "Nourriture / Individu", "Nourriture", "foodPerCapita" -> totalFood;
            case "Captured Primary Energy", "Énergie Captée", "Énergie Captée (MW)", "energyCaptured" -> energyCaptured;
            case "Kardashev Scale", "Échelle de Kardashev", "Échelle de Kardashev (Type K)", "kardashevScale", "kardashev" -> kardashevScale;
            case "Gini Inequality Index", "Indice de Gini (Inégalité)", "Indice de Gini (Inégalités)", "giniIndex", "gini" -> globalGini;
            case "Happiness Index", "Indice de Bonheur", "Indice de Bonheur (%)", "happinessIndex", "happiness" -> happinessIndex;
            case "Conflict & Warfare Intensity", "Taux de Conflits", "Taux de Conflits (%)", "conflictLevel", "conflict" -> conflictLevel;
            case "Gross World Product (GDP)", "PIB Global (GDP)", "gdpTotal", "gdp" -> gdpTotal;
            case "Life Expectancy at Birth", "Espérance de Vie", "Espérance de Vie (ans)", "lifeExpectancy" -> avgLifespan;
            case "Division of Labor", "Division du Travail", "Division du Travail & Spécialisation", "divisionLabor" -> divisionLabor;
            case "Systemic Complexity", "Complexité Systémique", "Interdépendance & Complexité Systémique", "systemComplexity" -> systemComplexity;
            case "Collective Memory Stock", "Stock Mémoire Collective", "Stock de Mémoire Collective (TB)", "collectiveMemory" -> collectiveMemory;
            case "Carbon Footprint", "Empreinte Carbone", "Empreinte Carbone (GtCO2)", "carbonFootprint" -> carbonFootprint;
            case "Systemic Collapse Risk", "Risque d'Effondrement", "Risque d'Effondrement Systémique (%)", "collapseVulnerability" -> collapseVulnerability;
            case "Average Technological Level", "Niveau Technologique Moyen", "Niveau Technologique", "avgTechLevel" -> avgTechnology;
            case "Built Capital & Tooling", "Capital Bâti & Outillage", "Matière Déplacée & Capital Bâti", "builtCapital" -> totalWealth;
            case "Elite Overproduction", "Surproduction Élitaire (Turchin)", "Pression Élitaire (Indice Turchin)", "eliteOverproduction" -> eliteOverproduction;
            case "Simulation Frequency (TPS)", "TPS (Images/s)", "engineTPS", "Fréquence de Calcul (Ticks/s)", "Fréquence de Calcul (ticks/sec)", "TPS", "tps" -> tps;
            case "Resource Depletion Rate", "Déplétion des Ressources", "resourceDepletion" -> resourceDepletion;
            case "Natural Biomass", "Biomasse Naturelle", "biomassNatural" -> naturalBiomass;
            case "Available Water Resources", "Eau Douce & Aquifères", "potableWater" -> potableWater;
            case "Total Fertility Rate", "Taux de Fertilité", "fertilityRate" -> fertilityRate;
            case "Number of City-States", "Nombre de Cités-États", "cityStates" -> cityStates;
            case "Political Stress Index (PSI)", "Indice de Stress Politique (PSI)", "Indice PSI de Turchin", "turchinPsi", "turchinPSI", "PSI" -> eliteOverproduction * (1.0 + globalGini) * Math.max(0.5, conflictLevel / 20.0);
            case "Carrying Capacity Saturation (N/K)", "Saturation Capacité Portante (N/K)", "carryingCapacitySat", "carryingCapacityRatio", "pressionMalthusienne", "Pression Malthusienne" -> totalFood > 0 ? (totalPopulation * 3.362) / totalFood : 1.0;
            case "Social Cohesion (Asabiyyah)", "Cohésion Asabiyyah", "asabiyyah", "Asabiyyah (Cohésion)" -> Math.max(0.0, Math.min(100.0, 100.0 - conflictLevel - globalGini * 40.0));
            case "Planetary Overshoot", "Dépassement Planétaire (Overshoot)", "planetaryOvershoot", "Empreinte Écologique" -> (carbonFootprint * 2.0 + resourceDepletion) / 100.0;
            case "Food EROI (Net Yield Ratio)", "EROI Énergétique", "energyEroi", "eroiAlim", "EROI Alimentaire (Rendement Net)" -> Math.max(1.0, 15.0 - (resourceDepletion * 0.1));
            case "Land Inequality (Land Gini)", "Inégalité Foncière (Gini Sol)", "landGini" -> Math.min(1.0, globalGini * 1.15);
            case "Soil NPK Quality", "Qualité NPK des Sols", "soilNPK" -> Math.max(10.0, 100.0 - resourceDepletion * 0.5);
            default -> totalPopulation;
        };
    }
}
