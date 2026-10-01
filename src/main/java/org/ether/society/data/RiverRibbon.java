/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

public class RiverRibbon {
    public double lng1, lat1, lng2, lat2, weight, widthDeg;

    public RiverRibbon(double lng1, double lat1, double lng2, double lat2, double weight, double widthDeg) {
        this.lng1 = lng1;
        this.lat1 = lat1;
        this.lng2 = lng2;
        this.lat2 = lat2;
        this.weight = weight;
        this.widthDeg = widthDeg;
    }

    public double distanceToPoint(double px, double py) {
        double dlat = lat1 - lat2;
        double dlng = (lng1 - lng2) * Math.cos(Math.toRadians((lat1 + lat2) * 0.5));
        double l2 = dlng * dlng + dlat * dlat;
        if (l2 == 0) {
            double dlatP = lat1 - py;
            double dlngP = (lng1 - px) * Math.cos(Math.toRadians((lat1 + py) * 0.5));
            return Math.sqrt(dlngP * dlngP + dlatP * dlatP);
        }
        double t = Math.max(0, Math.min(1, ((px - lng1) * (lng2 - lng1) + (py - lat1) * (lat2 - lat1)) / l2));
        double projX = lng1 + t * (lng2 - lng1);
        double projY = lat1 + t * (lat2 - lat1);
        double dlatP = projY - py;
        double dlngP = (projX - px) * Math.cos(Math.toRadians((projY + py) * 0.5));
        return Math.sqrt(dlngP * dlngP + dlatP * dlatP);
    }
}
