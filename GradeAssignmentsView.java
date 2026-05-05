package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.io.File;
import java.sql.*;

public class GradeAssignmentsView extends VBox {

    private final User instructor;

    private ComboBox<String> assignmentBox;
    private TableView<SubmissionRow> table;
    private ObservableList<SubmissionRow> rows = FXCollections.observableArrayList();
    private Label statusLbl;

    // ── Constructor: NO HostServices needed ───────────────────────────────────
    public GradeAssignmentsView(User instructor) {
        this.instructor = instructor;
        setSpacing(20);
        setPadding(new Insets(30));
        setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        // ── Header ────────────────────────────────────────────────────────
        Text title = new Text("Grade Assignments");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));

        Text sub = new Text("Review submissions and enter marks with feedback");
        sub.setFont(Font.font("System", 12));
        sub.setFill(Color.web(Main.C_MUTED));

        // ── Assignment selector ───────────────────────────────────────────
        Label assignLbl = Main.fieldLabel("SELECT ASSIGNMENT");
        assignmentBox = new ComboBox<>();
        assignmentBox.setPromptText("Choose an assignment...");
        assignmentBox.setMaxWidth(400);
        assignmentBox.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: " + Main.C_TEXT + ";" +
            "-fx-pref-height: 40px;");
        assignmentBox.setOnAction(e -> loadSubmissions());
        loadAssignments();

        // ── Submissions table ─────────────────────────────────────────────
        table = new TableView<>(rows);
        table.setStyle(
            "-fx-base: " + Main.C_PANEL + ";" +
            "-fx-control-inner-background: " + Main.C_PANEL + ";" +
            "-fx-control-inner-background-alt: #1a2840;" +
            "-fx-background-color: " + Main.C_PANEL + ";" +
            "-fx-table-cell-border-color: #2a4060;" +
            "-fx-border-color: #2a4060;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-selection-bar: #00c2cb33;" +
            "-fx-selection-bar-non-focused: #00c2cb22;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<SubmissionRow, String> studentCol = tcol("Student",      "student",    Main.C_TEXT);
        TableColumn<SubmissionRow, String> fileCol    = tcol("File",         "file",       Main.C_MUTED);
        TableColumn<SubmissionRow, String> timeCol    = tcol("Submitted At", "submittedAt",Main.C_MUTED);
        TableColumn<SubmissionRow, String> marksCol   = tcol("Marks",        "marks",      Main.C_WARNING);
        TableColumn<SubmissionRow, String> fbCol      = tcol("Feedback",     "feedback",   Main.C_ACCENT);

        // Colour-code marks cell
        marksCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + Main.C_PANEL + ";");
                    return;
                }
                setText(item);
                String color = item.equals("Not graded") ? Main.C_MUTED : Main.C_SUCCESS;
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;" +
                    "-fx-background-color: " + Main.C_PANEL + ";");
            }
        });

        table.getColumns().addAll(studentCol, fileCol, timeCol, marksCol, fbCol);

        // ── Action buttons ────────────────────────────────────────────────
        Button openBtn = actionBtn("📂  Open Submission", Main.C_ACCENT2);
        openBtn.setOnAction(e -> openSubmission());

        Button gradeBtn = actionBtn("✏️  Grade Selected", Main.C_SUCCESS);
        gradeBtn.setOnAction(e -> gradeSelected());

        Button refreshBtn = actionBtn("🔄  Refresh", Main.C_MUTED);
        refreshBtn.setOnAction(e -> loadSubmissions());

        HBox btnRow = new HBox(12, openBtn, gradeBtn, refreshBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        statusLbl = new Label("");
        statusLbl.setStyle("-fx-font-size: 12px;");

        getChildren().addAll(
            new VBox(4, title, sub),
            new Separator(),
            assignLbl, assignmentBox,
            table,
            btnRow,
            statusLbl
        );
    }

    // ── Load instructor's assignments ─────────────────────────────────────────
    private void loadAssignments() {
        assignmentBox.getItems().clear();
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT a.id, a.title, c.course_name FROM assignments a " +
                "JOIN courses c ON a.course_id = c.id " +
                "WHERE a.created_by = ? AND a.is_active = 1 ORDER BY a.title");
            ps.setString(1, instructor.getUsername());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                assignmentBox.getItems().add(
                    rs.getInt("id") + " | " + rs.getString("course_name") + " — " + rs.getString("title")
                );
            }
            if (assignmentBox.getItems().isEmpty())
                assignmentBox.getItems().add("No active assignments found");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ── Load submissions for selected assignment ───────────────────────────────
    private void loadSubmissions() {
        rows.clear();
        String selected = assignmentBox.getValue();
        if (selected == null || selected.equals("No active assignments found")) return;

        int assignmentId = Integer.parseInt(selected.split(" \\| ")[0]);

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT s.id, s.student_username, s.file_path, s.submitted_at, " +
                "       g.marks, g.feedback " +
                "FROM submissions s " +
                "LEFT JOIN grades g ON g.submission_id = s.id " +
                "WHERE s.assignment_id = ? ORDER BY s.submitted_at DESC");
            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String marks = rs.getObject("marks") == null
                    ? "Not graded"
                    : String.valueOf(rs.getInt("marks"));
                rows.add(new SubmissionRow(
                    rs.getInt("id"),
                    rs.getString("student_username"),
                    rs.getString("file_path"),
                    marks,
                    rs.getString("feedback"),
                    rs.getTimestamp("submitted_at") == null ? "" : rs.getTimestamp("submitted_at").toString()
                ));
            }

            if (rows.isEmpty())
                setStatus("No submissions yet for this assignment.", false);
            else
                setStatus(rows.size() + " submission(s) loaded.", true);

        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Error loading submissions: " + ex.getMessage(), false);
        }
    }

    // ── Open the submitted file ───────────────────────────────────────────────
    private void openSubmission() {
        SubmissionRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            setStatus("Select a submission first.", false);
            return;
        }

        String filePath = row.fileProperty().get();
        if (filePath == null || filePath.isBlank()) {
            setStatus("No file path recorded for this submission.", false);
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            setStatus("File not found at: " + filePath, false);
            return;
        }

        try {
            // Use ProcessBuilder — works on Windows without module restrictions
            new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath())
                .start();
            setStatus("Opening: " + file.getName(), true);
        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Cannot open file: " + ex.getMessage(), false);
        }
    }

    // ── Grade dialog ──────────────────────────────────────────────────────────
    private void gradeSelected() {
        SubmissionRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            setStatus("Select a submission to grade.", false);
            return;
        }

        // Custom dialog with marks + feedback
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Grade Submission");
        dialog.setHeaderText("Grading: " + row.studentProperty().get());

        ButtonType saveType = new ButtonType("Save Grade", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle(
            "-fx-background-color: " + Main.C_PANEL + ";");

        TextField marksField = (TextField) Main.styledField("Enter marks (0-100)", false);
        marksField.setText(row.marksProperty().get().equals("Not graded") ? "" : row.marksProperty().get());

        TextArea feedbackArea = new TextArea(row.feedbackProperty().get());
        feedbackArea.setPromptText("Enter feedback for the student...");
        feedbackArea.setPrefRowCount(3);
        feedbackArea.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-control-inner-background: #1e2f42;");

        VBox content = new VBox(10,
            Main.fieldLabel("MARKS (out of 100)"), marksField,
            Main.fieldLabel("FEEDBACK"), feedbackArea
        );
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(result -> {
            if (result != saveType) return;

            String marksText = marksField.getText().trim();
            String feedback  = feedbackArea.getText().trim();

            if (marksText.isEmpty()) {
                setStatus("Marks cannot be empty.", false);
                return;
            }

            int marks;
            try {
                marks = Integer.parseInt(marksText);
                if (marks < 0 || marks > 100) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                setStatus("Marks must be a number between 0 and 100.", false);
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                // Check if grade already exists
                PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM grades WHERE submission_id = ?");
                check.setInt(1, row.getId());
                ResultSet rs = check.executeQuery();
                rs.next();

                if (rs.getInt(1) > 0) {
                    // Update existing grade
                    PreparedStatement update = conn.prepareStatement(
                        "UPDATE grades SET marks=?, feedback=?, graded_by=?, graded_at=GETDATE() " +
                        "WHERE submission_id=?");
                    update.setInt(1, marks);
                    update.setString(2, feedback);
                    update.setString(3, instructor.getUsername());
                    update.setInt(4, row.getId());
                    update.executeUpdate();
                } else {
                    // Insert new grade
                    PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO grades (submission_id, marks, feedback, graded_by) VALUES (?,?,?,?)");
                    insert.setInt(1, row.getId());
                    insert.setInt(2, marks);
                    insert.setString(3, feedback);
                    insert.setString(4, instructor.getUsername());
                    insert.executeUpdate();
                }

                // Audit log
                PreparedStatement log = conn.prepareStatement(
                    "INSERT INTO audit_log (action_by, action_type, description) VALUES (?,?,?)");
                log.setString(1, instructor.getUsername());
                log.setString(2, "GRADE_SUBMISSION");
                log.setString(3, "Graded submission #" + row.getId() +
                    " for " + row.studentProperty().get() + ": " + marks + "/100");
                log.executeUpdate();

                // Update row display
                row.marksProperty().set(String.valueOf(marks));
                row.feedbackProperty().set(feedback);
                table.refresh();

                // Notify student their assignment has been graded
                // Get assignment title for notification
                try (Connection conn2 = DBConnection.getConnection()) {
                    PreparedStatement tp = conn2.prepareStatement(
                        "SELECT a.title FROM assignments a " +
                        "JOIN submissions s ON s.assignment_id = a.id " +
                        "WHERE s.id = ?");
                    tp.setInt(1, row.getId());
                    ResultSet tr = tp.executeQuery();
                    if (tr.next()) {
                        AttendanceAlertService.notifyGradePosted(
                            row.studentProperty().get(),
                            tr.getString("title"), marks, feedback);
                    }
                }

                setStatus("✅ Grade saved for " + row.studentProperty().get(), true);

            } catch (Exception ex) {
                ex.printStackTrace();
                setStatus("Error saving grade: " + ex.getMessage(), false);
            }
        });
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

    private TableColumn<SubmissionRow, String> tcol(String header, String prop, String color) {
        TableColumn<SubmissionRow, String> col = new TableColumn<>(header);
        col.setCellValueFactory(data -> {
            return switch (prop) {
                case "student"     -> data.getValue().studentProperty();
                case "file"        -> new javafx.beans.property.SimpleStringProperty(
                                        new java.io.File(data.getValue().fileProperty().get()).getName());
                case "submittedAt" -> data.getValue().submittedAtProperty();
                case "marks"       -> data.getValue().marksProperty();
                case "feedback"    -> data.getValue().feedbackProperty();
                default            -> new javafx.beans.property.SimpleStringProperty("");
            };
        });
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + Main.C_PANEL + ";");
                    return;
                }
                setText(item);
                setStyle("-fx-text-fill: " + color + ";" +
                    "-fx-background-color: " + Main.C_PANEL + ";");
            }
        });
        return col;
    }
}