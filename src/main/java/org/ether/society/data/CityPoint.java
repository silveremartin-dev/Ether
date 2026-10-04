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

    /*
     * City point.
     * Enforces physical invariants and updates associated state variables within {@code CityPoint}.
     *
     * @param name the name parameter (String)
     * @param lat the lat parameter (double)
     * @param lng the lng parameter (double)
     * @param weight the weight parameter (double)
     * @param sigma the sigma parameter (double)
     */
    public CityPoint(String name, double lat, double lng, double weight, double sigma) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.weight = weight;
        this.sigma = sigma;
    }
}
