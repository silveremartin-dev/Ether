package org.ether.society.model.events;

import org.ether.society.model.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VolcanicEruption implements Event {
    private static final Logger logger = LoggerFactory.getLogger(VolcanicEruption.class);
    private int durationYears;
    private int yearsPassed = 0;

    public VolcanicEruption(int durationYears) {
        this.durationYears = durationYears;
    }

    @Override
    public String getName() {
        return "Volcanic Eruption";
    }

    @Override
    public void onStart(World world) {
        logger.info("A massive volcanic eruption has occurred! Temperatures dropping, crops failing.");
        world.setGlobalTemperatureOffset(-2.0); // Drop by 2 degrees
        world.setGlobalHarvestModifier(0.5); // 50% harvest reduction
    }

    @Override
    public void onTick(World world) {
        yearsPassed++;
        // Slowly recover
        if (yearsPassed > durationYears / 2) {
             world.setGlobalHarvestModifier(0.5 + (0.5 * ((double)(yearsPassed - durationYears/2) / (durationYears/2))));
        }
    }

    @Override
    public void onEnd(World world) {
        logger.info("The volcanic winter has ended.");
        world.setGlobalTemperatureOffset(0.0);
        world.setGlobalHarvestModifier(1.0);
    }

    @Override
    public boolean isFinished() {
        return yearsPassed >= durationYears;
    }
}
