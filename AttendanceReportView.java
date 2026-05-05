package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * US-07 — Admin Attendance Report
 *
 * US-07  : Generate an attendance report for any course
 * US-07a : Export the report as CSV or PDF
 * US-07b : Filter report by student, date range, or course
 */
public class AttendanceReportView extends VBox {

    private final User admin;

    private ComboBox<String> courseFilter;
    private TextField        studentFilter;
    private DatePicker       fromDate;
    private DatePicker       toDate;

    private TableView<String[]> table;
    private ObservableList<String[]> data = FXCollections.observableArrayList();

    // Cached last-used report rows for export
    private List<String[]> lastReport = new ArrayList<>();

    public AttendanceReportView(User admin) {
        this.admin = admin;
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        // ── Title ─────────────────────────────────────────────────────────
        Text title = new Text("Attendance Report");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));

        Text subtitle = new Text("Filter, review, and export attendance records");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web(Main.C_MUTED));

        // ── Filter panel ──────────────────────────────────────────────────
        VBox filterBox = buildFilterPanel();

        // ── Summary cards (dynamic) ───────────────────────────────────────
        HBox summaryRow = new HBox(14);
        summaryRow.setAlignment(Pos.CENTER_LEFT);
        summaryRow.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-background-radius: 10;" +
            "-fx-padding: 14 18;");

        Label summaryLabel = new Label("Run a report to see summary statistics.");
        summaryLabel.setStyle("-fx-text-fill: " + Main.C_MUTED + "; -fx-font-size: 12px;");
        summaryRow.getChildren().add(summaryLabel);

        // ── Table ─────────────────────────────────────────────────────────
        table = new TableView<>(data);
        applyDarkTableStyle(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<String[], String> dateCol    = tcol("Date",    0, Main.C_TEXT);
        TableColumn<String[], String> studentCol = tcol("Student", 1, Main.C_TEXT);
        TableColumn<String[], String> courseCol  = tcol("Course",  2, Main.C_TEXT);
        TableColumn<String[], String> instrCol   = tcol("Instructor", 3, Main.C_MUTED);

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
                    "-fx-text-fill: " + color + ";" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-color: " + Main.C_PANEL + ";");
            }
        });

        TableColumn<String[], String> notesCol = tcol("Notes", 5, Main.C_MUTED);
        table.getColumns().addAll(dateCol, studentCol, courseCol, instrCol, statusCol, notesCol);

        // ── Export row ────────────────────────────────────────────────────
        Button csvBtn = actionBtn("⬇  Export CSV",  Main.C_SUCCESS);
        Button pdfBtn = actionBtn("⬇  Export PDF",  Main.C_DANGER);
        csvBtn.setOnAction(e -> exportCSV());
        pdfBtn.setOnAction(e -> exportPDF());

        Label exportNote = new Label("Generate a report first, then export.");
        exportNote.setStyle("-fx-text-fill: " + Main.C_MUTED + "; -fx-font-size: 11px;");

        HBox exportRow = new HBox(10, csvBtn, pdfBtn, exportNote);
        exportRow.setAlignment(Pos.CENTER_LEFT);

        // ── Generate button ───────────────────────────────────────────────
        Button genBtn = new Button("▶  Generate Report");
        genBtn.setStyle(
            "-fx-background-color: linear-gradient(to right," + Main.C_ACCENT + "," + Main.C_ACCENT2 + ");" +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;" +
            "-fx-background-radius: 8; -fx-pref-height: 40px; -fx-padding: 0 24; -fx-cursor: hand;");
        genBtn.setOnAction(e -> generateReport(summaryRow));

        this.getChildren().addAll(
            title, subtitle, new Separator(),
            filterBox,
            genBtn,
            summaryRow,
            table,
            exportRow);
    }

    // ── Filter panel ─────────────────────────────────────────────────────────

    private VBox buildFilterPanel() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle(
            "-fx-background-color: " + Main.C_PANEL + ";" +
            "-fx-background-radius: 12; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 12;");

        Label filterTitle = new Label("🔍  Filters");
        filterTitle.setStyle(
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-font-weight: bold; -fx-font-size: 13px;");

        // Row 1 — course + student
        courseFilter = new ComboBox<>();
        courseFilter.setPromptText("All Courses");
        courseFilter.setPrefWidth(220);
        styleCombo(courseFilter);
        loadCourses();

        studentFilter = (TextField) Main.styledField("Student username (leave blank = all)…", false);
        studentFilter.setPrefWidth(240);

        HBox row1 = new HBox(16,
            labeledControl("COURSE",  courseFilter),
            labeledControl("STUDENT", studentFilter));
        row1.setAlignment(Pos.CENTER_LEFT);

        // Row 2 — date range
        fromDate = new DatePicker(LocalDate.now().withDayOfMonth(1));
        toDate   = new DatePicker(LocalDate.now());
        stylePicker(fromDate);
        stylePicker(toDate);
        fromDate.setPrefWidth(160);
        toDate.setPrefWidth(160);

        Button clearBtn = new Button("✕  Clear Filters");
        clearBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: " + Main.C_MUTED + ";" +
            "-fx-border-color: #2a4060; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 6 12; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> {
            courseFilter.setValue(null);
            studentFilter.clear();
            fromDate.setValue(LocalDate.now().withDayOfMonth(1));
            toDate.setValue(LocalDate.now());
        });

        HBox row2 = new HBox(16,
            labeledControl("FROM DATE", fromDate),
            labeledControl("TO DATE",   toDate),
            clearBtn);
        row2.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(filterTitle, row1, row2);
        return box;
    }

    // ── Report generation ─────────────────────────────────────────────────────

    private void generateReport(HBox summaryRow) {
        data.clear();
        lastReport.clear();

        String course  = courseFilter.getValue();
        String student = studentFilter.getText().trim();
        LocalDate from = fromDate.getValue();
        LocalDate to   = toDate.getValue();

        StringBuilder sql = new StringBuilder(
            "SELECT a.attendance_date, a.student_username, c.course_name, " +
            "       a.marked_by, a.status, ISNULL(a.notes,'') as notes " +
            "FROM attendance a JOIN courses c ON a.course_id = c.id " +
            "WHERE a.attendance_date >= ? AND a.attendance_date <= ? ");

        if (course  != null && !course.isEmpty())  sql.append("AND c.course_name = ? ");
        if (!student.isEmpty())                     sql.append("AND a.student_username LIKE ? ");

        sql.append("ORDER BY a.attendance_date DESC, a.student_username");

        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in AttendanceReportView.java"); return; }
            try (conn) {
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setDate(idx++, java.sql.Date.valueOf(from));
            ps.setDate(idx++, java.sql.Date.valueOf(to));
            if (course  != null && !course.isEmpty())  ps.setString(idx++, course);
            if (!student.isEmpty())                     ps.setString(idx++, "%" + student + "%");

            ResultSet rs = ps.executeQuery();
            int total = 0, present = 0, absent = 0, late = 0;

            while (rs.next()) {
                String[] row = {
                    rs.getDate("attendance_date").toString(),
                    rs.getString("student_username"),
                    rs.getString("course_name"),
                    rs.getString("marked_by"),
                    rs.getString("status"),
                    rs.getString("notes")
                };
                data.add(row);
                lastReport.add(row);
                total++;
                switch (rs.getString("status")) {
                    case "Present" -> present++;
                    case "Absent"  -> absent++;
                    case "Late"    -> late++;
                }
            }

            // Update summary row
            summaryRow.getChildren().clear();
            summaryRow.getChildren().addAll(
                miniCard("Total Records", String.valueOf(total),   Main.C_ACCENT),
                miniCard("Present",       String.valueOf(present), Main.C_SUCCESS),
                miniCard("Absent",        String.valueOf(absent),  Main.C_DANGER),
                miniCard("Late",          String.valueOf(late),    Main.C_WARNING),
                miniCard("Attendance %",
                    total == 0 ? "—" :
                    String.format("%.1f%%", (double)(present + late) / total * 100),
                    Main.C_ACCENT2)
            );

            if (data.isEmpty())
                data.add(new String[]{"No records match the selected filters.",
                                      "", "", "", "", ""});

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).show();
        }
    }

    // ── US-07a : CSV export ───────────────────────────────────────────────────

    private void exportCSV() {
        if (lastReport.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Generate a report first.").show();
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save CSV Report");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("attendance_report_" + LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Date,Student,Course,Instructor,Status,Notes");
            for (String[] row : lastReport) {
                pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    row[0], row[1], row[2], row[3], row[4], row[5]);
            }
            new Alert(Alert.AlertType.INFORMATION,
                "✅ CSV saved to:\n" + file.getAbsolutePath()).show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).show();
        }
    }

    // ── US-07a : PDF export ───────────────────────────────────────────────────
    // Uses plain text in a UTF-8 encoded .pdf-named file.
    // For a real PDF add iText/Apache PDFBox to the classpath; the structure
    // below is designed to drop in as the body of a PDFBox call.

    private void exportPDF() {
        if (lastReport.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Generate a report first.").show();
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save PDF Report");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("attendance_report_" + LocalDate.now() + ".pdf");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        try {
            // ── Attempt iText / PDFBox if present on classpath ─────────
            // This block tries to use Apache PDFBox via reflection so the
            // project compiles without requiring the library.
            // If PDFBox is on the classpath, a proper PDF is written.
            // Otherwise, falls back to a formatted plain-text export.
            boolean pdfWritten = tryWriteWithPDFBox(file);

            if (!pdfWritten) {
                // Fallback: well-formatted plain text saved as .pdf
                writePlainTextPDF(file);
            }

            new Alert(Alert.AlertType.INFORMATION,
                "✅ PDF saved to:\n" + file.getAbsolutePath()).show();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).show();
        }
    }

    /** Fallback: writes a formatted plain-text report with .pdf extension. */
    private void writePlainTextPDF(File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("================================================================");
            pw.println("           ACADEMIC MANAGEMENT SYSTEM — ATTENDANCE REPORT      ");
            pw.println("================================================================");
            pw.println("Generated: " + java.time.LocalDateTime.now()
                                           .toString().substring(0, 16));
            pw.println("Total Records: " + lastReport.size());
            pw.println("----------------------------------------------------------------");
            pw.printf("%-12s %-18s %-22s %-16s %-9s %s%n",
                "Date", "Student", "Course", "Instructor", "Status", "Notes");
            pw.println("----------------------------------------------------------------");
            for (String[] row : lastReport) {
                pw.printf("%-12s %-18s %-22s %-16s %-9s %s%n",
                    truncate(row[0], 12), truncate(row[1], 18),
                    truncate(row[2], 22), truncate(row[3], 16),
                    truncate(row[4], 9),  row[5]);
            }
            pw.println("================================================================");
            pw.println("END OF REPORT");
        }
    }

    /**
     * Attempts to write a real PDF using Apache PDFBox via reflection.
     * Returns true on success, false if PDFBox is not available.
     */
    private boolean tryWriteWithPDFBox(File file) {
        try {
            Class<?> docClass  = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> pageClass = Class.forName("org.apache.pdfbox.pdmodel.PDPage");
            Class<?> streamClass = Class.forName(
                "org.apache.pdfbox.pdmodel.PDPageContentStream");
            Class<?> fontClass = Class.forName(
                "org.apache.pdfbox.pdmodel.font.PDType1Font");

            Object doc  = docClass.getDeclaredConstructor().newInstance();
            Object page = pageClass.getDeclaredConstructor().newInstance();
            docClass.getMethod("addPage", pageClass).invoke(doc, page);

            Object font = fontClass.getField("HELVETICA").get(null);
            Object cs   = streamClass
                .getDeclaredConstructor(docClass, pageClass)
                .newInstance(doc, page);

            // beginText / setFont / newLineAtOffset / showText / endText
            streamClass.getMethod("beginText").invoke(cs);
            streamClass.getMethod("setFont", fontClass, float.class)
                       .invoke(cs, font, 10f);
            streamClass.getMethod("newLineAtOffset", float.class, float.class)
                       .invoke(cs, 50f, 750f);

            String[] headers = {"Date","Student","Course","Instructor","Status","Notes"};
            String headerLine = String.join("  |  ", headers);
            streamClass.getMethod("showText", String.class).invoke(cs, headerLine);

            for (String[] row : lastReport) {
                streamClass.getMethod("newLineAtOffset", float.class, float.class)
                           .invoke(cs, 0f, -14f);
                String line = String.join("  |  ",
                    truncate(row[0], 11), truncate(row[1], 16),
                    truncate(row[2], 20), truncate(row[3], 14),
                    row[4], row[5]);
                streamClass.getMethod("showText", String.class).invoke(cs, line);
            }

            streamClass.getMethod("endText").invoke(cs);
            streamClass.getMethod("close").invoke(cs);
            docClass.getMethod("save", File.class).invoke(doc, file);
            docClass.getMethod("close").invoke(doc);
            return true;

        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
            // PDFBox not on classpath — use text fallback
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void loadCourses() {
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in AttendanceReportView.java"); return; }
            try (conn) {
            ResultSet rs = conn.createStatement()
                .executeQuery("SELECT course_name FROM courses ORDER BY course_name");
            while (rs.next()) courseFilter.getItems().add(rs.getString("course_name"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private HBox labeledControl(String label, javafx.scene.Node ctrl) {
        VBox box = new VBox(4, Main.fieldLabel(label), ctrl);
        return new HBox(box);
    }

    private VBox miniCard(String label, String value, String color) {
        VBox card = new VBox(2);
        card.setPadding(new Insets(8, 16, 8, 16));
        card.setStyle(
            "-fx-background-color: " + color + "11;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + color + "33;" +
            "-fx-border-radius: 8;");
        Text val = new Text(value);
        val.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        val.setFill(Color.web(color));
        Text lbl = new Text(label);
        lbl.setFont(Font.font("System", 10));
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

    private void styleCombo(ComboBox<?> c) {
        c.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-pref-height: 42px;");
    }

    private void stylePicker(DatePicker dp) {
        dp.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 13px; -fx-pref-height: 42px;");
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

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}