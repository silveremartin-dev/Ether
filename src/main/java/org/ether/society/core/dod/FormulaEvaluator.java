/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-Performance Formula Evaluator & Expression Parser.
 * Evaluates mathematical expressions over raw variables and statistical aggregations
 * (SUM, AVG, MEAN, MEDIAN, VAR, VARIANCE, STDDEV, STDEV, MIN, MAX, GINI, COUNT).
 *
 * Examples of valid expressions:
 *   - "SUM(wealth) / COUNT(population)"
 *   - "STDDEV(food)"
 *   - "GINI(wealth)"
 *   - "AVG(temperature) * 1.8 + 32"
 *   - "SQRT(VAR(age))"
 *
 * @author Silvere Martin-Michiellot
 */
public class FormulaEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(FormulaEvaluator.class);

    /**
     * Interface for resolving raw variable arrays (e.g. "wealth" -> float[]{10.5, 42.0, ...})
     */
    @FunctionalInterface
    public interface VariableResolver {
        float[] resolveArray(String varName);
    }

    /**
     * Interface for resolving single scalar values (e.g. "population" -> 1000.0)
     */
    @FunctionalInterface
    public interface ScalarResolver {
        double resolveScalar(String varName);
    }

    private final StatisticsKernel statisticsKernel = new StatisticsKernel();
    private final Map<String, List<Token>> astCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static class Token {
        final boolean isFunction;
        final String fnName;
        final String argVar;
        final String literal;

        Token(boolean isFunction, String fnName, String argVar, String literal) {
            this.isFunction = isFunction;
            this.fnName = fnName;
            this.argVar = argVar;
            this.literal = literal;
        }
    }

    /**
     * Evaluates a formula string given array and scalar resolvers with compiled token caching.
     */
    public double evaluate(String expression, VariableResolver arrayResolver, ScalarResolver scalarResolver) {
        if (expression == null || expression.isBlank()) return 0.0;
        try {
            List<Token> tokens = astCache.computeIfAbsent(expression, this::compileExpression);
            StringBuilder sb = new StringBuilder();
            for (Token t : tokens) {
                if (t.isFunction) {
                    double val = computeStatFunction(t.fnName, t.argVar, arrayResolver);
                    sb.append(String.format(Locale.US, "%.6f", val));
                } else {
                    sb.append(t.literal);
                }
            }
            return parseAndEvaluateMath(sb.toString(), scalarResolver);
        } catch (Exception e) {
            logger.warn("Failed to evaluate expression '{}': {}", expression, e.getMessage());
            return 0.0;
        }
    }

    private List<Token> compileExpression(String expr) {
        List<Token> tokens = new ArrayList<>();
        int len = expr.length();
        int i = 0;
        StringBuilder literalSb = new StringBuilder();

        while (i < len) {
            char c = expr.charAt(i);

            if (Character.isLetter(c)) {
                int start = i;
                while (i < len && (Character.isLetterOrDigit(expr.charAt(i)) || expr.charAt(i) == '_')) {
                    i++;
                }
                String token = expr.substring(start, i);

                int searchParen = i;
                while (searchParen < len && Character.isWhitespace(expr.charAt(searchParen))) {
                    searchParen++;
                }

                if (searchParen < len && expr.charAt(searchParen) == '(' && isStatFunction(token)) {
                    if (literalSb.length() > 0) {
                        tokens.add(new Token(false, null, null, literalSb.toString()));
                        literalSb.setLength(0);
                    }

                    int argStart = searchParen + 1;
                    int parenCount = 1;
                    int argEnd = argStart;

                    while (argEnd < len && parenCount > 0) {
                        if (expr.charAt(argEnd) == '(') parenCount++;
                        else if (expr.charAt(argEnd) == ')') parenCount--;
                        if (parenCount > 0) argEnd++;
                    }

                    String argVar = expr.substring(argStart, argEnd).trim();
                    i = argEnd + 1;

                    tokens.add(new Token(true, token, argVar, null));
                } else {
                    literalSb.append(token);
                }
            } else {
                literalSb.append(c);
                i++;
            }
        }
        if (literalSb.length() > 0) {
            tokens.add(new Token(false, null, null, literalSb.toString()));
        }
        return tokens;
    }

    private String sanitize(String expr) {
        return expr.trim();
    }

    /**
     * Finds and replaces stat function calls like SUM(var), AVG(var), STDDEV(var), GINI(var)
     * with their calculated scalar numerical values.
     */
    private String replaceStatFunctions(String expr, VariableResolver arrayResolver) {
        StringBuilder sb = new StringBuilder();
        int len = expr.length();
        int i = 0;

        while (i < len) {
            char c = expr.charAt(i);

            // Check if identifier starts here
            if (Character.isLetter(c)) {
                int start = i;
                while (i < len && (Character.isLetterOrDigit(expr.charAt(i)) || expr.charAt(i) == '_')) {
                    i++;
                }
                String token = expr.substring(start, i);

                // Skip trailing spaces
                int searchParen = i;
                while (searchParen < len && Character.isWhitespace(expr.charAt(searchParen))) {
                    searchParen++;
                }

                if (searchParen < len && expr.charAt(searchParen) == '(' && isStatFunction(token)) {
                    // It's a stat function call e.g. SUM(wealth)
                    int argStart = searchParen + 1;
                    int parenCount = 1;
                    int argEnd = argStart;

                    while (argEnd < len && parenCount > 0) {
                        if (expr.charAt(argEnd) == '(') parenCount++;
                        else if (expr.charAt(argEnd) == ')') parenCount--;
                        if (parenCount > 0) argEnd++;
                    }

                    String argVar = expr.substring(argStart, argEnd).trim();
                    i = argEnd + 1; // Move past closing parenthesis

                    double result = computeStatFunction(token, argVar, arrayResolver);
                    sb.append(String.format(Locale.US, "%.6f", result));
                } else {
                    sb.append(token);
                }
            } else {
                sb.append(c);
                i++;
            }
        }

        return sb.toString();
    }

    private boolean isStatFunction(String name) {
        String u = name.toUpperCase(Locale.ROOT);
        return u.equals("SUM") || u.equals("AVG") || u.equals("MEAN") || u.equals("MEDIAN")
                || u.equals("VAR") || u.equals("VARIANCE") || u.equals("STDDEV") || u.equals("STDEV")
                || u.equals("MIN") || u.equals("MAX") || u.equals("GINI") || u.equals("COUNT") || u.equals("RANGE")
                || u.equals("HERFINDAHL") || u.equals("HHI") || u.equals("THEIL") || u.equals("PALMA")
                || u.equals("SKEWNESS") || u.equals("KURTOSIS");
    }

    private double computeStatFunction(String funcName, String varName, VariableResolver arrayResolver) {
        if (arrayResolver == null) return 0.0;
        float[] data = arrayResolver.resolveArray(varName);
        if (data == null || data.length == 0) return 0.0;

        String fn = funcName.toUpperCase(Locale.ROOT);
        switch (fn) {
            case "SUM": {
                double s = 0;
                for (float v : data) s += v;
                return s;
            }
            case "AVG":
            case "MEAN": {
                double s = 0;
                for (float v : data) s += v;
                return s / data.length;
            }
            case "COUNT":
                return data.length;
            case "MIN": {
                float min = Float.MAX_VALUE;
                for (float v : data) if (v < min) min = v;
                return min == Float.MAX_VALUE ? 0.0 : min;
            }
            case "MAX": {
                float max = -Float.MAX_VALUE;
                for (float v : data) if (v > max) max = v;
                return max == -Float.MAX_VALUE ? 0.0 : max;
            }
            case "RANGE": {
                float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
                for (float v : data) {
                    if (v < min) min = v;
                    if (v > max) max = v;
                }
                return min == Float.MAX_VALUE ? 0.0 : (max - min);
            }
            case "MEDIAN": {
                float[] sorted = data.clone();
                Arrays.sort(sorted);
                int n = sorted.length;
                if (n % 2 == 1) return sorted[n / 2];
                return (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
            }
            case "VAR":
            case "VARIANCE": {
                float[] aggs = statisticsKernel.calculateAggregates(data); // avg, min, max, stdDev
                return (double) aggs[3] * aggs[3];
            }
            case "STDDEV":
            case "STDEV": {
                float[] aggs = statisticsKernel.calculateAggregates(data);
                return aggs[3];
            }
            case "GINI": {
                return statisticsKernel.calculateGini(data);
            }
            case "HERFINDAHL":
            case "HHI": {
                double sum = 0;
                for (float v : data) sum += Math.max(0, v);
                if (sum <= 0) return 0.0;
                double hhi = 0;
                for (float v : data) {
                    double s = Math.max(0, v) / sum;
                    hhi += s * s;
                }
                return hhi;
            }
            case "THEIL": {
                double mean = 0;
                for (float v : data) mean += Math.max(0, v);
                mean /= data.length;
                if (mean <= 0) return 0.0;
                double sumTheil = 0;
                for (float v : data) {
                    double x = Math.max(1e-9, v);
                    sumTheil += (x / mean) * Math.log(x / mean);
                }
                return sumTheil / data.length;
            }
            case "PALMA": {
                float[] sorted = data.clone();
                Arrays.sort(sorted);
                int n = sorted.length;
                if (n < 10) return 1.0;
                int top10Idx = (int) (n * 0.90);
                int bot40Idx = (int) (n * 0.40);
                double sumTop10 = 0, sumBot40 = 0;
                for (int i = top10Idx; i < n; i++) sumTop10 += sorted[i];
                for (int i = 0; i < bot40Idx; i++) sumBot40 += sorted[i];
                return sumBot40 > 0 ? sumTop10 / sumBot40 : 0.0;
            }
            case "SKEWNESS": {
                double mean = 0;
                for (float v : data) mean += v;
                mean /= data.length;
                double m3 = 0, m2 = 0;
                for (float v : data) {
                    double diff = v - mean;
                    m2 += diff * diff;
                    m3 += diff * diff * diff;
                }
                m2 /= data.length;
                m3 /= data.length;
                double std = Math.sqrt(m2);
                return std > 0 ? m3 / (std * std * std) : 0.0;
            }
            case "KURTOSIS": {
                double mean = 0;
                for (float v : data) mean += v;
                mean /= data.length;
                double m4 = 0, m2 = 0;
                for (float v : data) {
                    double diff = v - mean;
                    m2 += diff * diff;
                    m4 += diff * diff * diff * diff;
                }
                m2 /= data.length;
                m4 /= data.length;
                return m2 > 0 ? (m4 / (m2 * m2)) - 3.0 : 0.0;
            }
            default:
                return 0.0;
        }
    }

    /**
     * Evaluates a scalar mathematical expression containing arithmetic operations and scalar variables.
     */
    private double parseAndEvaluateMath(String expr, ScalarResolver scalarResolver) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expr.length()) throw new RuntimeException("Unexpected char: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double denom = parseFactor();
                        x = denom != 0 ? x / denom : 0.0;
                    } else if (eat('%')) {
                        double denom = parseFactor();
                        x = denom != 0 ? x % denom : 0.0;
                    } else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expr.substring(startPos, this.pos));
                } else if (Character.isLetter(ch) || ch == '_') {
                    while (Character.isLetterOrDigit(ch) || ch == '_') nextChar();
                    String name = expr.substring(startPos, this.pos);

                    if (eat('(')) {
                        x = parseExpression();
                        eat(')');
                        String fn = name.toLowerCase(Locale.ROOT);
                        x = switch (fn) {
                            case "sqrt" -> Math.sqrt(x);
                            case "abs" -> Math.abs(x);
                            case "log" -> Math.log(x);
                            case "exp" -> Math.exp(x);
                            case "round" -> Math.round(x);
                            case "floor" -> Math.floor(x);
                            case "ceil" -> Math.ceil(x);
                            default -> x;
                        };
                    } else {
                        // Scalar variable lookup
                        if (scalarResolver != null) {
                            x = scalarResolver.resolveScalar(name);
                        } else {
                            x = 0.0;
                        }
                    }
                } else {
                    throw new RuntimeException("Unexpected token: " + (char) ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor());

                return x;
            }
        }.parse();
    }
}
