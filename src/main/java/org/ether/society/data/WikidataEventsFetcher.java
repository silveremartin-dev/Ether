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
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final String CATALOG_FILE_PATH = "data/events/historical_events_catalog.json";

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
            case "famine" -> "disaster_famine";
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
        List<ClimateEvent> list = new ArrayList<>();

        // =========================================================================
        // 1. PALÉOLITHIQUE INFÉRIEUR & MOYEN (-1 000 000 à -50 000 BP)
        // =========================================================================
        list.add(new ClimateEvent("milestone_technology", "Domestication Précoce du Feu (Grotte de Wonderwerk)", -1000000, -27.84, 23.55, 0.0, 5.0));
        list.add(new ClimateEvent("milestone_technology", "Industrie Lithique Acheuléenne (Olorgesailie)", -800000, -1.58, 36.44, 0.0, 5.0));
        list.add(new ClimateEvent("milestone_technology", "Javelots de Schöningen & Chasse Coordonnée", -500000, 52.13, 10.96, 0.0, 5.0));
        list.add(new ClimateEvent("milestone_archaeology", "Émergence Anatomique d'Homo Sapiens (Djebel Irhoud)", -315000, 31.85, -8.87, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_technology", "Débitage Levallois & Emmanchement Lithique", -300000, 49.89, 2.30, 0.0, 5.0));
        list.add(new ClimateEvent("disaster_ice_age", "Optimum Interglaciaire Eémien", -130000, 0.0, 0.0, 0.0, 5.0));
        list.add(new ClimateEvent("milestone_archaeology", "Première Dispersion Hors d'Afrique (Skhul/Qafzeh)", -100000, 32.70, 35.00, 0.0, 5.5));
        list.add(new ClimateEvent("disaster_volcano", "Super-Éruption du Toba (VEI-8, τ=3.50, Refroidissement Global)", -74000, 2.88, 98.88, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_archaeology", "Gravures sur Ocre & Parures Symboliques (Blombos)", -70000, -34.41, 21.22, 0.0, 5.0));

        // =========================================================================
        // 2. PALÉOLITHIQUE SUPÉRIEUR (-50 000 à -12 000 BP)
        // =========================================================================
        list.add(new ClimateEvent("milestone_archaeology", "Peuplement Maritime du Sahul (Australie)", -50000, -25.0, 133.0, 0.0, 5.0));
        list.add(new ClimateEvent("milestone_archaeology", "Arrivée d'Homo Sapiens en Europe & Interaction Néandertal", -45000, 44.0, 5.0, 0.0, 5.0));
        list.add(new ClimateEvent("milestone_archaeology", "Art Pariétal & Pensée Symbolique (Grotte Chauvet)", -40000, 44.38, 4.41, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_archaeology", "Premiers Instruments de Musique / Flûtes en Os (Hohle Fels)", -35000, 48.38, 9.75, 0.0, 5.0));
        list.add(new ClimateEvent("milestone_archaeology", "Statues de Vénus Gravettiennes (Willendorf)", -30000, 48.32, 15.41, 0.0, 5.0));
        list.add(new ClimateEvent("milestone_archaeology", "Traversée de la Béringie vers les Amériques", -25000, 65.0, -168.0, 0.0, 5.5));
        list.add(new ClimateEvent("disaster_ice_age", "Dernier Maximum Glaciaire (LGM) & Refuges Solutréens", -20000, 55.0, 10.0, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_technology", "Invention de la Céramique Cuite (Grotte de Xianrendong)", -18000, 28.70, 117.20, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_archaeology", "Art Pariétal Magdalénien (Grotte de Lascaux)", -15000, 45.05, 1.17, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_archaeology", "Art Pariétal Polychrome (Grotte d'Altamira)", -14000, 43.37, -4.12, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_technology", "Première Domestication du Loup / Chien (Bonn-Oberkassel)", -14000, 50.72, 7.15, 0.0, 5.0));
        list.add(new ClimateEvent("disaster_ice_age", "Retrait des Glaciers & Fin du Dernier Pléniglaciaire", -12000, 52.52, 13.40, 0.0, 5.0));

        // =========================================================================
        // 3. RÉVOLUTION NÉOLITHIQUE & SÉDENTARISATION (-12 000 à -3500 BCE)
        // =========================================================================
        list.add(new ClimateEvent("disaster_ice_age", "Refroidissement Abrupt du Dryas Récent", -10900, 60.0, -20.0, 0.0, 4.5));
        list.add(new ClimateEvent("milestone_technology", "Révolution Néolithique & Agriculture Fondatrice (Croissant Fertile)", -10000, 37.22, 38.92, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_archaeology", "Sanctuaire Mégalithique Monumental de Göbekli Tepe", -9500, 37.22, 38.92, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_technology", "Domestication des Caprins & Ovins (Monts Zagros)", -8500, 34.0, 46.0, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_archaeology", "Premiers Remparts Urbains & Tour de Pierre de Jéricho", -8000, 31.86, 35.46, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_technology", "Domestication de la Riziculture Inondée (Bassin du Yangtze)", -7500, 29.50, 118.00, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_archaeology", "Proto-Ville Néolithique de Çatalhöyük", -7000, 37.66, 32.82, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_technology", "Domestication du Maïs (Téosinte, Vallée de Balsas)", -6500, 18.00, -100.00, 0.0, 6.0));
        list.add(new ClimateEvent("disaster_ice_age", "Événement Climatique Froid de 8200 cal BP", -6200, 55.0, 0.0, 0.0, 5.0));
        list.add(new ClimateEvent("milestone_archaeology", "Période Humide Africaine & Art Rupestre du Sahara Vert (Tassili)", -6000, 25.0, 9.0, 0.0, 5.0));
        list.add(new ClimateEvent("milestone_archaeology", "Culture de Yangshao & Céramique Peinte (Chine)", -6000, 34.50, 113.60, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_technology", "Culture de Samarra & Premiers Réseaux d'Irrigation Fluviale", -5500, 34.20, 43.80, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_technology", "Culture de Vinča & Métallurgie Extractive du Cuivre (Balkans)", -5000, 44.76, 20.62, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_archaeology", "Alignements Mégalithiques de Carnac (Bretagne)", -4500, 47.58, -3.08, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_archaeology", "Mégalopoles Chalcolithiques de Cucuteni-Trypillia", -4000, 48.0, 28.0, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_technology", "Domestication du Cheval (Culture de Botaï, Steppes)", -3800, 53.30, 67.65, 0.0, 6.0));

        // =========================================================================
        // 4. ÂGE DU BRONZE & PREMIERS ÉTATS (-3500 à -1200 BCE)
        // =========================================================================
        list.add(new ClimateEvent("milestone_technology", "Invention de l'Écriture Cunéiforme & Urbanisme à Uruk", -3500, 31.32, 45.63, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_archaeology", "Civilisation Hydraulique du Jade de Liangzhu (Chine)", -3300, 30.40, 120.00, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_polity", "Unification de l'Égypte sous Narmer & Hiéroglyphes", -3200, 25.69, 32.59, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_technology", "Métallurgie du Bronze & Invention de la Roue à Rayons", -3000, 33.31, 44.36, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_archaeology", "Cité Sacrée de Caral-Supe (Plus Ancienne Cité d'Amérique)", -3000, -10.89, -77.52, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_archaeology", "Civilisation de l'Indus & Hygiène Urbaine (Harappa & Mohenjo-Daro)", -2600, 27.32, 68.13, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_archaeology", "Construction de la Grande Pyramide de Khéops à Gizeh", -2560, 29.98, 31.13, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_polity", "Archives Royales Cunéiformes d'Ebla (Syrie)", -2500, 35.79, 36.79, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_archaeology", "Érection Monumentale de Stonehenge (Wiltshire)", -2500, 51.18, -1.82, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_polity", "Fondation du Premier Empire Territorial par Sargon d'Akkad", -2334, 33.10, 44.10, 0.0, 7.0));
        list.add(new ClimateEvent("disaster_famine", "Événement Climatique Aride de 4.2 ka BP (Sécheresse Mésopotamie/Nil)", -2200, 33.0, 44.0, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_golden_age", "Civilisation Minoenne & Palais de Cnossos (Crète)", -2000, 35.29, 25.16, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_archaeology", "Comptoirs Marchands Assyriens de Kanesh (Anatolie)", -1900, 38.85, 35.63, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_polity", "Code de Lois & Jurisprudence d'Hammurabi (Babylone)", -1750, 32.54, 44.42, 0.0, 7.0));
        list.add(new ClimateEvent("disaster_volcano", "Éruption Cataclysmique de Théra / Santorin (VEI-7)", -1640, 36.40, 25.40, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_polity", "Dynastie Shang & Écriture sur Os Oraculaires (Anyang)", -1600, 36.10, 114.35, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_archaeology", "Expansion Maritime de la Culture Lapita (Pacifique Ouest)", -1500, -10.0, 160.0, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_polity", "Citadelles Mycéniennes & Écriture Linéaire B (Grèce)", -1400, 37.73, 22.75, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_polity", "Bataille de Qadech & Traité de Paix Égypto-Hittite", -1274, 34.56, 36.52, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_archaeology", "Bronzes Mystiques de Sanxingdui (Sichuan)", -1200, 30.99, 104.20, 0.0, 6.5));
        list.add(new ClimateEvent("disaster_economic_crash", "Effondrement Généralisé de l'Âge du Bronze Récent & Peuples de la Mer", -1200, 35.0, 35.0, 0.0, 7.5));

        // =========================================================================
        // 5. ÂGE DU FER & ANTIQUITÉ CLASSIQUE (-1200 BCE à 500 CE)
        // =========================================================================
        list.add(new ClimateEvent("milestone_technology", "Métallurgie de Masse du Fer en Anatolie & Levant", -1200, 39.50, 35.00, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_archaeology", "Civilisation Olmèque & Têtes Colossales de San Lorenzo", -1000, 17.75, -94.76, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_technology", "Culture de Nok & Métallurgie Précoce du Fer au Nigeria", -1000, 9.50, 8.00, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_archaeology", "Centre Cérémoniel Andin de Chavín de Huántar (Pérou)", -900, -9.59, -77.17, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_polity", "Fondation Maritime de Carthage par les Phéniciens", -814, 36.85, 10.32, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_archaeology", "Épopées Homériques & Émergence de la Polis Grecque", -800, 37.98, 23.72, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_archaeology", "Premiers Jeux Olympiques Antiques à Olympie", -776, 37.64, 21.62, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_polity", "Fondation de Rome (Ab Urbe Condita)", -753, 41.90, 12.49, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_polity", "Royaume de Koush & Capitale Royale de Méroé (Nubie)", -750, 16.94, 33.73, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_technology", "Alphabet Phonétique Phénicien & Monnaies Frappées en Lydie", -600, 33.27, 35.20, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_polity", "Empire Achéménide Fondé par Cyrus le Grand (Pasargades)", -550, 30.19, 53.18, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_archaeology", "Construction Palatiale de Persépolis par Darius Ier", -518, 29.93, 52.89, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_polity", "Démocratie Athénienne & Siècle de Périclès", -508, 37.98, 23.72, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_golden_age", "Enseignements de Siddhartha Gautama (Bouddha) & Upanishads", -500, 27.48, 83.27, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_golden_age", "Philosophie de Confucius & Période des Royaumes Combattants", -475, 35.60, 116.98, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_archaeology", "Construction du Parthénon sur l'Acropole d'Athènes", -447, 37.97, 23.72, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_polity", "Conquêtes d'Alexandre & Fondation de la Bibliothèque d'Alexandrie", -331, 31.20, 29.91, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_polity", "Empire Maurya & Édits de Tolérance d'Ashoka (Pataliputra)", -268, 25.61, 85.14, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_polity", "Unification Impériale de la Chine par Qin Shi Huang (Xi'an)", -221, 34.26, 108.95, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_polity", "Dynastie Han & Ouverture de la Route de la Soie", -202, 34.26, 108.95, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_archaeology", "Géoglyphes & Lignes Cérémonielles de Nazca (Pérou)", -200, -14.73, -75.13, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_archaeology", "Fondation de la Métropole de Teotihuacán (Mésoamérique)", -100, 19.69, -98.84, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_archaeology", "Cité Nabatéenne Taillée dans le Roc de Pétra (Jordanie)", -100, 30.32, 35.44, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_polity", "Assassinat de Jules César & Fin de la République Romaine", -44, 41.90, 12.49, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_polity", "Établissement du Principat d'Auguste & Pax Romana", 0, 41.90, 12.49, 0.0, 6.5));
        list.add(new ClimateEvent("disaster_volcano", "Éruption du Vésuve & Destruction de Pompéi (Italie)", 79, 40.82, 14.43, 0.0, 5.0));
        list.add(new ClimateEvent("milestone_archaeology", "Inauguration du Colisée / Amphithéâtre Flavien à Rome", 80, 41.89, 12.49, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_technology", "Invention du Papier Chiffon par Cai Lun (Dynastie Han)", 105, 34.65, 112.45, 0.0, 7.5));
        list.add(new ClimateEvent("disaster_pandemic", "Peste Antonine (Variole dans l'Empire Romain)", 165, 41.90, 12.49, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_golden_age", "Âge d'Or de l'Empire Gupta en Inde (Sciences, Trigonométrie & Zéro)", 320, 25.61, 85.14, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_polity", "Conversion du Royaume d'Aksoum & Stèles Royales (Éthiopie)", 330, 14.13, 38.72, 0.0, 6.5));
        list.add(new ClimateEvent("disaster_earthquake", "Séisme Majeur de Crète & Tsunami Méditerranéen", 365, 35.20, 24.50, 20.0, 8.5));
        list.add(new ClimateEvent("milestone_archaeology", "Peuplement Polynésien de l'Archipel d'Hawaï", 400, 21.30, -157.85, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_polity", "Déposition de Romulus Augustule & Chute de Rome d'Occident", 476, 41.90, 12.49, 0.0, 7.5));

        // =========================================================================
        // 6. MOYEN ÂGE & EMPIRES POST-CLASSIQUES (500 à 1500 CE)
        // =========================================================================
        list.add(new ClimateEvent("disaster_earthquake", "Grand Séisme d'Antioche (250k victimes)", 526, 36.20, 36.16, 15.0, 7.0));
        list.add(new ClimateEvent("disaster_volcano", "Anomalie Volcanique Majeure & Hiver Global de 536", 536, 13.70, -89.20, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_archaeology", "Consécration de la Basilique Sainte-Sophie à Constantinople", 537, 41.00, 28.97, 0.0, 7.0));
        list.add(new ClimateEvent("disaster_pandemic", "Peste de Justinien (Peste Bubonique en Méditerranée)", 541, 41.00, 28.97, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_technology", "Construction du Grand Canal de Chine (Dynastie Sui)", 605, 34.0, 115.0, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_golden_age", "Dynastie Tang & Rayonnement Cosmopolite de Chang'an", 618, 34.26, 108.95, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_polity", "Expansion de l'Islam & Califats Rashidun / Omeyyade", 632, 21.42, 39.82, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_golden_age", "Fondation de Bagdad & Maison de la Sagesse (Âge d'Or Islamique)", 762, 33.31, 44.36, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_polity", "Couronnement de Charlemagne & Renaissance Carolingienne", 800, 41.90, 12.49, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_archaeology", "Colonisation Polynésienne de Rapa Nui (Île de Pâques) & Moaï", 800, -27.11, -109.36, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_archaeology", "Édification du Sanctuaire Bouddhiste de Borobudur (Java)", 825, -7.60, 110.20, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_technology", "Impression du Soutra du Diamant (Premier Livre Imprimé Daté)", 868, 40.14, 94.66, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_archaeology", "Apogée Monumentale de la Civilisation Maya Classique (Tikal)", 900, 17.22, -89.62, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_polity", "Empire Maritime Tu'i Tonga dans le Pacifique Sud", 950, -21.17, -175.20, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_technology", "Dynastie Song (Papier-Monnaie, Poudre à Canon, Boussole)", 960, 34.79, 114.30, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_archaeology", "Fondation de l'Université Sankoré à Tombouctou (Mali)", 989, 16.77, -3.00, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_archaeology", "Optimum Climatique Médiéval & Navigation Viking au Groenland", 1000, 64.14, -21.94, 0.0, 5.5));
        list.add(new ClimateEvent("milestone_polity", "Royaume de Pagan & Construction des Temples (Birmanie)", 1044, 21.17, 94.86, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_archaeology", "Métropole Mississippienne & Mounds Géants de Cahokia (Illinois)", 1050, 38.65, -90.06, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_golden_age", "Fondation de l'Université de Bologne (Première d'Occident)", 1088, 44.49, 11.34, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_archaeology", "Murailles en Pierre Sèche du Grand Zimbabwe (Afrique)", 1100, -20.27, 30.93, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_archaeology", "Construction Monumentale d'Angkor Wat (Empire Khmer)", 1150, 13.41, 103.86, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_archaeology", "Cités Troglodytiques des Falaises de Mesa Verde (Colorado)", 1190, 37.18, -108.48, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_archaeology", "Cité Mégalithique Flottante de Nan Madol (Micronésie)", 1200, 6.84, 158.33, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_archaeology", "Bronzes & Métallurgie d'Art du Royaume de Bénin (Nigeria)", 1200, 6.33, 5.62, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_polity", "Unification Nomade & Conquêtes de Gengis Khan (Karakorum)", 1206, 47.19, 102.83, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_polity", "Signature de la Magna Carta à Runnymede (Angleterre)", 1215, 51.44, -0.56, 0.0, 7.0));
        list.add(new ClimateEvent("disaster_volcano", "Super-Éruption du Mont Samalas / Rinjani (Lombok, VEI-7)", 1257, -8.40, 116.47, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_polity", "Dynastie Yuan & Pax Mongolica (Voyages de Marco Polo)", 1279, 39.90, 116.40, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_archaeology", "Premier Peuplement Maori de Nouvelle-Zélande (Aotearoa)", 1280, -41.28, 174.77, 0.0, 6.0));
        list.add(new ClimateEvent("disaster_famine", "Grande Famine Européenne de 1315-1317", 1315, 50.0, 10.0, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_golden_age", "Apogée de l'Empire du Mali & Pèlerinage de Mansa Musa", 1324, 12.63, -8.00, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_polity", "Fondation de Tenochtitlan par les Aztèques (Mexique)", 1325, 19.43, -99.13, 0.0, 7.0));
        list.add(new ClimateEvent("disaster_pandemic", "Pandémie Dévastatrice de la Peste Noire en Eurasie", 1347, 38.19, 15.55, 0.0, 9.0));
        list.add(new ClimateEvent("milestone_golden_age", "Renaissance Timouride & Architecture de Samarcande", 1370, 39.65, 66.97, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_polity", "Fondation du Royaume du Kongo par Lukeni lua Nimi", 1390, -6.26, 14.24, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_archaeology", "Grandes Expéditions Maritimes de Zheng He (Flotte des Trésors)", 1405, 32.05, 118.78, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_polity", "Expansion de l'Empire Inca (Tawantinsuyu) & Machu Picchu", 1438, -13.16, -72.54, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_technology", "Promulgation de l'Alphabet Hangul par Sejong le Grand (Corée)", 1443, 37.56, 126.97, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_technology", "Invention de l'Imprimerie à Caractères Mobiles par Gutenberg", 1450, 50.00, 8.27, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_polity", "Prise de Constantinople par Mehmed II & Fin de Byzance", 1453, 41.00, 28.97, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_polity", "Premier Voyage de Christophe Colomb & Échange Colombien", 1492, 24.06, -74.53, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_polity", "Signature du Traité de Tordesillas (Partage du Nouveau Monde)", 1494, 41.50, -5.00, 0.0, 7.0));

        // =========================================================================
        // 7. ÉPOQUE MODERNE & RÉVOLUTION SCIENTIFIQUE (1500 à 1800 CE)
        // =========================================================================
        list.add(new ClimateEvent("milestone_polity", "95 Thèses de Luther & Début de la Réforme Protestante", 1517, 51.86, 12.64, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_polity", "Chute de l'Empire Aztèque à Tenochtitlan", 1521, 19.43, -99.13, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_technology", "Publication de la Théorie Héliocentrique par Copernic", 1543, 54.35, 18.64, 0.0, 8.0));
        list.add(new ClimateEvent("disaster_pandemic", "Épidémie Dévastatrice de Cocoliztli au Mexique", 1545, 19.43, -99.13, 0.0, 7.5));
        list.add(new ClimateEvent("disaster_earthquake", "Séisme Meurtrier du Shaanxi (Chine, ~830k victimes)", 1556, 34.50, 109.70, 20.0, 8.0));
        list.add(new ClimateEvent("milestone_archaeology", "Promulgation du Calendrier Grégorien par Grégoire XIII", 1582, 41.90, 12.45, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_golden_age", "Ispahan Capitale Safavide & Apogée Architecturale (Iran)", 1598, 32.65, 51.66, 0.0, 7.0));
        list.add(new ClimateEvent("disaster_volcano", "Éruption du Volcan Huaynaputina (Pérou & Froid Hémisphérique)", 1600, -16.60, -71.35, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_technology", "Perfectionnement de la Lunette Astronomique par Galilée", 1609, 45.40, 11.87, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_polity", "Édit de Sakoku & Isolement Strict du Japon Tokugawa", 1639, 35.68, 139.69, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_polity", "Traités de Paix de Westphalie & Système d'États Souverains", 1648, 51.96, 7.62, 0.0, 7.5));
        list.add(new ClimateEvent("disaster_pandemic", "Grande Peste de Londres", 1665, 51.50, -0.12, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_technology", "Publication des Principia Mathematica d'Isaac Newton", 1687, 52.20, 0.12, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_earthquake", "Séisme de Subduction & Mégatsunami de Cascadia", 1700, 45.00, -125.00, 20.0, 9.0));
        list.add(new ClimateEvent("milestone_golden_age", "Publication de l'Encyclopédie de Diderot & d'Alembert", 1751, 48.85, 2.35, 0.0, 7.5));
        list.add(new ClimateEvent("disaster_earthquake", "Grand Séisme et Tsunami de Lisbonne (M8.7)", 1755, 38.72, -9.13, 30.0, 8.7));
        list.add(new ClimateEvent("milestone_technology", "Brevet de la Machine à Vapeur à Condenseur Séparé (Watt)", 1769, 53.48, -2.24, 0.0, 9.0));
        list.add(new ClimateEvent("milestone_polity", "Déclaration d'Indépendance des États-Unis (Philadelphie)", 1776, 39.95, -75.15, 0.0, 7.5));
        list.add(new ClimateEvent("disaster_volcano", "Éruption Fissurale du Laki (Islande, Nuage Toxique & Famine)", 1783, 64.06, -17.33, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_polity", "Prise de la Bastille & Révolution Française (Droits de l'Homme)", 1789, 48.85, 2.35, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_technology", "Première Vaccination Antivariolique par Edward Jenner", 1796, 51.72, -2.46, 0.0, 8.0));

        // =========================================================================
        // 8. ÈRE INDUSTRIELLE & XIXe SIÈCLE (1800 à 1900 CE)
        // =========================================================================
        list.add(new ClimateEvent("milestone_technology", "Première Locomotive à Vapeur sur Rails de Trevithick", 1804, 51.74, -3.37, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_volcano", "Éruption Cataclysmique du Tambora (Année sans été)", 1815, -8.25, 117.98, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_technology", "Brevet du Télégraphe Électrique & Code Morse", 1837, 40.71, -74.00, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_polity", "Guerre Civile de la Rébellion des Taiping en Chine", 1850, 32.05, 118.78, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_technology", "Publication de l'Origine des Espèces par Charles Darwin", 1859, 51.50, -0.12, 0.0, 8.0));
        list.add(new ClimateEvent("disaster_solar_emp", "Événement Solaire de Carrington (Tempête Géomagnétique Majeure)", 1859, 50.0, 0.0, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_polity", "Restauration de Meiji & Modernisation Accélérée du Japon", 1868, 35.68, 139.69, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_technology", "Inauguration et Ouverture du Canal de Suez (Égypte)", 1869, 30.58, 32.26, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_technology", "Brevet de l'Ampoule à Incandescence & Réseaux Électriques", 1879, 40.56, -74.33, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_volcano", "Éruption Cataclysmique & Explosion du Krakatoa", 1883, -6.10, 105.42, 0.0, 6.5));
        list.add(new ClimateEvent("milestone_polity", "Loi d'Or Abolissant Définitivement l'Esclavage au Brésil", 1888, -22.90, -43.17, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_polity", "Bataille d'Adoua : Préservation de l'Indépendance Éthiopienne", 1896, 14.16, 38.89, 0.0, 6.5));

        // =========================================================================
        // 9. XXe SIÈCLE & TEMPS PRÉSENTS (1900 à 2025 CE)
        // =========================================================================
        list.add(new ClimateEvent("disaster_volcano", "Éruption de la Montagne Pelée & Nuée Ardente (Martinique)", 1902, 14.81, -61.16, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_technology", "Premier Vol Motorisé Contrôlé des Frères Wright", 1903, 36.01, -75.67, 0.0, 8.0));
        list.add(new ClimateEvent("disaster_earthquake", "Grand Séisme et Incendie de San Francisco", 1906, 37.77, -122.41, 10.0, 7.9));
        list.add(new ClimateEvent("disaster_meteor", "Impact Météoritique Aérien de la Toungouska (Sibérie)", 1908, 60.89, 101.89, 0.0, 5.0));
        list.add(new ClimateEvent("milestone_technology", "Inauguration du Canal de Panama", 1914, 9.08, -79.68, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_polity", "Déclenchement de la Première Guerre Mondiale", 1914, 50.0, 3.0, 0.0, 8.0));
        list.add(new ClimateEvent("disaster_pandemic", "Pandémie Mondiale de Grippe Espagnole (H1N1)", 1918, 40.00, 0.00, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_earthquake", "Grand Séisme du Kanto (Destruction de Tokyo-Yokohama)", 1923, 35.30, 139.30, 15.0, 7.9));
        list.add(new ClimateEvent("milestone_technology", "Découverte de la Pénicilline par Alexander Fleming", 1928, 51.51, -0.17, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_economic_crash", "Krach Boursier de Wall Street & Grande Dépression", 1929, 40.70, -74.01, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_polity", "Déclenchement de la Seconde Guerre Mondiale", 1939, 52.52, 13.40, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_nuclear_strike", "Début de l'Ère Nucléaire (Essai Trinity & Hiroshima)", 1945, 34.38, 132.45, 0.0, 9.0));
        list.add(new ClimateEvent("milestone_technology", "Invention du Transistor aux Laboratoires Bell", 1947, 40.68, -74.40, 0.0, 9.0));
        list.add(new ClimateEvent("milestone_polity", "Indépendance et Partition de l'Inde & du Pakistan", 1947, 28.61, 77.20, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_technology", "Lancement de Spoutnik 1 & Début de l'Ère Spatiale", 1957, 45.96, 63.30, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_earthquake", "Grand Séisme de Valdivia (Chili, Record Mondial M9.5)", 1960, -38.14, -73.41, 25.0, 9.5));
        list.add(new ClimateEvent("milestone_archaeology", "Premier Pas Humain sur la Lune (Apollo 11)", 1969, 28.57, -80.64, 0.0, 9.0));
        list.add(new ClimateEvent("milestone_technology", "Premier Nœud du Réseau ARPANET (Origine d'Internet)", 1969, 34.06, -118.44, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_economic_crash", "Premier Choc Pétrolier Mondial & Crise Énergétique", 1973, 24.0, 45.0, 0.0, 7.5));
        list.add(new ClimateEvent("disaster_earthquake", "Séisme Meurtrier de Tangshan (Chine, ~240k victimes)", 1976, 39.63, 118.18, 12.0, 7.6));
        list.add(new ClimateEvent("disaster_nuclear_winter", "Catastrophe Nucléaire Majeure de Tchernobyl", 1986, 51.38, 30.10, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_polity", "Chute du Mur de Berlin & Ouverture du Rideau de Fer", 1989, 52.51, 13.37, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_technology", "Invention du World Wide Web au CERN par Tim Berners-Lee", 1989, 46.23, 6.05, 0.0, 9.0));
        list.add(new ClimateEvent("disaster_volcano", "Éruption du Pinatubo & Aérosols Stratosphériques", 1991, 15.13, 120.35, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_polity", "Dissolution Officielle de l'URSS & Fin de la Guerre Froide", 1991, 55.75, 37.61, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_polity", "Élection de Nelson Mandela & Fin de l'Apartheid en Afrique du Sud", 1994, -25.74, 28.18, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_technology", "Séquençage Complet du Génome Humain (Projet HGP)", 2000, 38.90, -77.03, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_tsunami", "Séisme et Tsunami Géant de Sumatra-Andaman (M9.1)", 2004, 3.30, 95.98, 30.0, 9.1));
        list.add(new ClimateEvent("milestone_technology", "Mise en Service du Barrage des Trois-Gorges (Chine)", 2006, 30.82, 111.00, 0.0, 7.0));
        list.add(new ClimateEvent("disaster_economic_crash", "Crise Financière Mondiale des Subprimes & Faillite Lehman", 2008, 40.71, -74.00, 0.0, 8.0));
        list.add(new ClimateEvent("disaster_earthquake", "Séisme Catastrophique de Port-au-Prince (Haïti)", 2010, 18.53, -72.33, 13.0, 7.0));
        list.add(new ClimateEvent("disaster_tsunami", "Séisme du Tōhoku, Tsunami Géant & Accident de Fukushima", 2011, 38.30, 142.37, 29.0, 9.0));
        list.add(new ClimateEvent("disaster_pandemic", "Pandémie Mondiale de COVID-19 (SARS-CoV-2)", 2020, 30.59, 114.30, 0.0, 8.0));
        list.add(new ClimateEvent("disaster_volcano", "Éruption Explosive du Volcan Hunga Tonga (Onde Atmosphérique)", 2022, -20.53, -175.38, 0.0, 6.0));
        list.add(new ClimateEvent("milestone_archaeology", "La Population Mondiale Franchit le Seuil des 8 Milliards", 2022, 0.0, 0.0, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_technology", "Déploiement Massif des Réseaux d'IA & Calcul Exaflopique", 2024, 37.38, -122.08, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_technology", "Avancées Clés en Fusion Thermonucléaire & Énergies Décarbonées", 2025, 43.60, 5.70, 0.0, 8.5));
        return list;
    }
}
