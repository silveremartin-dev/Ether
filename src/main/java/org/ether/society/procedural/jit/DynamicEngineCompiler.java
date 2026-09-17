/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.jit;

import org.ether.society.i18n.I18n;
import org.ether.society.network.EtherSecurityAuditLogger;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dynamic Runtime Java Compiler & JIT Engine Loader for Ether.
 * Allows users to import external .java simulation engine files at runtime,
 * validates security constraints via static AST/source inspection,
 * compiles them safely, registers them into the ProceduralEngineRegistry,
 * and integrates them into the ScenarioEngineJITCompiler execution pipeline.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class DynamicEngineCompiler {
    private static final Logger logger = LoggerFactory.getLogger(DynamicEngineCompiler.class);

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;", Pattern.MULTILINE);

    // List of dangerous tokens/APIs strictly forbidden in custom user engine scripts
    private static final List<String> FORBIDDEN_TOKENS = List.of(
            "java.lang.Runtime",
            "Runtime.getRuntime",
            "ProcessBuilder",
            "System.exit",
            "System.load",
            "System.loadLibrary",
            "System.setSecurityManager",
            "sun.misc.Unsafe",
            "jdk.internal",
            "java.lang.reflect",
            "java.lang.invoke.MethodHandles",
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.net.URLClassLoader",
            "ClassLoader",
            "javax.tools.JavaCompiler",
            "Native",
            "JNI"
    );

    private static URLClassLoader persistentClassLoader;

    public record CompilationResult(boolean success, String engineName, String message, ProceduralEnginePlugin plugin) {}

    /**
     * Statically inspects the Java source code to ensure it satisfies security sandboxing rules.
     *
     * @param sourceCode The raw Java source string
     * @return null if code is safe, or a security violation error description
     */
    public static String validateSourceCodeSecurity(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return "Empty or null source code.";
        }

        for (String forbidden : FORBIDDEN_TOKENS) {
            if (sourceCode.contains(forbidden)) {
                String auditMsg = "Security Violation: Custom engine contains forbidden API token '" + forbidden + "'";
                EtherSecurityAuditLogger.logAuditEvent("SECURITY_VIOLATION", "DynamicEngineCompiler", auditMsg);
                return auditMsg;
            }
        }
        return null;
    }

    /**
     * Compiles and loads a custom Java source file (.java) at runtime.
     *
     * @param javaSourceFile The .java source file to compile
     * @return CompilationResult containing status and instantiated engine plugin
     */
    public static CompilationResult compileAndLoadEngine(File javaSourceFile) {
        if (javaSourceFile == null || !javaSourceFile.exists()) {
            return new CompilationResult(false, null, 
                    I18n.getOrDefault("dynamic_engine.error.file_not_found", "The specified Java source file does not exist."), null);
        }

        try {
            String fileName = javaSourceFile.getName();
            if (!fileName.endsWith(".java")) {
                return new CompilationResult(false, null, 
                        I18n.getOrDefault("dynamic_engine.error.not_java", "The file must have a .java extension."), null);
            }

            String sourceCode = Files.readString(javaSourceFile.toPath());

            // 1. Static Security Validation
            String securityViolation = validateSourceCodeSecurity(sourceCode);
            if (securityViolation != null) {
                logger.warn("🛑 Rejected untrusted engine file '{}': {}", fileName, securityViolation);
                return new CompilationResult(false, fileName, "Security Error: " + securityViolation, null);
            }

            String simpleClassName = fileName.substring(0, fileName.lastIndexOf('.'));
            String fullClassName = simpleClassName;

            Matcher matcher = PACKAGE_PATTERN.matcher(sourceCode);
            if (matcher.find()) {
                fullClassName = matcher.group(1) + "." + simpleClassName;
            }

            Path outputDir = Paths.get("saves", "engines", "compiled");
            Files.createDirectories(outputDir);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                return new CompilationResult(false, simpleClassName, 
                        I18n.getOrDefault("dynamic_engine.error.no_jdk", "Java JDK Compiler (javac) not detected in current execution environment. JDK 21+ required for on-the-fly compilation."), null);
            }

            int compilationResult = compiler.run(null, null, null,
                    "-d", outputDir.toString(),
                    javaSourceFile.getAbsolutePath());

            if (compilationResult != 0) {
                return new CompilationResult(false, simpleClassName, 
                        I18n.getOrDefault("dynamic_engine.error.javac_failed", "Javac compilation error for: ") + fileName, null);
            }

            // 2. Load class using persistent ClassLoader (avoiding premature closure)
            URL[] urls = new URL[]{outputDir.toUri().toURL()};
            if (persistentClassLoader == null) {
                persistentClassLoader = new URLClassLoader(urls, DynamicEngineCompiler.class.getClassLoader());
            } else {
                persistentClassLoader = new URLClassLoader(urls, persistentClassLoader);
            }

            Class<?> loadedClass = persistentClassLoader.loadClass(fullClassName);
            
            if (!ProceduralEnginePlugin.class.isAssignableFrom(loadedClass)) {
                return new CompilationResult(false, simpleClassName, 
                        I18n.getOrDefault("dynamic_engine.error.not_plugin", "Class must implement org.ether.society.procedural.ProceduralEnginePlugin"), null);
            }

            ProceduralEnginePlugin pluginInstance = (ProceduralEnginePlugin) loadedClass.getDeclaredConstructor().newInstance();

            // 3. Register in ProceduralEngineRegistry
            ProceduralEngineRegistry.registerPlugin(simpleClassName, pluginInstance);

            EtherSecurityAuditLogger.logAuditEvent("ENGINE_LOADED", "DynamicEngineCompiler", "Loaded custom engine: " + fullClassName);
            logger.info("⚡ Custom Engine '{}' successfully verified, compiled & registered into ProceduralEngineRegistry!", fullClassName);
            return new CompilationResult(true, simpleClassName, 
                    I18n.getOrDefault("dynamic_engine.status.success", "Engine compiled and registered successfully!"), pluginInstance);

        } catch (Exception e) {
            logger.error("Failed to dynamically compile and load engine: {}", javaSourceFile.getName(), e);
            return new CompilationResult(false, javaSourceFile.getName(), 
                    I18n.getOrDefault("dynamic_engine.error.generic", "Execution error: ") + e.getMessage(), null);
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
             * Implements the ProceduralEnginePlugin interface for deterministic cliodynamic simulation.
             */
            public class %s implements ProceduralEnginePlugin {

                @Override
                public void process(List<H3Cell> cells, double deltaYears) {
                    if (cells == null || cells.isEmpty()) return;

                    for (H3Cell cell : cells) {
                        // Example: Apply cliodynamic physical transformation
                        double currentBiomass = cell.getBiomass();
                        double targetBiomass = currentBiomass * (1.0 + 0.01 * deltaYears);
                        cell.setBiomass(targetBiomass);
                    }
                }
            }
            """, engineName);
    }
}

