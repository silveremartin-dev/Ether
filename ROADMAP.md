# 🗺️ Ether Simulation - Roadmap Technique

**Version:** 2.0.0-SNAPSHOT  
**Dernière mise à jour:** 23 novembre 2025  
**Statut actuel:** ✅ v1.0 - Core Simulation Fonctionnel

---

## 📍 Où nous sommes

### ✅ Phase 5 Complétée - MVP v1.0 (23/11/2025)
**Réalisations:**
- ✅ Build Maven réussi (`mvn clean package`)
- ✅ Application JavaFX lancée et fonctionnelle
- ✅ Simulation 100×100 cellules avec agents
- ✅ Système de temps (20,000 av. J.-C. → présent)
- ✅ Contrôles UI (Start/Pause/Vitesse)
- ✅ 18 commits Git documentés

**Limites actuelles:**
- ⚠️ Système de grille rectangulaire (non-hexagonale)
- ⚠️ Pas de persistence en base de données
- ⚠️ Pas d'accélération GPU (CPU uniquement)
- ⚠️ Données procédurales (pas de vraies données terrestres)

---

## 🎯 Les 3 Prochaines Phases (Prioritaire)

### **Phase 6: Restauration Grille Hexagonale H3** 
**Durée estimée:** 1 session (2-3 heures)  
**Priorité:** 🔴 HAUTE  
**Objectif:** Passer de la grille rectangulaire à la grille hexagonale H3

#### Pourquoi H3 ?
H3 est un système de grille hexagonale développé par Uber qui offre:
- **Couverture uniforme** de la Terre (~691M hexagones au Level 8)
- **Distance constante** entre voisins (contrairement aux grilles carrées)
- **Indexation efficace** pour les requêtes spatiales
- **Multi-résolution** (zoom du Level 0 au Level 15)

**Level 8 choisi:**
- 📏 **~0.74 km²** par hexagone (860m × 860m)
- 📍 **~461 mètres** de rayon
- 🌍 **691 millions** d'hexagones pour toute la Terre
- 🇪🇺 **~13.5 millions** pour l'Europe entière
- 🎯 **50,000 hexagones** dans notre sample MVP

#### Le Problème Actuel
Nous avons créé un système H3 complet (commits `e0856b1`, `13a4802`, `1c01eb2`) mais il a été temporairement retiré car:
```
[ERROR] module not found: com.uber.h3
[ERROR] package jakarta.persistence is not visible
```
**Raison:** La bibliothèque H3 n'est pas modulaire (pas de `module-info.java`), donc incompatible avec le système de modules Java 21.

#### Solutions Possibles

##### **Option A: Supprimer module-info.java** (RAPIDE ⚡)
```bash
# 1. Supprimer le fichier module
rm src/main/java/module-info.java

# 2. Restaurer les commits H3
git cherry-pick e0856b1  # Entités persistence
git cherry-pick 13a4802  # Générateur données sample
git cherry-pick 1c01eb2  # H3GridInitializer

# 3. Rebuild
mvn clean package -DskipTests
```

**Avantages:**
- ✅ Fonctionne immédiatement (5 minutes)
- ✅ Toutes les dépendances accessibles
- ✅ Pas de configuration complexe

**Inconvénients:**
- ❌ Perd les avantages du système de modules
- ❌ Moins d'encapsulation
- ❌ Retour en arrière technique

##### **Option B: JVM Flags** (RECOMMANDÉ ⭐)
Garder `module-info.java` mais ajouter des flags JVM pour autoriser l'accès aux JARs non-modulaires.

**Modification dans pom.xml:**
```xml
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>com.ether.society.Main</mainClass>
        <options>
            <option>--add-modules</option>
            <option>ALL-MODULE-PATH</option>
            <option>--add-reads</option>
            <option>com.ether.society=ALL-UNNAMED</option>
            <option>--add-opens</option>
            <option>java.base/java.lang=ALL-UNNAMED</option>
        </options>
    </configuration>
</plugin>
```

**Avantages:**
- ✅ Garde les modules Java 21
- ✅ Meilleure encapsulation
- ✅ Performance optimale
- ✅ Compatible avec futures versions

**Inconvénients:**
- ⚠️ Configuration plus complexe
- ⚠️ Nécessite de comprendre le système de modules

##### **Option C: Multi-Module Maven** (AVANCÉ 🔬)
Créer un projet multi-modules:
```
ether/
├── ether-core/         (module)
├── ether-h3/           (non-module, H3 wrapper)
├── ether-database/     (module)
├── ether-ui/           (module)
└── ether-app/          (assemble tout)
```

**Avantages:**
- ✅ Séparation des préoccupations
- ✅ Build granulaire
- ✅ Testabilité maximale

**Inconvénients:**
- ❌ Complexité élevée
- ❌ Temps de setup long (1-2 jours)
- ❌ Overkill pour MVP

#### Recommandation: **Option B** 
C'est le meilleur compromis entre simplicité et architecture propre.

#### Étapes Détaillées (Option B)

**1. Décommenter les dépendances H3 et JPA dans pom.xml**
```xml
<!-- Actuellement commenté, à réactiver -->
<dependency>
    <groupId>com.uber</groupId>
    <artifactId>h3</artifactId>
    <version>4.1.1</version>
</dependency>

<!-- JPA pour persistence -->
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
    <version>3.1.0</version>
</dependency>
```

**2. Modifier module-info.java**
```java
module com.ether.society {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    
    // Pas de "requires com.uber.h3" car non-modulaire
    // Accessible via classpath avec --add-modules
    
    opens com.ether.society to javafx.fxml;
    opens com.ether.society.ui to javafx.fxml;
    opens com.ether.society.model to javafx.base;
    opens com.ether.society.database to org.hibernate.orm.core;
    
    exports com.ether.society;
    exports com.ether.society.core;
    exports com.ether.society.h3;
    exports com.ether.society.database;
}
```

**3. Ajouter les JVM flags (voir Option B ci-dessus)**

**4. Restaurer les fichiers H3 depuis Git**
```bash
# Créer une branche pour tester
git checkout -b feature/restore-h3

# Cherry-pick les commits
git cherry-pick e0856b1  # Persistence
git cherry-pick 13a4802  # Sample data
git cherry-pick 1c01eb2  # H3 grid

# Résoudre les conflits si nécessaire
git status
```

**5. Rebuild et Test**
```bash
mvn clean package -DskipTests
mvn javafx:run
```

**6. Vérifier que ça fonctionne**
- [ ] L'application se lance sans erreur
- [ ] H3Service accessible
- [ ] SampleDataGenerator génère 50K cellules Europe
- [ ] Affichage dans les logs: "Generated 50000 H3 cells for region Europe"

#### Résultat Attendu

**Avant (Phase 5):**
```
Grid: 100×100 = 10,000 cellules carrées
Données: Procédurales (Perlin noise)
Stockage: Mémoire uniquement
```

**Après (Phase 6):**
```
Grid: H3 Level 8 = 50,000 hexagones (Europe)
Données: Sample réaliste (élévation, biomes)
Stockage: Prêt pour PostgreSQL
Visualisation: Hexagones colorés par biome
```

#### Métriques de Succès
- [ ] 50K+ hexagones H3 chargés
- [ ] Rendu JavaFX des hexagones
- [ ] Couleur par type (océan bleu, forêt vert, désert jaune)
- [ ] Hover souris montre: lat/lng, élévation, biome
- [ ] Performance: 30+ FPS avec 50K hexagones

---

### **Phase 7: Persistence Base de Données**
**Durée estimée:** 2-3 sessions  
**Priorité:** 🟡 MOYENNE  
**Objectif:** Sauvegarder et charger les états de simulation

#### Architecture Persistence

```
┌─────────────────────────────────────────┐
│         Application (Java)              │
├─────────────────────────────────────────┤
│  Spring Data JPA Repositories           │
├─────────────────────────────────────────┤
│  PostgreSQL + PostGIS (Docker)          │
│  - world_maps (métadonnées cartes)      │
│  - simulations (états simulation)       │
│  - h3_cells (7.2M hexagones)            │
│  - density_maps (ressources GJ/km²)     │
├─────────────────────────────────────────┤
│  Redis (Cache L1)                       │
│  - Hot cells (cellules actives)         │
│  - Session data                         │
└─────────────────────────────────────────┘
```

#### Entités JPA (déjà créées dans commit `e0856b1`)

**1. WorldMap** - Métadonnées de carte
```java
@Entity
public class WorldMap {
    @Id private Long id;
    private String name;              // "Earth", "Mars", etc.
    private String type;              // "REAL", "PROCEDURAL"
    private int h3Resolution;         // 8 pour ~1km²
    private String planetName;
    private double planetRadius;      // 6371 km (Terre)
    private String dataSourceElevation;  // "SRTM v3"
    private String dataSourceBiome;      // "MODIS"
    
    @OneToMany
    private List<Simulation> simulations;
}
```

**2. Simulation** - État de simulation
```java
@Entity
public class Simulation {
    @Id private Long id;
    
    @ManyToOne
    private WorldMap worldMap;
    
    private int startYear;      // -20000 (20,000 BC)
    private int currentYear;
    private int currentMonth;
    private int tickCount;
    
    private SimulationState state;  // RUNNING, PAUSED, FINISHED
    
    private long totalPopulation;
    private double globalTemperatureOffset;  // +1.5°C
    
    @OneToMany
    private List<DensityMap> densityMaps;
}
```

**3. H3Cell** - Données hexagone
```java
@Entity
@Table(indexes = {
    @Index(name = "idx_h3_index", columnList = "h3Index"),
    @Index(name = "idx_region", columnList = "region")
})
public class H3Cell {
    @Id private Long id;
    private String h3Index;     // "881f1a48c7fffff"
    
    // Géométrie
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point centroid;     // PostGIS
    private double latitude;
    private double longitude;
    
    // Terrain
    private double elevation;    // mètres
    private String biome;        // "FOREST", "OCEAN"
    
    // Climat
    private double temperature;  // °C
    private double precipitation; // mm/an
    
    // Ressources (GJ/km²)
    private double biomass;
    private double minerals;
    private double water;
    
    // Métadonnées
    private String region;       // "Europe", "Asia"
    private String country;
}
```

**4. DensityMap** - Distribution ressources
```java
@Entity
public class DensityMap {
    @Id private Long id;
    
    @ManyToOne
    private Simulation simulation;
    
    private String h3Index;
    private ResourceType resourceType;  // POPULATION, FOOD, MINERALS
    
    private double density;             // GJ/km²
    private FractalPattern pattern;     // UNIFORM, CLUSTERED, DISPERSED
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdated;
}
```

#### Workflow Persistence

**1. Initialisation (premier lancement)**
```java
// 1. Créer la carte du monde
WorldMap earth = new WorldMap();
earth.setName("Earth");
earth.setH3Resolution(8);
earth.setPlanetRadius(6371.0);
worldMapRepository.save(earth);

// 2. Créer une simulation
Simulation sim = new Simulation();
sim.setWorldMap(earth);
sim.setStartYear(-20000);
sim.setState(SimulationState.RUNNING);
simulationRepository.save(sim);

// 3. Charger les données H3
List<H3Cell> cells = sampleDataGenerator.generateEurope();
h3CellRepository.saveAll(cells);  // Batch insert de 50K cellules
```

**2. Sauvegarde pendant simulation**
```java
// Toutes les 100 ticks
if (tickCount % 100 == 0) {
    simulation.setCurrentYear(timeManager.getYear());
    simulation.setTotalPopulation(world.getAgents().size());
    simulationRepository.save(simulation);
    
    // Sauvegarder les densités modifiées
    List<DensityMap> modified = getModifiedDensities();
    densityMapRepository.saveAll(modified);
}
```

**3. Chargement d'une sauvegarde**
```java
Simulation saved = simulationRepository.findById(savedId);
WorldMap map = saved.getWorldMap();

// Charger les cellules de la région
List<H3Cell> cells = h3CellRepository
    .findByWorldMapAndRegion(map.getId(), "Europe");

// Restaurer l'état
engine.loadFromSimulation(saved, cells);
```

#### Configuration Docker

**Lancer les services:**
```bash
docker-compose up -d

# Vérifier
docker ps
# Devrait montrer:
# - postgres:16 (port 5432)
# - postgis/postgis:16-3.4 (extension PostGIS)
# - redis:7-alpine (port 6379)
```

**Schema SQL (déjà dans `scripts/init-db.sql`):**
```sql
-- Extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tables (voir commit e0856b1)
CREATE TABLE world_maps (...);
CREATE TABLE simulations (...);
CREATE TABLE h3_cells (...);
CREATE TABLE density_maps (...);

-- Index optimisés
CREATE INDEX idx_h3_index ON h3_cells(h3_index);
CREATE INDEX idx_region ON h3_cells(region);
CREATE INDEX idx_simulation ON density_maps(simulation_id);
```

#### Métriques de Succès
- [ ] Docker services démarrés
- [ ] Connexion Spring Boot ↔ PostgreSQL OK
- [ ] Insert de 50K cellules < 2 secondes
- [ ] Query cellules par région < 100ms
- [ ] Save/Load simulation complète < 5 secondes
- [ ] Redis cache hit ratio > 80%

---

### **Phase 8: Données Terrestres Réelles**
**Durée estimée:** 1 semaine  
**Priorité:** 🟢 BASSE (après Phases 6-7)  
**Objectif:** Remplacer données procédurales par vraies données

#### Sources de Données

| Type | Source | Résolution | Coverage | Taille |
|------|--------|------------|----------|--------|
| **Élévation** | SRTM v3 | 90m (~3 arcsec) | Monde entier | ~25 GB |
| **Biomes** | MODIS Land Cover | 500m | Monde entier | ~2 GB |
| **Population** | SEDAC GPW v4 | 1km | Monde entier | ~500 MB |
| **Climat** | WorldClim 2.1 | 1km | Monde entier | ~10 GB |

#### Pipeline de Traitement

```
┌──────────────────────┐
│ 1. Téléchargement    │
│    - SRTM tiles      │
│    - MODIS HDF       │
│    - WorldClim TIF   │
└──────────┬───────────┘
           │
┌──────────▼───────────┐
│ 2. Conversion        │
│    - HDF → GeoTIFF   │
│    - Reprojection    │
│    - WGS84 (EPSG:4326)│
└──────────┬───────────┘
           │
┌──────────▼───────────┐
│ 3. Échantillonnage   │
│    - H3 Level 8 grid │
│    - Centroid sample │
│    - Bilinear interp │
└──────────┬───────────┘
           │
┌──────────▼───────────┐
│ 4. PostgreSQL        │
│    - Bulk insert     │
│    - Index spatial   │
│    - Vacuum analyze  │
└──────────────────────┘
```

#### Implémentation Java

**1. Download Manager**
```java
@Service
public class DataDownloadService {
    
    // SRTM: 1° × 1° tiles
    public void downloadSRTM(double lat, double lng) {
        String url = String.format(
            "https://srtm.csi.cgiar.org/wp-content/uploads/files/srtm_5x5/TIFF/srtm_%02d_%02d.zip",
            getTileX(lng), getTileY(lat)
        );
        downloadAndExtract(url, "data/srtm/");
    }
    
    // MODIS Land Cover
    public void downloadMODIS(int year) {
        String url = "https://e4ftl01.cr.usgs.gov/MOTA/MCD12Q1.061/" + year;
        downloadHDF(url, "data/modis/");
    }
}
```

**2. GeoTIFF Processing** (utilise GeoTools)
```java
@Service
public class GeoTIFFProcessor {
    
    public double sampleElevation(double lat, double lng) {
        GeoTiffReader reader = new GeoTiffReader(elevationFile);
        GridCoverage2D coverage = reader.read(null);
        
        DirectPosition pos = new DirectPosition2D(lng, lat);
        double[] values = coverage.evaluate(pos, (double[]) null);
        
        return values[0];  // Élévation en mètres
    }
}
```

**3. H3 Grid Sampler**
```java
@Service
public class H3GridSampler {
    
    public List<H3Cell> sampleEarth(int resolution) {
        List<H3Cell> cells = new ArrayList<>();
        
        // Itérer sur tous les hexagones niveau 8 (7.2M)
        for (long h3Index : H3Core.getAllCellsAtResolution(resolution)) {
            LatLng center = h3.h3ToLatLng(h3Index);
            
            H3Cell cell = new H3Cell();
            cell.setH3Index(h3.h3ToString(h3Index));
            cell.setLatitude(center.lat);
            cell.setLongitude(center.lng);
            
            // Sample données
            cell.setElevation(geoTIFF.sampleElevation(center.lat, center.lng));
            cell.setBiome(modis.sampleBiome(center.lat, center.lng));
            cell.setTemperature(worldClim.sampleTemp(center.lat, center.lng));
            
            cells.add(cell);
            
            // Batch insert tous les 10K
            if (cells.size() >= 10000) {
                h3CellRepository.saveAll(cells);
                cells.clear();
            }
        }
        
        return cells;
    }
}
```

#### Performance Optimization

**Parallélisation:**
```java
// Traiter par régions en parallèle
List<String> regions = List.of("Europe", "Asia", "Americas", "Africa", "Oceania");

regions.parallelStream().forEach(region -> {
    Bounds bounds = getRegionBounds(region);
    List<H3Cell> cells = sampleRegion(bounds);
    h3CellRepository.saveAll(cells);
});
```

**Batch Insert:**
```properties
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 1000
        order_inserts: true
        order_updates: true
```

#### Métriques de Succès
- [ ] Téléchargement SRTM complet (25 GB)
- [ ] Conversion GeoTIFF → H3 samples
- [ ] 7.2M cellules insérées en DB
- [ ] Temps total traitement: < 24 heures
- [ ] Précision élévation: ±10m
- [ ] Coverage: 100% terres émergées

---

## 🚀 Phases Avancées (Long Terme)

### **Phase 9: GPU Acceleration (TornadoVM)**
**Durée:** 2-3 semaines  
**Objectif:** 100x speedup sur kernels parallèles

**Kernels prioritaires:**
1. Climate Update (7.2M cells)
2. Resource Renewal (biomasse, minéraux)
3. Agent Movement (100K+ humains)

**Benchmark visé:**
- Climate: 10ms pour 7.2M cells
- Resources: 5ms pour toutes les cellules
- Agents: 5ms pour 1M agents
- **Total tick:** 16ms (60 FPS possible)

---

### **Phase 10: Visualisation Avancée**
**Durée:** 1-2 semaines  
**Objectif:** Rendering performant et beau

**JavaFX:**
- Viewport culling (LOD)
- GPU canvas acceleration
- Heat maps (température, densité)

**Web (optionnel):**
- React + TypeScript
- Deck.gl (hexagones 2D)
- Three.js (globe 3D)
- WebSocket temps réel

---

### **Phase 11: Intelligence Agents**
**Durée:** Ongoing  
**Objectif:** Comportements émergents

**Systèmes:**
- Behavior Trees
- Utility AI
- Pathfinding A*
- Group dynamics
- Cultural evolution

---

## 📊 Timeline Globale

```
Maintenant (Nov 2025)
│
├─ Phase 6: H3 Restoration          [2-3h]    ← PROCHAIN
├─ Phase 7: Database Persistence    [2-3j]
├─ Phase 8: Real Data Ingestion     [1sem]
│
Décembre 2025
│
├─ Phase 9: GPU Optimization        [2sem]
├─ Phase 10: Advanced Viz           [1sem]
│
Janvier 2026+
│
└─ Phase 11: Agent Intelligence     [ongoing]
```

---

## ✅ Checklist Quick Start (Phase 6)

### Préparation (10 min)
- [ ] Créer branche: `git checkout -b feature/restore-h3`
- [ ] Backup current: `git stash`
- [ ] Check commits existent: `git log --oneline | grep "H3\|persistence"`

### Option Rapide A (5 min)
- [ ] Supprimer: `rm src/main/java/module-info.java`
- [ ] Cherry-pick: `git cherry-pick e0856b1 13a4802 1c01eb2`
- [ ] Build: `mvn clean package -DskipTests`
- [ ] Run: `mvn javafx:run`

### Option Propre B (30 min)
- [ ] Modifier `pom.xml` (ajouter JVM flags)
- [ ] Modifier `module-info.java` (exports)
- [ ] Cherry-pick commits
- [ ] Résoudre conflits
- [ ] Build & test

### Vérification
- [ ] App démarre sans erreur
- [ ] Logs montrent: "Generated 50000 H3 cells"
- [ ] UI affiche hexagones
- [ ] Performance: 30+ FPS

---

**Prêt pour Phase 6 ?** 🚀  
Dis-moi quelle option tu préfères (A rapide ou B propre) !
