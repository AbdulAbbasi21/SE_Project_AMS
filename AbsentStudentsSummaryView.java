package ams;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.sql.*;

public class AbsentStudentsSummaryView extends VBox {

    private final User instructor;
    private final ObservableList<AbsentRow> rows = FXCollections.observableArrayList();
    private TableView<AbsentRow> table;
    private Label statusLbl;

    public AbsentStudentsSummaryView(User instructor) {
        this.instructor = instructor;

        setSpacing(20);
        setPadding(new Insets(30));
        setStyle("-fx-background-color: " + Main.C_BG + ";");

        build();
        loadRows();
    }

    private void build() {

        Text title = new Text("Frequently Absent Students");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        title.setFill(Color.web(Main.C_TEXT));

        Text sub = new Text("Students with repeated absences or low attendance");
        sub.setFont(Font.font("System", 13));
        sub.setFill(Color.web(Main.C_MUTED));

        VBox header = new VBox(6, title, sub);

        // TABLE
        table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-control-inner-background: " + Main.C_PANEL + ";" +
                "-fx-control-inner-background-alt: #16273a;" +
                "-fx-table-cell-border-color: #2a4060;"
        );

        VBox.setVgrow(table, Priority.ALWAYS);

        table.getColumns().addAll(
                col("Student", "student"),
                col("Course", "course"),
                col("Absent", "absent"),
                col("Total", "total"),
                col("Attendance %", "pct")
        );

        // BUTTON
        Button refresh = new Button("Refresh Summary");
        refresh.setStyle(
                "-fx-background-color: " + Main.C_ACCENT + "22;" +
                "-fx-text-fill: " + Main.C_ACCENT + ";" +
                "-fx-border-color: " + Main.C_ACCENT + "55;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 8 18;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
        );
        refresh.setOnAction(e -> loadRows());

        statusLbl = new Label("");

        HBox actions = new HBox(12, refresh, statusLbl);
        actions.setAlignment(Pos.CENTER_LEFT);

        // CARD
        VBox card = new VBox(14, table, actions);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: " + Main.C_PANEL + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #2a4060;" +
                "-fx-border-radius: 14;"
        );

        getChildren().addAll(header, new Separator(), card);
    }

    private TableColumn<AbsentRow, String> col(String title, String prop) {

        TableColumn<AbsentRow, String> c = new TableColumn<>(title);

        c.setCellValueFactory(d -> switch (prop) {
            case "student" -> d.getValue().student;
            case "course" -> d.getValue().course;
            case "absent" -> d.getValue().absent;
            case "total" -> d.getValue().total;
            case "pct" -> d.getValue().pct;
            default -> new SimpleStringProperty("");
        });

        c.setCellFactory(x -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                setText(empty || item == null ? null : item);

                setStyle(
                        "-fx-text-fill: " +
                        (prop.equals("pct") ? Main.C_WARNING : Main.C_TEXT) + ";" +
                        "-fx-background-color: transparent;" +
                        "-fx-padding: 10 6;"
                );
            }
        });

        return c;
    }

    private void loadRows() {
        rows.clear();

        String sql =
                "SELECT a.student_username, c.course_name, " +
                "SUM(CASE WHEN a.status='Absent' THEN 1 ELSE 0 END) absent_count, COUNT(*) total_count, " +
                "SUM(CASE WHEN a.status='Present' THEN 1 ELSE 0 END)*100.0/COUNT(*) pct " +
                "FROM attendance a JOIN courses c ON a.course_id=c.id " +
                "WHERE c.instructor_username=? " +
                "GROUP BY a.student_username, c.course_name " +
                "HAVING SUM(CASE WHEN a.status='Absent' THEN 1 ELSE 0 END) >= 2 " +
                "OR SUM(CASE WHEN a.status='Present' THEN 1 ELSE 0 END)*100.0/COUNT(*) < 75 " +
                "ORDER BY absent_count DESC, pct ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, instructor.getUsername());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                rows.add(new AbsentRow(
                        rs.getString("student_username"),
                        rs.getString("course_name"),
                        String.valueOf(rs.getInt("absent_count")),
                        String.valueOf(rs.getInt("total_count")),
                        String.format("%.1f%%", rs.getDouble("pct"))
                ));
            }

            if (rows.isEmpty()) {
                statusLbl.setText("No frequently absent students found.");
                statusLbl.setStyle(badgeStyle(Main.C_SUCCESS));
            } else {
                statusLbl.setText(rows.size() + " record(s) found");
                statusLbl.setStyle(badgeStyle(Main.C_WARNING));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLbl.setText("Error loading data");
            statusLbl.setStyle(badgeStyle(Main.C_DANGER));
        }
    }

    private String badgeStyle(String color) {
        return "-fx-background-color: " + color + "22;" +
               "-fx-text-fill: " + color + ";" +
               "-fx-border-color: " + color + "55;" +
               "-fx-border-radius: 8;" +
               "-fx-background-radius: 8;" +
               "-fx-padding: 4 10;" +
               "-fx-font-size: 11px;";
    }

    public static class AbsentRow {
        SimpleStringProperty student, course, absent, total, pct;

        AbsentRow(String s, String c, String a, String t, String p) {
            student = new SimpleStringProperty(s);
            course = new SimpleStringProperty(c);
            absent = new SimpleStringProperty(a);
            total = new SimpleStringProperty(t);
            pct = new SimpleStringProperty(p);
        }
    }
}