/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Pluggable Statistics & Formulas Registry for Ether.
 * Manages custom user-defined statistical formulas, calculates metrics over H3 cells / WorldBuffer,
 * and supports import/export to JSON / Properties files.
 *
 * @author Silvere Martin-Michiellot
 */
public class PluggableStatEngine {
    private static final Logger logger = LoggerFactory.getLogger(PluggableStatEngine.class);

    public static class StatDefinition {
        private String id;
        private String name;
        private String category;
        private String expression;
        private String unit;
        private String description;
        private boolean builtin;

        public StatDefinition() {}

        public StatDefinition(String id, String name, String category, String expression, String unit, String description, boolean builtin) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.expression = expression;
            this.unit = unit;
            this.description = description;
            this.builtin = builtin;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isBuiltin() { return builtin; }
        public void setBuiltin(boolean builtin) { this.builtin = builtin; }
    }

    private final Map<String, StatDefinition> registeredStats = new LinkedHashMap<>();
    private final FormulaEvaluator evaluator = new FormulaEvaluator();

    public PluggableStatEngine() {
        registerBuiltinDefaults();
    }

    private void registerBuiltinDefaults() {
        registerStat(new StatDefinition(
                "custom_gini_wealth",
                "Indice de Gini Richesse / PIB",
                "💎 Économie & Richesse",
                "GINI(wealth)",
                "Coeff",
                "Distribution d'inégalité de la richesse individuelle (0 = égalité absolue, 1 = inégalité totale)",
                true
        ));

        registerStat(new StatDefinition(
                "custom_variance_wealth",
                "Variance de Richesse Entre Individus",
                "💎 Économie & Richesse",
                "VAR(wealth)",
                "G$^2",
                "Statistique de variance entre individus sur la variable richesse : à quel point les gens s'éloignent du schéma standard",
                true
        ));

        registerStat(new StatDefinition(
                "custom_stddev_food",
                "Écart-Type Distribution Nourriture",
                "🌾 Énergie & Matière",
                "STDDEV(food)",
                "t",
                "Écart-type de la quantité de nourriture disponible par maille/individu par rapport à la moyenne",
                true
        ));

        registerStat(new StatDefinition(
                "custom_food_per_capita",
                "Nourriture Moyenne Par Habitant",
                "🌾 Énergie & Matière",
                "SUM(food) / COUNT(population)",
                "t/hab",
                "Formule calculée : Somme totale des réserves alimentaires divisée par le nombre de mailles/habitants",
                true
        ));

        registerStat(new StatDefinition(
                "custom_pop_density_variance",
                "Variance Densité de Population",
                "👥 Démographie & Santé",
                "VAR(population)",
                "hab^2",
                "Variance de la répartition de densité de population entre les zones du terrarium",
                true
        ));

        registerStat(new StatDefinition(
                "custom_median_age",
                "Âge Médian de la Population",
                "👥 Démographie & Santé",
                "MEDIAN(age)",
                "ans",
                "Valeur médiane séparant la moitié la plus jeune de la moitié la plus âgée",
                true
        ));
    }

    public void registerStat(StatDefinition stat) {
        if (stat != null && stat.getId() != null) {
            registeredStats.put(stat.getId(), stat);
            logger.info("Registered pluggable statistic formula: {} -> {}", stat.getName(), stat.getExpression());
        }
    }

    public void unregisterStat(String id) {
        StatDefinition def = registeredStats.get(id);
        if (def != null && !def.isBuiltin()) {
            registeredStats.remove(id);
            logger.info("Unregistered custom statistic: {}", id);
        }
    }

    public Collection<StatDefinition> getRegisteredStats() {
        return Collections.unmodifiableCollection(registeredStats.values());
    }

    public StatDefinition getStat(String id) {
        return registeredStats.get(id);
    }

    /**
     * Computes the numerical value of a formula over H3 cell data or WorldBuffer.
     */
    public double computeValue(String expression, List<H3Cell> cells, WorldBuffer buffer) {
        if (expression == null || expression.isBlank()) return 0.0;

        FormulaEvaluator.VariableResolver arrayResolver = varName -> extractVariableArray(varName, cells, buffer);
        FormulaEvaluator.ScalarResolver scalarResolver = varName -> computeScalarVariable(varName, cells, buffer);

        return evaluator.evaluate(expression, arrayResolver, scalarResolver);
    }

    public float[] extractVariableArray(String varName, List<H3Cell> cells, WorldBuffer buffer) {
        String key = varName.toLowerCase(Locale.ROOT).trim();

        if (buffer != null) {
            int cap = buffer.getCapacity();
            float[] res = new float[cap];
            switch (key) {
                case "wealth":
                case "gdp":
                case "capital":
                    return buffer.getBiomassHuman(); // Approximate human biomass/wealth in buffer
                case "population":
                case "pop":
                    return buffer.getBiomassHuman();
                case "food":
                    return buffer.getBiomassNatural();
                case "water":
                case "rainfall":
                case "rain":
                    return buffer.getRainfall();
                case "temperature":
                case "temp":
                    return buffer.getTemperature();
                case "elevation":
                case "alt":
                    return buffer.getElevation();
                case "age":
                    return buffer.getBiomassHuman();
                default:
                    break;
            }
        }

        if (cells != null && !cells.isEmpty()) {
            int size = cells.size();
            float[] res = new float[size];
            for (int i = 0; i < size; i++) {
                H3Cell c = cells.get(i);
                switch (key) {
                    // --- Socio-Economic & Capital ---
                    case "wealth":
                    case "gdp":
                    case "capital":
                    case "resourcecapital":
                        double popW = c.getPopulation() != null ? c.getPopulation() : 0.0;
                        double techW = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
                        double capW = c.getResourceCapital() != null && c.getResourceCapital() > 0 ? c.getResourceCapital() : popW * techW * 15.0;
                        res[i] = (float) capW;
                        break;
                    case "population":
                    case "pop":
                    case "biomasshuman":
                        res[i] = c.getPopulation() != null ? c.getPopulation().floatValue() : 0.0f;
                        break;
                    case "tech":
                    case "technology":
                    case "technologylevel":
                        res[i] = c.getTechnologyLevel() != null ? c.getTechnologyLevel().floatValue() : 1.0f;
                        break;
                    case "lifespan":
                        res[i] = c.getLifespan() != null ? c.getLifespan().floatValue() : 40.0f;
                        break;
                    case "fertility":
                        res[i] = c.getFertility() != null ? c.getFertility().floatValue() : 6.0f;
                        break;
                    case "giniindex":
                    case "gini":
                        res[i] = c.getGiniIndex() != null ? c.getGiniIndex().floatValue() : 0.0f;
                        break;

                    // --- Food, Water & Natural Resources ---
                    case "food":
                    case "foodresource":
                        res[i] = c.getFoodResource() != null ? c.getFoodResource().floatValue() : 0.0f;
                        break;
                    case "water":
                    case "waterresource":
                        res[i] = c.getWaterResource() != null ? c.getWaterResource().floatValue() : 0.0f;
                        break;
                    case "aquifer":
                    case "freshwateraquifer":
                        res[i] = c.getFreshwaterAquifer() != null ? c.getFreshwaterAquifer().floatValue() : 0.0f;
                        break;
                    case "accessibleaquifer":
                        res[i] = c.getAccessibleAquifer() != null ? c.getAccessibleAquifer().floatValue() : 0.0f;
                        break;
                    case "wood":
                    case "woodresource":
                        res[i] = c.getWoodResource() != null ? c.getWoodResource().floatValue() : 0.0f;
                        break;
                    case "metal":
                    case "resourcemetal":
                        res[i] = c.getResourceMetal() != null ? c.getResourceMetal().floatValue() : 0.0f;
                        break;
                    case "preciousmetal":
                    case "resourcepreciousmetal":
                        res[i] = c.getResourcePreciousMetal() != null ? c.getResourcePreciousMetal().floatValue() : 0.0f;
                        break;
                    case "clay":
                    case "resourceclay":
                        res[i] = c.getResourceClay() != null ? c.getResourceClay().floatValue() : 0.0f;
                        break;
                    case "soilcarbon":
                    case "soilorganiccarbon":
                        res[i] = c.getSoilOrganicCarbon() != null ? c.getSoilOrganicCarbon().floatValue() : 0.0f;
                        break;
                    case "mantleheat":
                    case "mantleheatflow":
                        res[i] = c.getMantleHeatFlow() != null ? c.getMantleHeatFlow().floatValue() : 87.0f;
                        break;

                    // --- Physical Climate & Environment ---
                    case "temperature":
                    case "temp":
                        res[i] = c.getTemperature() != null ? c.getTemperature().floatValue() : 0.0f;
                        break;
                    case "rainfall":
                    case "rain":
                        res[i] = c.getRainfall() != null ? c.getRainfall().floatValue() : 0.0f;
                        break;
                    case "elevation":
                    case "alt":
                        res[i] = c.getElevation() != null ? c.getElevation().floatValue() : 0.0f;
                        break;
                    case "pollution":
                    case "pollutionlevel":
                        res[i] = c.getPollutionLevel() != null ? c.getPollutionLevel().floatValue() : 0.0f;
                        break;
                    case "albedo":
                    case "dynamicalbedo":
                        res[i] = c.getDynamicAlbedo() != null ? c.getDynamicAlbedo().floatValue() : 0.30f;
                        break;
                    case "movementfriction":
                    case "friction":
                        res[i] = c.getMovementFriction() != null ? c.getMovementFriction().floatValue() : 1.0f;
                        break;

                    // --- Biomass & Ecology ---
                    case "biomassnatural":
                        res[i] = c.getBiomassNatural() != null ? c.getBiomassNatural().floatValue() : 0.0f;
                        break;
                    case "biomasslivestock":
                        res[i] = c.getBiomassLivestock() != null ? c.getBiomassLivestock().floatValue() : 0.0f;
                        break;
                    case "biomassfish":
                        res[i] = c.getBiomassFish() != null ? c.getBiomassFish().floatValue() : 0.0f;
                        break;
                    case "biomassagriculture":
                        res[i] = c.getBiomassAgriculture() != null ? c.getBiomassAgriculture().floatValue() : 0.0f;
                        break;

                    // --- Energy ---
                    case "energywind":
                        res[i] = c.getEnergyWind() != null ? c.getEnergyWind().floatValue() : 0.0f;
                        break;
                    case "energysolar":
                        res[i] = c.getEnergySolar() != null ? c.getEnergySolar().floatValue() : 0.0f;
                        break;
                    case "energyfire":
                        res[i] = c.getEnergyFire() != null ? c.getEnergyFire().floatValue() : 0.0f;
                        break;
                    case "energyslaves":
                        res[i] = c.getEnergySlaves() != null ? c.getEnergySlaves().floatValue() : 0.0f;
                        break;
                    case "energyfoodconsumed":
                        res[i] = c.getEnergyFoodConsumed() != null ? c.getEnergyFoodConsumed().floatValue() : 0.0f;
                        break;

                    // --- Demographics Age Cohorts ---
                    case "popyouth":
                        res[i] = c.getPopYouth() != null ? c.getPopYouth().floatValue() : 0.0f;
                        break;
                    case "popadult":
                        res[i] = c.getPopAdult() != null ? c.getPopAdult().floatValue() : 0.0f;
                        break;
                    case "popelderly":
                        res[i] = c.getPopElderly() != null ? c.getPopElderly().floatValue() : 0.0f;
                        break;
                    case "pop0to4":
                        res[i] = c.getPop0to4() != null ? c.getPop0to4().floatValue() : 0.0f;
                        break;
                    case "pop5to14":
                        res[i] = c.getPop5to14() != null ? c.getPop5to14().floatValue() : 0.0f;
                        break;
                    case "pop15to24":
                        res[i] = c.getPop15to24() != null ? c.getPop15to24().floatValue() : 0.0f;
                        break;
                    case "pop25to49":
                        res[i] = c.getPop25to49() != null ? c.getPop25to49().floatValue() : 0.0f;
                        break;
                    case "pop50to64":
                        res[i] = c.getPop50to64() != null ? c.getPop50to64().floatValue() : 0.0f;
                        break;
                    case "pop65to79":
                        res[i] = c.getPop65to79() != null ? c.getPop65to79().floatValue() : 0.0f;
                        break;
                    case "pop80plus":
                        res[i] = c.getPop80Plus() != null ? c.getPop80Plus().floatValue() : 0.0f;
                        break;
                    case "epidemicinfected":
                        res[i] = c.getEpidemicInfected() != null ? c.getEpidemicInfected().floatValue() : 0.0f;
                        break;
                    case "epidemicrecovered":
                        res[i] = c.getEpidemicRecovered() != null ? c.getEpidemicRecovered().floatValue() : 0.0f;
                        break;

                    // --- Derived Helpers ---
                    case "food_per_capita": {
                        float p = c.getPopulation() != null ? c.getPopulation().floatValue() : 0.0f;
                        float f = c.getFoodResource() != null ? c.getFoodResource().floatValue() : 0.0f;
                        res[i] = p > 0 ? f / p : 0.0f;
                        break;
                    }
                    case "work":
                    case "resourcework": {
                        float adult = c.getPopAdult() != null ? c.getPopAdult().floatValue() : c.getPopulation() * 0.6f;
                        float tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel().floatValue() : 1.0f;
                        res[i] = adult * tech;
                        break;
                    }
                    default:
                        res[i] = 0.0f;
                        break;
                }
            }
            return res;
        }

        return new float[0];
    }

    private double computeScalarVariable(String varName, List<H3Cell> cells, WorldBuffer buffer) {
        String key = varName.toLowerCase(Locale.ROOT).trim();
        float[] arr = extractVariableArray(key, cells, buffer);
        if (arr.length == 0) return 0.0;
        double sum = 0;
        for (float v : arr) sum += v;
        return sum;
    }

    /**
     * Export custom formulas to a properties/JSON formatted file.
     */
    public void exportFormulasToFile(File file) throws IOException {
        Properties props = new Properties();
        for (StatDefinition stat : registeredStats.values()) {
            if (!stat.isBuiltin()) {
                String val = String.join("||", stat.getName(), stat.getCategory(), stat.getExpression(), stat.getUnit(), stat.getDescription());
                props.setProperty(stat.getId(), val);
            }
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            props.store(writer, "Ether Pluggable Statistics & Custom Formulas");
        }
    }

    /**
     * Import custom formulas from a properties file.
     */
    public void importFormulasFromFile(File file) throws IOException {
        Properties props = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        for (String key : props.stringPropertyNames()) {
            String val = props.getProperty(key);
            String[] parts = val.split("\\|\\|");
            if (parts.length >= 5) {
                StatDefinition stat = new StatDefinition(key, parts[0], parts[1], parts[2], parts[3], parts[4], false);
                registerStat(stat);
            }
        }
    }
}
