package org.ether.society.core.dod;

/**
 * Kernel gérant la dérive culturelle et la diffusion memétique.
 * Utilise les tenseurs culturels (4D) de l'AgentBuffer.
 */
public class CultureKernel {

    /**
     * Simule l'évolution culturelle.
     */
    public void tick(WorldBuffer world, AgentBuffer agents, float dt) {
        drift(agents, dt);
        diffuse(world, agents, dt);
    }

    /**
     * Dérive locale : chaque cohorte mute légèrement ses valeurs culturelles.
     */
    private void drift(AgentBuffer agents, float dt) {
        float[][] culture = agents.getCulture();
        int[] hexIds = agents.getHexIds();
        
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            
            for (int d = 0; d < 4; d++) {
                // Bruit blanc simulant l'innovation/dérive locale
                culture[d][i] += (float)(Math.random() - 0.5) * 0.01f * dt;
                // Clamp entre 0 et 1
                culture[d][i] = Math.max(0.0f, Math.min(1.0f, culture[d][i]));
            }
        }
    }

    /**
     * Diffusion : les cohortes voisines tendent à harmoniser leurs cultures.
     * Simule les mariages, le commerce et les échanges d'idées.
     */
    private void diffuse(WorldBuffer world, AgentBuffer agents, float dt) {
        float[][] culture = agents.getCulture();
        int[] hexIds = agents.getHexIds();
        int[][] neighbors = world.getNeighborIndexes();
        
        // Structure temporaire pour stocker les changements
        float[][] delta = new float[4][agents.getCapacity()];
        
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            
            int myHex = hexIds[i];
            
            // Chercher les agents dans les hexagones voisins
            for (int j = 0; j < 6; j++) {
                int nHex = neighbors[myHex][j];
                if (nHex == -1) continue;
                
                // Trouver les agents dans cet hexagone voisin
                // Note : Pour une performance GPU réelle, on utiliserait une grille spatiale
                // Ici, on fait une recherche simplifiée pour le prototype DOD Java
                for (int targetIdx = 0; targetIdx < agents.getCapacity(); targetIdx++) {
                    if (agents.getHexIds()[targetIdx] == nHex) {
                        // Échange culturel
                        for (int d = 0; d < 4; d++) {
                            float diff = culture[d][targetIdx] - culture[d][i];
                            delta[d][i] += diff * 0.05f * dt; // Taux d'acculturation
                        }
                    }
                }
            }
        }
        
        // Appliquer les deltas
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            for (int d = 0; d < 4; d++) {
                culture[d][i] += delta[d][i];
            }
        }
    }
}
