/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 1-Wasserstein (Earth Mover's) Distance & Phylogenetic Counterfactual Tree Engine.
 *
 * <p>Constructs a topological dendrogram of divergent historical scenarios,
 * rigorously measuring the mathematical distance between alternative planetary states:</p>
 *
 * <pre>
 *   W_1(P, Q) = &int; |CDF_P(x) - CDF_Q(x)| dx
 * </pre>
 *
 * <p>Enables formal phylogenetic analysis of counterfactual branching points
 * (e.g. "Rome Never Fell", "Early Industrial Song Dynasty", "No Black Death 1347").</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class WassersteinCounterfactualTree {
    private static final Logger logger = LoggerFactory.getLogger(WassersteinCounterfactualTree.class);

    /**
     * Node representing a branched historical counterfactual scenario.
     */
    public record CounterfactualNode(
            String scenarioId,
            String parentScenarioId,
            double branchingYear,
            double[] spatialDistribution,
            double wassersteinDistanceToParent,
            Map<String, Double> metrics
    ) {}

    private final Map<String, CounterfactualNode> nodes = new LinkedHashMap<>();
    private String rootScenarioId = null;

    /**
     * Computes the 1D 1-Wasserstein (Earth Mover's) distance between two normalized histograms / spatial distributions.
     *
     * @param p first probability vector (sums to 1.0)
     * @param q second probability vector (sums to 1.0)
     * @return 1-Wasserstein distance W_1(p, q)
     */
    public static double computeWasserstein1D(double[] p, double[] q) {
        if (p == null || q == null || p.length != q.length || p.length == 0) {
            throw new IllegalArgumentException("Distributions must be non-null, equal length, and non-empty.");
        }

        int n = p.length;
        double sumP = 0.0;
        double sumQ = 0.0;
        for (int i = 0; i < n; i++) {
            sumP += Math.max(0.0, p[i]);
            sumQ += Math.max(0.0, q[i]);
        }

        if (sumP <= 0 || sumQ <= 0) return 0.0;

        double cdfP = 0.0;
        double cdfQ = 0.0;
        double distance = 0.0;

        for (int i = 0; i < n; i++) {
            cdfP += Math.max(0.0, p[i]) / sumP;
            cdfQ += Math.max(0.0, q[i]) / sumQ;
            distance += Math.abs(cdfP - cdfQ);
        }

        return distance / n;
    }

    /**
     * Registers the root scenario of the counterfactual tree.
     */
    public void registerRoot(String scenarioId, double startYear, double[] initialDistribution, Map<String, Double> initialMetrics) {
        CounterfactualNode root = new CounterfactualNode(
                scenarioId, null, startYear, initialDistribution, 0.0, initialMetrics != null ? initialMetrics : Map.of()
        );
        nodes.put(scenarioId, root);
        this.rootScenarioId = scenarioId;
    }

    /**
     * Branches a new counterfactual scenario off an existing parent scenario.
     */
    public CounterfactualNode branchScenario(String newScenarioId, String parentScenarioId, double branchingYear,
                                            double[] endDistribution, Map<String, Double> endMetrics) {
        CounterfactualNode parent = nodes.get(parentScenarioId);
        if (parent == null) {
            throw new IllegalArgumentException("Parent scenario '" + parentScenarioId + "' not found in tree.");
        }

        double wDist = computeWasserstein1D(parent.spatialDistribution(), endDistribution);
        CounterfactualNode child = new CounterfactualNode(
                newScenarioId, parentScenarioId, branchingYear, endDistribution, wDist, endMetrics != null ? endMetrics : Map.of()
        );
        nodes.put(newScenarioId, child);
        return child;
    }

    /**
     * Builds a pairwise Wasserstein distance matrix across all registered scenarios.
     */
    public double[][] computeDistanceMatrix() {
        List<CounterfactualNode> nodeList = new ArrayList<>(nodes.values());
        int n = nodeList.size();
        double[][] matrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0.0;
                } else {
                    matrix[i][j] = computeWasserstein1D(nodeList.get(i).spatialDistribution(), nodeList.get(j).spatialDistribution());
                }
            }
        }
        return matrix;
    }

    public Map<String, CounterfactualNode> getNodes() { return Collections.unmodifiableMap(nodes); }
    public String getRootScenarioId() { return rootScenarioId; }
    public int size() { return nodes.size(); }
}
