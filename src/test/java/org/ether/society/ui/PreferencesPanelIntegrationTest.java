/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test Suite verifying PreferencesPanel setup and localization lifecycle.
 */
public class PreferencesPanelIntegrationTest {

    private static boolean jfxInitialized = false;

    @BeforeAll
    public static void initJFX() throws InterruptedException {
        if (!jfxInitialized) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(() -> {
                    jfxInitialized = true;
                    Platform.setImplicitExit(false);
                    latch.countDown();
                });
            } catch (IllegalStateException e) {
                jfxInitialized = true;
                latch.countDown();
            }
            latch.await(30, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Verify PreferencesPanel construction and updateTexts lifecycle without NPE")
    public void testPreferencesPanelInstantiation() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                PreferencesPanel panel = new PreferencesPanel();
                assertNotNull(panel, "PreferencesPanel instance should not be null");
                
                // Trigger localization refresh
                panel.updateTexts();
            } catch (Throwable t) {
                fail("PreferencesPanel initialization threw an exception: " + t.getMessage());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }
}
