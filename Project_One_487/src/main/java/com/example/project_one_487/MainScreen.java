package com.example.project_one_487;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class MainScreen {

    public static void display(Stage primaryStage) {
        // Create the main layout for the main screen
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));

        // Create a button for swiping in
        Button swipeInBtn = new Button("Swipe In");

        // Define the action to perform when the swipe-in button is pressed
        swipeInBtn.setOnAction(e -> handleSwipeIn(primaryStage));  // Refactored to a method for clarity

        // Add the swipe-in button to the layout
        layout.getChildren().addAll(swipeInBtn);

        // Set up the primary stage for the main screen
        primaryStage.setScene(new Scene(layout, 300, 200));
        primaryStage.setTitle("Main View");
        primaryStage.show();
    }

    private static void handleSwipeIn(Stage primaryStage) {
        System.out.println("Swipe in button pressed.");
        try (Connection connection = DatabaseConnection.getConnection()) {
            String currentStudentId = LoginScreen.currentSchoolId;
            System.out.println("Using student ID: " + currentStudentId);

            // Get the user ID based on the school ID
            int userId = getUserIdBySchoolId(currentStudentId, connection);

            String query = "INSERT INTO Swipes (IsSwipeIn, UserId, SwipeTimeStamp) VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setBoolean(1, true);
            statement.setInt(2, userId);
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            statement.executeUpdate();

            System.out.println("Swipe-in logged successfully."); // Add this line

            primaryStage.hide();

            // Determine the user role based on the user ID
            String userRole = getUserRole(String.valueOf(userId));
            System.out.println("Detected user role: " + userRole);

            if ("Student".equalsIgnoreCase(userRole)) {
                StudentScreen.display(primaryStage);
            } else if ("Faculty".equalsIgnoreCase(userRole)) {
                AdminScreen.display(primaryStage);  // Assuming there's an AdminScreen you haven't shown yet
            } else {
                System.out.println("Unexpected user role: " + userRole);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Retrieve the user ID based on the school ID from the database
    static int getUserIdBySchoolId(String schoolId, Connection connection) throws SQLException {
        String query = "SELECT UserId FROM Users WHERE SchoolId = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, schoolId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("UserId");
                }
                throw new SQLException("No user found for student ID: " + schoolId);
            }
        }
    }

    // Retrieve the user role based on the user ID from the database
    static String getUserRole(String userId) {
        String role = "";
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT UserType FROM Users u JOIN UserTypes ut ON u.UserTypeId = ut.UserTypeId WHERE u.UserId = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, Integer.parseInt(userId));
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                role = resultSet.getString("UserType");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return role;
    }
}
