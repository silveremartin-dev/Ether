/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FetchUsgsPetroleumDatasets {
    private static final Logger logger = LoggerFactory.getLogger(FetchUsgsPetroleumDatasets.class);

    @Test
    public void fetchUsgsDatasets() throws Exception {
        String[] itemIds = {
            "60ad2fd7d34e4043c850edb3", // Geologic Provinces of the World, all defined provinces
            "60ad2fa1d34e4043c850ed98", // Geologic Provinces of the World, Assessed provinces only
            "60bfec21d34e86b938917fa7", // World Assessment Unit Summary and Geological Characterizations
            "60c3b391d34e86b93897ee35"  // World Total Petroleum System Summary and Geological Characterization
        };

        Path targetDir = Paths.get("data", "maps", "wep_world_energy", "usgs_world_petroleum");
        Files.createDirectories(targetDir);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        for (String id : itemIds) {
            String metadataUrl = "https://www.sciencebase.gov/catalog/item/" + id + "?format=json";
            logger.info("Fetching metadata for ScienceBase item: {}", id);

            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(metadataUrl))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(25))
                        .GET()
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    String json = resp.body();
                    logger.info("Retrieved metadata for item {} (length {} chars)", id, json.length());

                    // Find files array with name and url
                    Pattern p = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher m = p.matcher(json);
                    while (m.find()) {
                        String fileName = m.group(1);
                        String fileUrl = m.group(2);
                        logger.info("Found file in item {}: {} -> {}", id, fileName, fileUrl);

                        if (fileName.endsWith(".zip") || fileName.endsWith(".tar.gz") || fileName.endsWith(".csv") || fileName.endsWith(".shp") || fileName.endsWith(".dbf") || fileName.endsWith(".txt")) {
                            downloadAndExtract(client, fileUrl, fileName, targetDir);
                        }
                    }
                } else {
                    logger.warn("HTTP {} for item {}", resp.statusCode(), id);
                }
            } catch (Exception e) {
                logger.error("Error fetching ScienceBase item {}: {}", id, e.getMessage());
            }
        }
    }

    private void downloadAndExtract(HttpClient client, String fileUrl, String fileName, Path targetDir) {
        Path destFile = targetDir.resolve(fileName);
        logger.info("Downloading {} to {}...", fileName, destFile);

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(fileUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();

            HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() == 200) {
                try (InputStream is = resp.body();
                     FileOutputStream fos = new FileOutputStream(destFile.toFile())) {
                    byte[] buf = new byte[32768];
                    int read;
                    long total = 0;
                    while ((read = is.read(buf)) != -1) {
                        fos.write(buf, 0, read);
                        total += read;
                    }
                    logger.info("Successfully downloaded {} ({} bytes)", fileName, total);
                }

                if (fileName.endsWith(".zip")) {
                    extractZip(destFile, targetDir);
                }
            } else {
                logger.warn("HTTP {} downloading {}", resp.statusCode(), fileName);
            }
        } catch (Exception e) {
            logger.error("Failed downloading {}: {}", fileName, e.getMessage());
        }
    }

    private void extractZip(Path zipFile, Path targetDir) {
        logger.info("Extracting ZIP archive: {}", zipFile);
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    try (FileOutputStream fos = new FileOutputStream(outPath.toFile())) {
                        byte[] buf = new byte[16384];
                        int r;
                        while ((r = zis.read(buf)) != -1) {
                            fos.write(buf, 0, r);
                        }
                    }
                    logger.info("  Extracted: {} ({} bytes)", entry.getName(), outPath.toFile().length());
                }
            }
        } catch (Exception e) {
            logger.error("Failed extracting {}: {}", zipFile, e.getMessage());
        }
    }
}
