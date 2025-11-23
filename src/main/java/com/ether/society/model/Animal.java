package com.ether.society.model;

public class Animal extends Agent {
    public enum Species {
        COW, CHICKEN, SHEEP, HORSE, FISH, WILD_GAME
    }
    
    private final Species species;

    public Animal(int x, int y, Species species) {
        super(x, y);
        this.species = species;
    }

    @Override
    public void tick(World world) {
        if (!isAlive()) return;
        incrementAge();
        
        // Simple logic: graze if land animal
        if (species != Species.FISH) {
            // Graze logic
        }
    }
    
    public Species getSpecies() { return species; }
}
