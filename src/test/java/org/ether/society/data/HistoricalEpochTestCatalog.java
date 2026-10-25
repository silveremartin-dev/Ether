/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot & Gemini AI
 */
package org.ether.society.data;

import java.util.*;

/**
 * Historical Epoch Test Catalog defining metadata anchors and canonical timelines for 36 epochs.
 */
public final class HistoricalEpochTestCatalog {

    private HistoricalEpochTestCatalog() {}


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
    // Epoch registry Ã¢â‚¬â€ sorted ascending by year.
    // Original 36 entries are kept verbatim; interpolated grid entries fill gaps.
    // ---------------------------------------------------------------------------
    public static final List<EpochMeta> EPOCHS;
    static {
        List<EpochMeta> _list = new ArrayList<>();

        // -----------------------------------------------------------------------
        // Helper lambdas Ã¢â‚¬â€ linear interpolation utilities defined via anonymous
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
            if (yr <= -70000) return String.format("Des bandes d'Homo sapiens archaÃƒÂ¯ques se dÃƒÂ©placent dans les refuges africains durant le stade glaciaire MIS ÃƒÂ  %,d BP, avec des dispersions pionniÃƒÂ¨res vers les corridors afro-arabiques.", Math.abs(yr));
            if (yr <= -40000) return String.format("Les chasseurs-cueilleurs du PalÃƒÂ©olithique supÃƒÂ©rieur s'ÃƒÂ©tendent ÃƒÂ  travers l'Eurasie ÃƒÂ  %,d BP, ÃƒÂ©tablissant des industries laminaires, l'art pariÃƒÂ©tal et des traversÃƒÂ©es maritimes vers le Sahul.", Math.abs(yr));
            if (yr <= -20000) return String.format("Les conditions du Dernier Maximum Glaciaire prÃƒÂ©valent ÃƒÂ  %,d BP ; des refuges de chasseurs-cueilleurs persistent le long des corridors libres de glace.", Math.abs(yr));
            if (yr <= -10000) return String.format("Les chasseurs-cueilleurs du PlÃƒÂ©istocÃƒÂ¨ne terminal ÃƒÂ  %,d BP rÃƒÂ©coltent des cÃƒÂ©rÃƒÂ©ales sauvages au Levant et initient des campements sÃƒÂ©dentaires prÃƒÂ©cÃƒÂ©dant la transition nÃƒÂ©olithique.", Math.abs(yr));
            if (yr <= -5000) return String.format("Les communautÃƒÂ©s agropastorales du dÃƒÂ©but de l'HolocÃƒÂ¨ne ÃƒÂ  %,d BP cultivent l'ÃƒÂ©peautre et l'amidonnier dans le Croissant fertile, tandis que le dÃƒÂ©frichage forestier s'ÃƒÂ©tend en Europe tempÃƒÂ©rÃƒÂ©e.", Math.abs(yr));
            if (yr <= -3000) return String.format("Les citÃƒÂ©s proto-urbaines chalcolithiques de %,d av. J.-C. dÃƒÂ©veloppent la fonte du cuivre, les canaux d'irrigation et la poterie tournÃƒÂ©e au Proche-Orient et dans les Balkans.", Math.abs(yr));
            if (yr <= -1000) return String.format("Les Ãƒâ€°tats lettrÃƒÂ©s de l'ÃƒÂ¢ge du bronze de %,d av. J.-C. organisent des armÃƒÂ©es de chars, des ÃƒÂ©conomies palaciales et le commerce maritime ÃƒÂ  longue distance.", Math.abs(yr));
            if (yr <= -500) return String.format("La mÃƒÂ©tallurgie du fer se rÃƒÂ©pand ÃƒÂ  %,d av. J.-C., favorisant l'expansion nÃƒÂ©o-assyrienne, la diffusion de l'alphabet phÃƒÂ©nicien et la consolidation fÃƒÂ©odale des Zhou.", Math.abs(yr));
            if (yr <= 0) return String.format("L'AntiquitÃƒÂ© classique de %d ap. J.-C. voit l'administration provinciale romaine, la bureaucratie Han et les ÃƒÂ©changes intercontinentaux sur la Route de la Soie.", yr);
            if (yr <= 500) return String.format("Les sociÃƒÂ©tÃƒÂ©s tardo-antiques de %d ap. J.-C. naviguent entre fragmentation impÃƒÂ©riale, pandÃƒÂ©mies et montÃƒÂ©e des royaumes post-romains.", yr);
            if (yr <= 1000) return String.format("MÃƒÂ©diÃƒÂ©val %d ap. J.-C. : expansion agraire, construction de cathÃƒÂ©drales, rÃƒÂ©volution commerciale Song et rÃƒÂ©seaux diasporiques intercontinentaux.", yr);
            if (yr <= 1500) return String.format("Les Ãƒâ€°tats mÃƒÂ©diÃƒÂ©vaux tardifs de %d ap. J.-C. dÃƒÂ©ploient chevalerie montÃƒÂ©e, rÃƒÂ©seaux castraux et capitalisme marchand corporatif dans les corridors commerciaux eurasiatiques.", yr);
            if (yr <= 1800) return String.format("Les Ãƒâ€°tats de l'ÃƒÂ©poque moderne de %d ap. J.-C. gouvernent des empires maritimes, des compagnies ÃƒÂ  charte et des acadÃƒÂ©mies scientifiques dans un proto-capitalisme proto-industriel.", yr);
            if (yr <= 1900) return String.format("L'industrialisation de %d ap. J.-C. voit les usines ÃƒÂ  vapeur alimentÃƒÂ©es au charbon, les chemins de fer transcontinentaux et le tÃƒÂ©lÃƒÂ©graphe remodeler l'ordre ÃƒÂ©conomique mondial.", yr);
            if (yr <= 1950) return String.format("Le XXe siÃƒÂ¨cle de %d ap. J.-C. est marquÃƒÂ© par l'ÃƒÂ©lectrification, le moteur ÃƒÂ  combustion, la synthÃƒÂ¨se chimique et deux guerres mondiales accÃƒÂ©lÃƒÂ©rant le changement technologique.", yr);
            if (yr <= 2000) return String.format("L'aprÃƒÂ¨s-guerre de %d ap. J.-C. intÃƒÂ¨gre les transistors, les communications par satellite, la rÃƒÂ©volution verte et l'ÃƒÂ©change mondial d'informations par ordinateur.", yr);
            return String.format("Le futur proche de %d ap. J.-C. : des rÃƒÂ©seaux d'ÃƒÂ©nergie renouvelable planÃƒÂ©taires, des systÃƒÂ¨mes autonomes et des technologies d'adaptation climatique dÃƒÂ©finissent la civilisation mondiale ÃƒÂ©mergente.", yr);
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
        // RANGE 1: -100,000 to -30,000 BP Ã¢â‚¬â€ every 5,000 years
        // -----------------------------------------------------------------------
        for (long y = -100000L; y <= -30000L; y += 5000L) addInterpolated.accept(y);

        // -----------------------------------------------------------------------
        // RANGE 2: -30,000 to -10,000 BP Ã¢â‚¬â€ every 1,000 years
        // -----------------------------------------------------------------------
        for (long y = -30000L; y <= -10000L; y += 1000L) addInterpolated.accept(y);

        // -----------------------------------------------------------------------
        // RANGE 3: -10,000 to -100 Ã¢â‚¬â€ every 100 years (year 0 excluded)
        // -----------------------------------------------------------------------
        for (long y = -10000L; y <= -100L; y += 100L) addInterpolated.accept(y);

        // -----------------------------------------------------------------------
        // RANGE 4: 0 to 2060 Ã¢â‚¬â€ every 20 years
        // -----------------------------------------------------------------------
        for (long y = 0L; y <= 2060L; y += 20L) addInterpolated.accept(y);

        // -----------------------------------------------------------------------
        // ORIGINAL 36 ENTRIES Ã¢â‚¬â€ kept verbatim; duplicates are overridden by
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
            "Reconstitution des bandes d'Homo sapiens du Middle Stone Age en Afrique et des premiÃƒÂ¨res dispersions pionniÃƒÂ¨res vers le Levant et l'Arabie durant l'interglaciaire ÃƒÂ©ÃƒÂ©mien (MIS 5e).",
            "Middle Stone Age (MSA) Blade & Point Technocomplex, Pigment Use & Marine Foraging", "Egalitarian Forager Multi-Family Band", 2300));

        _list.add(new EpochMeta(-74000L, "Toba Super-Eruption & Genetic Bottleneck Horizon (-74,000 BP)", "ONE_CONTINENT", 0.6, 20_000L,
            "Reconstruction of global hominin refugia following the Youngest Toba Tuff super-eruption and rapid volcanic winter onset.",
            "Reconstitution des refuges d'hominines suite ÃƒÂ  la super-ÃƒÂ©ruption du Toba et au refroidissement volcanique abrupt.",
            "Late Middle Stone Age Microlithic Hearths & Coastal Shellfish Exploitation", "Refugial Forager Kin Networks", 2200));

        _list.add(new EpochMeta(-50000L, "Upper Paleolithic Revolution & Sahul Colonization (-50,000 BP)", "SAHUL_MIGRATION", 0.8, 200_000L,
            "Global Upper Paleolithic expansion of behavioral modernity, blade technology, cave art, and maritime colonization of Sahul (Australia/New Guinea).",
            "Expansion mondiale du PalÃƒÂ©olithique supÃƒÂ©rieur, art pariÃƒÂ©tal, dÃƒÂ©bitage laminaire et colonisation maritime du Sahul (Australie/Nouvelle-GuinÃƒÂ©e).",
            "Early Upper Paleolithic Blade & Bone Tool Industries, Ochre Art & Ocean Crossings", "Exogamous Clan Bands & Subsection Systems", 2400));

        _list.add(new EpochMeta(-25000L, "Last Glacial Maximum & Solutrean/Gravettian Mammoth Steppe (-25,000 BP)", "BERINGIA_AMERICAS", 1.0, 500_000L,
            "Peak glacial climate regime with extensive ice sheets, exposed continental shelves, Solutrean pressure flaking, and Gravettian mammoth hunter encampments.",
            "RÃƒÂ©gime glaciaire maximal avec calottes polaires ÃƒÂ©tendues, plateaux continentaux ÃƒÂ©mergÃƒÂ©s, retouche par pression solutrÃƒÂ©enne et campements gravettiens.",
            "Solutrean / Gravettian Pressure-Flaked Lithics, Tailored Fur Clothing & Portable Art", "Aggregation Band Networks & Seasonal Macro-Bands", 2500));

        _list.add(new EpochMeta(-20000L, "Last Glacial Maximum Peak & Beringian Standstill (-20,000 BP)", "BERINGIA_AMERICAS", 1.0, 600_000L,
            "Glacial maximum nadir with maritime standstill in Beringia, Epigravettian Mediterranean refugia, and Kebaran bladelet industries in the Levant.",
            "Nadir du maximum glaciaire avec pause beringienne, refuges ÃƒÂ©pigravettiens mÃƒÂ©diterranÃƒÂ©ens et industries kÃƒÂ©bariennes au Levant.",
            "Epigravettian & Kebaran Microlithic Bladelets, Mammoth Bone Dwellings", "Territorial Foraging Bands & Base Camps", 2500));

        _list.add(new EpochMeta(-10900L, "Younger Dryas Abrupt Cooling & Proto-Natufian Foragers (-10,900 BP / 8900 BC)", "YOUNGER_DRYAS", 1.2, 2_000_000L,
            "Abrupt hemispheric cooling trigger during Younger Dryas; sedentary Natufian hunter-gatherers, wild cereal harvesting, and stone mortar storage.",
            "Refroidissement abrupt du Dryas rÃƒÂ©cent ; chasseurs-cueilleurs sÃƒÂ©dentaires natoufiens, rÃƒÂ©colte de cÃƒÂ©rÃƒÂ©ales sauvages et mortiers en pierre.",
            "Natufian Microlithic Sickles, Stone Mortars & Semi-Subterranean Circular Huts", "Sedentary Hamlet Co-Residential Lineages", 2400));

        _list.add(new EpochMeta(-10000L, "Early Holocene & Fertile Crescent Pre-Pottery Neolithic (-10,000 BP / 8000 BC)", "NEOLITHIC", 1.5, 5_000_000L,
            "Transition to agriculture and domestication of emmer, einkorn, barley, goats, and sheep in the Fertile Crescent (GÃƒÂ¶bekli Tepe, Jericho, Ãƒâ€¡ayÃƒÂ¶nÃƒÂ¼).",
            "Transition vers l'agriculture et domestication des cÃƒÂ©rÃƒÂ©ales et caprinÃƒÂ©s dans le Croissant fertile (GÃƒÂ¶bekli Tepe, JÃƒÂ©richo, Ãƒâ€¡ayÃƒÂ¶nÃƒÂ¼).",
            "Pre-Pottery Neolithic A/B (PPNA/PPNBP) Ground Stone Axes, Mudbrick Architecture & Cult Centers", "Patrilocal Extended Peasant Households & Shrine Sodalities", 2500));

        _list.add(new EpochMeta(-8000L, "Neolithic Agricultural Expansion & Green Sahara (-8,000 BP / 6000 BC)", "GREEN_SAHARA", 1.8, 10_000_000L,
            "Cardial and Linear Pottery (LBK) farming expansion across Europe, Yangshao millet farming in China, and pastoral lacustrine cultures across the Green Sahara.",
            "Expansion agricole rubanÃƒÂ©e et cardiale en Europe, culture du millet Yangshao en Chine et pastoralisme lacustre au Sahara vert.",
            "Cardial / LBK Pottery, Polished Stone Adzes, Pastoral Cattle Corrals & Longhouses", "Segmentary Peasant Lineages & Village Communes", 2550));

        _list.add(new EpochMeta(-6000L, "Mid-Holocene Climatic Optimum & VinÃ„Âa / Ubaid Proto-Urbanism (-6,000 BP / 4000 BC)", "EGYPT_NILE", 2.0, 20_000_000L,
            "Proto-urban tell settlements in Mesopotamia (Ubaid period), Copper metallurgy in Balkans (VinÃ„Âa, Varna gold), and Majiabang/Hemudu wet-rice farming in the Yangtze.",
            "Proto-urbanisme mÃƒÂ©sopotamien (pÃƒÂ©riode d'ObeÃƒÂ¯d), mÃƒÂ©tallurgie du cuivre dans les Balkans (VinÃ„Âa, Varna) et riziculture de Majiabang/Hemudu.",
            "Copper Smelting, Wheel-Thrown Pottery, Mudbrick Temples & Irrigation Canals", "Corporate Lineage Estates & Temple Chiefdoms", 2600));

        _list.add(new EpochMeta(-3000L, "Early Bronze Age, Uruk Expansion & First Dynastic Egypt (-3000 BC)", "MESOPOTAMIA_ASSYRIA", 2.2, 45_000_000L,
            "Emergence of the state, archaic cuneiform writing, bronze metallurgy, monumental ziggurats and pyramids in Uruk Sumer, Early Dynastic Egypt, and Liangzhu China.",
            "Ãƒâ€°mergence de l'Ãƒâ€°tat, ÃƒÂ©criture cunÃƒÂ©iforme archaÃƒÂ¯que, mÃƒÂ©tallurgie du bronze et architecture monumentale ÃƒÂ  Sumer, en Ãƒâ€°gypte et ÃƒÂ  Liangzhu.",
            "Alloyed Bronze Tools, Cuneiform/Hieroglyphic Administration, Ox-Drawn Plows & Monumental Masonry", "Patriarchal Temple-Palace Dynasties & Redistributive Estates", 2650));

        _list.add(new EpochMeta(-1900L, "Middle Bronze Age & Indus Valley Urban Peak / Harappan Epoch (-1900 BC)", "INDIA_MAURYA", 2.4, 70_000_000L,
            "Mature Harappan urbanism with standardized grid planning, hydraulic drainage, Minoan palaces in Crete, Middle Kingdom Egypt, and Xia/Erlitou China.",
            "Urbanisme harappÃƒÂ©en planifiÃƒÂ© avec drainage hydraulique, palais minoens en CrÃƒÂ¨te, Moyen Empire ÃƒÂ©gyptien et culture d'Erlitou.",
            "Standardized Mudbrick Urban Architecture, Bronze Casting & Maritime Trade Docks", "Civic Guilds, Corporate Priesthoods & Extended Joint Families", 2650));

        _list.add(new EpochMeta(-1500L, "Late Bronze Age International System & Shang Dynasty (-1500 BC)", "SONG_DYNASTY", 2.5, 90_000_000L,
            "Amarna diplomacy era connecting New Kingdom Egypt, Hittites, Mittani, Mycenae, and Shang Dynasty bronze ritual state in China.",
            "SystÃƒÂ¨me diplomatique international de l'ÃƒÂ¢ge du bronze reliant l'Ãƒâ€°gypte du Nouvel Empire, les Hittites, MycÃƒÂ¨nes et les Shang en Chine.",
            "Chariot Warfare, Advanced Bronze Piece-Mold Casting, Oracle Bone Script & International Maritime Trade", "Agnatic Dynastic Lineages, Royal Palace Estates & CorvÃƒÂ©e Labor", 2700));

        _list.add(new EpochMeta(-1200L, "Late Bronze Age Collapse & Sea Peoples Horizon (-1200 BC)", "MESOPOTAMIA_ASSYRIA", 2.5, 80_000_000L,
            "Systemic Eastern Mediterranean collapse, destruction of Mycenaean and Hittite palaces, Sea Peoples incursions, and transition to Iron Age decentralization.",
            "Effondrement systÃƒÂ©mique de l'ÃƒÂ¢ge du bronze en MÃƒÂ©diterranÃƒÂ©e orientale, fin des palais mycÃƒÂ©niens et hittites et transition vers le fer.",
            "Early Wrought Iron Forging, Ashlar Fortifications & Dispersed Agrarian Homesteads", "Segmentary Warrior Kin-Groups & Decentralized Village Enclaves", 2600));

        _list.add(new EpochMeta(-1000L, "Early Iron Age & Neo-Assyrian / Zhou Dynasty Emergence (-1000 BC)", "MESOPOTAMIA_ASSYRIA", 2.6, 100_000_000L,
            "Widespread bloomery iron metallurgy, Neo-Assyrian military expansion, Western Zhou feudalism (Fengjian), and Phoenician alphabet diffusion.",
            "MÃƒÂ©tallurgie du fer au bas-fourneau, expansion militaire nÃƒÂ©o-assyrienne, fÃƒÂ©odalitÃƒÂ© des Zhou occidentaux et diffusion de l'alphabet phÃƒÂ©nicien.",
            "Iron Weapons, Ashlar Fortifications, PhÃƒÂ©nician Maritime Galleys & Alphabetic Writing", "Patriarchal Aristocratic Lineages & Feudal Estates", 2700));

        _list.add(new EpochMeta(-334L, "Classical Antiquity & Alexander's Hellenistic Expansion (-334 BC)", "ROMAN_EMPIRE", 3.0, 150_000_000L,
            "Alexander the Great's conquest of the Achaemenid Persian Empire, synthesis of Greek and Near Eastern civilizations, and Warring States China.",
            "ConquÃƒÂªte de l'Empire perse par Alexandre le Grand, synthÃƒÂ¨se hellÃƒÂ©nistique et pÃƒÂ©riode des Royaumes combattants en Chine.",
            "Iron Pikes (Sarissa), Siege Catapults, Hellenistic Urban Grid Planning & Monetal Coinage", "Civic Polis Citizen Assemblies & Imperial Administrative Bureaucracy", 2750));

        _list.add(new EpochMeta(-300L, "Hellenistic Kingdoms, Maurya Empire & Warring States (-300 BC)", "INDIA_MAURYA", 3.0, 160_000_000L,
            "Ptolemaic and Seleucid kingdoms, Ashoka's Maurya Empire in India, and late Warring States consolidation under Qin.",
            "Royaumes ptolÃƒÂ©maÃƒÂ¯que et sÃƒÂ©leucide, empire Maurya d'Ashoka en Inde et fin des Royaumes combattants en Chine.",
            "Iron Agricultural Implements, Hydraulic Canals, Ashokan Edicts & Monometallic Silver Standards", "Joint Family Households, Caste Guilds (Jati) & Imperial Bureaucracy", 2750));

        _list.add(new EpochMeta(0L, "Pax Romana, Han Empire & Classical Axial Age (1 AD)", "ROMAN_EMPIRE", 3.2, 250_000_000L,
            "High Classical antiquity: Roman Empire across the Mediterranean basin, Western Han Dynasty in China, Kushan Empire, and Parthia.",
            "Haute AntiquitÃƒÂ© classique : Empire romain en MÃƒÂ©diterranÃƒÂ©e, dynastie des Han occidentaux en Chine, Empire kouchan et Parthie.",
            "Roman Concrete (Opus Caementicium), Aqueducts, Silk Road Caravans, Han Blast Furnaces & Watermills", "Paterfamilias Roman Household & Confucian Patrilineal Clan", 2800));

        _list.add(new EpochMeta(536L, "Extreme Climate Event of 536 AD & Late Antique Little Ice Age (536 AD)", "ROMAN_EMPIRE", 3.1, 230_000_000L,
            "Volcanic dust veil event of 536 AD, Justinianic Plague pandemic, Sasanian-Byzantine wars, and Southern/Northern Dynasties China.",
            "Voile de poussiÃƒÂ¨re volcanique de 536, peste de Justinien, guerres perso-byzantines et dynasties du Nord et du Sud en Chine.",
            "Heavy Moldboard Plows, Fortified Castra, Blast-Furnace Cast Iron & Monastic Scriptoria", "Feudal Agrarian Colonate & Patrilineal Aristocratic Clans", 2700));

        _list.add(new EpochMeta(632L, "Early Islamic Expansion & Tang Dynasty Consolidation (632 AD)", "FERTILE_CRESCENT", 3.3, 240_000_000L,
            "Founding of the Rashidun Caliphate, rapid Middle Eastern expansion, Tang Dynasty reunification of China, and emergence of Srivijaya.",
            "Fondation du Califat des Rachidoune, expansion islamique au Moyen-Orient, rÃƒÂ©unification Tang de la Chine et ÃƒÂ©mergence de Srivijaya.",
            "Qanat Irrigation Engineering, Arabian Camel Caravans, Tang Woodblock Printing & Damascene Steel", "Segmentary Arab Patrilineages & Confucian Imperial Meritocracy", 2800));

        _list.add(new EpochMeta(1000L, "Medieval Climate Optimum, Song Dynasty & Norse Expansion (1000 AD)", "SONG_DYNASTY", 3.5, 300_000_000L,
            "Medieval Warm Period: Song Dynasty commercial revolution, Fatimid Cairo, Holy Roman Empire, Chola naval supremacy, and Norse Atlantic voyages.",
            "Optimum climatique mÃƒÂ©diÃƒÂ©val : rÃƒÂ©volution commerciale Song, Le Caire fatimide, Saint-Empire, suprÃƒÂ©matie navale Chola et voyages vikings.",
            "Movable Type Printing, Magnetic Compass, Gunpowder Formulas, Double-Cropping Champa Rice & Windmills", "Manorial Feudal Serfdom, Stem Households & Lineage Halls", 2850));

        _list.add(new EpochMeta(1206L, "Mongol World Empire Formation & High Medieval Crusades (1206 AD)", "SONG_DYNASTY", 3.6, 360_000_000L,
            "Coronation of Genghis Khan, unification of Steppe tribes, Pax Mongolica trans-Eurasian trade routes, and High Medieval cathedrals.",
            "Couronnement de Gengis Khan, unification des tribus de la steppe, Pax Mongolica et grandes cathÃƒÂ©drales mÃƒÂ©diÃƒÂ©vales.",
            "Composite Reflex Bows, Trebuchet Siegecraft, Yam Postal Relay Stations & Paper Money (Jiaochao)", "Nomadic Steppe Clan Federations (Otog) & Feudal Estates", 2850));

        _list.add(new EpochMeta(1324L, "Mansa Musa's Pilgrimage & High Middle Ages (1324 AD)", "WEST_AFRICA_MALI", 3.7, 420_000_000L,
            "Height of the Mali Empire, trans-Saharan gold-salt trade, Yuan Dynasty in China, Delhi Sultanate in India, and Italian Renaissance city-states.",
            "ApogÃƒÂ©e de l'Empire du Mali, commerce transsaharien de l'or, dynastie Yuan en Chine, sultanat de Delhi et citÃƒÂ©s-Ãƒâ€°tats italiennes.",
            "Gothic Stone Vaulting, Astrolabes, Blast Furnaces, Trans-Saharan Caravans & Portolan Charts", "Guild Merchant Families, African Segmentary Lineages & Feudal Manors", 2900));

        _list.add(new EpochMeta(1347L, "Black Death Pandemic & Fourteenth-Century Crisis (1347 AD)", "SONG_DYNASTY", 3.6, 370_000_000L,
            "Arrival of Yersinia pestis in Mediterranean ports, massive demographic contraction across Eurasia, and subsequent wage labor restructuring.",
            "ArrivÃƒÂ©e de la peste noire en MÃƒÂ©diterranÃƒÂ©e, effondrement dÃƒÂ©mographique en Eurasie et restructuration du salariat agricole.",
            "Full-Rigged Carracks, Mechanical Clock Towers, Heavy Trebuchets & Early Cannons", "Agrarian Peasant Households & Wage Labor Restructuring", 2800));

        _list.add(new EpochMeta(1491L, "Pre-Columbian Americas & Eurasian Renaissance Eve (1491 AD)", "AMERICAS_1491", 3.8, 500_000_000L,
            "Complex indigenous states in the Americas (Triple Alliance Aztec Empire, Inca Tawantinsuyu, Mississippian centers) on the eve of European contact.",
            "Ãƒâ€°tats indigÃƒÂ¨nes complexes dans les AmÃƒÂ©riques (Empire aztÃƒÂ¨que, Tawantinsuyu inca, citÃƒÂ©s mississippiennes) ÃƒÂ  la veille du contact europÃƒÂ©en.",
            "Chinampa Intensive Wetland Farming, Incan Quipu & Road Network, Bronze/Arquebus Arms in Eurasia", "Ayllu Dual Reciprocity, Calpulli Wards & European Manorial Households", 2850));

        _list.add(new EpochMeta(1492L, "Columbian Exchange & Age of Discovery (1492 AD)", "EPIDEMIC_CONTACT", 3.8, 500_000_000L,
            "Columbus transatlantic landfall, inception of the global Columbian Exchange of crops, animals, and pathogens, and Ming Dynasty maritime trade.",
            "ArrivÃƒÂ©e transatlantique de Colomb, dÃƒÂ©but de l'ÃƒÂ©change colombien (plantes, animaux, pathogÃƒÂ¨nes) et commerce maritime Ming.",
            "Oceanic Caravels & Carracks, Navigational Astronomy, Early Cast Iron Artillery & Gutenberg Movable Print", "Iberian HidalguÃƒÂ­a, Joint-Stock Proto-Enterprises & Indigenous Moieties", 2850));

        _list.add(new EpochMeta(1639L, "Sakoku Japan, Thirty Years' War & Ming-Qing Transition (1639 AD)", "SAKOKU_JAPAN", 4.2, 580_000_000L,
            "Tokugawa Shogunate Sakoku edicts, Thirty Years' War in Europe, Ming-Qing transition in China, and Mughal architectural golden age.",
            "Ãƒâ€°dits de fermeture Sakoku des Tokugawa, guerre de Trente Ans en Europe, transition Ming-Qing et ÃƒÂ¢ge d'or moghol.",
            "Flintlock Muskets, Galleons & Fluyts, Scientific Revolution Telescopes & Early Joint-Stock Companies", "Ie Stem Family System, Western European Nuclear Households & Agnatic Lineages", 2900));

        _list.add(new EpochMeta(1800L, "First Industrial Revolution & Global Napoleonic / Imperial Era (1800 AD)", "INDUSTRIAL", 5.0, 1_000_000_000L,
            "Steam engine industrialization, mechanical textile mills, Napoleonic administrative codification, Qing demographic apex, and Atlantic Revolutions.",
            "Industrialisation ÃƒÂ  la vapeur, filatures mÃƒÂ©caniques, codification napolÃƒÂ©onienne, apogÃƒÂ©e dÃƒÂ©mographique Qing et rÃƒÂ©volutions atlantiques.",
            "Watt Steam Engines, Puddling Iron Metallurgy, Power Looms, Canals & Semaphoric Telegraphs", "Industrial Wage-Labor Proletariat, Egalitarian Nuclear & Stem Families", 3100));

        _list.add(new EpochMeta(1900L, "Second Industrial Revolution & Belle Ãƒâ€°poque Imperial System (1900 AD)", "URBAN_CLUSTERS", 6.0, 1_650_000_000L,
            "Electrification, internal combustion engines, Bessemer steel, transcontinental railways, global telegraph cables, and High Imperialism.",
            "Ãƒâ€°lectrification, moteurs ÃƒÂ  combustion interne, acier Bessemer, chemins de fer transcontinentaux, cÃƒÂ¢bles tÃƒÂ©lÃƒÂ©graphiques et haut impÃƒÂ©rialisme.",
            "Electric Dynamos, Bessemer Steel, Internal Combustion Engines, Submarine Cables & Chemical Synthesis", "Urban Industrial Proletariat & Bourgeois Nuclear Households", 3200));

        _list.add(new EpochMeta(1914L, "Outbreak of World War I & End of Nineteenth-Century Order (1914 AD)", "URBAN_CLUSTERS", 6.2, 1_800_000_000L,
            "Total industrial warfare, global alliance systems, dreadnought battleships, Haber-Bosch nitrogen fixation, and breakdown of the Concert of Europe.",
            "Guerre industrielle totale, systÃƒÂ¨mes d'alliances mondiaux, cuirassÃƒÂ©s dreadnought, procÃƒÂ©dÃƒÂ© Haber-Bosch et fin du concert europÃƒÂ©en.",
            "Haber-Bosch Nitrogen Fixation, Mass Machine Guns, Dreadnoughts, Aircraft & Radio Telephony", "Total Mobilization Nation-States & Industrial Urban Families", 3200));

        _list.add(new EpochMeta(1950L, "Post-WWII Global Reconstruction & Great Acceleration (1950 AD)", "URBAN_CLUSTERS", 7.0, 2_500_000_000L,
            "Post-war Bretton Woods economic order, atomic energy, Green Revolution agrochemicals, mass antibiotics, and the start of the Great Acceleration.",
            "Ordre de Bretton Woods, ÃƒÂ©nergie atomique, rÃƒÂ©volution verte agrochimique, antibiotiques de masse et dÃƒÂ©but de la Grande AccÃƒÂ©lÃƒÂ©ration.",
            "Fission Reactors, Jet Aviation, Transistors, Synthetic Polymers & Industrial Petrochemistry", "Suburban Consumer Nuclear Families & Welfare State Institutions", 3300));

        _list.add(new EpochMeta(2000L, "Turn of the Millennium & Digital Information Age (2000 AD)", "URBAN_CLUSTERS", 8.0, 6_100_000_000L,
            "Global internet expansion, microprocessors, fiber optic telecoms, containerized supply chains, and post-Cold War globalization.",
            "Expansion mondiale d'Internet, microprocesseurs, tÃƒÂ©lÃƒÂ©coms ÃƒÂ  fibre optique, chaÃƒÂ®nes logistiques conteneurisÃƒÂ©es et mondialisation.",
            "VLSI Microprocessors, Global Fiber Optics, GPS Constellations, Containerized Intermodal Logistics", "Post-Industrial Nuclear & Single-Person Households", 3350));

        _list.add(new EpochMeta(2026L, "Anthropocene Present Day & Global Energy Transition (2026 AD)", "URBAN_CLUSTERS", 8.5, 8_150_000_000L,
            "Planetary computing, deep neural networks, renewable energy grids, geopolitical multipolarity, and active climate transition.",
            "Informatique planÃƒÂ©taire, rÃƒÂ©seaux de neurones profonds, rÃƒÂ©seaux d'ÃƒÂ©nergies renouvelables, multipolaritÃƒÂ© et transition climatique.",
            "GPU Compute Clusters, Advanced Photovoltaics, High-Capacity Lithium-Ion Storage & Satellite Megaconstellations", "Diverse Globalized Urban Households & Digital Network Affiliations", 3400));

        _list.add(new EpochMeta(2035L, "Near-Future Demographic Transition & Clean Energy Scaling (2035 AD)", "URBAN_CLUSTERS", 9.0, 8_800_000_000L,
            "Large-scale grid electrification, solid-state batteries, autonomous transport networks, precision fermentation, and demographic stabilization.",
            "Ãƒâ€°lectrification massive des rÃƒÂ©seaux, batteries solides, transports autonomes, fermentation de prÃƒÂ©cision et stabilisation dÃƒÂ©mographique.",
            "Solid-State Storage, Fusion Pilot Plants, Autonomous Robotic Freight & Synthetic Biology Bioreactors", "Flexible Urban Eco-Communities & Automated Labor Households", 3400));

        _list.add(new EpochMeta(2045L, "Mid-Century Climate Adaptation & Automated Labor Transition (2045 AD)", "URBAN_CLUSTERS", 9.3, 9_300_000_000L,
            "Industrial direct air carbon capture, global desalination pipelines, automated agriculture, and space launch reusability.",
            "Capture directe du carbone dans l'air, rÃƒÂ©seaux de dessalement mondiaux, agriculture automatisÃƒÂ©e et rÃƒÂ©utilisabilitÃƒÂ© spatiale.",
            "Gigawatt Direct Air Capture, Commercial Magnetic Fusion, Orbital Space Infrastructure & Closed-Loop Circular Metallurgy", "Universal Basic Infrastructure Communities & Post-Scarcity Nodes", 3450));

        _list.add(new EpochMeta(2050L, "Post-Fossil Equilibrium & Global Demographic Peak (2050 AD)", "URBAN_CLUSTERS", 9.5, 9_700_000_000L,
            "Global demographic inflection point, 100% clean primary energy matrix, planetary geoengineering monitoring, and ecological restoration.",
            "Point d'inflexion dÃƒÂ©mographique mondial, matrice ÃƒÂ©nergÃƒÂ©tique 100% dÃƒÂ©carbonÃƒÂ©e, gÃƒÂ©o-ingÃƒÂ©nierie surveillÃƒÂ©e et restauration ÃƒÂ©cologique.",
            "Planetary Energy Mesh, Deep Geothermal Supercritical Wells, Asteroid Resource Prospecting & Ecosystem Digital Twins", "Regenerative Bioregional Cooperatives & Automated Civil Polities", 3500));

        _list.add(new EpochMeta(2060L, "Planetary Ecological Restoration & Space Industrialization Horizon (2060 AD)", "URBAN_CLUSTERS", 9.8, 9_800_000_000L,
            "Large-scale rewilding, lunar industrial infrastructure, closed-cycle industrial ecosystems, and stable planetary boundary stewardship.",
            "RÃƒÂ©ensauvagement planÃƒÂ©taire, infrastructure industrielle lunaire, cycles industriels fermÃƒÂ©s et gestion stable des limites planÃƒÂ©taires.",
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
}