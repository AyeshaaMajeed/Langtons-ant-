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

public class SimulationController {

    private Timeline timeline;
    private Grid grid;
    private List<Ant> ants;
    private SimulationManager manager;
    private AntCanvas canvas;
    private Stage stageRef;

    private int totalSteps;
    private int currentStep = 0;
    private double stepDelayMs = 100;

    // Performance tracking
    private PerformanceMetrics performanceMetrics = new PerformanceMetrics();
    private PerformanceDashboard dashboard;
    private Stage dashboardStage;

    private Label antCountLabel = new Label("Ants: 1");
    private TextField antInputField;
    private Label stepCountLabel = new Label("Steps: 0");
    private Label modeLabel = new Label("Mode: Single Thread");

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
        int batchSize = 10;
        if (currentStep == 0) {
            performanceMetrics.startRun(manager.isUseParallel(), ants.size(), manager.getThreadCount());
        }

        int initialSteps = currentStep;
        int initialFlips = grid.getCellFlipCount();

        for (int i = 0; i < batchSize && currentStep < totalSteps; i++) {
            manager.step();
            currentStep++;
        }

        int newSteps = currentStep - initialSteps;
        int newFlips = grid.getNewFlipCount();

        performanceMetrics.updateMetrics(newSteps, newFlips);

        stepCountLabel.setText(String.format("Steps: %,d / %,d", currentStep, totalSteps));
        canvas.draw();

        if (currentStep >= totalSteps) {
            timeline.stop();
            performanceMetrics.finishRun();

            // Display result summary
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
    }

    private void resetSimulation() {
        timeline.stop();
        currentStep = 0;
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

        Random rand = new Random();
        int offset = 50;
        for (int i = 0; i < newAnts; i++) {
            ants.add(new Ant(rand.nextInt(offset * 2) - offset, rand.nextInt(offset * 2) - offset));
        }

        antCountLabel.setText("Ants: " + ants.size());
        stepCountLabel.setText("Steps: 0 / " + totalSteps);
        canvas.draw();
        canvas.centerOnAnts();
    }

    public ScrollPane getUI() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(10));
        layout.setAlignment(Pos.CENTER);

        // Simulation control buttons
        Button startButton = new Button("Start");
        startButton.setOnAction(e -> {
            timeline.play();
            modeLabel.setText("Mode: " + (manager.isUseParallel() ? "Parallel" : "Single Thread"));
        });

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> timeline.pause());

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

        // Status panel
        HBox statusPanel = new HBox(20,
                antCountLabel,
                stepCountLabel,
                modeLabel
        );
        statusPanel.setAlignment(Pos.CENTER);
        statusPanel.setPadding(new Insets(5));

        VBox controlsContainer = new VBox(10,
                simControls,
                navControls,
                antControls,
                speedControl,
                statusPanel,
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
            Random rand = new Random();
            int offset = 50;
            for (int i = 0; i < count; i++) {
                ants.add(new Ant(rand.nextInt(offset * 2) - offset, rand.nextInt(offset * 2) - offset));
            }
            antCountLabel.setText("Ants: " + ants.size());
            canvas.draw();
            canvas.centerOnAnts();
        } catch (NumberFormatException ex) {
            System.out.println("Invalid number entered.");
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
        boolean running = timeline.getStatus() == Timeline.Status.RUNNING;
        timeline.stop();
        createTimeline();
        if (running) timeline.play();
    }

    public void setStage(Stage stage) {
        this.stageRef = stage;
    }
}