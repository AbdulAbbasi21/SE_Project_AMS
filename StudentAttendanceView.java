package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.sql.*;

public class StudentAttendanceView extends VBox {

    private final User student;

    public StudentAttendanceView(User student) {
        this.student = student;
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        Text title = new Text("My Attendance");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));

        // ── Course summary cards ───────────────────────────────────────────
        HBox courseCards = new HBox(16);
        courseCards.setAlignment(Pos.TOP_LEFT);

        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in StudentAttendanceView.java"); return; }
            try (conn) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT c.id, c.course_name FROM courses c " +
                "JOIN enrollments e ON c.id = e.course_id " +
                "WHERE e.student_username = ?");
            ps.setString(1, student.getUsername());
            ResultSet rs = ps.executeQuery();

            boolean any = false;
            while (rs.next()) {
                any = true;
                int    courseId   = rs.getInt("id");
                String courseName = rs.getString("course_name");

                int total = 0, present = 0, absent = 0, late = 0;
                PreparedStatement statPs = conn.prepareStatement(
                    "SELECT status, COUNT(*) as cnt FROM attendance " +
                    "WHERE student_username = ? AND course_id = ? GROUP BY status");
                statPs.setString(1, student.getUsername());
                statPs.setInt(2, courseId);
                ResultSet statRs = statPs.executeQuery();
                while (statRs.next()) {
                    int cnt = statRs.getInt("cnt");
                    total += cnt;
                    switch (statRs.getString("status")) {
                        case "Present" -> present = cnt;
                        case "Absent"  -> absent  = cnt;
                        case "Late"    -> late     = cnt;
                    }
                }
                double pct = total == 0 ? 0 : (double)(present + late) / total * 100;
                courseCards.getChildren().add(
                    buildCourseCard(courseName, pct, present, absent, late, total));
            }

            if (!any) {
                Label noData = new Label("You are not enrolled in any courses.");
                noData.setStyle("-fx-text-fill: " + Main.C_MUTED + "; -fx-font-size: 13px;");
                courseCards.getChildren().add(noData);
            }
        } catch (Exception e) { e.printStackTrace(); }

        // ── Detail table ──────────────────────────────────────────────────
        Text detailTitle = new Text("Attendance Records");
        detailTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        detailTitle.setFill(Color.web(Main.C_TEXT));

        TableView<String[]> table = new TableView<>();
        applyDarkTableStyle(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<String[], String> dateCol   = tcol("Date",   0, Main.C_TEXT);
        TableColumn<String[], String> courseCol = tcol("Course", 1, Main.C_TEXT);

        TableColumn<String[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(
            p -> new javafx.beans.property.SimpleStringProperty(p.getValue()[2]));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + Main.C_PANEL + ";");
                    return;
                }
                setText(item);
                String color = switch (item) {
                    case "Present" -> Main.C_SUCCESS;
                    case "Absent"  -> Main.C_DANGER;
                    case "Late"    -> Main.C_WARNING;
                    default        -> Main.C_MUTED;
                };
                setStyle(
                    "-fx-text-fill: "        + color        + ";" +
                    "-fx-font-weight: bold;"                      +
                    "-fx-background-color: " + Main.C_PANEL + ";");
            }
        });

        TableColumn<String[], String> noteCol = tcol("Note", 3, Main.C_MUTED);
        table.getColumns().addAll(dateCol, courseCol, statusCol, noteCol);

        ObservableList<String[]> data = FXCollections.observableArrayList();
        Connection conn1 = DBConnection.getConnection();
            if (conn1 == null) { System.err.println("DB connection null in StudentAttendanceView.java"); return; }
            try (conn1) {
            PreparedStatement ps = conn1.prepareStatement(
                "SELECT a.attendance_date, c.course_name, a.status, a.notes " +
                "FROM attendance a JOIN courses c ON a.course_id = c.id " +
                "WHERE a.student_username = ? ORDER BY a.attendance_date DESC");
            ps.setString(1, student.getUsername());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new String[]{
                    rs.getDate("attendance_date").toString(),
                    rs.getString("course_name"),
                    rs.getString("status"),
                    rs.getString("notes") == null ? "" : rs.getString("notes")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (data.isEmpty()) data.add(new String[]{"No records yet", "", "", ""});
        table.setItems(data);

        this.getChildren().addAll(
            title, new Separator(),
            courseCards, new Separator(),
            detailTitle, table);
    }

    private VBox buildCourseCard(String name, double pct, int p, int ab, int l, int total) {
        String riskColor, riskLabel;
        if      (pct >= 75) { riskColor = Main.C_SUCCESS; riskLabel = "✓ Safe"; }
        else if (pct >= 65) { riskColor = Main.C_WARNING; riskLabel = "⚠ Warning"; }
        else                { riskColor = Main.C_DANGER;  riskLabel = "✗ Danger"; }

        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        card.setMinWidth(180);
        card.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-background-radius: 12;" +
            "-fx-border-color: " + riskColor + "44; -fx-border-radius: 12; -fx-border-width: 1;");

        Label courseName = new Label(name);
        courseName.setStyle(
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        courseName.setWrapText(true);

        Text pctTxt = new Text(String.format("%.0f%%", pct));
        pctTxt.setFont(Font.font("Georgia", FontWeight.BOLD, 32));
        pctTxt.setFill(Color.web(riskColor));

        Label riskBadge = new Label(riskLabel);
        riskBadge.setStyle(
            "-fx-background-color: " + riskColor + "22; -fx-text-fill: " + riskColor + ";" +
            "-fx-border-color: " + riskColor + "55; -fx-border-radius: 10; -fx-background-radius: 10;" +
            "-fx-padding: 2 10; -fx-font-size: 11px;");

        Label stats = new Label("P:" + p + "  A:" + ab + "  L:" + l + "  Total:" + total);
        stats.setStyle("-fx-text-fill: " + Main.C_MUTED + "; -fx-font-size: 11px;");

        card.getChildren().addAll(courseName, pctTxt, riskBadge, stats);
        return card;
    }

    /** Forces the TableView into a full dark mode via modena CSS variable overrides. */
    private void applyDarkTableStyle(TableView<?> table) {
        table.setStyle(
            "-fx-base:                         #162030;" +
            "-fx-control-inner-background:     " + Main.C_PANEL + ";" +
            "-fx-control-inner-background-alt: #1a2840;" +
            "-fx-background-color:             " + Main.C_PANEL + ";" +
            "-fx-table-cell-border-color:      #2a4060;" +
            "-fx-border-color:                 #2a4060;" +
            "-fx-border-radius:                10;" +
            "-fx-background-radius:            10;");
    }

    /** Column whose cells set BOTH background and text colour to stay visible. */
    private TableColumn<String[], String> tcol(String header, int idx, String textColor) {
        TableColumn<String[], String> col = new TableColumn<>(header);
        col.setCellValueFactory(
            p -> new javafx.beans.property.SimpleStringProperty(p.getValue()[idx]));
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + Main.C_PANEL + ";");
                    return;
                }
                setText(item);
                setStyle(
                    "-fx-text-fill:        " + textColor    + ";" +
                    "-fx-background-color: " + Main.C_PANEL + ";");
            }
        });
        return col;
    }
}