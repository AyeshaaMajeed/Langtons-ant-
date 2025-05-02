package com.example.langtonsant.Main;

import java.awt.Point;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Grid {
    private final int gridSize;
    private final ConcurrentMap<Point, Boolean> cells = new ConcurrentHashMap<>();

    // Performance metrics
    private AtomicInteger cellFlipCount = new AtomicInteger(0);
    private int lastReportedFlipCount = 0;

    public Grid(int size) {
        this.gridSize = size;
    }

    public int getGridSize() {
        return gridSize;
    }

    public boolean getCellState(int x, int y) {
        return cells.getOrDefault(new Point(x, y), false);
    }

    public void flip(int x, int y) {
        Point p = new Point(x, y);
        // Atomic operation: compute ensures thread-safety
        cells.compute(p, (k, v) -> v == null || !v);
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