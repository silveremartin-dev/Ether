package org.ether.society.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;

public class ExtractScenarioTelemetryReportTest {

    @Test
    public void printScenarioTrajectories() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        printScenario(mapper, "saves/RUN-LA-PESTE-NOIRE---INVERSION--CONOMIQUE-POST-F-ODALE--1347--2282/history.json",
                "LA PESTE NOIRE (1347 - 1353)");

        printScenario(mapper, "saves/RUN-GUERRES-MONDIALES--RUPTURE-TOTALITAIRE----RE-NUCL-AIRE--1914--4673/history.json",
                "GUERRES MONDIALES (1914 - 1920)");

        printScenario(mapper, "saves/RUN-RAMS-S-II--PAX-AEGYPTIACA---B-TISSEURS-MONUMENTAUX---1279--2005/history.json",
                "RAMSES II (-1279 a -1269)");
    }

    private void printScenario(ObjectMapper mapper, String path, String title) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("File not found: " + path);
            return;
        }
        try {
            JsonNode root = mapper.readTree(file);
            System.out.println("\n=======================================================");
            System.out.println("📊 TRAJECTOIRE COMPLETE : " + title);
            System.out.println("Fichier source: " + file.getAbsolutePath());
            System.out.println("=======================================================");
            System.out.printf("%-6s %-6s %-14s %-12s %-12s %-8s %-10s %-10s %-8s%n",
                    "Annee", "Mois", "Population", "Nourriture", "Capital($)", "Tech", "Conflit", "Bonheur", "Esperance");

            int lastYear = -999999;
            for (JsonNode node : root) {
                int year = node.path("year").asInt();
                int month = node.path("month").asInt();

                // Print annual snapshots (month 0) or final tick
                if (month == 0 || year != lastYear) {
                    long pop = node.path("totalPopulation").asLong();
                    double food = node.path("totalFood").asDouble();
                    double wealth = node.path("totalWealth").asDouble();
                    double tech = node.path("avgTechnology").asDouble();
                    double conflict = node.path("conflictLevel").asDouble();
                    double happiness = node.path("happinessIndex").asDouble();
                    double lifespan = node.path("avgLifespan").asDouble();

                    System.out.printf("%-6d %-6d %-14d %-12.2e %-12.2e %-8.2f %-10.2f %-10.2f %-8.1f%n",
                            year, month, pop, food, wealth, tech, conflict, happiness, lifespan);
                    lastYear = year;
                }
            }
            // Also print very last element
            if (root.size() > 0) {
                JsonNode lastNode = root.get(root.size() - 1);
                int year = lastNode.path("year").asInt();
                int month = lastNode.path("month").asInt();
                if (month != 0) {
                    long pop = lastNode.path("totalPopulation").asLong();
                    double food = lastNode.path("totalFood").asDouble();
                    double wealth = lastNode.path("totalWealth").asDouble();
                    double tech = lastNode.path("avgTechnology").asDouble();
                    double conflict = lastNode.path("conflictLevel").asDouble();
                    double happiness = lastNode.path("happinessIndex").asDouble();
                    double lifespan = lastNode.path("avgLifespan").asDouble();

                    System.out.printf("%-6d %-6d %-14d %-12.2e %-12.2e %-8.2f %-10.2f %-10.2f %-8.1f (FINAL)%n",
                            year, month, pop, food, wealth, tech, conflict, happiness, lifespan);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
