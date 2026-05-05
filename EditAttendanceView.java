package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.sql.*;

public class EditAttendanceView extends VBox {

    private final User instructor;
    private TableView<String[]> table;
    private ObservableList<String[]> data = FXCollections.observableArrayList();

    public EditAttendanceView(User instructor) {
        this.instructor = instructor;
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        Text title = new Text("Edit Attendance");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));

        // ── Search ────────────────────────────────────────────────────────
        Label searchLbl = Main.fieldLabel("SEARCH BY STUDENT");
        TextField searchField = (TextField) Main.styledField("Enter student username...", false);
        Button searchBtn = new Button("Search");
        searchBtn.setStyle(
            "-fx-background-color: " + Main.C_ACCENT2 + "33; -fx-text-fill: " + Main.C_ACCENT2 + ";" +
            "-fx-border-color: " + Main.C_ACCENT2 + "55; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 8 18; -fx-cursor: hand;");
        searchBtn.setOnAction(e -> loadRecords(searchField.getText().trim()));

        HBox searchRow = new HBox(10, searchField, searchBtn);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // ── Table ─────────────────────────────────────────────────────────
        table = new TableView<>(data);
        applyDarkTableStyle(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<String[], String> idCol      = tcol("ID",      0, Main.C_MUTED);
        TableColumn<String[], String> studentCol = tcol("Student", 1, Main.C_TEXT);
        TableColumn<String[], String> courseCol  = tcol("Course",  2, Main.C_TEXT);
        TableColumn<String[], String> dateCol    = tcol("Date",    3, Main.C_TEXT);

        TableColumn<String[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(
            p -> new javafx.beans.property.SimpleStringProperty(p.getValue()[4]));
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

        table.getColumns().addAll(idCol, studentCol, courseCol, dateCol, statusCol);

        // ── Edit controls ─────────────────────────────────────────────────
        Label editLbl = Main.fieldLabel("EDIT SELECTED RECORD");

        ComboBox<String> newStatusBox = new ComboBox<>();
        newStatusBox.getItems().addAll("Present", "Absent", "Late");
        newStatusBox.setPromptText("New Status");
        newStatusBox.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: " + Main.C_TEXT + ";" +
            "-fx-pref-height: 40px;");

        TextField reasonField = (TextField) Main.styledField("Reason for change (required)...", false);
        HBox.setHgrow(reasonField, Priority.ALWAYS);

        Button saveBtn = new Button("Save Edit");
        saveBtn.setStyle(
            "-fx-background-color: " + Main.C_SUCCESS + "33; -fx-text-fill: " + Main.C_SUCCESS + ";" +
            "-fx-border-color: " + Main.C_SUCCESS + "55; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 8 18; -fx-cursor: hand;");

        HBox editRow = new HBox(10, newStatusBox, reasonField, saveBtn);
        editRow.setAlignment(Pos.CENTER_LEFT);

        Label feedbackLbl = new Label("");
        feedbackLbl.setStyle("-fx-text-fill: " + Main.C_SUCCESS + "; -fx-font-size: 12px;");

        saveBtn.setOnAction(e -> {
            String[] selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                feedbackLbl.setStyle("-fx-text-fill: " + Main.C_DANGER + ";");
                feedbackLbl.setText("Select a record first.");
                return;
            }
            if (newStatusBox.getValue() == null) {
                feedbackLbl.setStyle("-fx-text-fill: " + Main.C_DANGER + ";");
                feedbackLbl.setText("Select a new status.");
                return;
            }
            if (reasonField.getText().isBlank()) {
                feedbackLbl.setStyle("-fx-text-fill: " + Main.C_DANGER + ";");
                feedbackLbl.setText("Please provide a reason for the change.");
                return;
            }

            int    id        = Integer.parseInt(selected[0]);
            String oldStatus = selected[4];
            String newStatus = newStatusBox.getValue();
            String reason    = reasonField.getText();

            Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in EditAttendanceView.java"); return; }
            try (conn) {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE attendance SET status = ? WHERE id = ?");
                ps.setString(1, newStatus);
                ps.setInt(2, id);
                ps.executeUpdate();

                PreparedStatement log = conn.prepareStatement(
                    "INSERT INTO audit_log (action_by, action_type, description) VALUES (?,?,?)");
                log.setString(1, instructor.getUsername());
                log.setString(2, "EDIT_ATTENDANCE");
                log.setString(3, "Changed record #" + id + " from " + oldStatus
                                + " to " + newStatus + ". Reason: " + reason);
                log.executeUpdate();

                feedbackLbl.setStyle("-fx-text-fill: " + Main.C_SUCCESS + ";");
                feedbackLbl.setText("✅ Record updated. Audit log saved.");
                loadRecords(selected[1]);

            } catch (Exception ex) {
                ex.printStackTrace();
                feedbackLbl.setStyle("-fx-text-fill: " + Main.C_DANGER + ";");
                feedbackLbl.setText("Error: " + ex.getMessage());
            }
        });

        loadRecords("");

        this.getChildren().addAll(
            title, new Separator(),
            searchLbl, searchRow,
            table,
            editLbl, editRow,
            feedbackLbl);
    }

    private void loadRecords(String filter) {
        data.clear();
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in EditAttendanceView.java"); return; }
            try (conn) {
            String sql =
                "SELECT a.id, a.student_username, c.course_name, a.attendance_date, a.status " +
                "FROM attendance a JOIN courses c ON a.course_id = c.id " +
                "WHERE a.marked_by = ? " +
                (filter.isEmpty() ? "" : "AND a.student_username LIKE ? ") +
                "ORDER BY a.attendance_date DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, instructor.getUsername());
            if (!filter.isEmpty()) ps.setString(2, "%" + filter + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                data.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("student_username"),
                    rs.getString("course_name"),
                    rs.getDate("attendance_date").toString(),
                    rs.getString("status")
                });
            }
            if (data.isEmpty())
                data.add(new String[]{"—", "No records found", "", "", ""});
        } catch (Exception e) { e.printStackTrace(); }
    }

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