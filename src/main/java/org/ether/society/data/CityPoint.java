/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

public class CityPoint {
    public String name;
    public double lat, lng, weight, sigma;

    public CityPoint(String name, double lat, double lng, double weight, double sigma) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.weight = weight;
        this.sigma = sigma;
    }
}
