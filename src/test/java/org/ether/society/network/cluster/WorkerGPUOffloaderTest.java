/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.cluster;

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WorkerGPUOffloaderTest {

    @Test
    public void testWorkerGPUOffloaderExecution() {
        WorkerGPUOffloader offloader = new WorkerGPUOffloader();

        WorldBuffer buffer = new WorldBuffer(20);
        buffer.getFluxPressure()[3] = 1.5f;

        offloader.computeChunk(buffer, 86400f);

        assertEquals(1, offloader.getTotalTicksComputed());
        assertTrue(offloader.getAverageComputeTimeMs() >= 0.0);
    }
}
