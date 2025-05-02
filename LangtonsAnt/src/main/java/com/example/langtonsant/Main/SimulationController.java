package com.example.langtonsant.Main;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationController {

    // UI update optimization
    private static final int UI_UPDATE_FREQUENCY = 5;
    private int uiUpdateCounter = 0;

    // Batch processing optimization
    private static final int DEFAULT_BATCH_SIZE = 10;
    private int batchSize = DEFAULT_BATCH_SIZE;

    private Timeline timeline;
    private Grid grid;
    private List<Ant> ants;
    private SimulationManager manager;
    private AntCanvas canvas;
    private Stage stageRef;

    private int totalSteps;
    private int currentStep = 0;
    private double stepDelayMs = 100;
    private boolean isRunning = false;

    // Performance tracking
    private PerformanceMetrics performanceMetrics = new PerformanceMetrics();
    private PerformanceDashboard dashboard;
    private Stage dashboardStage;

    // UI components
    private Label antCountLabel = new Label("Ants: 1");
    private TextField antInputField;
    private Label stepCountLabel = new Label("Steps: 0");
    private Label modeLabel = new Label("Mode: Single Thread");
    private Label cellsPerSecLabel = new Label("Cells/sec: 0");
    private Label timePerStepLabel = new Label("Time/step: 0 ms");

    // Statistics tracking
    private long lastUpdateTime = System.nanoTime();
    private AtomicInteger stepsProcessed = new AtomicInteger(0);

    public SimulationController(int gridSize, int steps) {
        this.totalSteps = steps;
        this.grid = new Grid(gridSize);
        this.ants = new ArrayList<>();
        this.manager = new SimulationManager(grid, ants);
        this.canvas = new AntCanvas(grid, ants, 800, 600);

        ants.add(new Ant(0, 0));
        createTimeline();
        canvas.draw();
    }

    private void createTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.millis(stepDelayMs), e -> stepSimulation()));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void stepSimulation() {
        // Initialize performance metrics on first step
        if (currentStep == 0) {
            performanceMetrics.startRun(manager.isUseParallel(), ants.size(), manager.getThreadCount());
            lastUpdateTime = System.nanoTime();
            stepsProcessed.set(0);
        }

        int initialSteps = currentStep;
        int initialFlips = grid.getCellFlipCount();

        // Dynamically adjust batch size based on ant count
        adjustBatchSize();

        // Process a batch of steps
        for (int i = 0; i < batchSize && currentStep < totalSteps; i++) {
            manager.step();
            currentStep++;
            stepsProcessed.incrementAndGet();
        }

        // Update metrics
        int newSteps = currentStep - initialSteps;
        int newFlips = grid.getNewFlipCount();
        performanceMetrics.updateMetrics(newSteps, newFlips);

        // Update step counter label always to show progress
        stepCountLabel.setText(String.format("Steps: %,d / %,d", currentStep, totalSteps));

        // Update performance statistics
        updatePerformanceStatistics();

        // Only update UI periodically to reduce overhead
        // Only update UI periodically to reduce overhead
        uiUpdateCounter++;
        boolean isLastBatch = currentStep >= totalSteps;

        if (isLastBatch || uiUpdateCounter % UI_UPDATE_FREQUENCY == 0) {
            canvas.draw();
            // Update dashboard if visible - fixed to match PerformanceDashboard API
            if (dashboard != null && dashboardStage != null && dashboardStage.isShowing()) {
                // The dashboard appears to be linked to performanceMetrics directly,
                // so it doesn't need an explicit update call
                // dashboard.update();
            }
        }

        // Handle completion
        if (isLastBatch) {
            timeline.stop();
            isRunning = false;
            performanceMetrics.finishRun();
            showCompletionDialog();
        }
    }

    private void adjustBatchSize() {
        if (manager.isUseParallel()) {
            if (ants.size() > 500) {
                batchSize = 25; // Large batch for many ants
            } else if (ants.size() > 200) {
                batchSize = 20; // Medium batch for medium ant count
            } else {
                batchSize = DEFAULT_BATCH_SIZE; // Default otherwise
            }
        } else {
            batchSize = DEFAULT_BATCH_SIZE; // Standard batch for single-threaded
        }
    }

    private void updatePerformanceStatistics() {
        long currentTime = System.nanoTime();
        long elapsedNanos = currentTime - lastUpdateTime;

        if (elapsedNanos > 1_000_000_000) { // Update stats every second
            int steps = stepsProcessed.getAndSet(0);
            long elapsedMs = elapsedNanos / 1_000_000;

            if (steps > 0) {
                double timePerStep = elapsedMs / (double) steps;
                double cellsPerSec = grid.getNewFlipCount() / (elapsedNanos / 1_000_000_000.0);

                timePerStepLabel.setText(String.format("Time/step: %.2f ms", timePerStep));
                cellsPerSecLabel.setText(String.format("Cells/sec: %,.0f", cellsPerSec));
            }

            lastUpdateTime = currentTime;
        }
    }

    private void showCompletionDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Simulation Completed");
        alert.setHeaderText("Simulation Results");

        String modeText = manager.isUseParallel() ? "Parallel" : "Single-threaded";
        double totalTimeSeconds = performanceMetrics.getTotalElapsedSeconds();
        double cellsPerSecond = grid.getCellFlipCount() / totalTimeSeconds;

        String content = String.format(
                "Mode: %s\n" +
                        "Total Time: %.2f seconds\n" +
                        "Steps Completed: %,d\n" +
                        "Cells Processed: %,d\n" +
                        "Cells per Second: %,.0f\n",
                modeText, totalTimeSeconds, currentStep, grid.getCellFlipCount(), cellsPerSecond
        );

        if (performanceMetrics.getSpeedup() > 0) {
            content += String.format(
                    "\nComparison:\n" +
                            "Speedup: %.2fx\n" +
                            "Efficiency: %.2f\n",
                    performanceMetrics.getSpeedup(),
                    performanceMetrics.getEfficiency()
            );
        }

        alert.setContentText(content);
        alert.showAndWait();
    }

    private void resetSimulation() {
        timeline.stop();
        isRunning = false;
        currentStep = 0;
        uiUpdateCounter = 0;
        grid.clear();
        ants.clear();

        int newAnts = 1;
        try {
            if (antInputField != null && !antInputField.getText().isEmpty()) {
                newAnts = Integer.parseInt(antInputField.getText());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ant count, using 1 by default.");
        }

        // More efficient ant creation with better spatial distribution
        spawnAntsWithDistribution(newAnts);

        antCountLabel.setText("Ants: " + ants.size());
        stepCountLabel.setText("Steps: 0 / " + totalSteps);
        timePerStepLabel.setText("Time/step: 0 ms");
        cellsPerSecLabel.setText("Cells/sec: 0");

        canvas.draw();
        canvas.centerOnAnts();
    }

    private void spawnAntsWithDistribution(int count) {
        Random rand = new Random();
        int gridSpan = Math.max(50, (int)Math.sqrt(count) * 10);

        for (int i = 0; i < count; i++) {
            // Use a better distribution to reduce clustering
            int x = rand.nextInt(gridSpan * 2) - gridSpan;
            int y = rand.nextInt(gridSpan * 2) - gridSpan;

            // Add a small random offset to prevent perfect grid alignment
            x += (rand.nextDouble() - 0.5);
            y += (rand.nextDouble() - 0.5);

            ants.add(new Ant(x, y));
        }
    }

    public ScrollPane getUI() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(10));
        layout.setAlignment(Pos.CENTER);

        // Simulation control buttons
        Button startButton = new Button("Start");
        startButton.setOnAction(e -> {
            if (!isRunning) {
                timeline.play();
                isRunning = true;
                modeLabel.setText("Mode: " + (manager.isUseParallel() ? "Parallel" : "Single Thread"));
            }
        });

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> {
            timeline.pause();
            isRunning = false;
        });

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> resetSimulation());

        Button parallelButton = new Button("Parallel Mode");
        parallelButton.setOnAction(e -> {
            manager.setUseParallel(true);
            modeLabel.setText("Mode: Parallel");
        });

        Button singleButton = new Button("Single Mode");
        singleButton.setOnAction(e -> {
            manager.setUseParallel(false);
            modeLabel.setText("Mode: Single Thread");
        });

        Button dashboardButton = new Button("Performance Dashboard");
        dashboardButton.setOnAction(e -> showPerformanceDashboard());

        // Navigation buttons
        Button zoomInButton = new Button("Zoom In");
        zoomInButton.setOnAction(e -> canvas.zoomIn());

        Button zoomOutButton = new Button("Zoom Out");
        zoomOutButton.setOnAction(e -> canvas.zoomOut());

        Button centerButton = new Button("Center on Ants");
        centerButton.setOnAction(e -> canvas.centerOnAnts());

        Button resetViewButton = new Button("Reset View");
        resetViewButton.setOnAction(e -> canvas.resetView());

        Button toggleGridButton = new Button("Toggle Grid");
        toggleGridButton.setOnAction(e -> canvas.toggleGridLines());

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> {
            if (dashboardStage != null) dashboardStage.close();
            if (stageRef != null) stageRef.close();
        });

        // Main control panel
        HBox simControls = new HBox(10, startButton, pauseButton, resetButton, parallelButton, singleButton, dashboardButton);
        simControls.setAlignment(Pos.CENTER);

        // Navigation control panel
        HBox navControls = new HBox(10, zoomInButton, zoomOutButton, centerButton, resetViewButton, toggleGridButton);
        navControls.setAlignment(Pos.CENTER);

        // Ant controls
        antInputField = new TextField();
        antInputField.setPromptText("How many ants");
        antInputField.setPrefWidth(100);

        Button addAntsButton = new Button("Add Ants");
        addAntsButton.setOnAction(e -> addAnts());

        HBox antControls = new HBox(10, antInputField, addAntsButton);
        antControls.setAlignment(Pos.CENTER);

        // Speed slider
        Slider speedSlider = new Slider(1, 1000, 100);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            stepDelayMs = newVal.doubleValue();
            restartTimeline();
        });

        Label speedLabel = new Label("Simulation Speed:");
        HBox speedControl = new HBox(10, speedLabel, speedSlider);
        speedControl.setAlignment(Pos.CENTER);

        // Status panel with additional performance metrics
        HBox statusPanelTop = new HBox(20, antCountLabel, stepCountLabel, modeLabel);
        statusPanelTop.setAlignment(Pos.CENTER);
        statusPanelTop.setPadding(new Insets(5));

        HBox statusPanelBottom = new HBox(20, cellsPerSecLabel, timePerStepLabel);
        statusPanelBottom.setAlignment(Pos.CENTER);
        statusPanelBottom.setPadding(new Insets(5));

        VBox controlsContainer = new VBox(10,
                simControls,
                navControls,
                antControls,
                speedControl,
                statusPanelTop,
                statusPanelBottom,
                closeButton
        );
        controlsContainer.setPadding(new Insets(10));
        controlsContainer.setAlignment(Pos.CENTER);

        // Set the canvas to grow to fill available space
        VBox.setVgrow(canvas, Priority.ALWAYS);
        canvas.setWidth(800);
        canvas.setHeight(600);

        layout.getChildren().addAll(
                canvas,
                controlsContainer
        );

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private void addAnts() {
        try {
            int count = Integer.parseInt(antInputField.getText());
            if (count > 0) {
                spawnAntsWithDistribution(count);
                antCountLabel.setText("Ants: " + ants.size());
                canvas.draw();
                canvas.centerOnAnts();
            }
        } catch (NumberFormatException ex) {
            System.out.println("Invalid number entered.");
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Please enter a valid number of ants.", ButtonType.OK);
            alert.show();
        }
    }

    private void showPerformanceDashboard() {
        if (dashboardStage == null) {
            dashboardStage = new Stage();
            dashboardStage.initModality(Modality.NONE);
            if (stageRef != null) {
                dashboardStage.initOwner(stageRef);
            }
            dashboardStage.setTitle("Performance Dashboard");

            dashboard = new PerformanceDashboard(performanceMetrics);
            Scene scene = new Scene(dashboard, 550, 500);
            dashboardStage.setScene(scene);

            dashboardStage.setOnCloseRequest(event -> {
                if (dashboard != null) {
                    dashboard.stop();
                }
                dashboardStage = null;
            });
        }

        dashboardStage.show();
        dashboardStage.setX(stageRef.getX() + stageRef.getWidth());
        dashboardStage.setY(stageRef.getY());
    }

    private void restartTimeline() {
        boolean wasRunning = isRunning;
        timeline.stop();
        isRunning = false;
        createTimeline();
        if (wasRunning) {
            timeline.play();
            isRunning = true;
        }
    }

    public void setStage(Stage stage) {
        this.stageRef = stage;
    }
}