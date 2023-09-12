package com.example.project_one_487;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javafx.application.Application;

import static com.example.project_one_487.DatabaseConnection.databasePassword;

public class LoginScreen extends Application {

    public static String currentSchoolId;

    private static final String SERVER = "localhost";
    private static final String DATABASE = "master";
    private static final String USERNAME = "Ryan_Brennan";
    static final String CONNECTION_STRING = "jdbc:sqlserver://" + SERVER + ":1433;"
            + "databaseName=" + DATABASE + ";"
            + "user=" + USERNAME + ";"
            + "password=" + databasePassword + ";";

    @Override
    public void start(Stage primaryStage) {
        display(primaryStage);
    }

    public void display(Stage primaryStage) {
        // Create the main layout for the login screen
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(20));

        // Create a text field for entering the school ID
        TextField schoolIdField = new TextField();
        schoolIdField.setPromptText("Enter 9-digit ID");

        // Create a button for logging in
        Button loginButton = new Button("Login");
        Label statusLabel = new Label();

        // Define the action to perform when the login button is clicked
        loginButton.setOnAction(event -> {
            String schoolId = schoolIdField.getText();
            if (isValid(schoolId)) {
                if (!userExists(schoolId)) {
                    statusLabel.setText("User not found");
                } else if (isSuspended(schoolId)) {
                    statusLabel.setText("Invalid login");
                } else {
                    currentSchoolId = schoolId;
                    MainScreen.display(primaryStage); // Assuming there's a MainScreen class for displaying the main application screen
                }
            } else {
                statusLabel.setText("Invalid ID");
            }
        });

        // Add UI elements to the main layout
        mainLayout.getChildren().addAll(schoolIdField, loginButton, statusLabel);

        // Set up the primary stage for the login screen
        primaryStage.setScene(new Scene(mainLayout, 300, 200));
        primaryStage.setTitle("Login");
        primaryStage.show();
    }

    private boolean isValid(String schoolId) {
        // Check if the entered school ID is valid (9 digits)
        return schoolId != null && schoolId.length() == 9;
    }

    private boolean isSuspended(String schoolId) {
        // Check if the user with the given school ID is suspended
        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING)) {
            String query = "SELECT Status FROM Users WHERE [SchoolId] = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, schoolId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int status = resultSet.getInt("Status");
                return status == 0; // Assuming 0 represents a suspended user
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private boolean userExists(String schoolId) {
        // Check if a user with the given school ID exists
        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING)) {
            String query = "SELECT COUNT(*) FROM Users WHERE [SchoolId] = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, schoolId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
