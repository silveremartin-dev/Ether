/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ether.society.model.ClimateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Ingestion and querying service for historical milestones from Wikidata SPARQL,
 * D-PLACE ethnographic database, and local historical repositories (Seshat Databank).
 */
public class WikidataEventsFetcher {
    private static final Logger logger = LoggerFactory.getLogger(WikidataEventsFetcher.class);
    private static final ObjectMapper mapper = org.ether.society.persistence.EtherJsonUtil.getMapper();

    /** Primary Earth catalog file path under data/history/ether/earth/events/ */
    public static final String CATALOG_FILE_PATH = "data/history/ether/earth/events/historical_events_catalog.json";
    /** Intermediate catalog file path for backward compatibility. */
    public static final String INTERMEDIATE_CATALOG_FILE_PATH = "data/maps/ether/earth/history/events/historical_events_catalog.json";
    /** Legacy catalog file path for backward compatibility. */
    public static final String LEGACY_CATALOG_FILE_PATH = "data/ether/earth/events/historical_events_catalog.json";

    /**
     * Normalizes event type keys to the standard prefixed naming convention.
     */
    public static String normalizeEventType(String rawType) {
        if (rawType == null || rawType.isBlank()) return "milestone_archaeology";
        String t = rawType.trim().toLowerCase();
        return switch (t) {
            case "milestone", "archaeology", "monument", "culture" -> "milestone_archaeology";
            case "historical", "polity", "politics", "state", "empire" -> "milestone_polity";
            case "tech_singularity", "technology", "tech", "science", "invention" -> "milestone_technology";
            case "renaissance_boom", "golden_age", "renaissance", "enlightenment" -> "milestone_golden_age";
            case "ethnography", "dplace", "indigenous", "traditional" -> "milestone_ethnography";
            case "volcano" -> "disaster_volcano";
            case "earthquake" -> "disaster_earthquake";
            case "tsunami" -> "disaster_tsunami";
            case "pandemic" -> "disaster_pandemic";
            case "heatwave" -> "disaster_heatwave";
            case "ice_age" -> "disaster_ice_age";
            case "meteor" -> "disaster_meteor";
            case "solar_emp" -> "disaster_solar_emp";
            case "nuclear_strike" -> "disaster_nuclear_strike";
            case "nuclear_winter" -> "disaster_nuclear_winter";
            case "economic_crash" -> "disaster_economic_crash";
            case "cyber_attack" -> "disaster_cyber_attack";
            case "biodiversity_collapse" -> "disaster_biodiversity_collapse";
            case "geoengineering" -> "disaster_geoengineering";
            case "alien_contact" -> "disaster_alien_contact";
            default -> t.startsWith("milestone_") || t.startsWith("disaster_") ? t : "milestone_" + t;
        };
    }

    /**
     * Standard SPARQL query targeting historical events, battles, settlements, and civilizational milestones.
     */
    public static final String SPARQL_HISTORICAL_EVENTS_QUERY = """
        SELECT ?event ?eventLabel ?date ?lat ?lon ?descLabel WHERE {
          ?event wdt:P31/wdt:P279* wd:Q1190554;
                 wdt:P625 ?coord;
                 wdt:P585 ?date.
          BIND(geof:latitude(?coord) AS ?lat)
          BIND(geof:longitude(?coord) AS ?lon)
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en,es,de,zh". }
        }
        ORDER BY ?date
        LIMIT 1500
        """;

    /**
     * Executes the Wikidata SPARQL query and parses results into a list of ClimateEvent objects.
     */
    public static List<ClimateEvent> fetchFromWikidata(int limit) {
        List<ClimateEvent> events = new ArrayList<>();
        String query = SPARQL_HISTORICAL_EVENTS_QUERY;
        if (limit > 0) {
            query = query.replace("LIMIT 1500", "LIMIT " + limit);
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://query.wikidata.org/sparql?query=" + encodedQuery;

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "EtherHistoricalSimulation/1.0 (https://github.com/ether; contact@ether-simulation.org)")
                    .header("Accept", "application/sparql-results+json")
                    .GET()
                    .build();

            logger.info("Executing Wikidata SPARQL query for global historical events...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode bindings = root.path("results").path("bindings");
                if (bindings.isArray()) {
                    for (JsonNode b : bindings) {
                        try {
                            String name = b.path("eventLabel").path("value").asText("Historical Milestone");
                            String dateStr = b.path("date").path("value").asText("");
                            double lat = b.path("lat").path("value").asDouble(0.0);
                            double lon = b.path("lon").path("value").asDouble(0.0);

                            int year = parseWikidataYear(dateStr);
                            events.add(new ClimateEvent("milestone_polity", name, year, lat, lon, 0.0, 5.0));
                        } catch (Exception ex) {
                            logger.trace("Skipping malformed event entry", ex);
                        }
                    }
                }
                logger.info("Successfully fetched {} historical events from Wikidata SPARQL.", events.size());
            } else {
                logger.warn("Wikidata SPARQL query failed with HTTP code: {}", response.statusCode());
            }
        } catch (Exception e) {
            logger.warn("Could not fetch events from online Wikidata SPARQL endpoint: {}", e.getMessage());
        }
        return events;
    }

    /**
     * Extracts and builds a consolidated catalog from local Seshat datasets.
     */
    public static List<ClimateEvent> loadSeshatMilestones() {
        List<ClimateEvent> events = new ArrayList<>();
        File seshatCapitals = new File("data/maps/seshat/seshat_api_capitals.json");
        if (!seshatCapitals.exists()) {
            return events;
        }

        try {
            JsonNode root = mapper.readTree(seshatCapitals);
            JsonNode results = root.path("results");
            if (results.isArray()) {
                for (JsonNode node : results) {
                    String name = node.path("name").asText("Seshat Historical Polity");
                    String note = node.path("note").asText("");
                    double lat = Double.parseDouble(node.path("latitude").asText("0.0"));
                    double lon = Double.parseDouble(node.path("longitude").asText("0.0"));

                    int year = extractYearFromNote(note);
                    String label = "Fondation / Capitale : " + name + (note.isEmpty() ? "" : " (" + note + ")");
                    if (label.length() > 120) label = label.substring(0, 117) + "...";
                    events.add(new ClimateEvent("milestone_polity", label, year, lat, lon, 0.0, 5.0));
                }
            }
            logger.info("Loaded {} historical polities and capitals from Seshat databank.", events.size());
        } catch (Exception e) {
            logger.warn("Error parsing Seshat local database: {}", e.getMessage());
        }
        return events;
    }

    /**
     * Loads the unified catalog from disk, or builds and saves it if not already generated.
     */
    public static List<ClimateEvent> getOrBuildFullCatalog() {
        File catalogFile = new File(CATALOG_FILE_PATH);
        if (!catalogFile.exists()) {
            catalogFile = new File(LEGACY_CATALOG_FILE_PATH);
        }
        if (catalogFile.exists()) {
            try {
                JsonNode root = mapper.readTree(catalogFile);
                if (root.isArray()) {
                    List<ClimateEvent> list = new ArrayList<>();
                    for (JsonNode n : root) {
                        list.add(new ClimateEvent(
                                normalizeEventType(n.path("type").asText("milestone_archaeology")),
                                n.path("name").asText("Historical Milestone"),
                                n.path("year").asInt(0),
                                n.path("latitude").asDouble(0.0),
                                n.path("longitude").asDouble(0.0),
                                n.path("depth").asDouble(0.0),
                                n.path("magnitude").asDouble(5.0)
                        ));
                    }
                    logger.info("Loaded {} chronological events from local catalog '{}'.", list.size(), CATALOG_FILE_PATH);
                    return list;
                }
            } catch (Exception ex) {
                logger.warn("Could not read existing catalog file '{}', rebuilding...", CATALOG_FILE_PATH, ex);
            }
        }

        List<ClimateEvent> fullList = new ArrayList<>();

        // 1. Core historical, civilizational & cataclysmic milestones
        fullList.addAll(getCoreMilestones());

        // 2. Seshat Global History Databank
        fullList.addAll(loadSeshatMilestones());

        // 3. D-PLACE Cross-Cultural & Ethnographic Societies
        fullList.addAll(DPlaceIngestionService.loadDPlaceMilestones());

        // 4. Save consolidated database to disk
        saveCatalogToDisk(fullList, catalogFile);

        return fullList;
    }

    /**
     * Save catalog to disk.
     *
     * @param list the list parameter (List&lt;ClimateEvent&gt;)
     * @param file the file parameter (File)
     */
    public static void saveCatalogToDisk(List<ClimateEvent> list, File file) {
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, list);
            logger.info("Successfully persisted {} events into '{}'.", list.size(), file.getPath());
        } catch (Exception e) {
            logger.error("Failed to persist historical events catalog to '{}'", file.getPath(), e);
        }
    }

    private static int parseWikidataYear(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return 0;
        try {
            boolean isBce = dateStr.startsWith("-");
            String cleaned = dateStr.replace("+", "").replace("-", "");
            int year = Integer.parseInt(cleaned.split("-")[0]);
            return isBce ? -year : year;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int extractYearFromNote(String note) {
        if (note == null || note.isEmpty()) return -1000;
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,4})\\s*(BCE|BC|CE|AD)").matcher(note);
            if (m.find()) {
                int val = Integer.parseInt(m.group(1));
                String era = m.group(2).toUpperCase();
                return (era.contains("BC")) ? -val : val;
            }
            java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("(\\d{3,4})").matcher(note);
            if (m2.find()) {
                return Integer.parseInt(m2.group(1));
            }
        } catch (Exception ignored) {}
        return -1000;
    }

    private static List<ClimateEvent> getCoreMilestones() {
        return DefaultHistoricalMilestonesCatalog.buildComprehensiveCatalog();
    }
}
