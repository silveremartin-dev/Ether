/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

/**
 * Quantitative cartographic comparison result record.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class MapComparisonResult {
        /** Internal state: rmse (double). */
        private final double rmse;
        /** Internal state: pearson r (double). */
        private final double pearsonR;
        /** Internal state: ssim (double). */
        private final double ssim;
        /** Internal state: jaccard index (double). */
        private final double jaccardIndex;
        /** Internal state: dice coefficient (double). */
        private final double diceCoefficient;
        /** Internal state: kl divergence (double). */
        private final double klDivergence;
        /** Internal state: max delta value (double). */
        private final double maxDeltaValue;
        /** Internal state: max delta lng (double). */
        private final double maxDeltaLng;
        /** Internal state: max delta lat (double). */
        private final double maxDeltaLat;
        /** Internal state: diagnostic summary (String). */
        private final String diagnosticSummary;

        public MapComparisonResult(double rmse, double pearsonR, double ssim, double jaccardIndex,
                                   double diceCoefficient, double klDivergence, double maxDeltaValue,
                                   double maxDeltaLng, double maxDeltaLat, String diagnosticSummary) {
            this.rmse = rmse;
            this.pearsonR = pearsonR;
            this.ssim = ssim;
            this.jaccardIndex = jaccardIndex;
            this.diceCoefficient = diceCoefficient;
            this.klDivergence = klDivergence;
            this.maxDeltaValue = maxDeltaValue;
            this.maxDeltaLng = maxDeltaLng;
            this.maxDeltaLat = maxDeltaLat;
            this.diagnosticSummary = diagnosticSummary;
        }

        /**
         * Get rmse.
         *
         * @return the resulting computation or state reference
         */
        public double getRmse() { return rmse; }
        /**
         * Get pearson r.
         *
         * @return the resulting computation or state reference
         */
        public double getPearsonR() { return pearsonR; }
        /**
         * Get ssim.
         *
         * @return the resulting computation or state reference
         */
        public double getSsim() { return ssim; }
        /**
         * Get jaccard index.
         *
         * @return the resulting computation or state reference
         */
        public double getJaccardIndex() { return jaccardIndex; }
        /**
         * Get dice coefficient.
         *
         * @return the resulting computation or state reference
         */
        public double getDiceCoefficient() { return diceCoefficient; }
        /**
         * Get kl divergence.
         *
         * @return the resulting computation or state reference
         */
        public double getKlDivergence() { return klDivergence; }
        /**
         * Get max delta value.
         *
         * @return the resulting computation or state reference
         */
        public double getMaxDeltaValue() { return maxDeltaValue; }
        /**
         * Get max delta lng.
         *
         * @return the resulting computation or state reference
         */
        public double getMaxDeltaLng() { return maxDeltaLng; }
        /**
         * Get max delta lat.
         *
         * @return the resulting computation or state reference
         */
        public double getMaxDeltaLat() { return maxDeltaLat; }
        /**
         * Get diagnostic summary.
         *
         * @return the resulting computation or state reference
         */
        public String getDiagnosticSummary() { return diagnosticSummary; }

        /**
         * Get formatted report.
         *
         * @return the resulting computation or state reference
         */
        public String getFormattedReport() {
            return String.format(java.util.Locale.US,
                "🗺️ SPATIAL TENSOR & CARTOGRAPHIC COMPARISON REPORT:\n" +
                "  - RMSE (Spatial Grid Error): %.4f\n" +
                "  - Pearson Correlation (r): %.4f\n" +
                "  - SSIM (Structural Similarity): %.4f\n" +
                "  - Categorical Jaccard Index: %.2f%%\n" +
                "  - Dice Overlap Coefficient: %.2f%%\n" +
                "  - Kullback-Leibler Divergence: %.4f nats\n" +
                "  - Maximum Spatial Divergence: Delta = %.2f at (Lat: %.2f°, Lng: %.2f°)\n" +
                "  - Diagnostic Assessment: %s",
                rmse, pearsonR, ssim, jaccardIndex * 100.0, diceCoefficient * 100.0,
                klDivergence, maxDeltaValue, maxDeltaLat, maxDeltaLng, diagnosticSummary
            );
        }
    }
