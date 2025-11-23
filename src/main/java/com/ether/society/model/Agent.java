package com.ether.society.model;

import java.util.UUID;

public abstract class Agent {
    private final String id;
    private int x;
    private int y;
    private int age; // in months
    private double health; // 0.0 to 100.0
    private double energy; // 0.0 to 100.0
    private boolean alive;

    public Agent(int x, int y) {
        this.id = UUID.randomUUID().toString();
        this.x = x;
        this.y = y;
        this.age = 0;
        this.health = 100.0;
        this.energy = 100.0;
        this.alive = true;
    }

    public abstract void tick(World world);

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
     * Move the agent by dx, dy. Returns true if move was successful.
     */
    public boolean move(int dx, int dy, World world) {
        int newX = x + dx;
        int newY = y + dy;

        // Check bounds
        if (newX >= 0 && newX < world.getWidth() && newY >= 0 && newY < world.getHeight()) {
            this.x = newX;
            this.y = newY;
            return true;
        }
        return false;
    }
}
