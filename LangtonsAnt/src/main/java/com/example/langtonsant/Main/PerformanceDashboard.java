package com.example.langtonsant.Main;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class PerformanceDashboard extends VBox {
    private final PerformanceMetrics metrics;

    // UI Components
    private final Label modeLabel = new Label("Mode: Single-threaded");
    private final Label antCountLabel = new Label("Ants: 0");
    private final Label stepCountLabel = new Label("Steps: 0");
    private final Label timePerStepLabel = new Label("Time per step: 0.00 ms");
    private final Label cellsPerSecondLabel = new Label("Cells per second: 0");
    private final Label stepsPerSecondLabel = new Label("Steps per second: 0");
    private final Label speedupLabel = new Label("Speedup: 0.00x");
    private final Label efficiencyLabel = new Label("Efficiency: 0.00");
    private final Label elapsedTimeLabel = new Label("Elapsed: 0.0 seconds");

    // Performance charts
    private final PerformanceChart cellsPerSecondChart;
    private final PerformanceChart timePerStepChart;
    private final PerformanceChart stepsPerSecondChart;
    private final ComparisonChart comparisonChart;

    // Update timer
    private AnimationTimer updateTimer;

    public PerformanceDashboard(PerformanceMetrics metrics) {
        this.metrics = metrics;

        // Configure this VBox
        setPadding(new Insets(10));
        setSpacing(10);

        // Create metric panels
        GridPane metricGrid = createMetricGrid();

        // Create performance charts
        cellsPerSecondChart = new PerformanceChart("Cells Processed per Second", "cells/s");
        timePerStepChart = new PerformanceChart("Time per Step", "ms");
        stepsPerSecondChart = new PerformanceChart("Steps per Second", "steps/s");
        comparisonChart = new ComparisonChart();

        // Create tabs for charts
        TabPane chartTabs = new TabPane();
        chartTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab cellsTab = new Tab("Cells/Second", cellsPerSecondChart);
        Tab timeTab = new Tab("Time/Step", timePerStepChart);
        Tab stepsTab = new Tab("Steps/Second", stepsPerSecondChart);
        Tab comparisonTab = new Tab("Comparison", comparisonChart);

        chartTabs.getTabs().addAll(cellsTab, timeTab, stepsTab, comparisonTab);

        // Add components to dashboard
        getChildren().addAll(metricGrid, chartTabs);

        // Start update timer
        startUpdateTimer();
    }

    private GridPane createMetricGrid() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(20);
        grid.setVgap(10);

        // Configure label fonts
        Font labelFont = Font.font("System", FontWeight.BOLD, 12);
        modeLabel.setFont(labelFont);
        speedupLabel.setFont(labelFont);

        // Add labels to grid
        grid.add(modeLabel, 0, 0);
        grid.add(antCountLabel, 1, 0);
        grid.add(stepCountLabel, 2, 0);
        grid.add(elapsedTimeLabel, 3, 0);

        grid.add(timePerStepLabel, 0, 1);
        grid.add(cellsPerSecondLabel, 1, 1);
        grid.add(stepsPerSecondLabel, 2, 1);

        grid.add(speedupLabel, 0, 2);
        grid.add(efficiencyLabel, 1, 2);

        return grid;
    }

    private void startUpdateTimer() {
        updateTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateMetricLabels();
                updateCharts();
            }
        };
        updateTimer.start();
    }

    private void updateMetricLabels() {
        modeLabel.setText("Mode: " + (metrics.isParallelMode() ? "Parallel" : "Single-threaded"));
        antCountLabel.setText(String.format("Ants: %d", metrics.getAntCount()));
        stepCountLabel.setText(String.format("Steps: %,d", metrics.getTotalStepsCompleted()));
        timePerStepLabel.setText(String.format("Time per step: %.3f ms", metrics.getCurrentTimePerStep()));
        cellsPerSecondLabel.setText(String.format("Cells per second: %,.0f", metrics.getCurrentCellsPerSecond()));
        stepsPerSecondLabel.setText(String.format("Steps per second: %,.1f", metrics.getCurrentStepsPerSecond()));
        speedupLabel.setText(String.format("Speedup: %.2fx", metrics.getSpeedup()));
        efficiencyLabel.setText(String.format("Efficiency: %.2f", metrics.getEfficiency()));
        elapsedTimeLabel.setText(String.format("Elapsed: %.1f seconds", metrics.getTotalElapsedSeconds()));
    }

    private void updateCharts() {
        // Fixed method calls with correct Queue type handling
        cellsPerSecondChart.updateData(metrics.getCellsPerSecondHistory());
        timePerStepChart.updateData(metrics.getTimePerStepHistory());
        stepsPerSecondChart.updateData(metrics.getStepsPerSecondHistory());
        comparisonChart.updateData(
                metrics.getSingleModeTimePerStep(),
                metrics.getParallelModeTimePerStep(),
                metrics.getSingleModeCellsPerSecond(),
                metrics.getParallelModeCellsPerSecond()
        );
    }

    public void stop() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
    }

    // Inner class for time-series chart
    private class PerformanceChart extends Canvas {
        private final String title;
        private final String unit;
        private final List<Double> data = new ArrayList<>();
        private double maxValue = 10;

        public PerformanceChart(String title, String unit) {
            super(500, 200);
            this.title = title;
            this.unit = unit;
            drawChart();
        }

        public void updateData(Queue<Double> newData) {
            data.clear();
            data.addAll(newData);

            // Find max value with a 20% buffer
            maxValue = data.stream().mapToDouble(v -> v).max().orElse(10) * 1.2;
            if (maxValue <= 0) maxValue = 10;

            drawChart();
        }

        private void drawChart() {
            GraphicsContext gc = getGraphicsContext2D();
            double width = getWidth();
            double height = getHeight();

            // Clear background
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, width, height);

            // Draw title
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.BOLD, 14));
            gc.fillText(title, 10, 20);

            // Draw axes
            gc.setStroke(Color.BLACK);
            gc.strokeLine(50, height - 30, width - 10, height - 30); // X-axis
            gc.strokeLine(50, height - 30, 50, 30); // Y-axis

            // Draw Y-axis labels
            gc.setFont(Font.font("System", FontWeight.NORMAL, 10));
            gc.fillText("0", 35, height - 30);
            gc.fillText(String.format("%.1f %s", maxValue, unit), 20, 40);

            // Draw data points
            if (data.size() > 1) {
                gc.setStroke(Color.BLUE);
                gc.setLineWidth(2);

                double xStep = (width - 60) / (data.size() - 1);
                double yScale = (height - 60) / maxValue;

                for (int i = 0; i < data.size() - 1; i++) {
                    double x1 = 50 + (i * xStep);
                    double y1 = height - 30 - (data.get(i) * yScale);
                    double x2 = 50 + ((i + 1) * xStep);
                    double y2 = height - 30 - (data.get(i + 1) * yScale);

                    gc.strokeLine(x1, y1, x2, y2);
                }
            }
        }
    }

    // Inner class for bar comparison chart
    private class ComparisonChart extends Canvas {
        private double singleTimePerStep = 0;
        private double parallelTimePerStep = 0;
        private double singleCellsPerSecond = 0;
        private double parallelCellsPerSecond = 0;

        public ComparisonChart() {
            super(500, 300);
            drawChart();
        }

        public void updateData(double singleTime, double parallelTime,
                               double singleCells, double parallelCells) {
            this.singleTimePerStep = singleTime;
            this.parallelTimePerStep = parallelTime;
            this.singleCellsPerSecond = singleCells;
            this.parallelCellsPerSecond = parallelCells;
            drawChart();
        }

        private void drawChart() {
            GraphicsContext gc = getGraphicsContext2D();
            double width = getWidth();
            double height = getHeight();

            // Clear background
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, width, height);

            // Draw title
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.BOLD, 14));
            gc.fillText("Performance Comparison", 10, 20);

            // Draw time per step comparison
            drawBarComparison(
                    gc,
                    "Time per Step (ms) - lower is better",
                    singleTimePerStep,
                    parallelTimePerStep,
                    Color.RED,
                    Color.GREEN,
                    50,
                    70,
                    width - 100,
                    60
            );

            // Draw cells per second comparison
            drawBarComparison(
                    gc,
                    "Cells per Second - higher is better",
                    singleCellsPerSecond,
                    parallelCellsPerSecond,
                    Color.BLUE,
                    Color.PURPLE,
                    50,
                    170,
                    width - 100,
                    60
            );

            // Draw legend
            gc.setFont(Font.font("System", FontWeight.NORMAL, 12));
            gc.setFill(Color.RED);
            gc.fillRect(50, height - 40, 15, 15);
            gc.setFill(Color.BLACK);
            gc.fillText("Single-threaded", 75, height - 28);

            gc.setFill(Color.GREEN);
            gc.fillRect(200, height - 40, 15, 15);
            gc.setFill(Color.BLACK);
            gc.fillText("Parallel", 225, height - 28);
        }

        private void drawBarComparison(GraphicsContext gc, String title, double value1, double value2,
                                       Color color1, Color color2, double x, double y,
                                       double maxWidth, double height) {
            // Draw title
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.NORMAL, 12));
            gc.fillText(title, x, y - 5);

            // Calculate bar widths
            double maxValue = Math.max(value1, value2);
            if (maxValue <= 0) maxValue = 1; // Avoid division by zero

            double width1 = value1 > 0 ? (value1 / maxValue) * maxWidth : 0;
            double width2 = value2 > 0 ? (value2 / maxValue) * maxWidth : 0;

            // Draw bars
            gc.setFill(color1);
            gc.fillRect(x, y, width1, height / 2 - 5);

            gc.setFill(color2);
            gc.fillRect(x, y + height / 2, width2, height / 2 - 5);

            // Draw values
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.NORMAL, 10));
            gc.fillText(String.format("%.2f", value1), x + width1 + 5, y + height / 4);
            gc.fillText(String.format("%.2f", value2), x + width2 + 5, y + height * 3/4);
        }
    }
}