# Guide d'Intégration & Activation du Support GPU (Ether Engine)

Ce guide explique pas à pas comment installer les composants requis et activer l'accélération matériel GPU (NVIDIA, AMD ou Intel) pour le moteur de simulation **Ether**.

---

## 1. Vue d'Ensemble & Architecture du Support GPU

Ether intègre un système d'accélération hybride intelligent :

1. **Calcul GPU Natif (`GPUManager` / OpenCL / TornadoVM) :**
   Lorsque le matériel GPU et le runtime OpenCL sont présents, Ether compile et exécute à chaud des Compute Shaders (kernels OpenCL/SPIR-V) générés par l'AST du scénario (`GPUFusedKernelGenerator`). Cela permet de simuler jusqu'à **1 000 000 de cellules en temps réel (350+ TPS)**.
2. **Bascule Automatique CPU JIT (Fallback Sécurisé) :**
   Si aucune carte graphique dédiée ou aucun pilote OpenCL n'est disponible (ex: GPU intégré standard ou absence de SDK OpenCL), le moteur intercepte la demande de manière transparente et s'exécute sur le **Compilateur JIT CPU (`ScenarioEngineJITCompiler`)**. La simulation reste 100% fonctionnelle, sans aucun plantage.

---

## 2. Prérequis Systèmes & Pilotes par Constructeur

| Constructeur GPU | Pilote Requis | Module Runtime Requis | Commande de Vérification |
| :--- | :--- | :--- | :--- |
| **NVIDIA** (GeForce / RTX / Quadro) | Pilote Game Ready ou Studio récent (>= 530.xx) | OpenCL + CUDA PTX (Intégré de base) | `nvidia-smi` & `clinfo` |
| **AMD** (Radeon RX / Vega) | AMD Software: Adrenalin Edition | OpenCL Runtime (Intégré au pilote) | `clinfo` |
| **Intel** (Arc / Iris Xe / UHD Graphics) | Pilote Intel Graphics DCH récent | Intel OpenCL Driver / SDK Package | `clinfo` |

---

## 3. Guide d'Installation Étape par Étape

### Étape 1 : Installation et Mise à Jour du Pilote Graphique
1. Téléchargez et installez les derniers pilotes graphiques officiels de votre constructeur (NVIDIA, AMD ou Intel).
2. Assurez-vous que l'option d'installation inclut le composant **OpenCL Runtime**.

### Étape 2 : Vérification du Runtime OpenCL (`clinfo`)
Ouvrez une invite de commande ou une fenêtre PowerShell et exécutez :
```powershell
clinfo
```
- **Résultat attendu :**
  ```text
  Number of platforms: 1
    Platform Name: OpenCL 3.0 / NVIDIA CUDA (ou AMD / Intel)
    Device Name: NVIDIA GeForce RTX 3080 (ou AMD / Intel Arc)
    Device Type: GPU
    Max Compute Units: 68
  ```
- Si la commande `clinfo` n'est pas reconnue, vous pouvez l'installer via Chocolatey/Winget (`winget install Oblomov.clinfo`) ou vérifier la présence des DLL `OpenCL.dll` dans `C:\Windows\System32\`.

### Étape 3 : Installation de TornadoVM (Optionnel pour compilation avancée SPIR-V / PTX)
Si vous souhaitez compiler directement des kernels Java bytecode vers GPU via TornadoVM :
```bash
# Clone du dépôt TornadoVM
git clone https://github.com/beehive-lab/TornadoVM.git
cd TornadoVM

# Installation avec backend OpenCL pour JDK 21
./bin/tornadovm-installer --jdk %JAVA_HOME% --backend opencl
```

### Étape 4 : Activation du Support GPU dans Ether

L'activation du GPU se fait de deux manières :

#### Option A : Via l'Interface Graphique (UI JavaFX)
1. Ouvrez l'application Ether (`mvn javafx:run`).
2. Allez dans le panneau **Préférences / Configuration Simulation**.
3. Cochez la case **"Activer l'Accélération GPU (OpenCL)"**.
4. Cliquez sur **"Diagnostic de Viabilité"** : le système affiche le statut du GPU détecté ou confirme la bascule active sur le JIT CPU.

#### Option B : Dans le Code Java ou Fichier de Configuration (`config.json`)
```java
// Dans votre code d'initialisation ou contrôleur :
GPUManager gpuManager = new GPUManager();
gpuManager.setGpuEnabled(true); // Activer la tentative GPU avec fallback automatique CPU
```

Dans `config/default-config.json` :
```json
{
  "simulation": {
    "gpuAccelerationEnabled": true,
    "fallbackToCpuJitOnFailure": true
  }
}
```

---

## 4. Diagnostic & Résolution de Problèmes (Troubleshooting)

### Q1 : Que se passe-t-il si je coche l'option GPU sur un PC sans carte graphique ?
**Réponse :** Aucun problème. Le `GPUManager` tente le dispatch OpenCL, intercepte l'absence de pilote natif, inscrit une ligne d'information dans les logs (`ℹ️ OpenCL GPU acceleration unavailable. Seamlessly running on JIT Fused CPU Kernel.`), et exécute la simulation sur le CPU JIT fusionné avec un gain de 35x à 45x.

### Q2 : Comment vérifier si mon GPU est réellement sollicité pendant la simulation ?
1. **Consulter les logs SLF4J d'Ether :**
   Cherchez les lignes suivantes au démarrage d'un tick :
   `[INFO] ⚡ TornadoVM / OpenCL API present. GPU Acceleration Candidate.`
2. **Utiliser le Gestionnaire des Tâches (Windows) / `nvidia-smi` (Linux) :**
   Dans l'onglet **Performance > GPU** du Gestionnaire des tâches, observez le graphe **Compute_0** ou **CUDA** lors de l'exécution d'un scénario à 100 000 cellules.

---

## 5. Synthèse des Références Documentaires

- [Audit d'Architecture & Rapport de Performance](GPU_AND_CPU_JIT_AUDIT_REPORT.md)
- [Documentation de l'Ordre des Moteurs & Spécification JIT](ENGINE_EXECUTION_AND_JIT_SPECIFICATION.md)
- [Guide de Configuration OpenCL Intel](OPENCL_SETUP.md)
- [Guide de Configuration TornadoVM](TORNADOVM_SETUP.md)
