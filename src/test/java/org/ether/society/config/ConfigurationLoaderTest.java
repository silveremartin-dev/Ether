package org.ether.society.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationLoaderTest {

    @Test
    @DisplayName("Load default configuration returns non-null configuration")
    void testLoadDefault() throws IOException {
        Configuration config = ConfigurationLoader.loadDefault();

        assertNotNull(config);
        assertNotNull(config.world());
        assertNotNull(config.simulation());
        assertNotNull(config.climate());
    }
}
