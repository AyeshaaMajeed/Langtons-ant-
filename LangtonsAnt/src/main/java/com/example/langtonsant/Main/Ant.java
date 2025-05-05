package com.example.langtonsant.Main;

import java.awt.Point;

public class Ant {
    private int x;
    private int y;
    private int direction; // 0=north, 1=east, 2=south, 3=west

    public Ant(int x, int y) {
        this.x = x;
        this.y = y;
        this.direction = 0; // Start facing north
    }

    public void move(Grid grid) {
        // Check if current cell is black
        boolean currentCellBlack = grid.isBlack(x, y);

        // Turn according to Langton's rules
        if (currentCellBlack) {
            turnLeft();
        } else {
            turnRight();
        }

        // Flip the color of the current cell
        grid.flip(x, y);

        // Move forward one step
        moveForward();
    }

    // Method to move forward based on current direction
    public void moveForward() {
        switch (direction) {
            case 0: // North
                y--;
                break;
            case 1: // East
                x++;
                break;
            case 2: // South
                y++;
                break;
            case 3: // West
                x--;
                break;
        }
    }

    // Turn 90 degrees right (clockwise)
    public void turnRight() {
        direction = (direction + 1) % 4;
    }

    // Turn 90 degrees left (counter-clockwise)
    public void turnLeft() {
        direction = (direction + 3) % 4; // +3 is equivalent to -1 with modulo 4
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDirection() {
        return direction;
    }

    public Point getDirectionPoint() {
        Point dirPoint = new Point(0, 0);
        switch (direction) {
            case 0: // North
                dirPoint.y = -1;
                break;
            case 1: // East
                dirPoint.x = 1;
                break;
            case 2: // South
                dirPoint.y = 1;
                break;
            case 3: // West
                dirPoint.x = -1;
                break;
        }
        return dirPoint;
    }
}