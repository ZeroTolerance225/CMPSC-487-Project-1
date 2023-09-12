package com.example.project_one_487;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import static com.example.project_one_487.MainScreen.getUserIdBySchoolId;
import static com.example.project_one_487.MainScreen.getUserRole;

public class StudentScreen {

    public static void display(Stage primaryStage) {
        // Create the main layout for the student screen
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(20));

        // Create a button for swiping out
        Button swipeOutButton = new Button("Swipe Out");

        // Define the action to perform when the swipe-out button is pressed
        swipeOutButton.setOnAction(event -> {
            logStudentSwipeOut(primaryStage); // Pass the primaryStage
            primaryStage.close();
        });

        // If the user closes the window using the 'X', it should also log their departure.
        primaryStage.setOnCloseRequest(event -> {
            logStudentSwipeOut(primaryStage); // Pass the primaryStage
        });

        // Add the swipe-out button to the layout
        mainLayout.getChildren().addAll(swipeOutButton);

        // Set up the primary stage for the student screen
        primaryStage.setScene(new Scene(mainLayout, 300, 200));
        primaryStage.setTitle("Student View");
        primaryStage.show();
    }

    private static void logStudentSwipeOut(Stage primaryStage) {
        System.out.println("Swipe out button pressed.");
        try (Connection connection = DatabaseConnection.getConnection()) {
            String currentStudentId = LoginScreen.currentSchoolId;
            System.out.println("Using student ID: " + currentStudentId);

            // Get the user ID based on the school ID
            int userId = getUserIdBySchoolId(currentStudentId, connection);

            String query = "INSERT INTO Swipes (IsSwipeIn, UserId, SwipeTimeStamp) VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setBoolean(1, false); // Set IsSwipeIn to false for swipe-out
            statement.setInt(2, userId);
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            int rowsInserted = statement.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("Swipe-out logged successfully.");
            } else {
                System.out.println("No swipe-out logged (no matching swipe-in record).");
            }

            primaryStage.hide();

            // Determine the user role based on the user ID
            String userRole = getUserRole(String.valueOf(userId));
            System.out.println("Detected user role: " + userRole);

            if ("Student".equalsIgnoreCase(userRole)) {
                display(primaryStage); // Display StudentScreen again
            } else if ("Faculty".equalsIgnoreCase(userRole)) {
                AdminScreen.display(primaryStage); // Assuming there's an AdminScreen you haven't shown yet
            } else {
                System.out.println("Unexpected user role: " + userRole);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

}
