package org.ether.society.core.dod;

import java.util.Arrays;

/**
 * <h1>Statistics Kernel</h1>
 * <p>
 * Core operational component for the Ether civilizational and planetary simulation framework.<br>
 * Integrates cellular dynamics, data structures, and deterministic state transitions.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class StatisticsKernel {

    /**
     * Computes the Gini inequality coefficient for a given resource or demographic distribution:
     * $$G = \frac{2 \sum_{i=1}^{n} i \cdot x_i}{n \sum_{i=1}^{n} x_i} - \frac{n + 1}{n}$$
     * Uses an $O(m \log m)$ positive-element sparse sort when positive entries are sparse.
     *
     * @param values array of resource or wealth values across cells
     * @return Gini coefficient bounded in $[0.0, 1.0]$
     */
    public float calculateGini(float[] values) {
        if (values == null || values.length == 0) return 0.0f;
        int n = values.length;

        // Step 1: Count non-zero positive elements in single contiguous scan
        int positiveCount = 0;
        for (int i = 0; i < n; i++) {
            float v = values[i];
            if (!Float.isNaN(v) && !Float.isInfinite(v) && v > 0.0f) {
                positiveCount++;
            }
        }

        if (positiveCount == 0) return 0.0f;

        // Step 2: If positive count is small compared to n (typical in geographical grids),
        // only sort the m positive numbers. All n - m zeros trivially sort to the front (indices 0..n-m-1)
        // and contribute 0 to sum and 0 to weightedSum.
        if (positiveCount < n / 2) {
            float[] positives = new float[positiveCount];
            int idx = 0;
            for (int i = 0; i < n; i++) {
                float v = values[i];
                if (!Float.isNaN(v) && !Float.isInfinite(v) && v > 0.0f) {
                    positives[idx++] = v;
                }
            }
            Arrays.sort(positives);

            double sum = 0.0;
            double weightedSum = 0.0;
            long zeroOffset = (long) (n - positiveCount); // 0-indexed positions before positives
            for (int j = 0; j < positiveCount; j++) {
                float v = positives[j];
                sum += v;
                // 1-based index in the fully sorted array of length n is (zeroOffset + j + 1)
                weightedSum += (zeroOffset + j + 1) * (double) v;
            }

            if (sum <= 0.0) return 0.0f;

            float gini = (float) ((2.0 * weightedSum) / (n * sum) - (n + 1.0) / n);
            if (Float.isNaN(gini) || Float.isInfinite(gini)) return 0.0f;
            return Math.clamp(gini, 0.0f, 1.0f);
        }

        // Fallback for dense arrays: sort full array
        float[] sorted = values.clone();
        Arrays.sort(sorted);

        double sum = 0;
        double weightedSum = 0;
        for (int i = 0; i < n; i++) {
            float v = sorted[i];
            if (Float.isNaN(v) || Float.isInfinite(v) || v < 0) v = 0;
            sum += v;
            weightedSum += (i + 1) * (double) v;
        }

        if (sum <= 0.0) return 0.0f;

        float gini = (float) ((2.0 * weightedSum) / (n * sum) - (n + 1.0) / n);
        if (Float.isNaN(gini) || Float.isInfinite(gini)) return 0.0f;
        return Math.clamp(gini, 0.0f, 1.0f);
    }

    /**
     * Computes the density distribution (histogram) over $[0, \text{maxVal}]$.
     *
     * @param values input data array
     * @param bins number of histogram bins
     * @param maxVal upper bound of the histogram
     * @return bin count array of length {@code bins}
     */
    public int[] calculateDistribution(float[] values, int bins, float maxVal) {
        return calculateDistribution(values, bins, 0.0f, maxVal);
    }

    /**
     * Computes the density distribution (histogram) between {@code minVal} and {@code maxVal}.
     *
     * @param values input data array
     * @param bins number of histogram bins
     * @param minVal lower bound of the histogram
     * @param maxVal upper bound of the histogram
     * @return bin count array of length {@code bins}
     */
    public int[] calculateDistribution(float[] values, int bins, float minVal, float maxVal) {
        if (bins <= 0) return new int[0];
        int[] histogram = new int[bins];
        if (values == null || values.length == 0) return histogram;

        float range = maxVal - minVal;
        if (range <= 0) {
            histogram[0] = values.length;
            return histogram;
        }

        for (float val : values) {
            if (Float.isNaN(val)) continue;
            int bin = (int) ((val - minVal) / range * bins);
            if (bin >= bins) bin = bins - 1;
            if (bin < 0) bin = 0;
            histogram[bin]++;
        }
        return histogram;
    }

    /**
     * Computes the exact median of an array of values.
     *
     * @param values input data array
     * @return median value, or {@code 0.0f} if empty
     */
    public float calculateMedian(float[] values) {
        if (values == null || values.length == 0) return 0.0f;
        float[] copy = values.clone();
        Arrays.sort(copy);
        int mid = copy.length / 2;
        if (copy.length % 2 == 0) {
            return (copy[mid - 1] + copy[mid]) / 2.0f;
        } else {
            return copy[mid];
        }
    }

    /**
     * Computes the mean, minimum, maximum, and standard deviation of an array in a single pass.
     *
     * @param values input data array
     * @return 4-element array {@code [mean, min, max, stdDev]}
     */
    public float[] calculateAggregates(float[] values) {
        if (values == null || values.length == 0) return new float[4];

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        double sum = 0;
        double sumSq = 0;
        int validCount = 0;

        for (float val : values) {
            if (Float.isNaN(val) || Float.isInfinite(val)) continue;
            if (val < min) min = val;
            if (val > max) max = val;
            sum += val;
            sumSq += (double) val * val;
            validCount++;
        }

        if (validCount == 0) return new float[4];

        float avg = (float) (sum / validCount);
        float variance = (float) (sumSq / validCount - (double) avg * avg);
        float stdDev = (float) Math.sqrt(Math.max(0.0, variance));

        return new float[]{avg, min, max, stdDev};
    }

    /**
     * Computes aggregate Gross Domestic Product (annual flow output Y derived from capital stock K).
     *
     * <p>In macroeconomics (Maddison Project, Solow Growth Model, Piketty 2014), the capital-output
     * ratio \(\beta = K / Y \approx 4.35\) connects the cumulative capital wealth stock \(K\) to annual output flow \(Y\):
     * \[ Y = \frac{\sum K}{\beta} \]</p>
     *
     * @param capital array of cellular resource capital
     * @return annual planetary GDP flow
     */
    /**
     * Calculates Gross World Product (GWP) annual flow in 1990 Geary-Khamis USD based on Maddison Project 2020.
     * Combines biological subsistence baseline ($450 / capita) with capital output contribution ($Y = A K^\alpha L^{1-\alpha}$).
     *
     * @param capital array of cellular resource capital (kg)
     * @param humanBiomass array of human population / biomass per cell
     * @param avgTech planetary average technology level
     * @return annual GDP flow in 1990 USD
     */
    public float calculateGDP(float[] capital, float[] humanBiomass, float avgTech) {
        double totalPop = 0;
        if (humanBiomass != null) {
            for (float b : humanBiomass) {
                if (!Float.isNaN(b) && b > 0) totalPop += b;
            }
        }
        double totalCapitalKg = 0;
        if (capital != null) {
            for (float val : capital) {
                if (!Float.isNaN(val) && !Float.isInfinite(val) && val > 0) {
                    totalCapitalKg += val;
                }
            }
        }
        // Maddison Project 2020: Baseline biological subsistence per capita is ~450 USD 1990 Geary-Khamis
        // Scaled by technological productivity factor: y_sub(T) = 450 * (1.0 + 0.08 * T)
        double tech = Math.max(0.1, avgTech);
        double subsistenceGDP = totalPop * 450.0 * (1.0 + 0.08 * tech);

        // Capital contribution to output flow: manufactured physical capital (kg) converted to 1990 USD value
        // ($15 / kg of bronze/steel/machinery in 1990 USD) divided by capital-to-output ratio beta = 3.5
        double capitalGDP = (totalCapitalKg * 15.0) / 3.5;

        return (float) (subsistenceGDP + capitalGDP);
    }

    /**
     * Calculates Gross World Product (GWP) with default demographic parameters.
     *
     * @param capital array of cellular resource capital
     * @return annual planetary GDP flow
     */
    public float calculateGDP(float[] capital) {
        return calculateGDP(capital, null, 1.0f);
    }

    /**
     * Computes aggregate cumulative capital stock (K).
     *
     * @param capital array of cellular resource capital
     * @return total physical capital stock
     */
    public float calculateTotalCapitalStock(float[] capital) {
        if (capital == null || capital.length == 0) return 0.0f;
        double sum = 0;
        for (float val : capital) {
            if (!Float.isNaN(val) && !Float.isInfinite(val)) {
                sum += val;
            }
        }
        return (float) sum;
    }

    /**
     * Computes life expectancy at birth using default technological and nutritional baselines.
     *
     * @param ages array of cohort ages
     * @param hexIds array of cell indices
     * @return life expectancy in years
     */
    public float calculateLifeExpectancy(float[] ages, int[] hexIds) {
        return calculateLifeExpectancy(ages, hexIds, 0.5f, 1.0f);
    }

    /**
     * Computes life expectancy at birth (Gompertz-Makeham and cliodynamic demographic model).
     *
     * @param ages array of cohort ages
     * @param hexIds array of cell indices
     * @param avgTech average technological index
     * @param foodSatisfaction nutritional satisfaction ratio in $[0, 1]$
     * @return life expectancy at birth in years, bounded in $[15.0, 85.0]$
     */
    public float calculateLifeExpectancy(float[] ages, int[] hexIds, float avgTech, float foodSatisfaction) {
        // Base Paleolithic life expectancy ~ 28-32 years, scaling with tech and food security
        float base = 28.0f + Math.min(52.0f, Math.max(0.0f, avgTech) * 0.55f);
        float foodMod = Math.max(0.35f, Math.min(1.0f, foodSatisfaction));
        return Math.max(15.0f, Math.min(85.0f, base * foodMod));
    }

    /**
     * Computes the synthetic Total Fertility Rate (TFR, average children per woman).
     *
     * @param births array of annual births per cohort/cell
     * @param mass array of total population mass per cohort/cell
     * @return synthetic TFR bounded in $[1.1, 7.5]$
     */
    public float calculateFertilityRate(float[] births, float[] mass) {
        if (births == null || mass == null) return 4.5f;
        int len = Math.min(births.length, mass.length);
        double totalBirths = 0;
        double totalMass = 0;
        for (int i = 0; i < len; i++) {
            totalBirths += births[i];
            totalMass += mass[i];
        }
        if (totalMass <= 0) return 4.5f; // Natural Paleolithic baseline
        // Crude birth rate b = births / mass
        double birthRateAnnual = totalBirths / totalMass;
        // TFR = b * reproductive lifespan (~27.5 yrs) / female ratio (~0.5) = b * 55
        double tfr = birthRateAnnual * 55.0;
        return (float) Math.max(1.1, Math.min(7.5, (tfr > 0.1 ? tfr : 4.8)));
    }
}
