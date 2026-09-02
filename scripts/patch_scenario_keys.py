import re

target_file = r"c:\Silvere\Encours\Developpement\Ether\src\main\java\org\ether\society\model\Scenario.java"

with open(target_file, "r", encoding="utf-8") as f:
    content = f.read()

replacements = [
    ("s0.setName(\"Sortie d'Afrique & Expansion Homo Sapiens (-100000)\");", "s0.setPresetKey(\"out_of_africa\");\n        s0.setName(\"Sortie d'Afrique & Expansion Homo Sapiens (-100000)\");"),
    ("sSahul.setName(\"Sahul & Premier Peuplement de l'Australie (-50000)\");", "sSahul.setPresetKey(\"sahul\");\n        sSahul.setName(\"Sahul & Premier Peuplement de l'Australie (-50000)\");"),
    ("sBeringia.setName(\"Béringie & Peuplement des Amériques (-25000)\");", "sBeringia.setPresetKey(\"beringia\");\n        sBeringia.setName(\"Béringie & Peuplement des Amériques (-25000)\");"),
    ("sYoungerDryas.setName(\"Le Récents Dryas & Choc Climatique Natufien (-10900)\");", "sYoungerDryas.setPresetKey(\"younger_dryas\");\n        sYoungerDryas.setName(\"Le Récents Dryas & Choc Climatique Natufien (-10900)\");"),
    ("s1.setName(\"Croissant Fertile & Néolithique (-8000)\");", "s1.setPresetKey(\"fertile_crescent\");\n        s1.setName(\"Croissant Fertile & Néolithique (-8000)\");"),
    ("sGreenSahara.setName(\"Le Sahara Vert & Période Humide Africaine (-6000)\");", "sGreenSahara.setPresetKey(\"green_sahara\");\n        sGreenSahara.setName(\"Le Sahara Vert & Période Humide Africaine (-6000)\");"),
    ("sEgypt.setName(\"Égypte Antique & Vallée du Nil (-3000)\");", "sEgypt.setPresetKey(\"ancient_egypt\");\n        sEgypt.setName(\"Égypte Antique & Vallée du Nil (-3000)\");"),
    ("s3.setName(\"Empire Assyrien & Irrigation Mésopotamienne (-2000)\");", "s3.setPresetKey(\"assyrian_empire\");\n        s3.setName(\"Empire Assyrien & Irrigation Mésopotamienne (-2000)\");"),
    ("sMeso.setName(\"Civilisations Mésoaméricaines (Olmèques & Mayas) (-1500)\");", "sMeso.setPresetKey(\"mesoamerica\");\n        sMeso.setName(\"Civilisations Mésoaméricaines (Olmèques & Mayas) (-1500)\");"),
    ("sMaurya.setName(\"Empire Maurya & Civilisation de l'Indus-Gange (-300)\");", "sMaurya.setPresetKey(\"maurya_empire\");\n        sMaurya.setName(\"Empire Maurya & Civilisation de l'Indus-Gange (-300)\");"),
    ("sRoman.setName(\"Empire Romain & Pax Romana (An 0)\");", "sRoman.setPresetKey(\"roman_empire\");\n        sRoman.setName(\"Empire Romain & Pax Romana (An 0)\");"),
    ("s2.setName(\"Le Petit Âge Glaciaire de l'Antiquité Tardive & Peste de Justinien (536)\");", "s2.setPresetKey(\"late_antique_ice_age\");\n        s2.setName(\"Le Petit Âge Glaciaire de l'Antiquité Tardive & Peste de Justinien (536)\");"),
    ("s4.setName(\"Dynastie Song & Pré-Industrialisation Hydraulique (1000)\");", "s4.setPresetKey(\"song_dynasty\");\n        s4.setName(\"Dynastie Song & Pré-Industrialisation Hydraulique (1000)\");"),
    ("sMali.setName(\"Empire du Mali & Commerce Trans-Saharien (1324)\");", "sMali.setPresetKey(\"mali_empire\");\n        sMali.setName(\"Empire du Mali & Commerce Trans-Saharien (1324)\");"),
    ("sAmericas1491.setName(\"Amériques Précolombiennes : Tawantinsuyu & Anahuac (1491)\");", "sAmericas1491.setPresetKey(\"americas_1491\");\n        sAmericas1491.setName(\"Amériques Précolombiennes : Tawantinsuyu & Anahuac (1491)\");"),
    ("sColumbian.setName(\"Arrivée des Européens aux Amériques & Choc Microbiens (1492)\");", "sColumbian.setPresetKey(\"columbian_contact\");\n        sColumbian.setName(\"Arrivée des Européens aux Amériques & Choc Microbiens (1492)\");"),
    ("sSakoku.setName(\"Japon Tokugawa & Isolement Sakoku (1639)\");", "sSakoku.setPresetKey(\"tokugawa_japan\");\n        sSakoku.setName(\"Japon Tokugawa & Isolement Sakoku (1639)\");"),
    ("sIndustrial1800.setName(\"Révolution Industrielle & Transition Charbonnière (1800)\");", "sIndustrial1800.setPresetKey(\"industrial_1800\");\n        sIndustrial1800.setName(\"Révolution Industrielle & Transition Charbonnière (1800)\");"),
    ("sModern2000.setName(\"Anthropocène & Grande Accélération Mondiale (2000)\");", "sModern2000.setPresetKey(\"anthropocene_2000\");\n        sModern2000.setName(\"Anthropocène & Grande Accélération Mondiale (2000)\");"),
    ("s5.setName(\"Business As Usual : Fossil Fuel Reliance & Warming (SSP5-8.5)\");", "s5.setPresetKey(\"ssp5_85\");\n        s5.setName(\"Business As Usual : Fossil Fuel Reliance & Warming (SSP5-8.5)\");"),
    ("s6.setName(\"Singularité Technologique, ASI & Fusion D-T (2045)\");", "s6.setPresetKey(\"singularity_2045\");\n        s6.setName(\"Singularité Technologique, ASI & Fusion D-T (2045)\");"),
    ("s7.setName(\"Hiver Nucléaire & Ombre Stratosphérique (2035)\");", "s7.setPresetKey(\"nuclear_winter_2035\");\n        s7.setName(\"Hiver Nucléaire & Ombre Stratosphérique (2035)\");"),
    ("s8.setName(\"Falaise du Phosphate Minéral & Crise N-P-K (2050)\");", "s8.setPresetKey(\"peak_phosphate_2050\");\n        s8.setName(\"Falaise du Phosphate Minéral & Crise N-P-K (2050)\");"),
    ("s9.setName(\"Super-Éruption Volcanique Toba/Yellowstone (2060)\");", "s9.setPresetKey(\"supervolcano_2060\");\n        s9.setName(\"Super-Éruption Volcanique Toba/Yellowstone (2060)\");"),
]

for target, replacement in replacements:
    if target in content:
        content = content.replace(target, replacement)
    else:
        print(f"Warning: target not found: {target}")

with open(target_file, "w", encoding="utf-8") as f:
    f.write(content)

print("Updated Scenario.java with preset keys successfully.")
