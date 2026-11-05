/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.data.TemporalMapTensorManager;
import org.ether.society.i18n.I18n;
import org.ether.society.model.AutoEpochScenarioGenerator;

/**
 * Presenter and localization helper for {@link AutoEpochScenarioDialog}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class AutoEpochDialogPresenter {

    private AutoEpochDialogPresenter() {}

    /**
     * Resolves human-readable explanation for a data fallback strategy.
     *
     * @param strategy the fallback strategy
     * @return localized description
     */
    public static String getStrategyExplanation(TemporalMapTensorManager.DataFallbackStrategy strategy) {
        if (strategy == null) {
            strategy = TemporalMapTensorManager.DataFallbackStrategy.PREVIOUS_EARLIER_EPOCH;
        }
        String note = " " + I18n.getOrDefault("scenario.autodialog.tab3_scope_note",
                "💡 Remarque : Ce réglage s'applique spécifiquement aux calques de l'Onglet 3 (données démographiques initiales et tenseurs culturels).");
        return switch (strategy) {
            case CONTINUOUS_INTERPOLATION -> I18n.getOrDefault("scenario.fallback.interpolation.desc",
                    "🔄 Interpolation temporelle continue (Fondu barycentrique) : Effectue un fondu progressif entre les 2 époques empiriques encadrant la date.") + note;
            case PREVIOUS_EARLIER_EPOCH -> I18n.getOrDefault("scenario.fallback.earlier_epoch.desc",
                    "⏮️ Repli conservateur sur l'époque antérieure : Verrouille strictement les données sur l'époque documentée précédente. Prévient tout anachronisme technologique, institutionnel ou géopolitique.") + note;
            case CLOSEST_ANCHOR_EPOCH -> I18n.getOrDefault("scenario.fallback.closest_anchor.desc",
                    "🎯 Jalon étalonné le plus proche : Aligne immédiatement l'ensemble des données sur le jalon historique ayant la distance chronologique minimale (|t - t_jalon| min).") + note;
        };
    }

    /**
     * Resolves localized planet display name.
     *
     * @param planet key name
     * @return localized planet name with icon
     */
    public static String getLocalizedPlanetName(String planet) {
        if (planet == null) return I18n.getOrDefault("planet.map.earth", "🌍 Terre");
        return switch (planet.toLowerCase()) {
            case "earth", "terre" -> I18n.getOrDefault("planet.map.earth", "🌍 Terre");
            case "mars" -> I18n.getOrDefault("planet.map.mars", "🔴 Mars");
            case "moon", "lune" -> I18n.getOrDefault("planet.map.moon", "🌕 Lune");
            case "venus", "vénus" -> I18n.getOrDefault("planet.map.venus", "♀️ Vénus");
            case "mercury", "mercure" -> I18n.getOrDefault("planet.map.mercury", "☿️ Mercure");
            default -> "🪐 " + planet;
        };
    }

    /**
     * Resolves milestone description for Earth at a given year.
     *
     * @param year the target year
     * @param defaultDesc the fallback description
     * @return localized milestone description
     */
    public static String resolveEarthMilestoneDesc(long year, String defaultDesc) {
        for (AutoEpochScenarioGenerator.EpochMilestone m : AutoEpochScenarioGenerator.EARTH_KEY_MILESTONES) {
            if (Math.abs(m.year() - year) < 500) {
                return I18n.getOrDefault(m.descriptionKey(), m.defaultDesc());
            }
        }
        return defaultDesc;
    }
}
