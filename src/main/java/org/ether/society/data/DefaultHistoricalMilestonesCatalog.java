/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.ClimateEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in fallback catalog of global historical milestones and natural shocks across human history.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class DefaultHistoricalMilestonesCatalog {

    private DefaultHistoricalMilestonesCatalog() {}

    public static List<ClimateEvent> buildComprehensiveCatalog() {
        List<ClimateEvent> list = new ArrayList<>();

        // 1. Paleolithic & Prehistoric Emergence
        list.add(new ClimateEvent("milestone_technology", "Maîtrise Contrôlée du Feu par Homo erectus (Wonderwerk)", -1000000, -27.84, 23.55, 0.0, 9.0));
        list.add(new ClimateEvent("disaster_volcano", "Super-Éruption Cataclysmique du Toba & Goulot d'Étranglement Génétique", -74000, 2.58, 98.83, 0.0, 9.5));
        list.add(new ClimateEvent("milestone_archaeology", "Art Pariétal Figuratif de la Grotte Chauvet (Aurignacien)", -36000, 44.38, 4.41, 0.0, 8.0));
        list.add(new ClimateEvent("disaster_ice_age", "Dernier Maximum Glaciaire (LGM) : Inlandsis et Niveau Marin -120m", -20000, 60.0, 10.0, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_archaeology", "Sanctuaire Monumental Mégalithique de Göbekli Tepe", -9600, 37.22, 38.92, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_ice_age", "Refroidissement Brutal du Dryas Récent (Younger Dryas)", -10800, 65.0, -30.0, 0.0, 8.0));
        list.add(new ClimateEvent("disaster_tsunami", "Méga-Glissement Sous-Marin de Storegga & Submersion du Doggerland", -6200, 64.87, 5.29, 30.0, 8.5));

        // 2. Neolithic Revolution & Early Civilizations
        list.add(new ClimateEvent("milestone_technology", "Domestication du Blé et Premières Cités-Villages (Jéricho)", -8000, 31.87, 35.44, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_archaeology", "Agglomération Proto-Urbaine Dense de Çatalhöyük (Anatolie)", -7100, 37.66, 32.82, 0.0, 7.5));
        list.add(new ClimateEvent("disaster_climate", "Événement Climatique Froid et Sec d'Aridification de 8200 cal BP", -6200, 40.0, 30.0, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_technology", "Invention de l'Écriture Proto-Cunéiforme à Uruk (Mésopotamie)", -3400, 31.32, 45.64, 0.0, 9.0));
        list.add(new ClimateEvent("milestone_polity", "Unification de la Haute et Basse Égypte sous Narmer (Dynastie 0)", -3100, 25.70, 32.65, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_archaeology", "Construction de la Grande Pyramide de Khéops à Gizeh", -2560, 29.98, 31.13, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_archaeology", "Urbanisme et Réseaux Sanitaires Avancés de Mohenjo-daro (Indus)", -2500, 27.32, 68.13, 0.0, 8.0));
        list.add(new ClimateEvent("disaster_climate", "Événement d'Aridification de 4200 cal BP (Chute de l'Ancien Empire)", -2200, 30.0, 35.0, 0.0, 8.5));

        // 3. Bronze Age & Collapse
        list.add(new ClimateEvent("milestone_polity", "Promulgation du Code des Lois de Hammurabi à Babylone", -1750, 32.54, 44.42, 0.0, 8.0));
        list.add(new ClimateEvent("disaster_volcano", "Éruption Cataclysmique Minoenne de Santorin (Théra)", -1610, 36.40, 25.40, 20.0, 8.5));
        list.add(new ClimateEvent("milestone_archaeology", "Inscriptions sur Os Divinatoires (Jiaguwen) de la Dynastie Shang", -1250, 36.10, 114.35, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_polity", "Effondrement Généralisé de l'Âge du Bronze Récent & Peuples de la Mer", -1177, 33.0, 35.0, 0.0, 8.5));

        // 4. Iron Age & Classical Antiquity
        list.add(new ClimateEvent("milestone_golden_age", "Période Axiale : Essor de la Philosophie et Grandes Religions", -600, 30.0, 70.0, 0.0, 9.0));
        list.add(new ClimateEvent("milestone_polity", "Instauration de la Démocratie Athénienne par Clisthène", -508, 37.97, 23.72, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_archaeology", "Construction de la Grande Muraille & Unification Qin (Chine)", -221, 40.43, 116.57, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_polity", "Avènement du Principat d'Auguste & Début de la Pax Romana", -27, 41.90, 12.45, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_volcano", "Éruption Explosive du Vésuve (Destruction de Pompéi et Herculanum)", 79, 40.82, 14.43, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_technology", "Perfectionnement et Standardisation de la Fabrication du Papier (Cai Lun)", 105, 34.26, 108.94, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_pandemic", "Peste Antonine dans l'Empire Romain (Variole)", 165, 41.90, 12.45, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_archaeology", "Apogée Classique Maya & Architecture Monumentale de Tikal", 378, 17.22, -89.62, 0.0, 7.5));
        list.add(new ClimateEvent("milestone_polity", "Fin de l'Empire Romain d'Occident (Déposition de Romulus Augustule)", 476, 44.41, 12.20, 0.0, 8.0));

        // 5. Early Middle Ages
        list.add(new ClimateEvent("disaster_volcano", "Hiver Volcanique Mondial Extrême de 536 (LALIA)", 536, 64.0, -18.0, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_pandemic", "Première Pandémie de Peste Bubonique (Peste de Justinien)", 541, 41.00, 28.97, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_golden_age", "Fondation de la Maison de la Sagesse à Bagdad (Bayt al-Hikma)", 800, 33.34, 44.40, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_archaeology", "Érection des Moaï Géants de Rapa Nui (Île de Pâques)", 1000, -27.11, -109.35, 0.0, 7.0));
        list.add(new ClimateEvent("milestone_technology", "Invention et Déploiement Militaire de la Poudre Noire (Dynastie Song)", 1044, 34.79, 114.30, 0.0, 8.5));

        // 6. High Middle Ages
        list.add(new ClimateEvent("milestone_archaeology", "Construction du Temple d'Angkor Vat par Suryavarman II", 1113, 13.41, 103.87, 0.0, 8.0));
        list.add(new ClimateEvent("milestone_polity", "Unification des Tribus Mongoles par Gengis Khan (Khouriltaï)", 1206, 47.92, 106.92, 0.0, 8.5));
        list.add(new ClimateEvent("milestone_polity", "Signature de la Magna Carta à Runnymede (Limitation Royale)", 1215, 51.44, -0.56, 0.0, 7.5));
        list.add(new ClimateEvent("disaster_volcano", "Éruption Ultra-Plinienne Cataclysmique du Samalas (Lombok)", 1257, -8.41, 116.46, 0.0, 8.5));
        list.add(new ClimateEvent("disaster_climate", "Refroidissement Pluvieux Extrême & Crise de Subsistance Européenne", 1315, 50.0, 10.0, 0.0, 6.0));
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

        // 7. Early Modern & Scientific Revolution
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

        // 8. Industrial Era & 19th Century
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

        // 9. 20th Century & Modern Era
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
