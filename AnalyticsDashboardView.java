package ams;

import javafx.application.Platform;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.sql.*;

public class AnalyticsDashboardView extends VBox {

    private final User user;

    private PieChart statusPie;
    private BarChart<String, Number> courseBar;
    private LineChart<String, Number> dateLine;
    private BarChart<String, Number> gpaBar;

    private Label statusLbl;

    private Label statTotalVal, statPresentVal, statAbsentVal, statLateVal;

    private static final String COLOR_PIE    = "#a78bfa";
    private static final String COLOR_COURSE = "#00c2cb";
    private static final String COLOR_DATE   = "#38bdf8";
    private static final String COLOR_GPA    = "#f59e0b";

    public AnalyticsDashboardView(User user) {
        this.user = user;
        setSpacing(24);
        setPadding(new Insets(32));
        setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
        loadCharts();
    }

    private void build() {

        // ── Header ──────────────────────────────────────────────────────────
        Text title = new Text("Attendance Analytics");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web(Main.C_TEXT));

        Text sub = new Text("Real-time trends by course, GPA, and date");
        sub.setFont(Font.font("System", 13));
        sub.setFill(Color.web(Main.C_MUTED));

        Button refresh = refreshBtn();
        refresh.setOnAction(e -> loadCharts());

        statusLbl = new Label("");
        statusLbl.setStyle("-fx-font-size: 11px;");

        VBox headerLeft  = new VBox(4, title, sub);
        Region spacer    = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox headerRight = new VBox(4, refresh, statusLbl);
        headerRight.setAlignment(Pos.CENTER_RIGHT);

        HBox headerRow = new HBox(headerLeft, spacer, headerRight);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // ── Stat pills ──────────────────────────────────────────────────────
        VBox pillTotal   = makePill("—", "Total Records", "#60a5fa");
        VBox pillPresent = makePill("—", "Present",       Main.C_SUCCESS);
        VBox pillAbsent  = makePill("—", "Absent",        Main.C_DANGER);
        VBox pillLate    = makePill("—", "Late",          Main.C_WARNING);

        statTotalVal   = (Label) pillTotal.getUserData();
        statPresentVal = (Label) pillPresent.getUserData();
        statAbsentVal  = (Label) pillAbsent.getUserData();
        statLateVal    = (Label) pillLate.getUserData();

        HBox statsRow = new HBox(16, pillTotal, pillPresent, pillAbsent, pillLate);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        // ── Charts ──────────────────────────────────────────────────────────
        statusPie = new PieChart();
        statusPie.setLabelsVisible(false);
        statusPie.setStyle("-fx-background-color: transparent;");
        statusPie.setLegendVisible(true);
        statusPie.setAnimated(false);

        courseBar = makeBarChart("Attendance %");
        dateLine  = makeLineChart("Records");
        gpaBar    = makeBarChart("GPA (0–4.0)");

        // ── Chart Grid ──────────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(0, 0, 20, 0)); // bottom padding so last row isn't clipped

        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col, col);

        // Each chart card has a fixed min height — grid will be tall enough to scroll
        grid.add(chartCard(statusPie, "Overall Attendance Status",
                "Breakdown by Present / Absent / Late",             COLOR_PIE),    0, 0);
        grid.add(chartCard(courseBar, "Attendance Rate by Course",
                "% present rate per course",                        COLOR_COURSE), 1, 0);
        grid.add(chartCard(dateLine,  "Attendance by Date",
                "Daily record count over time",                     COLOR_DATE),   0, 1);
        grid.add(chartCard(gpaBar,    "Average GPA by Course",
                "Avg GPA (0–4.0) derived from graded assignments",  COLOR_GPA),    1, 1);

        // ── THE FIX: wrap grid in a ScrollPane ──────────────────────────────
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);   // grid fills horizontal space
        scrollPane.setFitToHeight(false); // allow vertical scrolling
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background: transparent;" +
            "-fx-border-color: transparent;"
        );
        // Make the viewport background transparent too
        scrollPane.skinProperty().addListener((obs, o, n) -> {
            if (scrollPane.lookup(".viewport") != null)
                scrollPane.lookup(".viewport").setStyle("-fx-background-color: transparent;");
        });

        VBox.setVgrow(scrollPane, Priority.ALWAYS); // scrollPane — not grid — grows

        getChildren().addAll(headerRow, statsRow, new Separator(), scrollPane);

        Platform.runLater(() -> {
            // Fix viewport background (needs scene to be set)
            if (scrollPane.lookup(".viewport") != null)
                scrollPane.lookup(".viewport").setStyle("-fx-background-color: transparent;");

            applyDarkChartTheme(statusPie);
            applyDarkChartTheme(courseBar);
            applyDarkChartTheme(dateLine);
            applyDarkChartTheme(gpaBar);
        });
    }

    // ─── CHART CARD ───────────────────────────────────────────────────────────

    private VBox chartCard(Chart chart, String cardTitle, String hint, String accentColor) {

        Region accentBar = new Region();
        accentBar.setPrefHeight(3);
        accentBar.setMaxHeight(3);
        accentBar.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 2;");

        Label titleLbl = new Label(cardTitle);
        titleLbl.setStyle(
            "-fx-text-fill: " + Main.C_TEXT + ";" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 14px;"
        );

        Label hintLbl = new Label(hint);
        hintLbl.setStyle(
            "-fx-text-fill: " + Main.C_MUTED + ";" +
            "-fx-font-size: 11px;"
        );

        chart.setTitle("");
        chart.setLegendSide(Side.BOTTOM);
        chart.setPrefHeight(280);  // fixed preferred height — no Priority.ALWAYS needed
        VBox.setVgrow(chart, Priority.ALWAYS);

        VBox headerBox = new VBox(2, titleLbl, hintLbl);

        VBox card = new VBox(10, accentBar, headerBox, chart);
        card.setPadding(new Insets(16, 18, 18, 18));
        card.setMinHeight(340); // guaranteed height so bottom row is fully visible
        card.setPrefHeight(340);
        card.setStyle(
            "-fx-background-color: " + Main.C_PANEL + ";" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: #253550;" +
            "-fx-border-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.40), 14, 0, 0, 4);"
        );

        return card;
    }

    // ─── DARK THEME ───────────────────────────────────────────────────────────

    private void applyDarkChartTheme(Chart chart) {
        chart.applyCss();

        safeStyle(chart, ".chart-plot-background", "-fx-background-color: #0d1b2e;");
        safeStyle(chart, ".chart-content",         "-fx-background-color: transparent;");

        chart.lookupAll(".axis").forEach(n ->
            n.setStyle(
                "-fx-tick-label-fill: #cbd5e1;" +
                "-fx-tick-mark-stroke: #2a4060;" +
                "-fx-minor-tick-visible: false;"
            )
        );

        chart.lookupAll(".axis-label").forEach(n ->
            n.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px;")
        );

        chart.lookupAll(".chart-legend").forEach(n ->
            n.setStyle("-fx-background-color: transparent; -fx-padding: 4 0 0 0;")
        );

        chart.lookupAll(".chart-legend-item").forEach(n ->
            n.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px;")
        );

        chart.lookupAll(".chart-pie-label").forEach(n ->
            n.setStyle("-fx-fill: #e2e8f0; -fx-font-size: 12px;")
        );

        chart.lookupAll(".chart-pie-label-line").forEach(n ->
            n.setStyle("-fx-stroke: #4a6080;")
        );

        chart.lookupAll(".chart-horizontal-grid-lines").forEach(n ->
            n.setStyle("-fx-stroke: #1e3050; -fx-stroke-dash-array: 4 4;")
        );

        chart.lookupAll(".chart-vertical-grid-lines").forEach(n ->
            n.setStyle("-fx-stroke: #1e3050; -fx-stroke-dash-array: 4 4;")
        );

        chart.lookupAll(".chart-alternative-row-fill").forEach(n ->
            n.setStyle("-fx-fill: transparent;")
        );

        chart.lookupAll("Text").forEach(n ->
            n.setStyle("-fx-fill: #cbd5e1;")
        );

        chart.setStyle("-fx-background-color: transparent;");
    }

    private void safeStyle(Chart chart, String selector, String style) {
        var node = chart.lookup(selector);
        if (node != null) node.setStyle(style);
    }

    // ─── CHART FACTORIES ──────────────────────────────────────────────────────

    private BarChart<String, Number> makeBarChart(String yLabel) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);
        yAxis.setTickLabelFill(Color.web("#cbd5e1"));
        xAxis.setTickLabelFill(Color.web("#cbd5e1"));
        xAxis.setTickLabelRotation(-35);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setStyle("-fx-background-color: transparent;");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setBarGap(4);
        chart.setCategoryGap(20);
        return chart;
    }

    private LineChart<String, Number> makeLineChart(String yLabel) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);
        yAxis.setTickLabelFill(Color.web("#cbd5e1"));
        xAxis.setTickLabelFill(Color.web("#cbd5e1"));
        xAxis.setTickLabelRotation(-35);
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setStyle("-fx-background-color: transparent;");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        return chart;
    }

    // ─── DATA LOADING ─────────────────────────────────────────────────────────

    private void loadCharts() {
        statusPie.setData(FXCollections.observableArrayList());
        courseBar.getData().clear();
        dateLine.getData().clear();
        gpaBar.getData().clear();

        try (Connection conn = DBConnection.getConnection()) {
            loadSummaryStats(conn);
            loadStatus(conn);
            loadCourse(conn);
            loadDate(conn);
            loadGPA(conn);

            statusLbl.setText("✓  Refreshed");
            statusLbl.setStyle("-fx-text-fill: " + Main.C_SUCCESS + "; -fx-font-size: 11px;");

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLbl.setText("⚠  Error: " + ex.getMessage());
            statusLbl.setStyle("-fx-text-fill: " + Main.C_DANGER + "; -fx-font-size: 11px;");
        }
    }

    private void loadSummaryStats(Connection conn) throws SQLException {
        String sql =
            "SELECT COUNT(*) total, " +
            "SUM(CASE WHEN status='Present' THEN 1 ELSE 0 END) present_cnt, " +
            "SUM(CASE WHEN status='Absent'  THEN 1 ELSE 0 END) absent_cnt, " +
            "SUM(CASE WHEN status='Late'    THEN 1 ELSE 0 END) late_cnt " +
            "FROM attendance";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int total   = rs.getInt("total");
                int present = rs.getInt("present_cnt");
                int absent  = rs.getInt("absent_cnt");
                int late    = rs.getInt("late_cnt");
                Platform.runLater(() -> {
                    statTotalVal.setText(String.valueOf(total));
                    statPresentVal.setText(present + " (" + pct(present, total) + ")");
                    statAbsentVal.setText(absent   + " (" + pct(absent,  total) + ")");
                    statLateVal.setText(late       + " (" + pct(late,    total) + ")");
                });
            }
        }
    }

    private void loadStatus(Connection conn) throws SQLException {
        String sql = "SELECT status, COUNT(*) cnt FROM attendance GROUP BY status";
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                data.add(new PieChart.Data(
                    rs.getString("status") + " (" + rs.getInt("cnt") + ")",
                    rs.getInt("cnt")
                ));
        }

        statusPie.setData(data);

        Platform.runLater(() -> {
            for (PieChart.Data d : statusPie.getData()) {
                if (d.getNode() == null) continue;
                String name  = d.getName().toLowerCase();
                String color = name.startsWith("absent")  ? Main.C_DANGER
                             : name.startsWith("late")    ? Main.C_WARNING
                             : name.startsWith("present") ? Main.C_SUCCESS
                             : COLOR_PIE;
                d.getNode().setStyle("-fx-pie-color: " + color + ";");
            }
            applyDarkChartTheme(statusPie);
        });
    }

    private void loadCourse(Connection conn) throws SQLException {
        String sql =
            "SELECT c.course_name, " +
            "SUM(CASE WHEN a.status='Present' THEN 1 ELSE 0 END)*100.0/COUNT(*) pct " +
            "FROM attendance a JOIN courses c ON a.course_id=c.id " +
            "GROUP BY c.course_name";

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                s.getData().add(new XYChart.Data<>(
                    rs.getString("course_name"), rs.getDouble("pct")));
        }

        courseBar.getData().add(s);
        Platform.runLater(() -> {
            applyBarColor(s, COLOR_COURSE);
            applyDarkChartTheme(courseBar);
        });
    }

    private void loadDate(Connection conn) throws SQLException {
        String sql =
            "SELECT CONVERT(varchar(10), attendance_date, 120) day, COUNT(*) cnt " +
            "FROM attendance " +
            "GROUP BY CONVERT(varchar(10), attendance_date, 120) " +
            "ORDER BY day";

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                s.getData().add(new XYChart.Data<>(rs.getString("day"), rs.getInt("cnt")));
        }

        dateLine.getData().add(s);
        Platform.runLater(() -> {
            if (s.getNode() != null)
                s.getNode().setStyle(
                    "-fx-stroke: " + COLOR_DATE + "; -fx-stroke-width: 2.5px;");
            for (XYChart.Data<String, Number> d : s.getData()) {
                if (d.getNode() != null)
                    d.getNode().setStyle(
                        "-fx-background-color: " + COLOR_DATE + ", #0d1b2e;" +
                        "-fx-background-radius: 5px;" +
                        "-fx-background-insets: 0, 2;");
            }
            applyDarkChartTheme(dateLine);
        });
    }

    private void loadGPA(Connection conn) throws SQLException {
        String sql =
            "SELECT c.course_name, " +
            "  AVG(CAST(g.marks AS FLOAT) / 25.0) AS avg_gpa " +
            "FROM grades g " +
            "JOIN submissions s ON g.submission_id = s.id " +
            "JOIN assignments a ON s.assignment_id = a.id " +
            "JOIN courses c     ON a.course_id      = c.id " +
            "GROUP BY c.course_name " +
            "ORDER BY avg_gpa DESC";

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double gpa = Math.min(4.0, Math.max(0.0, rs.getDouble("avg_gpa")));
                s.getData().add(new XYChart.Data<>(rs.getString("course_name"), gpa));
            }
        }

        if (s.getData().isEmpty())
            s.getData().add(new XYChart.Data<>("No graded data", 0));

        gpaBar.getData().add(s);

        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : s.getData()) {
                if (d.getNode() == null) continue;
                double gpa   = d.getYValue().doubleValue();
                String color = gpa >= 3.0 ? Main.C_SUCCESS
                             : gpa >= 2.0 ? COLOR_GPA
                             : Main.C_DANGER;
                d.getNode().setStyle(
                    "-fx-bar-fill: " + color + ";" +
                    "-fx-background-radius: 4 4 0 0;"
                );
            }
            applyDarkChartTheme(gpaBar);
        });
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private void applyBarColor(XYChart.Series<String, Number> series, String color) {
        for (XYChart.Data<String, Number> d : series.getData()) {
            if (d.getNode() != null)
                d.getNode().setStyle(
                    "-fx-bar-fill: " + color + ";" +
                    "-fx-background-radius: 4 4 0 0;"
                );
        }
    }

    private VBox makePill(String value, String labelText, String color) {
        Label valLbl = new Label(value);
        valLbl.setStyle(
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 22px;" +
            "-fx-font-weight: bold;"
        );

        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: " + Main.C_MUTED + "; -fx-font-size: 11px;");

        VBox pill = new VBox(3, valLbl, lbl);
        pill.setPadding(new Insets(14, 22, 14, 22));
        pill.setMinWidth(148);
        pill.setStyle(
            "-fx-background-color: " + Main.C_PANEL + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + color + "44;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1.5;"
        );

        pill.setUserData(valLbl);
        return pill;
    }

    private String pct(int num, int total) {
        if (total == 0) return "0%";
        return String.format("%.0f%%", num * 100.0 / total);
    }

    private Button refreshBtn() {
        Button b = new Button("⟳  Refresh Analytics");
        b.setStyle(
            "-fx-background-color: " + Main.C_ACCENT + "22;" +
            "-fx-text-fill: " + Main.C_ACCENT + ";" +
            "-fx-border-color: " + Main.C_ACCENT + "66;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 9 20;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;"
        );
        return b;
    }
}