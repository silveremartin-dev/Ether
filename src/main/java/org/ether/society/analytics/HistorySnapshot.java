package org.ether.society.analytics;

/**
 * A comprehensive snapshot of global simulation statistics and cliodynamics at a specific point in time.
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
            case "Population Humaine", "population" -> totalPopulation;
            case "Nourriture / Individu", "Nourriture", "foodPerCapita" -> totalFood;
            case "Énergie Captée", "Énergie Captée (MW)", "energyCaptured" -> energyCaptured;
            case "Échelle de Kardashev", "Échelle de Kardashev (Type K)", "kardashevScale" -> kardashevScale;
            case "Indice de Gini (Inégalité)", "Indice de Gini (Inégalités)", "giniIndex" -> globalGini;
            case "Indice de Bonheur", "Indice de Bonheur (%)", "happinessIndex" -> happinessIndex;
            case "Taux de Conflits", "Taux de Conflits (%)", "conflictLevel" -> conflictLevel;
            case "PIB Global (GDP)", "gdpTotal" -> gdpTotal;
            case "Espérance de Vie", "Espérance de Vie (ans)", "lifeExpectancy" -> avgLifespan;
            case "Division du Travail", "Division du Travail & Spécialisation", "divisionLabor" -> divisionLabor;
            case "Complexité Systémique", "Interdépendance & Complexité Systémique", "systemComplexity" -> systemComplexity;
            case "Stock Mémoire Collective", "Stock de Mémoire Collective (TB)", "collectiveMemory" -> collectiveMemory;
            case "Empreinte Carbone", "Empreinte Carbone (GtCO2)", "carbonFootprint" -> carbonFootprint;
            case "Risque d'Effondrement", "Risque d'Effondrement Systémique (%)", "collapseVulnerability" -> collapseVulnerability;
            case "Niveau Technologique Moyen", "Niveau Technologique", "avgTechLevel" -> avgTechnology;
            case "Capital Bâti & Outillage", "Matière Déplacée & Capital Bâti", "builtCapital" -> totalWealth;
            case "Surproduction Élitaire (Turchin)", "Pression Élitaire (Indice Turchin)", "eliteOverproduction" -> eliteOverproduction;
            case "TPS (Images/s)", "engineTPS", "Fréquence de Calcul (Ticks/s)", "Fréquence de Calcul (ticks/sec)", "TPS", "tps" -> tps;
            case "Déplétion des Ressources", "resourceDepletion" -> resourceDepletion;
            case "Biomasse Naturelle", "biomassNatural" -> naturalBiomass;
            case "Eau Douce & Aquifères", "potableWater" -> potableWater;
            case "Taux de Fertilité", "fertilityRate" -> fertilityRate;
            case "Nombre de Cités-États", "cityStates" -> cityStates;
            default -> totalPopulation;
        };
    }
}
