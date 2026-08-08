/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.jit;

import org.ether.society.procedural.ProceduralEnginePlugin;
import org.ether.society.procedural.ProceduralEngineRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Dynamic Runtime Java Compiler & JIT Engine Loader for Ether.
 * Allows users to import external .java simulation engine files at runtime,
 * compile them on the fly, register them into the ProceduralEngineRegistry,
 * and integrate them into the ScenarioEngineJITCompiler execution pipeline.
 */
public class DynamicEngineCompiler {
    private static final Logger logger = LoggerFactory.getLogger(DynamicEngineCompiler.class);

    public record CompilationResult(boolean success, String engineName, String message, ProceduralEnginePlugin plugin) {}

    /**
     * Compiles and loads a custom Java source file (.java) at runtime.
     *
     * @param javaSourceFile The .java source file to compile
     * @return CompilationResult containing status and instantiated engine plugin
     */
    public static CompilationResult compileAndLoadEngine(File javaSourceFile) {
        if (javaSourceFile == null || !javaSourceFile.exists()) {
            return new CompilationResult(false, null, "Le fichier source Java n'existe pas.", null);
        }

        try {
            String fileName = javaSourceFile.getName();
            if (!fileName.endsWith(".java")) {
                return new CompilationResult(false, null, "Le fichier doit avoir l'extension .java", null);
            }

            String className = fileName.substring(0, fileName.lastIndexOf('.'));
            Path outputDir = Paths.get("saves", "engines", "compiled");
            Files.createDirectories(outputDir);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                // Fallback attempt: inspect bytecode if .class or return clear message
                return new CompilationResult(false, className, 
                        "Le compilateur Java (JDK Javac) n'a pas été détecté dans l'environnement Java courant execution.", null);
            }

            int compilationResult = compiler.run(null, null, null,
                    "-d", outputDir.toString(),
                    javaSourceFile.getAbsolutePath());

            if (compilationResult != 0) {
                return new CompilationResult(false, className, "Erreur de compilation javac pour " + fileName, null);
            }

            // Load class using URLClassLoader
            URL[] urls = new URL[]{outputDir.toUri().toURL()};
            try (URLClassLoader classLoader = new URLClassLoader(urls, DynamicEngineCompiler.class.getClassLoader())) {
                Class<?> loadedClass = classLoader.loadClass(className);
                
                if (!ProceduralEnginePlugin.class.isAssignableFrom(loadedClass)) {
                    return new CompilationResult(false, className, 
                            "La classe " + className + " doit implémenter l'interface org.ether.society.procedural.ProceduralEnginePlugin", null);
                }

                ProceduralEnginePlugin pluginInstance = (ProceduralEnginePlugin) loadedClass.getDeclaredConstructor().newInstance();

                // 1. Register in ProceduralEngineRegistry
                ProceduralEngineRegistry.registerPlugin(className, pluginInstance);

                // 2. Register in JIT Compiler Pipeline
                ScenarioEngineJITCompiler jit = new ScenarioEngineJITCompiler();
                jit.registerEngineStep(className, "customStateVar", new SymbolicExpression("customStateVar", 1.0, 0.0), 1.0);
                jit.compile();

                logger.info("⚡ Custom Engine '{}' successfully compiled & registered into JIT Compiler!", className);
                return new CompilationResult(true, className, "Moteur '" + className + "' compilé et enregistré avec succès dans le JIT !", pluginInstance);
            }
        } catch (Exception e) {
            logger.error("Failed to dynamically compile and load engine: {}", javaSourceFile.getName(), e);
            return new CompilationResult(false, javaSourceFile.getName(), "Erreur d'exécution : " + e.getMessage(), null);
        }
    }

    /**
     * Generates a template .java file for user custom simulation engine development.
     */
    public static String generateEngineTemplateCode(String engineName) {
        return String.format("""
            package org.ether.society.procedural.custom;

            import org.ether.society.procedural.ProceduralEnginePlugin;
            import org.ether.society.database.H3Cell;
            import java.util.List;

            /**
             * Custom Simulation Engine for Ether Framework.
             * Automatically registered into the ScenarioEngineJITCompiler.
             */
            public class %s implements ProceduralEnginePlugin {

                @Override
                public void process(List<H3Cell> cells, double deltaYears) {
                    for (H3Cell cell : cells) {
                        // Exemple : Modifier la biomasse ou l'énergie en fonction des conditions physiques
                        double currentBiomass = cell.getBiomass();
                        double targetBiomass = currentBiomass * (1.0 + 0.01 * deltaYears);
                        cell.setBiomass(targetBiomass);
                    }
                }
            }
            """, engineName);
    }
}
