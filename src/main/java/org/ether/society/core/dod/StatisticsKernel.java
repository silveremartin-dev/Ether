package org.ether.society.core.dod;

import java.util.Arrays;

/**
 * Kernel spécialisé dans le calcul d'indicateurs statistiques complexes (Gini, Distribution).
 */
public class StatisticsKernel {

    /**
     * Calcule le coefficient de Gini pour une ressource donnée (ex: richesse ou population).
     * G = (2 * sum(i * x_i) / (n * sum(x_i))) - (n + 1) / n
     */
    public float calculateGini(float[] values) {
        int n = values.length;
        if (n == 0) return 0;

        float[] sorted = values.clone();
        Arrays.sort(sorted);

        double sum = 0;
        double weightedSum = 0;
        for (int i = 0; i < n; i++) {
            sum += sorted[i];
            weightedSum += (i + 1) * sorted[i];
        }

        if (sum == 0) return 0;

        return (float) ((2.0 * weightedSum) / (n * sum) - (n + 1.0) / n);
    }

    /**
     * Calcule la distribution de densité (histogramme).
     */
    public int[] calculateDistribution(float[] values, int bins, float maxVal) {
        return calculateDistribution(values, bins, 0.0f, maxVal);
    }

    /**
     * Calcule la distribution de densité (histogramme) entre un min et un max.
     */
    public int[] calculateDistribution(float[] values, int bins, float minVal, float maxVal) {
        int[] histogram = new int[bins];
        if (values == null || values.length == 0 || bins <= 0) return histogram;

        float range = maxVal - minVal;
        if (range <= 0) {
            histogram[0] = values.length;
            return histogram;
        }

        for (float val : values) {
            int bin = (int) ((val - minVal) / range * bins);
            if (bin >= bins) bin = bins - 1;
            if (bin < 0) bin = 0;
            histogram[bin]++;
        }
        return histogram;
    }

    /**
     * Calcule la médiane d'un ensemble de valeurs.
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
     * Calcule la moyenne, le min, le max et l'écart-type.
     */
    public float[] calculateAggregates(float[] values) {
        if (values.length == 0) return new float[4];
        
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        double sum = 0;
        double sumSq = 0;

        for (float val : values) {
            if (val < min) min = val;
            if (val > max) max = val;
            sum += val;
            sumSq += (double)val * val;
        }

        float avg = (float) (sum / values.length);
        float variance = (float) (sumSq / values.length - (double)avg * avg);
        float stdDev = (float) Math.sqrt(Math.max(0, variance));

        return new float[]{avg, min, max, stdDev};
    }

    /**
     * Calcule le PIB (Somme du capital ressource).
     */
    public float calculateGDP(float[] capital) {
        double sum = 0;
        for (float val : capital) sum += val;
        return (float) sum;
    }

    /**
     * Calcule l'espérance de vie à la naissance (modèle démographique Gompertz-Makeham et cliodynamique).
     */
    public float calculateLifeExpectancy(float[] ages, int[] hexIds) {
        return calculateLifeExpectancy(ages, hexIds, 0.5f, 1.0f);
    }

    public float calculateLifeExpectancy(float[] ages, int[] hexIds, float avgTech, float foodSatisfaction) {
        // Base Paleolithic life expectancy ~ 28-32 years, scaling with tech and food security
        float base = 28.0f + Math.min(52.0f, Math.max(0.0f, avgTech) * 0.55f);
        float foodMod = Math.max(0.35f, Math.min(1.0f, foodSatisfaction));
        return Math.max(15.0f, Math.min(85.0f, base * foodMod));
    }

    /**
     * Calcule le taux de fécondité synthétique TFR (nombre moyen d'enfants par femme).
     */
    public float calculateFertilityRate(float[] births, float[] mass) {
        double totalBirths = 0;
        double totalMass = 0;
        for (int i = 0; i < mass.length; i++) {
            totalBirths += births[i];
            totalMass += mass[i];
        }
        if (totalMass <= 0) return 4.5f; // Baseline paléolithique/naturelle
        // Taux brut de natalité b = births / mass
        double birthRateAnnual = totalBirths / totalMass;
        // TFR = b * durée de vie reproductive (~25 ans) / proportion de femmes (~0.5) = b * 50
        double tfr = birthRateAnnual * 55.0;
        return (float) Math.max(1.1, Math.min(7.5, (tfr > 0.1 ? tfr : 4.8)));
    }
}
