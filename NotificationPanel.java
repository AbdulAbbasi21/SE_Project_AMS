package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.sql.*;

public class NotificationPanel extends VBox {

    private final User student;
    private final VBox listBox = new VBox(8);

    public NotificationPanel(User student) {
        this.student = student;
        this.setSpacing(0);
        this.setStyle(
            "-fx-background-color: " + Main.C_PANEL + ";" +
            "-fx-border-color: #2a4060; -fx-border-radius: 12; -fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, #00000088, 20, 0, 0, 4);");
        this.setPrefWidth(380);
        this.setMaxWidth(380);
        build();
    }

    private void build() {
        // ── Header ────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setPadding(new Insets(16, 16, 12, 16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
            "-fx-background-color: #1a2d40;" +
            "-fx-background-radius: 12 12 0 0;");

        Text titleTxt = new Text("🔔  Notifications");
        titleTxt.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        titleTxt.setFill(Color.web(Main.C_TEXT));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button markReadBtn = new Button("Mark all read");
        markReadBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: " + Main.C_ACCENT + ";" +
            "-fx-border-color: transparent; -fx-cursor: hand; -fx-font-size: 11px;");
        markReadBtn.setOnAction(e -> {
            AttendanceAlertService.markAllRead(student.getUsername());
            loadNotifications();
        });

        header.getChildren().addAll(titleTxt, spacer, markReadBtn);

        // ── Scrollable list ───────────────────────────────────────────────
        listBox.setPadding(new Insets(12));

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefHeight(360);
        scroll.setStyle(
            "-fx-background: " + Main.C_PANEL + ";" +
            "-fx-background-color: " + Main.C_PANEL + ";" +
            "-fx-border-color: transparent;");

        this.getChildren().addAll(header, scroll);
        loadNotifications();
    }

    public void loadNotifications() {
        listBox.getChildren().clear();
        ObservableList<String[]> notes = FXCollections.observableArrayList();

        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (conn) {
            // LEFT JOIN so notifications without course_id (grades) still show
            PreparedStatement ps = conn.prepareStatement(
                "SELECT n.id, " +
                "       ISNULL(c.course_name, '') AS course_name, " +
                "       n.message, " +
                "       ISNULL(CAST(n.attendance_pct AS NVARCHAR), '') AS attendance_pct, " +
                "       n.created_at, " +
                "       n.is_read, " +
                "       ISNULL(n.type, 'ATTENDANCE_ALERT') AS type " +
                "FROM notifications n " +
                "LEFT JOIN courses c ON n.course_id = c.id " +
                "WHERE n.student_username = ? " +
                "ORDER BY n.is_read ASC, n.created_at DESC");
            ps.setString(1, student.getUsername());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                notes.add(new String[]{
                    rs.getString("id"),
                    rs.getString("course_name"),
                    rs.getString("message"),
                    rs.getString("attendance_pct"),
                    rs.getTimestamp("created_at").toString().substring(0, 16),
                    rs.getString("is_read"),
                    rs.getString("type")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (notes.isEmpty()) {
            Label empty = new Label("✓  No new notifications");
            empty.setStyle("-fx-text-fill: " + Main.C_MUTED + "; -fx-font-size: 12px;");
            empty.setPadding(new Insets(20));
            listBox.getChildren().add(empty);
            return;
        }

        for (String[] n : notes) {
            listBox.getChildren().add(buildCard(n));
        }
    }

    private VBox buildCard(String[] n) {
        // n = [id, course_name, message, attendance_pct, created_at, is_read, type]
        boolean isUnread = "0".equals(n[5]);
        String  type     = n[6];
        String  pctStr   = n[3];

        // Pick icon and colour based on notification type
        String icon, accentColor;
        switch (type) {
            case "ASSIGNMENT" -> { icon = "📝"; accentColor = Main.C_ACCENT2; }
            case "GRADE"      -> { icon = "📊"; accentColor = Main.C_SUCCESS; }
            default           -> { icon = "⚠";  accentColor = Main.C_DANGER;  }
        }

        String borderColor = isUnread ? accentColor : "#2a4060";
        String bgColor     = isUnread ? accentColor + "11" : "#1a2d4044";

        VBox card = new VBox(6);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-radius: 10; -fx-border-width: 1;");

        // Top row: icon + title + optional badge
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Unread dot
        if (isUnread) {
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 8px;");
            topRow.getChildren().add(dot);
        }

        // Type icon + label
        String typeLabel = switch (type) {
            case "ASSIGNMENT" -> "New Assignment";
            case "GRADE"      -> "Grade Posted";
            default           -> n[1].isBlank() ? "Attendance Alert" : n[1];
        };
        Label titleLbl = new Label(icon + "  " + typeLabel);
        titleLbl.setStyle(
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        topRow.getChildren().add(titleLbl);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        topRow.getChildren().add(sp);

        // Attendance % badge — only for attendance alerts
        if (type.equals("ATTENDANCE_ALERT") && !pctStr.isBlank()) {
            try {
                double pct = Double.parseDouble(pctStr);
                String pctColor = pct >= 75 ? Main.C_SUCCESS : pct >= 65 ? Main.C_WARNING : Main.C_DANGER;
                Label pctBadge = new Label(String.format("%.0f%%", pct));
                pctBadge.setStyle(
                    "-fx-background-color: " + pctColor + "22;" +
                    "-fx-text-fill: "        + pctColor + ";" +
                    "-fx-border-color: "     + pctColor + "55;" +
                    "-fx-border-radius: 8; -fx-background-radius: 8;" +
                    "-fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: bold;");
                topRow.getChildren().add(pctBadge);
            } catch (NumberFormatException ignored) {}
        }

        // Message
        Label msgLabel = new Label(n[2]);
        msgLabel.setStyle("-fx-text-fill: " + Main.C_MUTED + "; -fx-font-size: 11px;");
        msgLabel.setWrapText(true);

        // Timestamp
        Label timeLabel = new Label(n[4]);
        timeLabel.setStyle("-fx-text-fill: #3a5a70; -fx-font-size: 10px;");

        card.getChildren().addAll(topRow, msgLabel, timeLabel);
        return card;
    }
}