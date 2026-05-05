package ams;

import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.scene.paint.Color;
import java.sql.*;
import java.time.LocalDate;

public class AttendanceModule extends VBox {

    private final User instructor;
    private ComboBox<String> courseBox;
    private TableView<AttendanceRow> table;
    private final ObservableList<AttendanceRow> rows = FXCollections.observableArrayList();
    private Label statusMsg;
    private Label rosterInfo;

    public AttendanceModule(User instructor) {
        this.instructor = instructor;
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        Text title = new Text("Mark Attendance");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));

        Text sub = new Text("Select a course to load enrolled students automatically.");
        sub.setFont(Font.font("System", 12));
        sub.setFill(Color.web(Main.C_MUTED));

        // Course selector
        Label courseLbl = Main.fieldLabel("SELECT COURSE");
        courseBox = new ComboBox<>();
        courseBox.setPromptText("Choose a course...");
        courseBox.setMaxWidth(280);
        courseBox.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: " + Main.C_TEXT + ";" +
            "-fx-font-size: 13px; -fx-pref-height: 40px;");
        loadCourses();
        courseBox.setOnAction(e -> loadRoster());

        Button loadBtn = actionBtn("Reload Students", Main.C_ACCENT2);
        loadBtn.setOnAction(e -> loadRoster());

        HBox courseRow = new HBox(12, courseBox, loadBtn);
        courseRow.setAlignment(Pos.CENTER_LEFT);

        rosterInfo = new Label("");
        rosterInfo.setStyle("-fx-text-fill: " + Main.C_ACCENT + "; -fx-font-size: 12px;");

        // Date picker
        Label dateLbl = Main.fieldLabel("SESSION DATE");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(200);
        datePicker.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 13px; -fx-pref-height: 40px;");

        // Bulk actions
        Label bulkLbl = Main.fieldLabel("BULK ACTIONS");
        Button markAllPresent = actionBtn("✓ Check All Present", Main.C_SUCCESS);
        Button markAllAbsent  = actionBtn("✗ Mark All Absent",   Main.C_DANGER);
        markAllPresent.setOnAction(e -> { rows.forEach(r -> r.setStatus("Present")); table.refresh(); });
        markAllAbsent.setOnAction(e  -> { rows.forEach(r -> r.setStatus("Absent"));  table.refresh(); });
        HBox bulkRow = new HBox(10, markAllPresent, markAllAbsent);

        // Table
        table = new TableView<>(rows);
        table.setEditable(true);
        applyTableTheme(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(new Label("Select a course above to load students."));

        // Name column
        TableColumn<AttendanceRow, String> nameCol = new TableColumn<>("Student Username");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentName()));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle("-fx-background-color:" + Main.C_PANEL + ";"); return; }
                setText(item);
                setStyle("-fx-text-fill:" + Main.C_TEXT + "; -fx-background-color:" + Main.C_PANEL + "; -fx-font-size:13px;");
            }
        });
        nameCol.setPrefWidth(200);

        // Status column with ComboBox
        TableColumn<AttendanceRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> combo = new ComboBox<>();
            {
                combo.getItems().addAll("Present", "Absent", "Late");
                combo.setMaxWidth(Double.MAX_VALUE);
                styleCombo(combo, "Present");
                combo.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < rows.size() && combo.getValue() != null) {
                        rows.get(idx).setStatus(combo.getValue());
                        styleCombo(combo, combo.getValue());
                    }
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:" + Main.C_PANEL + ";");
                if (empty || item == null || getIndex() < 0 || getIndex() >= rows.size()) { setGraphic(null); return; }
                combo.setValue(item.isEmpty() ? "Present" : item);
                styleCombo(combo, combo.getValue());
                setGraphic(combo);
            }
        });
        statusCol.setPrefWidth(160);

        // Note column
        TableColumn<AttendanceRow, String> noteCol = new TableColumn<>("Note (if Late) — US-01b");
        noteCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNote()));
        noteCol.setCellFactory(col -> new TableCell<>() {
            private final TextField field = new TextField();
            {
                field.setStyle(
                    "-fx-background-color:#1e2f42; -fx-text-fill:" + Main.C_TEXT + ";" +
                    "-fx-border-color:#2a4060; -fx-border-radius:6; -fx-prompt-text-fill:" + Main.C_MUTED + ";");
                field.setPromptText("Add note...");
                field.focusedProperty().addListener((obs, old, focused) -> {
                    if (!focused && getIndex() >= 0 && getIndex() < rows.size())
                        rows.get(getIndex()).setNote(field.getText());
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:" + Main.C_PANEL + ";");
                if (empty) { setGraphic(null); return; }
                field.setText(item == null ? "" : item);
                setGraphic(field);
            }
        });
        noteCol.setPrefWidth(220);

        table.getColumns().addAll(nameCol, statusCol, noteCol);

        // Submit
        Button submitBtn = new Button("Submit Attendance");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setStyle(
            "-fx-background-color: linear-gradient(to right," + Main.C_ACCENT + "," + Main.C_ACCENT2 + ");" +
            "-fx-text-fill:white; -fx-font-size:14px; -fx-font-weight:bold;" +
            "-fx-background-radius:8; -fx-pref-height:44px; -fx-cursor:hand;");
        submitBtn.setOnAction(e -> submitAttendance(datePicker.getValue()));

        statusMsg = new Label("");
        statusMsg.setStyle("-fx-font-size:12px;");

        this.getChildren().addAll(
            new VBox(4, title, sub),
            new Separator(),
            courseLbl, courseRow, rosterInfo,
            dateLbl, datePicker,
            bulkLbl, bulkRow,
            table,
            submitBtn, statusMsg
        );
    }

    private void styleCombo(ComboBox<String> combo, String value) {
        String color = switch (value == null ? "Present" : value) {
            case "Absent" -> Main.C_DANGER;
            case "Late"   -> Main.C_WARNING;
            default       -> Main.C_SUCCESS;
        };
        combo.setStyle(
            "-fx-background-color:" + color + "22; -fx-text-fill:" + color + ";" +
            "-fx-border-color:" + color + "55; -fx-border-radius:6; -fx-background-radius:6; -fx-font-size:12px;");
    }

    private void loadCourses() {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return;
            PreparedStatement ps = conn.prepareStatement(
                "SELECT course_name FROM courses WHERE instructor_username=? ORDER BY course_name");
            ps.setString(1, instructor.getUsername());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) courseBox.getItems().add(rs.getString("course_name"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadRoster() {
        rows.clear();
        String course = courseBox.getValue();
        if (course == null) return;

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return;
            PreparedStatement ps = conn.prepareStatement(
                "SELECT u.username FROM users u " +
                "JOIN enrollments en ON u.username=en.student_username " +
                "JOIN courses c ON en.course_id=c.id " +
                "WHERE c.course_name=? AND c.instructor_username=? AND u.role='Student' " +
                "ORDER BY u.username");
            ps.setString(1, course);
            ps.setString(2, instructor.getUsername());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                rows.add(new AttendanceRow(rs.getString("username"), "Present", ""));

            if (rows.isEmpty()) {
                rosterInfo.setText("⚠  No students enrolled in '" + course + "'.");
                rosterInfo.setStyle("-fx-text-fill:" + Main.C_WARNING + "; -fx-font-size:12px;");
            } else {
                rosterInfo.setText("✓  " + rows.size() + " student(s) loaded for '" + course + "'. Mark attendance below.");
                rosterInfo.setStyle("-fx-text-fill:" + Main.C_ACCENT + "; -fx-font-size:12px;");
            }
            table.refresh();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void submitAttendance(LocalDate date) {
        String course = courseBox.getValue();
        if (course == null) { setStatus("Select a course first.", false); return; }
        if (rows.isEmpty()) { setStatus("No students to submit.", false); return; }

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) { setStatus("Database connection failed.", false); return; }

            PreparedStatement cps = conn.prepareStatement(
                "SELECT id FROM courses WHERE course_name=? AND instructor_username=?");
            cps.setString(1, course); cps.setString(2, instructor.getUsername());
            ResultSet crs = cps.executeQuery();
            if (!crs.next()) { setStatus("Course not found.", false); return; }
            int courseId = crs.getInt("id");

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO attendance (student_username,course_id,status,notes,attendance_date,marked_by) VALUES(?,?,?,?,?,?)");
            for (AttendanceRow row : rows) {
                ps.setString(1, row.getStudentName());
                ps.setInt(2, courseId);
                ps.setString(3, row.getStatus().isEmpty() ? "Present" : row.getStatus());
                ps.setString(4, row.getNote());
                ps.setDate(5, java.sql.Date.valueOf(date));
                ps.setString(6, instructor.getUsername());
                ps.addBatch();
            }
            ps.executeBatch();

            PreparedStatement log = conn.prepareStatement(
                "INSERT INTO audit_log(action_by,action_type,description) VALUES(?,?,?)");
            log.setString(1, instructor.getUsername());
            log.setString(2, "MARK_ATTENDANCE");
            log.setString(3, "Marked attendance for " + course + " on " + date + " (" + rows.size() + " students)");
            log.executeUpdate();

            setStatus("✅ Attendance submitted for " + rows.size() + " students on " + date, true);

            // US-06: Check attendance % and send alerts if below 75%
            AttendanceAlertService.checkAndAlert(courseId, instructor.getUsername());

        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Error: " + e.getMessage(), false);
        }
    }

    private void applyTableTheme(TableView<?> t) {
        t.setStyle(
            "-fx-base:" + Main.C_PANEL + ";" +
            "-fx-control-inner-background:" + Main.C_PANEL + ";" +
            "-fx-control-inner-background-alt:#1a2840;" +
            "-fx-background-color:" + Main.C_PANEL + ";" +
            "-fx-table-cell-border-color:#2a4060;" +
            "-fx-border-color:#2a4060;" +
            "-fx-border-radius:10;" +
            "-fx-background-radius:10;" +
            "-fx-selection-bar:#00c2cb33;" +
            "-fx-selection-bar-non-focused:#00c2cb22;");
    }

    private void setStatus(String msg, boolean success) {
        statusMsg.setText(msg);
        statusMsg.setStyle("-fx-font-size:12px; -fx-text-fill:" + (success ? Main.C_SUCCESS : Main.C_DANGER) + ";");
    }

    private Button actionBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color:" + color + "33; -fx-text-fill:" + color + ";" +
            "-fx-border-color:" + color + "66; -fx-border-radius:8; -fx-background-radius:8;" +
            "-fx-font-size:12px; -fx-padding:8 16; -fx-cursor:hand;");
        return b;
    }

    // Row model
    public static class AttendanceRow {
        private String studentName;
        private String status;
        private String note;

        public AttendanceRow(String name, String status, String note) {
            this.studentName = name;
            this.status      = status == null ? "Present" : status;
            this.note        = note == null ? "" : note;
        }

        public String getStudentName() { return studentName; }
        public String getStatus()      { return status; }
        public String getNote()        { return note; }
        public void setStatus(String s){ this.status = s == null ? "Present" : s; }
        public void setNote(String n)  { this.note   = n == null ? "" : n; }

        public SimpleStringProperty studentNameProperty() { return new SimpleStringProperty(studentName); }
        public SimpleStringProperty statusProperty()      { return new SimpleStringProperty(status); }
        public SimpleStringProperty noteProperty()        { return new SimpleStringProperty(note); }
    }
}