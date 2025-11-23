package com.ether.society.model.events;

import com.ether.society.model.World;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EventManager {
    private final List<Event> activeEvents = new ArrayList<>();
    private final Random random = new Random();

    public void tick(World world) {
        // Process active events
        Iterator<Event> iterator = activeEvents.iterator();
        while (iterator.hasNext()) {
            Event event = iterator.next();
            event.onTick(world);
            if (event.isFinished()) {
                event.onEnd(world);
                iterator.remove();
            }
        }

        // Randomly trigger new events (e.g., 0.1% chance per year)
        if (random.nextDouble() < 0.001) {
            triggerEvent(new VolcanicEruption(5), world);
        }
    }

    public void triggerEvent(Event event, World world) {
        activeEvents.add(event);
        event.onStart(world);
    }
    
    public List<Event> getActiveEvents() {
        return activeEvents;
    }
}
