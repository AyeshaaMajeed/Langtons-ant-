package com.example.langtonsant.Main;

import java.awt.Point;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Grid {
    private final int LOCK_ARRAY_SIZE = 256; // Power of 2 for efficient modulo
    private final int gridSize;
    private final ConcurrentMap<Point, Boolean> cells = new ConcurrentHashMap<>();
    private final Object[] cellLocks;

    // Performance metrics
    private AtomicInteger cellFlipCount = new AtomicInteger(0);
    private int lastReportedFlipCount = 0;

    public Grid(int size) {
        this.gridSize = size;

        // Initialize fine-grained locks for cells
        this.cellLocks = new Object[LOCK_ARRAY_SIZE];
        for (int i = 0; i < LOCK_ARRAY_SIZE; i++) {
            cellLocks[i] = new Object();
        }
    }

    public int getGridSize() {
        return gridSize;
    }

    public boolean getCellState(int x, int y) {
        return cells.getOrDefault(new Point(x, y), false);
    }

    public void flip(int x, int y) {
        Point p = new Point(x, y);

        // Compute lock index based on position - distribute locks to reduce contention
        int lockIndex = Math.abs((x * 73 + y * 151) % LOCK_ARRAY_SIZE);

        // Lock only the specific cell region
        synchronized (cellLocks[lockIndex]) {
            // Toggle cell state
            Boolean currentState = cells.get(p);
            if (currentState == null || !currentState) {
                cells.put(p, true);
            } else {
                cells.remove(p); // Save memory by removing white cells
            }
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

    public void flipBatch(List<Point> points) {
        // Group points by their lock index to reduce lock contention
        for (Point p : points) {
            int lockIndex = Math.abs((p.x * 73 + p.y * 151) % LOCK_ARRAY_SIZE);
            synchronized (cellLocks[lockIndex]) {
                Boolean currentState = cells.get(p);
                if (currentState == null || !currentState) {
                    cells.put(p, true);
                } else {
                    cells.remove(p);
                }
            }
        }
        cellFlipCount.addAndGet(points.size());
    }
}