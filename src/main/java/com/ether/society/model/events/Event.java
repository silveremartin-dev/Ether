package com.ether.society.model.events;

import com.ether.society.model.World;

public interface Event {
    String getName();
    void onStart(World world);
    void onTick(World world);
    void onEnd(World world);
    boolean isFinished();
}
