/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CompensatedSumTest {

    @Test
    public void testCompensatedSummationAccuracy() {
        CompensatedSum accumulator = new CompensatedSum();

        // Adding 1.0e10 then 10,000 times 1.0e-7 then subtracting 1.0e10
        accumulator.add(1.0e10);
        for (int i = 0; i < 10000; i++) {
            accumulator.add(1.0e-7);
        }
        accumulator.add(-1.0e10);

        // Standard IEEE 754 summation with doubles completely loses 1.0e-7 due to truncation
        // Kahan-Neumaier preserves the precision
        assertEquals(0.001, accumulator.get(), 1e-10,
                "Compensated summation must prevent catastrophic precision loss");
    }

    @Test
    public void testStaticArraySum() {
        double[] values = {1.0, 2.0, 3.0, 4.0, 5.0};
        assertEquals(15.0, CompensatedSum.sum(values), 1e-12);
    }
}