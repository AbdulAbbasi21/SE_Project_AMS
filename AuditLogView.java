package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.sql.*;

public class AuditLogView extends VBox {

    public AuditLogView() {
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        Text title = new Text("Audit Log");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));

        TableView<String[]> table = new TableView<>();

        /*
         * ROOT FIX:
         * JavaFX modena.css sets TableCell background to white by default.
         * Setting -fx-text-fill alone on the cell is NOT enough — the white
         * cell background overrides everything and light text becomes invisible.
         *
         * The correct approach is TWO things:
         *   1. Set -fx-control-inner-background on the TableView to drive the
         *      derived colour lookups used by modena for rows/cells.
         *   2. Set BOTH -fx-background-color AND -fx-text-fill inside every
         *      cell's updateItem() so neither can be overridden by the stylesheet.
         */
        table.setStyle(
            "-fx-base:                       #162030;" +
            "-fx-control-inner-background:   " + Main.C_PANEL + ";" +
            "-fx-control-inner-background-alt: #1a2840;" +
            "-fx-background-color:           " + Main.C_PANEL + ";" +
            "-fx-table-cell-border-color:    #2a4060;" +
            "-fx-border-color:               #2a4060;" +
            "-fx-border-radius:              10;" +
            "-fx-background-radius:          10;");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<String[], String> timeCol = tcol("Time",      0, Main.C_TEXT);
        TableColumn<String[], String> userCol = tcol("Action By", 1, Main.C_TEXT);

        // Action Type — colour coded
        TableColumn<String[], String> typeCol = new TableColumn<>("Action Type");
        typeCol.setCellValueFactory(
            p -> new javafx.beans.property.SimpleStringProperty(p.getValue()[2]));
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + Main.C_PANEL + ";");
                    return;
                }
                setText(item);
                String color = switch (item) {
                    case "LOGIN"           -> Main.C_SUCCESS;
                    case "MARK_ATTENDANCE" -> Main.C_ACCENT;
                    case "EDIT_ATTENDANCE" -> Main.C_WARNING;
                    case "ADD_USER"        -> Main.C_ACCENT2;
                    default                -> Main.C_MUTED;
                };
                setStyle(
                    "-fx-text-fill: "        + color         + ";" +
                    "-fx-font-weight: bold;"                       +
                    "-fx-background-color: " + Main.C_PANEL  + ";");
            }
        });

        TableColumn<String[], String> descCol = tcol("Description", 3, Main.C_TEXT);

        table.getColumns().addAll(timeCol, userCol, typeCol, descCol);

        ObservableList<String[]> data = FXCollections.observableArrayList();
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in AuditLogView.java"); return; }
            try (conn) {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT action_time, action_by, action_type, description " +
                "FROM audit_log ORDER BY action_time DESC");
            while (rs.next()) {
                data.add(new String[]{
                    rs.getTimestamp("action_time").toString(),
                    rs.getString("action_by"),
                    rs.getString("action_type"),
                    rs.getString("description")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (data.isEmpty())
            data.add(new String[]{"—", "No log entries yet", "", ""});
        table.setItems(data);

        this.getChildren().addAll(title, new Separator(), table);
    }

    /**
     * Builds a column whose cells set BOTH -fx-background-color and
     * -fx-text-fill so JavaFX's default white-cell background cannot
     * make light-coloured text invisible.
     */
    private TableColumn<String[], String> tcol(String header, int idx, String textColor) {
        TableColumn<String[], String> col = new TableColumn<>(header);
        col.setCellValueFactory(
            p -> new javafx.beans.property.SimpleStringProperty(p.getValue()[idx]));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + Main.C_PANEL + ";");
                    return;
                }
                setText(item);
                setStyle(
                    "-fx-text-fill:        " + textColor     + ";" +
                    "-fx-background-color: " + Main.C_PANEL  + ";");
            }
        });
        return col;
    }
}