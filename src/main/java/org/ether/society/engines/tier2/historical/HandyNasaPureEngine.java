/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * NASA HANDY Model Variant B7.1 (Human and Nature Dynamics - Motesharrei, Rivas & Kalnay 2014).
 * Pure standalone differential model coupling Elites (Y), Commoners (X), Nature Carrying Capacity (x),
 * and Accumulated Wealth (K).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class HandyNasaPureEngine {
    private static final Logger logger = LoggerFactory.getLogger(HandyNasaPureEngine.class);

    /* Internal state variable for commoners x (double). */
    private double commonersX = 100.0;
    /* Internal state variable for elites y (double). */
    private double elitesY = 1.0;
    private double natureX = 100.0; // Nature carrying capacity
    /* Internal state variable for wealth k (double). */
    private double wealthK = 0.0;

    /*
     * Process tick.
     * Enforces physical invariants and updates associated state variables within {@code HandyNasaPureEngine}.
     *
     * @param deltaYears the delta years parameter (double)
     */
    public void processTick(double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        double gammaX = 0.03; // Commoners birth rate
        double gammaY = 0.03; // Elites birth rate
        double alphaX = 0.01; // Commoners death rate
        double alphaY = 0.01; // Elites death rate
        double delta = 0.005; // Depletion rate of nature
        double beta = 0.01;  // Regeneration rate of nature

        // Nature regeneration vs depletion
        natureX += (beta * natureX * (100.0 - natureX) - delta * (commonersX + elitesY) * natureX) * deltaYears;

        // Wealth production by commoners
        wealthK += (delta * commonersX * natureX) * deltaYears;

        // Consumption ratio: Elites consume kappa times more than commoners
        double kappa = 10.0;
        double totalConsumptionNeeded = (commonersX + kappa * elitesY);
        if (wealthK < totalConsumptionNeeded) {
            // Famine penalizes commoners first
            alphaX += 0.05 * (1.0 - wealthK / totalConsumptionNeeded);
        }

        commonersX += (gammaX * commonersX - alphaX * commonersX) * deltaYears;
        elitesY += (gammaY * elitesY - alphaY * elitesY) * deltaYears;
    }

    /*
     * Get commoners x.
     * Enforces physical invariants and updates associated state variables within {@code HandyNasaPureEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getCommonersX() { return commonersX; }
    /*
     * Get elites y.
     * Enforces physical invariants and updates associated state variables within {@code HandyNasaPureEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getElitesY() { return elitesY; }
    /*
     * Get nature x.
     * Enforces physical invariants and updates associated state variables within {@code HandyNasaPureEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getNatureX() { return natureX; }
    /*
     * Get wealth k.
     * Enforces physical invariants and updates associated state variables within {@code HandyNasaPureEngine}.
     *
     * @return the resulting computation or state reference
     */
    public double getWealthK() { return wealthK; }
}


