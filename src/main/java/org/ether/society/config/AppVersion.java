/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <h1>Centralized Application Version Resolver</h1>
 * <p>
 * Dynamically resolves and formats the application release version from Maven build properties,
 * the active runtime JAR manifest, or the local project {@code pom.xml}.<br>
 * Automatically adapts any future version release (e.g. {@code 1.0.0-beta.2} &rarr; {@code 1.0 b2},
 * {@code 1.0.0-beta.3} &rarr; {@code 1.0 b3}, {@code 1.0.0} &rarr; {@code 1.0.0}) across splash screens,
 * window titles, persistence metadata, and UI dialogues.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 */
public final class AppVersion {
    private static final Logger logger = LoggerFactory.getLogger(AppVersion.class);

    private static final String DEFAULT_VERSION = "1.0.0-beta.2";
    private static final Pattern BETA_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(?:\\.\\d+)?[-.](?:beta|b)[-.]?(\\d+)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RC_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(?:\\.\\d+)?[-.](?:rc)[-.]?(\\d+)?$", Pattern.CASE_INSENSITIVE);

    private static String cachedRawVersion;
    private static String cachedDisplayVersion;

    static {
        resolveVersion();
    }

    private AppVersion() {
        // Static utility class
    }

    /**
     * Resolves the version from available classpath resources, package manifests, or pom.xml.
     */
    private static synchronized void resolveVersion() {
        if (cachedRawVersion != null) return;

        String ver = null;

        // 1. Try reading version.properties from classpath (filtered by Maven)
        try (InputStream is = AppVersion.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String pVer = props.getProperty("version");
                if (pVer != null && !pVer.isBlank() && !pVer.contains("${")) {
                    ver = pVer.trim();
                }
            }
        } catch (Exception ex) {
            logger.debug("Could not read version.properties: {}", ex.getMessage());
        }

        // 2. Try reading Package Implementation Version
        if (ver == null) {
            Package pkg = AppVersion.class.getPackage();
            if (pkg != null && pkg.getImplementationVersion() != null && !pkg.getImplementationVersion().isBlank()) {
                ver = pkg.getImplementationVersion().trim();
            }
        }

        // 3. Fallback: Parse pom.xml if running in workspace development mode
        if (ver == null) {
            File pomFile = new File("pom.xml");
            if (pomFile.exists()) {
                try {
                    String content = Files.readString(pomFile.toPath());
                    Matcher m = Pattern.compile("<artifactId>society-simulation</artifactId>\\s*<version>([^<]+)</version>").matcher(content);
                    if (m.find()) {
                        ver = m.group(1).trim();
                    } else {
                        Matcher m2 = Pattern.compile("<version>([^<]+)</version>").matcher(content);
                        if (m2.find()) {
                            ver = m2.group(1).trim();
                        }
                    }
                } catch (Exception ex) {
                    logger.debug("Could not parse pom.xml for version: {}", ex.getMessage());
                }
            }
        }

        if (ver == null || ver.isBlank()) {
            ver = DEFAULT_VERSION;
        }

        cachedRawVersion = ver;
        cachedDisplayVersion = formatDisplayVersion(ver);
        logger.info("Ether Application Version resolved: raw='{}', display='{}'", cachedRawVersion, cachedDisplayVersion);
    }

    /**
     * Formats a raw semantic version into standard concise user-facing badge text.
     * E.g.:
     * <ul>
     *   <li>{@code 1.0.0-beta.2} &rarr; {@code 1.0 b2}</li>
     *   <li>{@code 1.0.0-beta.3} &rarr; {@code 1.0 b3}</li>
     *   <li>{@code 1.0.0-rc.1} &rarr; {@code 1.0 rc1}</li>
     *   <li>{@code 1.0.0} &rarr; {@code 1.0.0}</li>
     * </ul>
     *
     * @param rawVersion the raw semantic version string
     * @return clean display representation
     */
    public static String formatDisplayVersion(String rawVersion) {
        if (rawVersion == null || rawVersion.isBlank()) return "1.0";
        String clean = rawVersion.trim();

        Matcher betaMatch = BETA_PATTERN.matcher(clean);
        if (betaMatch.matches()) {
            String base = betaMatch.group(1);
            String betaNum = betaMatch.group(2);
            return base + " b" + (betaNum != null && !betaNum.isBlank() ? betaNum : "1");
        }

        Matcher rcMatch = RC_PATTERN.matcher(clean);
        if (rcMatch.matches()) {
            String base = rcMatch.group(1);
            String rcNum = rcMatch.group(2);
            return base + " rc" + (rcNum != null && !rcNum.isBlank() ? rcNum : "1");
        }

        return clean;
    }

    /**
     * Returns the raw semantic version string (e.g. {@code 1.0.0-beta.2}).
     *
     * @return raw version string
     */
    public static String getRawVersion() {
        if (cachedRawVersion == null) resolveVersion();
        return cachedRawVersion;
    }

    /**
     * Returns the concise display version badge (e.g. {@code 1.0 b2} or {@code 1.0.0}).
     *
     * @return user-facing display version
     */
    public static String getDisplayVersion() {
        if (cachedDisplayVersion == null) resolveVersion();
        return cachedDisplayVersion;
    }

    /**
     * Returns the prefixed display version (e.g. {@code v1.0 b2} or {@code v1.0.0}).
     *
     * @return version string with 'v' prefix
     */
    public static String getFullDisplayVersion() {
        return "v" + getDisplayVersion();
    }

    /**
     * Returns the release title suitable for window headers (e.g. {@code Release 1.0 b2}).
     *
     * @return release title string
     */
    public static String getReleaseTitle() {
        return "Release " + getDisplayVersion();
    }
}
