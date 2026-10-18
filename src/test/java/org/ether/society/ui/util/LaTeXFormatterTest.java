/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link LaTeXFormatter}.
 * Tests the conversion of LaTeX expressions to Unicode mathematical notations for UI tooltips.
 */
class LaTeXFormatterTest {

    @Test
    @DisplayName("formatLaTeX converts fractions and Greek letters to Unicode symbols")
    void testFractionAndGreekConversion() {
        String input = "\\frac{dA}{dt} = \\alpha \\cdot \\exp(\\beta \\cdot x)";
        String formatted = LaTeXFormatter.formatLaTeX(input);

        assertNotNull(formatted);
        assertTrue(formatted.contains("(dA) / (dt)"));
        assertTrue(formatted.contains("α"));
        assertTrue(formatted.contains("β"));
        assertFalse(formatted.contains("\\frac"));
        assertFalse(formatted.contains("\\alpha"));
    }

    @Test
    @DisplayName("formatLaTeX handles null, blank, and plain strings gracefully")
    void testNullAndBlankStrings() {
        assertEquals("", LaTeXFormatter.formatLaTeX(null));
        assertEquals("", LaTeXFormatter.formatLaTeX("   "));
        assertEquals("Plain text", LaTeXFormatter.formatLaTeX("Plain text"));
    }

    @Test
    @DisplayName("formatLaTeX converts uppercase Greek and mathematical operators")
    void testUppercaseGreekAndOperators() {
        String input = "\\Delta T = \\sum \\lambda_i \\cdot \\Omega";
        String formatted = LaTeXFormatter.formatLaTeX(input);

        assertTrue(formatted.contains("Δ"));
        assertTrue(formatted.contains("Ω"));
        assertTrue(formatted.contains("λ"));
    }
}
