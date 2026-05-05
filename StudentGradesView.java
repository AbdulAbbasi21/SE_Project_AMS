package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.sql.*;

public class StudentGradesView extends VBox {

    private final User student;
    private TableView<GradeRow> table;
    private ObservableList<GradeRow> grades = FXCollections.observableArrayList();

    public StudentGradesView(User student) {
        this.student = student;
        setSpacing(20);
        setPadding(new Insets(30));
        setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        // ── Header ────────────────────────────────────────────────────────
        Text title = new Text("My Grades");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));

        Text sub = new Text("View your marks, GPA points, CGPA, and instructor feedback per assignment");
        sub.setFont(Font.font("System", 12));
        sub.setFill(Color.web(Main.C_MUTED));

        // ── Summary cards ─────────────────────────────────────────────────
        HBox summaryRow = new HBox(16);
        summaryRow.setAlignment(Pos.CENTER_LEFT);
        buildSummaryCards(summaryRow);

        // ── Table ─────────────────────────────────────────────────────────
        table = new TableView<>(grades);
        applyTableTheme(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<GradeRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(d -> d.getValue().courseProperty());
        courseCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: " + Main.C_ACCENT + "; -fx-background-color: " + Main.C_PANEL + ";");
            }
        });

        TableColumn<GradeRow, String> assignmentCol = new TableColumn<>("Assignment");
        assignmentCol.setCellValueFactory(d -> d.getValue().assignmentProperty());
        assignmentCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: " + Main.C_TEXT + "; -fx-background-color: " + Main.C_PANEL + ";");
            }
        });

        TableColumn<GradeRow, String> marksCol = new TableColumn<>("Marks");
        marksCol.setCellValueFactory(d -> d.getValue().marksProperty());
        marksCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item.equals("0") || item.equals("—") ? item : item + " / 100");
                // Colour by score
                String color;
                try {
                    int m = Integer.parseInt(item);
                    color = m >= 75 ? Main.C_SUCCESS : m >= 50 ? Main.C_WARNING : Main.C_DANGER;
                } catch (NumberFormatException e) {
                    color = Main.C_MUTED;
                }
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });


        TableColumn<GradeRow, String> gpaCol = new TableColumn<>("GPA Points");
        gpaCol.setCellValueFactory(d -> d.getValue().marksProperty());
        gpaCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.equals("—")) { setText(null); return; }
                try {
                    double points = Math.min(4.0, Math.max(0.0, Integer.parseInt(item) / 25.0));
                    setText(String.format("%.2f / 4.00", points));
                    setStyle("-fx-text-fill: " + (points >= 3.0 ? Main.C_SUCCESS : points >= 2.0 ? Main.C_WARNING : Main.C_DANGER) + "; -fx-font-weight: bold;");
                } catch (NumberFormatException e) {
                    setText("—");
                    setStyle("-fx-text-fill: " + Main.C_MUTED + ";");
                }
            }
        });

        TableColumn<GradeRow, String> feedbackCol = new TableColumn<>("Instructor Feedback");
        feedbackCol.setCellValueFactory(d -> d.getValue().feedbackProperty());
        feedbackCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item.isBlank() ? "No feedback provided" : item);
                setStyle("-fx-text-fill: " +
                    (item.isBlank() ? Main.C_MUTED : Main.C_ACCENT) + ";");
            }
        });

        TableColumn<GradeRow, String> dateCol = new TableColumn<>("Graded At");
        dateCol.setCellValueFactory(d -> d.getValue().gradedAtProperty());
        dateCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: " + Main.C_MUTED + "; -fx-background-color: " + Main.C_PANEL + ";");
            }
        });

        table.getColumns().addAll(courseCol, assignmentCol, marksCol, gpaCol, feedbackCol, dateCol);

        // ── Refresh button ────────────────────────────────────────────────
        Button refreshBtn = new Button("🔄  Refresh");
        refreshBtn.setStyle(
            "-fx-background-color: " + Main.C_ACCENT + "33; -fx-text-fill: " + Main.C_ACCENT + ";" +
            "-fx-border-color: " + Main.C_ACCENT + "55; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 12px; -fx-padding: 8 16; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> { loadGrades(); buildSummaryCards(summaryRow); });

        loadGrades();

        getChildren().addAll(
            new VBox(4, title, sub),
            new Separator(),
            summaryRow,
            table,
            refreshBtn
        );
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

    private void loadGrades() {
        grades.clear();
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT a.title, c.course_name, g.marks, g.feedback, g.graded_at " +
                "FROM grades g " +
                "JOIN submissions s ON g.submission_id = s.id " +
                "JOIN assignments a ON s.assignment_id = a.id " +
                "JOIN courses c ON a.course_id = c.id " +
                "WHERE s.student_username = ? " +
                "ORDER BY g.graded_at DESC");
            ps.setString(1, student.getUsername());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String gradedAt = rs.getTimestamp("graded_at") == null
                    ? "" : rs.getTimestamp("graded_at").toString().substring(0, 16);
                grades.add(new GradeRow(
                    rs.getString("title"),
                    rs.getString("course_name"),
                    String.valueOf(rs.getInt("marks")),
                    rs.getString("feedback"),
                    gradedAt
                ));
            }

            if (grades.isEmpty()) {
                grades.add(new GradeRow("No graded assignments yet", "", "—", "", ""));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void buildSummaryCards(HBox row) {
        row.getChildren().clear();
        try (Connection conn = DBConnection.getConnection()) {
            // Total graded
            PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(g.id) as total, AVG(CAST(g.marks AS FLOAT)) as avg_marks " +
                "FROM grades g " +
                "JOIN submissions s ON g.submission_id = s.id " +
                "WHERE s.student_username = ?");
            ps.setString(1, student.getUsername());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int    total   = rs.getInt("total");
                double avgMark = rs.getDouble("avg_marks");

                String avgColor = avgMark >= 75 ? Main.C_SUCCESS
                    : avgMark >= 50 ? Main.C_WARNING : Main.C_DANGER;

                double gpa = total == 0 ? 0.0 : Math.min(4.0, Math.max(0.0, avgMark / 25.0));

                row.getChildren().addAll(
                    summaryCard("Graded",       String.valueOf(total),               Main.C_ACCENT),
                    summaryCard("Average Marks", total == 0 ? "—" :
                        String.format("%.1f", avgMark),                              avgColor),
                    summaryCard("GPA", total == 0 ? "—" : String.format("%.2f", gpa), avgColor),
                    summaryCard("CGPA", total == 0 ? "—" : String.format("%.2f", gpa), avgColor)
                );
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private VBox summaryCard(String label, String value, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setMinWidth(130);
        card.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-background-radius: 10;" +
            "-fx-border-color: " + color + "33; -fx-border-radius: 10;");

        Text val = new Text(value);
        val.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        val.setFill(Color.web(color));

        Text lbl = new Text(label);
        lbl.setFont(Font.font("System", 11));
        lbl.setFill(Color.web(Main.C_MUTED));

        card.getChildren().addAll(val, lbl);
        return card;
    }
}