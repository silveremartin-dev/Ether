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
 * Repository for managing Scenario persistence.
 */
public class ScenarioRepository extends JsonRepository<Scenario> {

    public ScenarioRepository() {
        super("scenarios.json", Scenario.class);
    }

    public void saveOrUpdate(Scenario scenario) {
        List<Scenario> all = findAll();
        // Remove existing with same name (simple ID strategy)
        all.removeIf(s -> s.getName().equals(scenario.getName()));
        all.add(scenario);
        saveAll(all);
    }

    public Optional<Scenario> findByName(String name) {
        return findAll().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst();
    }
}
