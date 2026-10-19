*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

/**
 * <h1>River Ribbon</h1>
 * <p>
 * Geospatial data ingestion, raster sampling, and tensor map management pipeline.<br>
 * Ingests global planetary datasets (NOAA ETOPO, WorldClim, UNESCO WHYMAP, Natural Earth, Seshat, D-PLACE) into standardized H3 hexagonal rasters.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class RiverRibbon {
    public double lng1, lat1, lng2, lat2, weight, widthDeg;

    /**
     * River ribbon.
     *
     * @param lng1 the lng1 parameter (double)
     * @param lat1 the lat1 parameter (double)
     * @param lng2 the lng2 parameter (double)
     * @param lat2 the lat2 parameter (double)
     * @param weight the weight parameter (double)
     * @param widthDeg the width deg parameter (double)
     */
    public RiverRibbon(double lng1, double lat1, double lng2, double lat2, double weight, double widthDeg) {
        this.lng1 = lng1;
        this.lat1 = lat1;
        this.lng2 = lng2;
        this.lat2 = lat2;
        this.weight = weight;
        this.widthDeg = widthDeg;
    }

    /**
     * Distance to point.
     *
     * @param px the px parameter (double)
     * @param py the py parameter (double)
     * @return the resulting computation or state reference
     */
    public double distanceToPoint(double px, double py) {
        double midLat = (lat1 + lat2) * 0.5;
        double cosLat = Math.cos(Math.toRadians(midLat));
        double vx = (lng2 - lng1) * cosLat;
        double vy = lat2 - lat1;
        double l2 = vx * vx + vy * vy;
        if (l2 == 0) {
            double dlatP = lat1 - py;
            double dlngP = (lng1 - px) * Math.cos(Math.toRadians((lat1 + py) * 0.5));
            return Math.sqrt(dlngP * dlngP + dlatP * dlatP);
        }
        double dx = (px - lng1) * cosLat;
        double dy = py - lat1;
        double t = Math.max(0.0, Math.min(1.0, (dx * vx + dy * vy) / l2));
        double projX = lng1 + t * (lng2 - lng1);
        double projY = lat1 + t * (lat2 - lat1);
        double dlatP = projY - py;
        double dlngP = (projX - px) * Math.cos(Math.toRadians((projY + py) * 0.5));
        return Math.sqrt(dlngP * dlngP + dlatP * dlatP);
    }
}
