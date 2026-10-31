package org.ether.society.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ether.society.persistence.EtherJsonUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class InspectScenarioNamesTest {

    private static final Map<String, String> ENGLISH_NAMES = new LinkedHashMap<>();

    static {
        ENGLISH_NAMES.put("archipelago_seasteading_2055", "Archipelago: Oceanic Confederation & Autonomous Seasteading (2055)");
        ENGLISH_NAMES.put("boreas_subglacial_2120", "Glacial World Boreas: Geothermal Havens & Subglacial Cities (2120)");
        ENGLISH_NAMES.put("pax_romana_augustus_0", "Pax Romana & Augustan Stability (0 -> 50 CE): Biophysical Continuity");
        ENGLISH_NAMES.put("roman_empire", "Roman Empire & Pax Romana (0 CE)");
        ENGLISH_NAMES.put("song_dynasty", "Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)");
        ENGLISH_NAMES.put("mongol_conquest_1206", "The Mongol Empire & Eurasian Rupture (1206 CE)");
        ENGLISH_NAMES.put("mali_empire", "Mali Empire & Trans-Saharan Trade (1324 CE)");
        ENGLISH_NAMES.put("black_death_1347", "The Black Death & Post-Feudal Economic Inversion (1347 CE)");
        ENGLISH_NAMES.put("americas_1491", "Pre-Columbian Americas: Tawantinsuyu & Anahuac (1491 CE)");
        ENGLISH_NAMES.put("columbian_contact", "European Arrival in the Americas & Microbial Shock (1492 CE)");
        ENGLISH_NAMES.put("tokugawa_japan", "Tokugawa Japan & Sakoku Isolation (1639 CE)");
        ENGLISH_NAMES.put("industrial_revolution", "Industrial Revolution & Fossil Energy Transition (1780 CE)");
        ENGLISH_NAMES.put("world_wars_totalitarian_1914", "World Wars, Totalitarian Rupture & Nuclear Era (1914 CE)");
        ENGLISH_NAMES.put("great_acceleration_1950", "The Great Acceleration & Anthropocene (1950 CE)");
        ENGLISH_NAMES.put("antonine_plague_165", "Antonine Plague & Imperial Crisis under Marcus Aurelius (165-180 CE)");
        ENGLISH_NAMES.put("earth_2026_future", "Ether Prospective Projection (2026-2100): Standard Biophysical Dynamics");
        ENGLISH_NAMES.put("ssp5_85", "Business As Usual: Fossil Fuel Reliance & Warming (SSP5-8.5)");
        ENGLISH_NAMES.put("nuclear_winter_2035", "Nuclear Winter & Stratospheric Solar Dimming (2035)");
        ENGLISH_NAMES.put("singularity_2045", "Technological Singularity, ASI & D-T Fusion (2045)");
        ENGLISH_NAMES.put("peak_phosphate_2050", "Mineral Phosphate Cliff & N-P-K Soil Crisis (2050)");
        ENGLISH_NAMES.put("supervolcano_2060", "Supervolcano Cataclysm (2060)");
        ENGLISH_NAMES.put("late_antique_ice_age", "Late Antique Little Ice Age & Justinian Plague (536 CE)");
        ENGLISH_NAMES.put("islamic_expansion_632", "Islamic Expansion & Caliphal Commercial Revolution (632 CE)");
        ENGLISH_NAMES.put("out_of_africa", "Out of Africa & Homo Sapiens Expansion (-100,000 BP)");
        ENGLISH_NAMES.put("benchmark_standard", "Benchmark Standard Planetary Scenario");
        ENGLISH_NAMES.put("early_iron_age", "Early Iron Age & Ancient Mediterranean (-1000 BCE)");
        ENGLISH_NAMES.put("younger_dryas", "Younger Dryas & Natufian Climatic Shock (-10900 BP)");
        ENGLISH_NAMES.put("bronze_age_collapse_1200bc", "Late Bronze Age Collapse & Sea Peoples (-1200 BCE)");
        ENGLISH_NAMES.put("ramesses_ii", "Ramesses II, Pax Aegyptiaca & Monumental Builders (-1279 BCE)");
        ENGLISH_NAMES.put("mesoamerica", "Mesoamerican Civilizations (Olmecs & Mayas) (-1500 BCE)");
        ENGLISH_NAMES.put("assyrian_empire", "Middle Bronze Age & Mesopotamia (-1900 BCE)");
        ENGLISH_NAMES.put("lgm_solutrean", "Last Glacial Maximum & Solutrean (-20,000 BP)");
        ENGLISH_NAMES.put("beringia", "Beringia & Peopling of the Americas (-25,000 BP)");
        ENGLISH_NAMES.put("ancient_egypt", "Ancient Egypt & Nile Valley (-3000 BCE)");
        ENGLISH_NAMES.put("maurya_empire", "Maurya Empire & Indus-Ganges Civilizations (-300 BCE)");
        ENGLISH_NAMES.put("alexander_hellenistic_334bc", "Hellenistic Expansion & Shock of Alexander the Great (-334 BCE)");
        ENGLISH_NAMES.put("sahul", "Sahul & First Peopling of Australia (-50,000 BP)");
        ENGLISH_NAMES.put("green_sahara", "Green Sahara & African Humid Period (-6000 BP)");
        ENGLISH_NAMES.put("toba_cataclysm_74k", "Toba Supervolcano Cataclysm & Demographic Bottleneck (-74,000 BP)");
        ENGLISH_NAMES.put("fertile_crescent", "Fertile Crescent & Neolithic Agriculture (-8000 BCE)");
        ENGLISH_NAMES.put("eyeball_world_twilight_2220", "Tidally Locked World: Twilight Ring & Terminator (2220)");
        ENGLISH_NAMES.put("mars_colony_2050", "Martian Colonization: Chryse Domes & Jezero Crater (2050)");
        ENGLISH_NAMES.put("mercury_caloris_forge_2120", "Solar Forge of Mercury & Caloris Basin (2120)");
        ENGLISH_NAMES.put("moon_shackleton_2035", "Shackleton Lunar Base & Marius Hills Lava Tubes (2035)");
        ENGLISH_NAMES.put("titan_cryo_methane_2150", "Titan Cryogenic Colony & Kraken Mare Hydrocarbons (2150)");
        ENGLISH_NAMES.put("oceania_aquapolis_2100", "Ocean World: Aquapolis & Abyssal Cities of Oceania (2100)");
        ENGLISH_NAMES.put("super_earth_gaia_2200", "Interstellar Ark: First Settlement of Gaia Prime (2200)");
        ENGLISH_NAMES.put("venus_cloud_cities_2080", "Venusian Floating Cities: High Altitude Aerostats (2080)");
    }

    @Test
    public void inspectAndTranslateScenarioNames() throws Exception {
        List<Path> dirs = List.of(
                Paths.get("data/presets/scenarios"),
                Paths.get("src/main/resources/data/presets/scenarios")
        );

        ObjectMapper mapper = EtherJsonUtil.getMapper();

        for (Path dir : dirs) {
            if (!Files.exists(dir)) continue;
            File[] files = dir.toFile().listFiles((d, name) -> name.endsWith(".json"));
            if (files == null) continue;

            for (File f : files) {
                JsonNode root = mapper.readTree(f);
                if (!(root instanceof ObjectNode obj)) continue;

                String key = obj.has("presetKey") ? obj.get("presetKey").asText() : "";
                if (ENGLISH_NAMES.containsKey(key)) {
                    String enName = ENGLISH_NAMES.get(key);
                    obj.put("name", enName);
                    obj.put("displayName", enName);
                    // Also clean description header if it starts with SCÉNARIO HISTORIQUE
                    if (obj.has("description")) {
                        String desc = obj.get("description").asText();
                        desc = desc.replace("SCÉNARIO HISTORIQUE", "HISTORICAL SCENARIO")
                                   .replace("CONTEXTE HISTORIQUE & PHYSIQUE", "HISTORICAL & BIOPHYSICAL CONTEXT")
                                   .replace("THÉORIE DE L'INFLUENCE DES GRANDS HOMMES & OBSERVABLES", "GREAT MAN THEORY & OBSERVABLE STATE DYNAMICS")
                                   .replace("av. J.-C.", "BCE");
                        obj.put("description", desc);
                    }
                    mapper.writerWithDefaultPrettyPrinter().writeValue(f, obj);
                    System.out.println("Updated " + f.getName() + " -> " + enName);
                }
            }
        }
    }
}
