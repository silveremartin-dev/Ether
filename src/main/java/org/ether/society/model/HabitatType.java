/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.model;

/**
 * Typologies of extraterrestrial and pressurized habitats in Ether.
 * Dictates physical shielding against cosmic radiation, micro-meteorites,
 * thermal extremes, baseline wear rate, and operational ECLSS energy consumption.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public enum HabitatType {
    /** No artificial pressurized habitat (open-air biosphere). */
    NONE("habitat.type.none", 0.0, 0.0, 0.0, 0.0),

    /** Surface Geodesic Glass/Polymer Pressurized Dome (Mars, Moon, Titan). */
    SURFACE_DOME("habitat.type.surface_dome", 0.035, 0.70, 1.20, 500.0),

    /** 3D Sintered Regolith Vault & Cut-and-Cover Bunker (Moon, Mars, Mercury). */
    SINTERED_3D_VAULT("habitat.type.sintered_vault", 0.015, 0.90, 1.00, 350.0),

    /** Subsurface Basaltic Lava Tube / Sealed Natural Cavity (Moon, Mars). */
    LAVA_TUBE("habitat.type.lava_tube", 0.005, 0.98, 0.80, 200.0),

    /** High-Altitude Aerostat Floating Cloud Habitat (Venus 50-55km, Titan). */
    VENUS_AEROSTAT("habitat.type.venus_aerostat", 0.025, 0.60, 1.50, 600.0);

    private final String i18nKey;
    private final double baselineWearRatePerYear;
    private final double radiationShieldingFactor; // 0.0 = no shielding, 1.0 = total cosmic ray blocking
    private final double baseEnergyKwPerCapita;    // ECLSS power requirement in kW per human
    private final double constructionMetalKgPerCapita;

    HabitatType(String i18nKey,
                double baselineWearRatePerYear,
                double radiationShieldingFactor,
                double baseEnergyKwPerCapita,
                double constructionMetalKgPerCapita) {
        this.i18nKey = i18nKey;
        this.baselineWearRatePerYear = baselineWearRatePerYear;
        this.radiationShieldingFactor = radiationShieldingFactor;
        this.baseEnergyKwPerCapita = baseEnergyKwPerCapita;
        this.constructionMetalKgPerCapita = constructionMetalKgPerCapita;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    public double getBaselineWearRatePerYear() {
        return baselineWearRatePerYear;
    }

    public double getRadiationShieldingFactor() {
        return radiationShieldingFactor;
    }

    public double getBaseEnergyKwPerCapita() {
        return baseEnergyKwPerCapita;
    }

    public double getConstructionMetalKgPerCapita() {
        return constructionMetalKgPerCapita;
    }

    public boolean isPressurized() {
        return this != NONE;
    }
}
