package com.example.langtonsant.Main;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.awt.Point;
import java.util.List;
import java.util.Map;

public class AntCanvas extends Canvas {

    private final Grid grid;
    private final List<Ant> ants;
    private final int width;
    private final int height;

    // Viewport management
    private double viewportX = 0; // center X position in grid coordinates
    private double viewportY = 0; // center Y position in grid coordinates
    private double scale = 5.0;   // pixels per cell

    // For panning
    private double lastMouseX;
    private double lastMouseY;
    private boolean isPanning = false;

    // Display information
    private boolean showGridLines = true;
    private boolean showInfo = true;

    public AntCanvas(Grid grid, List<Ant> ants, int width, int height) {
        super(width, height);
        this.grid = grid;
        this.ants = ants;
        this.width = width;
        this.height = height;

        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Zoom with mouse wheel
        setOnScroll(e -> {
            handleZoom(e);
            draw();
            e.consume();
        });

        // Pan with mouse drag
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.MIDDLE || e.getButton() == MouseButton.PRIMARY) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                isPanning = true;
                e.consume();
            }
        });

        setOnMouseDragged(e -> {
            if (isPanning) {
                double deltaX = e.getX() - lastMouseX;
                double deltaY = e.getY() - lastMouseY;

                viewportX -= deltaX / scale;
                viewportY -= deltaY / scale;

                lastMouseX = e.getX();
                lastMouseY = e.getY();
                draw();
                e.consume();
            }
        });

        setOnMouseReleased(e -> {
            isPanning = false;
            e.consume();
        });
    }

    private void handleZoom(ScrollEvent e) {
        double zoomFactor = e.getDeltaY() > 0 ? 1.1 : 0.9;

        // Get the mouse position in grid coordinates before scaling
        double mouseX = screenToGridX(e.getX());
        double mouseY = screenToGridY(e.getY());

        // Apply zoom
        scale *= zoomFactor;

        // Constrain the zoom level
        scale = Math.max(0.5, Math.min(50, scale));

        // Adjust viewportX and viewportY to keep mouse position fixed
        double newMouseX = screenToGridX(e.getX());
        double newMouseY = screenToGridY(e.getY());

        viewportX += (mouseX - newMouseX);
        viewportY += (mouseY - newMouseY);
    }

    // Convert grid coordinates to screen coordinates
    private double gridToScreenX(double gridX) {
        return width / 2.0 + (gridX - viewportX) * scale;
    }

    private double gridToScreenY(double gridY) {
        return height / 2.0 + (gridY - viewportY) * scale;
    }

    // Convert screen coordinates to grid coordinates
    private double screenToGridX(double screenX) {
        return viewportX + (screenX - width / 2.0) / scale;
    }

    private double screenToGridY(double screenY) {
        return viewportY + (screenY - height / 2.0) / scale;
    }

    // Determine if a cell is currently visible in the viewport
    private boolean isInViewport(double gridX, double gridY) {
        double screenX = gridToScreenX(gridX);
        double screenY = gridToScreenY(gridY);

        // Add a small buffer around the viewport to ensure smooth scrolling
        double buffer = scale * 2;
        return screenX >= -buffer && screenX <= width + buffer &&
                screenY >= -buffer && screenY <= height + buffer;
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();

        // Clear the canvas
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, width, height);

        // Calculate visible range in grid coordinates
        double minVisibleX = screenToGridX(0);
        double maxVisibleX = screenToGridX(width);
        double minVisibleY = screenToGridY(0);
        double maxVisibleY = screenToGridY(height);

        // Draw grid lines if scale is large enough and they're enabled
        if (showGridLines && scale >= 3.0) {
            gc.setStroke(Color.GRAY);
            gc.setLineWidth(0.5);

            // Vertical grid lines
            double startX = Math.floor(minVisibleX);
            double endX = Math.ceil(maxVisibleX);
            for (double x = startX; x <= endX; x++) {
                double screenX = gridToScreenX(x);
                gc.strokeLine(screenX, 0, screenX, height);
            }

            // Horizontal grid lines
            double startY = Math.floor(minVisibleY);
            double endY = Math.ceil(maxVisibleY);
            for (double y = startY; y <= endY; y++) {
                double screenY = gridToScreenY(y);
                gc.strokeLine(0, screenY, width, screenY);
            }
        }

        // Draw origin marker
        double originX = gridToScreenX(0);
        double originY = gridToScreenY(0);
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(1.0);
        gc.strokeLine(originX - 10, originY, originX + 10, originY);
        gc.strokeLine(originX, originY - 10, originX, originY + 10);

        // Draw black cells that are visible
        gc.setFill(Color.BLACK);
        for (Map.Entry<Point, Boolean> entry : grid.getCells().entrySet()) {
            if (entry.getValue()) { // If cell is black
                double x = entry.getKey().x;
                double y = entry.getKey().y;

                // Only draw if in viewport
                if (isInViewport(x, y)) {
                    double screenX = gridToScreenX(x);
                    double screenY = gridToScreenY(y);
                    double cellSize = Math.max(1, scale);

                    gc.fillRect(screenX - cellSize/2, screenY - cellSize/2, cellSize, cellSize);
                }
            }
        }

        // Draw ants with direction indicators
        Color[] antColors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE};
        int idx = 0;
        for (Ant ant : ants) {
            double x = ant.getX();
            double y = ant.getY();

            // Only draw if in viewport
            if (isInViewport(x, y)) {
                double screenX = gridToScreenX(x);
                double screenY = gridToScreenY(y);
                double antSize = Math.max(3, scale * 1.2);

                // Draw the ant body
                gc.setFill(antColors[idx % antColors.length]);
                gc.fillOval(screenX - antSize/2, screenY - antSize/2, antSize, antSize);

                // Draw direction indicator
                double dirX = 0, dirY = 0;
                int direction = ant.getDirection();

                // Calculate direction vector
                switch (direction) {
                    case 0: // North
                        dirX = 0;
                        dirY = -1;
                        break;
                    case 1: // East
                        dirX = 1;
                        dirY = 0;
                        break;
                    case 2: // South
                        dirX = 0;
                        dirY = 1;
                        break;
                    case 3: // West
                        dirX = -1;
                        dirY = 0;
                        break;
                }

                // Draw direction indicator
                double indicatorSize = Math.max(2, scale * 0.8);
                double indicatorX = screenX + dirX * (antSize / 2);
                double indicatorY = screenY + dirY * (antSize / 2);

                gc.setFill(Color.YELLOW);
                gc.fillOval(indicatorX - indicatorSize/2, indicatorY - indicatorSize/2,
                        indicatorSize, indicatorSize);

                // Draw a line to make direction more clear if scale is large enough
                if (scale >= 4.0) {
                    gc.setStroke(Color.BLACK);
                    gc.setLineWidth(Math.max(1, scale / 5));
                    gc.strokeLine(screenX, screenY,
                            screenX + dirX * antSize,
                            screenY + dirY * antSize);
                }
            }
            idx++;
        }

        // Display information
        if (showInfo) {
            gc.setFill(Color.WHITE);
            gc.fillRect(10, 10, 220, 70);
            gc.setStroke(Color.BLACK);
            gc.strokeRect(10, 10, 220, 70);

            gc.setFill(Color.BLACK);
            gc.setFont(new Font(12));
            gc.setTextAlign(TextAlignment.LEFT);

            gc.fillText(String.format("Zoom: %.1fx", scale), 20, 30);
            gc.fillText(String.format("Center: (%.0f, %.0f)", viewportX, viewportY), 20, 50);
            gc.fillText(String.format("Visible area: %.0f x %.0f cells",
                    (maxVisibleX - minVisibleX), (maxVisibleY - minVisibleY)), 20, 70);
        }

        // Draw mini-map
        drawMiniMap(gc);
    }

    private void drawMiniMap(GraphicsContext gc) {
        double mapSize = 120;
        double mapX = width - mapSize - 10;
        double mapY = height - mapSize - 10;

        // Draw map background
        gc.setFill(Color.WHITE);
        gc.fillRect(mapX, mapY, mapSize, mapSize);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(mapX, mapY, mapSize, mapSize);

        // Calculate the scale for the mini-map
        int gridSize = grid.getGridSize();
        double miniMapScale = mapSize / (gridSize * 2); // *2 because gridSize represents radius

        // Draw cells on mini-map
        for (Map.Entry<Point, Boolean> entry : grid.getCells().entrySet()) {
            if (entry.getValue()) {
                double x = entry.getKey().x;
                double y = entry.getKey().y;

                // Map grid coordinates to mini-map
                double miniX = mapX + mapSize/2 + x * miniMapScale;
                double miniY = mapY + mapSize/2 + y * miniMapScale;

                // Draw a point on the mini-map
                gc.setFill(Color.BLACK);
                gc.fillRect(miniX, miniY, 1, 1);
            }
        }

        // Draw viewport rectangle on mini-map
        double minX = screenToGridX(0);
        double maxX = screenToGridX(width);
        double minY = screenToGridY(0);
        double maxY = screenToGridY(height);

        double viewRectX = mapX + mapSize/2 + minX * miniMapScale;
        double viewRectY = mapY + mapSize/2 + minY * miniMapScale;
        double viewRectWidth = (maxX - minX) * miniMapScale;
        double viewRectHeight = (maxY - minY) * miniMapScale;

        gc.setStroke(Color.RED);
        gc.strokeRect(viewRectX, viewRectY, viewRectWidth, viewRectHeight);

        // Draw ants on mini-map with direction indicators
        int idx = 0;
        for (Ant ant : ants) {
            double x = ant.getX();
            double y = ant.getY();

            // Map ant coordinates to mini-map
            double miniX = mapX + mapSize/2 + x * miniMapScale;
            double miniY = mapY + mapSize/2 + y * miniMapScale;

            // Draw ant body
            gc.setFill(Color.RED);
            gc.fillOval(miniX - 1.5, miniY - 1.5, 3, 3);

            // Draw direction indicator on minimap
            int direction = ant.getDirection();
            double dirX = 0, dirY = 0;

            switch (direction) {
                case 0: dirY = -1; break; // North
                case 1: dirX = 1; break;  // East
                case 2: dirY = 1; break;  // South
                case 3: dirX = -1; break; // West
            }

            gc.setStroke(Color.BLACK);
            gc.strokeLine(miniX, miniY, miniX + dirX * 2, miniY + dirY * 2);

            idx++;
        }
    }

    // Public methods for controlling the viewport

    public void zoomIn() {
        scale *= 1.2;
        scale = Math.min(scale, 50);
        draw();
    }

    public void zoomOut() {
        scale /= 1.2;
        scale = Math.max(scale, 0.5);
        draw();
    }

    public void resetView() {
        viewportX = 0;
        viewportY = 0;
        scale = 5.0;
        draw();
    }

    public void centerOnAnts() {
        if (ants.isEmpty()) return;

        // Find center of all ants
        double sumX = 0, sumY = 0;
        for (Ant ant : ants) {
            sumX += ant.getX();
            sumY += ant.getY();
        }

        viewportX = sumX / ants.size();
        viewportY = sumY / ants.size();
        draw();
    }

    public void toggleGridLines() {
        showGridLines = !showGridLines;
        draw();
    }

    public void toggleInfo() {
        showInfo = !showInfo;
        draw();
    }
}