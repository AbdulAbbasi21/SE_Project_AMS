package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AssignmentManagementView extends VBox {

    private final User instructor;

    private TableView<String[]> table;
    private ObservableList<String[]> data = FXCollections.observableArrayList();
    private Label statusLbl;

    public AssignmentManagementView(User instructor) {
        this.instructor = instructor;
        setSpacing(20);
        setPadding(new Insets(30));
        setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        // ── Header ────────────────────────────────────────────────────────
        Text title = new Text("Assignment Management");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));

        Text sub = new Text("Upload assignments  |  Edit or retract assignments");
        sub.setFont(Font.font("System", 12));
        sub.setFill(Color.web(Main.C_MUTED));

        // ── Create form ───────────────────────────────────────────────────
        Label formLbl = new Label("Create New Assignment");
        formLbl.setStyle("-fx-text-fill: " + Main.C_TEXT + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        // Course selector
        Label courseLbl = Main.fieldLabel("COURSE");
        ComboBox<String> courseBox = new ComboBox<>();
        courseBox.setPromptText("Select course...");
        courseBox.setMaxWidth(Double.MAX_VALUE);
        courseBox.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-pref-height: 42px;");
        loadCourses(courseBox);

        // Title
        Label titleLbl = Main.fieldLabel("ASSIGNMENT TITLE");
        TextField titleField = (TextField) Main.styledField("e.g. Lab Report 1", false);

        // Instructions
        Label instrLbl = Main.fieldLabel("INSTRUCTIONS");
        TextArea instrArea = new TextArea();
        instrArea.setPromptText("Enter assignment instructions, requirements, and marking criteria...");
        instrArea.setPrefRowCount(4);
        instrArea.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-control-inner-background: #1e2f42;" +
            "-fx-prompt-text-fill: " + Main.C_MUTED + ";");

        // Deadline
        Label deadlineLbl = Main.fieldLabel("DEADLINE");
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Select deadline date");
        datePicker.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-pref-height: 42px;");

        ComboBox<String> timeBox = new ComboBox<>();
        timeBox.setPromptText("Time (HH:MM)");
        for (int h = 0; h < 24; h++)
            for (int m : new int[]{0, 30})
                timeBox.getItems().add(String.format("%02d:%02d", h, m));
        timeBox.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-pref-height: 42px;");

        HBox deadlineRow = new HBox(10, datePicker, timeBox);
        HBox.setHgrow(datePicker, Priority.ALWAYS);

        statusLbl = new Label("");
        statusLbl.setStyle("-fx-font-size: 12px;");

        Button publishBtn = new Button("📤  Publish Assignment");
        publishBtn.setMaxWidth(Double.MAX_VALUE);
        publishBtn.setStyle(
            "-fx-background-color: " + Main.C_ACCENT + "33; -fx-text-fill: " + Main.C_ACCENT + ";" +
            "-fx-border-color: " + Main.C_ACCENT + "55; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 13px; -fx-padding: 10; -fx-cursor: hand;");

        publishBtn.setOnAction(e -> {
            String course       = courseBox.getValue();
            String titleText    = titleField.getText().trim();
            String instructions = instrArea.getText().trim();
            var    date         = datePicker.getValue();
            String time         = timeBox.getValue();

            if (course == null || titleText.isEmpty() || date == null || time == null) {
                setStatus("All fields are required.", false);
                return;
            }

            String deadlineStr = date + " " + time + ":00";

            try (Connection conn = DBConnection.getConnection()) {
                // Get course id
                PreparedStatement cps = conn.prepareStatement(
                    "SELECT id FROM courses WHERE course_name = ? AND instructor_username = ?");
                cps.setString(1, course);
                cps.setString(2, instructor.getUsername());
                ResultSet crs = cps.executeQuery();
                if (!crs.next()) { setStatus("Course not found.", false); return; }
                int courseId = crs.getInt("id");

                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO assignments (course_id, title, instructions, deadline, created_by) " +
                    "VALUES (?, ?, ?, ?, ?)");
                ps.setInt(1, courseId);
                ps.setString(2, titleText);
                ps.setString(3, instructions);
                ps.setString(4, deadlineStr);
                ps.setString(5, instructor.getUsername());
                ps.executeUpdate();

                // Notify all enrolled students about new assignment
                AttendanceAlertService.notifyAssignmentPublished(
                    courseId, titleText, deadlineStr, instructor.getUsername());

                // Audit log
                PreparedStatement log = conn.prepareStatement(
                    "INSERT INTO audit_log (action_by, action_type, description) VALUES (?,?,?)");
                log.setString(1, instructor.getUsername());
                log.setString(2, "PUBLISH_ASSIGNMENT");
                log.setString(3, "Published assignment '" + titleText + "' for course: " + course);
                log.executeUpdate();

                setStatus("Assignment '" + titleText + "' published successfully.", true);
                titleField.clear(); instrArea.clear();
                datePicker.setValue(null); courseBox.setValue(null); timeBox.setValue(null);
                loadAssignments();

            } catch (Exception ex) {
                ex.printStackTrace();
                setStatus("Error: " + ex.getMessage(), false);
            }
        });

        VBox formBox = new VBox(8,
            formLbl,
            courseLbl, courseBox,
            titleLbl, titleField,
            instrLbl, instrArea,
            deadlineLbl, deadlineRow,
            publishBtn
        );
        formBox.setPadding(new Insets(20));
        formBox.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-background-radius: 12;" +
            "-fx-border-color: #2a4060; -fx-border-radius: 12;");

        // ── Assignment table ──────────────────────────────────────────────
        Label tableLbl = new Label("My Published Assignments");
        tableLbl.setStyle("-fx-text-fill: " + Main.C_TEXT + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        table = new TableView<>(data);
        applyTableTheme(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(220);

        table.getColumns().addAll(
            tcol("ID",           0, Main.C_MUTED),
            tcol("Course",       1, Main.C_ACCENT),
            tcol("Title",        2, Main.C_TEXT),
            tcol("Deadline",     3, Main.C_WARNING),
            tcol("Status",       4, Main.C_SUCCESS),
            tcol("Submissions",  5, Main.C_ACCENT2)
        );

        // ── Edit / Retract buttons ────────────────────────────────────────
        Button editBtn = actionBtn("✏️  Edit Selected", Main.C_WARNING);
        editBtn.setOnAction(e -> editSelected(courseBox));

        Button retractBtn = actionBtn("🗑  Retract Assignment", Main.C_DANGER);
        retractBtn.setOnAction(e -> retractSelected());

        Button refreshBtn = actionBtn("🔄  Refresh", Main.C_MUTED);
        refreshBtn.setOnAction(e -> loadAssignments());

        HBox actionRow = new HBox(10, editBtn, retractBtn, refreshBtn);

        loadAssignments();

        // ── Layout ────────────────────────────────────────────────────────
        HBox mainContent = new HBox(20, formBox,
            new VBox(10, tableLbl, table, actionRow, statusLbl));
        HBox.setHgrow(mainContent.getChildren().get(0), Priority.NEVER);
        HBox.setHgrow(mainContent.getChildren().get(1), Priority.ALWAYS);
        VBox.setVgrow(mainContent, Priority.ALWAYS);
        formBox.setMaxWidth(370);

        getChildren().addAll(
            new VBox(4, title, sub),
            new Separator(),
            mainContent
        );
    }

    // ── Edit selected assignment (US-08a) ─────────────────────────────────────
    private void editSelected(ComboBox<String> courseBox) {
        String[] selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select an assignment to edit.", false); return; }
        if (selected[4].equals("Retracted")) { setStatus("Cannot edit a retracted assignment.", false); return; }

        int id = Integer.parseInt(selected[0]);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Assignment");
        dialog.setHeaderText("Edit: " + selected[2]);

        ButtonType saveType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: " + Main.C_PANEL + ";");

        TextField titleField = (TextField) Main.styledField("Assignment title", false);
        titleField.setText(selected[2]);

        TextArea instrArea = new TextArea();
        instrArea.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-control-inner-background: #1e2f42;" +
            "-fx-text-fill: " + Main.C_TEXT + ";");
        instrArea.setPrefRowCount(3);

        DatePicker datePicker = new DatePicker();
        datePicker.setStyle("-fx-background-color: #1e2f42; -fx-pref-height: 40px;");

        // Load current instructions
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT instructions, deadline FROM assignments WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                instrArea.setText(rs.getString("instructions"));
                Timestamp ts = rs.getTimestamp("deadline");
                if (ts != null)
                    datePicker.setValue(ts.toLocalDateTime().toLocalDate());
            }
        } catch (Exception ex) { ex.printStackTrace(); }

        VBox content = new VBox(10,
            Main.fieldLabel("TITLE"), titleField,
            Main.fieldLabel("INSTRUCTIONS"), instrArea,
            Main.fieldLabel("NEW DEADLINE DATE"), datePicker
        );
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(result -> {
            if (result != saveType) return;
            String newTitle = titleField.getText().trim();
            String newInstr = instrArea.getText().trim();
            var    newDate  = datePicker.getValue();

            if (newTitle.isEmpty()) { setStatus("Title cannot be empty.", false); return; }

            try (Connection conn = DBConnection.getConnection()) {
                String newDeadline = newDate != null
                    ? newDate + " " + selected[3].split(" ")[1]
                    : selected[3];

                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE assignments SET title=?, instructions=?, deadline=? WHERE id=?");
                ps.setString(1, newTitle);
                ps.setString(2, newInstr);
                ps.setString(3, newDeadline);
                ps.setInt(4, id);
                ps.executeUpdate();

                PreparedStatement log = conn.prepareStatement(
                    "INSERT INTO audit_log (action_by, action_type, description) VALUES (?,?,?)");
                log.setString(1, instructor.getUsername());
                log.setString(2, "EDIT_ASSIGNMENT");
                log.setString(3, "Edited assignment #" + id + " — new title: " + newTitle);
                log.executeUpdate();

                setStatus("Assignment updated successfully.", true);
                loadAssignments();

            } catch (Exception ex) {
                ex.printStackTrace();
                setStatus("Error: " + ex.getMessage(), false);
            }
        });
    }

    // ── Retract assignment (US-08a) ───────────────────────────────────────────
    private void retractSelected() {
        String[] selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select an assignment to retract.", false); return; }
        if (selected[4].equals("Retracted")) { setStatus("Already retracted.", false); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Retract '" + selected[2] + "'? Students will no longer see this assignment.",
            ButtonType.YES, ButtonType.CANCEL);
        confirm.setHeaderText("Confirm Retraction");

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.YES) return;

            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE assignments SET is_active = 0 WHERE id = ?");
                ps.setInt(1, Integer.parseInt(selected[0]));
                ps.executeUpdate();

                PreparedStatement log = conn.prepareStatement(
                    "INSERT INTO audit_log (action_by, action_type, description) VALUES (?,?,?)");
                log.setString(1, instructor.getUsername());
                log.setString(2, "RETRACT_ASSIGNMENT");
                log.setString(3, "Retracted assignment: " + selected[2]);
                log.executeUpdate();

                setStatus("Assignment '" + selected[2] + "' retracted.", true);
                loadAssignments();

            } catch (Exception ex) {
                ex.printStackTrace();
                setStatus("Error: " + ex.getMessage(), false);
            }
        });
    }

    // ── Data loaders ──────────────────────────────────────────────────────────
    private void loadCourses(ComboBox<String> box) {
        box.getItems().clear();
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT course_name FROM courses WHERE instructor_username = ? ORDER BY course_name");
            ps.setString(1, instructor.getUsername());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) box.getItems().add(rs.getString("course_name"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadAssignments() {
        data.clear();
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT a.id, c.course_name, a.title, a.deadline, a.is_active," +
                "       (SELECT COUNT(*) FROM submissions WHERE assignment_id = a.id) AS sub_count " +
                "FROM assignments a " +
                "JOIN courses c ON a.course_id = c.id " +
                "WHERE a.created_by = ? ORDER BY a.deadline DESC");
            ps.setString(1, instructor.getUsername());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                data.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("course_name"),
                    rs.getString("title"),
                    rs.getTimestamp("deadline").toString().substring(0, 16),
                    rs.getBoolean("is_active") ? "Active" : "Retracted",
                    rs.getInt("sub_count") + " submissions"
                });
            }
            if (data.isEmpty())
                data.add(new String[]{"—", "No assignments yet", "", "", "", ""});
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Dark theme for TableView ──────────────────────────────────────────────
    private void applyTableTheme(TableView<?> t) {
        t.setStyle(
            "-fx-base: " + Main.C_PANEL + ";" +
            "-fx-control-inner-background: " + Main.C_PANEL + ";" +
            "-fx-control-inner-background-alt: #1a2840;" +
            "-fx-background-color: " + Main.C_PANEL + ";" +
            "-fx-table-cell-border-color: #2a4060;" +
            "-fx-border-color: #2a4060;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-text-fill: " + Main.C_TEXT + ";" +
            "-fx-selection-bar: #00c2cb33;" +
            "-fx-selection-bar-non-focused: #00c2cb22;");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void setStatus(String msg, boolean success) {
        statusLbl.setText(success ? "✅ " + msg : "⚠  " + msg);
        statusLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " +
            (success ? Main.C_SUCCESS : Main.C_DANGER) + ";");
    }

    private Button actionBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: " + color + "33; -fx-text-fill: " + color + ";" +
            "-fx-border-color: " + color + "55; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 12px; -fx-padding: 8 16; -fx-cursor: hand;");
        return b;
    }

    private TableColumn<String[], String> tcol(String header, int idx, String color) {
        TableColumn<String[], String> col = new TableColumn<>(header);
        col.setCellValueFactory(p ->
            new javafx.beans.property.SimpleStringProperty(p.getValue()[idx]));
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + Main.C_PANEL + ";");
                    return;
                }
                setText(item);
                String textColor;
                if (header.equals("Status"))
                    textColor = item.equals("Active") ? Main.C_SUCCESS : Main.C_DANGER;
                else
                    textColor = color;
                setStyle("-fx-text-fill: " + textColor + ";" +
                    "-fx-background-color: " + Main.C_PANEL + ";");
            }
        });
        return col;
    }
}