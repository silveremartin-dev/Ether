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

/**
 * Represents animal agents in the simulation.
 * Animals include livestock and wild species that interact with the
 * environment.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 1.0.0
 */
public class Animal extends Agent {
    /**
     * Species types for animals.
     */
    public enum Species {
        /** Domesticated cattle */
        COW,
        /** Domesticated poultry */
        CHICKEN,
        /** Domesticated sheep */
        SHEEP,
        /** Domesticated/wild horses */
        HORSE,
        /** Wild game animals */
        WILD_GAME,
        /** Fish and marine life */
        FISH
    }

    private final Species species;

    /**
     * Creates an animal at the specified position.
     *
     * @param x       X coordinate
     * @param y       Y coordinate
     * @param species Animal species
     */
    public Animal(int x, int y, Species species) {
        super(x, y);
        this.species = species;
    }

    @Override
    public void tick(World world) {
        if (!isAlive())
            return;
        incrementAge();

        // Simple grazing logic for land animals
        if (species != Species.FISH) {
            // Basic implementation: recover small amount of energy
            if (getEnergy() < 100) {
                setEnergy(Math.min(100, getEnergy() + 5.0));
            }
        }
    }

    public Species getSpecies() {
        return species;
    }
}
