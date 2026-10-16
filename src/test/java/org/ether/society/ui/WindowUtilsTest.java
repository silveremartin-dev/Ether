/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h1>WindowUtils Test Suite</h1>
 * <p>
 * Epistemic verification and numerical boundary testing for {@link WindowUtils}.
 * Ensures that optimal tick unit computations strictly prevent JavaFX internal tick overflow
 * (\(N_{\text{ticks}} < 2{,}000\)) across deep time scales (\(-100{,}000\) BP to \(2026\) AD).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class WindowUtilsTest {

    @Test
    @DisplayName("computeNiceTickUnit: Handles deep paleolithic range without tick overflow")
    void testComputeNiceTickUnitPaleolithic() {
        // Deep Paleolithic timeline: -100,000 to 2026 -> span = 102,026
        double range = 102026.0;
        double tickUnit = WindowUtils.computeNiceTickUnit(range);

        assertThat(tickUnit).isGreaterThanOrEqualTo(10000.0);
        double tickCount = range / tickUnit;
        assertThat(tickCount).isLessThanOrEqualTo(15.0);
        assertThat(tickCount).isLessThan(2000.0); // Strict JavaFX NumberAxis safety threshold
    }

    @Test
    @DisplayName("computeNiceTickUnit: Validates typical historical and modern eras")
    void testComputeNiceTickUnitHistoricalSpans() {
        // Millennium span: 2,000 years (e.g. 0 to 2000 AD)
        double span2k = 2000.0;
        double tick2k = WindowUtils.computeNiceTickUnit(span2k);
        assertThat(tick2k).isEqualTo(200.0);
        assertThat(span2k / tick2k).isEqualTo(10.0);

        // Century span: 100 years
        double span100 = 100.0;
        double tick100 = WindowUtils.computeNiceTickUnit(span100);
        assertThat(tick100).isEqualTo(10.0);
        assertThat(span100 / tick100).isEqualTo(10.0);

        // Decade span: 10 years
        double span10 = 10.0;
        double tick10 = WindowUtils.computeNiceTickUnit(span10);
        assertThat(tick10).isEqualTo(1.0);
        assertThat(span10 / tick10).isEqualTo(10.0);
    }

    @Test
    @DisplayName("computeNiceTickUnit: Handles degenerate, zero, and negative ranges safely")
    void testComputeNiceTickUnitDegenerateCases() {
        assertThat(WindowUtils.computeNiceTickUnit(0.0)).isEqualTo(10.0);
        assertThat(WindowUtils.computeNiceTickUnit(-500.0)).isEqualTo(10.0);
        assertThat(WindowUtils.computeNiceTickUnit(Double.NaN)).isEqualTo(10.0);
        assertThat(WindowUtils.computeNiceTickUnit(Double.POSITIVE_INFINITY)).isEqualTo(10.0);
    }
}
