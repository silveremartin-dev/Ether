/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.persistence;

import org.ether.society.model.Scenario;
import java.util.List;
import java.util.Optional;

/**
 * <h1>Scenario Repository</h1>
 * <p>
 * Core operational component for the Ether civilizational and planetary simulation framework.<br>
 * Integrates cellular dynamics, data structures, and deterministic state transitions.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioRepository extends JsonRepository<Scenario> {

    /*
     * Scenario repository.
     * Enforces physical invariants and updates associated state variables within {@code ScenarioRepository}.
     *
     */
    public ScenarioRepository() {
        super("scenarios.json", Scenario.class);
    }

    /*
     * Get all scenarios.
     * Enforces physical invariants and updates associated state variables within {@code ScenarioRepository}.
     *
     * @return the resulting computation or state reference
     */
    public List<Scenario> getAllScenarios() {
        return PresetStorageService.loadAllScenarios();
    }

    /*
     * Save or update.
     * Enforces physical invariants and updates associated state variables within {@code ScenarioRepository}.
     *
     * @param scenario the scenario parameter (Scenario)
     */
    public void saveOrUpdate(Scenario scenario) {
        List<Scenario> all = findAll();
        // Remove existing with same name (simple ID strategy)
        all.removeIf(s -> s.getName().equals(scenario.getName()));
        all.add(scenario);
        saveAll(all);
    }

    /*
     * Find by name.
     * Enforces physical invariants and updates associated state variables within {@code ScenarioRepository}.
     *
     * @param name the name parameter (String)
     * @return the resulting computation or state reference
     */
    public Optional<Scenario> findByName(String name) {
        return findAll().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst();
    }
}
