/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SvgMapIngestorTest {

    @Test
    @DisplayName("Verify SVG map ingestion for embedded scenario maps")
    public void testEmbeddedSvgIngestion() throws Exception {
        Scenario scenario = new Scenario();
        scenario.setName("Test Roman Empire");
        scenario.setPopulationDensityType("ROMAN_EMPIRE");

        HistoricalMapGenerator.populateScenarioHistoricalMaps(scenario);

        assertThat(scenario.getCustomDensityBase64()).isNotNull();
        assertThat(scenario.getCustomTensorMapBase64(0)).isNotNull();
        assertThat(scenario.getCustomTensorMapBase64(1)).isNotNull();
        assertThat(scenario.getCustomTensorMapBase64(2)).isNotNull();
        assertThat(scenario.getCustomTensorMapBase64(3)).isNotNull();
    }

    @Test
    @DisplayName("Verify inline SVG string parsing and rasterization")
    public void testInlineSvgParsing() throws Exception {
        String testSvg = """
            <svg viewBox="0 0 1024 512" width="1024" height="512">
              <g data-layer="SOVEREIGNTY">
                <path d="M 100 100 L 200 100 L 200 200 L 100 200 Z" fill="#FF0000" />
              </g>
              <g data-layer="ISOGLOSS">
                <path d="M 300 100 L 400 100 L 400 200 L 300 200 Z" fill="#00FF00" />
              </g>
            </svg>
            """;

        SvgMapIngestor.SvgIngestionResult result = SvgMapIngestor.ingestSvgContent(testSvg);

        assertThat(result).isNotNull();
        assertThat(result.parsedFeaturesCount).isEqualTo(2);
        assertThat(result.sovereigntyImage).isNotNull();
        assertThat(result.isoglossImage).isNotNull();
        assertThat(result.sovereigntyBase64).isNotNull();
        assertThat(result.isoglossBase64).isNotNull();
    }
}
