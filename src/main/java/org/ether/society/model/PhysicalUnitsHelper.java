/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.model;

/**
 * Pure static SI (International System of Units) dimensional conversion helper.
 *
 * <p>All unit conversions across Ether simulation engines, kernels, telemetry aggregators,
 * and data loaders MUST pass through this single authoritative class. Inline magic conversion
 * constants (such as {@code 31.688}, {@code 3.15576e7}, {@code 273.15}, {@code 1e-3}, or
 * {@code 365.25}) are strictly prohibited inside physics formulas so that dimensional consistency
 * can be audited and verified in one central location.</p>
 *
 * <h2>Canonical SI Base &amp; Derived Conventions</h2>
 * <ul>
 *   <li><b>Time</b>: seconds ($\text{s}$), Julian days ($86\,400\,\text{s}$), Julian years ($365.25\,\text{d} = 31\,557\,600\,\text{s}$).</li>
 *   <li><b>Energy &amp; Power</b>: Joules ($\text{J}$), Gigajoules ($\text{GJ} = 10^9\,\text{J}$), Watts ($\text{W} = \text{J}\cdot\text{s}^{-1}$).</li>
 *   <li><b>Mass</b>: kilograms ($\text{kg}$), metric tonnes ($\text{t} = 1\,000\,\text{kg}$).</li>
 *   <li><b>Temperature</b>: Kelvin ($\text{K}$), degrees Celsius ($T_{\text{K}} = T_{^\circ\text{C}} + 273.15$).</li>
 *   <li><b>Length &amp; Area</b>: meters ($\text{m}$), kilometers ($\text{km} = 10^3\,\text{m}$), square kilometers ($\text{km}^2 = 10^6\,\text{m}^2$).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public final class PhysicalUnitsHelper {

    // =========================================================================
    // 1. EXACT SI CONVERSION FACTORS
    // =========================================================================

    /** Exact number of seconds in one SI standard day ($86\,400\,\text{s/d}$). */
    public static final double SECONDS_PER_DAY = 86_400.0;

    /** Exact number of days in one IAU Julian year ($365.25\,\text{d/yr}$). */
    public static final double DAYS_PER_JULIAN_YEAR = 365.25;

    /** Exact number of seconds in one IAU Julian year ($365.25 \times 86\,400 = 31\,557\,600\,\text{s/yr}$). */
    public static final double SECONDS_PER_JULIAN_YEAR = DAYS_PER_JULIAN_YEAR * SECONDS_PER_DAY;

    /** Exact number of months in one calendar year ($12\,\text{mo/yr}$). */
    public static final double MONTHS_PER_YEAR = 12.0;

    /** Exact number of Joules per Megajoule ($10^6\,\text{J/MJ}$). */
    public static final double JOULES_PER_MJ = 1.0e6;

    /** Exact number of Joules per Gigajoule ($10^9\,\text{J/GJ}$). */
    public static final double JOULES_PER_GJ = 1.0e9;

    /** Exact number of Joules per kilowatt-hour ($3.6 \times 10^6\,\text{J/kWh}$). */
    public static final double JOULES_PER_KWH = 3.6e6;

    /** Exact number of Watts corresponding to an energy flux of $1\,\text{GJ/yr}$ ($10^9 / 31\,557\,600 \approx 31.6880878\,\text{W}$). */
    public static final double WATTS_PER_GJ_PER_YEAR = JOULES_PER_GJ / SECONDS_PER_JULIAN_YEAR;

    /** Exact number of Gigajoules integrated over one IAU Julian year per Megawatt ($10^6 \times 31\,557\,600 / 10^9 = 31\,557.6\,\text{GJ/(MW}\cdot\text{yr)}$). */
    public static final double GJ_PER_MW_YEAR = (JOULES_PER_MJ * SECONDS_PER_JULIAN_YEAR) / JOULES_PER_GJ;

    /** Exact number of Exajoules integrated over one IAU Julian year per Megawatt ($10^6 \times 31\,557\,600 / 10^{18} = 3.15576 \times 10^{-5}\,\text{EJ/(MW}\cdot\text{yr)}$). */
    public static final double EJ_PER_MW_YEAR = (JOULES_PER_MJ * SECONDS_PER_JULIAN_YEAR) / 1.0e18;

    /** Exact number of kilograms per metric tonne ($1\,000\,\text{kg/t}$). */
    public static final double KG_PER_METRIC_TONNE = 1_000.0;

    /** Exact number of grams per kilogram ($1\,000\,\text{g/kg}$). */
    public static final double GRAMS_PER_KG = 1_000.0;

    /** Exact thermodynamic offset between Celsius and Kelvin ($273.15\,\text{K}$). */
    public static final double KELVIN_OFFSET = 273.15;

    /** Exact number of meters per kilometer ($1\,000\,\text{m/km}$). */
    public static final double METERS_PER_KM = 1_000.0;

    /** Exact number of square meters per square kilometer ($10^6\,\text{m}^2/\text{km}^2$). */
    public static final double SQ_METERS_PER_SQ_KM = 1.0e6;

    /** Exact number of millimeters per meter ($1\,000\,\text{mm/m}$). */
    public static final double MM_PER_METER = 1_000.0;

    /** Exact number of Pascals per hectopascal / millibar ($100\,\text{Pa/hPa}$). */
    public static final double PASCALS_PER_MILLIBAR = 100.0;

    /** Natural logarithm of 2 ($\ln 2 \approx 0.69314718056$) for Shannon bit-to-nat conversion. */
    public static final double LN_2 = Math.log(2.0);

    private PhysicalUnitsHelper() {
        // Pure static SI unit conversion utility
    }

    // =========================================================================
    // 2. TIME CONVERSIONS
    // =========================================================================

    /**
     * Converts Julian days to Julian years ($\Delta t_{\text{yr}} = \Delta t_{\text{d}} / 365.25$).
     *
     * @param days duration in days [$\text{d}$]
     * @return duration in Julian years [$\text{yr}$]
     */
    public static double daysToYears(double days) {
        return days / DAYS_PER_JULIAN_YEAR;
    }

    /**
     * Converts Julian years to Julian days ($\Delta t_{\text{d}} = \Delta t_{\text{yr}} \times 365.25$).
     *
     * @param years duration in Julian years [$\text{yr}$]
     * @return duration in days [$\text{d}$]
     */
    public static double yearsToDays(double years) {
        return years * DAYS_PER_JULIAN_YEAR;
    }

    /**
     * Converts days to SI seconds ($\Delta t_{\text{s}} = \Delta t_{\text{d}} \times 86\,400$).
     *
     * @param days duration in days [$\text{d}$]
     * @return duration in seconds [$\text{s}$]
     */
    public static double daysToSeconds(double days) {
        return days * SECONDS_PER_DAY;
    }

    /**
     * Converts SI seconds to days ($\Delta t_{\text{d}} = \Delta t_{\text{s}} / 86\,400$).
     *
     * @param seconds duration in seconds [$\text{s}$]
     * @return duration in days [$\text{d}$]
     */
    public static double secondsToDays(double seconds) {
        return seconds / SECONDS_PER_DAY;
    }

    /**
     * Converts Julian years to SI seconds ($\Delta t_{\text{s}} = \Delta t_{\text{yr}} \times 31\,557\,600$).
     *
     * @param years duration in Julian years [$\text{yr}$]
     * @return duration in seconds [$\text{s}$]
     */
    public static double yearsToSeconds(double years) {
        return years * SECONDS_PER_JULIAN_YEAR;
    }

    /**
     * Converts SI seconds to Julian years ($\Delta t_{\text{yr}} = \Delta t_{\text{s}} / 31\,557\,600$).
     *
     * @param seconds duration in seconds [$\text{s}$]
     * @return duration in Julian years [$\text{yr}$]
     */
    public static double secondsToYears(double seconds) {
        return seconds / SECONDS_PER_JULIAN_YEAR;
    }

    /**
     * Converts Julian years to calendar months ($\Delta t_{\text{mo}} = \Delta t_{\text{yr}} \times 12$).
     *
     * @param years duration in years [$\text{yr}$]
     * @return duration in months [$\text{mo}$]
     */
    public static double yearsToMonths(double years) {
        return years * MONTHS_PER_YEAR;
    }

    /**
     * Converts calendar months to Julian years ($\Delta t_{\text{yr}} = \Delta t_{\text{mo}} / 12$).
     *
     * @param months duration in months [$\text{mo}$]
     * @return duration in years [$\text{yr}$]
     */
    public static double monthsToYears(double months) {
        return months / MONTHS_PER_YEAR;
    }

    // =========================================================================
    // 3. ENERGY & POWER CONVERSIONS
    // =========================================================================

    /**
     * Converts an annual energy flux in Gigajoules per year into continuous power in Watts:
     * $$P_{\text{W}} = \Phi_{\text{GJ/yr}} \times \frac{10^9\,\text{J/GJ}}{31\,557\,600\,\text{s/yr}}$$
     *
     * @param gjPerYear energy flux [$\text{GJ/yr}$]
     * @return continuous power [$\text{W}$]
     */
    public static double gjPerYearToWatts(double gjPerYear) {
        return gjPerYear * WATTS_PER_GJ_PER_YEAR;
    }

    /**
     * Converts continuous power in Watts into an annual energy flux in Gigajoules per year:
     * $$\Phi_{\text{GJ/yr}} = P_{\text{W}} \times \frac{31\,557\,600\,\text{s/yr}}{10^9\,\text{J/GJ}}$$
     *
     * @param watts continuous power [$\text{W}$]
     * @return annual energy flux [$\text{GJ/yr}$]
     */
    public static double wattsToGjPerYear(double watts) {
        return watts / WATTS_PER_GJ_PER_YEAR;
    }

    /**
     * Integrates continuous power in Watts over a duration in Julian years into Gigajoules:
     * $$E_{\text{GJ}} = \frac{P_{\text{W}} \cdot \Delta t_{\text{yr}} \cdot 31\,557\,600}{10^9}$$
     *
     * @param watts continuous power [$\text{W}$]
     * @param years duration [$\text{yr}$]
     * @return total energy [$\text{GJ}$]
     */
    public static double wattsAndYearsToGigajoules(double watts, double years) {
        return (watts * years) / WATTS_PER_GJ_PER_YEAR;
    }

    /**
     * Converts an energy quantity in Gigajoules expended over a duration in Julian years into mean power in Watts:
     * $$P_{\text{W}} = \frac{E_{\text{GJ}} \cdot 10^9}{\Delta t_{\text{yr}} \cdot 31\,557\,600}$$
     *
     * @param gigajoules energy quantity [$\text{GJ}$]
     * @param years      positive duration [$\text{yr}$]
     * @return mean continuous power [$\text{W}$]
     */
    public static double gigajoulesOverYearsToWatts(double gigajoules, double years) {
        if (years <= 0.0) {
            return 0.0;
        }
        return (gigajoules / years) * WATTS_PER_GJ_PER_YEAR;
    }

    /**
     * Converts Joules to Gigajoules ($E_{\text{GJ}} = E_{\text{J}} \times 10^{-9}$).
     *
     * @param joules energy in Joules [$\text{J}$]
     * @return energy in Gigajoules [$\text{GJ}$]
     */
    public static double joulesToGigajoules(double joules) {
        return joules / JOULES_PER_GJ;
    }

    /**
     * Converts Gigajoules to Joules ($E_{\text{J}} = E_{\text{GJ}} \times 10^9$).
     *
     * @param gigajoules energy in Gigajoules [$\text{GJ}$]
     * @return energy in Joules [$\text{J}$]
     */
    public static double gigajoulesToJoules(double gigajoules) {
        return gigajoules * JOULES_PER_GJ;
    }

    /**
     * Converts Megajoules to Gigajoules ($E_{\text{GJ}} = E_{\text{MJ}} \times 10^{-3}$).
     *
     * @param megajoules energy in Megajoules [$\text{MJ}$]
     * @return energy in Gigajoules [$\text{GJ}$]
     */
    public static double megajoulesToGigajoules(double megajoules) {
        return megajoules * 1.0e-3;
    }

    /**
     * Converts Gigajoules to Megajoules ($E_{\text{MJ}} = E_{\text{GJ}} \times 10^3$).
     *
     * @param gigajoules energy in Gigajoules [$\text{GJ}$]
     * @return energy in Megajoules [$\text{MJ}$]
     */
    public static double gigajoulesToMegajoules(double gigajoules) {
        return gigajoules * 1.0e3;
    }

    /**
     * Converts Watts to Megawatts ($P_{\text{MW}} = P_{\text{W}} \times 10^{-6}$).
     *
     * @param watts power in Watts [$\text{W}$]
     * @return power in Megawatts [$\text{MW}$]
     */
    public static double wattsToMegawatts(double watts) {
        return watts * 1.0e-6;
    }

    /**
     * Converts Megawatts to Watts ($P_{\text{W}} = P_{\text{MW}} \times 10^6$).
     *
     * @param megawatts power in Megawatts [$\text{MW}$]
     * @return power in Watts [$\text{W}$]
     */
    public static double megawattsToWatts(double megawatts) {
        return megawatts * 1.0e6;
    }

    /**
     * Converts Watts to Terawatts ($P_{\text{TW}} = P_{\text{W}} \times 10^{-12}$).
     *
     * @param watts power in Watts [$\text{W}$]
     * @return power in Terawatts [$\text{TW}$]
     */
    public static double wattsToTerawatts(double watts) {
        return watts * 1.0e-12;
    }

    /**
     * Converts Terawatts to Watts ($P_{\text{W}} = P_{\text{TW}} \times 10^{12}$).
     *
     * @param terawatts power in Terawatts [$\text{TW}$]
     * @return power in Watts [$\text{W}$]
     */
    public static double terawattsToWatts(double terawatts) {
        return terawatts * 1.0e12;
    }

    // =========================================================================
    // 4. MASS & BIOMASS SPECIFIC ENERGY CONVERSIONS
    // =========================================================================

    /**
     * Converts kilograms to metric tonnes ($m_{\text{t}} = m_{\text{kg}} / 1\,000$).
     *
     * @param kg mass in kilograms [$\text{kg}$]
     * @return mass in metric tonnes [$\text{t}$]
     */
    public static double kgToMetricTonnes(double kg) {
        return kg / KG_PER_METRIC_TONNE;
    }

    /**
     * Converts metric tonnes to kilograms ($m_{\text{kg}} = m_{\text{t}} \times 1\,000$).
     *
     * @param metricTonnes mass in metric tonnes [$\text{t}$]
     * @return mass in kilograms [$\text{kg}$]
     */
    public static double metricTonnesToKg(double metricTonnes) {
        return metricTonnes * KG_PER_METRIC_TONNE;
    }

    /**
     * Converts biomass mass in kilograms and specific energy density in $\text{MJ/kg}$ into Gigajoules:
     * $$E_{\text{GJ}} = m_{\text{kg}} \cdot e_{\text{MJ/kg}} \cdot 10^{-3}$$
     *
     * @param massKg               mass in kilograms [$\text{kg}$]
     * @param energyDensityMjPerKg specific energy density [$\text{MJ/kg}$]
     * @return total chemical energy [$\text{GJ}$]
     */
    public static double biomassKgToEnergyGJ(double massKg, double energyDensityMjPerKg) {
        return megajoulesToGigajoules(massKg * energyDensityMjPerKg);
    }

    /**
     * Converts biomass mass in metric tonnes and specific energy density in $\text{MJ/kg}$ into Gigajoules.
     * Uses the exact SI identity $1\,\text{MJ/kg} \equiv 1\,\text{GJ/t}$.
     *
     * @param massTonnes           mass in metric tonnes [$\text{t}$]
     * @param energyDensityMjPerKg specific energy density [$\text{MJ/kg} \equiv \text{GJ/t}$]
     * @return total chemical energy [$\text{GJ}$]
     */
    public static double biomassTonnesToEnergyGJ(double massTonnes, double energyDensityMjPerKg) {
        return massTonnes * energyDensityMjPerKg;
    }

    // =========================================================================
    // 5. TEMPERATURE, LENGTH, AREA & PRESSURE CONVERSIONS
    // =========================================================================

    /**
     * Converts degrees Celsius to Kelvin ($T_{\text{K}} = T_{^\circ\text{C}} + 273.15$).
     *
     * @param celsius temperature [$^\circ\text{C}$]
     * @return absolute temperature [$\text{K}$]
     */
    public static double celsiusToKelvin(double celsius) {
        return celsius + KELVIN_OFFSET;
    }

    /**
     * Converts Kelvin to degrees Celsius ($T_{^\circ\text{C}} = T_{\text{K}} - 273.15$).
     *
     * @param kelvin absolute temperature [$\text{K}$]
     * @return temperature [$^\circ\text{C}$]
     */
    public static double kelvinToCelsius(double kelvin) {
        return kelvin - KELVIN_OFFSET;
    }

    /**
     * Converts kilometers to meters ($L_{\text{m}} = L_{\text{km}} \times 1\,000$).
     *
     * @param km distance [$\text{km}$]
     * @return distance [$\text{m}$]
     */
    public static double kmToMeters(double km) {
        return km * METERS_PER_KM;
    }

    /**
     * Converts meters to kilometers ($L_{\text{km}} = L_{\text{m}} / 1\,000$).
     *
     * @param meters distance [$\text{m}$]
     * @return distance [$\text{km}$]
     */
    public static double metersToKm(double meters) {
        return meters / METERS_PER_KM;
    }

    /**
     * Converts square kilometers to square meters ($A_{\text{m}^2} = A_{\text{km}^2} \times 10^6$).
     *
     * @param sqKm area [$\text{km}^2$]
     * @return area [$\text{m}^2$]
     */
    public static double sqKmToSqMeters(double sqKm) {
        return sqKm * SQ_METERS_PER_SQ_KM;
    }

    /**
     * Converts square meters to square kilometers ($A_{\text{km}^2} = A_{\text{m}^2} / 10^6$).
     *
     * @param sqMeters area [$\text{m}^2$]
     * @return area [$\text{km}^2$]
     */
    public static double sqMetersToSqKm(double sqMeters) {
        return sqMeters / SQ_METERS_PER_SQ_KM;
    }

    /**
     * Converts millimeters to meters ($h_{\text{m}} = h_{\text{mm}} / 1\,000$).
     *
     * @param mm length or precipitation depth [$\text{mm}$]
     * @return length or precipitation depth [$\text{m}$]
     */
    public static double mmToMeters(double mm) {
        return mm / MM_PER_METER;
    }

    /**
     * Converts millibars (hectopascals) to Pascals ($P_{\text{Pa}} = P_{\text{mbar}} \times 100$).
     *
     * @param millibars pressure [$\text{mbar} = \text{hPa}$]
     * @return pressure [$\text{Pa}$]
     */
    public static double millibarsToPascals(double millibars) {
        return millibars * PASCALS_PER_MILLIBAR;
    }

    /**
     * Converts Shannon bits to natural units of information (nats): $I_{\text{nat}} = I_{\text{bit}} \cdot \ln 2$.
     *
     * @param bits information quantity [$\text{bits}$]
     * @return information quantity [$\text{nats}$]
     */
    public static double bitsToNats(double bits) {
        return bits * LN_2;
    }
}
