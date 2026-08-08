/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

public interface ISimulationEngine {
    void start();

    void pause();

    void reset();

    void setSpeed(int multiplier);

    int getSpeed();

    TimeManager getTimeManager();

    boolean isRunning();
}

