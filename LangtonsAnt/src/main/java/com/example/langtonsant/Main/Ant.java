package com.example.langtonsant.Main;

import javafx.scene.paint.Color;

import java.util.Random;

public class Ant {
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

    public void move(Grid grid) {
        // Get current cell state
        boolean currentCellState = grid.isBlack(x, y);

        // Flip the color of the current cell
        grid.flip(x, y);

        // Turn based on the cell's color
        if (currentCellState) {
            // On black cell, turn left
            direction = (direction + 3) % 4;
        } else {
            // On white cell, turn right
            direction = (direction + 1) % 4;
        }

        // Move forward
        switch (direction) {
            case 0: // up
                y--;
                break;
            case 1: // right
                x++;
                break;
            case 2: // down
                y++;
                break;
            case 3: // left
                x--;
                break;
        }
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