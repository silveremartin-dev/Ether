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
        int[] histogram = new int[bins];
        if (maxVal <= 0) return histogram;

        for (float val : values) {
            int bin = (int) (val / maxVal * bins);
            if (bin >= bins) bin = bins - 1;
            if (bin < 0) bin = 0;
            histogram[bin]++;
        }
        return histogram;
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
     * Calcule l'espérance de vie moyenne (âge moyen des cohortes).
     */
    public float calculateLifeExpectancy(float[] ages, int[] hexIds) {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < hexIds.length; i++) {
            if (hexIds[i] != -1) {
                sum += ages[i];
                count++;
            }
        }
        return count > 0 ? (float) (sum / count) : 0;
    }

    /**
     * Calcule le taux de fécondité (naissances / masse totale).
     */
    public float calculateFertilityRate(float[] births, float[] mass) {
        double totalBirths = 0;
        double totalMass = 0;
        for (int i = 0; i < mass.length; i++) {
            totalBirths += births[i];
            totalMass += mass[i];
        }
        return totalMass > 0 ? (float) (totalBirths / totalMass * 1000.0f) : 0;
    }
}
