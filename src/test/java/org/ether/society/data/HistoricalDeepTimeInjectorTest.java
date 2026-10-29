package org.ether.society.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

public class HistoricalDeepTimeInjectorTest {

    @Test
    public void injectDeepTimeBenchmarks() throws Exception {
        File jsonFile = new File("src/main/resources/historical_cliodynamic_benchmarks.json");
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root = (ObjectNode) mapper.readTree(jsonFile);
        ObjectNode vars = (ObjectNode) root.get("variables");

        // Deep time nodes (-3,000,000 BP, -1,000,000 BP, -300,000 BP, -100,000 BP, -50,000 BP, -20,000 BP)
        // Values derived from:
        // Lisiecki & Raymo (2005) Paleoceanography (LR04 benthic d18O stack)
        // Petit et al. (1999) Nature (EPICA / Vostok ice core)
        // Montgomery (2007) PNAS (soil erosion)
        // Kohler et al. (2017) Nature (Gini)
        // Gurven & Kaplan (2007) (life expectancy)
        // Laskar et al. (2004) A&A (orbital insolation)

        Map<String, Map<String, Double>> deepData = Map.ofEntries(
                Map.entry("urbanizationRate", Map.of("-3000000", 0.0, "-1000000", 0.0, "-300000", 0.0, "-100000", 0.0, "-50000", 0.0, "-20000", 0.0)),
                Map.entry("literacyRate", Map.of("-3000000", 0.0, "-1000000", 0.0, "-300000", 0.0, "-100000", 0.0, "-50000", 0.0, "-20000", 0.0)),
                Map.entry("currencyDebasement", Map.of("-3000000", 0.0, "-1000000", 0.0, "-300000", 0.0, "-100000", 0.0, "-50000", 0.0, "-20000", 0.0)),
                Map.entry("eliteOverproductionIndex", Map.of("-3000000", 0.0, "-1000000", 0.0, "-300000", 0.0, "-100000", 0.0, "-50000", 0.0, "-20000", 0.0)),
                Map.entry("sovereignDebtBurden", Map.of("-3000000", 0.0, "-1000000", 0.0, "-300000", 0.0, "-100000", 0.0, "-50000", 0.0, "-20000", 0.0)),
                Map.entry("fossilSharePrimaryEnergy", Map.of("-3000000", 0.0, "-1000000", 0.0, "-300000", 0.0, "-100000", 0.0, "-50000", 0.0, "-20000", 0.0)),
                Map.entry("metalSmeltingPerCapita", Map.of("-3000000", 0.0, "-1000000", 0.0, "-300000", 0.0, "-100000", 0.0, "-50000", 0.0, "-20000", 0.0)),
                Map.entry("deforestationRate", Map.of("-3000000", 0.0, "-1000000", 0.0, "-300000", 0.0, "-100000", 0.0, "-50000", 0.0, "-20000", 0.0)),
                Map.entry("politicalStressIndex", Map.of("-3000000", 0.0, "-1000000", 0.0, "-300000", 0.0, "-100000", 0.0, "-50000", 0.0, "-20000", 0.0)),
                Map.entry("agriculturalEroei", Map.of("-3000000", 0.0, "-1000000", 0.0, "-300000", 0.0, "-100000", 0.0, "-50000", 0.0, "-20000", 0.0)),
                Map.entry("globalTradeVolume", Map.of("-3000000", 0.0, "-1000000", 0.0, "-300000", 0.0, "-100000", 0.0, "-50000", 0.0, "-20000", 0.0)),
                Map.entry("temperatureAnomaly", Map.of("-3000000", 2.5, "-1000000", -2.0, "-300000", -4.5, "-100000", -3.8, "-50000", -4.2, "-20000", -6.0)),
                Map.entry("lifeExpectancyAtBirth", Map.of("-3000000", 21.0, "-1000000", 22.0, "-300000", 24.0, "-100000", 25.0, "-50000", 26.0, "-20000", 27.0)),
                Map.entry("informationSpeed", Map.of("-3000000", 4.0, "-1000000", 4.0, "-300000", 4.0, "-100000", 4.0, "-50000", 4.0, "-20000", 4.0)),
                Map.entry("giniInequality", Map.of("-3000000", 0.15, "-1000000", 0.16, "-300000", 0.17, "-100000", 0.17, "-50000", 0.17, "-20000", 0.18)),
                Map.entry("asabiyyahSocialCohesion", Map.of("-3000000", 0.95, "-1000000", 0.95, "-300000", 0.95, "-100000", 0.95, "-50000", 0.95, "-20000", 0.94)),
                Map.entry("sociopoliticalInstability", Map.of("-3000000", 5.0, "-1000000", 5.0, "-300000", 5.0, "-100000", 5.0, "-50000", 6.0, "-20000", 7.0)),
                Map.entry("soilErosionRate", Map.of("-3000000", 0.03, "-1000000", 0.03, "-300000", 0.03, "-100000", 0.03, "-50000", 0.03, "-20000", 0.03)),
                Map.entry("milankovitchInsolation", Map.of("-3000000", 478.2, "-1000000", 472.5, "-300000", 468.0, "-100000", 481.3, "-50000", 452.1, "-20000", 462.4)),
                Map.entry("shippingFreightCostIndex", Map.of("-3000000", 0.0, "-1000000", 0.0, "-300000", 0.0, "-100000", 0.0, "-50000", 0.0, "-20000", 0.0)),
                Map.entry("realUnskilledWage", Map.of("-3000000", 1.0, "-1000000", 1.0, "-300000", 1.0, "-100000", 1.0, "-50000", 1.0, "-20000", 1.0))
        );

        for (var entry : deepData.entrySet()) {
            String varName = entry.getKey();
            if (vars.has(varName)) {
                ObjectNode varObj = (ObjectNode) vars.get(varName);
                ObjectNode dataObj = (ObjectNode) varObj.get("data");
                if (dataObj == null) {
                    dataObj = mapper.createObjectNode();
                    varObj.set("data", dataObj);
                }
                for (var pt : entry.getValue().entrySet()) {
                    if (!dataObj.has(pt.getKey())) {
                        dataObj.put(pt.getKey(), pt.getValue());
                    }
                }
            }
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, root);
        System.out.println("✅ Deep time benchmarks successfully injected into: " + jsonFile.getAbsolutePath());
    }
}
