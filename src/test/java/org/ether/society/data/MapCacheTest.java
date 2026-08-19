package org.ether.society.data;

import org.junit.jupiter.api.Test;
import java.io.File;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MapCacheTest {

    @Test
    public void testPrecacheAllBuiltInScenarios() {
        HistoricalMapGenerator.precacheAllBuiltInScenarios();
        File cacheDir = new File("data/maps/cache");
        assertTrue(cacheDir.exists() && cacheDir.isDirectory(), "Cache directory should exist");
        File[] files = cacheDir.listFiles();
        assertTrue(files != null && files.length > 0, "Disk cache should contain populated PNG map images");
    }
}
