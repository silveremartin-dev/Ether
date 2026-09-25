/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WassersteinCounterfactualTreeTest {

    @Test
    @DisplayName("Verify 1-Wasserstein distance properties and counterfactual tree dendrogram construction")
    void testWassersteinTree() {
        WassersteinCounterfactualTree tree = new WassersteinCounterfactualTree();

        double[] rootDist = new double[]{0.4, 0.3, 0.2, 0.1};
        tree.registerRoot("ROOT_HISTORICAL", 0.0, rootDist, Map.of("GDP", 1000.0));

        assertEquals(1, tree.size());
        assertEquals("ROOT_HISTORICAL", tree.getRootScenarioId());

        // Identical distribution must have distance 0.0
        double distZero = WassersteinCounterfactualTree.computeWasserstein1D(rootDist, rootDist);
        assertEquals(0.0, distZero, 1e-6);

        // Shifted distribution (e.g. urbanization to higher bins)
        double[] branch1Dist = new double[]{0.1, 0.2, 0.3, 0.4};
        var child1 = tree.branchScenario("SONG_INDUSTRIAL_ACCELERATION", "ROOT_HISTORICAL", 1000.0,
                branch1Dist, Map.of("GDP", 5000.0));

        assertNotNull(child1);
        assertTrue(child1.wassersteinDistanceToParent() > 0.0, "Shifted distribution must have positive Wasserstein distance.");
        assertEquals(2, tree.size());

        // Branch another counterfactual off ROOT
        double[] branch2Dist = new double[]{0.5, 0.3, 0.1, 0.1};
        tree.branchScenario("ROME_NEVER_FELL", "ROOT_HISTORICAL", 476.0, branch2Dist, Map.of("GDP", 3000.0));

        assertEquals(3, tree.size());

        double[][] distMatrix = tree.computeDistanceMatrix();
        assertEquals(3, distMatrix.length);
        assertEquals(0.0, distMatrix[0][0], 1e-6);
        assertEquals(distMatrix[0][1], distMatrix[1][0], 1e-6, "Distance matrix must be symmetric.");
    }
}
