package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.sql.*;

public class CourseEnrollmentModule extends VBox {

    private final User admin;

    private TextField courseNameField;
    private ComboBox<String> instructorBox;
    private TableView<String[]> courseTable;
    private ObservableList<String[]> courseData = FXCollections.observableArrayList();

    private ComboBox<String> enrollCourseBox;
    private ComboBox<String> enrollStudentBox;
    private TableView<String[]> enrollTable;
    private ObservableList<String[]> enrollData = FXCollections.observableArrayList();

    private Label courseFeedback = new Label("");
    private Label enrollFeedback = new Label("");

    public CourseEnrollmentModule(User admin) {
        this.admin = admin;
        this.setSpacing(24);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        Text title = new Text("Course & Enrollment Management");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));

        Text subtitle = new Text("Create courses, assign instructors, and enroll students.");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web(Main.C_MUTED));

        HBox sections = new HBox(20, buildCourseSection(), buildEnrollSection());
        HBox.setHgrow(sections.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(sections.getChildren().get(1), Priority.ALWAYS);
        VBox.setVgrow(sections, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(sections);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        this.getChildren().addAll(title, subtitle, new Separator(), scroll);
    }

    // ── COURSE SECTION ───────────────────────────────────────────────────────
    private VBox buildCourseSection() {
        Label secLbl = new Label("Create New Course");
        secLbl.setStyle("-fx-text-fill: " + Main.C_TEXT + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        courseNameField = (TextField) Main.styledField("Course name e.g. Mathematics 101", false);

        instructorBox = new ComboBox<>();
        instructorBox.setPromptText("Assign Instructor");
        instructorBox.setMaxWidth(Double.MAX_VALUE);
        styleCombo(instructorBox);
        loadInstructors();

        courseFeedback.setStyle("-fx-font-size: 12px;");

        Button addBtn = styledBtn("+ Add Course", Main.C_ACCENT);
        addBtn.setOnAction(e -> addCourse());

        courseTable = new TableView<>(courseData);
        applyTableStyle(courseTable);
        courseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        courseTable.setPrefHeight(200);
        courseTable.getColumns().addAll(
            tcol("ID", 0, Main.C_MUTED),
            tcol("Course Name", 1, Main.C_TEXT),
            tcol("Instructor", 2, Main.C_WARNING)
        );

        Button deleteBtn = styledBtn("Delete Selected Course", Main.C_DANGER);
        deleteBtn.setOnAction(e -> deleteCourse());

        Button refreshBtn = styledBtn("Refresh", Main.C_MUTED);
        refreshBtn.setOnAction(e -> { loadCourses(); loadEnrollCourseBox(); });

        HBox btnRow = new HBox(8, deleteBtn, refreshBtn);

        VBox box = new VBox(10,
            secLbl,
            Main.fieldLabel("COURSE NAME"), courseNameField,
            Main.fieldLabel("ASSIGN INSTRUCTOR"), instructorBox,
            addBtn, courseFeedback,
            Main.fieldLabel("EXISTING COURSES"), courseTable,
            btnRow
        );
        box.setPadding(new Insets(20));
        box.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-background-radius: 12;" +
            "-fx-border-color: #2a4060; -fx-border-radius: 12;");

        loadCourses();
        return box;
    }

    // ── ENROLLMENT SECTION ───────────────────────────────────────────────────
    private VBox buildEnrollSection() {
        Label secLbl = new Label("Enroll Student into Course");
        secLbl.setStyle("-fx-text-fill: " + Main.C_TEXT + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        enrollCourseBox = new ComboBox<>();
        enrollCourseBox.setPromptText("Select Course");
        enrollCourseBox.setMaxWidth(Double.MAX_VALUE);
        styleCombo(enrollCourseBox);
        loadEnrollCourseBox();

        enrollStudentBox = new ComboBox<>();
        enrollStudentBox.setPromptText("Select Student");
        enrollStudentBox.setMaxWidth(Double.MAX_VALUE);
        styleCombo(enrollStudentBox);
        loadStudents();

        enrollFeedback.setStyle("-fx-font-size: 12px;");

        Button enrollBtn = styledBtn("Enroll Student", Main.C_SUCCESS);
        enrollBtn.setOnAction(e -> enrollStudent());

        enrollTable = new TableView<>(enrollData);
        applyTableStyle(enrollTable);
        enrollTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        enrollTable.setPrefHeight(200);
        enrollTable.getColumns().addAll(
            tcol("ID", 0, Main.C_MUTED),
            tcol("Course", 1, Main.C_ACCENT),
            tcol("Student", 2, Main.C_SUCCESS),
            tcol("Instructor", 3, Main.C_WARNING)
        );

        Button removeBtn = styledBtn("Remove Selected Enrollment", Main.C_DANGER);
        removeBtn.setOnAction(e -> removeEnrollment());

        Button refreshBtn = styledBtn("Refresh", Main.C_MUTED);
        refreshBtn.setOnAction(e -> loadEnrollments());

        HBox btnRow = new HBox(8, removeBtn, refreshBtn);

        VBox box = new VBox(10,
            secLbl,
            Main.fieldLabel("SELECT COURSE"), enrollCourseBox,
            Main.fieldLabel("SELECT STUDENT"), enrollStudentBox,
            enrollBtn, enrollFeedback,
            Main.fieldLabel("CURRENT ENROLLMENTS"), enrollTable,
            btnRow
        );
        box.setPadding(new Insets(20));
        box.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-background-radius: 12;" +
            "-fx-border-color: #2a4060; -fx-border-radius: 12;");

        loadEnrollments();
        return box;
    }

    // ── ACTIONS ───────────────────────────────────────────────────────────────

    private void addCourse() {
        String name  = courseNameField.getText().trim();
        String instr = instructorBox.getValue();
        if (name.isEmpty() || instr == null) {
            setFeedback(courseFeedback, "All fields required.", false);
            return;
        }
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in CourseEnrollmentModule.java"); return; }
            try (conn) {
            PreparedStatement check = conn.prepareStatement(
                "SELECT id FROM courses WHERE course_name = ?");
            check.setString(1, name);
            if (check.executeQuery().next()) {
                setFeedback(courseFeedback, "Course already exists.", false);
                return;
            }
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO courses (course_name, instructor_username) VALUES (?, ?)");
            ps.setString(1, name);
            ps.setString(2, instr);
            ps.executeUpdate();
            auditLog(conn, "ADD_COURSE", "Created course: " + name + " -> " + instr);
            setFeedback(courseFeedback, "Course '" + name + "' created successfully.", true);
            courseNameField.clear();
            instructorBox.setValue(null);
            loadCourses();
            loadEnrollCourseBox();
        } catch (Exception ex) {
            setFeedback(courseFeedback, "Error: " + ex.getMessage(), false);
        }
    }

    private void deleteCourse() {
        String[] sel = courseTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setFeedback(courseFeedback, "Select a course first.", false); return; }
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in CourseEnrollmentModule.java"); return; }
            try (conn) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM courses WHERE id = ?");
            ps.setInt(1, Integer.parseInt(sel[0]));
            ps.executeUpdate();
            auditLog(conn, "DELETE_COURSE", "Deleted course: " + sel[1]);
            setFeedback(courseFeedback, "Course deleted.", true);
            loadCourses();
            loadEnrollCourseBox();
        } catch (Exception ex) {
            setFeedback(courseFeedback, "Error: " + ex.getMessage(), false);
        }
    }

    private void enrollStudent() {
        String course  = enrollCourseBox.getValue();
        String student = enrollStudentBox.getValue();
        if (course == null || student == null) {
            setFeedback(enrollFeedback, "Select both course and student.", false);
            return;
        }
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in CourseEnrollmentModule.java"); return; }
            try (conn) {
            PreparedStatement cps = conn.prepareStatement(
                "SELECT id FROM courses WHERE course_name = ?");
            cps.setString(1, course);
            ResultSet crs = cps.executeQuery();
            if (!crs.next()) { setFeedback(enrollFeedback, "Course not found.", false); return; }
            int courseId = crs.getInt("id");

            PreparedStatement dup = conn.prepareStatement(
                "SELECT id FROM enrollments WHERE course_id = ? AND student_username = ?");
            dup.setInt(1, courseId);
            dup.setString(2, student);
            if (dup.executeQuery().next()) {
                setFeedback(enrollFeedback, student + " is already enrolled in " + course, false);
                return;
            }
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO enrollments (student_username, course_id) VALUES (?, ?)");
            ps.setString(1, student);
            ps.setInt(2, courseId);
            ps.executeUpdate();
            auditLog(conn, "ENROLL_STUDENT", "Enrolled " + student + " into " + course);
            setFeedback(enrollFeedback, student + " enrolled in " + course, true);
            loadEnrollments();
        } catch (Exception ex) {
            setFeedback(enrollFeedback, "Error: " + ex.getMessage(), false);
        }
    }

    private void removeEnrollment() {
        String[] sel = enrollTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setFeedback(enrollFeedback, "Select an enrollment first.", false); return; }
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in CourseEnrollmentModule.java"); return; }
            try (conn) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM enrollments WHERE id = ?");
            ps.setInt(1, Integer.parseInt(sel[0]));
            ps.executeUpdate();
            auditLog(conn, "REMOVE_ENROLLMENT", "Removed: " + sel[2] + " from " + sel[1]);
            setFeedback(enrollFeedback, "Enrollment removed.", true);
            loadEnrollments();
        } catch (Exception ex) {
            setFeedback(enrollFeedback, "Error: " + ex.getMessage(), false);
        }
    }

    // ── DATA LOADERS ──────────────────────────────────────────────────────────

    private void loadInstructors() {
        instructorBox.getItems().clear();
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in CourseEnrollmentModule.java"); return; }
            try (conn) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT username FROM users WHERE role = 'Instructor' ORDER BY username");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) instructorBox.getItems().add(rs.getString("username"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadStudents() {
        enrollStudentBox.getItems().clear();
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in CourseEnrollmentModule.java"); return; }
            try (conn) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT username FROM users WHERE role = 'Student' ORDER BY username");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) enrollStudentBox.getItems().add(rs.getString("username"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadCourses() {
        courseData.clear();
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in CourseEnrollmentModule.java"); return; }
            try (conn) {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT id, course_name, instructor_username FROM courses ORDER BY course_name");
            while (rs.next()) courseData.add(new String[]{
                String.valueOf(rs.getInt("id")),
                rs.getString("course_name"),
                rs.getString("instructor_username")
            });
            if (courseData.isEmpty())
                courseData.add(new String[]{"—", "No courses created yet", "—"});
        } catch (Exception e) { e.printStackTrace(); }
    }

    void loadEnrollCourseBox() {
        enrollCourseBox.getItems().clear();
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in CourseEnrollmentModule.java"); return; }
            try (conn) {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT course_name FROM courses ORDER BY course_name");
            while (rs.next()) enrollCourseBox.getItems().add(rs.getString("course_name"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadEnrollments() {
        enrollData.clear();
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in CourseEnrollmentModule.java"); return; }
            try (conn) {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT e.id, c.course_name, e.student_username, c.instructor_username " +
                "FROM enrollments e " +
                "JOIN courses c ON e.course_id = c.id " +
                "ORDER BY c.course_name, e.student_username");
            while (rs.next()) enrollData.add(new String[]{
                String.valueOf(rs.getInt("id")),
                rs.getString("course_name"),
                rs.getString("student_username"),
                rs.getString("instructor_username")
            });
            if (enrollData.isEmpty())
                enrollData.add(new String[]{"—", "No enrollments yet", "—", "—"});
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void auditLog(Connection conn, String type, String desc) throws SQLException {
        PreparedStatement log = conn.prepareStatement(
            "INSERT INTO audit_log (action_by, action_type, description) VALUES (?,?,?)");
        log.setString(1, admin.getUsername());
        log.setString(2, type);
        log.setString(3, desc);
        log.executeUpdate();
    }

    private void setFeedback(Label lbl, String msg, boolean success) {
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " +
            (success ? Main.C_SUCCESS : Main.C_DANGER) + ";");
        lbl.setText(success ? "✅ " + msg : "⚠  " + msg);
    }

    private void applyTableStyle(TableView<?> t) {
        t.setStyle(
            "-fx-base: #162030;" +
            "-fx-control-inner-background: " + Main.C_PANEL + ";" +
            "-fx-control-inner-background-alt: #1a2840;" +
            "-fx-background-color: " + Main.C_PANEL + ";" +
            "-fx-table-cell-border-color: #2a4060;" +
            "-fx-border-color: #2a4060;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;");
    }

    private TableColumn<String[], String> tcol(String header, int idx, String textColor) {
        TableColumn<String[], String> col = new TableColumn<>(header);
        col.setCellValueFactory(
            p -> new javafx.beans.property.SimpleStringProperty(p.getValue()[idx]));
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle("-fx-background-color: " + Main.C_PANEL + ";"); return; }
                setText(item);
                setStyle("-fx-text-fill: " + textColor + "; -fx-background-color: " + Main.C_PANEL + ";");
            }
        });
        return col;
    }

    private Button styledBtn(String text, String color) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle(
            "-fx-background-color: " + color + "33; -fx-text-fill: " + color + ";" +
            "-fx-border-color: " + color + "55; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 12px; -fx-padding: 10 16; -fx-cursor: hand;");
        return b;
    }

    private void styleCombo(ComboBox<?> c) {
        c.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-pref-height: 42px; -fx-font-size: 13px;");
    }
}
