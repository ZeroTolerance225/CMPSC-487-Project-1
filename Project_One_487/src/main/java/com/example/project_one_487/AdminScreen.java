package com.example.project_one_487;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.example.project_one_487.DatabaseConnection.getConnection;

public class AdminScreen extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        display(primaryStage);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logStudentSwipeOut(primaryStage);
        }));
    }

    public static void display(Stage primaryStage) {
        // If they close the window using the 'X', it should log their departure.
        primaryStage.setOnCloseRequest(event -> {
            logStudentSwipeOut(primaryStage); // Pass the primaryStage
        });

        HBox mainLayout = new HBox(15);
        mainLayout.setPadding(new Insets(20));

        // Left side: Users Table
        TableView<User> usersTable = new TableView<>();
        TableColumn<User, String> nameColumn = new TableColumn<>("Name");
        TableColumn<User, String> statusColumn = new TableColumn<>("Status");
        TableColumn<User, String> actionColumn = new TableColumn<>("Action");

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        actionColumn.setCellFactory(param -> new TableCell<>() {
            final Button toggleStatusBtn = new Button();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    toggleStatusBtn.setText(user.getStatus().equals("Active") ? "Suspend" : "Activate");
                    toggleStatusBtn.setOnAction(event -> {
                        toggleUserStatus(user.getName(), user.getStatus().equals("Active"), usersTable);
                    });

                    setGraphic(toggleStatusBtn);
                }
            }
        });

        usersTable.getColumns().addAll(nameColumn, statusColumn, actionColumn);
        populateUsersTable(usersTable);

        // Create a TextField for searching
        TextField searchField = new TextField();
        searchField.setPromptText("Search by Name");

        // Add a listener to filter the usersTable based on the search input
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String searchText = newValue.trim().toLowerCase();
            if (searchText.isEmpty()) {
                populateUsersTable(usersTable);
            } else {
                filterUsersTable(searchText, usersTable);
            }
        });

        // Create a VBox for the search input
        VBox searchBox = new VBox(5, new Label("Search Name:"), searchField);
        searchBox.setPadding(new Insets(10));

        // Right side: Swipes History
        VBox swipesBox = new VBox(10);
        Label swipesLabel = new Label("Swipe History:");

        // Filter by School ID
        HBox schoolIdFilterBox = new HBox(10);
        TextField schoolIdFilterField = new TextField();
        schoolIdFilterField.setPromptText("Filter by School ID");
        schoolIdFilterBox.getChildren().addAll(new Label("School ID Filter:"), schoolIdFilterField);

        // Date Filter
        HBox datePickerBox = new HBox(10);
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Filter by Date");
        datePickerBox.getChildren().addAll(new Label("Date Filter:"), datePicker);

        // Time Intervals
        HBox timeFilterBox = new HBox(10);
        Label startTimeLabel = new Label("Start Time:");
        Spinner<Integer> startHourSpinner = new Spinner<>(1, 12, 1);
        Spinner<Integer> startMinuteSpinner = new Spinner<>(0, 59, 0);
        Spinner<String> startAmPmSpinner = new Spinner<>(FXCollections.observableArrayList("AM", "PM"));
        startAmPmSpinner.getValueFactory().setValue("AM"); // Use getValueFactory() to set the value

        Label endTimeLabel = new Label("End Time:");
        Spinner<Integer> endHourSpinner = new Spinner<>(1, 12, 1);
        Spinner<Integer> endMinuteSpinner = new Spinner<>(0, 59, 0);
        Spinner<String> endAmPmSpinner = new Spinner<>(FXCollections.observableArrayList("AM", "PM"));
        endAmPmSpinner.getValueFactory().setValue("AM"); // Use getValueFactory() to set the value

        timeFilterBox.getChildren().addAll(
                new Label("Time Interval Filter:"),
                startTimeLabel, startHourSpinner, new Label(":"), startMinuteSpinner, startAmPmSpinner,
                endTimeLabel, endHourSpinner, new Label(":"), endMinuteSpinner, endAmPmSpinner
        );

        // Submit and Clear Filters Buttons
        HBox filterButtonsBox = new HBox(10);
        Button filterSwipesButton = new Button("Submit Filters");
        Button clearFiltersButton = new Button("Clear Filters");
        Button swipeOutButton = new Button("Swipe Out"); // Add a "Swipe Out" button
        filterButtonsBox.getChildren().addAll(filterSwipesButton, clearFiltersButton, swipeOutButton);

        // Add elements to the filter box
        swipesBox.getChildren().addAll(
                swipesLabel,
                schoolIdFilterBox,
                datePickerBox,
                timeFilterBox,
                filterButtonsBox
        );

        // Swipes Table
        TableView<Swipe> swipesTable = new TableView<>();
        swipesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Create a Name column
        TableColumn<Swipe, String> userNameColumn = new TableColumn<>("Name");
        userNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getUserName()));

        // Filter by School ID
        TableColumn<Swipe, String> schoolIdColumn = new TableColumn<>("School ID");
        schoolIdColumn.setCellValueFactory(new PropertyValueFactory<>("schoolId"));

        // Swipe Timestamp
        TableColumn<Swipe, String> swipeTimestampColumn = new TableColumn<>("Timestamp");
        swipeTimestampColumn.setCellValueFactory(new PropertyValueFactory<>("swipeTimestamp"));

        // Add columns to the swipesTable
        swipesTable.getColumns().addAll(userNameColumn, schoolIdColumn, swipeTimestampColumn);

        // Add a new column for swipe type to the swipesTable
        TableColumn<Swipe, String> swipeTypeColumn = new TableColumn<>("Swipe Type");
        swipeTypeColumn.setCellValueFactory(new PropertyValueFactory<>("swipeType"));
        swipesTable.getColumns().add(swipeTypeColumn);

        // Add the swipesTable to the swipesBox
        swipesBox.getChildren().add(swipesTable);

        // Submit and Clear Filters Buttons Event Handlers
        filterSwipesButton.setOnAction(e -> {
            String schoolIdFilter = schoolIdFilterField.getText();
            LocalDate dateFilter = datePicker.getValue();
            LocalTime startTimeFilter = getTimeFromSpinners(startHourSpinner, startMinuteSpinner, startAmPmSpinner);
            LocalTime endTimeFilter = getTimeFromSpinners(endHourSpinner, endMinuteSpinner, endAmPmSpinner);
            populateSwipesTable(swipesTable, schoolIdFilter, dateFilter, startTimeFilter, endTimeFilter);
        });

        clearFiltersButton.setOnAction(e -> {
            schoolIdFilterField.clear();
            datePicker.getEditor().clear();
            startHourSpinner.getValueFactory().setValue(1);
            startMinuteSpinner.getValueFactory().setValue(0);
            startAmPmSpinner.getValueFactory().setValue("AM");
            endHourSpinner.getValueFactory().setValue(1);
            endMinuteSpinner.getValueFactory().setValue(0);
            endAmPmSpinner.getValueFactory().setValue("AM");
            populateSwipesTable(swipesTable, null, null, null, null);
        });

        // Add a handler for the "Swipe Out" button
        swipeOutButton.setOnAction(event -> {
            logStudentSwipeOut(primaryStage); // Pass the primaryStage
            primaryStage.close();
        });

        // Add searchBox, usersTable, and swipesBox to mainLayout
        mainLayout.getChildren().addAll(searchBox, usersTable, swipesBox);

        // Set up the stage
        primaryStage.setMaximized(true);
        primaryStage.setScene(new Scene(mainLayout));
        primaryStage.setTitle("Admin View");
        primaryStage.show();

        // Initialize the swipesTable
        populateSwipesTable(swipesTable, null, null, null, null);
    }


    private static LocalTime getTimeFromSpinners(Spinner<Integer> hourSpinner, Spinner<Integer> minuteSpinner, Spinner<String> amPmSpinner) {
        int hour = hourSpinner.getValue();
        int minute = minuteSpinner.getValue();
        String amPm = amPmSpinner.getValue();

        if ("PM".equals(amPm) && hour < 12) {
            hour += 12;
        } else if ("AM".equals(amPm) && hour == 12) {
            hour = 0;
        }

        return LocalTime.of(hour, minute);
    }


    private static void populateSwipesTable(
            TableView<Swipe> swipesTable,
            String schoolId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        swipesTable.getItems().clear();

        StringBuilder query = new StringBuilder("SELECT * FROM SwipeHistory");

        // Both schoolId and date are null or schoolId is empty
        if ((schoolId == null || schoolId.isEmpty()) && date == null) {
            // The default query retrieves all records from SwipeHistory
        }
        // If only date is null
        else if (date == null) {
            query.append(" WHERE SchoolId = ?");
        }
        // If only schoolId is null or empty
        else if (schoolId == null || schoolId.isEmpty()) {
            LocalDateTime startDateTime = date.atTime(startTime);
            LocalDateTime endDateTime = date.atTime(endTime);
            query.append(" WHERE SwipeTimeStamp >= '")
                    .append(startDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .append("' AND SwipeTimeStamp <= '")
                    .append(endDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .append("'");
        }
        // Both schoolId and date are provided
        else {
            LocalDateTime startDateTime = date.atTime(startTime);
            LocalDateTime endDateTime = date.atTime(endTime);
            query.append(" WHERE SchoolId = ? AND SwipeTimeStamp >= '")
                    .append(startDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .append("' AND SwipeTimeStamp <= '")
                    .append(endDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .append("'");
        }

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query.toString())) {

            if (schoolId != null && !schoolId.isEmpty()) {
                stmt.setString(1, schoolId);
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String swipeTime = rs.getTime("SwipeTimeStamp").toString();
                String retrievedSchoolId = rs.getString("SchoolId");
                String userName = rs.getString("Name");
                boolean isSwipeIn = rs.getBoolean("IsSwipeIn");
                String swipeType = isSwipeIn ? "Swipe In" : "Swipe Out";
                swipesTable.getItems().add(new Swipe(retrievedSchoolId, swipeTime, userName, swipeType));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private static void toggleUserStatus(String userName, boolean isActive, TableView<User> usersTable) {
        String query = "UPDATE Users SET Status = ? WHERE Name = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            int newStatus = isActive ? 0 : 1;
            statement.setInt(1, newStatus);
            statement.setString(2, userName);
            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("User status updated successfully.");
            } else {
                System.out.println("No rows were updated.");
            }

            // Refresh the users table
            populateUsersTable(usersTable);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }



    private static void populateUsersTable(TableView<User> usersTable) {
        usersTable.getItems().clear();
        try (Connection connection = getConnection()) {
            String query = "SELECT Name, Status FROM Users";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                String name = rs.getString("Name");
                String status = rs.getInt("Status") == 1 ? "Active" : "Suspended";
                usersTable.getItems().add(new User(name, status));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void filterUsersTable(String searchText, TableView<User> usersTable) {
        usersTable.getItems().clear();

        StringBuilder query = new StringBuilder("SELECT * FROM Users");

        if (searchText != null && !searchText.isEmpty()) {
            query.append(" WHERE Name LIKE ?");
        }

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query.toString())) {

            if (searchText != null && !searchText.isEmpty()) {
                stmt.setString(1, "%" + searchText + "%");
            }

            ResultSet rs = stmt.executeQuery();

            ObservableList<User> filteredUsers = FXCollections.observableArrayList();

            while (rs.next()) {
                String name = rs.getString("Name");
                String status = rs.getBoolean("Status") ? "Active" : "Inactive";

                User user = new User(name, status);
                filteredUsers.add(user);
            }

            usersTable.setItems(filteredUsers);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    static String getUserRole(String userId) {
        String role = "";
        try (Connection connection = getConnection()) {
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
    private static void logStudentSwipeOut(Stage primaryStage) {
        System.out.println("Swipe out button pressed.");
        try (Connection connection = getConnection()) {
            String currentStudentId = LoginScreen.currentSchoolId;
            System.out.println("Using student ID: " + currentStudentId);

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

            // Close the stage (application window)
            primaryStage.close();

            String userRole = getUserRole(String.valueOf(userId));
            System.out.println("Detected user role: " + userRole);

            if ("Student".equalsIgnoreCase(userRole)) {
                display(primaryStage); // Display StudentScreen again
            } else if ("Faculty".equalsIgnoreCase(userRole)) {
                AdminScreen.display(primaryStage); // Assuming there's an AdminScreen you haven't shown yet
            } else {
                System.out.println("Unexpected user role: " + userRole);
            }

            // Terminate the JavaFX application
            Platform.exit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }



    public static class User {
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty status = new SimpleStringProperty();

        public User(String name, String status) {
            this.name.set(name);
            this.status.set(status);
        }

        public String getName() {
            return name.get();
        }

        public String getStatus() {
            return status.get();
        }
    }

    public static class Swipe {
        private final StringProperty schoolId = new SimpleStringProperty();
        private final StringProperty swipeTimestamp = new SimpleStringProperty();
        private final StringProperty userName = new SimpleStringProperty();
        private final StringProperty swipeType = new SimpleStringProperty(); // New property for swipe type

        public Swipe(String schoolId, String swipeTimestamp, String userName, String swipeType) {
            this.schoolId.set(schoolId);
            this.swipeTimestamp.set(swipeTimestamp);
            this.userName.set(userName);
            this.swipeType.set(swipeType); // Initialize swipe type
        }

        public String getSchoolId() {
            return schoolId.get();
        }

        public String getSwipeTimestamp() {
            return swipeTimestamp.get();
        }

        public String getUserName() {
            return userName.get();
        }

        public String getSwipeType() {
            return swipeType.get();
        }
    }
}



