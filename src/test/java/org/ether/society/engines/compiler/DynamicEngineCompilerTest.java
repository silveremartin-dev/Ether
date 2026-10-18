/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.compiler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for DynamicEngineCompiler security sandbox validation and compilation entry points.
 */
public class DynamicEngineCompilerTest {

    @Test
    @DisplayName("Level 1: Security Sandboxing - Forbids dangerous tokens and system APIs")
    void testSecuritySandboxForbiddenTokens() {
        String safeCode = """
            package org.ether.society.engines.custom;
            public class SafeCustomEngine {
                public double compute(double x) {
                    return Math.sin(x) * 2.5;
                }
            }
            """;
        assertNull(DynamicEngineCompiler.validateSourceCodeSecurity(safeCode), "Safe code must produce no security violation");

        String maliciousCode1 = "public class BadEngine { void hack() { Runtime.getRuntime().exec(\"calc\"); } }";
        assertNotNull(DynamicEngineCompiler.validateSourceCodeSecurity(maliciousCode1));
        assertTrue(DynamicEngineCompiler.validateSourceCodeSecurity(maliciousCode1).contains("forbidden API token"));

        String maliciousCode2 = "public class BadEngine { void hack() { ProcessBuilder pb = new ProcessBuilder(); } }";
        assertNotNull(DynamicEngineCompiler.validateSourceCodeSecurity(maliciousCode2));

        String maliciousCode3 = "public class BadEngine { void hack() { System.exit(1); } }";
        assertNotNull(DynamicEngineCompiler.validateSourceCodeSecurity(maliciousCode3));

        String maliciousCode4 = "public class BadEngine { void hack() { sun.misc.Unsafe u = null; } }";
        assertNotNull(DynamicEngineCompiler.validateSourceCodeSecurity(maliciousCode4));

        String maliciousCode5 = "public class BadEngine { void hack() { java.net.Socket s = null; } }";
        assertNotNull(DynamicEngineCompiler.validateSourceCodeSecurity(maliciousCode5));
    }

    @Test
    @DisplayName("Level 1: Source Code Validation - Handles null and empty code")
    void testEmptyOrNullSourceValidation() {
        assertNotNull(DynamicEngineCompiler.validateSourceCodeSecurity(null));
        assertNotNull(DynamicEngineCompiler.validateSourceCodeSecurity("   "));
    }

    @Test
    @DisplayName("Level 1: File Loader Error Handling - Non-existent or invalid files")
    void testCompileAndLoadEngineInvalidFiles() {
        DynamicEngineCompiler.CompilationResult resNull = DynamicEngineCompiler.compileAndLoadEngine(null);
        assertFalse(resNull.success());
        assertNull(resNull.plugin());

        File missingFile = new File("non_existent_engine_file.java");
        DynamicEngineCompiler.CompilationResult resMissing = DynamicEngineCompiler.compileAndLoadEngine(missingFile);
        assertFalse(resMissing.success());
        assertNull(resMissing.plugin());

        File invalidExt = new File("README.md");
        DynamicEngineCompiler.CompilationResult resInvalid = DynamicEngineCompiler.compileAndLoadEngine(invalidExt);
        assertFalse(resInvalid.success());
    }
}
