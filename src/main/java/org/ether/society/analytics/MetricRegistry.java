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
 * @version 1.0.0
 */
public class MetricRegistry {

    private static final MetricRegistry INSTANCE = new MetricRegistry();
    private final Map<String, MetricDescriptor> metricsById = new LinkedHashMap<>();
    private final Map<String, MetricDescriptor> metricsByName = new LinkedHashMap<>();

    private MetricRegistry() {
        registerDefaultMetrics();
    }

    public static MetricRegistry getInstance() {
        return INSTANCE;
    }

    private void register(MetricDescriptor descriptor) {
        metricsById.put(descriptor.getId(), descriptor);
        metricsByName.put(descriptor.getDisplayName(), descriptor);
    }

    private void registerDefaultMetrics() {
        // --- ⚡ 1. ÉNERGIE & MATIÈRE ---
        register(new MetricDescriptor(
            "energyCaptured", "Énergie Captée", MetricDescriptor.Category.ENERGY_MATTER, "MW",
            "P_tot = ∑ (P_solaire + P_biomasse + P_géothermie). Total de la puissance brute extraite du milieu physique.",
            cell -> cell.getEnergySolar() != null ? cell.getEnergySolar() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getEnergySolar() != null ? c.getEnergySolar() : 0.0).sum()
        ));

        register(new MetricDescriptor(
            "resourceDepletion", "Déplétion des Ressources", MetricDescriptor.Category.ENERGY_MATTER, "%",
            "Pourcentage cumulé de consommation des réserves minérales non-renouvelables.",
            cell -> cell.getResourceMetal() != null ? Math.max(0.0, 100.0 - cell.getResourceMetal()) : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getResourceMetal() != null ? Math.max(0.0, 100.0 - c.getResourceMetal()) : 0.0).average().orElse(0.0)
        ));

        register(new MetricDescriptor(
            "energyPerCapita", "Énergie / Individu", MetricDescriptor.Category.ENERGY_MATTER, "MJ/hab",
            "Énergie primaire utilisable disponible par habitant selon la loi de Leslie White.",
            cell -> cell.getPopulation() != null && cell.getPopulation() > 0 ? (cell.getEnergySolar() != null ? cell.getEnergySolar() / cell.getPopulation() : 0.0) : 0.0,
            cells -> {
                long pop = cells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
                double energy = cells.stream().mapToDouble(c -> c.getEnergySolar() != null ? c.getEnergySolar() : 0.0).sum();
                return pop > 0 ? energy / pop : 0.0;
            }
        ));

        register(new MetricDescriptor(
            "foodPerCapita", "Nourriture / Individu", MetricDescriptor.Category.ENERGY_MATTER, "mois/hab",
            "Autonomie métabolique résiduelle sans nouvelle récolte.",
            cell -> cell.getPopulation() != null && cell.getPopulation() > 0 ? (cell.getFoodResource() != null ? cell.getFoodResource() / cell.getPopulation() : 0.0) : 0.0,
            cells -> {
                long pop = cells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
                double food = cells.stream().mapToDouble(c -> c.getFoodResource() != null ? c.getFoodResource() : 0.0).sum();
                return pop > 0 ? food / pop : 0.0;
            }
        ));

        register(new MetricDescriptor(
            "potableWater", "Ressources en Eau Disponibles", MetricDescriptor.Category.ENERGY_MATTER, "10³ m³",
            "Niveau des réserves d'eau douce (aquifères, rivières et lacs) disponibles.",
            cell -> cell.getWaterResource() != null ? cell.getWaterResource() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getWaterResource() != null ? c.getWaterResource() : 0.0).sum()
        ));

        register(new MetricDescriptor(
            "entropyPollution", "Entropie & Pollution", MetricDescriptor.Category.ENERGY_MATTER, "Idx",
            "Génération d'entropie thermodynamique, rejets polluants et dégradations toxiques.",
            cell -> cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getPollutionLevel() != null ? c.getPollutionLevel() : 0.0).average().orElse(0.0)
        ));

        register(new MetricDescriptor(
            "temperature", "Température Moyenne", MetricDescriptor.Category.ENERGY_MATTER, "°C",
            "Température de surface (°C) calculée par l'insolation solaire et l'albédo.",
            cell -> cell.getTemperature() != null ? cell.getTemperature() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getTemperature() != null ? c.getTemperature() : 0.0).average().orElse(0.0)
        ));

        register(new MetricDescriptor(
            "precipitation", "Précipitations Moyennes", MetricDescriptor.Category.ENERGY_MATTER, "mm",
            "Précipitations annuelles moyennes en millimètres d'eau.",
            cell -> cell.getRainfall() != null ? cell.getRainfall() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getRainfall() != null ? c.getRainfall() : 0.0).average().orElse(0.0)
        ));

        // --- 👥 2. DÉMOGRAPHIE & SANTÉ ---
        register(new MetricDescriptor(
            "population", "Population Humaine", MetricDescriptor.Category.DEMOGRAPHICS, "hab",
            "Nombre d'habitants résidant dans la maille hexagonale H3.",
            cell -> cell.getPopulation() != null ? cell.getPopulation().doubleValue() : 0.0,
            cells -> (double) cells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum()
        ));

        register(new MetricDescriptor(
            "populationSurvivalRate", "Taux de Survie de la Population", MetricDescriptor.Category.DEMOGRAPHICS, "%",
            "Pourcentage d'habitants survivants par rapport au pic démographique historique.",
            cell -> 100.0,
            cells -> 100.0
        ));

        register(new MetricDescriptor(
            "fertilityRate", "Taux de Fertilité", MetricDescriptor.Category.DEMOGRAPHICS, "enf/femme",
            "Nombre moyen d'enfants par femme en âge de procréer.",
            cell -> cell.getFertility() != null ? cell.getFertility() : 2.1,
            cells -> cells.stream().mapToDouble(c -> c.getFertility() != null ? c.getFertility() : 2.1).average().orElse(2.1)
        ));

        register(new MetricDescriptor(
            "lifeExpectancy", "Espérance de Vie", MetricDescriptor.Category.DEMOGRAPHICS, "ans",
            "Espérance de vie moyenne théorique et résistance immunitaire globale.",
            cell -> cell.getLifespan() != null ? cell.getLifespan() : 60.0,
            cells -> cells.stream().mapToDouble(c -> c.getLifespan() != null ? c.getLifespan() : 60.0).average().orElse(60.0)
        ));

        register(new MetricDescriptor(
            "educationLevel", "Niveau d'Éducation", MetricDescriptor.Category.DEMOGRAPHICS, "%",
            "Part de la population maîtrisant les compétences techniques et l'écriture.",
            cell -> cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() * 10.0 : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() * 10.0 : 0.0).average().orElse(0.0)
        ));

        // --- 🏛️ 3. SOCIÉTÉ & INSTITUTIONS ---
        register(new MetricDescriptor(
            "asabiyyah", "Cohésion sociale (Asabiyyah %)", MetricDescriptor.Category.SOCIETY_POLITICS, "%",
            "Indice Khaldounien de cohésion sociale, solidarité de groupe et sérénité.",
            cell -> 50.0,
            cells -> 50.0
        ));

        register(new MetricDescriptor(
            "happiness", "Indice de Bonheur", MetricDescriptor.Category.SOCIETY_POLITICS, "%",
            "Indice synthétique de satisfaction de vie et de sérénité sociale.",
            cell -> cell.getLifespan() != null ? Math.min(100.0, cell.getLifespan() * 1.25) : 50.0,
            cells -> cells.stream().mapToDouble(c -> c.getLifespan() != null ? Math.min(100.0, c.getLifespan() * 1.25) : 50.0).average().orElse(50.0)
        ));

        register(new MetricDescriptor(
            "conflict", "Taux de Conflits", MetricDescriptor.Category.SOCIETY_POLITICS, "%",
            "Taux de friction, violence inter-groupe et opérations de guerre.",
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
            "kardashev", "Échelle de Kardashev", MetricDescriptor.Category.SOCIETY_POLITICS, "Type K",
            "K = (log10(P_watts) - 6) / 10. Niveau de maîtrise énergétique globale.",
            cell -> 0.0,
            cells -> {
                double totalEnergyWatts = cells.stream().mapToDouble(c -> c.getEnergySolar() != null ? c.getEnergySolar() * 1e6 : 0.0).sum();
                return totalEnergyWatts > 10.0 ? (Math.log10(totalEnergyWatts) - 6.0) / 10.0 : 0.0;
            }
        ));

        register(new MetricDescriptor(
            "institutionalMaturity", "Maturité Institutionnelle", MetricDescriptor.Category.SOCIETY_POLITICS, "Idx",
            "Degré de complexité administrative et juridique de l'État.",
            cell -> cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 0.0).average().orElse(0.0)
        ));

        // --- 💎 4. ÉCONOMIE & RICHESSE ---
        register(new MetricDescriptor(
            "gini", "Indice de Gini (Inégalité)", MetricDescriptor.Category.ECONOMY, "Coeff",
            "G = A / (A + B). Mesure de concentration des richesses (0 = égalité, 1 = inégalité absolue).",
            cell -> cell.getGiniIndex() != null ? cell.getGiniIndex().doubleValue() : 0.0,
            cells -> {
                float[] pops = new float[cells.size()];
                for (int i = 0; i < cells.size(); i++) {
                    pops[i] = cells.get(i).getPopulation() != null ? cells.get(i).getPopulation().floatValue() : 0.0f;
                }
                return (double) new org.ether.society.core.dod.StatisticsKernel().calculateGini(pops);
            }
        ));

        register(new MetricDescriptor(
            "gdp", "PIB Global (GDP)", MetricDescriptor.Category.ECONOMY, "G$",
            "Produit Intérieur Brut total converti en monnaie constante.",
            cell -> cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).sum()
        ));

        register(new MetricDescriptor(
            "builtCapital", "Capital Bâti & Outillage", MetricDescriptor.Category.ECONOMY, "kg/hab",
            "Stock total d'infrastructures physiques et de machines par habitant.",
            cell -> cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).average().orElse(0.0)
        ));

        // --- 🧠 5. COGNITION & INFORMATION ---
        register(new MetricDescriptor(
            "collectiveMemory", "Stock Mémoire Collective", MetricDescriptor.Category.COGNITION, "TB",
            "Volume cumulé des connaissances, données et patrimoines écrits.",
            cell -> cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 0.0).sum()
        ));

        register(new MetricDescriptor(
            "languageDiversity", "Diversité Isoglosse & Langues", MetricDescriptor.Category.COGNITION, "Entropie",
            "Diversité et répartition des isoglosses et dialectes parlés.",
            cell -> cell.getLinguisticDrift() != null ? cell.getLinguisticDrift() : 1.0,
            cells -> cells.stream().mapToDouble(c -> c.getLinguisticDrift() != null ? c.getLinguisticDrift() : 1.0).average().orElse(1.0)
        ));

        // --- ⏳ 6. CLIODYNAMIQUE & RISQUES SYSTÉMIQUES ---
        register(new MetricDescriptor(
            "eliteOverproduction", "Surproduction Élitaire (Turchin)", MetricDescriptor.Category.CLIODYNAMICS, "Idx",
            "Ratio de compétition pour le pouvoir et d'aspiration des élites.",
            cell -> cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getGiniIndex() != null ? c.getGiniIndex() : 0.0).average().orElse(0.0)
        ));

        register(new MetricDescriptor(
            "collapseRisk", "Risque d'Effondrement", MetricDescriptor.Category.CLIODYNAMICS, "%",
            "Probabilité mathématique d'effondrement systémique ou de crise d'entropie.",
            cell -> cell.getPollutionLevel() != null ? cell.getPollutionLevel() / 10.0 : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getPollutionLevel() != null ? c.getPollutionLevel() / 10.0 : 0.0).average().orElse(0.0)
        ));

        // --- ⚙️ 7. COMPLEXITÉ SYSTÉMIQUE & PALÉOLITHIQUE ---
        register(new MetricDescriptor(
            "systemInterdependence", "Interdépendance & Complexité Systémique", MetricDescriptor.Category.COMPLEXITY, "%",
            "Indice d'interconnexion et de fragilité des chaînes logistiques.",
            cell -> cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0,
            cells -> cells.stream().mapToDouble(c -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 0.0).average().orElse(0.0)
        ));

        register(new MetricDescriptor(
            "megafaunaIndex", "🦣 Abondance Mégafaune", MetricDescriptor.Category.CLIODYNAMICS, "%",
            "Indice d'abondance relative des grands herbivores préhistoriques (Mammouths, Bisons, Rhinocéros laineux).",
            cell -> 100.0,
            cells -> 100.0
        ));

        register(new MetricDescriptor(
            "milankovitchInsolation", "☀️ Insolation Milankovitch 65°N", MetricDescriptor.Category.CLIODYNAMICS, "W/m²",
            "Insolation solaire d'été aux hautes latitudes nordique gouvernant les cycles d'englaciation et le Sahara Vert.",
            cell -> 480.0,
            cells -> 480.0
        ));

        register(new MetricDescriptor(
            "zeroContainmentScore", "🛡️ Confinement Biogéographique", MetricDescriptor.Category.CLIODYNAMICS, "%",
            "Respect strict des contraintes d'absence de population humaine dans les amériques (< -25k BP) et le Sahul (< -50k BP).",
            cell -> 100.0,
            cells -> 100.0
        ));
    }

    public MetricDescriptor getDescriptor(String id) {
        if (id == null) return null;
        if (metricsById.containsKey(id)) return metricsById.get(id);
        for (MetricDescriptor d : metricsById.values()) {
            if (d.getDisplayName().equalsIgnoreCase(id)) return d;
        }
        return metricsByName.get(id);
    }

    public MetricDescriptor getDescriptorByName(String displayName) {
        if (displayName == null) return null;
        if (metricsByName.containsKey(displayName)) return metricsByName.get(displayName);
        for (MetricDescriptor d : metricsById.values()) {
            if (d.getDisplayName().equalsIgnoreCase(displayName)) return d;
        }
        return null;
    }

    public Collection<MetricDescriptor> getAllMetrics() {
        return Collections.unmodifiableCollection(metricsById.values());
    }

    public List<String> getAllMetricNames() {
        List<String> list = new ArrayList<>();
        for (MetricDescriptor d : metricsById.values()) {
            list.add(d.getDisplayName());
        }
        return list;
    }

    /**
     * Computes a full map of metric snapshot values for the given simulation engine state.
     */
    public Map<String, Double> computeMetricsMap(H3SimulationEngine engine) {
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
        double insolation = org.ether.society.procedural.ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(currentYear);
        double megafauna = org.ether.society.procedural.ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(currentYear, engine.getTotalPopulation() / 1e6, 0.2);
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
