package org.ether.society.core;

import java.nio.file.Path;

/**
 * Service contract for simulation state serialization, persistence, and checkpointing.
 * Decouples core simulation execution from storage formats and IO implementations.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public interface SimulationSaveService {

    /**
     * Saves the current simulation state into a named save directory.
     *
     * @param engine the simulation engine instance to save
     * @param saveName the unique directory or snapshot name
     * @return Path to the saved directory
     */
    Path saveSimulation(H3SimulationEngine engine, String saveName);

    /**
     * Saves the current simulation state with specified ID and metadata scenario name.
     *
     * @param engine the simulation engine instance to save
     * @param saveId the unique directory save ID
     * @param saveName the human-readable scenario/save name
     * @return Path to the saved directory
     */
    Path saveSimulation(H3SimulationEngine engine, String saveId, String saveName);

    /**
     * Loads a simulation state from a directory.
     *
     * @param engine the simulation engine into which the state is restored
     * @param savePath path to the save directory
     */
    void loadSimulation(H3SimulationEngine engine, Path savePath);
}
