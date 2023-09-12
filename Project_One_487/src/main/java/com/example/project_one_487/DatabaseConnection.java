package com.example.project_one_487;

import java.sql.*;
import java.util.Scanner;
import javafx.application.Application;

public class DatabaseConnection {

    private static final String SERVER = "localhost";
    private static final String DATABASE = "master";
    private static final String USERNAME = "Ryan_Brennan";
    public static String databasePassword; // This field is used by LoginScreen for the connection string.

    public static void main(String[] args) {
        // Get password from user
        Scanner scanner = new Scanner(System.in);
        System.out.print("Please enter the database password: ");
        databasePassword = scanner.nextLine(); // Corrected this line (removed quotes)

        // Test the connection and data access
        if (testConnection()) {
            System.out.println("Database connection established successfully!");

            // Let's test the data access now
            testDataAccess();

            Application.launch(LoginScreen.class, args);
        } else {
            System.out.println("Failed to connect to the database.");
        }
    }

    public static Connection getConnection() throws SQLException {
        String connectionString = "jdbc:sqlserver://" + SERVER + ":1433;"
                + "databaseName=" + DATABASE + ";"
                + "user=" + USERNAME + ";"
                + "password=" + databasePassword + ";"; // Corrected the variable name here

        return DriverManager.getConnection(connectionString);
    }

    private static boolean testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("Connected to the database successfully.");
            return true;
        } catch (SQLException ex) {
            System.out.println("Failed to connect to the database.");
            ex.printStackTrace();  // This will print more details about the error
            return false;
        }
    }

    private static void testDataAccess() {
        try (Connection conn = getConnection()) {
            String testQuery = "SELECT TOP 1 * FROM Swipes";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(testQuery);

            if (rs.next()) {
                System.out.println("Data accessed successfully! Sample data: " + rs.getString(1)); // Assuming your table has at least one column
            } else {
                System.out.println("Data accessed, but no records found!");
            }

            rs.close();
            stmt.close();

        } catch (SQLException ex) {
            System.out.println("Failed to access data from the database!");
            ex.printStackTrace();
        }
    }
}
