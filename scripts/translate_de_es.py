import os

i18n_dir = r"c:\Silvere\Encours\Developpement\Ether\src\main\resources\i18n"

de_translations = {
    # Geological Tensors
    "resource.tensor.1.title": "⛏️ 4.2.1 Kohlevorkommen (COAL — USGS / BGR)",
    "resource.tensor.1.desc": "Kohlebecken und Anthrazitlagerstätten. Quelle: USGS MRDS / BGR Deutschland.",
    "resource.tensor.2.title": "🛢️ 4.2.2 Rohölreserven & Treibstoffe (CRUDE_OIL — WEP)",
    "resource.tensor.2.desc": "Bituminöse Becken sowie Offshore- und Kontinentalölfelder.",
    "resource.tensor.3.title": "🔥 4.2.3 Erdgasfelder (NATURAL_GAS — WEP / BGR)",
    "resource.tensor.3.desc": "Konventionelle und unkonventionelle Erdgasfelder (Schiefergas).",
    "resource.tensor.4.title": "⚛️ 4.2.4 Uranerze & Kernspaltung (URANIUM — IAEA UDEPO)",
    "resource.tensor.4.desc": "Pechblende-Kondensation und Uran-/Thoriumlagerstätten. Quelle: IAEA UDEPO.",
    "resource.tensor.5.title": "🌌 4.2.5 Helium-3 & Lunare Fusion (HELIUM_3 — NASA / LPI)",
    "resource.tensor.5.desc": "Mit Helium-3 angereicherter Regolith (Mondbecken & planetare Vorkommen).",
    "resource.tensor.6.title": "⛓️ 4.2.6 Industriemetalle BIF-Eisen & Porphyr-Kupfer (IRON_COPPER)",
    "resource.tensor.6.desc": "Bändererz-Formationen (BIF) und Porphyr-Kupferlagerstätten.",
    "resource.tensor.7.title": "💎 4.2.7 Seltene Erden, Lithium-Sole & Spodumen (PRECIOUS_REE / Li)",
    "resource.tensor.7.desc": "Gold-, Platin-, Seltene-Erden-Lagerstätten (REE) und Lithium-Salare.",
    "resource.tensor.8.title": "💧 4.2.8 Aquifere & Süßwasserbecken (FRESHWATER_AQUIFERS — WHYMAP)",
    "resource.tensor.8.desc": "Tiefe Grundwasserleiter und fossile Großaquifere. Quelle: UNESCO WHYMAP.",
    "resource.tensor.custom.title_prefix": "⛏️ 4.2.",
    "resource.tensor.custom.title_mid": " Geologische Tensorschicht ",
    "resource.tensor.custom.desc": "Erweiterbare geologische Schicht.",

    # Cultural Tensors
    "scenario.tensor.1.preview": "📜 Tensor 1: Isoglossen & Linguistische Kontinua (Sprachen)",
    "scenario.tensor.2.preview": "🏛 Tensor 2: Verwandtschaft & Clanstrukturen (Verwandtschaft)",
    "scenario.tensor.3.preview": "🔮 Tensor 3: Rituale, Glaubenssysteme & Heiliges (Asabiyyah)",
    "scenario.tensor.4.preview": "👑 Tensor 4: Politisch-Militärische Souveränität & Hauptstädte",
    "scenario.tensor.5.preview": "🏺 Tensor 5: Werkzeuge, Materialität & Technologien (Artefakte)",
    "scenario.tensor.6.preview": "🐫 Tensor 6: Korridore & Handelsnetzwerke (Wirtschaftswege)",
    "scenario.tensor.7.preview": "⚖ Tensor 7: Institutionelle Komplexität & Normen (Seshat & Recht)",
    "scenario.tensor.8.preview": "⚠️ Tensor 8: Ökologischer Fußabdruck & Malthus-Spannung (Degradation)",
    "scenario.tensor.9.preview": "🧬 Tensor 9: Erregerimmunität & Gesundheit (Epidemiologie)",
    "scenario.tensor.custom.preview_prefix": "🧬 Tensor ",
    "scenario.tensor.custom.preview_mid": " : Kulturelles Substrat ",

    # Analytics
    "analytics.scenario.ground_truth": "🌍 Historische Realität (Kliodynamische Referenz)",
    "analytics.btn.executing": "⏳ Ausführung läuft...",
    "analytics.divergence.break_point": "Bruchpunkt: Wählen Sie mindestens 2 ausgeführte Szenarien",
    "analytics.divergence.select_two_hint": "Wählen Sie mindestens zwei Szenarien aus der Tabelle (z. B. Historische Realität + Simuliertes Szenario), um Abweichungen zu analysieren.",
    "analytics.divergence.prompt_report": "## Bitte wählen Sie mindestens zwei ausgeführte Szenarien aus, um den Vergleichsbericht zu erstellen.",

    # Built-in Scenarios (German)
    "scenario.preset.out_of_africa.name": "Out of Africa & Expansion des Homo Sapiens (-100.000)",
    "scenario.preset.out_of_africa.desc": "🌍 PALÄOLITHISCHES SZENARIO: Afrikanische Wiege, Kontinentalüberquerung & Out of Africa (-100.000 v. Chr.)\n\nModelliert Demografie und räumliche Expansion des Homo Sapiens von Ostafrika über den Nahen Osten, Eurasien, Ozeanien und Amerika.",

    "scenario.preset.sahul.name": "Sahul & Erste Besiedlung Australiens (-50.000)",
    "scenario.preset.sahul.desc": "🦘 PALÄOLITHISCHES SZENARIO: Meeresüberquerung & Besiedlung von Sahul (-50.000 v. Chr.)\n\nErste Ozeanüberquerung der Wallace-Linie durch Vorfahren der Aborigines. Modelliert die Besiedlung des Sahul-Kontinents und Anpassung an aride Ökosysteme.",

    "scenario.preset.beringia.name": "Beringia & Besiedlung Amerikas (-25.000)",
    "scenario.preset.beringia.desc": "🏔️ PALÄOLITHISCHES SZENARIO: Die Bering-Landbrücke & Amerikanische Besiedlung (-25.000 v. Chr.)\n\nModelliert die Isolation paläolithischer Populationen auf Beringia während des Letzten Glazialen Maximums (LGM) und die Ausbreitung entlang der Pazifikküste.",

    "scenario.preset.younger_dryas.name": "Jüngere Dryaszeit & Natufien-Schock (-10.900)",
    "scenario.preset.younger_dryas.desc": "❄️ PALÄOKLIMATISCHES SZENARIO: Jüngere Dryaszeit & Natufien-Sammeldruck (-10.900 v. Chr.)\n\nPlotzliche Abkühlung des Nordatlantiks um 5-8°C. Dürre in der Levante verringert Wildgetreide und zwingt Natufien-Gruppen zur prä-landwirtschaftlichen Sesshaftigkeit.",

    "scenario.preset.fertile_crescent.name": "Fruchtbarer Halbmond & Neolithische Revolution (-8000)",
    "scenario.preset.fertile_crescent.desc": "🌾 HISTORISCHES SZENARIO: Die Wiege der Landwirtschaft im Fruchtbaren Halbmond (-8000 v. Chr.)\n\nModelliert den Übergang von Jäger-Sammler-Gesellschaften zu sesshaften Ackerbaugesellschaften an Euphrat, Tigris und Nil.",

    "scenario.preset.green_sahara.name": "Grüne Sahara & Afrikanische Feuchtperiode (-6000)",
    "scenario.preset.green_sahara.desc": "🌴 PALÄOKLIMATISCHES SZENARIO: Grüne Sahara & Hirten-Oasen-Ära (-6000 v. Chr.)\n\nDer verstärkte afrikanische Monsun verwandelt die Sahara in eine grüne Savanne mit Großseen. Hirtengemeinschaften florieren vor der Versteppung im Mittelholozän.",

    "scenario.preset.ancient_egypt.name": "Vereinigung Ägyptens & Frühe Dynastien (-3100)",
    "scenario.preset.ancient_egypt.desc": "🏛️ HISTORISCHES SZENARIO: Vereinigung von Ober- und Unterägypten durch Narmer (-3100 v. Chr.)\n\nEntstehung des ersten Territorialstaates am Nil korridor. Organisation um Überschwemmungslandwirtschaft, Vorratshaltung und göttliches Königtum.",

    "scenario.preset.assyrian_empire.name": "Bronzezeit-Kollaps & Neuassyrisches Reich (-1200)",
    "scenario.preset.assyrian_empire.desc": "⚔️ HISTORISCHES SZENARIO: Kollaps der Späten Bronzezeit & Assyrische Expansion (-1200 v. Chr.)\n\nSystemischer Zusammenbruch östlicher Handelsnetze, Dürrekrisen und Aufstieg der Eisenmetallurgie sowie militarisierter Reichstrukturen.",

    "scenario.preset.mesoamerica.name": "Olmeken-Hegemonie & Mesoamerikanische Urbanisierung (-1200)",
    "scenario.preset.mesoamerica.desc": "🏺 HISTORISCHES SZENARIO: Olmekisches Kernland & Mesoamerikanische Stadtbildung (-1200 v. Chr.)\n\nMilpa-Maisanbau, Basalt-Monumentalarchitektur, rituelle Ballspiele und Zeremonialzentren im tropischen Tiefland.",

    "scenario.preset.maurya_empire.name": "Maurya-Reich & Eisenzeitliche Seidenstraße (-322)",
    "scenario.preset.maurya_empire.desc": "🐘 HISTORISCHES SZENARIO: Die Maurya-Reichsvereinigung & Panasiatische Handelsrouten (-322 v. Chr.)\n\nEinigung des indischen Subkontinents unter Chandragupta und Ashoka, Ediktskodifizierung und Expansion der Handelswege.",

    "scenario.preset.roman_empire.name": "Pax Romana & Mediterranes Handelsnetz (117)",
    "scenario.preset.roman_empire.desc": "🏛️ HISTORISCHES SZENARIO: Pax Romana & Mediterrane Wirtschaftsintegration (117 n. Chr.)\n\nMaximale Ausdehnung des Römischen Reiches, kaiserliches Straßennetz, Seegetreidehandel, Münzverschlechterung und urbane Hygiene.",

    "scenario.preset.late_antique_ice_age.name": "Kleine Eiszeit der Spätantike & Völkerwanderung (536)",
    "scenario.preset.late_antique_ice_age.desc": "❄️ PALÄOKLIMATISCHES SZENARIO: Vulkanischer Winter 536 n. Chr. & Spätantike Krise\n\nAbrupte weltweite Abkühlung durch Vulkaneruptionen. Ernteausfälle, Justinianische Pest und große eurasische Völkerwanderungen.",

    "scenario.preset.song_dynasty.name": "Song-Industrielle Revolution & Seehandel (1000)",
    "scenario.preset.song_dynasty.desc": "🐉 HISTORISCHES SZENARIO: Die Proto-Industrielle Revolution der Song-Dynastie (1000 n. Chr.)\n\nWirtschaftsboom unter den Song, Papiergeld (Jiaozi), Koks-Eisenverhüttung, Kompass-Navigation und hochentwickelte Handelsnetze.",

    "scenario.preset.mali_empire.name": "Mali-Reich & Transsahara-Goldrouten (1324)",
    "scenario.preset.mali_empire.desc": "🕌 HISTORISCHES SZENARIO: Das Reich Mali & Die Pilgerreise von Mansa Musa (1324 n. Chr.)\n\nTranssahara-Gold- und Salzhandel, Scholastik in Timbuktu, Karawanenlogistik und sahelische Urbanität.",

    "scenario.preset.americas_1491.name": "Präkolumbianisches Amerika vor dem Kontakt (1491)",
    "scenario.preset.americas_1491.desc": "🏹 HISTORISCHES SZENARIO: Präkolumbianisches Amerika vor der Europäischen Ankunft (1491 n. Chr.)\n\nInka-Terrassenfeldbau, Azteken-Chinampas, Mississippi-Kultur und anthropogene Terra-Preta-Böden im Amazonasgebiet.",

    "scenario.preset.columbian_contact.name": "Kolumbianischer Austausch & Globales Handelsnetz (1500)",
    "scenario.preset.columbian_contact.desc": "⛵ HISTORISCHES SZENARIO: Der Große Kontakt & Globaler Biologischer Austausch (1500 n. Chr.)\n\nZusammenführung beider Hemisphären, Pflanzentransfer (Mais, Kartoffel), Silberströme und verheerender epidemiologischer Schock.",

    "scenario.preset.tokugawa_japan.name": "Tokugawa-Isolation & Edo-Stabilität (1603)",
    "scenario.preset.tokugawa_japan.desc": "⛩️ HISTORISCHES SZENARIO: Pax Tokugawa & Sakoku-Isolationspolitik (1603 n. Chr.)\n\nZwei Jahrhunderte nachhaltige Forstwirtschaft, demografische Stabilität, Nullwachstums-Gleichgewicht in Edo und strikte Abschottung.",

    "scenario.preset.industrial_1800.name": "Erste Industrielle Revolution & Fossile Energie (1800)",
    "scenario.preset.industrial_1800.desc": "⚙️ HISTORISCHES SZENARIO: Die Große Divergenz & Kohle-Dampf-Transformation (1800 n. Chr.)\n\nÜbergang von organischer Wirtschaft zu fossilem Energieregime, Dampfmaschinen-Mechanisierung, Urbandynamik und Bevölkerungsexplosion.",

    "scenario.preset.anthropocene_2000.name": "Große Beschleunigung & Anthropozän-Referenz (2000)",
    "scenario.preset.anthropocene_2000.desc": "🌐 GEGENWARTS-SZENARIO: Globale Beschleunigung & Planetare Grenzen (2000 n. Chr.)\n\nGlobalisierte Hyperkonnektivität, 6,1 Milliarden Menschen, Haber-Bosch-Synthetikdünger, CO2-Treibhausantrieb und digitale Netze.",

    "scenario.preset.ssp5_85.name": "SSP5-8.5 Fossiles Wachstum & Klimawandel (2026)",
    "scenario.preset.ssp5_85.desc": "🔥 ZUKUNFTS-SZENARIO: Fossiler Entwicklungspfad SSP5-8.5 (2026 n. Chr.)\n\nFossiles Hochemissions-Szenario, rasche Erwärmung, Meeresspiegelanstieg und regionaler malthusianischer Agrarstress.",

    "scenario.preset.singularity_2045.name": "Technologische Singularität & KI-Transformation (2045)",
    "scenario.preset.singularity_2045.desc": "🤖 ZUKUNFTS-SZENARIO: Technologische Singularität & Superintelligenz (2045 n. Chr.)\n\nExponentielles Technologiewachstum, vollautomatisierte Produktion, Molekular-Nanotechnologie und post-humane Transformation.",

    "scenario.preset.nuclear_winter_2035.name": "Thermonuklearer Krieg & Nuklearer Winter (2035)",
    "scenario.preset.nuclear_winter_2035.desc": "⚠️ KRISEN-SZENARIO: Thermonuklearer Konflikt & Stratosphärischer Rußschock (2035 n. Chr.)\n\n150 Tg Rußinjektion in die Stratosphäre, Zusammenbruch der Sonneneinstrahlung, globale Kälte, Missernten und gesellschaftlicher Kollaps.",

    "scenario.preset.peak_phosphate_2050.name": "Phosphatknappheit & Malthus-Krise (2050)",
    "scenario.preset.peak_phosphate_2050.desc": "🌾 KRISEN-SZENARIO: Erschöpfung von Mineraldüngern & Agrarstress (2050 n. Chr.)\n\nErschöpfung hochwertiger Phosphatlagerstätten, Düngemittel-Preisschock, Ernteeinbußen und demografische Spannungen.",

    "scenario.preset.supervolcano_2060.name": "Supervulkan-Ausbruch & Vulkanischer Winter (2060)",
    "scenario.preset.supervolcano_2060.desc": "🌋 KATASTROPHEN-SZENARIO: VEI-8 Supereruption & Dekadische Globale Abkühlung (2060 n. Chr.)\n\nMassiver Schwefeldioxid-Ausstoß, jahrelanger vulkanischer Winter, Ausfall von Monsunsystemen und Bevölkerungsengpass."
}

es_translations = {
    # Geological Tensors
    "resource.tensor.1.title": "⛏️ 4.2.1 Yacimientos de Carbón (COAL — USGS / BGR)",
    "resource.tensor.1.desc": "Cuencas carboníferas y depósitos de antracita. Fuente: USGS MRDS / BGR Alemania.",
    "resource.tensor.2.title": "🛢️ 4.2.2 Reservas de Petróleo Crudo y Combustible (CRUDE_OIL — WEP)",
    "resource.tensor.2.desc": "Cuencas bituminosas y campos petrolíferos marinos y continentales.",
    "resource.tensor.3.title": "🔥 4.2.3 Campos de Gas Natural (NATURAL_GAS — WEP / BGR)",
    "resource.tensor.3.desc": "Campos de gas natural convencional y no convencional (gas de esquisto).",
    "resource.tensor.4.title": "⚛️ 4.2.4 Minerales de Uranio y Fisión (URANIUM — IAEA UDEPO)",
    "resource.tensor.4.desc": "Concentración de pechblenda y depósitos de uranio/torio. Fuente: IAEA UDEPO.",
    "resource.tensor.5.title": "🌌 4.2.5 Helio-3 y Fusión Lunar (HELIUM_3 — NASA / LPI)",
    "resource.tensor.5.desc": "Regolito enriquecido en Helio-3 (cuenca lunar y depósitos planetarios).",
    "resource.tensor.6.title": "⛓️ 4.2.6 Metales Industriales Hierro BIF y Cobre Porfídico (IRON_COPPER)",
    "resource.tensor.6.desc": "Formaciones de hierro bandeado (BIF) y yacimientos porfídicos de cobre.",
    "resource.tensor.7.title": "💎 4.2.7 Tierras Raras, Salmueras de Litio y Espodumena (PRECIOUS_REE / Li)",
    "resource.tensor.7.desc": "Yacimientos de oro, platino, elementos de tierras raras (REE) y salares de litio.",
    "resource.tensor.8.title": "💧 4.2.8 Acuíferos y Cuencas de Agua Dulce (FRESHWATER_AQUIFERS — WHYMAP)",
    "resource.tensor.8.desc": "Acuíferos subterráneos profundos y grandes cuencas fósiles. Fuente: UNESCO WHYMAP.",
    "resource.tensor.custom.title_prefix": "⛏️ 4.2.",
    "resource.tensor.custom.title_mid": " Capa de Tensor Geológico ",
    "resource.tensor.custom.desc": "Capa geológica extensible.",

    # Cultural Tensors
    "scenario.tensor.1.preview": "📜 Tensor 1: Isoglosas y Continuos Lingüísticos (Idiomas)",
    "scenario.tensor.2.preview": "🏛 Tensor 2: Parentesco y Estructuras de Clanes (Parentesco)",
    "scenario.tensor.3.preview": "🔮 Tensor 3: Rituales, Creencias y lo Sagrado (Asabiyyah)",
    "scenario.tensor.4.preview": "👑 Tensor 4: Soberanía Político-Militar y Capitales",
    "scenario.tensor.5.preview": "🏺 Tensor 5: Herramientas, Materialidad y Tecnología (Artefactos)",
    "scenario.tensor.6.preview": "🐫 Tensor 6: Corredores y Redes Comerciales (Rutas Económicas)",
    "scenario.tensor.7.preview": "⚖ Tensor 7: Complejidad Institucional y Normas (Seshat y Derecho)",
    "scenario.tensor.8.preview": "⚠️ Tensor 8: Huella Ecológica y Tensión Malthusiana (Degradación)",
    "scenario.tensor.9.preview": "🧬 Tensor 9: Inmunidad a Patógenos y Memoria Sanitaria (Epidemiología)",
    "scenario.tensor.custom.preview_prefix": "🧬 Tensor ",
    "scenario.tensor.custom.preview_mid": " : Substrato Cultural ",

    # Analytics
    "analytics.scenario.ground_truth": "🌍 Realidad Histórica (Referencia Cliodinámica)",
    "analytics.btn.executing": "⏳ Ejecución en curso...",
    "analytics.divergence.break_point": "Punto de ruptura: Seleccione al menos 2 escenarios ejecutados",
    "analytics.divergence.select_two_hint": "Marque al menos dos escenarios en la tabla (ej. Realidad Histórica + Escenario Simulado) para analizar divergencias.",
    "analytics.divergence.prompt_report": "## Seleccione al menos dos escenarios ejecutados para generar el informe comparativo.",

    # Built-in Scenarios (Spanish)
    "scenario.preset.out_of_africa.name": "Salida de África y Expansión del Homo Sapiens (-100,000)",
    "scenario.preset.out_of_africa.desc": "🌍 ESCENARIO PALEOLÍTICO: Cuna Africana, Cruce Continental y Expansión (-100.000 a. C.)\n\nModela la dinámica demográfica y expansión espacial de Homo Sapiens desde África Oriental por Oriente Medio, Eurasia, Oceanía y América.",

    "scenario.preset.sahul.name": "Sahul y Primer Poblamiento de Australia (-50,000)",
    "scenario.preset.sahul.desc": "🦘 ESCENARIO PALEOLÍTICO: Travesía Marítima e Incursión en Sahul (-50.000 a. C.)\n\nPrimer cruce oceánico de la Línea de Wallace por ancestros aborígenes. Modela la colonización de Sahul y la adaptación a ecosistemas áridos.",

    "scenario.preset.beringia.name": "Beringia y Poblamiento de América (-25,000)",
    "scenario.preset.beringia.desc": "🏔️ ESCENARIO PALEOLÍTICO: El Puente de Beringia e Incursión Americana (-25.000 a. C.)\n\nModela el aislamiento de poblaciones paleolíticas en Beringia durante el Último Máximo Glacial (LGM) y su dispersión por la ruta costera del Pacífico.",

    "scenario.preset.younger_dryas.name": "Dryas Reciente y Choque Forrajero Natufiense (-10,900)",
    "scenario.preset.younger_dryas.desc": "❄️ ESCENARIO PALEOCLIMÁTICO: El Dryas Reciente y Presión Natufiense (-10.900 a. C.)\n\nEnfriamiento brusco de 5-8°C en el Atlántico Norte. La sequía en Levante reduce cereales silvestres, forzando la sedentarización preagrícola natufiense.",

    "scenario.preset.fertile_crescent.name": "Creciente Fértil y Revolución Neolítica (-8000)",
    "scenario.preset.fertile_crescent.desc": "🌾 ESCENARIO HISTÓRICO: El Alba de la Agricultura en el Creciente Fértil (-8000 a. C.)\n\nModela la transición del Neolítico desde cazadores-recolectores hacia comunidades agrícolas sedentarias a lo largo del Tigris, Éufrates y Nilo.",

    "scenario.preset.green_sahara.name": "Sáhara Verde y Periodo Húmedo Africano (-6000)",
    "scenario.preset.green_sahara.desc": "🌴 ESCENARIO PALEOCLIMÁTICO: El Sáhara Verde y Era de los Oasis (-6000 a. C.)\n\nEl monzón africano transforma el Sáhara en una sabana frondosa con megalagos. Las comunidades pastoriles prosperan antes de la desertificación del Holoceno medio.",

    "scenario.preset.ancient_egypt.name": "Unificación de Egipto y Primeras Dinastías (-3100)",
    "scenario.preset.ancient_egypt.desc": "🏛️ ESCENARIO HISTÓRICO: Unificación del Alto y Bajo Egipto por Narmer (-3100 a. C.)\n\nSurgimiento del primer Estado territorial en el valle del Nilo. Estructuración dinástica basada en agricultura de inundación y realeza divina.",

    "scenario.preset.assyrian_empire.name": "Colapso del Bronce y Imperio Neoasirio (-1200)",
    "scenario.preset.assyrian_empire.desc": "⚔️ ESCENARIO HISTÓRICO: Colapso del Bronce Final y Expansión Asiria (-1200 a. C.)\n\nRuptura sistémica de redes comerciales mediterráneas, sequías y auge de la metalurgia del hierro e imperios militarizados.",

    "scenario.preset.mesoamerica.name": "Hegemonía Olmeca y Urbanización Mesoamericana (-1200)",
    "scenario.preset.mesoamerica.desc": "🏺 ESCENARIO HISTÓRICO: Corazón Olmeca y Urbanización Mesoamericana (-1200 a. C.)\n\nAgricultura de milpa (maíz), arquitectura monumental de basalto, juego de pelota ritual y centros ceremoniales en tierras bajas del Golfo.",

    "scenario.preset.maurya_empire.name": "Imperio Maurya y Ruta de la Seda del Hierro (-322)",
    "scenario.preset.maurya_empire.desc": "🐘 ESCENARIO HISTÓRICO: Unificación Imperial Maurya y Rutas Panasiáticas (-322 a. C.)\n\nUnificación del subcontinente indio bajo Chandragupta y Ashoka, codificación de edictos y expansión de rutas comerciales marítimas y terrestres.",

    "scenario.preset.roman_empire.name": "Pax Romana y Red Comercial Mediterránea (117)",
    "scenario.preset.roman_empire.desc": "🏛️ ESCENARIO HISTÓRICO: Pax Romana e Integración Económica Mediterránea (117 d. C.)\n\nMáxima extensión territorial del Imperio Romano, calzadas imperiales, comercio marítimo de grano, alteración monetaria e higiene urbana.",

    "scenario.preset.late_antique_ice_age.name": "Pequeña Edad de Hielo de la Antigüedad Tardía (536)",
    "scenario.preset.late_antique_ice_age.desc": "❄️ ESCENARIO PALEOCLIMÁTICO: Invierno Volcánico de 536 y Crisis de la Antigüedad Tardía\n\nEnfriamiento global brusco por erupciones volcánicas. Pérdida de cosechas, plaga de Justiniano y grandes migraciones eurasianas.",

    "scenario.preset.song_dynasty.name": "Industrialización Song y Comercio Marítimo (1000)",
    "scenario.preset.song_dynasty.desc": "🐉 ESCENARIO HISTÓRICO: Revolución Protoindustrial Song y Ruta de la Seda Marítima (1000 d. C.)\n\nAuge económico Song, papel moneda (Jiaozi), siderurgia con coque, brújula marítima y redes comerciales fluviales y marítimas.",

    "scenario.preset.mali_empire.name": "Imperio de Malí y Rutas del Oro Transajarianas (1324)",
    "scenario.preset.mali_empire.desc": "🕌 ESCENARIO HISTÓRICO: El Imperio de Malí y la Peregrinación de Mansa Musa (1324 d. C.)\n\nComercio transajariano de oro y sal, centro escolástico de Tombuctú, logística de caravanas y urbanismo saheliano.",

    "scenario.preset.americas_1491.name": "América Precolombina y Redes Indígenas (1491)",
    "scenario.preset.americas_1491.desc": "🏹 ESCENARIO HISTÓRICO: El Mundo Precolombino antes del Contacto (1491 d. C.)\n\nTerrazas incas, chinampas aztecas, montículos del Misisipi y suelos antropogénicos de Terra Preta en la Amazonia.",

    "scenario.preset.columbian_contact.name": "Intercambio Colombino y Red Comercial Global (1500)",
    "scenario.preset.columbian_contact.desc": "⛵ ESCENARIO HISTÓRICO: El Gran Contacto e Intercambio Biológico Global (1500 d. C.)\n\nConvergencia de ambos hemisferios, transferencia de cultivos (maíz, patata), flujos de plata y choque epidemiológico devastador.",

    "scenario.preset.tokugawa_japan.name": "Aislamiento Tokugawa y Estabilidad en Edo (1603)",
    "scenario.preset.tokugawa_japan.desc": "⛩️ ESCENARIO HISTÓRICO: Pax Tokugawa y Política de Aislamiento Sakoku (1603 d. C.)\n\nDos siglos de gestión forestal sostenible, estabilidad demográfica, equilibrio urbano en Edo y cierre hermético de fronteras.",

    "scenario.preset.industrial_1800.name": "Primera Revolución Industrial y Energía Fósil (1800)",
    "scenario.preset.industrial_1800.desc": "⚙️ ESCENARIO HISTÓRICO: La Gran Divergencia y Transición del Carbón (1800 d. C.)\n\nTransición de la economía orgánica al régimen fósil (carbón), mecanización a vapor, rápida urbanización y explosión demográfica.",

    "scenario.preset.anthropocene_2000.name": "Gran Aceleración y Línea Base del Antropoceno (2000)",
    "scenario.preset.anthropocene_2000.desc": "🌐 ESCENARIO CONTEMPORÁNEO: Gran Aceleración y Límites Planetarios (2000 d. C.)\n\nHiperconectividad global, 6.100 millones de habitantes, fertilizantes sintéticos Haber-Bosch, forzamiento de CO2 y redes digitales.",

    "scenario.preset.ssp5_85.name": "SSP5-8.5 Desarrollo Fósil y Cambio Climático (2026)",
    "scenario.preset.ssp5_85.desc": "🔥 ESCENARIO FUTURO: Trayectoria de Altas Emisiones SSP5-8.5 (2026 d. C.)\n\nDesarrollo basado en combustibles fósiles, calentamiento acelerado, subida del nivel del mar y estrés agrícola malthusiano.",

    "scenario.preset.singularity_2045.name": "Singularidad Tecnológica y Transición IA (2045)",
    "scenario.preset.singularity_2045.desc": "🤖 ESCENARIO FUTURO: Singularidad Tecnológica y Transición AGI (2045 d. C.)\n\nCrecimiento tecnológico exponencial, producción automatizada, nanotecnología molecular y transformación poshumana.",

    "scenario.preset.nuclear_winter_2035.name": "Guerra Termonuclear e Invierno Nuclear (2035)",
    "scenario.preset.nuclear_winter_2035.desc": "⚠️ ESCENARIO DE CRISIS: Conflicto Termonuclear e Invierno Estratosférico (2035 d. C.)\n\nInyección de 150 Tg de hollín en la estratosfera, colapso de radiación solar, congelación global, ruina agrícola y colapso social.",

    "scenario.preset.peak_phosphate_2050.name": "Escasez de Fosfato y Crisis Malthusiana (2050)",
    "scenario.preset.peak_phosphate_2050.desc": "🌾 ESCENARIO DE CRISIS: Agotamiento de Fertilizantes Minerales (2050 d. C.)\n\nAgotamiento de yacimientos de fosfato, subida de precios de fertilizantes, caída de cosechas y tensión demográfica global.",

    "scenario.preset.supervolcano_2060.name": "Erupción de Supervolcán e Invierno Volcánico (2060)",
    "scenario.preset.supervolcano_2060.desc": "🌋 ESCENARIO CATACLÍSMICO: Supererupción VEI-8 y Enfriamiento Decenal (2060 d. C.)\n\nInyección masiva de SO2, invierno volcánico decenal, interrupción de monzones globales y cuello de botella demográfico."
}

def apply_translations(lang, translation_map):
    fpath = os.path.join(i18n_dir, f"messages_{lang}.properties")
    if not os.path.exists(fpath):
        return
    
    # Load current key-value
    kv = {}
    lines = []
    with open(fpath, "r", encoding="utf-8") as f:
        lines = f.readlines()
    
    updated_count = 0
    new_lines = []
    for line in lines:
        stripped = line.strip()
        if stripped and not stripped.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            k = k.strip()
            if k in translation_map:
                new_v = translation_map[k].replace("\n", "\\n")
                new_lines.append(f"{k}={new_v}\n")
                updated_count += 1
            else:
                new_lines.append(line)
        else:
            new_lines.append(line)
            
    with open(fpath, "w", encoding="utf-8") as f:
        f.writelines(new_lines)
        
    print(f"Applied {updated_count} native translations to messages_{lang}.properties")

apply_translations("de", de_translations)
apply_translations("es", es_translations)
