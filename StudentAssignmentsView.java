package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;

public class StudentAssignmentsView extends VBox {

    private final User student;
    private TableView<AssignmentRow> table;
    private ObservableList<AssignmentRow> assignments = FXCollections.observableArrayList();
    private Label statusLbl;

    // Folder where submissions are saved
    private static final String UPLOAD_DIR = "C:\\ams_uploads\\";

    public StudentAssignmentsView(User student) {
        this.student = student;
        setSpacing(20);
        setPadding(new Insets(30));
        setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        // ── Header ────────────────────────────────────────────────────────
        Text title = new Text("My Assignments");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));

        Text sub = new Text("Submit your assignments before the deadline");
        sub.setFont(Font.font("System", 12));
        sub.setFill(Color.web(Main.C_MUTED));

        // ── Table ─────────────────────────────────────────────────────────
        table = new TableView<>(assignments);
        applyTableTheme(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<AssignmentRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(d -> d.getValue().courseProperty());
        courseCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: " + Main.C_ACCENT + "; -fx-background-color: " + Main.C_PANEL + ";");
            }
        });

        TableColumn<AssignmentRow, String> titleCol = new TableColumn<>("Assignment");
        titleCol.setCellValueFactory(d -> d.getValue().titleProperty());
        titleCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: " + Main.C_TEXT + "; -fx-background-color: " + Main.C_PANEL + ";");
            }
        });

        TableColumn<AssignmentRow, String> deadlineCol = new TableColumn<>("Deadline");
        deadlineCol.setCellValueFactory(d -> d.getValue().deadlineProperty());
        deadlineCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                // Red if past deadline
                setStyle("-fx-text-fill: " +
                    (item.compareTo(LocalDateTime.now().toString().substring(0, 16)) < 0
                        ? Main.C_DANGER : Main.C_WARNING) + ";");
            }
        });

        TableColumn<AssignmentRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> d.getValue().statusProperty());
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                setStyle("-fx-text-fill: " +
                    (item.equals("Submitted") ? Main.C_SUCCESS : Main.C_WARNING) +
                    "; -fx-font-weight: bold;");
            }
        });

        table.getColumns().addAll(courseCol, titleCol, deadlineCol, statusCol);

        // ── Buttons ───────────────────────────────────────────────────────
        Button submitBtn = actionBtn("📎  Submit Assignment", Main.C_ACCENT);
        submitBtn.setOnAction(e -> submitAssignment());

        Button refreshBtn = actionBtn("🔄  Refresh", Main.C_MUTED);
        refreshBtn.setOnAction(e -> loadAssignments());

        HBox btnRow = new HBox(12, submitBtn, refreshBtn);

        statusLbl = new Label("");
        statusLbl.setStyle("-fx-font-size: 12px;");

        // Instructions label
        Label instrLbl = new Label(
            "Select an assignment from the table and click 'Submit Assignment' to upload your file.");
        instrLbl.setStyle("-fx-text-fill: " + Main.C_MUTED + "; -fx-font-size: 11px;");
        instrLbl.setWrapText(true);

        loadAssignments();

        getChildren().addAll(
            new VBox(4, title, sub),
            new Separator(),
            instrLbl,
            table,
            btnRow,
            statusLbl
        );
    }

    // ── Load assignments for enrolled courses ─────────────────────────────────
    private void loadAssignments() {
        assignments.clear();
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT a.id, a.title, a.deadline, c.course_name " +
                "FROM assignments a " +
                "JOIN courses c ON a.course_id = c.id " +
                "JOIN enrollments e ON e.course_id = c.id " +
                "WHERE e.student_username = ? AND a.is_active = 1 " +
                "ORDER BY a.deadline ASC");
            ps.setString(1, student.getUsername());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int    id         = rs.getInt("id");
                String titleStr   = rs.getString("title");
                String deadline   = rs.getTimestamp("deadline").toString().substring(0, 16);
                String courseName = rs.getString("course_name");
                boolean submitted = checkSubmission(conn, id);

                assignments.add(new AssignmentRow(
                    id, titleStr,
                    submitted ? "Submitted" : "Pending",
                    deadline, courseName
                ));
            }

            if (assignments.isEmpty()) {
                setStatus("No active assignments found for your courses.", false);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Error loading assignments: " + ex.getMessage(), false);
        }
    }

    private boolean checkSubmission(Connection conn, int assignmentId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT COUNT(*) FROM submissions WHERE assignment_id=? AND student_username=?");
        ps.setInt(1, assignmentId);
        ps.setString(2, student.getUsername());
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }

    // ── Submit assignment ─────────────────────────────────────────────────────
    private void submitAssignment() {
        AssignmentRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            setStatus("Select an assignment first.", false);
            return;
        }

        if (row.getStatus().equals("Submitted")) {
            setStatus("You have already submitted this assignment.", false);
            return;
        }

        // Deadline check
        String deadline = row.deadlineProperty().get();
        String now      = LocalDateTime.now().toString().substring(0, 16);
        if (now.compareTo(deadline) > 0) {
            setStatus("Deadline has passed for this assignment (" + deadline + ").", false);
            return;
        }

        // File chooser
        FileChooser fc = new FileChooser();
        fc.setTitle("Select your submission file");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt", "*.zip"));
        File file = fc.showOpenDialog(getScene().getWindow());
        if (file == null) return;

        try {
            // Create upload directory if it doesn't exist
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            // Save file as: student_assignmentId_originalName
            String destName = student.getUsername() + "_" + row.getId() + "_" + file.getName();
            Path destPath   = Paths.get(UPLOAD_DIR, destName);
            Files.copy(file.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO submissions (assignment_id, student_username, file_path) VALUES (?,?,?)");
                ps.setInt(1, row.getId());
                ps.setString(2, student.getUsername());
                ps.setString(3, destPath.toString());
                ps.executeUpdate();

                // Audit log
                PreparedStatement log = conn.prepareStatement(
                    "INSERT INTO audit_log (action_by, action_type, description) VALUES (?,?,?)");
                log.setString(1, student.getUsername());
                log.setString(2, "SUBMIT_ASSIGNMENT");
                log.setString(3, "Submitted assignment #" + row.getId() +
                    " '" + row.titleProperty().get() + "' — file: " + file.getName());
                log.executeUpdate();
            }

            setStatus("✅ Submitted successfully: " + file.getName(), true);
            loadAssignments();

        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Error submitting: " + ex.getMessage(), false);
        }
    }

    private String getStatus(AssignmentRow row) { return row.statusProperty().get(); }

    private void setStatus(String msg, boolean success) {
        statusLbl.setText(success ? "✅ " + msg : "⚠  " + msg);
        statusLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " +
            (success ? Main.C_SUCCESS : Main.C_DANGER) + ";");
    }

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

    private Button actionBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: " + color + "33; -fx-text-fill: " + color + ";" +
            "-fx-border-color: " + color + "55; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 12px; -fx-padding: 8 16; -fx-cursor: hand;");
        return b;
    }
}