/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.jit;

/**
 * Represents a symbolic mathematical expression for procedural state updates.
 * Used for constant folding, AST reduction, and Kernel Fusion.
 */
public class SymbolicExpression {

    public enum OpType {
        IDENTITY,
        CONSTANT,
        AFFINE, // A * x + B
        ADDITIVE_DELTA // x + delta * dt
    }

    private final String variableName;
    private final OpType opType;
    private final double coefficient; // A or delta
    private final double offset;      // B

    public SymbolicExpression(String variableName) {
        this.variableName = variableName;
        this.opType = OpType.IDENTITY;
        this.coefficient = 1.0;
        this.offset = 0.0;
    }

    public SymbolicExpression(String variableName, double coefficient, double offset) {
        this.variableName = variableName;
        this.opType = OpType.AFFINE;
        this.coefficient = coefficient;
        this.offset = offset;
    }

    public String getVariableName() {
        return variableName;
    }

    public OpType getOpType() {
        return opType;
    }

    public double getCoefficient() {
        return coefficient;
    }

    public double getOffset() {
        return offset;
    }

    /**
     * Composes this expression f(x) = A1*x + B1 with g(x) = A2*x + B2
     * Result g(f(x)) = A2*(A1*x + B1) + B2 = (A1*A2)*x + (A2*B1 + B2)
     */
    public SymbolicExpression compose(SymbolicExpression g) {
        if (!this.variableName.equals(g.variableName)) {
            throw new IllegalArgumentException("Cannot compose expressions for different variables: " 
                    + this.variableName + " vs " + g.variableName);
        }
        double newCoeff = this.coefficient * g.coefficient;
        double newOffset = g.coefficient * this.offset + g.offset;
        return new SymbolicExpression(this.variableName, newCoeff, newOffset);
    }

    /**
     * Evaluates the simplified expression for a given input value x.
     */
    public double evaluate(double x) {
        return coefficient * x + offset;
    }

    @Override
    public String toString() {
        if (Math.abs(coefficient - 1.0) < 1e-9 && Math.abs(offset) < 1e-9) {
            return variableName;
        }
        if (Math.abs(offset) < 1e-9) {
            return String.format("%.4f * %s", coefficient, variableName);
        }
        if (Math.abs(coefficient - 1.0) < 1e-9) {
            return String.format("%s + %.4f", variableName, offset);
        }
        return String.format("%.4f * %s + %.4f", coefficient, variableName, offset);
    }
}
