/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ether.society.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Discovers and parses ethnolinguistic cultural registries and entities across historical epochs
 * from the cartographic tensor repository or procedural heuristics.
 */
public final class CulturalEpochRegistryLoader {

    private static final Logger logger = LoggerFactory.getLogger(CulturalEpochRegistryLoader.class);
    private static final ObjectMapper MAPPER = org.ether.society.persistence.EtherJsonUtil.getMapper();

    private CulturalEpochRegistryLoader() {
        // Utility class
    }

    public static List<Long> discoverAllEpochs() {
        List<Long> epochs = new ArrayList<>();
        Path earthDir = Paths.get("data", "maps", "ether", "earth");
        if (Files.exists(earthDir) && Files.isDirectory(earthDir)) {
            try (var stream = Files.list(earthDir)) {
                stream.filter(Files::isDirectory)
                        .forEach(p -> {
                            String name = p.getFileName().toString();
                            try {
                                long yr = Long.parseLong(name);
                                epochs.add(yr);
                            } catch (NumberFormatException ignored) {}
                        });
            } catch (Exception ex) {
                logger.warn("Failed to dynamically list Earth map epochs: {}", ex.getMessage());
            }
        }
        if (epochs.isEmpty()) {
            long[] defaults = {-100000L, -74000L, -50000L, -25000L, -20000L, -10900L, -10000L, -8000L, -6000L, -3000L, -1900L, -1000L, -500L, 0L, 500L, 1000L, 1500L, 1800L, 1900L, 2026L};
            for (long d : defaults) epochs.add(d);
        } else {
            Collections.sort(epochs);
        }
        return epochs;
    }

    public static long closestSupportedEpoch(long yr) {
        List<Long> supported = discoverAllEpochs();
        if (supported.isEmpty()) return -100000L;
        long best = supported.get(0);
        long minDiff = Math.abs(yr - best);
        for (long s : supported) {
            long d = Math.abs(yr - s);
            if (d < minDiff) {
                minDiff = d;
                best = s;
            }
        }
        return best;
    }

    public static String formatEpochName(long yr) {
        if (yr == -100000L) return I18n.getOrDefault("cultural.epoch.eemian", "–100 000 BP (Sortie d'Afrique / Éémien)");
        if (yr == -74000L)  return I18n.getOrDefault("cultural.epoch.toba", "–74 000 BP (Goulot d'Étranglement de Toba)");
        if (yr == -50000L)  return I18n.getOrDefault("cultural.epoch.sahul", "–50 000 BP (Peuplement Maritime du Sahul)");
        if (yr == -25000L)  return I18n.getOrDefault("cultural.epoch.beringia", "–25 000 BP (Pause Béringienne & Gravettien)");
        if (yr == -20000L)  return I18n.getOrDefault("cultural.epoch.lgm", "–20 000 BP (Dernier Maximum Glaciaire & Solutréen)");
        if (yr == -10900L)  return I18n.getOrDefault("cultural.epoch.dryas", "–10 900 BP (Dryas Récent & Proto-Néolithique)");
        if (yr == -10000L)  return I18n.getOrDefault("cultural.epoch.holocene", "–10 000 BP (Aube de l'Holocène / Göbekli Tepe)");
        if (yr == -8000L)   return I18n.getOrDefault("cultural.epoch.neolithic", "–8 000 BP (Néolithique / Çatalhöyük & Jéricho)");
        if (yr == -6000L)   return I18n.getOrDefault("cultural.epoch.uruk", "–6 000 BP (Proto-Urbain / Mésopotamie & Yangshao)");
        if (yr == -3000L)   return I18n.getOrDefault("cultural.epoch.earlybronze", "–3 000 AEC (Premier Âge du Bronze / Dynasties Archaïques)");
        if (yr == -1900L)   return I18n.getOrDefault("cultural.epoch.bronze", "–1 900 AEC (Âge du Bronze Moyen / Babylone & Shang)");
        if (yr == -1000L)   return I18n.getOrDefault("cultural.epoch.iron", "–1 000 AEC (Premier Âge du Fer / Phéniciens & Zhou)");
        if (yr == -500L)    return I18n.getOrDefault("cultural.epoch.classical", "–500 AEC (Époque Classique / Athènes, Confucius & Magadha)");
        if (yr == 0L)       return "0 CE / AD (Pax Romana & Dynastie Han)";
        if (yr == 500L)     return "+500 CE (Antiquité Tardive / Justinien & Gupta)";
        if (yr == 1000L)    return "+1 000 CE (Moyen Âge / Song & Califats)";
        if (yr == 1500L)    return "+1 500 CE (Grandes Découvertes & Renaissance)";
        if (yr == 1800L)    return "+1 800 CE (Révolution Industrielle)";
        if (yr == 1900L)    return "+1 900 CE (Ère Moderne / Belle Époque)";
        if (yr == 2026L)    return "+2 026 CE (Anthropocène Contemporain)";
        if (yr < 0) {
            long absYr = Math.abs(yr);
            if (absYr >= 10000) {
                return String.format("–%,d BP", absYr).replace(',', ' ');
            } else {
                return String.format("–%,d AEC", absYr).replace(',', ' ');
            }
        }
        return String.format("+%,d CE", yr).replace(',', ' ');
    }

    public static List<CulturalAffinityMatrixDialog.CulturalEntity> loadEpochRegistry(long epoch) {
        List<CulturalAffinityMatrixDialog.CulturalEntity> activeEntities = new ArrayList<>();
        Path p = Paths.get("data", "maps", "ether", "earth", String.valueOf(epoch), "cultural_registry.json");
        if (!Files.exists(p)) {
            long closest = closestSupportedEpoch(epoch);
            p = Paths.get("data", "maps", "ether", "earth", String.valueOf(closest), "cultural_registry.json");
        }

        if (Files.exists(p)) {
            try {
                JsonNode root = MAPPER.readTree(p.toFile());
                JsonNode entitiesNode = root.get("entities");
                if (entitiesNode != null && entitiesNode.isArray()) {
                    for (JsonNode eNode : entitiesNode) {
                        String id = eNode.path("id").asText("unknown");
                        String colorHex = eNode.path("colorHex").asText("#FFFFFF");
                        int[] colorRgb = new int[]{255, 255, 255};
                        if (eNode.has("colorRgb") && eNode.get("colorRgb").isArray()) {
                            colorRgb[0] = eNode.get("colorRgb").get(0).asInt(255);
                            colorRgb[1] = eNode.get("colorRgb").get(1).asInt(255);
                            colorRgb[2] = eNode.get("colorRgb").get(2).asInt(255);
                        }
                        double[] traits = new double[4];
                        if (eNode.has("traits") && eNode.get("traits").isArray()) {
                            for (int i = 0; i < 4 && i < eNode.get("traits").size(); i++) {
                                traits[i] = eNode.get("traits").get(i).asDouble(0.0);
                            }
                        }
                        JsonNode nameNode = eNode.path("name");
                        String nameEn = nameNode.path("en").asText(id);
                        String nameFr = nameNode.path("fr").asText(nameEn);
                        String nameDe = nameNode.path("de").asText(nameEn);
                        String nameEs = nameNode.path("es").asText(nameEn);
                        String nameZh = nameNode.path("zh").asText(nameEn);
                        String kinship = eNode.path("kinshipType").asText("Bilateral Foragers");
                        String lithic = eNode.path("lithicTechnocomplex").asText("Middle Paleolithic");

                        activeEntities.add(new CulturalAffinityMatrixDialog.CulturalEntity(id, colorHex, colorRgb, traits, nameEn, nameFr, nameDe, nameEs, nameZh, kinship, lithic));
                    }
                }
            } catch (Exception ex) {
                logger.warn("Failed to parse cultural_registry.json for epoch {}: {}", epoch, ex.getMessage());
            }
        }

        if (activeEntities.isEmpty()) {
            buildProceduralEntitiesFallback(epoch, activeEntities);
        } else if (activeEntities.size() > 1) {
            boolean uniformTraits = true;
            double[] t0 = activeEntities.get(0).traits();
            for (int k = 1; k < activeEntities.size(); k++) {
                double[] tk = activeEntities.get(k).traits();
                if (tk == null || t0 == null || tk.length != t0.length) { uniformTraits = false; break; }
                for (int d = 0; d < t0.length; d++) {
                    if (Math.abs(tk[d] - t0[d]) > 0.001) {
                        uniformTraits = false;
                        break;
                    }
                }
            }
            if (uniformTraits) {
                generateDistinctCulturalTraits(activeEntities, epoch);
            }
        }
        return activeEntities;
    }

    public static void generateDistinctCulturalTraits(List<CulturalAffinityMatrixDialog.CulturalEntity> entities, long currentEpoch) {
        if (entities == null || entities.isEmpty()) return;
        List<CulturalAffinityMatrixDialog.CulturalEntity> updated = new ArrayList<>();
        int n = entities.size();
        for (int idx = 0; idx < n; idx++) {
            CulturalAffinityMatrixDialog.CulturalEntity orig = entities.get(idx);
            double[] traits = org.ether.society.data.CliopatriaPolityVectorReader.computePolityTraits(orig.nameEn(), orig.id(), currentEpoch);
            updated.add(new CulturalAffinityMatrixDialog.CulturalEntity(
                    orig.id(), orig.colorHex(), orig.colorRgb(), traits,
                    orig.nameEn(), orig.nameFr(), orig.nameDe(), orig.nameEs(), orig.nameZh(),
                    orig.kinshipType(), orig.lithicTechnocomplex()
            ));
        }
        entities.clear();
        entities.addAll(updated);
    }

    public static void buildProceduralEntitiesFallback(long epoch, List<CulturalAffinityMatrixDialog.CulturalEntity> activeEntities) {
        activeEntities.clear();
        String[] colors = {"#E67E22", "#D35400", "#F39C12", "#2980B9", "#1F4788", "#27AE60", "#8E44AD", "#E11D48", "#0EA5E9"};
        int[][] rgb = {
            {230, 126, 34}, {211, 84, 0}, {243, 156, 18}, {41, 128, 185}, {31, 71, 136}, {39, 174, 96}, {142, 68, 173}, {225, 29, 72}, {14, 165, 233}
        };
        String[] ids = {"clade_alpha", "clade_beta", "clade_gamma", "clade_delta", "clade_epsilon", "clade_zeta", "clade_eta", "clade_theta", "clade_iota"};
        String[] namesEn = {"Equatorial Basin Clade", "Rift & Valley Lineage", "Maritime Coastal Foragers", "Highland Mountain Tribe", "Savanna Nomad Confederacy", "Lacustrine Forest Dwellers", "Arid Steppe Clan", "Alluvial Delta Society", "Tundra Boreal Band"};
        String[] namesFr = {"Clade du Bassin Équatorial", "Lignée du Rift & Vallées", "Chasseurs-Cueilleurs Côtiers", "Tribu des Hauts Plateaux", "Confédération Nomade des Savanes", "Société Lacustre des Forêts", "Clan des Steppes Arides", "Société des Deltas Alluviaux", "Bande Boréale de Toundra"};
        String[] namesDe = {"Äquatoriales Becken-Klade", "Grabenbruch-Linie", "Küstensammler-Gemeinschaft", "Hochland-Bergstamm", "Savannen-Nomadenbund", "See- und Waldbewohner", "Arider Steppen-Klan", "Alluviale Delta-Gemeinschaft", "Boreale Tundragruppe"};
        String[] namesEs = {"Clado de la Cuenca Ecuatorial", "Linaje del Rift y Valles", "Recolectores Costeros Marítimos", "Tribu de las Altas Montañas", "Confederación Nómada de la Sabana", "Habitantes Lacustres del Bosque", "Clan de la Estepa Árida", "Sociedad del Delta Aluvial", "Banda Boreal de Tundra"};
        String[] namesZh = {"赤道盆地演化支", "大裂谷与山谷世系", "海洋沿海采集者", "高山山地部落", "稀树草原游牧联盟", "湖泊森林居民", "干旱草原部落", "冲积三角洲社会", "苔原北方群体"};
        double[][] traits = {
            {0.10, 0.15, 0.20, 0.40},
            {0.18, 0.22, 0.28, 0.48},
            {0.30, 0.35, 0.50, 0.60},
            {0.75, 0.68, 0.55, 0.20},
            {0.82, 0.74, 0.62, 0.22},
            {0.50, 0.45, 0.40, 0.30},
            {0.62, 0.58, 0.48, 0.15},
            {0.40, 0.60, 0.70, 0.80},
            {0.88, 0.30, 0.25, 0.10}
        };

        for (int i = 0; i < ids.length; i++) {
            activeEntities.add(new CulturalAffinityMatrixDialog.CulturalEntity(
                ids[i], colors[i], rgb[i], traits[i],
                namesEn[i], namesFr[i], namesDe[i], namesEs[i], namesZh[i],
                "Bilateral Exogamous Bands", "Mode 3 Technocomplex"
            ));
        }
    }
}
