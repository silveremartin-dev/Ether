/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.util.Map;

/**
 * Data-driven configuration rule engine for calibration diagnostic suggestions.
 * Analyzes empirical MAPE deviations and R² fit against historical ground truth benchmarks
 * to generate actionable parameter tuning recommendations for simulation engines.
 */
public class CalibrationDiagnosticRules {

    public static String generateTuningSuggestions(Map<String, Double> mapes, double rSquared, int divergenceYear) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 🛠️ 3. Pistes de Calibration des Paramètres Moteur (Analyse Dynamique orientée Données)\n\n");

        if (rSquared >= 0.95 && mapes.values().stream().allMatch(m -> m < 15.0)) {
            sb.append("✅ **Modèle Optimalement Calibré** :\n");
            sb.append("   - Les trajectoires simulées sont remarquablement alignées avec la réalité historique ($R^2 = ")
              .append(String.format("%.4f", rSquared))
              .append("$). Aucune révision majeure des constantes moteurs n'est requise.\n\n");
            return sb.toString();
        }

        int ruleCount = 1;

        // Rule 1: Demographic Engine
        Double popMape = getMapeForKeyword(mapes, "Population");
        if (popMape != null && popMape > 15.0) {
            sb.append(String.format("%d. **Moteur Démographique & Capacité d'Accueil (`DemographicEngine`)** [MAPE = %.1f%%] :\n", ruleCount++, popMape));
            if (popMape > 35.0) {
                sb.append("   - *Diagnostic* : Fort écart de vitesse de croissance démographique. La capacité de charge $K(t)$ sature trop tôt ou le taux de croissance net surestime la mortalité.\n");
                sb.append("   - *Actions recommandées* : Augmenter `carryingCapacityScale` de 1.15x et ajuster `agricultural_spread_rate` de 0.010 à 0.018/an.\n\n");
            } else {
                sb.append("   - *Diagnostic* : Légère dérive logistique de la population au niveau des transitions d'époques.\n");
                sb.append("   - *Actions recommandées* : Affiner les seuils de regroupement de cohortes `targetCohortSize` et étalonner la fertilité résiduelle.\n\n");
            }
        }

        // Rule 2: Ecology & Resource Engine
        Double energyMape = getMapeForKeyword(mapes, "Énergie");
        Double co2Mape = getMapeForKeyword(mapes, "CO2");
        if ((energyMape != null && energyMape > 15.0) || (co2Mape != null && co2Mape > 15.0)) {
            double maxEco = Math.max(energyMape != null ? energyMape : 0, co2Mape != null ? co2Mape : 0);
            sb.append(String.format("%d. **Moteur Écologique & Empreinte Carbone (`EcologyEngine`)** [MAPE max = %.1f%%] :\n", ruleCount++, maxEco));
            sb.append("   - *Diagnostic* : Décalage dans la consommation d'énergie primaire et la cinétique des émissions de gaz à effet de serre.\n");
            sb.append("   - *Actions recommandées* : Ré-étalonner le coefficient d'extraction énergétique `alpha_burn` (de 0.040 à 0.028) et réduire la consommation de biomasse par habitant `wood_consumption_per_capita`.\n\n");
        }

        // Rule 3: Economic & Capital Engine (GWP)
        Double gwpMape = getMapeForKeyword(mapes, "Produit");
        if (gwpMape != null && gwpMape > 15.0) {
            sb.append(String.format("%d. **Moteur Économique & Stock de Capital (`SociologyEngine / Capital`)** [MAPE = %.1f%%] :\n", ruleCount++, gwpMape));
            sb.append("   - *Diagnostic* : La production de richesse globale (GWP) sous-estime ou sur-estime le rendement du capital fixe ($K_0$).\n");
            sb.append("   - *Actions recommandées* : Ajuster l'élasticité de production `alpha_k` (ex: 0.33) et rehausser le taux d'accumulation du savoir technologique `initialInformationPerCapita`.\n\n");
        }

        // Rule 4: Settlement & Urbanization Engine
        Double urbanMape = getMapeForKeyword(mapes, "Urbanisation");
        if (urbanMape != null && urbanMape > 15.0) {
            sb.append(String.format("%d. **Moteur de Peuplement & Métropolisation (`SettlementEngine`)** [MAPE = %.1f%%] :\n", ruleCount++, urbanMape));
            sb.append("   - *Diagnostic* : Taux d'urbanisation en décalage par rapport aux données d'agglomération historique HYDE.\n");
            sb.append("   - *Actions recommandées* : Ajuster le facteur de migration vers les clusters urbains `urban_migration_rate` et vérifier la densité critique H3.\n\n");
        }

        // Rule 5: Cultural & Educational Engine (Literacy)
        Double literacyMape = getMapeForKeyword(mapes, "Alphabétisation");
        if (literacyMape != null && literacyMape > 15.0) {
            sb.append(String.format("%d. **Moteur Socioculturel & Diffusion de l'Information (`CulturalSociologyEngine`)** [MAPE = %.1f%%] :\n", ruleCount++, literacyMape));
            sb.append("   - *Diagnostic* : La vitesse de propagation du savoir archivé et de l'instruction publique s'écarte des benchmarks historiques.\n");
            sb.append("   - *Actions recommandées* : Augmenter le taux de rétention du savoir `information_retention_rate` et réduire l'usure de transmission.\n\n");
        }

        // Rule 6: Institutional & Monetary Engine
        Double currencyMape = getMapeForKeyword(mapes, "Monétaire");
        if (currencyMape != null && currencyMape > 15.0) {
            sb.append(String.format("%d. **Moteur Institutionnel & Stabilité Monétaire (`InstitutionalEngine`)** [MAPE = %.1f%%] :\n", ruleCount++, currencyMape));
            sb.append("   - *Diagnostic* : Trajectoire de détérioration monétaire ou d'instabilité politique asynchrone.\n");
            sb.append("   - *Actions recommandées* : Réduire la sensibilité au choc fiscal et modérer le taux de dégradation de la cohésion sociale (Asabiyyah).\n\n");
        }

        if (ruleCount == 1) {
            sb.append("1. **Ajustements Généraux Moteur** :\n");
            sb.append("   - *Diagnostic* : Légères dérives globales disséminées sur plusieurs indicateurs secondaires.\n");
            sb.append("   - *Actions recommandées* : Exécuter l'auto-calibrateur `HistoricalAutoCalibrator.evaluateAndAutoCalibrate()` pour affiner la sélection des modules Type B.\n\n");
        }

        return sb.toString();
    }

    private static Double getMapeForKeyword(Map<String, Double> mapes, String keyword) {
        for (var entry : mapes.entrySet()) {
            if (entry.getKey().toLowerCase().contains(keyword.toLowerCase())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
