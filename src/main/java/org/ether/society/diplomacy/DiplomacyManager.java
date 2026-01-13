package org.ether.society.diplomacy;


import org.ether.society.model.Nation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the global list of nations and their diplomatic relationships.
 * Handles the creation of new nations and geopolitical events.
 */
public class DiplomacyManager {
    private static final Logger logger = LoggerFactory.getLogger(DiplomacyManager.class);

    private final List<Nation> nations = new CopyOnWriteArrayList<>();

    public void registerNation(Nation nation) {
        nations.add(nation);
        logger.info("New Nation Registered: {} (Color: {})", nation.getName(), nation.getColor());
    }

    public void removeNation(Nation nation) {
        nations.remove(nation);
        // Clean up references in other components if necessary
    }

    public List<Nation> getNations() {
        return Collections.unmodifiableList(nations);
    }

    public Nation getNationById(String id) {
        return nations.stream().filter(n -> n.getId().equals(id)).findFirst().orElse(null);
    }

    // Future: Relationship matrix (War, Peace, Alliance)
    // For now, implicit: Different ID = Foreign = Potential Competitor
}
