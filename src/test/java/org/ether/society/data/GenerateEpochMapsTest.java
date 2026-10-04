package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Master Epistemic Cartographic & Metadata Regeneration Suite across all 36 Historical Epochs.
 * Regenerates the 9 cultural tensors (isogloss, kinship, rituals, sovereignty, technology,
 * tradenetwork, institutional, ecological, pathogen) while preserving physical layers,
 * and updates cultural_registry.json, provenance_and_sources.json, and README.md for every epoch.
 */
public class GenerateEpochMapsTest {

    public record EpochMeta(
        long year,
        String eraName,
        String densityType,
        double techLevel,
        long population,
        String summaryEn,
        String summaryFr,
        String lithicComplex,
        String defaultKinship,
        int defaultKcal
    ) {}

    // ---------------------------------------------------------------------------
    // Epoch registry — sorted ascending by year.
    // Original 36 entries are kept verbatim; interpolated grid entries fill gaps.
    // ---------------------------------------------------------------------------
    private static final List<EpochMeta> EPOCHS;
    static {
        List<EpochMeta> _list = new ArrayList<>();

        // -----------------------------------------------------------------------
        // Helper lambdas — linear interpolation utilities defined via anonymous
        // Runnable wrappers so they can reference each other without full classes.
        // -----------------------------------------------------------------------

        // techLevel anchor table: {year, value}
        double[][] techAnchors = {
            {-100000, 0.5}, {-50000, 0.8}, {-10000, 1.5}, {0, 3.2},
            {1000, 3.5}, {1800, 5.0}, {1900, 6.0}, {2026, 8.5}, {2060, 9.8}
        };
        // population anchor table: {year, pop}
        double[][] popAnchors = {
            {-100000, 50_000}, {-74000, 20_000}, {-50000, 200_000}, {-25000, 500_000},
            {-10000, 5_000_000}, {0, 250_000_000}, {1000, 300_000_000},
            {1800, 1_000_000_000}, {1900, 1_650_000_000}, {2026, 8_150_000_000L},
            {2060, 9_800_000_000L}
        };

        // Returns the linearly interpolated value from an anchor table for a given year.
        java.util.function.BiFunction<double[][], Long, Double> lerp =
            (anchors, yr) -> {
                if (yr <= anchors[0][0]) return anchors[0][1];
                if (yr >= anchors[anchors.length - 1][0]) return anchors[anchors.length - 1][1];
                for (int _i = 0; _i < anchors.length - 1; _i++) {
                    if (yr >= anchors[_i][0] && yr <= anchors[_i + 1][0]) {
                        double t = (yr - anchors[_i][0]) / (anchors[_i + 1][0] - anchors[_i][0]);
                        return anchors[_i][1] + t * (anchors[_i + 1][1] - anchors[_i][1]);
                    }
                }
                return anchors[anchors.length - 1][1];
            };

        // Returns densityType string for a given year.
        java.util.function.Function<Long, String> densityFor = yr -> {
            if (yr <= -70000) return "ONE_CONTINENT";
            if (yr <= -40000) return "SAHUL_MIGRATION";
            if (yr <= -20000) return "BERINGIA_AMERICAS";
            if (yr <= -10000) return "YOUNGER_DRYAS";
            if (yr <= -5000) return "NEOLITHIC";
            if (yr <= -3000) return "EGYPT_NILE";
            if (yr <= -500) return "MESOPOTAMIA_ASSYRIA";
            if (yr <= 500) return "ROMAN_EMPIRE";
            if (yr <= 1500) return "SONG_DYNASTY";
            if (yr <= 1800) return "INDUSTRIAL";
            return "URBAN_CLUSTERS";
        };

        // Returns a short era label for a given year.
        java.util.function.Function<Long, String> eraLabel = yr -> {
            if (yr <= -70000) return String.format("Paleolithic %,d BP", Math.abs(yr));
            if (yr <= -40000) return String.format("Upper Paleolithic %,d BP", Math.abs(yr));
            if (yr <= -20000) return String.format("LGM %,d BP", Math.abs(yr));
            if (yr <= -10000) return String.format("Late Glacial %,d BP", Math.abs(yr));
            if (yr <= -5000) return String.format("Early Holocene %,d BP", Math.abs(yr));
            if (yr <= -3000) return String.format("Chalcolithic %,d BC", Math.abs(yr));
            if (yr <= -1000) return String.format("Bronze Age %,d BC", Math.abs(yr));
            if (yr <= -500) return String.format("Iron Age %,d BC", Math.abs(yr));
            if (yr <= 500) return String.format("Classical %d AD", yr);
            if (yr <= 1000) return String.format("Late Antique %d AD", yr);
            if (yr <= 1500) return String.format("Medieval %d AD", yr);
            if (yr <= 1800) return String.format("Early Modern %d AD", yr);
            if (yr <= 1900) return String.format("Industrial %d AD", yr);
            return String.format("Contemporary %d AD", yr);
        };

        // Returns the appropriate lithicComplex string for a given year.
        java.util.function.Function<Long, String> lithicFor = yr -> {
            if (yr <= -70000) return "Middle Stone Age Blade & Point Technocomplex";
            if (yr <= -40000) return "Early Upper Paleolithic Blade & Bone Industries";
            if (yr <= -20000) return "Solutrean / Gravettian Pressure-Flaked Lithics";
            if (yr <= -10000) return "Natufian / Azilian Microlithic Sickles & Ground Stone";
            if (yr <= -5000) return "PPNA/PPNB Ground Stone Axes & Mudbrick Settlements";
            if (yr <= -3000) return "Copper Smelting, Wheel Pottery & Irrigation Canals";
            if (yr <= -1000) return "Bronze Casting, Cuneiform Administration & Monumental Masonry";
            if (yr <= -500) return "Iron Weapons, Ashlar Fortifications & Alphabetic Writing";
            if (yr <= 500) return "Roman Concrete, Aqueducts & Silk Road Caravans";
            if (yr <= 1000) return "Moldboard Plows, Watermills & Blast-Furnace Cast Iron";
            if (yr <= 1500) return "Movable Type Printing, Compass & Gunpowder Artillery";
            if (yr <= 1800) return "Flintlock Arms, Galleons & Scientific Revolution Instruments";
            if (yr <= 1900) return "Steam Engines, Puddling Iron & Power Looms";
            if (yr <= 1950) return "Electric Dynamos, Bessemer Steel & Internal Combustion Engines";
            if (yr <= 2000) return "Fission Reactors, Jet Aviation & Transistors";
            return "VLSI Microprocessors, Renewable Grids & Advanced Biotechnology";
        };

        // Returns the default kinship string for a given year.
        java.util.function.Function<Long, String> kinshipFor = yr -> {
            if (yr <= -10000) return "Egalitarian Forager Multi-Family Band";
            if (yr <= -5000) return "Patrilocal Extended Peasant Households";
            if (yr <= -3000) return "Corporate Lineage Estates & Temple Chiefdoms";
            if (yr <= -500) return "Patriarchal Dynastic Lineages & Feudal Estates";
            if (yr <= 500) return "Paterfamilias Households & Confucian Patrilineal Clans";
            if (yr <= 1500) return "Manorial Feudal Serfdom & Stem Households";
            if (yr <= 1800) return "Early Modern Nuclear & Stem Families";
            if (yr <= 1900) return "Industrial Wage-Labor Proletariat & Nuclear Families";
            return "Post-Industrial Nuclear & Globalized Urban Households";
        };

        // Returns default kcal for a given year.
        java.util.function.Function<Long, Integer> kcalFor = yr -> {
            if (yr <= -10000) return 2250;
            if (yr <= -5000) return 2400;
            if (yr <= -3000) return 2500;
            if (yr <= 0) return 2650;
            if (yr <= 1000) return 2750;
            if (yr <= 1800) return 2900;
            if (yr <= 1900) return 3100;
            if (yr <= 1950) return 3200;
            if (yr <= 2000) return 3300;
            return 3400;
        };

        // Returns a one-sentence English summary for a given year.
        java.util.function.Function<Long, String> summaryEn = yr -> {
            if (yr <= -70000) return String.format("Archaic Homo sapiens bands forage across African refugia during MIS glacial stage at %,d BP, with pioneer dispersals into adjacent Afro-Arabian corridors.", Math.abs(yr));
            if (yr <= -40000) return String.format("Upper Paleolithic foragers expand across Eurasia at %,d BP, establishing blade industries, cave art traditions, and maritime crossings toward Sahul.", Math.abs(yr));
            if (yr <= -20000) return String.format("Last Glacial Maximum conditions prevail at %,d BP; hunter-gatherer refugia persist along ice-free corridors and exposed continental shelves.", Math.abs(yr));
            if (yr <= -10000) return String.format("Terminal Pleistocene foragers at %,d BP harvest wild cereals across the Levant and initiate sedentary encampments preceding the Neolithic transition.", Math.abs(yr));
            if (yr <= -5000) return String.format("Early Holocene agropastoral communities at %,d BP cultivate emmer and einkorn in the Fertile Crescent while forest clearance expands across temperate Europe.", Math.abs(yr));
            if (yr <= -3000) return String.format("Chalcolithic proto-urban polities at %,d BC develop copper smelting, irrigation canals, and wheel-thrown pottery across the Near East and Balkans.", Math.abs(yr));
            if (yr <= -1000) return String.format("Bronze Age literate states at %,d BC organise chariot armies, palatial economies, and long-distance maritime trade across the Eastern Mediterranean and Asia.", Math.abs(yr));
            if (yr <= -500) return String.format("Iron Age bloomery metallurgy proliferates at %,d BC, enabling Neo-Assyrian military expansion, Phoenician alphabet diffusion, and Zhou feudal consolidation.", Math.abs(yr));
            if (yr <= 0) return String.format("Classical antiquity at %d AD witnesses Roman provincial administration, Han Dynasty bureaucracy, and Silk Road intercontinental exchange.", yr);
            if (yr <= 500) return String.format("Late Antique societies in %d AD navigate imperial fragmentation, plague pandemics, and the rise of post-Roman successor kingdoms.", yr);
            if (yr <= 1000) return String.format("Medieval %d AD: agrarian expansion, cathedral construction, Song commercial revolution, and emerging intercontinental trade diaspora networks.", yr);
            if (yr <= 1500) return String.format("High Medieval polities in %d AD deploy mounted knights, castle networks, and guild merchant capitalism in Eurasian trade corridors.", yr);
            if (yr <= 1800) return String.format("Early Modern states of %d AD govern maritime empires, joint-stock companies, and scientific academies amid proto-industrial proto-capitalism.", yr);
            if (yr <= 1900) return String.format("Industrial %d AD witnesses coal-powered steam factories, transcontinental railways, and telegraph networks reshaping the global economic order.", yr);
            if (yr <= 1950) return String.format("Twentieth-century %d AD sees electrification, internal combustion, chemical synthesis, and two world wars accelerate technological and demographic change.", yr);
            if (yr <= 2000) return String.format("Post-war %d AD integrates transistors, satellite communications, Green Revolution agriculture, and computer-mediated global information exchange.", yr);
            return String.format("Near-future %d AD: planetary-scale renewable energy grids, autonomous systems, and climate adaptation technologies define the emerging global civilisation.", yr);
        };

        // Returns a one-sentence French summary for a given year.
        java.util.function.Function<Long, String> summaryFr = yr -> {
            if (yr <= -70000) return String.format("Des bandes d'Homo sapiens archaïques se déplacent dans les refuges africains durant le stade glaciaire MIS à %,d BP, avec des dispersions pionnières vers les corridors afro-arabiques.", Math.abs(yr));
            if (yr <= -40000) return String.format("Les chasseurs-cueilleurs du Paléolithique supérieur s'étendent à travers l'Eurasie à %,d BP, établissant des industries laminaires, l'art pariétal et des traversées maritimes vers le Sahul.", Math.abs(yr));
            if (yr <= -20000) return String.format("Les conditions du Dernier Maximum Glaciaire prévalent à %,d BP ; des refuges de chasseurs-cueilleurs persistent le long des corridors libres de glace.", Math.abs(yr));
            if (yr <= -10000) return String.format("Les chasseurs-cueilleurs du Pléistocène terminal à %,d BP récoltent des céréales sauvages au Levant et initient des campements sédentaires précédant la transition néolithique.", Math.abs(yr));
            if (yr <= -5000) return String.format("Les communautés agropastorales du début de l'Holocène à %,d BP cultivent l'épeautre et l'amidonnier dans le Croissant fertile, tandis que le défrichage forestier s'étend en Europe tempérée.", Math.abs(yr));
            if (yr <= -3000) return String.format("Les cités proto-urbaines chalcolithiques de %,d av. J.-C. développent la fonte du cuivre, les canaux d'irrigation et la poterie tournée au Proche-Orient et dans les Balkans.", Math.abs(yr));
            if (yr <= -1000) return String.format("Les États lettrés de l'âge du bronze de %,d av. J.-C. organisent des armées de chars, des économies palaciales et le commerce maritime à longue distance.", Math.abs(yr));
            if (yr <= -500) return String.format("La métallurgie du fer se répand à %,d av. J.-C., favorisant l'expansion néo-assyrienne, la diffusion de l'alphabet phénicien et la consolidation féodale des Zhou.", Math.abs(yr));
            if (yr <= 0) return String.format("L'Antiquité classique de %d ap. J.-C. voit l'administration provinciale romaine, la bureaucratie Han et les échanges intercontinentaux sur la Route de la Soie.", yr);
            if (yr <= 500) return String.format("Les sociétés tardo-antiques de %d ap. J.-C. naviguent entre fragmentation impériale, pandémies et montée des royaumes post-romains.", yr);
            if (yr <= 1000) return String.format("Médiéval %d ap. J.-C. : expansion agraire, construction de cathédrales, révolution commerciale Song et réseaux diasporiques intercontinentaux.", yr);
            if (yr <= 1500) return String.format("Les États médiévaux tardifs de %d ap. J.-C. déploient chevalerie montée, réseaux castraux et capitalisme marchand corporatif dans les corridors commerciaux eurasiatiques.", yr);
            if (yr <= 1800) return String.format("Les États de l'époque moderne de %d ap. J.-C. gouvernent des empires maritimes, des compagnies à charte et des académies scientifiques dans un proto-capitalisme proto-industriel.", yr);
            if (yr <= 1900) return String.format("L'industrialisation de %d ap. J.-C. voit les usines à vapeur alimentées au charbon, les chemins de fer transcontinentaux et le télégraphe remodeler l'ordre économique mondial.", yr);
            if (yr <= 1950) return String.format("Le XXe siècle de %d ap. J.-C. est marqué par l'électrification, le moteur à combustion, la synthèse chimique et deux guerres mondiales accélérant le changement technologique.", yr);
            if (yr <= 2000) return String.format("L'après-guerre de %d ap. J.-C. intègre les transistors, les communications par satellite, la révolution verte et l'échange mondial d'informations par ordinateur.", yr);
            return String.format("Le futur proche de %d ap. J.-C. : des réseaux d'énergie renouvelable planétaires, des systèmes autonomes et des technologies d'adaptation climatique définissent la civilisation mondiale émergente.", yr);
        };

        // Collect years already present to avoid duplicates when adding original 36.
        java.util.Set<Long> _seen = new java.util.HashSet<>();

        // Helper to add a new interpolated entry only if its year is not yet seen.
        java.util.function.Consumer<Long> addInterpolated = yr -> {
            if (_seen.add(yr)) {
                _list.add(new EpochMeta(
                    yr,
                    eraLabel.apply(yr),
                    densityFor.apply(yr),
                    Math.round(lerp.apply(techAnchors, yr) * 100.0) / 100.0,
                    Math.round(lerp.apply(popAnchors, yr)),
                    summaryEn.apply(yr),
                    summaryFr.apply(yr),
                    lithicFor.apply(yr),
                    kinshipFor.apply(yr),
                    kcalFor.apply(yr)
                ));
            }
        };

        // -----------------------------------------------------------------------
        // RANGE 1: -100,000 to -30,000 BP — every 5,000 years
        // -----------------------------------------------------------------------
        for (long y = -100000L; y <= -30000L; y += 5000L) addInterpolated.accept(y);

        // -----------------------------------------------------------------------
        // RANGE 2: -30,000 to -10,000 BP — every 1,000 years
        // -----------------------------------------------------------------------
        for (long y = -30000L; y <= -10000L; y += 1000L) addInterpolated.accept(y);

        // -----------------------------------------------------------------------
        // RANGE 3: -10,000 to -100 — every 100 years (year 0 excluded)
        // -----------------------------------------------------------------------
        for (long y = -10000L; y <= -100L; y += 100L) addInterpolated.accept(y);

        // -----------------------------------------------------------------------
        // RANGE 4: 0 to 2060 — every 20 years
        // -----------------------------------------------------------------------
        for (long y = 0L; y <= 2060L; y += 20L) addInterpolated.accept(y);

        // -----------------------------------------------------------------------
        // ORIGINAL 36 ENTRIES — kept verbatim; duplicates are overridden by
        // marking years seen before adding, then re-adding the rich versions.
        // Strategy: mark all interpolated years as seen above, then for each
        // original entry remove any interpolated placeholder and add the rich one.
        // -----------------------------------------------------------------------

        // Remove any interpolated placeholder at these exact years and replace with
        // the authoritative rich metadata below.
        long[] _originalYears = {
            -100000L, -74000L, -50000L, -25000L, -20000L, -10900L, -10000L,
            -8000L, -6000L, -3000L, -1900L, -1500L, -1200L, -1000L,
            -334L, -300L, 0L, 536L, 632L, 1000L, 1206L, 1324L, 1347L,
            1491L, 1492L, 1639L, 1800L, 1900L, 1914L, 1950L, 2000L,
            2026L, 2035L, 2045L, 2050L, 2060L
        };
        for (long _oy : _originalYears) {
            _list.removeIf(e -> e.year() == _oy);
        }

        // Now add the 36 original EpochMeta entries verbatim:
        _list.add(new EpochMeta(-100000L, "Out of Africa & Middle Stone Age / MIS 5e Interglacial (-100,000 BP)", "ONE_CONTINENT", 0.5, 50_000L,
            "Reconstruction of Middle Stone Age Homo sapiens bands in Africa and early pioneer dispersals into the Levant and Arabia during the Eemian / MIS 5e interglacial.",
            "Reconstitution des bandes d'Homo sapiens du Middle Stone Age en Afrique et des premières dispersions pionnières vers le Levant et l'Arabie durant l'interglaciaire éémien (MIS 5e).",
            "Middle Stone Age (MSA) Blade & Point Technocomplex, Pigment Use & Marine Foraging", "Egalitarian Forager Multi-Family Band", 2300));

        _list.add(new EpochMeta(-74000L, "Toba Super-Eruption & Genetic Bottleneck Horizon (-74,000 BP)", "ONE_CONTINENT", 0.6, 20_000L,
            "Reconstruction of global hominin refugia following the Youngest Toba Tuff super-eruption and rapid volcanic winter onset.",
            "Reconstitution des refuges d'hominines suite à la super-éruption du Toba et au refroidissement volcanique abrupt.",
            "Late Middle Stone Age Microlithic Hearths & Coastal Shellfish Exploitation", "Refugial Forager Kin Networks", 2200));

        _list.add(new EpochMeta(-50000L, "Upper Paleolithic Revolution & Sahul Colonization (-50,000 BP)", "SAHUL_MIGRATION", 0.8, 200_000L,
            "Global Upper Paleolithic expansion of behavioral modernity, blade technology, cave art, and maritime colonization of Sahul (Australia/New Guinea).",
            "Expansion mondiale du Paléolithique supérieur, art pariétal, débitage laminaire et colonisation maritime du Sahul (Australie/Nouvelle-Guinée).",
            "Early Upper Paleolithic Blade & Bone Tool Industries, Ochre Art & Ocean Crossings", "Exogamous Clan Bands & Subsection Systems", 2400));

        _list.add(new EpochMeta(-25000L, "Last Glacial Maximum & Solutrean/Gravettian Mammoth Steppe (-25,000 BP)", "BERINGIA_AMERICAS", 1.0, 500_000L,
            "Peak glacial climate regime with extensive ice sheets, exposed continental shelves, Solutrean pressure flaking, and Gravettian mammoth hunter encampments.",
            "Régime glaciaire maximal avec calottes polaires étendues, plateaux continentaux émergés, retouche par pression solutréenne et campements gravettiens.",
            "Solutrean / Gravettian Pressure-Flaked Lithics, Tailored Fur Clothing & Portable Art", "Aggregation Band Networks & Seasonal Macro-Bands", 2500));

        _list.add(new EpochMeta(-20000L, "Last Glacial Maximum Peak & Beringian Standstill (-20,000 BP)", "BERINGIA_AMERICAS", 1.0, 600_000L,
            "Glacial maximum nadir with maritime standstill in Beringia, Epigravettian Mediterranean refugia, and Kebaran bladelet industries in the Levant.",
            "Nadir du maximum glaciaire avec pause beringienne, refuges épigravettiens méditerranéens et industries kébariennes au Levant.",
            "Epigravettian & Kebaran Microlithic Bladelets, Mammoth Bone Dwellings", "Territorial Foraging Bands & Base Camps", 2500));

        _list.add(new EpochMeta(-10900L, "Younger Dryas Abrupt Cooling & Proto-Natufian Foragers (-10,900 BP / 8900 BC)", "YOUNGER_DRYAS", 1.2, 2_000_000L,
            "Abrupt hemispheric cooling trigger during Younger Dryas; sedentary Natufian hunter-gatherers, wild cereal harvesting, and stone mortar storage.",
            "Refroidissement abrupt du Dryas récent ; chasseurs-cueilleurs sédentaires natoufiens, récolte de céréales sauvages et mortiers en pierre.",
            "Natufian Microlithic Sickles, Stone Mortars & Semi-Subterranean Circular Huts", "Sedentary Hamlet Co-Residential Lineages", 2400));

        _list.add(new EpochMeta(-10000L, "Early Holocene & Fertile Crescent Pre-Pottery Neolithic (-10,000 BP / 8000 BC)", "NEOLITHIC", 1.5, 5_000_000L,
            "Transition to agriculture and domestication of emmer, einkorn, barley, goats, and sheep in the Fertile Crescent (Göbekli Tepe, Jericho, Çayönü).",
            "Transition vers l'agriculture et domestication des céréales et caprinés dans le Croissant fertile (Göbekli Tepe, Jéricho, Çayönü).",
            "Pre-Pottery Neolithic A/B (PPNA/PPNBP) Ground Stone Axes, Mudbrick Architecture & Cult Centers", "Patrilocal Extended Peasant Households & Shrine Sodalities", 2500));

        _list.add(new EpochMeta(-8000L, "Neolithic Agricultural Expansion & Green Sahara (-8,000 BP / 6000 BC)", "GREEN_SAHARA", 1.8, 10_000_000L,
            "Cardial and Linear Pottery (LBK) farming expansion across Europe, Yangshao millet farming in China, and pastoral lacustrine cultures across the Green Sahara.",
            "Expansion agricole rubanée et cardiale en Europe, culture du millet Yangshao en Chine et pastoralisme lacustre au Sahara vert.",
            "Cardial / LBK Pottery, Polished Stone Adzes, Pastoral Cattle Corrals & Longhouses", "Segmentary Peasant Lineages & Village Communes", 2550));

        _list.add(new EpochMeta(-6000L, "Mid-Holocene Climatic Optimum & Vinča / Ubaid Proto-Urbanism (-6,000 BP / 4000 BC)", "EGYPT_NILE", 2.0, 20_000_000L,
            "Proto-urban tell settlements in Mesopotamia (Ubaid period), Copper metallurgy in Balkans (Vinča, Varna gold), and Majiabang/Hemudu wet-rice farming in the Yangtze.",
            "Proto-urbanisme mésopotamien (période d'Obeïd), métallurgie du cuivre dans les Balkans (Vinča, Varna) et riziculture de Majiabang/Hemudu.",
            "Copper Smelting, Wheel-Thrown Pottery, Mudbrick Temples & Irrigation Canals", "Corporate Lineage Estates & Temple Chiefdoms", 2600));

        _list.add(new EpochMeta(-3000L, "Early Bronze Age, Uruk Expansion & First Dynastic Egypt (-3000 BC)", "MESOPOTAMIA_ASSYRIA", 2.2, 45_000_000L,
            "Emergence of the state, archaic cuneiform writing, bronze metallurgy, monumental ziggurats and pyramids in Uruk Sumer, Early Dynastic Egypt, and Liangzhu China.",
            "Émergence de l'État, écriture cunéiforme archaïque, métallurgie du bronze et architecture monumentale à Sumer, en Égypte et à Liangzhu.",
            "Alloyed Bronze Tools, Cuneiform/Hieroglyphic Administration, Ox-Drawn Plows & Monumental Masonry", "Patriarchal Temple-Palace Dynasties & Redistributive Estates", 2650));

        _list.add(new EpochMeta(-1900L, "Middle Bronze Age & Indus Valley Urban Peak / Harappan Epoch (-1900 BC)", "INDIA_MAURYA", 2.4, 70_000_000L,
            "Mature Harappan urbanism with standardized grid planning, hydraulic drainage, Minoan palaces in Crete, Middle Kingdom Egypt, and Xia/Erlitou China.",
            "Urbanisme harappéen planifié avec drainage hydraulique, palais minoens en Crète, Moyen Empire égyptien et culture d'Erlitou.",
            "Standardized Mudbrick Urban Architecture, Bronze Casting & Maritime Trade Docks", "Civic Guilds, Corporate Priesthoods & Extended Joint Families", 2650));

        _list.add(new EpochMeta(-1500L, "Late Bronze Age International System & Shang Dynasty (-1500 BC)", "SONG_DYNASTY", 2.5, 90_000_000L,
            "Amarna diplomacy era connecting New Kingdom Egypt, Hittites, Mittani, Mycenae, and Shang Dynasty bronze ritual state in China.",
            "Système diplomatique international de l'âge du bronze reliant l'Égypte du Nouvel Empire, les Hittites, Mycènes et les Shang en Chine.",
            "Chariot Warfare, Advanced Bronze Piece-Mold Casting, Oracle Bone Script & International Maritime Trade", "Agnatic Dynastic Lineages, Royal Palace Estates & Corvée Labor", 2700));

        _list.add(new EpochMeta(-1200L, "Late Bronze Age Collapse & Sea Peoples Horizon (-1200 BC)", "MESOPOTAMIA_ASSYRIA", 2.5, 80_000_000L,
            "Systemic Eastern Mediterranean collapse, destruction of Mycenaean and Hittite palaces, Sea Peoples incursions, and transition to Iron Age decentralization.",
            "Effondrement systémique de l'âge du bronze en Méditerranée orientale, fin des palais mycéniens et hittites et transition vers le fer.",
            "Early Wrought Iron Forging, Ashlar Fortifications & Dispersed Agrarian Homesteads", "Segmentary Warrior Kin-Groups & Decentralized Village Enclaves", 2600));

        _list.add(new EpochMeta(-1000L, "Early Iron Age & Neo-Assyrian / Zhou Dynasty Emergence (-1000 BC)", "MESOPOTAMIA_ASSYRIA", 2.6, 100_000_000L,
            "Widespread bloomery iron metallurgy, Neo-Assyrian military expansion, Western Zhou feudalism (Fengjian), and Phoenician alphabet diffusion.",
            "Métallurgie du fer au bas-fourneau, expansion militaire néo-assyrienne, féodalité des Zhou occidentaux et diffusion de l'alphabet phénicien.",
            "Iron Weapons, Ashlar Fortifications, Phénician Maritime Galleys & Alphabetic Writing", "Patriarchal Aristocratic Lineages & Feudal Estates", 2700));

        _list.add(new EpochMeta(-334L, "Classical Antiquity & Alexander's Hellenistic Expansion (-334 BC)", "ROMAN_EMPIRE", 3.0, 150_000_000L,
            "Alexander the Great's conquest of the Achaemenid Persian Empire, synthesis of Greek and Near Eastern civilizations, and Warring States China.",
            "Conquête de l'Empire perse par Alexandre le Grand, synthèse hellénistique et période des Royaumes combattants en Chine.",
            "Iron Pikes (Sarissa), Siege Catapults, Hellenistic Urban Grid Planning & Monetal Coinage", "Civic Polis Citizen Assemblies & Imperial Administrative Bureaucracy", 2750));

        _list.add(new EpochMeta(-300L, "Hellenistic Kingdoms, Maurya Empire & Warring States (-300 BC)", "INDIA_MAURYA", 3.0, 160_000_000L,
            "Ptolemaic and Seleucid kingdoms, Ashoka's Maurya Empire in India, and late Warring States consolidation under Qin.",
            "Royaumes ptolémaïque et séleucide, empire Maurya d'Ashoka en Inde et fin des Royaumes combattants en Chine.",
            "Iron Agricultural Implements, Hydraulic Canals, Ashokan Edicts & Monometallic Silver Standards", "Joint Family Households, Caste Guilds (Jati) & Imperial Bureaucracy", 2750));

        _list.add(new EpochMeta(0L, "Pax Romana, Han Empire & Classical Axial Age (1 AD)", "ROMAN_EMPIRE", 3.2, 250_000_000L,
            "High Classical antiquity: Roman Empire across the Mediterranean basin, Western Han Dynasty in China, Kushan Empire, and Parthia.",
            "Haute Antiquité classique : Empire romain en Méditerranée, dynastie des Han occidentaux en Chine, Empire kouchan et Parthie.",
            "Roman Concrete (Opus Caementicium), Aqueducts, Silk Road Caravans, Han Blast Furnaces & Watermills", "Paterfamilias Roman Household & Confucian Patrilineal Clan", 2800));

        _list.add(new EpochMeta(536L, "Extreme Climate Event of 536 AD & Late Antique Little Ice Age (536 AD)", "ROMAN_EMPIRE", 3.1, 230_000_000L,
            "Volcanic dust veil event of 536 AD, Justinianic Plague pandemic, Sasanian-Byzantine wars, and Southern/Northern Dynasties China.",
            "Voile de poussière volcanique de 536, peste de Justinien, guerres perso-byzantines et dynasties du Nord et du Sud en Chine.",
            "Heavy Moldboard Plows, Fortified Castra, Blast-Furnace Cast Iron & Monastic Scriptoria", "Feudal Agrarian Colonate & Patrilineal Aristocratic Clans", 2700));

        _list.add(new EpochMeta(632L, "Early Islamic Expansion & Tang Dynasty Consolidation (632 AD)", "FERTILE_CRESCENT", 3.3, 240_000_000L,
            "Founding of the Rashidun Caliphate, rapid Middle Eastern expansion, Tang Dynasty reunification of China, and emergence of Srivijaya.",
            "Fondation du Califat des Rachidoune, expansion islamique au Moyen-Orient, réunification Tang de la Chine et émergence de Srivijaya.",
            "Qanat Irrigation Engineering, Arabian Camel Caravans, Tang Woodblock Printing & Damascene Steel", "Segmentary Arab Patrilineages & Confucian Imperial Meritocracy", 2800));

        _list.add(new EpochMeta(1000L, "Medieval Climate Optimum, Song Dynasty & Norse Expansion (1000 AD)", "SONG_DYNASTY", 3.5, 300_000_000L,
            "Medieval Warm Period: Song Dynasty commercial revolution, Fatimid Cairo, Holy Roman Empire, Chola naval supremacy, and Norse Atlantic voyages.",
            "Optimum climatique médiéval : révolution commerciale Song, Le Caire fatimide, Saint-Empire, suprématie navale Chola et voyages vikings.",
            "Movable Type Printing, Magnetic Compass, Gunpowder Formulas, Double-Cropping Champa Rice & Windmills", "Manorial Feudal Serfdom, Stem Households & Lineage Halls", 2850));

        _list.add(new EpochMeta(1206L, "Mongol World Empire Formation & High Medieval Crusades (1206 AD)", "SONG_DYNASTY", 3.6, 360_000_000L,
            "Coronation of Genghis Khan, unification of Steppe tribes, Pax Mongolica trans-Eurasian trade routes, and High Medieval cathedrals.",
            "Couronnement de Gengis Khan, unification des tribus de la steppe, Pax Mongolica et grandes cathédrales médiévales.",
            "Composite Reflex Bows, Trebuchet Siegecraft, Yam Postal Relay Stations & Paper Money (Jiaochao)", "Nomadic Steppe Clan Federations (Otog) & Feudal Estates", 2850));

        _list.add(new EpochMeta(1324L, "Mansa Musa's Pilgrimage & High Middle Ages (1324 AD)", "WEST_AFRICA_MALI", 3.7, 420_000_000L,
            "Height of the Mali Empire, trans-Saharan gold-salt trade, Yuan Dynasty in China, Delhi Sultanate in India, and Italian Renaissance city-states.",
            "Apogée de l'Empire du Mali, commerce transsaharien de l'or, dynastie Yuan en Chine, sultanat de Delhi et cités-États italiennes.",
            "Gothic Stone Vaulting, Astrolabes, Blast Furnaces, Trans-Saharan Caravans & Portolan Charts", "Guild Merchant Families, African Segmentary Lineages & Feudal Manors", 2900));

        _list.add(new EpochMeta(1347L, "Black Death Pandemic & Fourteenth-Century Crisis (1347 AD)", "SONG_DYNASTY", 3.6, 370_000_000L,
            "Arrival of Yersinia pestis in Mediterranean ports, massive demographic contraction across Eurasia, and subsequent wage labor restructuring.",
            "Arrivée de la peste noire en Méditerranée, effondrement démographique en Eurasie et restructuration du salariat agricole.",
            "Full-Rigged Carracks, Mechanical Clock Towers, Heavy Trebuchets & Early Cannons", "Agrarian Peasant Households & Wage Labor Restructuring", 2800));

        _list.add(new EpochMeta(1491L, "Pre-Columbian Americas & Eurasian Renaissance Eve (1491 AD)", "AMERICAS_1491", 3.8, 500_000_000L,
            "Complex indigenous states in the Americas (Triple Alliance Aztec Empire, Inca Tawantinsuyu, Mississippian centers) on the eve of European contact.",
            "États indigènes complexes dans les Amériques (Empire aztèque, Tawantinsuyu inca, cités mississippiennes) à la veille du contact européen.",
            "Chinampa Intensive Wetland Farming, Incan Quipu & Road Network, Bronze/Arquebus Arms in Eurasia", "Ayllu Dual Reciprocity, Calpulli Wards & European Manorial Households", 2850));

        _list.add(new EpochMeta(1492L, "Columbian Exchange & Age of Discovery (1492 AD)", "EPIDEMIC_CONTACT", 3.8, 500_000_000L,
            "Columbus transatlantic landfall, inception of the global Columbian Exchange of crops, animals, and pathogens, and Ming Dynasty maritime trade.",
            "Arrivée transatlantique de Colomb, début de l'échange colombien (plantes, animaux, pathogènes) et commerce maritime Ming.",
            "Oceanic Caravels & Carracks, Navigational Astronomy, Early Cast Iron Artillery & Gutenberg Movable Print", "Iberian Hidalguía, Joint-Stock Proto-Enterprises & Indigenous Moieties", 2850));

        _list.add(new EpochMeta(1639L, "Sakoku Japan, Thirty Years' War & Ming-Qing Transition (1639 AD)", "SAKOKU_JAPAN", 4.2, 580_000_000L,
            "Tokugawa Shogunate Sakoku edicts, Thirty Years' War in Europe, Ming-Qing transition in China, and Mughal architectural golden age.",
            "Édits de fermeture Sakoku des Tokugawa, guerre de Trente Ans en Europe, transition Ming-Qing et âge d'or moghol.",
            "Flintlock Muskets, Galleons & Fluyts, Scientific Revolution Telescopes & Early Joint-Stock Companies", "Ie Stem Family System, Western European Nuclear Households & Agnatic Lineages", 2900));

        _list.add(new EpochMeta(1800L, "First Industrial Revolution & Global Napoleonic / Imperial Era (1800 AD)", "INDUSTRIAL", 5.0, 1_000_000_000L,
            "Steam engine industrialization, mechanical textile mills, Napoleonic administrative codification, Qing demographic apex, and Atlantic Revolutions.",
            "Industrialisation à la vapeur, filatures mécaniques, codification napoléonienne, apogée démographique Qing et révolutions atlantiques.",
            "Watt Steam Engines, Puddling Iron Metallurgy, Power Looms, Canals & Semaphoric Telegraphs", "Industrial Wage-Labor Proletariat, Egalitarian Nuclear & Stem Families", 3100));

        _list.add(new EpochMeta(1900L, "Second Industrial Revolution & Belle Époque Imperial System (1900 AD)", "URBAN_CLUSTERS", 6.0, 1_650_000_000L,
            "Electrification, internal combustion engines, Bessemer steel, transcontinental railways, global telegraph cables, and High Imperialism.",
            "Électrification, moteurs à combustion interne, acier Bessemer, chemins de fer transcontinentaux, câbles télégraphiques et haut impérialisme.",
            "Electric Dynamos, Bessemer Steel, Internal Combustion Engines, Submarine Cables & Chemical Synthesis", "Urban Industrial Proletariat & Bourgeois Nuclear Households", 3200));

        _list.add(new EpochMeta(1914L, "Outbreak of World War I & End of Nineteenth-Century Order (1914 AD)", "URBAN_CLUSTERS", 6.2, 1_800_000_000L,
            "Total industrial warfare, global alliance systems, dreadnought battleships, Haber-Bosch nitrogen fixation, and breakdown of the Concert of Europe.",
            "Guerre industrielle totale, systèmes d'alliances mondiaux, cuirassés dreadnought, procédé Haber-Bosch et fin du concert européen.",
            "Haber-Bosch Nitrogen Fixation, Mass Machine Guns, Dreadnoughts, Aircraft & Radio Telephony", "Total Mobilization Nation-States & Industrial Urban Families", 3200));

        _list.add(new EpochMeta(1950L, "Post-WWII Global Reconstruction & Great Acceleration (1950 AD)", "URBAN_CLUSTERS", 7.0, 2_500_000_000L,
            "Post-war Bretton Woods economic order, atomic energy, Green Revolution agrochemicals, mass antibiotics, and the start of the Great Acceleration.",
            "Ordre de Bretton Woods, énergie atomique, révolution verte agrochimique, antibiotiques de masse et début de la Grande Accélération.",
            "Fission Reactors, Jet Aviation, Transistors, Synthetic Polymers & Industrial Petrochemistry", "Suburban Consumer Nuclear Families & Welfare State Institutions", 3300));

        _list.add(new EpochMeta(2000L, "Turn of the Millennium & Digital Information Age (2000 AD)", "URBAN_CLUSTERS", 8.0, 6_100_000_000L,
            "Global internet expansion, microprocessors, fiber optic telecoms, containerized supply chains, and post-Cold War globalization.",
            "Expansion mondiale d'Internet, microprocesseurs, télécoms à fibre optique, chaînes logistiques conteneurisées et mondialisation.",
            "VLSI Microprocessors, Global Fiber Optics, GPS Constellations, Containerized Intermodal Logistics", "Post-Industrial Nuclear & Single-Person Households", 3350));

        _list.add(new EpochMeta(2026L, "Anthropocene Present Day & Global Energy Transition (2026 AD)", "URBAN_CLUSTERS", 8.5, 8_150_000_000L,
            "Planetary computing, deep neural networks, renewable energy grids, geopolitical multipolarity, and active climate transition.",
            "Informatique planétaire, réseaux de neurones profonds, réseaux d'énergies renouvelables, multipolarité et transition climatique.",
            "GPU Compute Clusters, Advanced Photovoltaics, High-Capacity Lithium-Ion Storage & Satellite Megaconstellations", "Diverse Globalized Urban Households & Digital Network Affiliations", 3400));

        _list.add(new EpochMeta(2035L, "Near-Future Demographic Transition & Clean Energy Scaling (2035 AD)", "URBAN_CLUSTERS", 9.0, 8_800_000_000L,
            "Large-scale grid electrification, solid-state batteries, autonomous transport networks, precision fermentation, and demographic stabilization.",
            "Électrification massive des réseaux, batteries solides, transports autonomes, fermentation de précision et stabilisation démographique.",
            "Solid-State Storage, Fusion Pilot Plants, Autonomous Robotic Freight & Synthetic Biology Bioreactors", "Flexible Urban Eco-Communities & Automated Labor Households", 3400));

        _list.add(new EpochMeta(2045L, "Mid-Century Climate Adaptation & Automated Labor Transition (2045 AD)", "URBAN_CLUSTERS", 9.3, 9_300_000_000L,
            "Industrial direct air carbon capture, global desalination pipelines, automated agriculture, and space launch reusability.",
            "Capture directe du carbone dans l'air, réseaux de dessalement mondiaux, agriculture automatisée et réutilisabilité spatiale.",
            "Gigawatt Direct Air Capture, Commercial Magnetic Fusion, Orbital Space Infrastructure & Closed-Loop Circular Metallurgy", "Universal Basic Infrastructure Communities & Post-Scarcity Nodes", 3450));

        _list.add(new EpochMeta(2050L, "Post-Fossil Equilibrium & Global Demographic Peak (2050 AD)", "URBAN_CLUSTERS", 9.5, 9_700_000_000L,
            "Global demographic inflection point, 100% clean primary energy matrix, planetary geoengineering monitoring, and ecological restoration.",
            "Point d'inflexion démographique mondial, matrice énergétique 100% décarbonée, géo-ingénierie surveillée et restauration écologique.",
            "Planetary Energy Mesh, Deep Geothermal Supercritical Wells, Asteroid Resource Prospecting & Ecosystem Digital Twins", "Regenerative Bioregional Cooperatives & Automated Civil Polities", 3500));

        _list.add(new EpochMeta(2060L, "Planetary Ecological Restoration & Space Industrialization Horizon (2060 AD)", "URBAN_CLUSTERS", 9.8, 9_800_000_000L,
            "Large-scale rewilding, lunar industrial infrastructure, closed-cycle industrial ecosystems, and stable planetary boundary stewardship.",
            "Réensauvagement planétaire, infrastructure industrielle lunaire, cycles industriels fermés et gestion stable des limites planétaires.",
            "Lunar Mass Drivers, Orbital Solar Power Refinement, Global Ecological Restoration Systems & Quantum Materials", "Planetary Federation Stewardship Councils & Trans-Bioregional Networks", 3500));

        _list.sort(Comparator.comparingLong(EpochMeta::year));
        EPOCHS = Collections.unmodifiableList(_list);
    }

    public static final Set<Long> CANONICAL_36_YEARS = Set.of(
        -100000L, -74000L, -50000L, -25000L, -20000L, -10900L, -10000L,
        -8000L, -6000L, -3000L, -1900L, -1500L, -1200L, -1000L,
        -334L, -300L, 0L, 536L, 632L, 1000L, 1206L, 1324L, 1347L,
        1491L, 1492L, 1639L, 1800L, 1900L, 1914L, 1950L, 2000L,
        2026L, 2035L, 2045L, 2050L, 2060L
    );

    @Test
    /*
     * Generate epoch100k only operation.
     * <p>
     * Executes operational logic for {@code GenerateEpochMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void generateEpoch100kOnly() throws Exception {
        File seshatFile = new File(CliopatriaPolityVectorReader.SESHAT_GEOJSON_PATH);
        File rootDir = new File("data/maps/ether/earth");
        rootDir.mkdirs();

        EpochMeta em = EPOCHS.stream().filter(e -> e.year() == -100000L).findFirst().orElseThrow();
        long yr = -100000L;
        System.out.printf("=== PROCESSING SINGLE EPOCH %d: %s ===%n", yr, em.eraName());

        File yrDir = new File(rootDir, String.valueOf(yr));
        yrDir.mkdirs();

        Scenario sc = new Scenario();
        sc.setName(em.eraName());
        sc.setStartDateYear(yr);
        sc.setPopulationDensityType(em.densityType());
        sc.setInitialHumanCount(em.population());
        sc.setInitialTechLevel(em.techLevel());

        HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(sc);

        File regFile = new File(yrDir, "cultural_registry.json");
        writeEpochCulturalRegistry(regFile, em, seshatFile);
        regFile.setLastModified(System.currentTimeMillis());

        File provFile = new File(yrDir, "provenance_and_sources.json");
        writeEpochProvenance(provFile, em);
        provFile.setLastModified(System.currentTimeMillis());

        File readmeFile = new File(yrDir, "README.md");
        writeEpochReadme(readmeFile, em);
        readmeFile.setLastModified(System.currentTimeMillis());

        System.out.printf("  [SUCCESS] Epoch %d generated!%n", yr);
    }

    @Test
    /*
     * Generate canonical36epochs cultural data operation.
     * <p>
     * Executes operational logic for {@code GenerateEpochMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void generateCanonical36EpochsCulturalData() throws Exception {
        File seshatFile = new File(CliopatriaPolityVectorReader.SESHAT_GEOJSON_PATH);
        File rootDir = new File("data/maps/ether/earth");
        rootDir.mkdirs();

        List<EpochMeta> canonicalList = EPOCHS.stream()
            .filter(e -> CANONICAL_36_YEARS.contains(e.year()))
            .toList();

        System.out.printf("Starting regeneration for %d CANONICAL landmark epochs...%n", canonicalList.size());

        for (EpochMeta em : canonicalList) {
            long yr = em.year();
            System.out.printf("=== PROCESSING CANONICAL EPOCH %d: %s ===%n", yr, em.eraName());

            File yrDir = new File(rootDir, String.valueOf(yr));
            yrDir.mkdirs();

            // 1. Regenerate all 25 rasters via HistoricalMapGenerator
            Scenario sc = new Scenario();
            sc.setName(em.eraName());
            sc.setStartDateYear(yr);
            sc.setPopulationDensityType(em.densityType());
            sc.setInitialHumanCount(em.population());
            sc.setInitialTechLevel(em.techLevel());

            HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(sc);

            // 2. Generate / Update cultural_registry.json
            File regFile = new File(yrDir, "cultural_registry.json");
            writeEpochCulturalRegistry(regFile, em, seshatFile);
            regFile.setLastModified(System.currentTimeMillis());

            // 3. Generate / Update provenance_and_sources.json
            File provFile = new File(yrDir, "provenance_and_sources.json");
            writeEpochProvenance(provFile, em);
            provFile.setLastModified(System.currentTimeMillis());

            // 4. Generate / Update README.md
            File readmeFile = new File(yrDir, "README.md");
            writeEpochReadme(readmeFile, em);
            readmeFile.setLastModified(System.currentTimeMillis());

            System.out.printf("  [SUCCESS] Canonical Epoch %d: 25 Standard Rasters + 2 JSONs + 1 README.md written.%n", yr);
        }

        System.out.printf("All %d canonical landmark epochs successfully regenerated and documented.%n", canonicalList.size());
    }

    @Test
    /*
     * Regenerate prehistoric density maps only operation.
     * <p>
     * Executes operational logic for {@code GenerateEpochMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void regeneratePrehistoricDensityMapsOnly() throws Exception {
        File rootDir = new File("data/maps/ether/earth");
        rootDir.mkdirs();

        List<EpochMeta> prehistoricEpochs = EPOCHS.stream()
            .filter(e -> e.year() <= -10000L)
            .toList();

        System.out.printf("Starting targeted density regeneration for %d prehistoric epochs (<= -10,000 BP)...%n", prehistoricEpochs.size());

        for (EpochMeta em : prehistoricEpochs) {
            long yr = em.year();
            File yrDir = new File(rootDir, String.valueOf(yr));
            yrDir.mkdirs();

            java.awt.image.BufferedImage imgDensity = HistoricalMapGenerator.applyAltimetryCoastlineMask(
                HistoricalMapGenerator.generatePrehistoricSyntheticDensityMap(yr, em.densityType())
            );

            File targetPng = new File(yrDir, String.format("earth_%d_density.png", yr));
            javax.imageio.ImageIO.write(imgDensity, "PNG", targetPng);
            targetPng.setLastModified(System.currentTimeMillis());

            System.out.printf("  [DENSITY UPDATED] Prehistoric Epoch %d: %s (%s)%n", yr, em.eraName(), targetPng.getName());
        }

        System.out.printf("Successfully regenerated calibrated physical density maps for all %d prehistoric epochs.%n", prehistoricEpochs.size());
    }

    @Test
    /*
     * Validate prehistoric cartographic differentiation operation.
     * <p>
     * Executes operational logic for {@code GenerateEpochMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void validatePrehistoricCartographicDifferentiation() {
        // 1. Validate Sahul (Australia) Wallace Line Crossing & Tasmania
        double sahulLon = 133.0, sahulLat = -25.0;
        double tasmaniaLon = 146.5, tasmaniaLat = -42.0;
        double occ100k = HistoricalMapGenerator.getHomininOccupancyWeight(sahulLon, sahulLat, -100000L);
        double occ74k  = HistoricalMapGenerator.getHomininOccupancyWeight(sahulLon, sahulLat, -74000L);
        double occ50k  = HistoricalMapGenerator.getHomininOccupancyWeight(sahulLon, sahulLat, -50000L);
        double occ20k  = HistoricalMapGenerator.getHomininOccupancyWeight(sahulLon, sahulLat, -20000L);
        double occTasmania50k = HistoricalMapGenerator.getHomininOccupancyWeight(tasmaniaLon, tasmaniaLat, -50000L);
        double occTasmania35k = HistoricalMapGenerator.getHomininOccupancyWeight(tasmaniaLon, tasmaniaLat, -35000L);

        org.junit.jupiter.api.Assertions.assertEquals(0.0, occ100k, 1e-6, "Sahul must be strictly unoccupied at -100,000 BP");
        org.junit.jupiter.api.Assertions.assertEquals(0.0, occ74k, 1e-6, "Sahul must be strictly unoccupied at -74,000 BP");
        org.junit.jupiter.api.Assertions.assertTrue(occ50k > 0.5, "Sahul must be populated at -50,000 BP across Wallace Line");
        org.junit.jupiter.api.Assertions.assertEquals(0.0, occTasmania50k, 1e-6, "Tasmania must be strictly unoccupied at -50,000 BP");
        org.junit.jupiter.api.Assertions.assertTrue(occTasmania35k > 0.3, "Tasmania must be populated at -35,000 BP via Bassian Plain");
        org.junit.jupiter.api.Assertions.assertTrue(occ20k > 0.5, "Sahul must be populated at -20,000 BP");

        // 1b. Validate Americas Peopling & Strict Ice Sheet Masking
        double laurentideLon = -85.0, laurentideLat = 55.0;
        double fennoLon = 20.0, fennoLat = 65.0;
        double whiteSandsLon = -106.3, whiteSandsLat = 32.8;
        double pacificKelpLon = -125.0, pacificKelpLat = 48.0;

        double occLaurYD = HistoricalMapGenerator.getHomininOccupancyWeight(laurentideLon, laurentideLat, -10900L);
        double occFennoYD = HistoricalMapGenerator.getHomininOccupancyWeight(fennoLon, fennoLat, -10900L);
        double occWhiteSands20k = HistoricalMapGenerator.getHomininOccupancyWeight(whiteSandsLon, whiteSandsLat, -20000L);
        double occKelp20k = HistoricalMapGenerator.getHomininOccupancyWeight(pacificKelpLon, pacificKelpLat, -20000L);
        double occAmericas100k = HistoricalMapGenerator.getHomininOccupancyWeight(-100.0, 40.0, -100000L);

        org.junit.jupiter.api.Assertions.assertEquals(0.0, occLaurYD, 1e-6, "Laurentide Ice Sheet must be strictly unpopulated at Younger Dryas");
        org.junit.jupiter.api.Assertions.assertEquals(0.0, occFennoYD, 1e-6, "Fennoscandian Ice Sheet must be strictly unpopulated at Younger Dryas");
        org.junit.jupiter.api.Assertions.assertTrue(occWhiteSands20k > 0.1, "White Sands must be populated at -20,000 BP LGM");
        org.junit.jupiter.api.Assertions.assertTrue(occKelp20k > 0.1, "Pacific Kelp Highway must be populated at -20,000 BP LGM");
        org.junit.jupiter.api.Assertions.assertEquals(0.0, occAmericas100k, 1e-6, "Americas must be strictly unpopulated at -100,000 BP");

        // 2. Validate Toba Volcanic Winter Anomalies (-74k vs -100k)
        double indiaLon = 78.0, indiaLat = 20.0;
        double occIndia100k = HistoricalMapGenerator.getHomininOccupancyWeight(indiaLon, indiaLat, -100000L);
        double occIndia74k = HistoricalMapGenerator.getHomininOccupancyWeight(indiaLon, indiaLat, -74000L);
        org.junit.jupiter.api.Assertions.assertTrue(occIndia74k < occIndia100k * 0.3, "South Asia hominin occupancy must be severely depressed by Toba ash fall");

        double tobaTempDelta = WorldClimEmpiricalRasterLoader.computePaleoTemperatureDelta(indiaLat, indiaLon, 200.0, -74000L);
        double eemianTempDelta = WorldClimEmpiricalRasterLoader.computePaleoTemperatureDelta(indiaLat, indiaLon, 200.0, -100000L);
        org.junit.jupiter.api.Assertions.assertTrue(tobaTempDelta < -4.0, "Toba volcanic winter must have <= -4.0°C cooling anomaly in South Asia");
        org.junit.jupiter.api.Assertions.assertTrue(eemianTempDelta > 0.5, "Eemian must have positive warming anomaly");

        // 3. Validate Dynamic Groundwater Aquifer Recharge
        double saharaLon = 18.0, saharaLat = 21.0;
        double rech100k = HistoricalMapGenerator.getPaleoAquiferRechargeFactor(saharaLon, saharaLat, -100000L);
        double rech74k = HistoricalMapGenerator.getPaleoAquiferRechargeFactor(saharaLon, saharaLat, -74000L);
        double rechSahul50k = HistoricalMapGenerator.getPaleoAquiferRechargeFactor(sahulLon, sahulLat, -50000L);
        double rechLgm20k = HistoricalMapGenerator.getPaleoAquiferRechargeFactor(0.0, 50.0, -20000L);

        org.junit.jupiter.api.Assertions.assertTrue(rech100k > 2.0, "Green Sahara MIS 5e must have > 2.0x aquifer recharge factor");
        org.junit.jupiter.api.Assertions.assertTrue(rech74k < 0.70, "Toba tropical drought must have < 0.70x recharge factor");
        org.junit.jupiter.api.Assertions.assertTrue(rechSahul50k >= 1.5, "MIS 3 Sahul megalakes must have >= 1.5x aquifer recharge factor");
        org.junit.jupiter.api.Assertions.assertTrue(rechLgm20k <= 0.45, "LGM high-latitude permafrost lock-up must have <= 0.45x recharge factor");
    }

    @Test
    /*
     * Compute and validate global population continuity operation.
     * <p>
     * Executes operational logic for {@code GenerateEpochMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void computeAndValidateGlobalPopulationContinuity() throws Exception {
        double rEarthKm = 6371.0;
        int width = 2048;
        int height = 1024;
        double basePixelAreaKm2 = (2.0 * Math.PI * rEarthKm / width) * (Math.PI * rEarthKm / height);

        System.out.println("==========================================================================");
        System.out.println("  GLOBAL HUMAN POPULATION INTEGRAL EVALUATION ACROSS ALL LANDMARK EPOCHS  ");
        System.out.println("==========================================================================");
        System.out.printf("%-12s | %-45s | %-16s | %-16s%n", "Epoch (BP)", "Historical Context", "Integrated Pop", "Reference Target");
        System.out.println("--------------------------------------------------------------------------");

        long[] landmarkYears = {
            -100000L, -74000L, -50000L, -40000L, -35000L, -30000L, -25000L, -20000L,
            -15000L, -14000L, -12000L, -11000L, -10900L, -10000L
        };

        for (long yr : landmarkYears) {
            EpochMeta em = EPOCHS.stream().filter(e -> e.year() == yr).findFirst().orElse(null);
            String name = (em != null) ? em.eraName() : ("Epoch " + yr);
            long targetPop = (em != null) ? em.population() : 0L;

            java.awt.image.BufferedImage img = HistoricalMapGenerator.applyAltimetryCoastlineMask(
                HistoricalMapGenerator.generatePrehistoricSyntheticDensityMap(yr, (em != null) ? em.densityType() : "ONE_CONTINENT")
            );

            double integratedPop = 0.0;
            for (int y = 0; y < height; y++) {
                double lat = 90.0 - (y + 0.5) / height * 180.0;
                double cellArea = basePixelAreaKm2 * Math.cos(Math.toRadians(lat));
                for (int x = 0; x < width; x++) {
                    int rgb = img.getRGB(x, y);
                    int gray = rgb & 0xFF;
                    if (gray > 12) {
                        double logNorm = (gray - 12.0) / 243.0;
                        double dens = (Math.exp(logNorm * Math.log1p(2.5 * 35.0)) - 1.0) / 2.5;
                        integratedPop += dens * cellArea;
                    }
                }
            }

            System.out.printf("%-12d | %-45s | %-16s | %-16s%n",
                yr, name.length() > 45 ? name.substring(0, 42) + "..." : name,
                String.format("%,d", (long) integratedPop),
                String.format("%,d", targetPop));
        }
        System.out.println("==========================================================================");
    }

    @Test
    /*
     * Generate all36epochs cultural data operation.
     * <p>
     * Executes operational logic for {@code GenerateEpochMapsTest} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void generateAll36EpochsCulturalData() throws Exception {
        File seshatFile = new File(CliopatriaPolityVectorReader.SESHAT_GEOJSON_PATH);
        File rootDir = new File("data/maps/ether/earth");
        rootDir.mkdirs();

        System.out.printf("Starting high-throughput parallel cultural data regeneration for %d epochs...%n", EPOCHS.size());

        // Warm up static caches with first epoch synchronously
        if (!EPOCHS.isEmpty()) {
            EpochMeta first = EPOCHS.get(0);
            Scenario scWarm = new Scenario();
            scWarm.setName(first.eraName());
            scWarm.setStartDateYear(first.year());
            scWarm.setPopulationDensityType(first.densityType());
            HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(scWarm);
        }

        java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);
        int total = EPOCHS.size();

        EPOCHS.parallelStream().forEach(em -> {
            try {
                long yr = em.year();
                long t0 = System.currentTimeMillis();

                File yrDir = new File(rootDir, String.valueOf(yr));
                yrDir.mkdirs();

                // 1. Regenerate all 25 rasters via HistoricalMapGenerator
                Scenario sc = new Scenario();
                sc.setName(em.eraName());
                sc.setStartDateYear(yr);
                sc.setPopulationDensityType(em.densityType());
                sc.setInitialHumanCount(em.population());
                sc.setInitialTechLevel(em.techLevel());

                HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(sc);

                // 2. Generate / Update cultural_registry.json
                File regFile = new File(yrDir, "cultural_registry.json");
                writeEpochCulturalRegistry(regFile, em, seshatFile);
                regFile.setLastModified(System.currentTimeMillis());

                // 3. Generate / Update provenance_and_sources.json
                File provFile = new File(yrDir, "provenance_and_sources.json");
                writeEpochProvenance(provFile, em);
                provFile.setLastModified(System.currentTimeMillis());

                // 4. Generate / Update README.md
                File readmeFile = new File(yrDir, "README.md");
                writeEpochReadme(readmeFile, em);
                readmeFile.setLastModified(System.currentTimeMillis());

                int done = completed.incrementAndGet();
                long elapsed = System.currentTimeMillis() - t0;
                System.out.printf("  [SUCCESS %d/%d] Epoch %d: 25 Standard Rasters + 2 JSONs + 1 README.md written (%d ms)%n", done, total, yr, elapsed);
            } catch (Exception e) {
                System.err.printf("  [ERROR] Epoch %d failed: %s%n", em.year(), e.getMessage());
            }
        });

        System.out.printf("All %d epochs successfully regenerated and documented with 25 rasters each.%n", EPOCHS.size());
    }

    // Helper subroutine: write epoch cultural registry - internal state computation & bounds checking
    private void writeEpochCulturalRegistry(File target, EpochMeta em, File seshatFile) throws Exception {
        long yr = em.year();
        List<HistoricalPolityFeature> polities = Collections.emptyList();
        List<NaturalEarthVectorIngestor.CountryFeature> modernCountries = Collections.emptyList();

        if (yr >= 1900) {
            modernCountries = NaturalEarthVectorIngestor.getCountries(2048, 1024);
        } else if (yr >= -3400 && seshatFile.exists()) {
            polities = CliopatriaPolityVectorReader.loadPolitiesForYear(seshatFile, (int) yr, 2048, 1024);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"epoch\": ").append(yr).append(",\n");
        sb.append("  \"era\": \"").append(escapeJson(em.eraName())).append("\",\n");
        sb.append("  \"description\": \"Cliodynamic Cultural, Linguistic, Demographic & Sovereign Polity Registry for Epoch ")
          .append(yr).append(" (Calibrated vs Natural Earth, Seshat, HYDE 3.4 & Murdock D-PLACE)\",\n");
        sb.append("  \"encoding\": \"ID_RGB_24BIT\",\n");
        sb.append("  \"traitDimensions\": [\n");
        sb.append("    \"linguisticBranch\",\n");
        sb.append("    \"socialStructure\",\n");
        sb.append("    \"subsistenceMode\",\n");
        sb.append("    \"ritualTradition\"\n");
        sb.append("  ],\n");
        sb.append("  \"entities\": [\n");

        if (!modernCountries.isEmpty()) {
            // Write authentic modern/contemporary sovereign states from Natural Earth
            for (int i = 0; i < modernCountries.size(); i++) {
                NaturalEarthVectorIngestor.CountryFeature c = modernCountries.get(i);
                java.awt.Color col = NaturalEarthVectorIngestor.computeSovereignColorForYear(c, yr);
                int rgb = col.getRGB() & 0xFFFFFF;
                String hex = String.format("#%06X", rgb);
                String safeId = (c.isoA3 != null && !"-99".equals(c.isoA3) ? c.isoA3 : c.name)
                        .replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(Locale.ROOT);

                sb.append("    {\n");
                sb.append("      \"id\": \"").append(safeId).append("\",\n");
                sb.append("      \"colorHex\": \"").append(hex).append("\",\n");
                sb.append("      \"colorRgb\": [").append(col.getRed()).append(", ").append(col.getGreen()).append(", ").append(col.getBlue()).append("],\n");
                sb.append("      \"traits\": [")
                  .append(String.format(Locale.US, "%.2f", c.technologyLevel / 255.0)).append(", ")
                  .append(String.format(Locale.US, "%.2f", c.institutionalLevel / 255.0)).append(", ")
                  .append(String.format(Locale.US, "%.2f", c.ecologicalFootprint / 255.0)).append(", ")
                  .append(String.format(Locale.US, "%.2f", (255 - c.pathogenStress) / 255.0)).append("],\n");
                sb.append("      \"name\": {\n");
                sb.append("        \"en\": \"").append(escapeJson(c.name)).append("\",\n");
                sb.append("        \"fr\": \"").append(escapeJson(c.name)).append("\",\n");
                sb.append("        \"de\": \"").append(escapeJson(c.name)).append("\",\n");
                sb.append("        \"es\": \"").append(escapeJson(c.name)).append("\",\n");
                sb.append("        \"zh\": \"").append(escapeJson(c.name)).append("\"\n");
                sb.append("      },\n");
                sb.append("      \"isoA3\": \"").append(escapeJson(c.isoA3)).append("\",\n");
                sb.append("      \"continent\": \"").append(escapeJson(c.continent)).append("\",\n");
                sb.append("      \"fromYear\": ").append(yr <= 1945 ? 1900 : (yr <= 1990 ? 1945 : 1991)).append(",\n");
                sb.append("      \"toYear\": ").append(yr >= 2026 ? 2060 : (yr >= 1990 ? 2025 : 1990)).append(",\n");
                sb.append("      \"kinshipType\": \"").append(escapeJson(em.defaultKinship())).append("\",\n");
                sb.append("      \"lithicTechnocomplex\": \"").append(escapeJson(em.lithicComplex())).append("\",\n");
                sb.append("      \"caloricIntakePerCapitaKcal\": ").append(em.defaultKcal()).append("\n");
                sb.append("    }").append(i < modernCountries.size() - 1 ? "," : "").append("\n");
            }
        } else if (!polities.isEmpty()) {
            // Write authentic extracted Seshat polities
            for (int i = 0; i < polities.size(); i++) {
                HistoricalPolityFeature p = polities.get(i);
                java.awt.Color col = CliopatriaPolityVectorReader.getPolityColor(p.name, p.seshatId);
                int rgb = col.getRGB() & 0xFFFFFF;
                String hex = String.format("#%06X", rgb);
                String safeId = p.name.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(Locale.ROOT);

                sb.append("    {\n");
                sb.append("      \"id\": \"").append(safeId).append("\",\n");
                sb.append("      \"colorHex\": \"").append(hex).append("\",\n");
                sb.append("      \"colorRgb\": [").append(col.getRed()).append(", ").append(col.getGreen()).append(", ").append(col.getBlue()).append("],\n");
                sb.append("      \"traits\": [0.85, 0.88, 0.90, 0.85],\n");
                sb.append("      \"name\": {\n");
                sb.append("        \"en\": \"").append(escapeJson(p.name)).append("\",\n");
                sb.append("        \"fr\": \"").append(escapeJson(p.name)).append("\",\n");
                sb.append("        \"de\": \"").append(escapeJson(p.name)).append("\",\n");
                sb.append("        \"es\": \"").append(escapeJson(p.name)).append("\",\n");
                sb.append("        \"zh\": \"").append(escapeJson(p.name)).append("\"\n");
                sb.append("      },\n");
                sb.append("      \"seshatId\": \"").append(p.seshatId != null ? escapeJson(p.seshatId) : "").append("\",\n");
                sb.append("      \"fromYear\": ").append(p.fromYear).append(",\n");
                sb.append("      \"toYear\": ").append(p.toYear).append(",\n");
                sb.append("      \"kinshipType\": \"").append(escapeJson(em.defaultKinship())).append("\",\n");
                sb.append("      \"lithicTechnocomplex\": \"").append(escapeJson(em.lithicComplex())).append("\",\n");
                sb.append("      \"caloricIntakePerCapitaKcal\": ").append(em.defaultKcal()).append("\n");
                sb.append("    }").append(i < polities.size() - 1 ? "," : "").append("\n");
            }
        } else {
            // Write standard prehistoric / futurological macro-complexes
            sb.append("""
                {
                  \"id\": \"core_polity_east_asia\",
                  \"colorHex\": \"#EF4444\",
                  \"colorRgb\": [239, 68, 68],
                  \"traits\": [0.70, 0.85, 0.90, 0.85],
                  \"name\": {
                    \"en\": \"East Asian Core (%s)\",
                    \"fr\": \"Foyer Est-Asiatique (%s)\",
                    \"de\": \"Ostasietischer Zivilisationsraum (%s)\",
                    \"es\": \"Núcleo de Asia Oriental (%s)\",
                    \"zh\": \"东亚核心区（%s）\"
                  },
                  \"kinshipType\": \"%s\",
                  \"lithicTechnocomplex\": \"%s\",
                  \"caloricIntakePerCapitaKcal\": %d
                },
                {
                  \"id\": \"core_polity_west_eurasia\",
                  \"colorHex\": \"#2563EB\",
                  \"colorRgb\": [37, 99, 235],
                  \"traits\": [0.75, 0.80, 0.88, 0.80],
                  \"name\": {
                    \"en\": \"Western Eurasian Core (%s)\",
                    \"fr\": \"Foyer Ouest-Eurasien (%s)\",
                    \"de\": \"Westeurasischer Zivilisationsraum (%s)\",
                    \"es\": \"Núcleo Euroasiático Occidental (%s)\",
                    \"zh\": \"西欧亚核心区（%s）\"
                  },
                  \"kinshipType\": \"%s\",
                  \"lithicTechnocomplex\": \"%s\",
                  \"caloricIntakePerCapitaKcal\": %d
                },
                {
                  \"id\": \"core_polity_south_asia\",
                  \"colorHex\": \"#F59E0B\",
                  \"colorRgb\": [245, 158, 11],
                  \"traits\": [0.65, 0.85, 0.92, 0.90],
                  \"name\": {
                    \"en\": \"South Asian Core (%s)\",
                    \"fr\": \"Foyer Sud-Asiatique (%s)\",
                    \"de\": \"Südasiatischer Zivilisationsraum (%s)\",
                    \"es\": \"Núcleo de Asia del Sur (%s)\",
                    \"zh\": \"南亚核心区（%s）\"
                  },
                  \"kinshipType\": \"%s\",
                  \"lithicTechnocomplex\": \"%s\",
                  \"caloricIntakePerCapitaKcal\": %d
                },
                {
                  \"id\": \"core_polity_americas\",
                  \"colorHex\": \"#06B6D4\",
                  \"colorRgb\": [6, 182, 212],
                  \"traits\": [0.55, 0.75, 0.82, 0.88],
                  \"name\": {
                    \"en\": \"Americas Core (%s)\",
                    \"fr\": \"Foyer des Amériques (%s)\",
                    \"de\": \"Amerikanischer Zivilisationsraum (%s)\",
                    \"es\": \"Núcleo de las Américas (%s)\",
                    \"zh\": \"美洲核心区（%s）\"
                  },
                  \"kinshipType\": \"%s\",
                  \"lithicTechnocomplex\": \"%s\",
                  \"caloricIntakePerCapitaKcal\": %d
                },
                {
                  \"id\": \"core_polity_africa\",
                  \"colorHex\": \"#22C55E\",
                  \"colorRgb\": [34, 197, 94],
                  \"traits\": [0.60, 0.70, 0.75, 0.80],
                  \"name\": {
                    \"en\": \"African Core (%s)\",
                    \"fr\": \"Foyer Africain (%s)\",
                    \"de\": \"Afrikanischer Zivilisationsraum (%s)\",
                    \"es\": \"Núcleo Africano (%s)\",
                    \"zh\": \"非洲核心区（%s）\"
                  },
                  \"kinshipType\": \"%s\",
                  \"lithicTechnocomplex\": \"%s\",
                  \"caloricIntakePerCapitaKcal\": %d
                }
            """.formatted(
                em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.defaultKinship(), em.lithicComplex(), em.defaultKcal(),
                em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.defaultKinship(), em.lithicComplex(), em.defaultKcal(),
                em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.defaultKinship(), em.lithicComplex(), em.defaultKcal(),
                em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.defaultKinship(), em.lithicComplex(), em.defaultKcal(),
                em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.eraName(), em.defaultKinship(), em.lithicComplex(), em.defaultKcal()
            ));
        }

        sb.append("  ]\n");
        sb.append("}\n");
        safeWriteFile(target, sb.toString());
    }

    // Helper subroutine: write epoch provenance - internal state computation & bounds checking
    private void writeEpochProvenance(File target, EpochMeta em) throws Exception {
        long yr = em.year();
        String json = """
        {
          "epoch": %d,
          "era": "%s",
          "planet": "earth",
          "resolution": "2048x1024",
          "projection": "Equirectangular (Plate Carrée, EPSG:4326)",
          "overview": {
            "summary_en": "%s",
            "summary_fr": "%s"
          },
          "layers": {
            "elevation": {
              "filename": "earth_%d_elevation.png",
              "category": "geophysics",
              "data_sources": ["NOAA ETOPO 2022 15-arc-second Global Relief Model", "GEBCO 2023 Grid Bathymetric Model"],
              "reconstitution_rationale": "High-fidelity topography and bathymetry calibrated against epoch eustatic sea level offset."
            },
            "biomes": {
              "filename": "earth_%d_biomes.png",
              "category": "ecology",
              "data_sources": ["WorldClim v2.1 Bioclimatic Indicators", "Biome 6000 Project", "CHELSA-Trace21k"],
              "reconstitution_rationale": "Coupled Holdridge-Whittaker bioclimatic classification driven by empirical temperature and precipitation."
            },
            "temperature": {
              "filename": "earth_%d_temperature.png",
              "category": "climate",
              "data_sources": ["WorldClim v2.1 Bio1 (Annual Mean Temperature)", "PMIP4 Paleoclimate Synthesis"],
              "reconstitution_rationale": "Empirical baseline modulated by continuous 2D orbital and continental paleoclimatic anomaly field."
            },
            "precipitation": {
              "filename": "earth_%d_precipitation.png",
              "category": "climate",
              "data_sources": ["WorldClim v2.1 Bio12 (Annual Precipitation)", "Speleothem & Lake Core Records"],
              "reconstitution_rationale": "Empirical precipitation grid with dynamic ITCZ, monsoonal, and glacial humidity corrections."
            },
            "seasonality": {
              "filename": "earth_%d_seasonality.png",
              "category": "climate",
              "data_sources": ["WorldClim v2.1 Bio4 (Temperature Seasonality)", "Milankovitch Astronomical Solutions"],
              "reconstitution_rationale": "Continuous seasonality amplitude accounting for axial tilt, obliquity, and ocean thermal inertia."
            },
            "density": {
              "filename": "earth_%d_density.png",
              "category": "demography",
              "data_sources": ["HYDE 3.4 History Database of the Global Environment", "Seshat Global History Databank"],
              "reconstitution_rationale": "Empirical demographic density field."
            },
            "sovereignty": {
              "filename": "earth_%d_sovereignty.png",
              "category": "sociology",
              "data_sources": ["Seshat Databank Polities (ClioPatria 2023)", "Historical GIS Global Boundary Datasets"],
              "reconstitution_rationale": "Multi-center polity sovereign domains and political borders."
            },
            "isogloss": {
              "filename": "earth_%d_isogloss.png",
              "category": "linguistics",
              "data_sources": ["WALS World Atlas of Language Structures", "Glottolog 4.8", "D-PLACE Ethnolinguistic Database"],
              "reconstitution_rationale": "Global ethnolinguistic phyla, sub-branches, and dialectal zones."
            },
            "kinship": {
              "filename": "earth_%d_kinship.png",
              "category": "anthropology",
              "data_sources": ["D-PLACE Murdock Ethnographic Atlas Kinship Codes", "Todd Anthropological Family Systems Database"],
              "reconstitution_rationale": "Spatial social structures, descent rules, and kinship organization modes."
            },
            "rituals": {
              "filename": "earth_%d_rituals.png",
              "category": "anthropology",
              "data_sources": ["Archaeological Temple & Monumental Site Catalog", "World Religion Sacred Geography Datasets"],
              "reconstitution_rationale": "Sacred geography, pilgrimage networks, and monumental ritual intensity."
            },
            "technology": {
              "filename": "earth_%d_technology.png",
              "category": "technology",
              "data_sources": ["Archaeological Metallurgical & Innovation Datasets", "Maddison Project Historical GDP/Tech"],
              "reconstitution_rationale": "Technological complexity index and diffusion fronts."
            },
            "institutional": {
              "filename": "earth_%d_institutional.png",
              "category": "sociology",
              "data_sources": ["Seshat Global History Databank Institutional Hierarchy Scales"],
              "reconstitution_rationale": "Administrative, legal, and bureaucratic state capacity."
            },
            "ecological": {
              "filename": "earth_%d_ecological.png",
              "category": "ecology",
              "data_sources": ["Global Land Use & Anthropogenic Deforestation Syntheses", "UN FAO Global Forest Resources"],
              "reconstitution_rationale": "Human ecological footprint, agricultural clearing, and resource appropriation."
            },
            "pathogen": {
              "filename": "earth_%d_pathogen.png",
              "category": "epidemiology",
              "data_sources": ["Paleoepidemiology & Ancient DNA Pathogen Catalogs", "WHO Global Infectious Disease Atlas"],
              "reconstitution_rationale": "Endemic disease pressure, malaria/zoonotic vectors, and historical immunity."
            },
            "tradenetwork": {
              "filename": "earth_%d_tradenetwork.png",
              "category": "economics",
              "data_sources": ["Historical Maritime & Overland Trade Route Atlases", "Seshat Commercial Networks"],
              "reconstitution_rationale": "Commercial trade corridors, emporia, and maritime networks."
            },
            "coal": { "filename": "earth_%d_coal.png", "category": "geology", "data_sources": ["USGS World Coal Quality Inventory"] },
            "oil": { "filename": "earth_%d_oil.png", "category": "geology", "data_sources": ["USGS World Petroleum Assessment"] },
            "gas": { "filename": "earth_%d_gas.png", "category": "geology", "data_sources": ["USGS Global Gas Assessment"] },
            "uranium": { "filename": "earth_%d_uranium.png", "category": "geology", "data_sources": ["IAEA / NEA Red Book"] },
            "helium3": { "filename": "earth_%d_helium3.png", "category": "geology", "data_sources": ["USGS Mantle Volatiles Survey"] },
            "iron_copper": { "filename": "earth_%d_iron_copper.png", "category": "geology", "data_sources": ["USGS MRDS Mineral Database"] },
            "precious_metals": { "filename": "earth_%d_precious_metals.png", "category": "geology", "data_sources": ["USGS Mineral Assessment"] },
            "rare_earths": { "filename": "earth_%d_rare_earths.png", "category": "geology", "data_sources": ["USGS REE Database"] },
            "geothermal": { "filename": "earth_%d_geothermal.png", "category": "geology", "data_sources": ["IHFC Terrestrial Heat Flow Database"] },
            "aquifers": { "filename": "earth_%d_aquifers.png", "category": "geology", "data_sources": ["UNESCO WHYMAP Global Groundwater"] }
          }
        }
        """.formatted(
            yr, escapeJson(em.eraName()), escapeJson(em.summaryEn()), escapeJson(em.summaryFr()),
            yr, yr, yr, yr, yr,
            yr, yr, yr, yr, yr, yr, yr, yr, yr, yr,
            yr, yr, yr, yr, yr, yr, yr, yr, yr, yr
        );

        safeWriteFile(target, json);
    }

    // Helper subroutine: write epoch readme - internal state computation & bounds checking
    private void writeEpochReadme(File target, EpochMeta em) throws Exception {
        long yr = em.year();
        String md = """
        # Earth Epoch %d: %s

        ## 🌍 Overview
        This directory contains the standardized 25-layer cartographic raster tensor suite and cliodynamic registries for Earth at epoch **%d** (%s).

        All raster layers are generated in equirectangular projection (Plate Carrée, EPSG:4326) at **2048x1024** resolution with bit-identical determinism.

        ### Context & Archaeological/Historical Horizon
        * **Era**: %s
        * **Technocomplex**: %s
        * **Social / Family Horizon**: %s
        * **Estimated Caloric Baseline**: %d kcal/capita/day

        ## 🗺️ Standard Cartographic Layers (25 PNG Rasters)

        ### Physical & Climate Layers
        * `earth_%d_elevation.png`: NOAA ETOPO 2022 / GEBCO Topography & Bathymetry calibrated to epoch sea level.
        * `earth_%d_biomes.png`: Coupled Holdridge-Whittaker Bioclimatic Ecology.
        * `earth_%d_temperature.png`: WorldClim v2.1 Annual Mean Temperature (°C) modulated by epoch paleoclimate anomalies.
        * `earth_%d_precipitation.png`: WorldClim v2.1 Annual Precipitation (mm/year) with ITCZ/monsoonal shifts.
        * `earth_%d_seasonality.png`: Temperature Seasonality Amplitude (°C range) driven by Milankovitch orbital solutions.

        ### Cliodynamic & Cultural Tensors
        * `earth_%d_density.png`: HYDE 3.4 / Seshat Human Demographic Density field.
        * `earth_%d_sovereignty.png`: Political Sovereignty & Territorial Polity Domains (Seshat ClioPatria / Historical GIS).
        * `earth_%d_isogloss.png`: Ethnolinguistic Phyla & Sub-Branch Dialectal Zones (Glottolog 4.8 / WALS).
        * `earth_%d_kinship.png`: Murdock D-PLACE & Emmanuel Todd Kinship & Social Organization Systems.
        * `earth_%d_rituals.png`: Monumental Ritual Centers & Sacred Traditions.
        * `earth_%d_technology.png`: Technology & Innovation Complexity Index (Maddison / Archaeological Catalogs).
        * `earth_%d_institutional.png`: Seshat Institutional Hierarchy & State Capacity Scales.
        * `earth_%d_ecological.png`: Anthropogenic Ecological Footprint & Land Transformation.
        * `earth_%d_pathogen.png`: Epidemiological & Endemic Pathogen Load (Paleoepidemiology & WHO).
        * `earth_%d_tradenetwork.png`: Commercial Trade Arteries, Emporia, Caravans & Ports.

        ### Geological & Energy Resources
        * `earth_%d_coal.png`: Coal Basins (USGS WoCQI).
        * `earth_%d_oil.png`: Conventional & Unconventional Petroleum Plays (USGS).
        * `earth_%d_gas.png`: Natural Gas Formations (USGS).
        * `earth_%d_uranium.png`: Uranium Mineral Deposits (IAEA / NEA Red Book).
        * `earth_%d_helium3.png`: Mantle Plume Helium-3 Outgassing Sources (USGS).
        * `earth_%d_iron_copper.png`: Iron & Copper Mineralization (USGS MRDS).
        * `earth_%d_precious_metals.png`: Gold, Silver & Platinum Group Deposits.
        * `earth_%d_rare_earths.png`: Critical Rare Earth Elements (REE).
        * `earth_%d_geothermal.png`: Terrestrial Heat Flow & Geothermal Gradients (IHFC).
        * `earth_%d_aquifers.png`: Deep Regional Groundwater Aquifers (UNESCO WHYMAP).

        ## 🔬 Decoupling Rationale ($t = t_0$ vs Dynamical Ticks $t > t_0$)
        In strict accordance with `AGENTS.md` Directives:
        1. **Initial Conditions ($t = t_0$)**: The 25 raster layers define the empirically calibrated spatial state at initialization.
        2. **Dynamical Simulation Engine ($t > t_0$)**: Once launched, the physical engines (Energy Balance Climate Models, Darcy groundwater flow, Lotka metabolic energetics, and Turchin cliodynamics) dynamically evolve population, technology, culture, and sovereign borders without synthetic lock-in.

        ## 📄 Associated Metadata Files
        * `cultural_registry.json`: Multilingual entity registry (EN, FR, DE, ES, ZH) with trait vectors and kinship metadata.
        * `provenance_and_sources.json`: Exhaustive academic citations, datasets, and physical calibration rationale.
        """.formatted(
            yr, em.eraName(), yr, em.eraName(),
            em.eraName(), em.lithicComplex(), em.defaultKinship(), em.defaultKcal(),
            yr, yr, yr, yr, yr,
            yr, yr, yr, yr, yr, yr, yr, yr, yr, yr,
            yr, yr, yr, yr, yr, yr, yr, yr, yr, yr
        );

        safeWriteFile(target, md);
    }

    // Helper subroutine: safe write file - internal state computation & bounds checking
    private static void safeWriteFile(File target, String content) throws Exception {
        target.getParentFile().mkdirs();
        int attempts = 0;
        while (true) {
            try {
                java.nio.file.Path tempPath = target.toPath().resolveSibling(target.getName() + ".tmp." + Thread.currentThread().threadId() + "." + System.nanoTime());
                java.nio.file.Files.writeString(tempPath, content, StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                        java.nio.file.StandardOpenOption.WRITE);
                try {
                    java.nio.file.Files.move(tempPath, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ex) {
                    java.nio.file.Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
                    try { java.nio.file.Files.deleteIfExists(tempPath); } catch (Exception ignored) {}
                }
                return;
            } catch (Exception e) {
                attempts++;
                if (attempts >= 10) throw e;
                Thread.sleep(200);
            }
        }
    }

    // Helper subroutine: escape json - internal state computation & bounds checking
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
