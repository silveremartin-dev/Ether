/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.compiler;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating the Security Sandbox and AST checks of DynamicEngineCompiler.
 */
public class DynamicEngineCompilerSecurityTest {

    @Test
    /*
     * Test validate source code security blocks runtime exec operation.
     * <p>
     * Executes operational logic for {@code DynamicEngineCompilerSecurityTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testValidateSourceCodeSecurityBlocksRuntimeExec() {
        String maliciousCode = """
            package org.ether.society.procedural.custom;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;
            import org.ether.society.engines.ProceduralEnginePlugin;
            import org.ether.society.database.H3Cell;
            import java.util.List;

            public class AttackEngine implements ProceduralEnginePlugin {
                @Override
                public void process(List<H3Cell> cells, double deltaYears) {
                    try {
                        Runtime.getRuntime().exec("calc.exe");
                    } catch (Exception ignored) {}
                }
            }
            """;

        String violation = DynamicEngineCompiler.validateSourceCodeSecurity(maliciousCode);
        assertNotNull(violation, "Security validator must detect and reject Runtime.getRuntime()");
        assertTrue(violation.contains("Runtime.getRuntime") || violation.contains("java.lang.Runtime"));
    }

    @Test
    /*
     * Test validate source code security blocks process builder operation.
     * <p>
     * Executes operational logic for {@code DynamicEngineCompilerSecurityTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testValidateSourceCodeSecurityBlocksProcessBuilder() {
        String maliciousCode = """
            public class BadEngine {
                public void run() {
                    new ProcessBuilder("cmd.exe").start();
                }
            }
            """;

        String violation = DynamicEngineCompiler.validateSourceCodeSecurity(maliciousCode);
        assertNotNull(violation, "Security validator must detect and reject ProcessBuilder");
        assertTrue(violation.contains("ProcessBuilder"));
    }

    @Test
    /*
     * Test validate source code security blocks system exit operation.
     * <p>
     * Executes operational logic for {@code DynamicEngineCompilerSecurityTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testValidateSourceCodeSecurityBlocksSystemExit() {
        String maliciousCode = """
            public class ExitEngine {
                public void run() {
                    System.exit(1);
                }
            }
            """;

        String violation = DynamicEngineCompiler.validateSourceCodeSecurity(maliciousCode);
        assertNotNull(violation, "Security validator must detect and reject System.exit");
        assertTrue(violation.contains("System.exit"));
    }

    @Test
    /*
     * Test validate source code security blocks reflection operation.
     * <p>
     * Executes operational logic for {@code DynamicEngineCompilerSecurityTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testValidateSourceCodeSecurityBlocksReflection() {
        String maliciousCode = """
            import java.lang.reflect.Method;
            public class ReflectEngine {
                public void run() {}
            }
            """;

        String violation = DynamicEngineCompiler.validateSourceCodeSecurity(maliciousCode);
        assertNotNull(violation, "Security validator must detect and reject reflection");
        assertTrue(violation.contains("java.lang.reflect"));
    }

    @Test
    /*
     * Test validate source code security allows clean template operation.
     * <p>
     * Executes operational logic for {@code DynamicEngineCompilerSecurityTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testValidateSourceCodeSecurityAllowsCleanTemplate() {
        String cleanCode = DynamicEngineCompiler.generateEngineTemplateCode("SafeCustomEngine");
        String violation = DynamicEngineCompiler.validateSourceCodeSecurity(cleanCode);
        assertNull(violation, "Security validator must allow standard mathematical simulation template code");
    }

    @Test
    /*
     * Test compile and load engine rejects malicious file operation.
     * <p>
     * Executes operational logic for {@code DynamicEngineCompilerSecurityTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testCompileAndLoadEngineRejectsMaliciousFile() throws IOException {
        Path tempDir = Files.createTempDirectory("ether_sec_test");
        File badFile = tempDir.resolve("MaliciousEngine.java").toFile();
        Files.writeString(badFile.toPath(), """
            package org.ether.society.procedural.custom;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;
            public class MaliciousEngine {
                public void bad() {
                    System.exit(0);
                }
            }
            """);

        DynamicEngineCompiler.CompilationResult result = DynamicEngineCompiler.compileAndLoadEngine(badFile);
        assertFalse(result.success(), "Compilation of malicious source file must fail");
        assertTrue(result.message().toLowerCase().contains("security") || result.message().toLowerCase().contains("forbidden"));

        // Cleanup
        badFile.delete();
        tempDir.toFile().delete();
    }
}

