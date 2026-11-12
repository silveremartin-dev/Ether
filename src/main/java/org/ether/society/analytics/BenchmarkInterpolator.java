/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.util.List;
import java.util.Map;

/**
 * <h1>BenchmarkInterpolator</h1>
 * <p>
 * Mathematical spline and monotone interpolation algorithms for cliodynamic time-series:
 * Linear, Catmull-Rom cubic spline, and Monotone Cubic Hermite Spline (PCHIP, Fritsch &amp; Carlson 1980)
 * ensuring monotonicity and avoiding Runge overshoots across historical crises.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class BenchmarkInterpolator {

    private BenchmarkInterpolator() {
        // Utility class
    }

    /**
     * Performs piecewise linear interpolation between adjacent time series data points.
     *
     * @param dataset empirical dataset map (Year -> Value)
     * @param x       sorted list of sample years
     * @param targetX target continuous evaluation year
     * @return interpolated value
     */
    public static double linearInterpolate(Map<Integer, Double> dataset, List<Integer> x, double targetX) {
        int intYear = (int) Math.round(targetX);
        if (dataset.containsKey(intYear)) return dataset.get(intYear);

        Integer lowerKey = null;
        Integer upperKey = null;

        for (Integer k : x) {
            if (k <= targetX) {
                lowerKey = k;
            } else if (k > targetX && upperKey == null) {
                upperKey = k;
                break;
            }
        }

        if (lowerKey == null && upperKey == null) return 0.0;
        if (lowerKey == null) return dataset.get(upperKey);
        if (upperKey == null) return dataset.get(lowerKey);

        double valLower = dataset.get(lowerKey);
        double valUpper = dataset.get(upperKey);

        double span = upperKey - lowerKey;
        if (span <= 0) return valLower;

        double fraction = (targetX - lowerKey) / span;
        return valLower + fraction * (valUpper - valLower);
    }

    /**
     * Performs Piecewise Cubic Hermite Interpolating Polynomial (PCHIP) evaluation (Fritsch &amp; Carlson 1980).
     *
     * @param dataset empirical dataset map (Year -> Value)
     * @param x       sorted list of sample years
     * @param targetX target continuous evaluation year
     * @return monotonic interpolated value
     */
    public static double pchipInterpolate(Map<Integer, Double> dataset, List<Integer> x, double targetX) {
        int n = x.size();
        double[] h = new double[n - 1];
        double[] delta = new double[n - 1];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            y[i] = dataset.get(x.get(i));
        }

        for (int i = 0; i < n - 1; i++) {
            h[i] = x.get(i + 1) - x.get(i);
            delta[i] = (y[i + 1] - y[i]) / h[i];
        }

        double[] d = new double[n];

        for (int i = 1; i < n - 1; i++) {
            if (delta[i - 1] * delta[i] <= 0.0) {
                d[i] = 0.0;
            } else {
                double w1 = 2.0 * h[i] + h[i - 1];
                double w2 = h[i] + 2.0 * h[i - 1];
                d[i] = (w1 + w2) / (w1 / delta[i - 1] + w2 / delta[i]);
            }
        }

        d[0] = pchipEndDerivative(h[0], h[1], delta[0], delta[1]);
        d[n - 1] = pchipEndDerivative(h[n - 2], (n >= 4 ? h[n - 3] : h[n - 2]), delta[n - 2], (n >= 3 ? delta[n - 3] : delta[n - 2]));

        int k = 0;
        for (int i = 0; i < n - 1; i++) {
            if (targetX >= x.get(i) && targetX <= x.get(i + 1)) {
                k = i;
                break;
            }
        }

        double hk = h[k];
        double t = (targetX - x.get(k)) / hk;
        double t2 = t * t;
        double t3 = t2 * t;

        double h00 = 2.0 * t3 - 3.0 * t2 + 1.0;
        double h10 = t3 - 2.0 * t2 + t;
        double h01 = -2.0 * t3 + 3.0 * t2;
        double h11 = t3 - t2;

        return h00 * y[k] + h10 * hk * d[k] + h01 * y[k + 1] + h11 * hk * d[k + 1];
    }

    private static double pchipEndDerivative(double h1, double h2, double delta1, double delta2) {
        double d = ((2.0 * h1 + h2) * delta1 - h1 * delta2) / (h1 + h2);
        if (d * delta1 < 0.0) {
            d = 0.0;
        } else if ((delta1 * delta2 < 0.0) && (Math.abs(d) > Math.abs(3.0 * delta1))) {
            d = 3.0 * delta1;
        }
        return d;
    }

    /**
     * Performs Catmull-Rom cubic spline interpolation.
     *
     * @param dataset empirical dataset map (Year -> Value)
     * @param x       sorted list of sample years
     * @param targetX target continuous evaluation year
     * @return spline interpolated value
     */
    public static double catmullRomInterpolate(Map<Integer, Double> dataset, List<Integer> x, double targetX) {
        int n = x.size();
        int k = 0;
        for (int i = 0; i < n - 1; i++) {
            if (targetX >= x.get(i) && targetX <= x.get(i + 1)) {
                k = i;
                break;
            }
        }

        double y0 = dataset.get(x.get(Math.max(0, k - 1)));
        double y1 = dataset.get(x.get(k));
        double y2 = dataset.get(x.get(Math.min(n - 1, k + 1)));
        double y3 = dataset.get(x.get(Math.min(n - 1, k + 2)));

        double hk = x.get(Math.min(n - 1, k + 1)) - x.get(k);
        if (hk <= 0) return y1;

        double t = (targetX - x.get(k)) / hk;
        double t2 = t * t;
        double t3 = t2 * t;

        double a0 = -0.5 * y0 + 1.5 * y1 - 1.5 * y2 + 0.5 * y3;
        double a1 = y0 - 2.5 * y1 + 2.0 * y2 - 0.5 * y3;
        double a2 = -0.5 * y0 + 0.5 * y2;
        double a3 = y1;

        return a0 * t3 + a1 * t2 + a2 * t + a3;
    }
}
