package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.sql.*;

/**
 * US-06b — Email Notification Queue Viewer (Admin panel).
 *
 * Shows all queued and sent e-mail attendance alerts.
 * Admins can see who was notified, for which course, and the status.
 *
 * In a production deployment the email_queue table would be processed
 * by a background mailer service (e.g. JavaMail / SMTP).
 * This view provides visibility into that queue.
 */
public class EmailNotificationView extends VBox {

    private TableView<String[]> table;
    private ObservableList<String[]> data = FXCollections.observableArrayList();

    public EmailNotificationView() {
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        // ── Title ─────────────────────────────────────────────────────────
        Text title = new Text("Email Notification Queue");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));

        Text subtitle = new Text(
            "Attendance-drop alerts queued for delivery");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web(Main.C_MUTED));

        // ── Summary row ───────────────────────────────────────────────────
        HBox summary = new HBox(16);
        summary.setAlignment(Pos.CENTER_LEFT);
        summary.getChildren().addAll(
            summaryCard("Pending",  countQueued(false), Main.C_WARNING),
            summaryCard("Sent",     countQueued(true),  Main.C_SUCCESS)
        );

        // ── Table ─────────────────────────────────────────────────────────
        table = new TableView<>(data);
        applyDarkTableStyle(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<String[], String> idCol   = tcol("ID",        0, Main.C_MUTED);
        TableColumn<String[], String> userCol = tcol("Student",   1, Main.C_TEXT);
        TableColumn<String[], String> emailCol= tcol("Email",     2, Main.C_TEXT);
        TableColumn<String[], String> courseCol=tcol("Course",    3, Main.C_TEXT);
        TableColumn<String[], String> subjCol = tcol("Subject",   4, Main.C_TEXT);
        TableColumn<String[], String> queuedCol=tcol("Queued At", 5, Main.C_MUTED);

        TableColumn<String[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(
            p -> new javafx.beans.property.SimpleStringProperty(p.getValue()[6]));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + Main.C_PANEL + ";");
                    return;
                }
                boolean sent = item.equals("Sent");
                String color = sent ? Main.C_SUCCESS : Main.C_WARNING;
                setText(sent ? "✓ Sent" : "⏳ Pending");
                setStyle(
                    "-fx-text-fill: "        + color        + ";" +
                    "-fx-font-weight: bold;"                      +
                    "-fx-background-color: " + Main.C_PANEL + ";");
            }
        });

        table.getColumns().addAll(
            idCol, userCol, emailCol, courseCol, subjCol, queuedCol, statusCol);

        // ── Refresh + simulate-send buttons ──────────────────────────────
        Button refreshBtn = actionBtn("↻  Refresh", Main.C_ACCENT);
        refreshBtn.setOnAction(e -> loadData());

        Button simulateBtn = actionBtn("▶  Simulate Send (Dev)", Main.C_ACCENT2);
        simulateBtn.setOnAction(e -> simulateSend());

        HBox btnRow = new HBox(10, refreshBtn, simulateBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        Label note = new Label(
            "ℹ  In production, a background SMTP service processes Pending emails automatically.");
        note.setStyle("-fx-text-fill: " + Main.C_MUTED + "; -fx-font-size: 11px;");
        note.setWrapText(true);

        loadData();

        this.getChildren().addAll(
            title, subtitle, new Separator(),
            summary,
            btnRow,
            table,
            note);
    }

    private void loadData() {
        data.clear();
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in EmailNotificationView.java"); return ; }
            try (conn) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT eq.id, eq.recipient_username, eq.recipient_email, " +
                "       c.course_name, eq.subject, eq.queued_at, eq.is_sent " +
                "FROM email_queue eq " +
                "JOIN courses c ON eq.course_id = c.id " +
                "ORDER BY eq.is_sent ASC, eq.queued_at DESC");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("recipient_username"),
                    rs.getString("recipient_email"),
                    rs.getString("course_name"),
                    rs.getString("subject"),
                    rs.getTimestamp("queued_at").toString().substring(0, 16),
                    rs.getBoolean("is_sent") ? "Sent" : "Pending"
                });
            }
            if (data.isEmpty())
                data.add(new String[]{"—", "No email alerts queued yet", "", "", "", "", ""});
        } catch (Exception e) { e.printStackTrace(); }
    }

    /** Dev helper — marks all pending emails as sent (simulates mailer). */
    private void simulateSend() {
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in EmailNotificationView.java"); return; }
            try (conn) {
            conn.createStatement().executeUpdate(
                "UPDATE email_queue SET is_sent = 1, sent_at = GETDATE() WHERE is_sent = 0");
            new Alert(Alert.AlertType.INFORMATION,
                "✅ All pending emails marked as sent (simulation).").show();
            loadData();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String countQueued(boolean sent) {
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in EmailNotificationView.java"); return "0"; }
            try (conn) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM email_queue WHERE is_sent = ?");
            ps.setBoolean(1, sent);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? String.valueOf(rs.getInt(1)) : "0";
        } catch (Exception e) { return "0"; }
    }

    private VBox summaryCard(String label, String value, String color) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(14, 20, 14, 20));
        card.setStyle(
            "-fx-background-color: " + Main.C_PANEL + ";" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + color + "33;" +
            "-fx-border-radius: 10;");
        Text val = new Text(value);
        val.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        val.setFill(Color.web(color));
        Text lbl = new Text(label);
        lbl.setFont(Font.font("System", 11));
        lbl.setFill(Color.web(Main.C_MUTED));
        card.getChildren().addAll(val, lbl);
        return card;
    }

    private Button actionBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: " + color + "33; -fx-text-fill: " + color + ";" +
            "-fx-border-color: " + color + "66; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 12px; -fx-padding: 8 16; -fx-cursor: hand;");
        return b;
    }

    private void applyDarkTableStyle(TableView<?> t) {
        t.setStyle(
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