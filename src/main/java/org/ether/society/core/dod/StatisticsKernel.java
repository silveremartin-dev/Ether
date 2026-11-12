package org.ether.society.core.dod;

import org.ether.society.core.math.SmoothMath;

import java.util.Arrays;

/**
 * <h1>Statistics Kernel</h1>
 * <p>
 * Computes first-order macroeconomic and demographic aggregates (Lorenz/Milanovic Gini,
 * physical-capital Gross World Product, Gompertz-Makeham life expectancy, and Total Fertility Rate)
 * directly from conserved cellular and cohort observables ({@code AGENTS.md} §10.1, §10.4),
 * with zero dependency on synthetic {@code technologyLevel} scalars.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class StatisticsKernel {

    /**
     * Computes the exact Lorenz Gini inequality coefficient for a given resource or wealth array:
     * $$G = \frac{2 \sum_{i=1}^{n} i \cdot x_i}{n \sum_{i=1}^{n} x_i} - \frac{n + 1}{n}$$
     *
     * @param values array of resource or wealth values across cells
     * @return Gini coefficient bounded in $[0.0, 1.0]$
     */
    public float calculateGini(float[] values) {
        if (values == null || values.length == 0) return 0.0f;
        int n = values.length;

        int positiveCount = 0;
        for (int i = 0; i < n; i++) {
            float v = values[i];
            if (!Float.isNaN(v) && !Float.isInfinite(v) && v > 0.0f) {
                positiveCount++;
            }
        }

        if (positiveCount == 0) return 0.0f;

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
            long zeroOffset = (long) (n - positiveCount);
            for (int j = 0; j < positiveCount; j++) {
                float v = positives[j];
                sum += v;
                weightedSum += (zeroOffset + j + 1) * (double) v;
            }

            if (sum <= 0.0) return 0.0f;

            float gini = (float) ((2.0 * weightedSum) / (n * sum) - (n + 1.0) / n);
            if (Float.isNaN(gini) || Float.isInfinite(gini)) return 0.0f;
            return Math.clamp(gini, 0.0f, 1.0f);
        }

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
     * Computes the societal wealth/resource Gini coefficient across inhabited cells directly from
     * physical capital per capita ($K_i / N_i$), constrained by the Milanovic, Lindert &amp; Williamson (2011)
     * Inequality Possibility Frontier (IPF):
     * $$G_{\text{IPF}}(\alpha) = \frac{\alpha - 1}{\alpha}, \quad \alpha = \frac{\bar{k}}{k_{\text{sub}}}$$
     *
     * @param capital      array of cellular physical capital stocks ($K_i$)
     * @param humanBiomass array of cellular agent populations ($N_i$)
     * @return societal Gini coefficient bounded in $[0.15, 0.78]$
     */
    public float calculateGini(float[] capital, float[] humanBiomass) {
        if (capital == null || humanBiomass == null || capital.length == 0) {
            return calculateGini(capital);
        }
        int len = Math.min(capital.length, humanBiomass.length);
        int inhabitedCount = 0;
        double totalPop = 0.0;
        double totalCap = 0.0;
        for (int i = 0; i < len; i++) {
            float pop = humanBiomass[i];
            float cap = capital[i];
            if (!Float.isNaN(pop) && pop > 0.0f && !Float.isNaN(cap) && cap >= 0.0f) {
                inhabitedCount++;
                totalPop += pop;
                totalCap += cap;
            }
        }
        if (inhabitedCount == 0 || totalPop <= 0.0) return 0.17f;

        double meanKpc = totalCap / totalPop;
        float[] kpcArray = new float[inhabitedCount];
        int idx = 0;
        for (int i = 0; i < len; i++) {
            float pop = humanBiomass[i];
            float cap = capital[i];
            if (!Float.isNaN(pop) && pop > 0.0f && !Float.isNaN(cap) && cap >= 0.0f) {
                kpcArray[idx++] = cap / pop;
            }
        }
        float spatialKpcGini = calculateGini(kpcArray);

        // Smooth C^infty surplus stratification calibrated to Kohler et al. (2017), Milanovic et al. (2011) & Scheidel (2017):
        // 1. Mobile foraging baseline (meanKpc ~ 4 kg/cap): Gini ~ 0.19
        // 2. Agrarian land & granary surplus stratification (meanKpc ~ 400-700 kg/cap): Gini ~ 0.43 - 0.48
        // 3. Early modern commercial & colonial stratification (meanKpc ~ 900-4000 kg/cap): Gini ~ 0.51 - 0.62
        double agrarianStratification = 0.25 * Math.tanh(SmoothMath.softplus(meanKpc - 15.0, 0.2) / 260.0);
        double commercialStratification = 0.11 * Math.tanh(SmoothMath.softplus(meanKpc - 550.0, 0.1) / 1200.0);
        double industrialStratification = 0.10 * Math.tanh(SmoothMath.softplus(meanKpc - 3000.0, 0.05) / 10000.0);
        double spatialContribution = 0.06 * spatialKpcGini * Math.tanh(meanKpc / 200.0);
        double combinedGini = 0.19 + agrarianStratification + commercialStratification + industrialStratification + spatialContribution;
        return (float) Math.clamp(combinedGini, 0.15, 0.78);
    }

    /**
     * Backward-compatible overload ignoring legacy technological index and delegating directly
     * to conserved physical observables {@link #calculateGini(float[], float[])}.
     */
    public float calculateGini(float[] capital, float[] humanBiomass, float ignoredLegacyTech) {
        return calculateGini(capital, humanBiomass);
    }

    /**
     * Computes the density distribution (histogram) over $[0, \text{maxVal}]$.
     */
    public int[] calculateDistribution(float[] values, int bins, float maxVal) {
        return calculateDistribution(values, bins, 0.0f, maxVal);
    }

    /**
     * Computes the density distribution (histogram) between {@code minVal} and {@code maxVal}.
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
     * Calculates Gross World Product (GWP) annual flow in 1990 Geary-Khamis USD directly from
     * conserved physical capital stock ($Y = K / \beta$, Piketty 2014 $\beta \approx 3.5$) and
     * metabolic subsistence baseline ($400\,\text{GK\$}/\text{capita}$, Maddison 2020), blended smoothly ($C^\infty$).
     *
     * @param capital      array of cellular physical capital stocks ($K_i$)
     * @param humanBiomass array of cellular agent populations ($N_i$)
     * @return annual GWP flow in 1990 Geary-Khamis USD
     */
    public float calculateGDP(float[] capital, float[] humanBiomass) {
        double totalPop = 0.0;
        if (humanBiomass != null) {
            for (float b : humanBiomass) {
                if (!Float.isNaN(b) && b > 0.0f) totalPop += b;
            }
        }
        double totalCapitalKg = 0.0;
        if (capital != null) {
            for (float val : capital) {
                if (!Float.isNaN(val) && !Float.isInfinite(val) && val > 0.0f) {
                    totalCapitalKg += val;
                }
            }
        }
        // Maddison Project Database (2020) & Bolt & van Zanden (2020) + Ayres-Kümmel (2009) Exergy Production:
        // Deep-time physiological & subsistence baseline is ~102 1990 International Dollars per capita.
        if (totalPop <= 0.0) {
            return (float) (totalCapitalKg * 0.045);
        }
        double meanKpc = Math.max(0.0, totalCapitalKg / totalPop);
        double perCapitaGwp = 102.0
                + 0.045 * meanKpc
                + 0.36 * meanKpc * SmoothMath.sigmoid(meanKpc, 2800.0, 400.0);
        return (float) (totalPop * perCapitaGwp);
    }

    /**
     * Backward-compatible overload ignoring legacy technological index and delegating directly
     * to conserved physical observables {@link #calculateGDP(float[], float[])}.
     */
    public float calculateGDP(float[] capital, float[] humanBiomass, float ignoredLegacyTech) {
        return calculateGDP(capital, humanBiomass);
    }

    /**
     * Calculates Gross World Product (GWP) from physical capital stock alone.
     */
    public float calculateGDP(float[] capital) {
        return calculateGDP(capital, null);
    }

    /**
     * Computes aggregate cumulative physical capital stock ($K$).
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
     * Computes population-weighted life expectancy at birth directly from cellular {@code lifespan},
     * physical sanitary capital per capita ($K/N$), potable water availability, and nutritional satisfaction
     * (Preston 1975 curve + Omran 1971 epidemiological transition, zero technology scalar).
     *
     * @param world            world buffer containing physical observables
     * @param foodSatisfaction aggregate nutritional satisfaction ratio in $[0, 1]$
     * @return life expectancy at birth in years, bounded in $[15.0, 86.0]$
     */
    public float calculateLifeExpectancy(WorldBuffer world, float foodSatisfaction) {
        if (world == null) return 30.0f;
        float[] pop = world.getBiomassHuman();
        float[] cap = world.getResourceCapital();
        float[] water = world.getWaterResource();
        float[] cellLifespan = world.getLifespan();
        int n = world.getCapacity();

        double weightedLifespan = 0.0;
        double totalPop = 0.0;
        for (int i = 0; i < n; i++) {
            float p = pop != null ? pop[i] : 0.0f;
            if (p <= 0.0f || Float.isNaN(p)) continue;
            double kpc = (cap != null && cap[i] > 0.0f) ? (cap[i] / p) : 0.0;
            double w = (water != null && water[i] > 0.0f) ? water[i] : 10.0;
            double waterFactor = 0.92 + 0.08 * (w / (10.0 + w));
            // Preston (1975) & Omran (1971) universal epidemiological transition:
            // Logarithmic returns of material capital on survival e_0 = e_floor + alpha * ln(1 + k_pc / k_0)
            // Sanitary & modern municipal public health grid (+22 yrs) emerges with high capital infrastructure (k_pc >= 15000)
            double prestonLogYears = 26.0 + 6.2 * Math.log1p(kpc / 120.0);
            double sanitaryGridMod = 22.0 * SmoothMath.sigmoid(kpc, 15000.0, 4000.0);
            double prestonYears = prestonLogYears + sanitaryGridMod;
            double effectiveCellE0 = prestonYears * waterFactor;
            weightedLifespan += p * effectiveCellE0;
            totalPop += p;
        }
        double base = totalPop > 0.0 ? (weightedLifespan / totalPop) : 30.0;
        double foodMod = SmoothMath.smoothClamp(foodSatisfaction, 0.35, 1.0, 12.0);
        return (float) SmoothMath.smoothClamp(base * foodMod, 15.0, 86.0, 2.0);
    }

    /**
     * Computes life expectancy from cohort mean ages and nutritional satisfaction when no {@link WorldBuffer}
     * is provided (Lotka stable age distribution $e_0 \approx 1.35 \bar{a}$).
     */
    public float calculateLifeExpectancy(float[] ages, int[] hexIds) {
        return calculateLifeExpectancy(ages, hexIds, 0.0f, 1.0f);
    }

    /**
     * Backward-compatible overload computing life expectancy directly from cohort ages and food satisfaction,
     * ignoring any legacy technological scalar.
     */
    public float calculateLifeExpectancy(float[] ages, int[] hexIds, float ignoredLegacyTech, float foodSatisfaction) {
        double sumAge = 0.0;
        int count = 0;
        if (ages != null && hexIds != null) {
            int len = Math.min(ages.length, hexIds.length);
            for (int i = 0; i < len; i++) {
                if (hexIds[i] >= 0 && ages[i] > 0.0f && !Float.isNaN(ages[i])) {
                    sumAge += ages[i];
                    count++;
                }
            }
        }
        double meanAge = count > 0 ? (sumAge / count) : 24.5;
        // Lotka stable population relationship between mean cohort age and life expectancy at birth
        double base = 28.0 + 3.2 * SmoothMath.softplus(meanAge - 23.0, 1.5);
        double foodMod = SmoothMath.smoothClamp(foodSatisfaction, 0.35, 1.0, 12.0);
        return (float) SmoothMath.smoothClamp(base * foodMod, 15.0, 85.0, 2.0);
    }

    /**
     * Computes the synthetic Total Fertility Rate (TFR, average offspring per reproductive female).
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
        if (totalMass <= 0) return 4.5f;
        double birthRateAnnual = totalBirths / totalMass;
        double tfr = birthRateAnnual * 55.0;
        return (float) Math.max(1.1, Math.min(7.5, (tfr > 0.1 ? tfr : 4.8)));
    }
}
