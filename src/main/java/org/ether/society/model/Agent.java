/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Silvere Martin-Michiellot (silvere.martin@gmail.com)
 * Contributors: AI Assistant (Antigravity/Claude)
 */
package org.ether.society.model;

import java.util.UUID;

/**
 * Base class for all agents in the simulation (humans, animals).
 * Contains common properties like position, age, health, and energy.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 1.0.0
 */
public abstract class Agent {
    private final String id;
    private int x;
    private int y;
    private int age; // in months
    private double health; // 0.0 to 100.0
    private double energy; // 0.0 to 100.0
    private boolean alive;

    /**
     * Creates an agent at the specified position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     */
    public Agent(int x, int y) {
        this.id = UUID.randomUUID().toString();
        this.x = x;
        this.y = y;
        this.age = 0;
        this.health = 100.0;
        this.energy = 100.0;
        this.alive = true;
    }

    /**
     * Updates the agent for one simulation tick.
     *
     * @param world The world the agent exists in
     */
    public abstract void tick(World world);

    // Getters and setters

    public String getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void incrementAge() {
        this.age++;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    /**
     * Moves the agent by the specified delta.
     *
     * @param dx    X displacement
     * @param dy    Y displacement
     * @param world The world for bounds checking
     * @return true if move was successful
     */
    public boolean move(int dx, int dy, World world) {
        int newX = x + dx;
        int newY = y + dy;

        if (newX >= 0 && newX < world.getWidth() && newY >= 0 && newY < world.getHeight()) {
            this.x = newX;
            this.y = newY;
            return true;
        }
        return false;
    }
}

