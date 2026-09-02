import os

i18n_dir = r"c:\Silvere\Encours\Developpement\Ether\src\main\resources\i18n"

# Translations dictionary per locale
data = {
    "en": {
        # Geological Tensors
        "resource.tensor.1.title": "⛏️ 4.2.1 Coal Deposits (COAL — USGS / BGR)",
        "resource.tensor.1.desc": "Coal basins and anthracite deposits. Source: USGS MRDS / BGR Germany.",
        "resource.tensor.2.title": "🛢️ 4.2.2 Crude Oil Reserves & Fuel (CRUDE_OIL — WEP)",
        "resource.tensor.2.desc": "Bituminous basins and offshore & continental oil fields.",
        "resource.tensor.3.title": "🔥 4.2.3 Natural Gas Fields (NATURAL_GAS — WEP / BGR)",
        "resource.tensor.3.desc": "Conventional and non-conventional natural gas fields (shale gas).",
        "resource.tensor.4.title": "⚛️ 4.2.4 Uranium Ores & Fission (URANIUM — IAEA UDEPO)",
        "resource.tensor.4.desc": "Pitchblende concentration and uranium/thorium deposits. Source: IAEA UDEPO.",
        "resource.tensor.5.title": "🌌 4.2.5 Helium-3 & Lunar Fusion (HELIUM_3 — NASA / LPI)",
        "resource.tensor.5.desc": "Regolith enriched in Helium-3 (lunar basin & planetary deposits).",
        "resource.tensor.6.title": "⛓️ 4.2.6 Industrial Metals BIF Iron & Porphyry Copper (IRON_COPPER)",
        "resource.tensor.6.desc": "Banded iron formations (BIF) and porphyry copper deposits.",
        "resource.tensor.7.title": "💎 4.2.7 Rare Earths, Lithium Brines & Spodumene (PRECIOUS_REE / Li)",
        "resource.tensor.7.desc": "Gold, platinum, rare earth element (REE) deposits, and lithium salars.",
        "resource.tensor.8.title": "💧 4.2.8 Aquifers & Freshwater Basins (FRESHWATER_AQUIFERS — WHYMAP)",
        "resource.tensor.8.desc": "Deep groundwater tables and large fossil aquifers. Source: UNESCO WHYMAP.",
        "resource.tensor.custom.title_prefix": "⛏️ 4.2.",
        "resource.tensor.custom.title_mid": " Geology Tensor Layer ",
        "resource.tensor.custom.desc": "Extensible geological layer.",

        # Cultural Tensors Previews
        "scenario.tensor.1.preview": "📜 Tensor 1: Isoglosses & Linguistic Continua (Languages)",
        "scenario.tensor.2.preview": "🏛 Tensor 2: Kinship & Clan Structures (Kinship)",
        "scenario.tensor.3.preview": "🔮 Tensor 3: Rituals, Beliefs & Sacred (Asabiyyah)",
        "scenario.tensor.4.preview": "👑 Tensor 4: Politico-Military Sovereignty & Capitals",
        "scenario.tensor.5.preview": "🏺 Tensor 5: Tooling, Materiality & Technologies (Artifacts)",
        "scenario.tensor.6.preview": "🐫 Tensor 6: Corridors & Trade Networks (Economic Routes)",
        "scenario.tensor.7.preview": "⚖ Tensor 7: Institutional Complexity & Norms (Seshat & Law)",
        "scenario.tensor.8.preview": "⚠️ Tensor 8: Ecological Footprint & Malthusian Tension (Degradation)",
        "scenario.tensor.9.preview": "🧬 Tensor 9: Pathogen Immunity & Health Memory (Epidemiology)",
        "scenario.tensor.custom.preview_prefix": "🧬 Tensor ",
        "scenario.tensor.custom.preview_mid": " : Cultural Substrate ",

        # Analytics
        "analytics.scenario.ground_truth": "🌍 Historical Reality (Cliodynamic Ground Truth)",
        "analytics.btn.executing": "⏳ Execution in progress...",
        "analytics.divergence.break_point": "Break point: Select at least 2 executed scenarios",
        "analytics.divergence.select_two_hint": "Check at least two scenarios in table (e.g. Historical Reality + Simulated Scenario) to analyze divergences.",
        "analytics.divergence.prompt_report": "## Please select at least two executed scenarios to generate comparative summary and audit report.",

        # Built-in Scenario Names & Descriptions
        "scenario.preset.out_of_africa.name": "Out of Africa & Homo Sapiens Expansion (-100,000 BP)",
        "scenario.preset.out_of_africa.desc": "PALEOLITHIC SCENARIO: African Cradle, Continental Crossing & Out of Africa (-100,000 BC)\n\nModels demographic dynamics and spatial expansion of early Homo Sapiens from East Africa across the Middle East, Eurasia, Oceania, and the Americas.",
        
        "scenario.preset.sahul.name": "Sahul & First Settlement of Australia (-50,000 BP)",
        "scenario.preset.sahul.desc": "PALEOLITHIC SCENARIO: Maritime Crossing & Incursion into Sahul (-50,000 BC)\n\nFirst major ocean crossing of the Wallace Line by ancestors of Indigenous Australians. Models colonization of Sahul (Australia, Tasmania, New Guinea combined) and adaptation to arid environments.",

        "scenario.preset.beringia.name": "Beringia & Peopling of the Americas (-25,000 BP)",
        "scenario.preset.beringia.desc": "PALEOLITHIC SCENARIO: The Bering Land Bridge & American Incursion (-25,000 BC)\n\nModels isolation of Paleolithic populations on the Bering land bridge during the Last Glacial Maximum (LGM), followed by dispersal through the ice-free corridor and Pacific coastal route.",

        "scenario.preset.younger_dryas.name": "Younger Dryas & Natufian Forager Shock (-10,900 BP)",
        "scenario.preset.younger_dryas.desc": "PALEOCLIMATE SCENARIO: The Younger Dryas & Levant Foraging Pressure (-10,900 BC)\n\nAbrupt 5-8°C cooling in the North Atlantic triggered by Lake Agassiz freshwater outburst. Severe drought in the Levant reduces wild cereals, forcing Natufian foragers toward pre-agricultural sedentism.",

        "scenario.preset.fertile_crescent.name": "Fertile Crescent & Neolithic Revolution (-8000 BP)",
        "scenario.preset.fertile_crescent.desc": "HISTORICAL SCENARIO: The Dawn of Agriculture in the Fertile Crescent (-8000 BC)\n\nModels the major Neolithic transition from hunter-gatherer subsistence to settled agricultural communities along the Tigris, Euphrates, Nile, and Levantine coast.",

        "scenario.preset.green_sahara.name": "Green Sahara & African Humid Period (-6000 BP)",
        "scenario.preset.green_sahara.desc": "PALEOCLIMATE SCENARIO: Green Sahara & Pastoral Oasis Era (-6000 BC)\n\nEnhanced African monsoon transforms the Sahara into a lush savanna with mega-lakes (Lake Mega-Chad). Pastoral communities thrive before mid-Holocene desertification.",

        "scenario.preset.ancient_egypt.name": "Unification of Egypt & Early Dynasties (-3100 BP)",
        "scenario.preset.ancient_egypt.desc": "HISTORICAL SCENARIO: Unification of Upper & Lower Egypt (-3100 BC)\n\nEmergence of the first territorial state along the Nile corridor, organized around inundation agriculture, centralized storage, and divine kingship.",

        "scenario.preset.assyrian_empire.name": "Bronze Age Collapse & Neo-Assyrian Empire (-1200 BP)",
        "scenario.preset.assyrian_empire.desc": "HISTORICAL SCENARIO: Late Bronze Age Collapse & Neo-Assyrian Imperial Expansion (-1200 BC)\n\nSimulates systemic breakdown of Eastern Mediterranean trade networks, drought crisis, and rise of iron metallurgy and empire building.",

        "scenario.preset.mesoamerica.name": "Olmec Hegemony & Mesoamerican Urbanization (-1200 BP)",
        "scenario.preset.mesoamerica.desc": "HISTORICAL SCENARIO: Olmec Heartlands & Early Mesoamerican Urbanization (-1200 BC)\n\nMilpa maize agriculture, basalt monumentality, ritual ballgames, and ceremonial center networks across Gulf Coast tropical lowlands.",

        "scenario.preset.maurya_empire.name": "Maurya Empire & Iron Age Silk Road (-322 BP)",
        "scenario.preset.maurya_empire.desc": "HISTORICAL SCENARIO: Maurya Imperial Unification & Pan-Asian Trade Routes (-322 BC)\n\nUnification of the Indian subcontinent under Chandragupta and Ashoka, edict codification, and expansion of land and maritime Silk Road trade.",

        "scenario.preset.roman_empire.name": "Pax Romana & Mediterranean Trade Peak (117 AD)",
        "scenario.preset.roman_empire.desc": "HISTORICAL SCENARIO: Pax Romana & Mediterranean Economic Integration (117 AD)\n\nMaximum territorial extent of the Roman Empire, imperial road networks, maritime grain trade, coin debasement dynamics, and urban sanitation.",

        "scenario.preset.late_antique_ice_age.name": "Late Antique Little Ice Age & Migrations (536 AD)",
        "scenario.preset.late_antique_ice_age.desc": "PALEOCLIMATE SCENARIO: Volcanic Winter 536 AD & Late Antique Crisis\n\nTriple volcanic eruptions trigger severe global cooling, crop failures, Justinianic Plague pandemic, and large-scale Eurasian barbarian migrations.",

        "scenario.preset.song_dynasty.name": "Song Dynasty Industrialization & Maritime Trade (1000 AD)",
        "scenario.preset.song_dynasty.desc": "HISTORICAL SCENARIO: Proto-Industrial Revolution & Maritime Silk Road (1000 AD)\n\nSong Dynasty economic boom, paper currency (Jiaozi), coke-fueled iron smelting, compass navigation, and commercialized river/maritime networks.",

        "scenario.preset.mali_empire.name": "Mali Empire & Trans-Saharan Gold Routes (1324 AD)",
        "scenario.preset.mali_empire.desc": "HISTORICAL SCENARIO: Mansa Musa Pilgrimage & Trans-Saharan Trade Peak (1324 AD)\n\nTrans-Saharan gold and salt trade, Timbuktu scholastic centers, caravan logistics, and West African Sahelian urbanism.",

        "scenario.preset.americas_1491.name": "Pre-Columbian Americas Peak & Indigenous Networks (1491 AD)",
        "scenario.preset.americas_1491.desc": "HISTORICAL SCENARIO: Pre-Columbian Hemispheric Baseline (1491 AD)\n\nHigh-density Inca terrace engineering, Aztec chinampa hydro-agriculture, Mississippian mound centers, and Amazonian Terra Preta anthropogenic soils.",

        "scenario.preset.columbian_contact.name": "Columbian Exchange & Global Trade Network (1500 AD)",
        "scenario.preset.columbian_contact.desc": "HISTORICAL SCENARIO: The Great Contact & Global Biological Exchange (1500 AD)\n\nHemispheric convergence, transatlantic crop transfers (maize, potato, cassava), silver flows, and catastrophic epidemiological shocks.",

        "scenario.preset.tokugawa_japan.name": "Tokugawa Isolation & Edo Stability (1603 AD)",
        "scenario.preset.tokugawa_japan.desc": "HISTORICAL SCENARIO: Pax Tokugawa & Closed Country Policy / Sakoku (1603 AD)\n\nTwo centuries of sustainable resource management, deforestation control, zero-growth urban equilibrium in Edo, and strict maritime isolation.",

        "scenario.preset.industrial_1800.name": "First Industrial Revolution & Fossil Energy (1800 AD)",
        "scenario.preset.industrial_1800.desc": "HISTORICAL SCENARIO: The Great Divergence & Coal Steam Transition (1800 AD)\n\nTransition from organic economy to fossil energy regime, steam engine mechanization, rapid urbanization, and exponential population explosion.",

        "scenario.preset.anthropocene_2000.name": "Great Acceleration & Anthropocene Baseline (2000 AD)",
        "scenario.preset.anthropocene_2000.desc": "CONTEMPORARY SCENARIO: Global Acceleration & Planetary Boundaries (2000 AD)\n\nGlobalized hyper-connectivity, 6.1 billion human population, Haber-Bosch synthetic nitrogen reliance, CO2 greenhouse forcing, and digital information grids.",

        "scenario.preset.ssp5_85.name": "SSP5-8.5 Fossil-Fueled Development & Climate (2026 AD)",
        "scenario.preset.ssp5_85.desc": "FUTURE SCENARIO: High-Emission Baseline & Global Warming (2026 AD)\n\nSSP5-8.5 extreme climate trajectory, rapid energy demand growth, sea level rise, extreme weather events, and regional Malthusian agricultural stress.",

        "scenario.preset.singularity_2045.name": "Technological Singularity & AGI Transition (2045 AD)",
        "scenario.preset.singularity_2045.desc": "FUTURE SCENARIO: Post-Scarcity & Superintelligence Transition (2045 AD)\n\nExponential technological growth, automated production, molecular nanotechnology, space resource extraction, and post-human societal transformations.",

        "scenario.preset.nuclear_winter_2035.name": "Global Thermonuclear War & Nuclear Winter (2035 AD)",
        "scenario.preset.nuclear_winter_2035.desc": "CRISIS SCENARIO: Thermonuclear Exchange & Stratospheric Soot Shock (2035 AD)\n\n150 Tg stratospheric soot injection, global solar irradiance collapse, abrupt freezing temperatures, crop failure, and societal breakdown.",

        "scenario.preset.peak_phosphate_2050.name": "Global Phosphorus Scarcity & Malthusian Crisis (2050 AD)",
        "scenario.preset.peak_phosphate_2050.desc": "CRISIS SCENARIO: Depletion of Mineral Fertilizers & Food System Stress (2050 AD)\n\nExhaustion of high-grade rock phosphate reserves, fertilizer price spikes, agricultural yield contraction, and global demographic tension.",

        "scenario.preset.supervolcano_2060.name": "Supervolcano Eruption & Volcanic Winter (2060 AD)",
        "scenario.preset.supervolcano_2060.desc": "CATACLYSM SCENARIO: VEI-8 Supereruption & Global Decade Cooling (2060 AD)\n\nMassive sulfur dioxide injection, multidecadal volcanic winter, global monsoon disruption, infrastructure collapse, and survival bottlenecks."
    },
    "fr": {
        # Geological Tensors
        "resource.tensor.1.title": "⛏️ 4.2.1 Gisements de Charbon (COAL — USGS / BGR)",
        "resource.tensor.1.desc": "Bassin houiller et gisements d'anthracite. Source: USGS MRDS / BGR Germany.",
        "resource.tensor.2.title": "🛢️ 4.2.2 Réserves de Pétrole Brut & Fuel (CRUDE_OIL — WEP)",
        "resource.tensor.2.desc": "Bassins bitumineux et champs pétrolifères sous-marins et continentaux.",
        "resource.tensor.3.title": "🔥 4.2.3 Champs de Gaz Naturel (NATURAL_GAS — WEP / BGR)",
        "resource.tensor.3.desc": "Champs de gaz naturel conventionnel et non-conventionnel (shale gas).",
        "resource.tensor.4.title": "⚛️ 4.2.4 Minerais d'Uranium & Fission (URANIUM — IAEA UDEPO)",
        "resource.tensor.4.desc": "Concentration en pitchblende et gisements d'uranium/thorium. Source: IAEA UDEPO.",
        "resource.tensor.5.title": "🌌 4.2.5 Hélium-3 & Fusion Lunaires (HELIUM_3 — NASA / LPI)",
        "resource.tensor.5.desc": "Régolithe enrichi en Hélium-3 (bassin lunaire & dépôts planétaires).",
        "resource.tensor.6.title": "⛓️ 4.2.6 Métaux Industriels Fer BIF & Cuivre Porphyrique (IRON_COPPER)",
        "resource.tensor.6.desc": "Formations ferrifères rubanées (BIF) et porphyres cuprifères.",
        "resource.tensor.7.title": "💎 4.2.7 Terres Rares, Lithium Brines & Spodumène (PRECIOUS_REE / Li)",
        "resource.tensor.7.desc": "Gisements d'or, platine, terres rares (REE) et salars de lithium.",
        "resource.tensor.8.title": "💧 4.2.8 Aquifères & Eau Douce Grands Bassins (FRESHWATER_AQUIFERS — WHYMAP)",
        "resource.tensor.8.desc": "Nappes phréatiques profondes et grands aquifères fossiles. Source: UNESCO WHYMAP.",
        "resource.tensor.custom.title_prefix": "⛏️ 4.2.",
        "resource.tensor.custom.title_mid": " Calque Tenseur Géologique ",
        "resource.tensor.custom.desc": "Calque géologique extensible.",

        # Cultural Tensors Previews
        "scenario.tensor.1.preview": "📜 Tenseur 1 : Isoglosses & Continua Linguistiques (Langues)",
        "scenario.tensor.2.preview": "🏛 Tenseur 2 : Kinship & Structures de Clans (Parenté)",
        "scenario.tensor.3.preview": "🔮 Tenseur 3 : Rituels, Croyances & Sacré (Asabiyyah)",
        "scenario.tensor.4.preview": "👑 Tenseur 4 : Souveraineté Politico-Militaire & Capitales",
        "scenario.tensor.5.preview": "🏺 Tenseur 5 : Outillage, Matérialité & Technologies (Artefacts)",
        "scenario.tensor.6.preview": "🐫 Tenseur 6 : Corridors & Réseaux Commerciaux (Voies Économiques)",
        "scenario.tensor.7.preview": "⚖ Tenseur 7 : Complexité Institutionnelle & Normes (Seshat & Droit)",
        "scenario.tensor.8.preview": "⚠️ Tenseur 8 : Empreinte Écologique & Tension Malthusienne (Dégradation)",
        "scenario.tensor.9.preview": "🧬 Tenseur 9 : Immunité Pathogène & Mémoire Sanitaire (Épidémiologie)",
        "scenario.tensor.custom.preview_prefix": "🧬 Tenseur ",
        "scenario.tensor.custom.preview_mid": " : Substrat Culturel ",

        # Analytics
        "analytics.scenario.ground_truth": "🌍 Réalité Historique (Cliodynamic Ground Truth)",
        "analytics.btn.executing": "⏳ Exécution en cours...",
        "analytics.divergence.break_point": "Point de rupture : Sélectionnez au moins 2 scénarios exécutés",
        "analytics.divergence.select_two_hint": "Cochez au moins deux scénarios dans le tableau (ex. Réalité Historique + Scénario Simulé) pour analyser les divergences.",
        "analytics.divergence.prompt_report": "## Veuillez sélectionner au moins deux scénarios exécutés pour générer la synthèse comparative et le rapport d'audit.",

        # Built-in Scenario Names & Descriptions
        "scenario.preset.out_of_africa.name": "Sortie d'Afrique & Expansion Homo Sapiens (-100000)",
        "scenario.preset.out_of_africa.desc": "🌍 SCÉNARIO PALÉOLITHIQUE : Berceau Africain, Traversée des Continents & Out of Africa (-100 000 av. J.-C.)\n\nModélise la dynamique démographique et l'expansion spatiale des premières populations d'Homo Sapiens depuis l'Afrique de l'Est à travers le Moyen-Orient, l'Eurasie, l'Océanie et les Amériques.",

        "scenario.preset.sahul.name": "Sahul & Premier Peuplement de l'Australie (-50000)",
        "scenario.preset.sahul.desc": "🦘 SCÉNARIO PALÉOLITHIQUE : Traversée Maritime & Incursion dans le Sahul (-50 000 av. J.-C.)\n\nPremier franchissement maritime majeur de la ligne de Wallace par les ancêtres des Aborigènes d'Australie. Modélise la colonisation du continent Sahul (Australie, Tasmanie, Nouvelle-Guinée réunies) et l'adaptation aux écosystèmes arides.",

        "scenario.preset.beringia.name": "Béringie & Peuplement des Amériques (-25000)",
        "scenario.preset.beringia.desc": "🏔️ SCÉNARIO PALÉOLITHIQUE : Le Pont Terrestre de Béringie & Incursion Américaine (-25 000 av. J.-C.)\n\nModélise l'isolation des populations paléolithiques sur le pont terrestre de Béringie pendant le Dernier Maximum Glaciaire (LGM), suivie de leur dispersion à travers le corridor libre de glace et la route côtière du Pacifique.",

        "scenario.preset.younger_dryas.name": "Le Récents Dryas & Choc Climatique Natufien (-10900)",
        "scenario.preset.younger_dryas.desc": "❄️ SCÉNARIO PALÉOCLIMATIQUE : Le Récents Dryas & Pression Foragère Au Levant (-10 900 av. J.-C.)\n\nRefroidissement brutal de 5 à 8°C de l'Atlantique Nord déclenché par le déversement d'eau douce du Lac Agassiz. Au Levant, la sécheresse aiguë réduit les céréales sauvages, contraignant les populations Natufiennes à la sédentarisation pré-agricole et au contrôle des graines.",

        "scenario.preset.fertile_crescent.name": "Croissant Fertile & Néolithique (-8000)",
        "scenario.preset.fertile_crescent.desc": "🌾 SCÉNARIO HISTORIQUE : L'Aube de l'Agriculture au Croissant Fertile (-8000 av. J.-C.)\n\nCe scénario modélise la transition majeure du Néolithique entre l'économie de subsistance des chasseurs-cueilleurs et l'émergence des premières communautés agricoles sédentaires le long du Tigre, de l'Euphrate, du Nil et de la côte Levantine.",

        "scenario.preset.green_sahara.name": "Le Sahara Vert & Période Humide Africaine (-6000)",
        "scenario.preset.green_sahara.desc": "🌴 SCÉNARIO PALÉOCLIMATIQUE : Le Sahara Vert & Période Humide Africaine (-6000 av. J.-C.)\n\nL'intensification de la mousson africaine transforme le désert du Sahara en une savane verdoyante parsemée de mégalacs (Mégalac Tchad). Les populations de pasteurs prospèrent avant la désertification de l'Holocène moyen.",

        "scenario.preset.ancient_egypt.name": "Unification de l'Égypte & Premières Dynasties (-3100)",
        "scenario.preset.ancient_egypt.desc": "🏛️ SCÉNARIO HISTORIQUE : L'Unification de la Haute et Basse Égypte par Narmer (-3100 av. J.-C.)\n\nÉmergence du premier État territorial le long du couloir du Nil. Structuration des premières dynasties autour de l'agriculture de crue, du stockage centralisé et de la royauté divine.",

        "scenario.preset.assyrian_empire.name": "Effondrement du Bronze & Empire Néo-Assyrien (-1200)",
        "scenario.preset.assyrian_empire.desc": "⚔️ SCÉNARIO HISTORIQUE : L'Effondrement de l'Âge du Bronze & l'Expansion Assyrienne (-1200 av. J.-C.)\n\nModélise la rupture systémique des réseaux commerciaux de Méditerranée orientale, les épisodes de sécheresse et l'essor de la métallurgie du fer et des structures impériales militarisées.",

        "scenario.preset.mesoamerica.name": "Hégémonie Olmèque & Urbanisation Mésoaméricaine (-1200)",
        "scenario.preset.mesoamerica.desc": "🏺 SCÉNARIO HISTORIQUE : Le Cœur Olmèque & L'Urbanisation Mésoaméricaine (-1200 av. J.-C.)\n\nAgriculture de milpa (maïs), monumentalité basaltique, jeu de balle rituel et réseaux de centres cérémoniels dans les terres basses tropicales du Golfe.",

        "scenario.preset.maurya_empire.name": "Empire Maurya & Route de la Soie Âge du Fer (-322)",
        "scenario.preset.maurya_empire.desc": "🐘 SCÉNARIO HISTORIQUE : L'Unification Maurya & Les Routes Commerciales Pan-Asiatiques (-322 av. J.-C.)\n\nUnification du sous-continent indien sous Chandragupta et Ashoka, codification des édits et expansion des routes commerciales terrestres et maritimes.",

        "scenario.preset.roman_empire.name": "Pax Romana & Réseau Commercial Méditerranéen (117)",
        "scenario.preset.roman_empire.desc": "🏛️ SCÉNARIO HISTORIQUE : Pax Romana & L'Intégration Économique Méditerranéenne (117 apr. J.-C.)\n\nExtension territoriale maximale de l'Empire Romain, réseaux routiers impériaux, commerce annonaire maritime, dynamique d'altération monétaire et hygiène urbaine.",

        "scenario.preset.late_antique_ice_age.name": "Petit Âge Glaciaire de l'Antiquité Tardive (536)",
        "scenario.preset.late_antique_ice_age.desc": "❄️ SCÉNARIO PALÉOCLIMATIQUE : L'Hiver Volcanique de 536 & La Crise de l'Antiquité Tardive (536 apr. J.-C.)\n\nRefroidissement mondial brutal déclenché par une série d'éruptions volcaniques majeures. Effondrement des récoltes, peste de Justinien et grandes migrations eurasiennes.",

        "scenario.preset.song_dynasty.name": "Industrialisation Song & Commerce Maritime (1000)",
        "scenario.preset.song_dynasty.desc": "🐉 SCÉNARIO HISTORIQUE : La Révolution Proto-Industrielle Song (1000 apr. J.-C.)\n\nEssor économique sous la dynastie Song, papier-monnaie (Jiaozi), métallurgie au coke, boussole maritime et réseaux commerciaux fluviaux et maritimes hyper-développés.",

        "scenario.preset.mali_empire.name": "Empire du Mali & Routes de l'Or Transsahariennes (1324)",
        "scenario.preset.mali_empire.desc": "🕌 SCÉNARIO HISTORIQUE : L'Empire du Mali & Le Pèlerinage de Kankou Moussa (1324 apr. J.-C.)\n\nCommerce transsaharien de l'or et du sel, universités scolastiques de Tombouctou, logistique des caravanes et urbanisme sahélien ouest-africain.",

        "scenario.preset.americas_1491.name": "Amériques Précolombiennes & Réseaux Indigènes (1491)",
        "scenario.preset.americas_1491.desc": "🏹 SCÉNARIO HISTORIQUE : Le Monde Précolombien avant le Contact (1491 apr. J.-C.)\n\nInca (terrasses et voies royales), Aztèques (chinampas), Mississippiens (terramares) et sols anthropiques d'Amazonie (Terra Preta).",

        "scenario.preset.columbian_contact.name": "Échange Colombien & Réseau Commercial Mondial (1500)",
        "scenario.preset.columbian_contact.desc": "⛵ SCÉNARIO HISTORIQUE : Le Grand Contact & L'Échange Biologique Mondial (1500 apr. J.-C.)\n\nConvergence des deux hémisphères, transferts de cultures (maïs, pomme de terre), flux d'argent métal et choc épidémiologique dévastateur.",

        "scenario.preset.tokugawa_japan.name": "Isolation Tokugawa & Stabilité d'Edo (1603)",
        "scenario.preset.tokugawa_japan.desc": "⛩️ SCÉNARIO HISTORIQUE : La Pax Tokugawa & Le Régime de Sakoku (1603 apr. J.-C.)\n\nDeux siècles de gestion durable des ressources forestières, stabilité démographique, équilibre urbain d'Edo et fermeture hermétique des frontières maritimes.",

        "scenario.preset.industrial_1800.name": "Révolution Industrielle & Énergie Fossile (1800)",
        "scenario.preset.industrial_1800.desc": "⚙️ SCÉNARIO HISTORIQUE : La Grande Divergence & La Révolution de la Vapeur (1800 apr. J.-C.)\n\nTransition de l'économie organique vers l'énergie fossile (charbon), mécanisation de la vapeur, urbanisation rapide et explosion démographique.",

        "scenario.preset.anthropocene_2000.name": "Grande Accélération & Baseline Anthropocène (2000)",
        "scenario.preset.anthropocene_2000.desc": "🌐 SCÉNARIO CONTEMPORAIN : La Grande Accélération & L'Anthropocène (2000 apr. J.-C.)\n\nHyper-connectivité mondiale, 6,1 milliards d'humains, engrais de synthèse Haber-Bosch, forçage CO2 et réseaux numériques.",

        "scenario.preset.ssp5_85.name": "SSP5-8.5 Développement Fossile & Climat (2026)",
        "scenario.preset.ssp5_85.desc": "🔥 SCÉNARIO FUTURISTE : Trajectoire Énergétique SSP5-8.5 (2026 apr. J.-C.)\n\nDéveloppement intensif basé sur les combustibles fossiles, réchauffement climatique accéléré, élévation du niveau de la mer et stress agricole malthusien.",

        "scenario.preset.singularity_2045.name": "Singularité Technologique & Transition IA (2045)",
        "scenario.preset.singularity_2045.desc": "🤖 SCÉNARIO FUTURISTE : La Singularité Technologique & L'Ère de l'AGI (2045 apr. J.-C.)\n\nCroissance technologique exponentielle, automatisation totale de la production, nanotechnologies et transformation post-humaine.",

        "scenario.preset.nuclear_winter_2035.name": "Guerre Thermonucléaire & Hiver Nucléaire (2035)",
        "scenario.preset.nuclear_winter_2035.desc": "⚠️ SCÉNARIO DE CRISE : Conflit Thermonucléaire Global & Hiver Nucléaire (2035 apr. J.-C.)\n\nInjection de 150 Tg de suie dans la stratosphère, effondrement de l'irradiance solaire, gel mondial, ruine des cultures et effondrement sociétal.",

        "scenario.preset.peak_phosphate_2050.name": "Pénurie de Phosphate & Crise Malthusienne (2050)",
        "scenario.preset.peak_phosphate_2050.desc": "🌾 SCÉNARIO DE CRISE : Épuisement des Phosphates & Tension Alimentaire (2050 apr. J.-C.)\n\nÉpuisement des réserves de phosphate minéral, flambée des prix des engrais, baisse des rendements agricoles et crise démographique.",

        "scenario.preset.supervolcano_2060.name": "Éruption Supervolcanique & Hiver Volcanique (2060)",
        "scenario.preset.supervolcano_2060.desc": "🌋 SCÉNARIO CATACLYSMIQUE : Éruption VEI-8 & Refroidissement Décennal (2060 apr. J.-C.)\n\nInjection massive de SO2, hiver volcanique décennal, effondrement des moussons globales et goulot d'étranglement de la population."
    },
    "zh": {
        # Geological Tensors
        "resource.tensor.1.title": "⛏️ 4.2.1 煤炭矿床 (COAL — USGS / BGR)",
        "resource.tensor.1.desc": "煤田及无烟煤矿床。数据来源：USGS MRDS / 德国 BGR。",
        "resource.tensor.2.title": "🛢️ 4.2.2 原油储备与燃料 (CRUDE_OIL — WEP)",
        "resource.tensor.2.desc": "沥青盆地及近海与大陆油田。",
        "resource.tensor.3.title": "🔥 4.2.3 天然气田 (NATURAL_GAS — WEP / BGR)",
        "resource.tensor.3.desc": "常规与页岩气天然气田。",
        "resource.tensor.4.title": "⚛️ 4.2.4 铀矿石与核裂变 (URANIUM — IAEA UDEPO)",
        "resource.tensor.4.desc": "沥青铀矿浓度及铀/钍矿床。数据来源：IAEA UDEPO。",
        "resource.tensor.5.title": "🌌 4.2.5 氦-3 与月球核聚变 (HELIUM_3 — NASA / LPI)",
        "resource.tensor.5.desc": "富含氦-3 的月壤及行星矿床。",
        "resource.tensor.6.title": "⛓️ 4.2.6 工业金属 BIF 铁矿与斑岩铜矿 (IRON_COPPER)",
        "resource.tensor.6.desc": "条带状铁矿 (BIF) 与斑岩铜矿床。",
        "resource.tensor.7.title": "💎 4.2.7 稀土、锂盐湖与锂辉石 (PRECIOUS_REE / Li)",
        "resource.tensor.7.desc": "黄金、铂金、稀土元素 (REE) 矿床及锂盐湖。",
        "resource.tensor.8.title": "💧 4.2.8 含水层与大盆地淡水 (FRESHWATER_AQUIFERS — WHYMAP)",
        "resource.tensor.8.desc": "深层地下水与化石含水层。数据来源：UNESCO WHYMAP。",
        "resource.tensor.custom.title_prefix": "⛏️ 4.2.",
        "resource.tensor.custom.title_mid": " 地质张量图层 ",
        "resource.tensor.custom.desc": "可扩展地质图层。",

        # Cultural Tensors Previews
        "scenario.tensor.1.preview": "📜 张量 1：同言线与语言连续体 (语言)",
        "scenario.tensor.2.preview": "🏛 张量 2：亲属关系与氏族结构 (亲属)",
        "scenario.tensor.3.preview": "🔮 张量 3：仪式、信仰与神圣 (Asabiyyah 凝聚力)",
        "scenario.tensor.4.preview": "👑 张量 4：政治军事主权与首都",
        "scenario.tensor.5.preview": "🏺 张量 5：工具、物质性与技术 (文物)",
        "scenario.tensor.6.preview": "🐫 张量 6：走廊与贸易网络 (经济路线)",
        "scenario.tensor.7.preview": "⚖ 张量 7：制度复杂性与规范 (Seshat 与法律)",
        "scenario.tensor.8.preview": "⚠️ 张量 8：生态足迹与马尔萨斯张力 (环境退化)",
        "scenario.tensor.9.preview": "🧬 张量 9：病原体免疫与健康记忆 (流行病学)",
        "scenario.tensor.custom.preview_prefix": "🧬 张量 ",
        "scenario.tensor.custom.preview_mid": "：文化基质 ",

        # Analytics
        "analytics.scenario.ground_truth": "🌍 历史现实 (历史动态学基线)",
        "analytics.btn.executing": "⏳ 执行中...",
        "analytics.divergence.break_point": "临界断点：请至少选择 2 个已执行情景",
        "analytics.divergence.select_two_hint": "在表格中勾选至少两个情景（例如：历史现实 + 模拟情景）以分析偏差。",
        "analytics.divergence.prompt_report": "## 请至少选择两个已执行的情景，以生成比较摘要与审计报告。",

        # Built-in Scenario Names & Descriptions
        "scenario.preset.out_of_africa.name": "走出非洲与智人扩张 (-100,000 BP)",
        "scenario.preset.out_of_africa.desc": "🌍 旧石器时代情景：非洲摇篮、跨洲迁徙与走出非洲 (-100,000 BC)\n\n模拟早期智人从东非跨越中东、欧亚大陆、大洋洲及美洲的人口动态与空间扩张。",

        "scenario.preset.sahul.name": "萨胡尔与澳大利亚首次定居 (-50,000 BP)",
        "scenario.preset.sahul.desc": "🦘 旧石器时代情景：跨海迁徙与进入萨胡尔 (-50,000 BC)\n\n澳大利亚原住民祖先首次跨越华莱士线。模拟萨胡尔大陆（澳大利亚、塔斯马尼亚、新几内亚）的定居及对干旱环境的适应。",

        "scenario.preset.beringia.name": "白令陆桥与美洲定居 (-25,000 BP)",
        "scenario.preset.beringia.desc": "🏔️ 旧石器时代情景：白令陆桥与美洲迁徙 (-25,000 BC)\n\n模拟末次盛冰期 (LGM) 期间旧石器人群在白令陆桥的隔离，及其随后沿无冰走廊和太平洋沿岸路线的扩散。",

        "scenario.preset.younger_dryas.name": "新仙女木期与纳图夫采集者冲击 (-10,900 BP)",
        "scenario.preset.younger_dryas.desc": "❄️ 古气候情景：新仙女木期与黎凡特采集压力 (-10,900 BC)\n\n阿加西兹湖淡水喷发引发北大西洋 5-8°C 剧烈降温。黎凡特严重干旱导致野生谷物减少，迫使纳图夫采集者走向前农业定居。",

        "scenario.preset.fertile_crescent.name": "新月沃地与新石器革命 (-8000 BP)",
        "scenario.preset.fertile_crescent.desc": "🌾 历史情景：新月沃地农业的曙光 (-8000 BC)\n\n模拟从狩猎采集生存模式向底格里斯河、幼发拉底河、尼罗河及黎凡特沿岸最早农业社区的重大转变。",

        "scenario.preset.green_sahara.name": "绿色撒哈拉与非洲湿润期 (-6000 BP)",
        "scenario.preset.green_sahara.desc": "🌴 古气候情景：绿色撒哈拉与游牧绿洲时代 (-6000 BC)\n\n增强的非洲季风将撒哈拉变成拥有大湖泊（超级乍得湖）的繁茂稀树草原。游牧群体在全新世中期沙漠化前繁荣发展。",

        "scenario.preset.ancient_egypt.name": "埃及统一与早期王朝 (-3100 BP)",
        "scenario.preset.ancient_egypt.desc": "🏛️ 历史情景：纳尔迈统一上下埃及 (-3100 BC)\n\n尼罗河走廊沿线第一个领土国家出现，围绕泛滥农业、集中储藏和神圣王权组织建立。",

        "scenario.preset.assyrian_empire.name": "青铜时代崩溃与新亚述帝国 (-1200 BP)",
        "scenario.preset.assyrian_empire.desc": "⚔️ 历史情景：青铜时代晚期崩溃与亚述帝国扩张 (-1200 BC)\n\n模拟东地中海贸易网络系统性崩溃、干旱危机、铁器冶炼崛起与军事化帝国建设。",

        "scenario.preset.mesoamerica.name": "奥尔梅克霸权与中美洲城市化 (-1200 BP)",
        "scenario.preset.mesoamerica.desc": "🏺 历史情景：奥尔梅克核心区与早期中美洲城市化 (-1200 BC)\n\n玉米农业、玄武岩巨石建筑、仪式球赛以及湾岸热带低地的祭祀中心网络。",

        "scenario.preset.maurya_empire.name": "孔雀帝国与铁器时代丝绸之路 (-322 BP)",
        "scenario.preset.maurya_empire.desc": "🐘 历史情景：孔雀帝国统一与泛亚贸易路线 (-322 BC)\n\n印度次大陆在旃陀罗笈多和阿育王统治下统一，敕令编纂，陆上与海上丝绸之路扩张。",

        "scenario.preset.roman_empire.name": "罗马和平与地中海贸易鼎盛 (117 AD)",
        "scenario.preset.roman_empire.desc": "🏛️ 历史情景：罗马和平与地中海经济一体化 (117 AD)\n\n罗马帝国领土达到最大范围、帝国公路网、海上粮食贸易、货币贬值动态与城市卫生。",

        "scenario.preset.late_antique_ice_age.name": "古代晚期小冰期与民族大迁徙 (536 AD)",
        "scenario.preset.late_antique_ice_age.desc": "❄️ 古气候情景：公元 536 年火山冬天与古代晚期危机\n\n三次连续火山喷发引发全球剧烈降温、粮食减产、查士丁尼大瘟疫及欧亚游牧民族大迁徙。",

        "scenario.preset.song_dynasty.name": "宋代工业化与海上贸易 (1000 AD)",
        "scenario.preset.song_dynasty.desc": "🐉 历史情景：宋代原工业革命与海上丝绸之路 (1000 AD)\n\n宋朝经济繁荣、交子纸币、焦炭炼铁、指南针航海及高度商业化的水路网络。",

        "scenario.preset.mali_empire.name": "马里帝国与跨撒哈拉黄金之路 (1324 AD)",
        "scenario.preset.mali_empire.desc": "🕌 历史情景：曼萨·穆萨朝圣与跨撒哈拉贸易鼎盛 (1324 AD)\n\n跨撒哈拉黄金与盐业贸易、桑科雷大学学术中心、商队物流及西非萨赫勒城市化。",

        "scenario.preset.americas_1491.name": "哥伦布到达前的美洲鼎盛 (1491 AD)",
        "scenario.preset.americas_1491.desc": "🏹 历史情景：接触前的美洲大陆基线 (1491 AD)\n\n印加梯田工程、阿兹特克浮动农田水利、密西西比丘陵中心及亚马逊黑土 (Terra Preta)。",

        "scenario.preset.columbian_contact.name": "哥伦布大交换与全球贸易网 (1500 AD)",
        "scenario.preset.columbian_contact.desc": "⛵ 历史情景：大接触与全球生物大交换 (1500 AD)\n\n两半球会合、跨大西洋作物转移（玉米、土豆）、白银流动与毁灭性流行病冲击。",

        "scenario.preset.tokugawa_japan.name": "德川幕府锁国与江户稳定 (1603 AD)",
        "scenario.preset.tokugawa_japan.desc": "⛩️ 历史情景：德川和平与锁国政策 (1603 AD)\n\n长达两个世纪的森林资源可持续管理、人口稳定、江户城市零增长平衡与严格的海禁。",

        "scenario.preset.industrial_1800.name": "第一次工业革命与化石能源 (1800 AD)",
        "scenario.preset.industrial_1800.desc": "⚙️ 历史情景：大分流与蒸汽化石转型 (1800 AD)\n\n从有机经济向化石能源（煤炭）体制转型、蒸汽机机械化、快速城市化与人口爆炸。",

        "scenario.preset.anthropocene_2000.name": "大加速与人类世基线 (2000 AD)",
        "scenario.preset.anthropocene_2000.desc": "🌐 当代情景：全球大加速与行星边界 (2000 AD)\n\n全球化高度互联、61 亿人口、哈伯-博施合成氮肥依赖、二氧化碳温室气体强迫与数字网格。",

        "scenario.preset.ssp5_85.name": "SSP5-8.5 化石燃料高排放情景 (2026 AD)",
        "scenario.preset.ssp5_85.desc": "🔥 未来情景：SSP5-8.5 极端气候轨迹 (2026 AD)\n\n基于化石燃料的高速发展、加速全球变暖、海平面上升及区域马尔萨斯农业压力。",

        "scenario.preset.singularity_2045.name": "技术奇点与通用人工智能转型 (2045 AD)",
        "scenario.preset.singularity_2045.desc": "🤖 未来情景：后稀缺与超智能转型 (2045 AD)\n\n指数级技术增长、全自动化生产、分子纳米技术、太空资源开采及后人类社会变革。",

        "scenario.preset.nuclear_winter_2035.name": "全面核战争与核冬天 (2035 AD)",
        "scenario.preset.nuclear_winter_2035.desc": "⚠️ 危机情景：热核冲突与平流层烟尘冲击 (2035 AD)\n\n1.5 亿吨平流层烟尘注入、全球太阳辐射骤降、全球冰冻、作物绝收与社会崩塌。",

        "scenario.preset.peak_phosphate_2050.name": "全球磷矿枯竭与马尔萨斯危机 (2050 AD)",
        "scenario.preset.peak_phosphate_2050.desc": "🌾 危机情景：化肥枯竭与粮食系统压力 (2050 AD)\n\n高品位磷酸盐矿石枯竭、化肥价格飙升、农业减产及全球人口张力。",

        "scenario.preset.supervolcano_2060.name": "超级火山喷发与火山冬天 (2060 AD)",
        "scenario.preset.supervolcano_2060.desc": "🌋 灾难情景：VEI-8 超级喷发与十年全球降温 (2060 AD)\n\n大量二氧化硫注入、持续数 dynamically 纪的火山冬天、全球季风中断及人口瓶颈。"
    }
}

# For de and es, default to en for missing detailed descriptions if not yet localized, but provide core translations
data["de"] = dict(data["en"])
data["es"] = dict(data["en"])

def update_properties_file(file_path, key_value_map):
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        return

    # Read existing lines
    with open(file_path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    existing_keys = set()
    for line in lines:
        stripped = line.strip()
        if stripped and not stripped.startswith("#") and "=" in stripped:
            k = stripped.split("=", 1)[0].strip()
            existing_keys.add(k)

    new_lines = []
    added_count = 0
    for k, v in key_value_map.items():
        if k not in existing_keys:
            # Escape newlines as \\n for properties files
            v_escaped = v.replace("\n", "\\n")
            new_lines.append(f"{k}={v_escaped}\n")
            added_count += 1

    if new_lines:
        with open(file_path, "a", encoding="utf-8") as f:
            f.write("\n# Additional Injected Scenario & Geology Keys\n")
            f.writelines(new_lines)
        print(f"Added {added_count} keys to {os.path.basename(file_path)}")
    else:
        print(f"No new keys added to {os.path.basename(file_path)} (all exist)")

for lang, kv_map in data.items():
    file_path = os.path.join(i18n_dir, f"messages_{lang}.properties")
    update_properties_file(file_path, kv_map)
