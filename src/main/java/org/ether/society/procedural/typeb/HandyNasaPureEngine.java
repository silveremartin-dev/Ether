/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

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
 * @version 1.0.0-beta.1
 */
public class HandyNasaPureEngine {
    private static final Logger logger = LoggerFactory.getLogger(HandyNasaPureEngine.class);

    private double commonersX = 100.0;
    private double elitesY = 1.0;
    private double natureX = 100.0; // Nature carrying capacity
    private double wealthK = 0.0;

    public void processTick(double deltaYears) {
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

    public double getCommonersX() { return commonersX; }
    public double getElitesY() { return elitesY; }
    public double getNatureX() { return natureX; }
    public double getWealthK() { return wealthK; }
}

