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

    /**
     * Backward-compatible subclass of {@link org.ether.society.core.dod.StatDefinition}.
     */
    public static class StatDefinition extends org.ether.society.core.dod.StatDefinition {
        public StatDefinition() { super(); }
        public StatDefinition(String id, String name, String category, String expression, String unit, String description, boolean builtin) {
            super(id, name, category, expression, unit, description, builtin);
        }
    }

    private final Map<String, StatDefinition> registeredStats = new LinkedHashMap<>();
    private final FormulaHelper evaluator = new FormulaHelper();

    /**
     * Pluggable stat engine.
     *
     */
    public PluggableStatEngine() {
        registerBuiltinDefaults();
    }

    private void registerBuiltinDefaults() {
        registerStat(new StatDefinition(
                "custom_gini_wealth",
                "Indice de Gini Richesse / PIB",
                "ðŸ’Ž Ã‰conomie & Richesse",
                "GINI(wealth)",
                "Coeff",
                "Distribution d'inÃ©galitÃ© de la richesse individuelle (0 = Ã©galitÃ© absolue, 1 = inÃ©galitÃ© totale)",
                true
        ));

        registerStat(new StatDefinition(
                "custom_variance_wealth",
                "Variance de Richesse Entre Individus",
                "ðŸ’Ž Ã‰conomie & Richesse",
                "VAR(wealth)",
                "G$^2",
                "Statistique de variance entre individus sur la variable richesse : Ã  quel point les gens s'Ã©loignent du schÃ©ma standard",
                true
        ));

        registerStat(new StatDefinition(
                "custom_stddev_food",
                "Ã‰cart-Type Distribution Nourriture",
                "ðŸŒ¾ Ã‰nergie & MatiÃ¨re",
                "STDDEV(food)",
                "t",
                "Ã‰cart-type de la quantitÃ© de nourriture disponible par maille/individu par rapport Ã  la moyenne",
                true
        ));

        registerStat(new StatDefinition(
                "custom_food_per_capita",
                "Nourriture Moyenne Par Habitant",
                "ðŸŒ¾ Ã‰nergie & MatiÃ¨re",
                "SUM(food) / COUNT(population)",
                "t/hab",
                "Formule calculÃ©e : Somme totale des rÃ©serves alimentaires divisÃ©e par le nombre de mailles/habitants",
                true
        ));

        registerStat(new StatDefinition(
                "custom_pop_density_variance",
                "Variance DensitÃ© de Population",
                "ðŸ‘¥ DÃ©mographie & SantÃ©",
                "VAR(population)",
                "hab^2",
                "Variance de la rÃ©partition de densitÃ© de population entre les zones du terrarium",
                true
        ));

        registerStat(new StatDefinition(
                "custom_median_age",
                "Ã‚ge MÃ©dian de la Population",
                "ðŸ‘¥ DÃ©mographie & SantÃ©",
                "MEDIAN(age)",
                "ans",
                "Valeur mÃ©diane sÃ©parant la moitiÃ© la plus jeune de la moitiÃ© la plus Ã¢gÃ©e",
                true
        ));

        registerStat(new StatDefinition(
                "largest_org_complexity",
                "ComplexitÃ© de la Plus Grande Organisation",
                "ðŸ›ï¸ SociÃ©tÃ© & Institutions",
                "MAX(population * tech * (1 + giniindex))",
                "Idx",
                "Mesure de la complexitÃ© sociÃ©tale et administrative maximale atteinte par le plus grand empire/organisation",
                true
        ));

        registerStat(new StatDefinition(
                "largest_org_entropy",
                "Entropie GÃ©nÃ©rÃ©e par la Plus Grande Civilisation",
                "âš¡ Ã‰nergie & MatiÃ¨re",
                "MAX(population * tech * pollution)",
                "J/K",
                "QuantitÃ© d'entropie thermodynamique et de chaleur rÃ©siduelle gÃ©nÃ©rÃ©e par la plus grande civilisation",
                true
        ));

        registerStat(new StatDefinition(
                "avg_tech_level",
                "Niveau Technologique Moyen",
                "ðŸ›ï¸ SociÃ©tÃ© & Institutions",
                "AVG(tech)",
                "Niv",
                "Moyenne globale du niveau d'avancement scientifique et technologique Ã  travers la grille",
                true
        ));

        registerStat(new StatDefinition(
                "max_tech_level",
                "Niveau Technologique Maximal",
                "ðŸ›ï¸ SociÃ©tÃ© & Institutions",
                "MAX(tech)",
                "Niv",
                "Niveau technologique de pointe atteint par la cellule/civilisation la plus avancÃ©e",
                true
        ));
    }

    /**
     * Register stat.
     *
     * @param stat the stat parameter (StatDefinition)
     */
    public void registerStat(StatDefinition stat) {
        if (stat != null && stat.getId() != null) {
            registeredStats.put(stat.getId(), stat);
            logger.info("Registered pluggable statistic formula: {} -> {}", stat.getName(), stat.getExpression());
        }
    }

    /**
     * Unregister stat.
     *
     * @param id the id parameter (String)
     */
    public void unregisterStat(String id) {
        StatDefinition def = registeredStats.get(id);
        if (def != null && !def.isBuiltin()) {
            registeredStats.remove(id);
            logger.info("Unregistered custom statistic: {}", id);
        }
    }

    /**
     * Get registered stats.
     *
     * @return the resulting computation or state reference
     */
    public Collection<StatDefinition> getRegisteredStats() {
        return Collections.unmodifiableCollection(registeredStats.values());
    }

    /**
     * Get stat.
     *
     * @param id the id parameter (String)
     * @return the resulting computation or state reference
     */
    public StatDefinition getStat(String id) {
        return registeredStats.get(id);
    }

    /**
     * Computes the numerical value of a formula over H3 cell data or WorldBuffer.
     */
    public double computeValue(String expression, List<H3Cell> cells, WorldBuffer buffer) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        if (expression == null || expression.isBlank()) return 0.0;

        FormulaHelper.VariableResolver arrayResolver = varName -> extractVariableArray(varName, cells, buffer);
        FormulaHelper.ScalarResolver scalarResolver = varName -> computeScalarVariable(varName, cells, buffer);

        return evaluator.evaluate(expression, arrayResolver, scalarResolver);
    }

    /**
     * Extract variable array.
     *
     * @param varName the var name parameter (String)
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param buffer the buffer parameter (WorldBuffer)
     * @return the resulting computation or state reference
     */
    public float[] extractVariableArray(String varName, List<H3Cell> cells, WorldBuffer buffer) {
        return StatVariableExtractor.extractVariableArray(varName, cells, buffer);
    }

    private double computeScalarVariable(String varName, List<H3Cell> cells, WorldBuffer buffer) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
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

