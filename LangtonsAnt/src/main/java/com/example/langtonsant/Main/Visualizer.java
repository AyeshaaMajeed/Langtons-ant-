package com.example.langtonsant.Main;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.ScrollPane;

public class Visualizer extends Application {

    private int gridSize = 10000;
    private int steps = 1000;

    @Override
    public void start(Stage primaryStage) {
        showSetupPopup(primaryStage);
    }

    private void showSetupPopup(Stage owner) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(owner);
        popup.setTitle("Langton's Ant Simulation Setup");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Grid size input
        Label gridSizeLabel = new Label("Grid Size:");
        TextField gridField = new TextField("10000");
        grid.add(gridSizeLabel, 0, 0);
        grid.add(gridField, 1, 0);

        // Steps input
        Label stepsLabel = new Label("Number of Steps:");
        TextField stepField = new TextField("1000");
        grid.add(stepsLabel, 0, 1);
        grid.add(stepField, 1, 1);

        // Description
        Label descriptionLabel = new Label(
                "This simulation will show Langton's Ants moving on a grid.\n" +
                        "You can add multiple ants and compare parallel vs. single-thread performance.\n" +
                        "Use the mouse wheel to zoom, and middle/left button drag to pan the view."
        );
        descriptionLabel.setWrapText(true);
        grid.add(descriptionLabel, 0, 3, 2, 1);

        Button submit = new Button("Start Simulation");
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        grid.add(submit, 0, 4);
        grid.add(errorLabel, 1, 4);

        submit.setOnAction(e -> {
            try {
                gridSize = Integer.parseInt(gridField.getText());
                steps = Integer.parseInt(stepField.getText());

                // Validate input
                if (gridSize < 100 || gridSize > 100000) {
                    errorLabel.setText("Grid size must be between 100 and 100,000");
                    return;
                }

                if (steps < 10 || steps > 10000000) {
                    errorLabel.setText("Steps must be between 10 and 10,000,000");
                    return;
                }

                popup.close();
                launchMainApp(owner);
            } catch (NumberFormatException ex) {
                errorLabel.setText("Please enter valid numbers.");
            }
        });

        VBox layout = new VBox(20, grid);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        popup.setScene(new Scene(layout, 450, 300));
        popup.show();
    }

    private void launchMainApp(Stage stage) {
        SimulationController controller = new SimulationController(gridSize, steps);
        ScrollPane layout = controller.getUI();

        Scene scene = new Scene(layout, 900, 750);
        stage.setTitle("Langton's Ant Simulation - Grid Size: " + gridSize);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        controller.setStage(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}