/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous HTTP/CDN Data Downloader and Local Disk Cache Manager for Ether.
 * Downloads missing HYDE 3.4, Natural Earth, GADM, and HGIS datasets directly from public vaults or GitHub releases.
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 4.0.0
 */
public class DataDownloaderService {
    private static final Logger logger = LoggerFactory.getLogger(DataDownloaderService.class);

    public static final String HYDE_VAULT_URL = "https://geo.public.data.uu.nl/vault-hyde/";
    public static final String GITHUB_HYDE_RELEASES = "https://github.com/UtrechtUniversity/Hyde-Platform/raw/master/";

    /**
     * Ensures a dataset file exists locally; if not, downloads it asynchronously.
     */
    public static CompletableFuture<File> ensureFileDownloaded(String remoteUrl, String localRelativePath) {
        return CompletableFuture.supplyAsync(() -> {
            File targetFile = new File(localRelativePath);
            if (targetFile.exists() && targetFile.length() > 0) {
                logger.info("Dataset file already exists locally: {}", targetFile.getAbsolutePath());
                return targetFile;
            }

            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            logger.info("Downloading remote cartographic dataset from {} -> {}", remoteUrl, targetFile.getAbsolutePath());
            try {
                URL url = URI.create(remoteUrl).toURL();
                try (BufferedInputStream in = new BufferedInputStream(url.openStream());
                     FileOutputStream fileOutputStream = new FileOutputStream(targetFile)) {
                    byte[] dataBuffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(dataBuffer, 0, 8192)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }
                }
                logger.info("Successfully downloaded dataset: {} ({} bytes)", targetFile.getName(), targetFile.length());
                return targetFile;
            } catch (Exception e) {
                logger.warn("Failed to download dataset from {}: {}", remoteUrl, e.getMessage());
                return null;
            }
        });
    }
}
