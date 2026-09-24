/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.ClimateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service for ingesting, indexing, and querying ethnographic and cultural datasets from D-PLACE
 * (Murdock Ethnographic Atlas, SCCS, Binford Foraging Frames, and Glottolog language mappings).
 */
public class DPlaceIngestionService {
    private static final Logger logger = LoggerFactory.getLogger(DPlaceIngestionService.class);

    public static final String DPLACE_BASE_PATH = "data/maps/dplace";

    public record DPlaceSociety(
            String id,
            String name,
            String glottocode,
            String altNames,
            int focalYear,
            double latitude,
            double longitude,
            String dataset
    ) {}

    /**
     * Loads pre-industrial and indigenous societies from D-PLACE datasets as informative milestone events.
     */
    public static List<ClimateEvent> loadDPlaceMilestones() {
        List<ClimateEvent> events = new ArrayList<>();
        List<DPlaceSociety> societies = loadAllSocieties();

        for (DPlaceSociety soc : societies) {
            String label = "Société Traditionnelle : " + soc.name() 
                    + (soc.glottocode() != null && !soc.glottocode().isBlank() ? " [" + soc.glottocode() + "]" : "")
                    + " (" + soc.dataset() + ")";
            if (label.length() > 120) label = label.substring(0, 117) + "...";

            events.add(new ClimateEvent(
                    "milestone_ethnography",
                    label,
                    soc.focalYear(),
                    soc.latitude(),
                    soc.longitude(),
                    0.0,
                    5.0
            ));
        }

        logger.info("Loaded {} ethnographic and indigenous societies from D-PLACE database.", events.size());
        return events;
    }

    /**
     * Parses societies from EA (Ethnographic Atlas), SCCS, and Binford datasets.
     */
    public static List<DPlaceSociety> loadAllSocieties() {
        List<DPlaceSociety> list = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        // 1. Murdock Ethnographic Atlas (1,291 societies)
        loadSocietiesFromCsv(new File(DPLACE_BASE_PATH, "datasets/EA/societies.csv"), "EA", list, seenNames);

        // 2. Binford Constructing Frames of Reference (339 foraging societies)
        loadSocietiesFromCsv(new File(DPLACE_BASE_PATH, "datasets/Binford/societies.csv"), "Binford", list, seenNames);

        // 3. SCCS (Standard Cross-Cultural Sample)
        loadSocietiesFromCsv(new File(DPLACE_BASE_PATH, "datasets/SCCS/societies.csv"), "SCCS", list, seenNames);

        return list;
    }

    private static void loadSocietiesFromCsv(File file, String datasetName, List<DPlaceSociety> target, Set<String> seenNames) {
        if (!file.exists()) {
            logger.warn("D-PLACE dataset file not found: {}", file.getPath());
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) return;

            String[] headerCols = parseCsvLine(header);
            int idxId = findColIndex(headerCols, "id");
            int idxName = findColIndex(headerCols, "pref_name_for_society");
            int idxGlotto = findColIndex(headerCols, "glottocode");
            int idxAlt = findColIndex(headerCols, "alt_names_by_society");
            int idxYear = findColIndex(headerCols, "main_focal_year");
            int idxLat = findColIndex(headerCols, "Lat");
            int idxLon = findColIndex(headerCols, "Long");

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = parseCsvLine(line);
                if (cols.length <= Math.max(idxLat, idxLon)) continue;

                try {
                    String id = idxId >= 0 && idxId < cols.length ? cols[idxId].trim() : "";
                    String name = idxName >= 0 && idxName < cols.length ? cols[idxName].trim() : "";
                    if (name.isBlank() || seenNames.contains(name.toLowerCase(Locale.ROOT))) {
                        continue;
                    }
                    seenNames.add(name.toLowerCase(Locale.ROOT));

                    String glotto = idxGlotto >= 0 && idxGlotto < cols.length ? cols[idxGlotto].trim() : "";
                    String alt = idxAlt >= 0 && idxAlt < cols.length ? cols[idxAlt].trim() : "";
                    
                    int year = 1800; // Default pre-industrial baseline
                    if (idxYear >= 0 && idxYear < cols.length && !cols[idxYear].isBlank()) {
                        try {
                            year = Integer.parseInt(cols[idxYear].trim().replaceAll("[^0-9-]", ""));
                        } catch (Exception ignored) {}
                    }

                    double lat = Double.parseDouble(cols[idxLat].trim());
                    double lon = Double.parseDouble(cols[idxLon].trim());

                    target.add(new DPlaceSociety(id, name, glotto, alt, year, lat, lon, datasetName));
                } catch (Exception ex) {
                    logger.trace("Skipping invalid society row in {}: {}", file.getName(), line);
                }
            }
        } catch (Exception e) {
            logger.error("Error reading D-PLACE dataset {}: {}", file.getPath(), e.getMessage());
        }
    }

    private static int findColIndex(String[] headers, String colName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(colName)) {
                return i;
            }
        }
        return -1;
    }

    public static String[] parseCsvLine(String line) {
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString());
        return list.toArray(new String[0]);
    }
}
