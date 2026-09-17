/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Service Provider Interface (SPI) Loader for Procedural Simulation Engines.
 * Discovers and registers {@link ProceduralEnginePlugin} modules from the classpath
 * and the external 'plugins/' directory.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ProceduralEngineSpiLoader {
    private static final Logger logger = LoggerFactory.getLogger(ProceduralEngineSpiLoader.class);
    private static final String DEFAULT_PLUGIN_DIR = "plugins";

    /**
     * Discovers and registers all available plugins via standard Java ServiceLoader.
     */
    public static int loadAllPlugins() {
        int count = loadClasspathPlugins();
        count += loadExternalDirectoryPlugins(Paths.get(DEFAULT_PLUGIN_DIR));
        return count;
    }

    /**
     * Loads plugins registered on the standard classpath.
     */
    public static int loadClasspathPlugins() {
        int count = 0;
        try {
            ServiceLoader<ProceduralEnginePlugin> loader = ServiceLoader.load(ProceduralEnginePlugin.class);
            for (ProceduralEnginePlugin plugin : loader) {
                String name = plugin.getClass().getSimpleName();
                ProceduralEngineRegistry.registerPlugin(name, plugin);
                logger.info("📦 SPI: Registered classpath engine plugin: {}", name);
                count++;
            }
        } catch (Exception e) {
            logger.error("Error discovering classpath SPI plugins: {}", e.getMessage(), e);
        }
        return count;
    }

    /**
     * Loads plugins from external .jar files in the specified directory.
     */
    public static int loadExternalDirectoryPlugins(Path directory) {
        if (directory == null || !Files.exists(directory) || !Files.isDirectory(directory)) {
            return 0;
        }

        int count = 0;
        try {
            File[] jarFiles = directory.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) return 0;

            List<URL> urls = new ArrayList<>();
            for (File jar : jarFiles) {
                urls.add(jar.toURI().toURL());
            }

            URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), ProceduralEngineSpiLoader.class.getClassLoader());
            ServiceLoader<ProceduralEnginePlugin> loader = ServiceLoader.load(ProceduralEnginePlugin.class, classLoader);

            for (ProceduralEnginePlugin plugin : loader) {
                String name = plugin.getClass().getSimpleName();
                ProceduralEngineRegistry.registerPlugin(name, plugin);
                logger.info("📦 SPI: Registered external JAR engine plugin: {}", name);
                count++;
            }
        } catch (Exception e) {
            logger.error("Error loading external plugins from '{}': {}", directory, e.getMessage(), e);
        }
        return count;
    }
}
