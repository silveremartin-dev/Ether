/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.math;

/**
 * Stateless \(O(1)\) infinitely differentiable (\(C^\infty\)) analytical primitives for
 * biophysical and cliodynamic differential equations.
 *
 * <p>Replaces discontinuous boolean thresholds, ternary step cliffs, and \(C^0\) piecewise-linear
 * corners ({@code Math.max(0, x - x0)}, {@code Math.min(a, b)}, {@code Math.clamp}) with
 * numerically stable, overflow-safe closed-form expressions grounded in statistical mechanics:</p>
 * <ul>
 *   <li><b>Fermi-Dirac Sigmoid</b>: \(\sigma(x; x_0, s) = \frac{1}{1 + \exp(-(x - x_0)/s)}\)</li>
 *   <li><b>Softplus</b>: \(\operatorname{softplus}(x; x_0, s) = s \ln\left(1 + \exp\left(\frac{x - x_0}{s}\right)\right)\)</li>
 *   <li><b>LogSumExp Softmax / Softmin</b>: smooth Liebig law of the minimum without derivative kinks</li>
 *   <li><b>Smooth Clamp</b>: \(\operatorname{sclamp}(x, a, b; s) = a + \operatorname{softplus}(x; a, s) - \operatorname{softplus}(x; b, s)\)</li>
 *   <li><b>Garrett Harmonic Coupling</b>: \(C^\infty\) complementary bound \(h(a, b) = \frac{a \cdot b}{a + b + \varepsilon}\)</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class SmoothMath {

    /** Small positive floor preventing division by zero in harmonic and rational forms. */
    public static final double EPSILON = 1.0e-12;

    private SmoothMath() {
        // Stateless mathematical utility
    }

    /**
     * Evaluates the \(C^\infty\) Fermi-Dirac logistic sigmoid \(\sigma(x; x_0, s) \in (0, 1)\).
     *
     * <p>$$\sigma(x; x_0, s) = \frac{1}{1 + \exp\!\left(-\frac{x - x_0}{s}\right)}$$</p>
     *
     * @param x     input physical observable
     * @param x0    inflection midpoint (\(\sigma(x_0; x_0, s) = 0.5\))
     * @param scale strictly positive transition width \(s > 0\) in physical units of {@code x}
     * @return smooth activation weight in \((0, 1)\)
     */
    public static double sigmoid(double x, double x0, double scale) {
        double s = Math.max(EPSILON, Math.abs(scale));
        double z = (x - x0) / s;
        if (z >= 40.0) {
            return 1.0;
        }
        if (z <= -40.0) {
            return 0.0;
        }
        return 1.0 / (1.0 + Math.exp(-z));
    }

    /**
     * Single-precision \(C^\infty\) Fermi-Dirac logistic sigmoid for DoD streaming loops.
     *
     * @param x     input physical observable
     * @param x0    inflection midpoint
     * @param scale positive transition width \(s > 0\)
     * @return smooth activation weight in \((0, 1)\)
     */
    public static float sigmoidF(float x, float x0, float scale) {
        return (float) sigmoid(x, x0, scale);
    }

    /**
     * Evaluates the \(C^\infty\) softplus function \(\operatorname{softplus}(x; 0, k) = \frac{\ln(1 + \exp(k \cdot x))}{k}\).
     *
     * @param x         input physical observable
     * @param sharpness transition sharpness \(k > 0\) (inverse transition scale)
     * @return strictly positive \(C^\infty\) ramp value
     */
    public static double softplus(double x, double sharpness) {
        return softplus(x, 0.0, sharpness);
    }

    /**
     * Single-precision \(C^\infty\) softplus function with default onset at 0.
     *
     * @param x         input physical observable
     * @param sharpness transition sharpness \(k > 0\)
     * @return strictly positive \(C^\infty\) ramp value
     */
    public static float softplusF(float x, float sharpness) {
        return (float) softplus(x, 0.0, sharpness);
    }

    /**
     * Single-precision \(C^\infty\) softplus function with explicit onset.
     *
     * @param x         input physical observable
     * @param x0        onset reference point
     * @param sharpness transition sharpness \(k > 0\)
     * @return strictly positive \(C^\infty\) ramp value
     */
    public static float softplusF(float x, float x0, float sharpness) {
        return (float) softplus(x, x0, sharpness);
    }

    /**
     * Evaluates the \(C^\infty\) scaled softplus function, providing a globally differentiable
     * replacement for the \(C^0\) ramp \(\max(0, x - x_0)\).
     *
     * <p>$$\operatorname{softplus}(x; x_0, k) = \frac{\ln\!\left(1 + \exp(k(x - x_0))\right)}{k}$$</p>
     *
     * <p>Its first derivative is strictly \(\frac{d}{dx}\operatorname{softplus}(x; x_0, k) = \sigma(x; x_0, 1/k) \in (0, 1)\).</p>
     *
     * @param x         input physical observable
     * @param x0        onset reference point
     * @param sharpness transition sharpness \(k > 0\)
     * @return strictly positive \(C^\infty\) ramp value
     */
    public static double softplus(double x, double x0, double sharpness) {
        double k = Math.max(EPSILON, Math.abs(sharpness));
        double kx = k * (x - x0);
        if (kx >= 40.0) {
            return x - x0;
        }
        if (kx <= -40.0) {
            return 0.0;
        }
        return Math.log1p(Math.exp(kx)) / k;
    }

    /**
     * Evaluates the \(C^\infty\) LogSumExp smooth maximum of two physical observables.
     *
     * <p>$$\operatorname{softmax}(a, b; k) = m + \frac{\ln\!\left(1 + \exp(-k |a - b|)\right)}{k}, \quad m = \max(a, b)$$</p>
     *
     * @param a         first observable
     * @param b         second observable
     * @param sharpness smoothing sharpness \(k > 0\)
     * @return smooth upper envelope \(\ge \max(a, b)\)
     */
    public static double softmax(double a, double b, double sharpness) {
        double k = Math.max(EPSILON, Math.abs(sharpness));
        double m = Math.max(a, b);
        double diff = -k * Math.abs(a - b);
        if (diff <= -40.0) {
            return m;
        }
        return m + Math.log1p(Math.exp(diff)) / k;
    }

    /**
     * Evaluates the \(C^\infty\) LogSumExp smooth minimum (differentiable Liebig Law of the Minimum).
     *
     * <p>$$\operatorname{softmin}(a, b; k) = -\operatorname{softmax}(-a, -b; k)$$</p>
     *
     * @param a         first limiting factor
     * @param b         second limiting factor
     * @param sharpness smoothing sharpness \(k > 0\)
     * @return smooth lower envelope \(\le \min(a, b)\)
     */
    public static double softmin(double a, double b, double sharpness) {
        return -softmax(-a, -b, sharpness);
    }

    /**
     * Evaluates a \(C^\infty\) smooth interval saturation replacing piecewise {@code Math.clamp(x, min, max)}.
     *
     * <p>$$\operatorname{sclamp}(x, a, b; k) = \operatorname{softmin}(b, \operatorname{softmax}(a, x; k); k)$$</p>
     *
     * @param x         input observable
     * @param min       asymptotic lower bound \(a\)
     * @param max       asymptotic upper bound \(b > a\)
     * @param sharpness transition sharpness \(k > 0\)
     * @return smoothly bounded value strictly inside \((a, b)\)
     */
    public static double smoothClamp(double x, double min, double max, double sharpness) {
        if (max <= min) {
            return min;
        }
        return softmin(max, softmax(min, x, sharpness), sharpness);
    }

    /**
     * Evaluates Michaelis-Menten / Monod saturation \(\frac{x}{x_{1/2} + x}\) for non-negative stocks.
     *
     * @param x        non-negative physical stock or flux \(x \ge 0\)
     * @param halfSat  half-saturation constant \(x_{1/2} > 0\)
     * @return saturation fraction in \([0, 1)\)
     */
    public static double michaelisMenten(double x, double halfSat) {
        double posX = softplus(x, 0.0, Math.max(1e-6, halfSat * 0.01));
        double k = Math.max(EPSILON, halfSat);
        return posX / (k + posX);
    }

    /**
     * Evaluates Garrett's (2011) \(C^\infty\) harmonic complementarity bound between two positive capacities
     * (e.g., gross structural capital per capita \(k\) and energized maintenance capacity \(k_{\text{energized}}\)).
     *
     * <p>$$h(a, b) = \frac{a \cdot b}{a + b + \varepsilon}$$</p>
     *
     * <p>Satisfies \(0 \le h(a, b) \le \min(a, b)\): if either input approaches \(0\), the harmonic
     * coupling smoothly vanishes without derivative discontinuities.</p>
     *
     * @param a first non-negative physical capacity
     * @param b second non-negative physical capacity
     * @return harmonically coupled active capacity
     */
    public static double harmonicCoupling(double a, double b) {
        if (a <= 0.0 || b <= 0.0) {
            return 0.0;
        }
        return (a * b) / (a + b + EPSILON);
    }
}
