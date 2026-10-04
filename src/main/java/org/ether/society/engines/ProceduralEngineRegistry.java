/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for pluggable user procedural simulation engines.
 * Manages custom simulation plugins registered at runtime.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ProceduralEngineRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ProceduralEngineRegistry.class);

    private static final Map<String, ProceduralEnginePlugin> registeredPlugins = new ConcurrentHashMap<>();

    /*
     * Register plugin.
     * Enforces physical invariants and updates associated state variables within {@code ProceduralEngineRegistry}.
     *
     * @param name the name parameter (String)
     * @param plugin the plugin parameter (ProceduralEnginePlugin)
     */
    public static void registerPlugin(String name, ProceduralEnginePlugin plugin) {
        if (name == null || plugin == null) return;
        registeredPlugins.put(name, plugin);
        logger.info("ðŸ”Œ Registered custom procedural engine plugin: {}", name);
    }

    /*
     * Unregister plugin.
     * Enforces physical invariants and updates associated state variables within {@code ProceduralEngineRegistry}.
     *
     * @param name the name parameter (String)
     */
    public static void unregisterPlugin(String name) {
        if (name != null) {
            registeredPlugins.remove(name);
            logger.info("ðŸ”Œ Unregistered custom procedural engine plugin: {}", name);
        }
    }

    /*
     * Clear plugins.
     * Enforces physical invariants and updates associated state variables within {@code ProceduralEngineRegistry}.
     *
     */
    public static void clearPlugins() {
        registeredPlugins.clear();
    }

    /*
     * Process plugins.
     * Enforces physical invariants and updates associated state variables within {@code ProceduralEngineRegistry}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processPlugins(List<H3Cell> cells, double deltaYears) {
        for (Map.Entry<String, ProceduralEnginePlugin> entry : registeredPlugins.entrySet()) {
            try {
                entry.getValue().process(cells, deltaYears);
            } catch (Exception e) {
                logger.error("Error executing pluggable engine '{}': {}", entry.getKey(), e.getMessage(), e);
            }
        }
    }

    /*
     * Get plugin count.
     * Enforces physical invariants and updates associated state variables within {@code ProceduralEngineRegistry}.
     *
     * @return the resulting computation or state reference
     */
    public static int getPluginCount() {
        return registeredPlugins.size();
    }
}


