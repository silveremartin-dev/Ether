/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core.integration;

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class MultiScaleSymplecticIntegratorTest {

    @Test
    public void testThreeTierTimeDecoupling() {
        MultiScaleSymplecticIntegrator integrator = new MultiScaleSymplecticIntegrator(30, 12, true);
        WorldBuffer buffer = new WorldBuffer(10);

        AtomicInteger dailyCount = new AtomicInteger(0);
        AtomicInteger monthlyCount = new AtomicInteger(0);
        AtomicInteger annualCount = new AtomicInteger(0);
        AtomicReference<Float> lastMonthlyDt = new AtomicReference<>(0f);
        AtomicReference<Float> lastAnnualDt = new AtomicReference<>(0f);

        // Run 365 daily steps (1 year + 5 days)
        for (int day = 1; day <= 365; day++) {
            int currentMonth = ((day - 1) / 30) + 1;
            int currentYear = 2026;

            var result = integrator.step(
                    buffer,
                    1, // 1 stepDay per tick
                    dt -> dailyCount.incrementAndGet(),
                    (dt, m) -> {
                        monthlyCount.incrementAndGet();
                        lastMonthlyDt.set(dt);
                    },
                    (dt, y) -> {
                        annualCount.incrementAndGet();
                        lastAnnualDt.set(dt);
                    },
                    currentMonth,
                    currentYear
            );

            if (day % 30 == 0) {
                assertTrue(result.isMonthlyExecuted(), "Monthly should trigger every 30 days at day " + day);
            } else {
                assertFalse(result.isMonthlyExecuted(), "Monthly must NOT trigger on day " + day);
            }

            if (day == 360) { // 12 * 30 days
                assertTrue(result.isAnnualExecuted(), "Annual should trigger at month 12 (day 360)");
            } else {
                assertFalse(result.isAnnualExecuted(), "Annual must NOT trigger on day " + day);
            }
        }

        // Daily executed 365 times
        assertEquals(365, dailyCount.get(), "Daily equations must execute on every single tick");
        // Monthly executed 12 times (360 days / 30)
        assertEquals(12, monthlyCount.get(), "Monthly equations must execute exactly 12 times in a 360-day year");
        // Annual executed exactly 1 time
        assertEquals(1, annualCount.get(), "Annual equations must execute exactly once per year");

        assertEquals(30 * 86400f, lastMonthlyDt.get(), 1e-3, "Accumulated monthly dt must equal 30 days");
        assertEquals(360 * 86400f, lastAnnualDt.get(), 1e-3, "Accumulated annual dt must equal 360 days");
    }

    @Test
    public void testStrictDeterminismDoesNotCollapseTimescales() {
        // Strict determinism true
        MultiScaleSymplecticIntegrator integrator = new MultiScaleSymplecticIntegrator(30, 12, true);
        WorldBuffer buffer = new WorldBuffer(10);

        AtomicInteger monthlyCount = new AtomicInteger(0);

        for (int day = 1; day <= 10; day++) {
            integrator.step(
                    buffer,
                    1,
                    dt -> {},
                    (dt, m) -> monthlyCount.incrementAndGet(),
                    (dt, y) -> {},
                    1,
                    2026
            );
        }

        // Even with strictDeterminism = true, monthly equations are NOT executed daily
        assertEquals(0, monthlyCount.get(), "Monthly equations must NEVER execute daily even under strictDeterminism");
    }
}
