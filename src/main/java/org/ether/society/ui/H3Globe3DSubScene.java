/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 * SINCE: 2.0
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.ether.society.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Hardware-accelerated 3D Globe SubScene utilizing JavaFX 3D MeshView,
 * PhongMaterial shading, depth buffer, and solar lighting.
 */
public class H3Globe3DSubScene {

    private static final Logger logger = LoggerFactory.getLogger(H3Globe3DSubScene.class);

    private final SubScene subScene;
    private final Group rootGroup = new Group();
    private final Group globeGroup = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    private final Sphere oceanSphere;
    private final Sphere atmosphereSphere;
    private final Sphere cloudSphere;
    private final MeshView terrainMeshView = new MeshView();

    private final PointLight sunLight;
    private final AmbientLight ambientLight;

    private double globeRadius = 300.0;
    private double reliefScale = 1.0;
    private double lastMouseX, lastMouseY;
    private AnimationTimer flyTimer;
    private AnimationTimer autoRotateTimer;
    private boolean autoRotating = false;
    private static final double DEFAULT_CAMERA_Z = -850.0;

    public H3Globe3DSubScene(double width, double height) {
        rootGroup.getChildren().add(globeGroup);

        // Setup 3D Camera
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setTranslateZ(DEFAULT_CAMERA_Z);
        rootGroup.getChildren().add(camera);

        // Setup Solar & Ambient Lighting
        sunLight = new PointLight(Color.rgb(255, 248, 220));
        sunLight.setTranslateX(1500.0);
        sunLight.setTranslateY(-500.0);
        sunLight.setTranslateZ(-1200.0);

        ambientLight = new AmbientLight(Color.rgb(45, 55, 75));
        rootGroup.getChildren().addAll(sunLight, ambientLight);

        // Ocean Base Sphere
        oceanSphere = new Sphere(globeRadius);
        PhongMaterial oceanMaterial = new PhongMaterial();
        oceanMaterial.setDiffuseColor(Color.rgb(10, 35, 80));
        oceanMaterial.setSpecularColor(Color.rgb(180, 220, 255));
        oceanMaterial.setSpecularPower(64.0);
        oceanSphere.setMaterial(oceanMaterial);
        globeGroup.getChildren().add(oceanSphere);

        // Terrain Mesh Node
        globeGroup.getChildren().add(terrainMeshView);

        // Cloud Layer Shell
        cloudSphere = new Sphere(globeRadius * 1.018);
        PhongMaterial cloudMaterial = new PhongMaterial();
        cloudMaterial.setDiffuseColor(Color.rgb(255, 255, 255, 0.25));
        cloudMaterial.setSpecularColor(Color.rgb(255, 255, 255, 0.40));
        cloudSphere.setMaterial(cloudMaterial);
        globeGroup.getChildren().add(cloudSphere);

        // Atmosphere Glow Shell
        atmosphereSphere = new Sphere(globeRadius * 1.035);
        PhongMaterial atmosphereMaterial = new PhongMaterial();
        atmosphereMaterial.setDiffuseColor(Color.rgb(56, 189, 248, 0.12));
        atmosphereMaterial.setSpecularColor(Color.rgb(56, 189, 248, 0.25));
        atmosphereSphere.setMaterial(atmosphereMaterial);
        globeGroup.getChildren().add(atmosphereSphere);

        // Globe Transformations
        globeGroup.getTransforms().addAll(rotateX, rotateY);

        // SubScene Initialization
        subScene = new SubScene(rootGroup, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);

        setupInteractionHandlers();
    }

    public SubScene getSubScene() {
        return subScene;
    }

    public void resetCamera() {
        camera.setTranslateZ(DEFAULT_CAMERA_Z);
        rotateX.setAngle(0);
        rotateY.setAngle(0);
    }

    public boolean isAutoRotating() {
        return autoRotating;
    }

    public void setAutoRotating(boolean autoRotating) {
        this.autoRotating = autoRotating;
        if (autoRotating) {
            if (autoRotateTimer == null) {
                autoRotateTimer = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        rotateY.setAngle((rotateY.getAngle() + 0.3) % 360.0);
                    }
                };
            }
            autoRotateTimer.start();
        } else {
            if (autoRotateTimer != null) {
                autoRotateTimer.stop();
            }
        }
    }

    public void setReliefScale(double scale) {
        this.reliefScale = scale;
    }

    public double getReliefScale() {
        return reliefScale;
    }

    private void setupInteractionHandlers() {
        subScene.setOnMousePressed(e -> {
            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();
        });

        subScene.setOnMouseDragged(e -> {
            double deltaX = e.getSceneX() - lastMouseX;
            double deltaY = e.getSceneY() - lastMouseY;

            rotateY.setAngle(rotateY.getAngle() + deltaX * 0.4);
            rotateX.setAngle(Math.clamp(rotateX.getAngle() - deltaY * 0.4, -85.0, 85.0));

            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();
        });

        subScene.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double newZ = camera.getTranslateZ() + delta * 2.0;
            camera.setTranslateZ(Math.clamp(newZ, -2200.0, -450.0));
        });
    }

    public void flyTo(double targetLat, double targetLng) {
        if (flyTimer != null) {
            flyTimer.stop();
        }

        final double startX = rotateX.getAngle();
        final double startY = rotateY.getAngle();

        final double targetRotateX = -targetLat;
        double dY = (-targetLng) - startY;
        while (dY > 180.0) dY -= 360.0;
        while (dY < -180.0) dY += 360.0;
        final double targetRotateY = startY + dY;

        final long startNs = System.nanoTime();
        final long durationNs = 1_000_000_000L;

        flyTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double elapsed = (now - startNs) / (double) durationNs;
                if (elapsed >= 1.0) {
                    rotateX.setAngle(targetRotateX);
                    rotateY.setAngle(targetRotateY);
                    stop();
                    flyTimer = null;
                } else {
                    double t = 0.5 - 0.5 * Math.cos(elapsed * Math.PI);
                    rotateX.setAngle(startX + t * (targetRotateX - startX));
                    rotateY.setAngle(startY + t * (targetRotateY - startY));
                }
            }
        };
        flyTimer.start();
    }

    /**
     * Builds or updates the hardware 3D mesh representation of the H3 cell terrain.
     */
    public void updateTerrainMesh(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        TriangleMesh mesh = new TriangleMesh();
        int cellCount = cells.size();

        // 1. Generate Vertices and UV Coordinates
        float[] points = new float[cellCount * 3];
        float[] texCoords = new float[cellCount * 2];

        // Dynamic Texture Map Generation
        int texWidth = 512;
        int texHeight = 256;
        WritableImage textureMap = new WritableImage(texWidth, texHeight);
        PixelWriter pw = textureMap.getPixelWriter();

        for (int y = 0; y < texHeight; y++) {
            for (int x = 0; x < texWidth; x++) {
                pw.setColor(x, y, Color.rgb(10, 35, 80));
            }
        }

        for (int i = 0; i < cellCount; i++) {
            H3Cell cell = cells.get(i);
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();
            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;

            double elevDisplacement = (elev > 0 ? (elev / 8848.0) * 18.0 * reliefScale : 0.0);
            double r = globeRadius + elevDisplacement;

            double radLat = Math.toRadians(lat);
            double radLng = Math.toRadians(lng);

            float x = (float) (r * Math.cos(radLat) * Math.sin(radLng));
            float y = (float) (-r * Math.sin(radLat));
            float z = (float) (r * Math.cos(radLat) * Math.cos(radLng));

            points[i * 3] = x;
            points[i * 3 + 1] = y;
            points[i * 3 + 2] = z;

            float u = (float) ((lng + 180.0) / 360.0);
            float v = (float) ((90.0 - lat) / 180.0);

            texCoords[i * 2] = u;
            texCoords[i * 2 + 1] = v;

            // Paint texture map pixel for cell with smooth radial kernel blur
            int px = Math.clamp((int) (u * texWidth), 0, texWidth - 1);
            int py = Math.clamp((int) (v * texHeight), 0, texHeight - 1);

            Color cellColor = getBiomeColor(cell);
            int radius = 4; // Smooth interpolation radius across H3 cell neighbors
            for (int dy = -radius; dy <= radius; dy++) {
                int ny = py + dy;
                if (ny < 0 || ny >= texHeight) continue;
                for (int dx = -radius; dx <= radius; dx++) {
                    int nx = (px + dx + texWidth) % texWidth; // Wrap longitude horizontally
                    double distSq = dx * dx + dy * dy;
                    if (distSq <= radius * radius) {
                        pw.setColor(nx, ny, cellColor);
                    }
                }
            }
        }

        // Apply a fast 3x3 spatial smoothing pass over the texture map for continuous fluid heatmap transitions
        WritableImage smoothedTextureMap = new WritableImage(texWidth, texHeight);
        PixelWriter spw = smoothedTextureMap.getPixelWriter();
        PixelReader pr = textureMap.getPixelReader();

        for (int y = 0; y < texHeight; y++) {
            for (int x = 0; x < texWidth; x++) {
                double rAcc = 0, gAcc = 0, bAcc = 0, weightAcc = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    int ny = Math.clamp(y + dy, 0, texHeight - 1);
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = (x + dx + texWidth) % texWidth;
                        Color c = pr.getColor(nx, ny);
                        double weight = (dx == 0 && dy == 0) ? 2.0 : 1.0;
                        rAcc += c.getRed() * weight;
                        gAcc += c.getGreen() * weight;
                        bAcc += c.getBlue() * weight;
                        weightAcc += weight;
                    }
                }
                spw.setColor(x, y, Color.color(
                    Math.clamp(rAcc / weightAcc, 0.0, 1.0),
                    Math.clamp(gAcc / weightAcc, 0.0, 1.0),
                    Math.clamp(bAcc / weightAcc, 0.0, 1.0)
                ));
            }
        }
        textureMap = smoothedTextureMap;

        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);

        // 2. Generate Triangles (Delaunay/Grid Connectivity Proxy)
        int numFaces = Math.max(0, cellCount - 2);
        int[] faces = new int[numFaces * 6];

        for (int i = 0; i < numFaces; i++) {
            faces[i * 6] = i;
            faces[i * 6 + 1] = i;

            faces[i * 6 + 2] = i + 1;
            faces[i * 6 + 3] = i + 1;

            faces[i * 6 + 4] = i + 2;
            faces[i * 6 + 5] = i + 2;
        }

        mesh.getFaces().setAll(faces);

        // 3. Apply PhongMaterial with generated texture map
        PhongMaterial terrainMaterial = new PhongMaterial();
        terrainMaterial.setDiffuseMap(textureMap);
        terrainMaterial.setSpecularColor(Color.rgb(120, 140, 160));
        terrainMaterial.setSpecularPower(16.0);

        terrainMeshView.setMesh(mesh);
        terrainMeshView.setMaterial(terrainMaterial);
        logger.info("Updated JavaFX 3D Hardware Globe mesh with {} vertices", cellCount);
    }

    private Color getBiomeColor(H3Cell cell) {
        if (cell.getElevation() != null && cell.getElevation() < 0) {
            return Color.rgb(14, 55, 115);
        }

        Biome biome = cell.getBiome();
        if (biome == null) return Color.rgb(34, 197, 94);

        return switch (biome) {
            case TUNDRA -> Color.rgb(148, 163, 184);
            case SNOW, GLACIER -> Color.rgb(241, 245, 249);
            case DESERT -> Color.rgb(234, 179, 8);
            case SAVANNAH -> Color.rgb(163, 230, 53);
            case FOREST -> Color.rgb(22, 101, 52);
            case JUNGLE -> Color.rgb(6, 78, 59);
            case MOUNTAINS -> Color.rgb(100, 116, 139);
            case HILLS -> Color.rgb(134, 239, 172);
            default -> Color.rgb(34, 197, 94);
        };
    }

    public void updateDimensions(double width, double height) {
        subScene.setWidth(width);
        subScene.setHeight(height);
    }
}
