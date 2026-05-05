package ams;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import java.sql.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

public class AttendanceCalendarView extends VBox {

    private final User student;
    private YearMonth currentMonth;
    private ComboBox<String> courseBox;
    private GridPane calGrid;
    private Map<LocalDate, String> attendanceMap = new HashMap<>();

    public AttendanceCalendarView(User student) {
        this.student      = student;
        this.currentMonth = YearMonth.now();
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        Text title = new Text("Calendar View");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));
        // REMOVED: subtitle with US story reference

        // Course selector
        Label courseLbl = Main.fieldLabel("SELECT COURSE");
        courseBox = new ComboBox<>();
        courseBox.setPromptText("Choose course...");
        courseBox.setMaxWidth(300);
        courseBox.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: " + Main.C_TEXT + ";" +
            "-fx-pref-height: 40px;");
        loadCourses();
        courseBox.setOnAction(e -> refreshCalendar());

        // Month navigation
        Button prevBtn  = navBtn("◀  Prev");
        Button nextBtn  = navBtn("Next  ▶");
        Label monthLabel = new Label();
        monthLabel.setStyle(
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        prevBtn.setOnAction(e -> { currentMonth = currentMonth.minusMonths(1); refreshCalendar(); updateMonthLabel(monthLabel); });
        nextBtn.setOnAction(e -> { currentMonth = currentMonth.plusMonths(1);  refreshCalendar(); updateMonthLabel(monthLabel); });
        updateMonthLabel(monthLabel);

        HBox navRow = new HBox(16, prevBtn, monthLabel, nextBtn);
        navRow.setAlignment(Pos.CENTER_LEFT);

        // Legend
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_LEFT);
        for (String[] item : new String[][]{
            {"Present", Main.C_SUCCESS}, {"Absent", Main.C_DANGER},
            {"Late",    Main.C_WARNING}, {"No session", "#2a4060"}}) {
            HBox l = new HBox(6);
            l.setAlignment(Pos.CENTER_LEFT);
            Region dot = new Region();
            dot.setPrefSize(12, 12);
            dot.setStyle("-fx-background-color: " + item[1] + "; -fx-background-radius: 6;");
            Label lbl = new Label(item[0]);
            lbl.setStyle("-fx-text-fill: " + Main.C_MUTED + "; -fx-font-size: 11px;");
            l.getChildren().addAll(dot, lbl);
            legend.getChildren().add(l);
        }

        // Calendar grid
        calGrid = new GridPane();
        calGrid.setHgap(8);
        calGrid.setVgap(8);
        calGrid.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-background-radius: 12; -fx-padding: 20;");

        refreshCalendar();

        this.getChildren().addAll(
            title,
            new Separator(),
            courseLbl, courseBox,
            navRow,
            legend,
            calGrid
        );
    }

    private void refreshCalendar() {
        attendanceMap.clear();
        String course = courseBox.getValue();

        if (course != null) {
            Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in AttendanceCalendarView.java"); return; }
            try (conn) {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT a.attendance_date, a.status FROM attendance a " +
                    "JOIN courses c ON a.course_id = c.id " +
                    "WHERE a.student_username = ? AND c.course_name = ?");
                ps.setString(1, student.getUsername());
                ps.setString(2, course);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    LocalDate d = rs.getDate("attendance_date").toLocalDate();
                    attendanceMap.put(d, rs.getString("status"));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        buildCalendarGrid();
    }

    private void buildCalendarGrid() {
        calGrid.getChildren().clear();

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label d = new Label(days[i]);
            d.setStyle(
                "-fx-text-fill: " + Main.C_MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold;");
            d.setAlignment(Pos.CENTER);
            d.setMinWidth(52);
            calGrid.add(d, i, 0);
        }

        LocalDate first      = currentMonth.atDay(1);
        int       startCol   = first.getDayOfWeek().getValue() % 7;
        int       daysInMonth = currentMonth.lengthOfMonth();

        int col = startCol, row = 1;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date   = currentMonth.atDay(day);
            String    status = attendanceMap.getOrDefault(date, null);
            calGrid.add(buildDayCell(day, date, status), col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    private StackPane buildDayCell(int day, LocalDate date, String status) {
        StackPane cell = new StackPane();
        cell.setMinSize(52, 48);

        String bg, textColor;
        if (status == null) {
            bg = "#1e2f42"; textColor = Main.C_MUTED;
        } else {
            bg = switch (status) {
                case "Present" -> Main.C_SUCCESS + "33";
                case "Absent"  -> Main.C_DANGER  + "33";
                case "Late"    -> Main.C_WARNING  + "33";
                default        -> "#1e2f42";
            };
            textColor = switch (status) {
                case "Present" -> Main.C_SUCCESS;
                case "Absent"  -> Main.C_DANGER;
                case "Late"    -> Main.C_WARNING;
                default        -> Main.C_MUTED;
            };
        }

        boolean isToday = date.equals(LocalDate.now());
        String  border  = isToday ? Main.C_ACCENT : "#2a4060";

        cell.setStyle(
            "-fx-background-color: " + bg + "; -fx-background-radius: 8;" +
            "-fx-border-color: " + border + "; -fx-border-radius: 8;" +
            "-fx-border-width: " + (isToday ? "2" : "1") + ";");

        VBox content = new VBox(2);
        content.setAlignment(Pos.CENTER);

        Label dayLbl = new Label(String.valueOf(day));
        dayLbl.setStyle(
            "-fx-text-fill: " + textColor + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        content.getChildren().add(dayLbl);

        if (status != null) {
            Label statusLbl = new Label(status.substring(0, 1));
            statusLbl.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 9px;");
            content.getChildren().add(statusLbl);
        }

        cell.getChildren().add(content);

        if (status != null) {
            Tooltip tip = new Tooltip(date + "\nStatus: " + status);
            tip.setStyle("-fx-font-size: 11px;");
            Tooltip.install(cell, tip);
        }

        return cell;
    }

    private void loadCourses() {
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in AttendanceCalendarView.java"); return; }
            try (conn) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT c.course_name FROM courses c " +
                "JOIN enrollments e ON c.id = e.course_id " +
                "WHERE e.student_username = ?");
            ps.setString(1, student.getUsername());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) courseBox.getItems().add(rs.getString("course_name"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateMonthLabel(Label lbl) {
        lbl.setText(currentMonth.getMonth()
                               .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + " " + currentMonth.getYear());
    }

    private Button navBtn(String text) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-text-fill: " + Main.C_TEXT + ";" +
            "-fx-border-color: #2a4060; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 6 14; -fx-cursor: hand;");
        return b;
    }
}