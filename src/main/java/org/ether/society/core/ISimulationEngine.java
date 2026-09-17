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

    void stepForward(int ticks);

    void stepBackward(int ticks);

    default void seekToEnd() {}

    default void setPauseAtNextEvent(boolean pause) {}

    default boolean isPauseAtNextEvent() { return false; }

    default H3SimulationEngine.TemporalScale getTemporalScale() { return H3SimulationEngine.TemporalScale.DAILY; }

    default org.ether.society.model.Scenario getCurrentScenario() { return null; }

    default long getTickCounter() { return 0L; }

    default org.ether.society.events.EventSystem getEventSystem() { return null; }
}

