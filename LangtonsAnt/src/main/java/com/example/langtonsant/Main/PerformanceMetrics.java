package com.example.langtonsant.Main;

import java.util.LinkedList;
import java.util.Queue;

public class PerformanceMetrics {
    private static final int MAX_HISTORY = 100;

    // Current metrics
    private long startTimeNano;
    private long lastSampleTimeNano;
    private int cellsFlipped;
    private int stepsCompleted;

    // History for charting
    private final Queue<Double> stepsPerSecondHistory = new LinkedList<>();
    private final Queue<Double> cellsPerSecondHistory = new LinkedList<>();
    private final Queue<Double> timePerStepHistory = new LinkedList<>();

    // Comparison data
    private double singleModeTimePerStep;
    private double parallelModeTimePerStep;
    private double singleModeCellsPerSecond;
    private double parallelModeCellsPerSecond;

    // Runtime information
    private int threadCount;
    private boolean isParallelMode;
    private int antCount;

    public PerformanceMetrics() {
        reset();
    }

    public void reset() {
        startTimeNano = System.nanoTime();
        lastSampleTimeNano = startTimeNano;
        cellsFlipped = 0;
        stepsCompleted = 0;
        stepsPerSecondHistory.clear();
        cellsPerSecondHistory.clear();
        timePerStepHistory.clear();
    }

    public void startRun(boolean isParallel, int antCount, int threadCount) {
        this.isParallelMode = isParallel;
        this.antCount = antCount;
        this.threadCount = threadCount;
        reset();
    }

    public void updateMetrics(int newSteps, int newCellFlips) {
        long currentTime = System.nanoTime();
        double elapsedSeconds = (currentTime - lastSampleTimeNano) / 1_000_000_000.0;

        if (elapsedSeconds > 0.1) { // Update every 100ms
            cellsFlipped += newCellFlips;
            stepsCompleted += newSteps;

            // Calculate metrics
            double stepsPerSecond = newSteps / elapsedSeconds;
            double cellsPerSecond = newCellFlips / elapsedSeconds;
            double timePerStep = elapsedSeconds * 1000 / newSteps; // in ms

            // Add to history, maintaining max size
            addToHistory(stepsPerSecondHistory, stepsPerSecond);
            addToHistory(cellsPerSecondHistory, cellsPerSecond);
            addToHistory(timePerStepHistory, timePerStep);

            lastSampleTimeNano = currentTime;
        }
    }

    private void addToHistory(Queue<Double> history, double value) {
        history.add(value);
        while (history.size() > MAX_HISTORY) {
            history.poll();
        }
    }

    public void finishRun() {
        long endTime = System.nanoTime();
        double totalTimeSeconds = (endTime - startTimeNano) / 1_000_000_000.0;
        double avgTimePerStep = 0;
        double avgCellsPerSecond = 0;

        if (stepsCompleted > 0) {
            avgTimePerStep = (totalTimeSeconds * 1000) / stepsCompleted; // in ms
        }

        if (totalTimeSeconds > 0) {
            avgCellsPerSecond = cellsFlipped / totalTimeSeconds;
        }

        // Store for comparison
        if (isParallelMode) {
            parallelModeTimePerStep = avgTimePerStep;
            parallelModeCellsPerSecond = avgCellsPerSecond;
        } else {
            singleModeTimePerStep = avgTimePerStep;
            singleModeCellsPerSecond = avgCellsPerSecond;
        }
    }

    // Getters
    public double getSpeedup() {
        if (parallelModeTimePerStep > 0 && singleModeTimePerStep > 0) {
            return singleModeTimePerStep / parallelModeTimePerStep;
        }
        return 0;
    }

    public double getEfficiency() {
        double speedup = getSpeedup();
        return speedup > 0 ? speedup / threadCount : 0;
    }

    public double getCurrentStepsPerSecond() {
        return stepsPerSecondHistory.isEmpty() ? 0 : stepsPerSecondHistory.peek();
    }

    public double getCurrentCellsPerSecond() {
        return cellsPerSecondHistory.isEmpty() ? 0 : cellsPerSecondHistory.peek();
    }

    public double getCurrentTimePerStep() {
        return timePerStepHistory.isEmpty() ? 0 : timePerStepHistory.peek();
    }

    public Queue<Double> getStepsPerSecondHistory() {
        return stepsPerSecondHistory;
    }

    public Queue<Double> getCellsPerSecondHistory() {
        return cellsPerSecondHistory;
    }

    public Queue<Double> getTimePerStepHistory() {
        return timePerStepHistory;
    }

    public double getSingleModeTimePerStep() {
        return singleModeTimePerStep;
    }

    public double getParallelModeTimePerStep() {
        return parallelModeTimePerStep;
    }

    public double getSingleModeCellsPerSecond() {
        return singleModeCellsPerSecond;
    }

    public double getParallelModeCellsPerSecond() {
        return parallelModeCellsPerSecond;
    }

    public boolean isParallelMode() {
        return isParallelMode;
    }

    public int getAntCount() {
        return antCount;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public int getTotalStepsCompleted() {
        return stepsCompleted;
    }

    public int getTotalCellsFlipped() {
        return cellsFlipped;
    }

    public double getTotalElapsedSeconds() {
        return (System.nanoTime() - startTimeNano) / 1_000_000_000.0;
    }
}