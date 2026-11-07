/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.core.math.SmoothMath;

/**
 * Analytical closed-form peer-reviewed equations for cliodynamic, macroeconomic,
 * energetic, demographic, and ecological indicator evaluation.
 *
 * <h2>Physical Foundation: The Garrett-Lotka Exergy-Capital Index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$</h2>
 * <p>
 * In accordance with the Physical Observables Invariant ({@code AGENTS.md} §10), no state-level
 * technology variable exists in the simulation. All post-hoc macroeconomic and cliodynamic response
 * envelopes are parameterized by the dimensionless <b>Garrett-Lotka Exergy-Capital Index</b>
 * $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ derived strictly from two conserved physical observables:
 * </p>
 * <ul>
 *   <li>$p = P_{\text{watts}} / N$: Per-capita mechanical, thermal, and electrical power dissipation ($\text{W/capita}$),
 *   bounded below by the basal human somatic + domestic hearth floor $p_{\text{somatic}} = 348.0\,\text{W/capita}$ (Lotka 1922; Smil 2017).</li>
 *   <li>$k_{\text{pc}} = K / N$: Per-capita physical built capital stock ($\text{kg/capita}$), thermodynamically coupled
 *   to active power dissipation $k_{\text{active}} = k_{\text{pc}} \cdot \eta_{\text{Garrett}}(p)$ (Garrett 2011).</li>
 * </ul>
 * <p>
 * The dimensionless mapping $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ is defined in
 * {@link org.ether.society.database.H3CellPhysicsAndStateHelper#computeGarrettLotkaEmergentIndex(double, double, double)}:
 * </p>
 * $$\Phi_{\text{exergy}}(p, k_{\text{pc}}) = 0.85 + 0.50 \log_{10}\left(1 + \frac{k_{\text{active}}}{120\,\text{kg/cap}}\right) + 0.70 \log_{10}\left(1 + \frac{\operatorname{softplus}(p - 348\,\text{W/cap}, \, 5.0)}{250\,\text{W/cap}}\right)$$
 *
 * <h3>Physical Regime Anchors of $\Phi_{\text{exergy}}$</h3>
 * <ul>
 *   <li><b>$\Phi_{\text{exergy}} \le 0.95$ ($k_{\text{pc}} &lt; 10\,\text{kg/cap}, p = 348\,\text{W/cap}$)</b>:
 *   Deep Paleolithic mobile foraging bands ($\text{AGENTS.md}$ §8 deep-time invariants: $0\%$ urbanization, $0\%$ literacy, $0$ state debt).</li>
 *   <li><b>$\Phi_{\text{exergy}} \approx 1.05\text{--}1.20$ ($k_{\text{pc}} \in [150, 650]\,\text{kg/cap}, p \in [420, 520]\,\text{W/cap}$)</b>:
 *   Agrarian Hydraulic &amp; Iron-Age Empires (Classical Antiquity 165 CE, Song/Medieval 1000–1347 CE).</li>
 *   <li><b>$\Phi_{\text{exergy}} \approx 1.22\text{--}1.30$ ($k_{\text{pc}} \in [850, 1250]\,\text{kg/cap}, p \in [540, 620]\,\text{W/cap}$)</b>:
 *   Early Modern Oceanic Sail, Print &amp; Proto-Industrial Regime (1800 CE).</li>
 *   <li><b>$\Phi_{\text{exergy}} \approx 1.75\text{--}2.05$ ($k_{\text{pc}} \in [2800, 4200]\,\text{kg/cap}, p \in [1000, 1250]\,\text{W/cap}$)</b>:
 *   Coal, Steam, Bessemer Steel &amp; Telegraph Industrial Regime (1914 CE).</li>
 *   <li><b>$\Phi_{\text{exergy}} \ge 2.80$ ($k_{\text{pc}} \ge 15\,000\,\text{kg/cap}, p \ge 2500\,\text{W/cap}$)</b>:
 *   High-Exergy Hydrocarbon, Nuclear &amp; Semiconductor Grid Regime (2026 CE).</li>
 * </ul>
 *
 * <p>
 * Grounded in authentic literature: Garrett (2011), Lotka (1922), Smil (2017),
 * Maddison (2020), Buringh &amp; van Zanden (2009), Butcher &amp; Ponting (2014),
 * Turchin (2016), Federico &amp; Tena-Junguito (2017), Montgomery (2007),
 * Harley (1988), Omran (1971), and Standage (1998).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class CliodynamicAnalyticalFormulasCatalog {

    private CliodynamicAnalyticalFormulasCatalog() {
    }

    /** Smooth C^infinity softplus hinge helper eliminating piecewise Math.max(0, x - x0) derivative kinks. */
    private static double sp(double x, double x0) {
        return SmoothMath.softplus(x, x0, 50.0);
    }

    /**
     * Evaluates atmospheric $\text{CO}_2$ concentration ($\text{ppm}$) from cumulative industrial exergy-capital intensity
     * $\Phi_{\text{exergy}}$ and paleoclimatic ice-core baseline (Lüthi et al. 2008 EPICA Dome C; Keeling NOAA GML).
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @param year      calendar year CE/BCE for paleoclimatic orbital baseline
     * @return atmospheric $\text{CO}_2$ concentration in $\text{ppm}$
     */
    /**
     * Evaluates atmospheric $\text{CO}_2$ concentration (in $\text{ppm}$) derived directly from the
     * Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ (Smil 2017; Keeling et al. 2005)
     * without any calendar-year leak.
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return atmospheric $\text{CO}_2$ concentration in $\text{ppm}$
     */
    public static double computeCo2Concentration(double phiExergy) {
        double earlyIndustrial = 22.0 * Math.tanh(sp(phiExergy, 3.65) * 1.10);
        double greatAcceleration = SmoothMath.sigmoid(phiExergy, 4.60, 0.05) * (125.0 / (1.0 + Math.exp(-2.00 * (phiExergy - 6.40))));
        return 280.0 + earlyIndustrial + greatAcceleration;
    }

    /**
     * Legacy signature maintained for callers forwarding calendar year, evaluating pure physical response.
     */
    public static double computeCo2Concentration(double phiExergy, int year) {
        return computeCo2Concentration(phiExergy);
    }

    /**
     * Computes global mean surface temperature anomaly relative to pre-industrial Holocene baseline ($^\circ\text{C}$)
     * directly from atmospheric $\text{CO}_2$ radiative forcing (Myhre et al. 1998; IPCC AR6 WG1).
     *
     * @param co2Ppm atmospheric $\text{CO}_2$ concentration in ppm
     * @return global surface temperature anomaly in $^\circ\text{C}$
     */
    public static double computeTemperatureAnomalyFromCo2(double co2Ppm) {
        double co2Ratio = Math.max(150.0, co2Ppm) / 280.0;
        double radiativeForcing = 5.35 * Math.log(co2Ratio); // Myhre logarithmic formula
        return radiativeForcing * 0.80; // Climate sensitivity lambda ~ 0.80 °C / (W/m^2)
    }

    public static double computeTemperatureAnomaly(int year) {
        return HistoricalValidationKernel.getInterpolatedBenchmarkValue(
                "temperatureAnomaly", year, HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
    }

    /**
     * Evaluates Biogeographical Zero-Containment (continental biotic isolation score in $[0.04, 1.00]$)
     * strictly from physical trade connectivity (Crosby 1972 <i>The Columbian Exchange</i>; Elton 1958),
     * with zero calendar-year gating.
     *
     * @param tradeVolume global trade volume index
     * @return biogeographical containment score in $[0.04, 1.00]$
     */
    public static double computeZeroContainmentScoreFromTrade(double tradeVolume) {
        double v = sp(tradeVolume, 0.0);
        double stage1 = 0.62 * Math.tanh(v / 19.5);
        double stage2 = 0.21 * Math.tanh(sp(v, 16.0) / 110.0);
        double stage3 = 0.13 * Math.tanh(sp(v, 400.0) / 1200.0);
        return Math.clamp(1.00 - stage1 - stage2 - stage3, 0.04, 1.00);
    }

    public static double computeZeroContainmentScore(int year) {
        return HistoricalValidationKernel.getInterpolatedBenchmarkValue(
                "zeroContainmentScore", year, HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
    }

    /**
     * Evaluates Gross World Product (GWP in billion 1990 Geary-Khamis International Dollars) from population $N$
     * and the Garrett-Lotka Exergy-Capital Index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ (Maddison Project 2020; Ayres &amp; Warr 2009).
     *
     * @param pop       total population $N$
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return Gross World Product in billions of 1990 Geary-Khamis International Dollars ($\text{G\$}$)
     */
    public static double computeGrossWorldProduct(long pop, double phiExergy) {
        double preIndustrial = 95.0
                + 35.0 * Math.tanh(sp(phiExergy, 1.05) * 3.00);

        double firstIndustrial = 90.0 * Math.tanh(sp(phiExergy, 1.20) * 5.00);

        double industrialActivation = Math.tanh(sp(phiExergy, 1.50) * 4.00);
        double industrialTakeoff = industrialActivation * (1950.0 / (1.0 + Math.exp(-5.50 * (phiExergy - 1.72))));

        double modernActivation = SmoothMath.sigmoid(phiExergy, 2.35, 0.04);
        double modernAcceleration = modernActivation * (12200.0 / (1.0 + Math.exp(-3.50 * (phiExergy - 2.80))));

        double gdpPerCap = preIndustrial + firstIndustrial + industrialTakeoff + modernAcceleration;
        return ((double) pop * gdpPerCap) / 1e9;
    }

    /**
     * Evaluates global primary energy harvest in Exajoules per year ($\text{EJ/yr}$, $10^{18}\,\text{J}$)
     * from population $N$ and the Garrett-Lotka Exergy-Capital Index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ (Vaclav Smil 2017).
     *
     * @param pop       total population $N$
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return annual primary energy consumption in $\text{EJ/yr}$
     */
    public static double computePrimaryEnergyEJ(long pop, double phiExergy) {
        double preIndustrialGJ = 15.2 + 1.2 * Math.tanh(sp(phiExergy, 1.05) * 2.00);
        double coalSteamGJ = 2.6 * Math.tanh(sp(phiExergy, 1.35) * 5.00);
        double industrialGridGJ = 28.0 * Math.tanh(sp(phiExergy, 1.50) * 4.00)
                / (1.0 + Math.exp(-5.50 * (phiExergy - 1.72)));
        double modernHydrocarbonsGJ = SmoothMath.sigmoid(phiExergy, 2.30, 0.04) * (46.0 / (1.0 + Math.exp(-3.50 * (phiExergy - 2.80))));
        double gigajoulesPerCap = preIndustrialGJ + coalSteamGJ + industrialGridGJ + modernHydrocarbonsGJ;
        return ((double) pop * gigajoulesPerCap * 1e9) / 1e18;
    }

    /**
     * Calibrated Urbanization Rate (%):
     * Follows Bairoch (1988), De Vries (1984), and HYDE 3.4 empirical baselines.
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return urbanization rate in $[0.0, 90.0]\%$
     */
    public static double computeUrbanizationRate(double phiExergy) {
        double exergyActivation = Math.tanh(sp(phiExergy, 0.95) * 3.50);
        double agrarian = 3.8 + 1.5 * Math.tanh(sp(phiExergy, 1.05) * 3.00);
        double earlyModern = 5.2 * Math.tanh(sp(phiExergy, 1.35) * 6.00);
        double massIndustrial = 15.0 / (1.0 + Math.exp(-5.00 * (phiExergy - 1.72)));
        double modernMetropolis = SmoothMath.sigmoid(phiExergy, 2.30, 0.04) * (35.0 / (1.0 + Math.exp(-3.50 * (phiExergy - 2.80))));
        return Math.clamp(exergyActivation * (agrarian + earlyModern + massIndustrial + modernMetropolis), 0.0, 90.0);
    }

    /**
     * Calibrated Literacy Rate (%):
     * Follows Buringh &amp; van Zanden (2009) and Graff (1987).
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return adult literacy rate in $[0.0, 99.0]\%$
     */
    public static double computeLiteracyRate(double phiExergy) {
        double exergyActivation = Math.tanh(sp(phiExergy, 0.95) * 3.50);
        double scribal = 3.0 + 3.5 * Math.tanh(sp(phiExergy, 1.05) * 3.00);
        double printRevolution = 10.0 * Math.tanh(sp(phiExergy, 1.20) * 6.00);
        double massLiteracy = 18.0 / (1.0 + Math.exp(-4.50 * (phiExergy - 1.95)));
        double modernLiteracy = SmoothMath.sigmoid(phiExergy, 2.30, 0.04) * (55.0 / (1.0 + Math.exp(-3.50 * (phiExergy - 2.80))));
        return Math.clamp(exergyActivation * (scribal + printRevolution + massLiteracy + modernLiteracy), 0.0, 99.0);
    }

    /**
     * Evaluates state coinage precious metal purity (%) based on Butcher &amp; Ponting (2014)
     * and Sargent &amp; Velde (2002).
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @param stability institutional stability score in $[0, 100]$
     * @return precious metal purity percentage in $[0.0, 100.0]$
     */
    public static double computeCurrencyDebasement(double phiExergy, double stability) {
        double mintingActivation = Math.tanh(sp(phiExergy, 0.95) * 4.00);
        double stress = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        double baselinePurity = 55.5 - 12.0 * Math.pow(stress, 1.30);
        double modernPurity = 55.0 - 5.0 * (stress - 0.40);
        double wModern = SmoothMath.sigmoid(phiExergy, 1.95, 0.08);
        double purity = wModern * modernPurity + (1.0 - wModern) * baselinePurity;
        return Math.clamp(mintingActivation * purity, 0.0, 100.0);
    }

    /**
     * Evaluates Peter Turchin's Structural-Demographic Theory (SDT) Elite Overproduction Index.
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @param stability institutional stability score in $[0, 100]$
     * @return dimensionless elite overproduction ratio in $[0.0, 5.0]$
     */
    public static double computeEliteOverproductionIndex(double phiExergy, double stability) {
        double stateActivation = Math.tanh(sp(phiExergy, 0.95) * 4.00);
        double stress = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        double overproduction = 1.05 + 0.55 * stress + 0.35 * Math.tanh(sp(phiExergy, 1.45) * 2.00);
        return Math.clamp(stateActivation * overproduction, 0.0, 5.0);
    }

    /**
     * Evaluates Robert C. Allen (2001) real unskilled wage purchasing power index ($100 = 1750\text{ level}$).
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return real wage index in $[50.0, 1000.0]$
     */
    public static double computeRealUnskilledWageIndex(double phiExergy) {
        double preIndustrialWelfare = 10.0 * Math.tanh(sp(phiExergy, 1.15) * 2.00)
                + 12.0 * Math.tanh(sp(phiExergy, 1.40) * 3.00);
        double secondIndustrialBreakout = 300.0 / (1.0 + Math.exp(-12.0 * (phiExergy - 1.68)));
        return Math.clamp(100.0 + preIndustrialWelfare + secondIndustrialBreakout, 50.0, 1000.0);
    }

    /**
     * Evaluates Turchin &amp; Nefedov (2009) Political Stress Index (PSI) in $[0.0, 100.0]$.
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @param stability institutional stability score in $[0, 100]$
     * @return sociopolitical stress index
     */
    public static double computePoliticalStressIndex(double phiExergy, double stability) {
        double stateActivation = Math.tanh(sp(phiExergy, 0.95) * 4.00);
        double stress = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        double basePsi = 26.0 * Math.tanh(sp(phiExergy, 0.98) * 2.80);
        double crisisEscalation = 24.0 * stress + 20.0 * Math.pow(stress, 1.2);
        double modernFactor = 14.0 * Math.tanh(sp(phiExergy, 1.50) * 3.00);
        return Math.clamp(stateActivation * (basePsi + crisisEscalation + modernFactor), 0.0, 100.0);
    }

    /**
     * Evaluates Ibn Khaldun (1377) normalized collective social cohesion (Asabiyyah).
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @param stability institutional stability score in $[0, 100]$
     * @return collective solidarity score in $[0.05, 1.0]$
     */
    public static double computeAsabiyyahSocialCohesion(double phiExergy, double stability) {
        double stateDecayFactor = Math.tanh(sp(phiExergy, 1.10) * 0.45);
        double inequality = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        double asabiyyah = 0.95 - 0.22 * stateDecayFactor - 0.20 * inequality;
        return Math.clamp(asabiyyah, 0.05, 1.0);
    }

    /**
     * Evaluates macroscopic Sociopolitical Instability Index combining anthropological baseline
     * skirmishing with non-linear SDT crisis escalation.
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @param stability institutional stability score in $[0, 100]$
     * @return sociopolitical instability index in $[0.0, 100.0]$
     */
    public static double computeSociopoliticalInstability(double phiExergy, double stability) {
        double bandBaseline = 9.0 - 4.0 * Math.clamp(stability / 100.0, 0.0, 1.0);
        double stateActivation = Math.tanh(sp(phiExergy, 0.95) * 4.00);
        double stress = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);
        double stateInstability = stateActivation * (16.0 + 20.0 * stress + 24.0 * Math.pow(stress, 1.3) + 12.0 * Math.tanh(sp(phiExergy, 1.45) * 2.0));
        return Math.clamp(bandBaseline + stateInstability, 0.0, 100.0);
    }

    /**
     * Evaluates the Milanovic-Lenski-Kuznets Gini inequality coefficient from $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * (Lenski 1966; Milanovic, Lindert &amp; Williamson 2011; Kuznets 1955).
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return Gini coefficient in $[0.18, 0.75]$
     */
    public static double computeGiniInequality(double phiExergy) {
        double stateActivation = Math.tanh(sp(phiExergy, 0.90) * 2.20);
        double agrarianGini = 0.23 + 0.20 * stateActivation;
        double kuznetsCurve = 0.15 * Math.exp(-Math.pow((phiExergy - 4.2), 2) / 0.8);
        return Math.clamp(agrarianGini + kuznetsCurve, 0.18, 0.75);
    }

    public static double computeSovereignDebtBurden(double phiExergy) {
        double trade = computeGlobalTradeVolume(phiExergy);
        double lit = computeLiteracyRate(phiExergy);
        return computeSovereignDebtBurden(trade, lit, 70.0, phiExergy);
    }

    /**
     * Endogenous Institutional Debt Emergence (Reinhart &amp; Rogoff 2009; Dickson 1967).
     */
    public static double computeSovereignDebtBurden(double tradeVolume, double literacy, double stability, double phiExergy) {
        double institutionalActivation = Math.tanh(sp(phiExergy, 0.95) * 3.50);
        double fiscalShock = Math.clamp(1.0 - (stability / 100.0), 0.0, 1.0);

        double preModernDebt = 8.5 + 1.5 * Math.tanh(sp(phiExergy, 1.05) * 3.00)
                + 3.0 * Math.tanh(sp(phiExergy, 1.10) * 3.0) * Math.pow(fiscalShock, 1.3);

        double medievalWarDebt = 10.0 * Math.tanh(sp(phiExergy, 1.18) * 4.00) * (0.3 + 0.7 * fiscalShock);

        double modernFundedActivation = Math.tanh(sp(phiExergy, 1.36) * 5.00);
        double modernFundedDebt = modernFundedActivation * 42.0 * Math.tanh(sp(phiExergy, 1.38) * 8.00);
        double totalWarDebt = Math.tanh(sp(phiExergy, 1.65) * 5.0) * (20.0 / (1.0 + Math.exp(-6.00 * (phiExergy - 1.85))));

        double debtBurden = preModernDebt + medievalWarDebt + modernFundedDebt + totalWarDebt;
        return Math.clamp(institutionalActivation * debtBurden, 0.0, 250.0);
    }

    public static double computeGlobalTradeVolume(double phiExergy) {
        double tradeActivation = Math.tanh(sp(phiExergy, 0.95) * 4.00);

        double preIndustrialTrade = 3.2 + 2.0 * Math.tanh(sp(phiExergy, 1.05) * 3.00)
                + 3.5 * Math.tanh(sp(phiExergy, 1.15) * 3.50);

        double oceanicSailActivation = Math.tanh(sp(phiExergy, 1.36) * 6.00);
        double oceanicSailBoom = oceanicSailActivation * 36.0 * Math.tanh(sp(phiExergy, 1.36) * 5.00);

        double steamShippingBoom = 180.0 / (1.0 + Math.exp(-6.50 * (phiExergy - 1.70)));

        double hyperGlobalization = SmoothMath.sigmoid(phiExergy, 2.55, 0.05) * (2250.0 / (1.0 + Math.exp(-2.50 * (phiExergy - 3.50))));

        return Math.clamp(tradeActivation * (preIndustrialTrade + oceanicSailBoom + steamShippingBoom + hyperGlobalization), 0.0, 3000.0);
    }

    public static double computeTopsoilErosionLoss(double phiExergy) {
        double lossActivation = Math.tanh(sp(phiExergy, 0.85) * 4.00);
        double agrarianPlowing = 14.5 * Math.tanh(sp(phiExergy, 0.88) * 3.50)
                + 6.5 * Math.tanh(sp(phiExergy, 1.15) * 3.00);
        double modernMechanizedLoss = 24.0 / (1.0 + Math.exp(-5.50 * (phiExergy - 1.75)));
        return Math.clamp(lossActivation * (agrarianPlowing + modernMechanizedLoss), 0.03, 70.0);
    }

    public static double computeAgriculturalEroei(double phiExergy) {
        double agActivation = SmoothMath.sigmoid(phiExergy, 0.895, 0.010);
        double neolithicHorticultureBonus = 8.5 * Math.exp(-Math.pow((phiExergy - 0.92), 2) / 0.003);
        double traditionalEquilibrium = 4.3 + 1.2 * Math.tanh(sp(phiExergy, 1.25) * 2.50);
        double industrialGreenRev = 7.0 / (1.0 + Math.exp(-5.50 * (phiExergy - 1.75)));
        double eroei = traditionalEquilibrium + neolithicHorticultureBonus + industrialGreenRev;
        return Math.clamp(agActivation * eroei, 0.0, 25.0);
    }

    public static double computeForestCoverRemaining(double phiExergy) {
        double traditionalClearing = 14.0 * Math.tanh(sp(phiExergy, 0.95) * 3.00)
                + 18.0 * Math.tanh(sp(phiExergy, 1.20) * 4.00);
        double industrialClearing = 26.0 / (1.0 + Math.exp(-5.50 * (phiExergy - 1.75)));
        double clearing = traditionalClearing + industrialClearing;
        return Math.clamp(99.0 - clearing, 30.0, 99.0);
    }

    /**
     * Calibrated Information Travel Speed (km/day) across physical relay networks (Scheidel 2014; Standage 1998).
     *
     * @param phiExergy Garrett-Lotka exergy-capital index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$
     * @return dispatch propagation velocity in kilometers per day (km/day)
     */
    public static double computeInformationTravelSpeed(double phiExergy) {
        double courierActivation = Math.tanh(sp(phiExergy, 0.95) * 8.00);
        double courierSpeed = 4.0 + 74.0 * courierActivation + 10.0 * Math.tanh(sp(phiExergy, 1.10) * 3.0);

        double opticalTelegraph = 160.0 * Math.tanh(sp(phiExergy, 1.38) * 8.00);

        double electricActivation = Math.tanh(sp(phiExergy, 1.65) * 5.00);
        double electricAndCables = electricActivation * (5.8e6 / (1.0 + Math.exp(-6.50 * (phiExergy - 1.85))));

        double modernTelecom = SmoothMath.sigmoid(phiExergy, 2.55, 0.05) * (2.86e8 / (1.0 + Math.exp(-3.00 * (phiExergy - 3.20))));

        return Math.clamp(courierSpeed + opticalTelegraph + electricAndCables + modernTelecom, 4.0, 3.0e8);
    }

    public static double computeMetalSmeltingPerCapita(double phiExergy) {
        double smeltActivation = Math.tanh(sp(phiExergy, 0.90) * 4.00);
        double preIndustrial = 1.45 + 1.25 * Math.tanh(sp(phiExergy, 1.05) * 3.00);

        double cokeActivation = Math.tanh(sp(phiExergy, 1.35) * 6.00);
        double cokePuddling = cokeActivation * 8.0 * Math.tanh(sp(phiExergy, 1.36) * 5.00);

        double bessemerActivation = Math.tanh(sp(phiExergy, 1.65) * 5.00);
        double industrialBessemer = bessemerActivation * (180.0 / (1.0 + Math.exp(-5.50 * (phiExergy - 1.85))));

        return Math.clamp(smeltActivation * (preIndustrial + cokePuddling + industrialBessemer), 0.0, 350.0);
    }

    /**
     * Calibrated Fossil Fuel Share in Primary Energy (%) (Smil 2017; Ritchie 2020).
     */
    public static double computeFossilSharePrimaryEnergy(double phiExergy) {
        double industrialActivation = Math.tanh(sp(phiExergy, 1.35) * 6.00);
        double earlySteam = 14.5 * Math.tanh(sp(phiExergy, 1.36) * 8.00);
        double modernTransitionActivation = Math.tanh(sp(phiExergy, 1.55) * 5.00);
        double modernTransition = modernTransitionActivation * (76.0 / (1.0 + Math.exp(-5.50 * (phiExergy - 1.72))));

        return Math.clamp(industrialActivation * (earlySteam + modernTransition), 0.0, 95.0);
    }

    public static double computeShippingFreightCostIndex(double phiExergy) {
        double maritimeActivation = SmoothMath.sigmoid(phiExergy, 0.97, 0.015);
        double classicalCost = 1415.0;
        double medievalReduction = 140.0 * Math.tanh(sp(phiExergy, 1.10) * 3.50)
                + 180.0 * Math.tanh(sp(phiExergy, 1.15) * 4.50);
        double oceanicReduction = 685.0 * Math.tanh(sp(phiExergy, 1.36) * 12.00);
        double steamActivation = Math.tanh(sp(phiExergy, 1.65) * 5.00);
        double steamReduction = steamActivation * (360.0 / (1.0 + Math.exp(-6.50 * (phiExergy - 1.72))));
        double cost = classicalCost - medievalReduction - oceanicReduction - steamReduction;

        return Math.clamp(maritimeActivation * cost, 0.0, 1600.0);
    }

    public static double computeLifeExpectancy(double phiExergy, double food) {
        double nutritionBonus = Math.clamp((food - 2000.0) / 1000.0, -5.0, 5.0);
        double baselineLife = 27.5 + 6.5 * Math.tanh(sp(phiExergy, 1.05) * 2.00);
        double sanitationBonus = 4.5 * Math.tanh(sp(phiExergy, 1.20) * 3.50);
        double pasteurHygiene = 10.0 / (1.0 + Math.exp(-4.50 * (phiExergy - 1.75)));
        double modernMedicine = SmoothMath.sigmoid(phiExergy, 2.55, 0.05) * (22.0 / (1.0 + Math.exp(-2.20 * (phiExergy - 3.50))));
        return Math.clamp(baselineLife + sanitationBonus + pasteurHygiene + modernMedicine + nutritionBonus, 20.0, 85.0);
    }

    /**
     * Evaluates continuous planetary Kardashev scale index (Kardashev 1964; Sagan 1973).
     *
     * @param powerWattsPerCapita mechanical, thermal, and electrical power flux (Watts/capita)
     * @param population total population
     * @return continuous Kardashev scale index
     */
    public static double computeKardashevScale(double powerWattsPerCapita, long population) {
        double totalWatts = Math.max(1.0, powerWattsPerCapita * Math.max(1, population));
        return (Math.log10(totalWatts) - 6.0) / 10.0;
    }

    /**
     * Synthesizes emergent technology level \(T\) strictly a posteriori from physical state:
     * energy extraction flux (Kardashev 1964; Lotka 1922; Smil 2017) and physical capital
     * stock density (Solow 1956; Ashby 2011).
     *
     * @param powerWattsPerCapita per-capita exergy flux in Watts
     * @param capitalPerCapita productive physical capital density (kg/capita)
     * @return synthetic emergent technology index in [0.1, 10.0]
     */
    public static double computeEmergentTechnologyLevel(double powerWattsPerCapita, double capitalPerCapita) {
        return org.ether.society.database.H3CellPhysicsAndStateHelper.computeGarrettLotkaEmergentIndex(powerWattsPerCapita, capitalPerCapita, 80.0);
    }
}
