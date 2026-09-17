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
package org.ether.society.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Configuration POJO for simulation parameters, loaded from JSON.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1-beta.1
 * @since 1.0.0
 */
public record Configuration(
        @JsonProperty("world") WorldConfig world,
        @JsonProperty("simulation") SimulationConfig simulation,
        @JsonProperty("agents") AgentsConfig agents,
        @JsonProperty("climate") ClimateConfig climate,
        @JsonProperty("resources") ResourcesConfig resources) {

    public record WorldConfig(
            @JsonProperty("width") int width,
            @JsonProperty("height") int height,
            @JsonProperty("seed") long seed,
            @JsonProperty("generationParams") GenerationParams generationParams) {
    }

    public record GenerationParams(
            @JsonProperty("elevationOctaves") int elevationOctaves,
            @JsonProperty("elevationScale") double elevationScale,
            @JsonProperty("rainfallOctaves") int rainfallOctaves,
            @JsonProperty("rainfallScale") double rainfallScale) {
    }

    public record SimulationConfig(
            @JsonProperty("startYear") int startYear,
            @JsonProperty("tickRateMs") int tickRateMs,
            @JsonProperty("speedMultipliers") int[] speedMultipliers) {
    }

    public record AgentsConfig(
            @JsonProperty("initialHumans") int initialHumans,
            @JsonProperty("initialAnimals") Map<String, Integer> initialAnimals) {
    }

    public record ClimateConfig(
            @JsonProperty("seasonalVariation") double seasonalVariation,
            @JsonProperty("latitudeEffect") double latitudeEffect,
            @JsonProperty("elevationLapseRate") double elevationLapseRate) {
    }

    public record ResourcesConfig(
            @JsonProperty("renewalRate") double renewalRate,
            @JsonProperty("maxCapacity") double maxCapacity,
            @JsonProperty("harvestEfficiency") double harvestEfficiency) {
    }
}


