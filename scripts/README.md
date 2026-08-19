# 🚀 Ether 2.0 — Script Execution & Architecture Guide

Ce répertoire contient l'ensemble des scripts d'exécution, d'orchestration et de gestion d'infrastructure pour la simulation **Ether 2.0**.

---

## 🗄️ Architecture de Base de Données (H2 vs PostgreSQL)

Ether 2.0 supporte deux modes de fonctionnement pour la persistance et l'analyse spatiale :

1. **Mode PostgreSQL + PostGIS (via Docker)** :
   - Mode principal de persistance spatiale haute performance.
   - Utilisé pour enregistrer les données géospatiales des cellules H3 et la mémoire historique des simulations.
   - Lancé via `docker-compose.yml` (évite de devoir installer PostgreSQL localement sur la machine hôte).
   - Port par défaut : `54320` (Database: `ether_simulation`, User: `ether`).

2. **Mode Hors Ligne / Sans Base de Données (In-Memory)** :
   - Lancé via `start-no-db` ou automatiquement lorsque le serveur PostgreSQL n'est pas détecté.
   - La simulation tourne de manière fluide en mémoire sans aucune dépendance externe (pratique pour des tests rapides ou des exécutions légères).

---

## 📜 Repertoire des Scripts

### 1. ⚡ Exécution Rapide (Mode Sans Base de Données)
- **`start-no-db.bat`** (Windows CMD) / **`start-no-db.ps1`** (PowerShell) / **`start-no-db.sh`** (Linux/macOS)
  - **Description** : Lance l'interface graphique JavaFX d'Ether directement en mode hors ligne / mémoire. Ne requiert pas Docker ni PostgreSQL.

### 2. 🐳 Exécution Complète avec Base de Données Docker
- **`start-docker.bat`** (Windows CMD) / **`start-docker.sh`** (Linux/macOS)
  - **Description** : Vérifie Docker, démarre le conteneur PostgreSQL + PostGIS, attend l'initialisation de la BD, puis lance la simulation. À la fermeture de l'application, le conteneur est automatiquement arrêté.
- **`stop.bat`** / **`stop.sh`**
  - **Description** : Arrête les conteneurs Docker de la base de données.
- **`database-status.bat`** / **`database-status.sh`**
  - **Description** : Vérifie l'état de santé du conteneur PostgreSQL (`pg_isready`).

### 3. 🤖 Mode Headless (Simulation en Ligne de Commande sans GUI)
- **`start-headless.ps1`** (PowerShell) / **`start-headless.sh`** (Bash)
  - **Description** : Exécute le moteur de simulation Ether à haute vitesse en mode purement CLI sans surcoût JavaFX.
  - **Paramètres supportés** :
    - `-Scenario` / `--scenario=...` : Preset historique (ex: `OUT_OF_AFRICA`, `CLASSICAL_ANTIQUITY`, `MEDIEVAL_WARFARE`).
    - `-Ticks` / `--ticks=...` : Nombre de ticks à exécuter (ex: `100`).
    - `-Cells` / `--cells=...` : Nombre de cellules H3 à générer (ex: `3000`).
    - `-Profile` / `--profile` : Active le rapport de profilage détaillé des performances par sous-système.

  - **Exemple PowerShell** :
    ```powershell
    .\scripts\start-headless.ps1 -Scenario OUT_OF_AFRICA -Ticks 300 -Cells 5000 -Profile
    ```

### 4. 🌐 Cluster Distribué (Master / Worker Node)
- **`start-master.ps1`** (PowerShell) / **`start-master.sh`** (Bash)
  - **Description** : Lance le nœud **Master** d'un cluster gRPC distribué. Le Master écoute les connexions entrantes, partitionne les cellules H3 et distribue le calcul aux Workers.
  - **Paramètres** : `-Port` (défaut `9090`), `-Secret`, `-Scenario`, `-Ticks`, `-Cells`, `-Profile`.
  - **Exemple** :
    ```powershell
    .\scripts\start-master.ps1 -Port 9090 -Scenario OUT_OF_AFRICA -Ticks 1000
    ```

- **`start-worker.ps1`** (PowerShell) / **`start-worker.sh`** (Bash)
  - **Description** : Connecte un nœud **Worker** au nœud Master distant pour participer au calcul parallèle du maillage H3.
  - **Paramètres** : `-MasterHost` (défaut `localhost`), `-Port` (défaut `9090`), `-Secret`.
  - **Exemple** :
    ```powershell
    .\scripts\start-worker.ps1 -MasterHost "192.168.1.50" -Port 9090
    ```

### 5. 📚 Documentation & Lancement Standard
- **`run.bat`** / **`run.sh`** : Commande standard de lancement `mvn exec:java`.
- **`javadoc.bat`** / **`javadoc.sh`** : Génère la documentation Javadoc dans `target/site/apidocs`.
