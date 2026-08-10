/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core.profiling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-precision micro-profiler for Ether simulation ticks.
 * Measures tick durations, phase breakdowns (in nanoseconds/microseconds),
 * and calculates min/max/avg/P95 latency stats and TPS.
 */
public class SimulationProfiler {

    private final AtomicLong totalTicks = new AtomicLong(0);
    private final AtomicLong totalSimulationNanos = new AtomicLong(0);
    private final AtomicLong totalRenderNanos = new AtomicLong(0);

    private long minTickNanos = Long.MAX_VALUE;
    private long maxTickNanos = 0;
    private long lastTickNanos = 0;

    // Rolling tick duration buffer for P95 calculation (1000 samples)
    private static final int ROLLING_BUFFER_SIZE = 1000;
    private final long[] rollingTickBuffer = new long[ROLLING_BUFFER_SIZE];
    private int bufferIndex = 0;
    private boolean bufferFilled = false;

    // Phase timings: Phase Name -> Total Accumulation Nanos
    private final Map<String, AtomicLong> phaseAccumulatedNanos = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> phaseInvocationCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> phaseLastDurationNanos = new ConcurrentHashMap<>();

    // Thread-local phase start times
    private final ThreadLocal<Map<String, Long>> phaseStartTimes = ThreadLocal.withInitial(LinkedHashMap::new);

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void reset() {
        totalTicks.set(0);
        totalSimulationNanos.set(0);
        totalRenderNanos.set(0);
        minTickNanos = Long.MAX_VALUE;
        maxTickNanos = 0;
        lastTickNanos = 0;
        bufferIndex = 0;
        bufferFilled = false;
        phaseAccumulatedNanos.clear();
        phaseInvocationCounts.clear();
        phaseLastDurationNanos.clear();
    }

    public void beginPhase(String phaseName) {
        if (!enabled) return;
        phaseStartTimes.get().put(phaseName, System.nanoTime());
    }

    public void endPhase(String phaseName) {
        if (!enabled) return;
        Long start = phaseStartTimes.get().remove(phaseName);
        if (start != null) {
            long duration = System.nanoTime() - start;
            recordPhaseDuration(phaseName, duration);
        }
    }

    public void recordPhaseDuration(String phaseName, long durationNanos) {
        if (!enabled) return;
        phaseAccumulatedNanos.computeIfAbsent(phaseName, k -> new AtomicLong(0)).addAndGet(durationNanos);
        phaseInvocationCounts.computeIfAbsent(phaseName, k -> new AtomicLong(0)).incrementAndGet();
        phaseLastDurationNanos.put(phaseName, durationNanos);
    }

    public void recordTick(long tickDurationNanos, long renderDurationNanos) {
        if (!enabled) return;
        long ticks = totalTicks.incrementAndGet();
        totalSimulationNanos.addAndGet(tickDurationNanos);
        totalRenderNanos.addAndGet(renderDurationNanos);

        lastTickNanos = tickDurationNanos;
        if (tickDurationNanos < minTickNanos) minTickNanos = tickDurationNanos;
        if (tickDurationNanos > maxTickNanos) maxTickNanos = tickDurationNanos;

        synchronized (rollingTickBuffer) {
            rollingTickBuffer[bufferIndex] = tickDurationNanos;
            bufferIndex = (bufferIndex + 1) % ROLLING_BUFFER_SIZE;
            if (bufferIndex == 0) bufferFilled = true;
        }
    }

    public long getTotalTicks() {
        return totalTicks.get();
    }

    public double getAverageTickTimeMs() {
        long ticks = totalTicks.get();
        if (ticks == 0) return 0.0;
        return (totalSimulationNanos.get() / (double) ticks) / 1_000_000.0;
    }

    public double getMinTickTimeMs() {
        return minTickNanos == Long.MAX_VALUE ? 0.0 : minTickNanos / 1_000_000.0;
    }

    public double getMaxTickTimeMs() {
        return maxTickNanos / 1_000_000.0;
    }

    public double getLastTickTimeMs() {
        return lastTickNanos / 1_000_000.0;
    }

    public double getP95TickTimeMs() {
        int count = bufferFilled ? ROLLING_BUFFER_SIZE : bufferIndex;
        if (count == 0) return 0.0;

        long[] copy = new long[count];
        synchronized (rollingTickBuffer) {
            System.arraycopy(rollingTickBuffer, 0, copy, 0, count);
        }
        java.util.Arrays.sort(copy);
        int p95Index = (int) Math.ceil(0.95 * count) - 1;
        p95Index = Math.max(0, Math.min(count - 1, p95Index));
        return copy[p95Index] / 1_000_000.0;
    }

    public double getEffectiveTPS() {
        double avgMs = getAverageTickTimeMs();
        return avgMs > 0 ? 1000.0 / avgMs : 0.0;
    }

    public Map<String, Double> getPhaseAverageDurationsMs() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : phaseAccumulatedNanos.entrySet()) {
            String phase = entry.getKey();
            long accum = entry.getValue().get();
            long count = phaseInvocationCounts.getOrDefault(phase, new AtomicLong(1)).get();
            double avgMs = (accum / (double) Math.max(1, count)) / 1_000_000.0;
            result.put(phase, avgMs);
        }
        return result;
    }

    public Map<String, Double> getPhasePercentages() {
        double totalPhaseNanos = 0;
        for (AtomicLong accum : phaseAccumulatedNanos.values()) {
            totalPhaseNanos += accum.get();
        }
        if (totalPhaseNanos <= 0) return Collections.emptyMap();

        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : phaseAccumulatedNanos.entrySet()) {
            double pct = (entry.getValue().get() / totalPhaseNanos) * 100.0;
            result.put(entry.getKey(), pct);
        }
        return result;
    }

    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================\n");
        sb.append("                 ETHER SIMULATION PROFILER REPORT                       \n");
        sb.append("========================================================================\n");
        sb.append(String.format("Total Ticks Executed : %,d\n", totalTicks.get()));
        sb.append(String.format("Total Engine Time    : %.2f s\n", totalSimulationNanos.get() / 1_000_000_000.0));
        sb.append(String.format("Total Render Time    : %.2f s\n", totalRenderNanos.get() / 1_000_000_000.0));
        sb.append(String.format("Effective TPS        : %.2f ticks/sec\n", getEffectiveTPS()));
        sb.append(String.format("Tick Duration Stats  : Avg: %.3f ms | Min: %.3f ms | Max: %.3f ms | P95: %.3f ms\n",
                getAverageTickTimeMs(), getMinTickTimeMs(), getMaxTickTimeMs(), getP95TickTimeMs()));
        sb.append("------------------------------------------------------------------------\n");
        sb.append(String.format("%-32s | %-12s | %-12s | %-8s\n", "Phase / System", "Total (ms)", "Avg (ms)", "Share (%)"));
        sb.append("------------------------------------------------------------------------\n");

        double totalNanos = totalSimulationNanos.get();
        for (Map.Entry<String, AtomicLong> entry : phaseAccumulatedNanos.entrySet()) {
            String phase = entry.getKey();
            long accumNanos = entry.getValue().get();
            long count = phaseInvocationCounts.getOrDefault(phase, new AtomicLong(1)).get();
            double totalMs = accumNanos / 1_000_000.0;
            double avgMs = totalMs / Math.max(1, count);
            double pct = totalNanos > 0 ? (accumNanos / totalNanos) * 100.0 : 0.0;

            sb.append(String.format("%-32s | %12.2f | %12.4f | %7.2f%%\n", phase, totalMs, avgMs, pct));
        }
        sb.append("========================================================================\n");
        return sb.toString();
    }
}
