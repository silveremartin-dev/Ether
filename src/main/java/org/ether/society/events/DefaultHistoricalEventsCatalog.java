/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import java.util.List;

/**
 * <h1>DefaultHistoricalEventsCatalog</h1>
 * <p>
 * Standard reference catalog of human civilizational milestones,
 * technological transitions, and macro-historical markers (from Deep Paleolithic to Modern Era).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class DefaultHistoricalEventsCatalog {

    private DefaultHistoricalEventsCatalog() {
        // Static catalog class
    }

    /**
     * Returns immutable list of canonical historical milestones.
     *
     * @return list of historical milestones
     */
    public static List<HistoricalEvent> getCanonicalEvents() {
        return List.of(
            // Prehistoric
            new HistoricalEvent(-100000, "Out of Africa", "Humans begin migrating out of Africa to new continents.", 9.02, 38.74),
            new HistoricalEvent(-40000, "Cave Art", "First known cave paintings appear, showing symbolic thought.", 44.38, 4.41),
            new HistoricalEvent(-12000, "Last Ice Age Ends", "Warming climate opens new lands for settlement.", 52.52, 13.40),

            // Neolithic
            new HistoricalEvent(-10000, "Agricultural Revolution", "Farming begins in the Fertile Crescent.", 37.22, 38.92),
            new HistoricalEvent(-8000, "First Villages", "Permanent settlements like Jericho are established.", 31.86, 35.46),
            new HistoricalEvent(-6000, "Pottery Invented", "Ceramic vessels allow food storage and trade.", 34.50, 113.60),

            // Bronze Age
            new HistoricalEvent(-3500, "Writing Invented", "Cuneiform and hieroglyphics enable record-keeping.", 31.32, 45.63),
            new HistoricalEvent(-3000, "Bronze Age Begins", "Metal tools and weapons transform society.", 33.31, 44.36),
            new HistoricalEvent(-2560, "Great Pyramid", "Massive construction project demonstrates organized labor.", 29.98, 31.13),

            // Iron Age
            new HistoricalEvent(-1200, "Iron Age Begins", "Iron working spreads, making tools accessible.", 39.50, 35.00),
            new HistoricalEvent(-800, "Greek City-States Rise", "Democracy and philosophy emerge.", 37.98, 23.72),

            // Classical
            new HistoricalEvent(-500, "Classical Era", "Golden age of Greece and early Rome.", 41.90, 12.49),
            new HistoricalEvent(0, "New Era", "A new calendar era begins.", 31.76, 35.21),
            new HistoricalEvent(476, "Fall of Rome", "Western Roman Empire collapses, beginning the medieval period.", 41.90, 12.49),

            // Medieval
            new HistoricalEvent(1000, "Medieval Warming", "Warmer climate allows population growth.", 50.85, 4.35),
            new HistoricalEvent(1347, "Black Death Arrives", "Devastating plague reaches Europe.", 38.19, 15.55),

            // Modern
            new HistoricalEvent(1492, "Age of Exploration", "Columbian exchange begins.", 37.23, -6.89),
            new HistoricalEvent(1760, "Industrial Revolution", "Steam power transforms production.", 53.48, -2.24),
            new HistoricalEvent(1945, "Nuclear Age", "First nuclear weapons used.", 34.38, 132.45)
        );
    }
}
