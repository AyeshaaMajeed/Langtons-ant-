package com.example.langtonsant.Main;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

public class RegionTask extends RecursiveAction {
    private final Grid grid;
    private final List<Ant> ants;
    private final int regionX;
    private final int regionY;
    private final int regionSize;
    private static final int SPLIT_THRESHOLD = 16;

    public RegionTask(Grid grid, List<Ant> ants, int regionX, int regionY, int regionSize) {
        this.grid = grid;
        this.ants = ants;
        this.regionX = regionX;
        this.regionY = regionY;
        this.regionSize = regionSize;
    }

    @Override
    protected void compute() {
        // Calculate region boundaries
        int startX = regionX * regionSize;
        int startY = regionY * regionSize;
        int endX = startX + regionSize - 1;
        int endY = startY + regionSize - 1;

        // Find all ants in this region
        List<Ant> regionAnts = new ArrayList<>();
        for (Ant ant : ants) {
            int x = ant.getX();
            int y = ant.getY();

            if (x >= startX && x <= endX && y >= startY && y <= endY) {
                regionAnts.add(ant);
            }
        }

        // If this region has too many ants and is large enough, split it
        if (regionAnts.size() > 8 && regionSize > SPLIT_THRESHOLD) {
            splitRegion();
        } else {
            // Process all ants in this region
            processRegion(regionAnts, startX, startY, endX, endY);
        }
    }

    // Process a region by moving all ants within it
    private void processRegion(List<Ant> regionAnts, int startX, int startY, int endX, int endY) {
        // Get the lock for this region
        Object regionLock = grid.getRegionLock(startX, startY);

        synchronized(regionLock) {
            for (Ant ant : regionAnts) {
                int x = ant.getX();
                int y = ant.getY();

                // Check if ant is near region boundary
                boolean nearBoundary = isNearRegionBoundary(ant, startX, startY, endX, endY);

                if (nearBoundary) {
                    // Handle boundary crossing with special care
                    moveAntWithBoundaryCheck(ant);
                } else {
                    // Regular ant movement within region
                    ant.move(grid);
                }
            }
        }
    }

    // Split the region into four quadrants for parallel processing
    private void splitRegion() {
        int newRegionSize = regionSize / 2;

        // Create four sub-regions
        List<RecursiveAction> tasks = new ArrayList<>(4);

        // Top-left quadrant
        tasks.add(new RegionTask(grid, ants, regionX * 2, regionY * 2, newRegionSize));

        // Top-right quadrant
        tasks.add(new RegionTask(grid, ants, regionX * 2 + 1, regionY * 2, newRegionSize));

        // Bottom-left quadrant
        tasks.add(new RegionTask(grid, ants, regionX * 2, regionY * 2 + 1, newRegionSize));

        // Bottom-right quadrant
        tasks.add(new RegionTask(grid, ants, regionX * 2 + 1, regionY * 2 + 1, newRegionSize));

        // Execute all tasks in parallel
        invokeAll(tasks);
    }

    // Check if an ant is near a region boundary (1 cell away considering direction)
    private boolean isNearRegionBoundary(Ant ant, int startX, int startY, int endX, int endY) {
        int x = ant.getX();
        int y = ant.getY();
        int direction = ant.getDirection();

        // Determine if the ant's next potential move would cross a boundary
        if (direction == 0) { // North
            return y - 1 < startY;
        } else if (direction == 1) { // East
            return x + 1 > endX;
        } else if (direction == 2) { // South
            return y + 1 > endY;
        } else { // West
            return x - 1 < startX;
        }
    }

    // Handle ant movement across region boundaries with proper locking
    private void moveAntWithBoundaryCheck(Ant ant) {
        int currentX = ant.getX();
        int currentY = ant.getY();
        int direction = ant.getDirection();

        // Calculate potential new position after move
        int newX = currentX;
        int newY = currentY;

        switch(direction) {
            case 0: newY--; break; // North
            case 1: newX++; break; // East
            case 2: newY++; break; // South
            case 3: newX--; break; // West
        }

        int currentRegionX = calcRegionIndex(currentX);
        int currentRegionY = calcRegionIndex(currentY);
        int newRegionX = calcRegionIndex(newX);
        int newRegionY = calcRegionIndex(newY);

        // Check if this move crosses a region boundary
        if (currentRegionX != newRegionX || currentRegionY != newRegionY) {
            // Get locks for both regions to prevent deadlock
            Object currentRegionLock = grid.getRegionLock(currentX, currentY);
            Object newRegionLock = grid.getRegionLock(newX, newY);

            // Lock both regions in a consistent order to prevent deadlock
            Object firstLock, secondLock;
            if (System.identityHashCode(currentRegionLock) < System.identityHashCode(newRegionLock)) {
                firstLock = currentRegionLock;
                secondLock = newRegionLock;
            } else {
                firstLock = newRegionLock;
                secondLock = currentRegionLock;
            }

            synchronized(firstLock) {
                synchronized(secondLock) {
                    // Safe to process the ant's move now that we have both region locks
                    boolean currentCellBlack = grid.isBlack(currentX, currentY);

                    // Update direction based on current cell state
                    if (currentCellBlack) {
                        ant.turnLeft();
                    } else {
                        ant.turnRight();
                    }

                    // Flip the cell
                    grid.flipCellInternal(currentX, currentY);


                    ant.moveForward();
                }
            }
        } else {
            ant.move(grid);
        }
    }

    private int calcRegionIndex(int coordinate) {
        int regionSizeForCalc = regionSize;
        // Handle negative coordinates correctly
        if (coordinate < 0) {
            return ((coordinate - regionSizeForCalc + 1) / regionSizeForCalc);
        }
        return coordinate / regionSizeForCalc;
    }
}