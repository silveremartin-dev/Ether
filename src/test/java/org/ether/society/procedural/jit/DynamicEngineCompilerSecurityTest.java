/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.jit;

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
    public void testValidateSourceCodeSecurityBlocksRuntimeExec() {
        String maliciousCode = """
            package org.ether.society.procedural.custom;
            import org.ether.society.procedural.ProceduralEnginePlugin;
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
    public void testValidateSourceCodeSecurityAllowsCleanTemplate() {
        String cleanCode = DynamicEngineCompiler.generateEngineTemplateCode("SafeCustomEngine");
        String violation = DynamicEngineCompiler.validateSourceCodeSecurity(cleanCode);
        assertNull(violation, "Security validator must allow standard mathematical simulation template code");
    }

    @Test
    public void testCompileAndLoadEngineRejectsMaliciousFile() throws IOException {
        Path tempDir = Files.createTempDirectory("ether_sec_test");
        File badFile = tempDir.resolve("MaliciousEngine.java").toFile();
        Files.writeString(badFile.toPath(), """
            package org.ether.society.procedural.custom;
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
