/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.util;

/**
 * Formats LaTeX mathematical expressions and sanitizes text strings
 * into clean, human-readable Unicode mathematical notation for JavaFX tooltips and UI components.
 */
public class LaTeXFormatter {

    /**
     * Converts LaTeX syntax into elegant Unicode math characters.
     * Example: "\\frac{dA}{dt} = \\alpha \\cdot \\exp(\\beta \\cdot x)"
     * becomes: "dA/dt = α · exp(β · x)"
     */
    public static String formatLaTeX(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String s = input;

        // Strip variation selector-16 (\uFE0F) from emojis for clean rendering on Windows JavaFX
        s = s.replace("\uFE0F", "");

        // Fractions: \frac{num}{den} -> (num) / (den) or num/den
        s = s.replaceAll("\\\\frac\\{([^\\}]+)\\}\\{([^\\}]+)\\}", "($1) / ($2)");

        // Greek letters (lowercase)
        s = s.replace("\\alpha", "α")
             .replace("\\beta", "β")
             .replace("\\gamma", "γ")
             .replace("\\delta", "δ")
             .replace("\\epsilon", "ε")
             .replace("\\zeta", "ζ")
             .replace("\\eta", "η")
             .replace("\\theta", "θ")
             .replace("\\iota", "ι")
             .replace("\\kappa", "κ")
             .replace("\\lambda", "λ")
             .replace("\\mu", "μ")
             .replace("\\nu", "ν")
             .replace("\\xi", "ξ")
             .replace("\\pi", "π")
             .replace("\\rho", "ρ")
             .replace("\\sigma", "σ")
             .replace("\\tau", "τ")
             .replace("\\upsilon", "υ")
             .replace("\\phi", "φ")
             .replace("\\chi", "χ")
             .replace("\\psi", "ψ")
             .replace("\\omega", "ω");

        // Greek letters (uppercase)
        s = s.replace("\\Gamma", "Γ")
             .replace("\\Delta", "Δ")
             .replace("\\Theta", "Θ")
             .replace("\\Lambda", "Λ")
             .replace("\\Xi", "Ξ")
             .replace("\\Pi", "Π")
             .replace("\\Sigma", "Σ")
             .replace("\\Phi", "Φ")
             .replace("\\Psi", "Ψ")
             .replace("\\Omega", "Ω");

        // Mathematical operators & symbols
        s = s.replace("\\cdot", "·")
             .replace("\\times", "×")
             .replace("\\div", "÷")
             .replace("\\pm", "±")
             .replace("\\leq", "≤")
             .replace("\\geq", "≥")
             .replace("\\neq", "≠")
             .replace("\\approx", "≈")
             .replace("\\infty", "∞")
             .replace("\\partial", "∂")
             .replace("\\nabla", "∇")
             .replace("\\int", "∫")
             .replace("\\sum", "Σ")
             .replace("\\prod", "∏")
             .replace("\\sqrt", "√")
             .replace("\\in", "∈")
             .replace("\\notin", "∉")
             .replace("\\subset", "⊂")
             .replace("\\forall", "∀")
             .replace("\\exists", "∃");

        // Superscripts & Subscripts
        s = s.replace("^0", "⁰").replace("^1", "¹").replace("^2", "²").replace("^3", "³").replace("^4", "⁴").replace("^5", "⁵")
             .replace("^6", "⁶").replace("^7", "⁷").replace("^8", "⁸").replace("^9", "⁹").replace("^+", "⁺").replace("^-", "⁻");
        s = s.replace("_0", "₀").replace("_1", "₁").replace("_2", "₂").replace("_3", "₃").replace("_4", "₄").replace("_5", "₅")
             .replace("_6", "₆").replace("_7", "₇").replace("_8", "₈").replace("_9", "₉").replace("_t", "ₜ").replace("_i", "ᵢ").replace("_j", "ⱼ");

        // Clean up remaining backslashes if any standalone LaTeX tags remain
        s = s.replace("\\", "");

        return s;
    }
}
