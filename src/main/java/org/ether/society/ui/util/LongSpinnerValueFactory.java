/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.util;

import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

/**
 * Custom SpinnerValueFactory for Long values to avoid ClassCastException and handle large
 * demographic quantities up to billions of agents.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class LongSpinnerValueFactory extends SpinnerValueFactory<Long> {
    private final long min;
    private final long max;
    private final long step;

    /**
     * Constructs a LongSpinnerValueFactory.
     *
     * @param min minimum value
     * @param max maximum value
     * @param initialValue initial value
     * @param step increment/decrement step
     */
    public LongSpinnerValueFactory(long min, long max, long initialValue, long step) {
        this.min = min;
        this.max = max;
        this.step = step;
        setConverter(new StringConverter<>() {
            @Override
            public String toString(Long object) {
                return object == null ? "" : String.valueOf(object);
            }

            @Override
            public Long fromString(String string) {
                if (string == null || string.isBlank()) return initialValue;
                try {
                    String clean = string.replaceAll("[^0-9\\-]", "");
                    return Long.parseLong(clean);
                } catch (Exception e) {
                    return initialValue;
                }
            }
        });
        setValue(initialValue);
    }

    @Override
    public void decrement(int steps) {
        long current = getValue() != null ? getValue() : min;
        long newValue = Math.max(min, current - (long) steps * step);
        setValue(newValue);
    }

    @Override
    public void increment(int steps) {
        long current = getValue() != null ? getValue() : min;
        long newValue = Math.min(max, current + (long) steps * step);
        setValue(newValue);
    }

    public long getMin() { return min; }
    public long getMax() { return max; }
    public long getStep() { return step; }
}
