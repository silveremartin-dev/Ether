/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.model;

/**
 * Universal Allometric Biophysical Profile for Collective Dissipative Organisms.
 *
 * <p>Parameterizes metabolic energetics, generation turnover, and reproductive stoichiometry
 * across arbitrary living collectives—from eusocial insects (<i>Atta</i> leafcutter ants,
 * <i>Macrotermes</i> mound-building termites) to <i>Homo sapiens</i> and theoretical
 * extraterrestrial biosystems—without hardcoded anthropocentric constants.</p>
 *
 * <h2>Academic Grounding</h2>
 * <ul>
 *   <li><b>Kleiber (1932) &amp; West, Brown &amp; Enquist (1997, <i>Science</i> 276:122)</b>:
 *       Basal endosomatic metabolic power scales across 27 orders of magnitude of body mass \(M_b\) as
 *       $$B_{\text{basal}}(M_b) = B_0 \cdot M_b^{3/4} \quad [\text{W/individual}]$$
 *       with universal mammalian/endothermic normalization \(B_0 \approx 4.24\,\text{W}\cdot\text{kg}^{-3/4}\)
 *       (\(B_{\text{basal}}(65\,\text{kg}) = 97.0\,\text{W} \approx 2000\,\text{kcal/day} = 3.06\,\text{GJ/yr}\))
 *       and ectothermic/arthropod normalization \(B_{0,\text{ecto}} \approx 0.71\,\text{W}\cdot\text{kg}^{-3/4}\).</li>
 *   <li><b>Brown, Gillooly, Allen, Savage &amp; West (2004, <i>Ecology</i> 85:1771)</b>:
 *       Biological timescales (ontogenetic maturation \(\tau_{\text{repro}}\), intrinsic lifespan \(\tau_{\text{life}}\))
 *       scale quarter-power allometrically as \(\tau(M_b) \propto M_b^{1/4}\).</li>
 *   <li><b>Waters, Holbrook, Fewell &amp; Harrison (2010, <i>PNAS</i> 107:20382)</b>:
 *       Whole-colony metabolic and organizational scaling in social insect superorganisms.</li>
 *   <li><b>Lotka (1922, <i>PNAS</i> 8:147) &amp; Fischer-Kowalski &amp; Haberl (2007)</b>:
 *       Dimensionless Exosomatic-to-Endosomatic Metabolic Ratio \(\mathcal{M} = \frac{P_{\text{watts}}/N}{B_{\text{basal}}(M_b)}\).</li>
 * </ul>
 *
 * @param speciesIdentifier         canonical identifier of the collective organism
 * @param adultBodyMassKg           mean adult individual body mass \(M_b\) in kilograms (\(\text{kg}\))
 * @param kleiberNormalizationWatts metabolic normalization constant \(B_0\) in \(\text{W}\cdot\text{kg}^{-3/4}\)
 * @param referencePrimiparityYears baseline reproductive onset age \(\tau_{\text{repro}}\) in Earth years
 * @param referenceLifespanYears    baseline intrinsic senescent lifespan \(\tau_{\text{life}}\) in Earth years
 * @param broodMetabolicMultiple    reproductive/brood energetic cost as a multiple of annual basal metabolism
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public record OrganismBiophysicalProfile(
        String speciesIdentifier,
        double adultBodyMassKg,
        double kleiberNormalizationWatts,
        double referencePrimiparityYears,
        double referenceLifespanYears,
        double broodMetabolicMultiple
) {

    /** Seconds per Julian year (\(365.25 \times 86\,400\)). */
    public static final double SECONDS_PER_YEAR = 31_557_600.0;

    /** Standard endothermic Kleiber normalization \(B_0 = 4.24\,\text{W}\cdot\text{kg}^{-3/4}\) (Kleiber 1932). */
    public static final double ENDOTHERM_KLEIBER_B0_WATTS = 4.24;

    /** Standard arthropod/ectothermic Kleiber normalization \(B_0 = 0.71\,\text{W}\cdot\text{kg}^{-3/4}\) at \(293\,\text{K}\) ( Gillooly et al. 2001). */
    public static final double ECTOTHERM_KLEIBER_B0_WATTS = 0.71;

    /**
     * Canonical <i>Homo sapiens</i> biophysical profile (\(M_b = 65\,\text{kg}\), \(B_{\text{basal}} \approx 97.0\,\text{W}\)).
     */
    public static final OrganismBiophysicalProfile HOMO_SAPIENS = new OrganismBiophysicalProfile(
            "HOMO_SAPIENS",
            65.0,
            ENDOTHERM_KLEIBER_B0_WATTS,
            15.5,
            72.0,
            1.40
    );

    /**
     * Eusocial fungus-cultivating leafcutter ant profile (<i>Atta cephalotes</i>, Waters et al. 2010, \(M_b = 5\,\text{mg}\)).
     */
    public static final OrganismBiophysicalProfile EUSOCIAL_LEAFCUTTER_ANT = new OrganismBiophysicalProfile(
            "ATTA_CEPHALOTES",
            5.0e-6,
            ECTOTHERM_KLEIBER_B0_WATTS,
            0.12,
            2.5,
            0.85
    );

    /**
     * Eusocial mound-building termite profile (<i>Macrotermes bellicosus</i>, \(M_b = 8\,\text{mg}\)) with passive ventilation capital.
     */
    public static final OrganismBiophysicalProfile EUSOCIAL_MOUND_TERMITE = new OrganismBiophysicalProfile(
            "MACROTERMES_BELLICOSUS",
            8.0e-6,
            ECTOTHERM_KLEIBER_B0_WATTS,
            0.15,
            3.2,
            0.90
    );

    /**
     * Constructs a custom allometrically scaled collective organism from body mass \(M_b\) and metabolic normalization \(B_0\).
     *
     * @param id                        species identifier
     * @param bodyMassKg                adult individual body mass \(M_b > 0\) in \(\text{kg}\)
     * @param kleiberNormalizationWatts metabolic prefactor \(B_0 > 0\) in \(\text{W}\cdot\text{kg}^{-3/4}\)
     * @return allometrically scaled biophysical profile
     */
    public static OrganismBiophysicalProfile fromAllometricScaling(
            String id, double bodyMassKg, double kleiberNormalizationWatts) {
        double safeMass = Math.max(1.0e-9, bodyMassKg);
        double quarterPowerRatio = Math.pow(safeMass / 65.0, 0.25);
        return new OrganismBiophysicalProfile(
                id,
                safeMass,
                Math.max(0.05, kleiberNormalizationWatts),
                15.5 * quarterPowerRatio,
                72.0 * quarterPowerRatio,
                1.25
        );
    }

    /**
     * Computes Kleiber's (1932) basal endosomatic metabolic power per individual in Watts (\(\text{J/s}\)).
     *
     * <p>$$B_{\text{basal}}(M_b) = B_0 \cdot M_b^{3/4}$$</p>
     *
     * @return basal metabolic rate in Watts per individual
     */
    public double basalMetabolicPowerWatts() {
        return kleiberNormalizationWatts * Math.pow(Math.max(1.0e-12, adultBodyMassKg), 0.75);
    }

    /**
     * Computes annual basal endosomatic metabolic energy intake per individual in Gigajoules (\(\text{GJ/yr}\)).
     *
     * @return annual basal metabolic energy in \(\text{GJ/individual/yr}\)
     */
    public double annualBasalMetabolicEnergyGJ() {
        return (basalMetabolicPowerWatts() * SECONDS_PER_YEAR) * 1.0e-9;
    }

    /**
     * Alias for {@link #annualBasalMetabolicEnergyGJ()}.
     *
     * @return annual basal metabolic energy in \(\text{GJ/individual/yr}\)
     */
    public double annualMetabolicEnergyGJ() {
        return annualBasalMetabolicEnergyGJ();
    }

    /**
     * Baseline reproductive onset age in Earth years.
     *
     * @return reproductive age threshold in years
     */
    public double minPrimiparityAgeYears() {
        return referencePrimiparityYears;
    }

    /**
     * Computes energetic cost of producing one surviving offspring/brood unit in Gigajoules (\(\text{GJ}\)).
     *
     * @return gestation/brood metabolic investment in \(\text{GJ}\)
     */
    public double gestationBroodEnergyGJ() {
        return annualBasalMetabolicEnergyGJ() * broodMetabolicMultiple;
    }

    /**
     * Alias for {@link #gestationBroodEnergyGJ()}.
     *
     * @return gestation/brood metabolic investment in \(\text{GJ}\)
     */
    public double gestationEnergyGJ() {
        return gestationBroodEnergyGJ();
    }

    /**
     * Computes the dimensionless Lotka-Fischer-Kowalski Exosomatic-to-Endosomatic Metabolic Ratio \(\mathcal{M}\).
     *
     * <p>$$\mathcal{M} = \frac{\max(B_{\text{basal}}, P_{\text{watts}}/N)}{B_{\text{basal}}(M_b)} \ge 1.0$$</p>
     *
     * @param totalPowerWattsPerIndividual total somatic + exosomatic power captured per individual (\(\text{W}\))
     * @return dimensionless metabolic ratio \(\mathcal{M} \ge 1.0\)
     */
    public double computeMetabolicRatio(double totalPowerWattsPerIndividual) {
        double basal = Math.max(1.0e-12, basalMetabolicPowerWatts());
        return Math.max(1.0, totalPowerWattsPerIndividual / basal);
    }
}
