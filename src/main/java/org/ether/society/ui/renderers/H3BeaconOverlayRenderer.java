/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.renderers;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.ether.society.events.ActiveEvent;
import org.ether.society.events.EventSystem;
import org.ether.society.ui.ViewMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Renders dynamic event beacon alerts, ping radar rings, and scenario HUD badges.
 *
 * <p>Extracted from {@code H3MapCanvas} to fulfill SRP and decouple event-driven alert animations
 * and HUD overlays from the main geographic render loop.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class H3BeaconOverlayRenderer {

    /**
     * Hitbox target for clickable event beacons.
     */
    public static class EventBeaconTarget {
        public double x, y, width, height;
        public double lat, lng;
    }

    /**
     * Represents a temporary or historical geographical event beacon alert.
     */
    public static class BeaconItem {
        private final double lat;
        private final double lng;
        private final String title;
        private final String type;
        private final double magnitude;
        private final long createdAtMs;
        private final long durationMs;

        public BeaconItem(double lat, double lng, String title, String type, double magnitude, long createdAtMs, long durationMs) {
            this.lat = lat;
            this.lng = lng;
            this.title = title;
            this.type = type;
            this.magnitude = magnitude;
            this.createdAtMs = createdAtMs;
            this.durationMs = durationMs;
        }

        public double getLatitude() { return lat; }
        public double getLongitude() { return lng; }
        public String getTitle() { return title; }
        public String getType() { return type; }
        public double getMagnitude() { return magnitude; }
        public long getCreatedAtMs() { return createdAtMs; }
        public boolean isExpired(long now) { return (now - createdAtMs) >= durationMs; }
    }

    private final List<EventBeaconTarget> activeBeaconTargets = new ArrayList<>();
    private final List<BeaconItem> manualPings = new CopyOnWriteArrayList<>();
    private AnimationTimer pingAnimationTimer;

    /**
     * Draws top-right scenario name and date badge.
     */
    public void drawCornerOverlays(GraphicsContext gc, boolean showCornerOverlays,
                                  String scenarioName, String currentDateStr,
                                  double canvasWidth, double canvasHeight) {
        if (!showCornerOverlays) return;
        if (canvasHeight < 60 || canvasWidth < 220) return;

        String scen = (scenarioName != null && !scenarioName.isBlank()) ? scenarioName : "Scénario Ether";
        String date = (currentDateStr != null && !currentDateStr.isBlank()) ? currentDateStr : "An --";
        String unifiedText = "🎬 " + scen + "  •  " + date;

        Font font = Font.font("Segoe UI", FontWeight.BOLD, 12);
        gc.setFont(font);

        Text textNode = new Text(unifiedText);
        textNode.setFont(font);
        double textWidth = textNode.getLayoutBounds().getWidth();
        double badgeWidth = Math.max(180, textWidth + 24);
        double badgeHeight = 28;
        double badgeX = canvasWidth - badgeWidth - 16;
        double badgeY = 14;

        gc.setFill(Color.rgb(15, 23, 42, 0.82));
        gc.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 8, 8);
        gc.setStroke(Color.rgb(56, 189, 248, 0.60));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 8, 8);

        gc.setFill(Color.rgb(241, 245, 249));
        gc.fillText(unifiedText, badgeX + 12, badgeY + 19);
    }

    /**
     * Registers a manual geographical ping animation.
     */
    public void pingLocation(double lat, double lng, String title, String type, double magnitude, Runnable repaintCallback) {
        long now = System.currentTimeMillis();
        manualPings.removeIf(p -> p.isExpired(now));
        manualPings.add(new BeaconItem(lat, lng, title != null ? title : "📍 Événement", type != null ? type : "PING", magnitude > 0 ? magnitude : 6.5, now, 8000L));

        if (pingAnimationTimer == null) {
            pingAnimationTimer = new AnimationTimer() {
                @Override
                public void handle(long nowNs) {
                    long currentMs = System.currentTimeMillis();
                    manualPings.removeIf(p -> p.isExpired(currentMs));
                    if (!manualPings.isEmpty()) {
                        if (repaintCallback != null) repaintCallback.run();
                    } else {
                        stop();
                        pingAnimationTimer = null;
                        if (repaintCallback != null) repaintCallback.run();
                    }
                }
            };
            pingAnimationTimer.start();
        } else {
            if (repaintCallback != null) repaintCallback.run();
        }
    }

    /**
     * Draws event beacons and radar rings across 2D flat or 3D globe views.
     */
    public void drawEventBeacons(GraphicsContext gc, EventSystem eventSystem, ViewMode viewMode,
                                double zoomFactor, double centerLat, double centerLng,
                                double minLng, double maxLat, double scale,
                                double offsetX, double offsetY,
                                double canvasWidth, double canvasHeight) {
        List<BeaconItem> allBeacons = new ArrayList<>();
        long now = System.currentTimeMillis();

        if (eventSystem != null) {
            List<ActiveEvent> events = eventSystem.getActiveEvents();
            if (events != null) {
                for (ActiveEvent ev : events) {
                    if (!ev.isExpired()) {
                        allBeacons.add(new BeaconItem(ev.getLatitude(), ev.getLongitude(), ev.getTitle(), ev.getType(), ev.getMagnitude(), ev.getCreatedAtMs(), 8000L));
                    }
                }
            }
        }

        for (BeaconItem ping : manualPings) {
            if (!ping.isExpired(now)) {
                allBeacons.add(ping);
            }
        }

        synchronized (activeBeaconTargets) {
            activeBeaconTargets.clear();
        }

        if (allBeacons.isEmpty()) return;

        for (BeaconItem event : allBeacons) {
            double lat = event.getLatitude();
            double lng = event.getLongitude();
            double screenX, screenY;

            if (viewMode == ViewMode.VIEW_3D) {
                double radius = Math.min(canvasWidth, canvasHeight) * 0.45 * zoomFactor;
                double cx = canvasWidth / 2;
                double cy = canvasHeight / 2;

                double radLat = Math.toRadians(lat);
                double radLng = Math.toRadians(lng - centerLng);

                double cosLat = Math.cos(radLat);
                double x3d = radius * cosLat * Math.sin(radLng);
                double z3d = radius * cosLat * Math.cos(radLng);
                double y3d = -radius * Math.sin(radLat);

                double radTilt = Math.toRadians(centerLat);
                double yRotated = y3d * Math.cos(radTilt) - z3d * Math.sin(radTilt);
                double zRotated = y3d * Math.sin(radTilt) + z3d * Math.cos(radTilt);

                if (zRotated < -radius * 0.1) continue;

                screenX = cx + x3d;
                screenY = cy + yRotated;
            } else {
                screenX = (lng - minLng) * scale + offsetX;
                screenY = (maxLat - lat) * scale + offsetY;
            }

            if (screenX < -50 || screenX > canvasWidth + 50 || screenY < -50 || screenY > canvasHeight + 50) {
                continue;
            }

            double mag = event.getMagnitude() > 0 ? event.getMagnitude() : 5.0;
            double coreRadius = Math.clamp(6.0 + (mag * 3.5), 6.0, 48.0);

            long elapsedMs = event.getCreatedAtMs() > 0 ? now - event.getCreatedAtMs() : 0;
            boolean isBlinking = elapsedMs >= 0 && elapsedMs < 8000;

            Color eventColor = switch (event.getType() != null ? event.getType() : "") {
                case "VOLCANO", "METEOR", "NUCLEAR_WINTER" -> Color.rgb(239, 68, 68);
                case "FLOOD", "ECOLOGICAL" -> Color.rgb(14, 165, 233);
                case "FAMINE" -> Color.rgb(234, 179, 8);
                case "PANDEMIC" -> Color.rgb(168, 85, 247);
                case "EARTHQUAKE" -> Color.rgb(249, 115, 22);
                case "GOD_MODE" -> Color.rgb(236, 72, 153);
                default -> Color.rgb(56, 189, 248);
            };

            gc.setFill(eventColor);
            gc.fillOval(screenX - coreRadius / 2.0, screenY - coreRadius / 2.0, coreRadius, coreRadius);

            if (isBlinking) {
                double cycle = (elapsedMs % 1500) / 1500.0;
                double pulseRadius = coreRadius + cycle * (coreRadius * 1.6);
                double alpha = Math.max(0.0, 1.0 - cycle);

                gc.setStroke(Color.color(eventColor.getRed(), eventColor.getGreen(), eventColor.getBlue(), alpha));
                gc.setLineWidth(2.5);
                gc.strokeOval(screenX - pulseRadius / 2.0, screenY - pulseRadius / 2.0, pulseRadius, pulseRadius);
            }

            Font titleFont = Font.font("Consolas", FontWeight.BOLD, 10);
            Text textNode = new Text(event.getTitle());
            textNode.setFont(titleFont);
            double textW = textNode.getLayoutBounds().getWidth();
            double titleWidth = Math.max(80.0, textW + 18.0);

            gc.setFill(Color.rgb(15, 23, 42, 0.90));
            gc.fillRoundRect(screenX + 8, screenY - 18, titleWidth, 20, 4, 4);
            gc.setStroke(eventColor);
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(screenX + 8, screenY - 18, titleWidth, 20, 4, 4);

            gc.setFill(Color.WHITE);
            gc.setFont(titleFont);
            gc.fillText(event.getTitle(), screenX + 14, screenY - 4);

            EventBeaconTarget target = new EventBeaconTarget();
            target.x = screenX - 10;
            target.y = screenY - 20;
            target.width = titleWidth + 25;
            target.height = 32;
            target.lat = lat;
            target.lng = lng;

            synchronized (activeBeaconTargets) {
                activeBeaconTargets.add(target);
            }
        }
    }

    /**
     * Hit-tests screen coordinates against active event beacon badges.
     */
    public EventBeaconTarget findHitBeacon(double mouseX, double mouseY) {
        synchronized (activeBeaconTargets) {
            for (EventBeaconTarget target : activeBeaconTargets) {
                if (mouseX >= target.x && mouseX <= target.x + target.width &&
                    mouseY >= target.y && mouseY <= target.y + target.height) {
                    return target;
                }
            }
        }
        return null;
    }

    /**
     * Stops background animation timers.
     */
    public void dispose() {
        if (pingAnimationTimer != null) {
            pingAnimationTimer.stop();
            pingAnimationTimer = null;
        }
        manualPings.clear();
        synchronized (activeBeaconTargets) {
            activeBeaconTargets.clear();
        }
    }
}
