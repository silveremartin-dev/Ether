/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Online Data Fetching Service for real planetary satellite datasets (Earth, Moon, Mars, Venus, Mercury).
 * Connects to USGS Astrogeology WMS, NASA GIBS, and OpenTopography endpoints.
 * Caches retrieved datasets locally under ~/.ether/cache/maps/ for offline reusability.
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class OnlineMapService {
    private static final Logger logger = LoggerFactory.getLogger(OnlineMapService.class);

    private static final File CACHE_DIR = new File(System.getProperty("user.home"), ".ether/cache/maps");
    private final HttpClient httpClient;

    public OnlineMapService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        if (!CACHE_DIR.exists()) {
            CACHE_DIR.mkdirs();
        }
    }

    public enum CelestialBody {
        EARTH("Earth",
              "https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi?SERVICE=WMS&REQUEST=GetMap&VERSION=1.3.0&LAYERS=BlueMarble_ShadedRelief_Bathymetry&STYLES=&FORMAT=image/png&TRANSPARENT=TRUE&HEIGHT=512&WIDTH=1024&CRS=EPSG:4326&BBOX=-90,-180,90,180",
              "https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi?SERVICE=WMS&REQUEST=GetMap&VERSION=1.3.0&LAYERS=BlueMarble_NextGeneration&STYLES=&FORMAT=image/png&TRANSPARENT=TRUE&HEIGHT=512&WIDTH=1024&CRS=EPSG:4326&BBOX=-90,-180,90,180",
              "https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi?SERVICE=WMS&REQUEST=GetMap&VERSION=1.3.0&LAYERS=MODIS_Terra_Land_Surface_Temp_Day&STYLES=&FORMAT=image/png&TRANSPARENT=TRUE&HEIGHT=512&WIDTH=1024&CRS=EPSG:4326&BBOX=-90,-180,90,180",
              "https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi?SERVICE=WMS&REQUEST=GetMap&VERSION=1.3.0&LAYERS=IMERG_Precipitation_Rate&STYLES=&FORMAT=image/png&TRANSPARENT=TRUE&HEIGHT=512&WIDTH=1024&CRS=EPSG:4326&BBOX=-90,-180,90,180",
              null),
        MARS("Mars",
             "https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map&service=WMS&version=1.1.1&request=GetMap&layers=MOLA_dem&styles=&format=image/png&srs=EPSG:4326&bbox=-180,-90,180,90&width=1024&height=512",
             "https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map&service=WMS&version=1.1.1&request=GetMap&layers=MOLA_color&styles=&format=image/png&srs=EPSG:4326&bbox=-180,-90,180,90&width=1024&height=512",
             "https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map&service=WMS&version=1.1.1&request=GetMap&layers=THEMIS_day_IR&styles=&format=image/png&srs=EPSG:4326&bbox=-180,-90,180,90&width=1024&height=512",
             null,
             null),
        MOON("Moon",
             "https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/earth/moon_simp_cyl.map&service=WMS&version=1.1.1&request=GetMap&layers=LOLA_dem&styles=&format=image/png&srs=EPSG:4326&bbox=-180,-90,180,90&width=1024&height=512",
             null,
             null,
             null,
             null),
        VENUS("Venus",
              "https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/venus/venus_simp_cyl.map&service=WMS&version=1.1.1&request=GetMap&layers=venus_topo&styles=&format=image/png&srs=EPSG:4326&bbox=-180,-90,180,90&width=1024&height=512",
              null,
              null,
              null,
              null);

        private final String name;
        private final String elevationWmsUrl;
        private final String biomeWmsUrl;
        private final String climateWmsUrl;
        private final String rainfallWmsUrl;
        private final String hydroWmsUrl;

        CelestialBody(String name, String elevationWmsUrl, String biomeWmsUrl, String climateWmsUrl, String rainfallWmsUrl, String hydroWmsUrl) {
            this.name = name;
            this.elevationWmsUrl = elevationWmsUrl;
            this.biomeWmsUrl = biomeWmsUrl;
            this.climateWmsUrl = climateWmsUrl;
            this.rainfallWmsUrl = rainfallWmsUrl;
            this.hydroWmsUrl = hydroWmsUrl;
        }

        public String getName() { return name; }
        public String getElevationWmsUrl() { return elevationWmsUrl; }
        public String getBiomeWmsUrl() { return biomeWmsUrl; }
        public String getClimateWmsUrl() { return climateWmsUrl; }
        public String getRainfallWmsUrl() { return rainfallWmsUrl; }
        public String getHydroWmsUrl() { return hydroWmsUrl; }
    }

    /**
     * Fetch elevation map image asynchronously from USGS / NASA WMS endpoints.
     */
    public CompletableFuture<Image> fetchElevationMapAsync(CelestialBody body) {
        return fetchMapFromUrlAsync(body.getName().toLowerCase() + "_elevation.png", body.getElevationWmsUrl());
    }

    /**
     * Fetch biome/color map image asynchronously from USGS / NASA WMS endpoints.
     */
    public CompletableFuture<Image> fetchBiomeMapAsync(CelestialBody body) {
        if (body.getBiomeWmsUrl() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return fetchMapFromUrlAsync(body.getName().toLowerCase() + "_biome.png", body.getBiomeWmsUrl());
    }

    /**
     * Fetch climate/thermal map image asynchronously from NASA GIBS / USGS WMS endpoints.
     */
    public CompletableFuture<Image> fetchClimateMapAsync(CelestialBody body) {
        if (body.getClimateWmsUrl() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return fetchMapFromUrlAsync(body.getName().toLowerCase() + "_climate.png", body.getClimateWmsUrl());
    }

    /**
     * Fetch rainfall/precipitation map image asynchronously from NASA GIBS WMS endpoints.
     */
    public CompletableFuture<Image> fetchRainfallMapAsync(CelestialBody body) {
        if (body.getRainfallWmsUrl() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return fetchMapFromUrlAsync(body.getName().toLowerCase() + "_rainfall.png", body.getRainfallWmsUrl());
    }

    /**
     * Fetch hydrographic/river network map image asynchronously from NASA GIBS WMS endpoints.
     */
    public CompletableFuture<Image> fetchHydroMapAsync(CelestialBody body) {
        if (body.getHydroWmsUrl() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return fetchMapFromUrlAsync(body.getName().toLowerCase() + "_hydro.png", body.getHydroWmsUrl());
    }

    /**
     * Downloads an image from a URL, using cache if available.
     */
    public CompletableFuture<Image> fetchMapFromUrlAsync(String cacheFileName, String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        File cacheFile = new File(CACHE_DIR, cacheFileName);

        // Check local disk cache first
        if (cacheFile.exists() && cacheFile.length() > 0) {
            logger.info("Loading cached satellite dataset from {}", cacheFile.getAbsolutePath());
            try (InputStream is = new FileInputStream(cacheFile)) {
                Image cachedImage = new Image(is);
                if (!cachedImage.isError()) {
                    return CompletableFuture.completedFuture(cachedImage);
                }
            } catch (Exception e) {
                logger.warn("Failed to load cached map file: {}, redownloading...", cacheFile.getName());
            }
        }

        // Fetch remote map over HTTP WMS
        logger.info("Fetching remote satellite map from WMS service: {}", urlString);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .header("User-Agent", "EtherPlanetGenerator/2.2.0 (Human Society Simulation)")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(cacheFile.toPath()))
                .thenApply(response -> {
                    if (response.statusCode() == 200 && cacheFile.exists() && cacheFile.length() > 0) {
                        logger.info("Successfully downloaded satellite dataset: {} ({} bytes)", cacheFile.getName(), cacheFile.length());
                        try (InputStream is = new FileInputStream(cacheFile)) {
                            return new Image(is);
                        } catch (Exception e) {
                            logger.error("Error parsing downloaded map image", e);
                        }
                    } else {
                        logger.warn("WMS fetch returned HTTP status code: {}", response.statusCode());
                    }
                    return null;
                })
                .exceptionally(ex -> {
                    logger.error("WMS remote satellite fetch failed: {}", ex.getMessage());
                    return null;
                });
    }
}

