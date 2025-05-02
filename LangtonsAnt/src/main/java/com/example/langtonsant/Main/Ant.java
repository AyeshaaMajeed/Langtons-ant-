package com.example.langtonsant.Main;

import javafx.scene.paint.Color;
import java.awt.Point;
import java.util.Random;

public class Ant {
    // Direction vectors for efficient movement calculation
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DY = {-1, 0, 1, 0};

    private int x;
    private int y;
    private int direction; // 0: up, 1: right, 2: down, 3: left
    private final Color color;
    private static final Random random = new Random();

    public Ant(int x, int y) {
        this.x = x;
        this.y = y;
        this.direction = random.nextInt(4);

        // Generate random color for visualization
        this.color = Color.rgb(
                random.nextInt(200) + 55,
                random.nextInt(200) + 55,
                random.nextInt(200) + 55
        );
    }

    /**
     * Move the ant according to Langton's ant rules
     */
    public void move(Grid grid) {
        // Get current cell state
        boolean currentCellState = grid.isBlack(x, y);

        // Flip the color of the current cell
        grid.flip(x, y);

        // Turn based on the cell's color (using bitwise AND for modulo 4)
        if (currentCellState) {
            // On black cell, turn left (90° counter-clockwise)
            direction = (direction + 3) & 3;
        } else {
            // On white cell, turn right (90° clockwise)
            direction = (direction + 1) & 3;
        }

        // Move forward using direction vectors (more efficient)
        x += DX[direction];
        y += DY[direction];
    }

    /**
     * Move the ant and optionally flip the cell color
     * Returns the current position before moving
     */
    public Point move(Grid grid, boolean justCalculate) {
        // Create point representing current position
        Point p = new Point(x, y);

        // Get current cell state
        boolean currentCellState = grid.isBlack(x, y);

        // Flip the cell color if not just calculating
        if (!justCalculate) {
            grid.flip(x, y);
        }

        // Turn based on the cell's color
        if (currentCellState) {
            // On black cell, turn left
            direction = (direction + 3) & 3; // Using bitwise AND for modulo 4
        } else {
            // On white cell, turn right
            direction = (direction + 1) & 3;
        }

        // Move forward using direction vectors
        x += DX[direction];
        y += DY[direction];

        return p;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Color getColor() {
        return color;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}