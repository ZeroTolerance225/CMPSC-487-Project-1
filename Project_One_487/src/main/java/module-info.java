module com.example.project_one_487 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.project_one_487 to javafx.fxml;
    exports com.example.project_one_487;
}