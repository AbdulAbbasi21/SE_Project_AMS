package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;

public class AssignmentModule extends VBox {

    private final User instructor;
    private ComboBox<String> courseBox;
    private TextField titleField;
    private TextArea instructionsField;
    private DatePicker deadlinePicker;
    private TableView<String> assignmentTable;
    private ObservableList<String> assignments = FXCollections.observableArrayList();

    public AssignmentModule(User instructor) {
        this.instructor = instructor;
        setSpacing(20);
        setPadding(new Insets(30));
        setStyle("-fx-background-color:" + Main.C_BG + ";");
        build();
    }

    private void build() {
        Text heading = new Text("Assignments");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        heading.setFill(Color.web(Main.C_TEXT));

        courseBox = new ComboBox<>();
        courseBox.setPromptText("Select Course");
        styleCombo(courseBox);

        titleField = styledField("Assignment Title");
        instructionsField = new TextArea();
        instructionsField.setPromptText("Instructions");
        instructionsField.setStyle("-fx-control-inner-background:" + Main.C_PANEL +
                "; -fx-text-fill:white;");
        instructionsField.setPrefHeight(100);

        deadlinePicker = new DatePicker(LocalDate.now());

        Button uploadBtn = actionBtn("Publish Assignment", Main.C_ACCENT);
        uploadBtn.setOnAction(e -> uploadAssignment());

        assignmentTable = new TableView<>(assignments);
        assignmentTable.setPlaceholder(new Label("No assignments"));
        applyDarkTableStyle(assignmentTable);

        TableColumn<String, String> col = new TableColumn<>("Published Assignments");
        col.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()));
        col.setPrefWidth(500);
        assignmentTable.getColumns().add(col);

        Button retractBtn = actionBtn("Retract Selected", Main.C_DANGER);
        retractBtn.setOnAction(e -> retractAssignment());

        loadCourses();
        loadAssignments();

        getChildren().addAll(
                heading,
                courseBox,
                titleField,
                instructionsField,
                deadlinePicker,
                uploadBtn,
                assignmentTable,
                retractBtn
        );
    }

    private void uploadAssignment() {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO assignments(course_id,title,instructions,deadline,created_by) " +
                        "VALUES((SELECT id FROM courses WHERE course_name=?),?,?,?,?)"
            );
            ps.setString(1, courseBox.getValue());
            ps.setString(2, titleField.getText());
            ps.setString(3, instructionsField.getText());
            ps.setTimestamp(4, Timestamp.valueOf(deadlinePicker.getValue().atStartOfDay()));
            ps.setString(5, instructor.getUsername());
            ps.executeUpdate();
            loadAssignments();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void retractAssignment() {
        String selected = assignmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        int id = Integer.parseInt(selected.split(" - ")[0]);

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE assignments SET is_active=0 WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate();
            loadAssignments();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadAssignments() {
        assignments.clear();
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id,title FROM assignments WHERE created_by=? AND is_active=1");
            ps.setString(1, instructor.getUsername());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                assignments.add(rs.getInt("id") + " - " + rs.getString("title"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadCourses() {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT course_name FROM courses WHERE instructor_username=?");
            ps.setString(1, instructor.getUsername());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) courseBox.getItems().add(rs.getString("course_name"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color:" + Main.C_PANEL +
                "; -fx-text-fill:white;");
        return f;
    }

    private void styleCombo(ComboBox<String> c) {
        c.setStyle("-fx-background-color:" + Main.C_PANEL +
                "; -fx-text-fill:white;");
    }

    private Button actionBtn(String txt, String color) {
        Button b = new Button(txt);
        b.setStyle("-fx-background-color:" + color +
                "; -fx-text-fill:white;");
        return b;
    }

    private void applyDarkTableStyle(TableView<?> t) {
        t.setStyle("-fx-control-inner-background:" + Main.C_PANEL +
                "; -fx-background-color:" + Main.C_PANEL +
                "; -fx-text-fill:white;");
    }
}