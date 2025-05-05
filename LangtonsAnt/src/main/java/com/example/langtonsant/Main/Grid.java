package com.example.langtonsant.Main;

import java.awt.Point;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Grid {
    private final int REGION_SIZE = 64; // Size of each region
    private final int gridSize;
    private final ConcurrentMap<Point, Boolean> cells = new ConcurrentHashMap<>();
    private final Object[][] regionLocks;

    // Performance metrics
    private AtomicInteger cellFlipCount = new AtomicInteger(0);
    private int lastReportedFlipCount = 0;

    // Number of regions in each dimension
    private final int numRegions;

    public Grid(int size) {
        this.gridSize = size;
        this.numRegions = (size / REGION_SIZE) + 1;

        // Initialize region-level locks
        this.regionLocks = new Object[numRegions][numRegions];
        for (int x = 0; x < numRegions; x++) {
            for (int y = 0; y < numRegions; y++) {
                regionLocks[x][y] = new Object();
            }
        }
    }

    public int getRegionIndex(int coordinate) {
        // Handle negative coordinates correctly
        if (coordinate < 0) {
            return ((coordinate - REGION_SIZE + 1) / REGION_SIZE);
        }
        return coordinate / REGION_SIZE;
    }

    public int[] getRegionBoundaries(int regionX, int regionY) {
        int startX = regionX * REGION_SIZE;
        int startY = regionY * REGION_SIZE;
        int endX = startX + REGION_SIZE - 1;
        int endY = startY + REGION_SIZE - 1;
        return new int[]{startX, startY, endX, endY};
    }
    
    public Object getRegionLock(int x, int y) {
        int regionX = getRegionIndex(x);
        int regionY = getRegionIndex(y);

        regionX = Math.floorMod(regionX, numRegions);
        regionY = Math.floorMod(regionY, numRegions);

        return regionLocks[regionX][regionY];
    }

    public int getGridSize() {
        return gridSize;
    }

    public int getRegionSize() {
        return REGION_SIZE;
    }

    public int getNumRegions() {
        return numRegions;
    }

    public boolean getCellState(int x, int y) {
        return cells.getOrDefault(new Point(x, y), false);
    }

    public void flip(int x, int y) {
        Point p = new Point(x, y);

        Object lock = getRegionLock(x, y);

        synchronized (lock) {
            // Toggle cell state
            Boolean currentState = cells.get(p);
            if (currentState == null || !currentState) {
                cells.put(p, true);
            } else {
                cells.remove(p);
            }
        }

        cellFlipCount.incrementAndGet();
    }

    public void flipWithBoundaryCheck(int x, int y) {
        Point p = new Point(x, y);

        int regionX = getRegionIndex(x);
        int regionY = getRegionIndex(y);

        boolean nearBoundaryX = (x % REGION_SIZE == 0) || (x % REGION_SIZE == REGION_SIZE - 1);
        boolean nearBoundaryY = (y % REGION_SIZE == 0) || (y % REGION_SIZE == REGION_SIZE - 1);

        if (nearBoundaryX || nearBoundaryY) {
            synchronized(regionLocks[Math.floorMod(regionX, numRegions)][Math.floorMod(regionY, numRegions)]) {
                // Lock potential adjacent regions if we're at a boundary
                if (nearBoundaryX) {
                    synchronized(regionLocks[Math.floorMod(regionX + (x % REGION_SIZE == 0 ? -1 : 1), numRegions)]
                            [Math.floorMod(regionY, numRegions)]) {
                        if (nearBoundaryY) {
                            synchronized(regionLocks[Math.floorMod(regionX, numRegions)]
                                    [Math.floorMod(regionY + (y % REGION_SIZE == 0 ? -1 : 1), numRegions)]) {
                                flipCellInternal(p);
                            }
                        } else {
                            flipCellInternal(p);
                        }
                    }
                } else if (nearBoundaryY) {
                    synchronized(regionLocks[Math.floorMod(regionX, numRegions)]
                            [Math.floorMod(regionY + (y % REGION_SIZE == 0 ? -1 : 1), numRegions)]) {
                        flipCellInternal(p);
                    }
                }
            }
        } else {
            synchronized(regionLocks[Math.floorMod(regionX, numRegions)][Math.floorMod(regionY, numRegions)]) {
                flipCellInternal(p);
            }
        }

        cellFlipCount.incrementAndGet();
    }

    private void flipCellInternal(Point p) {
        Boolean currentState = cells.get(p);
        if (currentState == null || !currentState) {
            cells.put(p, true);
        } else {
            cells.remove(p);
        }
    }

    public void flipCellInternal(int x, int y) {
        Point p = new Point(x, y);
        Boolean currentState = cells.get(p);
        if (currentState == null || !currentState) {
            cells.put(p, true);
        } else {
            cells.remove(p);
        }
        cellFlipCount.incrementAndGet();
    }

    public boolean isBlack(int x, int y) {
        return getCellState(x, y);
    }

    public ConcurrentMap<Point, Boolean> getCells() {
        return cells;
    }

    public int getCellFlipCount() {
        return cellFlipCount.get();
    }

    public int getNewFlipCount() {
        int current = cellFlipCount.get();
        int newFlips = current - lastReportedFlipCount;
        lastReportedFlipCount = current;
        return newFlips;
    }

    public void clear() {
        cells.clear();
        cellFlipCount.set(0);
        lastReportedFlipCount = 0;
    }
}