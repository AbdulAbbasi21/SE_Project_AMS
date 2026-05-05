package ams;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;

public class DashboardView extends BorderPane {

    private final Stage stage;
    private final User  user;
    private final Main  app;
    private Button      activeBtn = null;

    // Notification bell badge — kept as field so it can be refreshed
    private Label bellBadge;

    public DashboardView(Stage stage, User user, Main app) {
        this.stage = stage;
        this.user  = user;
        this.app   = app;
        this.setStyle("-fx-background-color: " + Main.C_BG + ";");
        buildSidebar();
        showWelcome();
    }

    // ════════════════════════════════════════════════════════════════════════
    // SIDEBAR
    // ════════════════════════════════════════════════════════════════════════
    private void buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(230);
        sidebar.setStyle("-fx-background-color: " + Main.C_SIDEBAR + ";");

        // ── Branding ──────────────────────────────────────────────────────
        VBox brand = new VBox(4);
        brand.setPadding(new Insets(28, 20, 20, 20));
        brand.setStyle("-fx-background-color: " + Main.C_BG + ";");

        StackPane logoBox = new StackPane();
        Rectangle logoRect = new Rectangle(40, 40);
        logoRect.setArcWidth(10);
        logoRect.setArcHeight(10);
        logoRect.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(Main.C_ACCENT)),
            new Stop(1, Color.web(Main.C_ACCENT2))));

        Text logoTxt = new Text("AMS");
        logoTxt.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        logoTxt.setFill(Color.WHITE);

        logoBox.getChildren().addAll(logoRect, logoTxt);
        logoBox.setMaxWidth(40);

        HBox brandRow = new HBox(10, logoBox);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        VBox brandText = new VBox(1);
        Text brandName = new Text("AMS");
        brandName.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        brandName.setFill(Color.web(Main.C_TEXT));

        Text brandSub = new Text("Academic System");
        brandSub.setFont(Font.font("System", 10));
        brandSub.setFill(Color.web(Main.C_MUTED));

        brandText.getChildren().addAll(brandName, brandSub);
        brandRow.getChildren().add(brandText);
        brand.getChildren().add(brandRow);

        // ── User info card ────────────────────────────────────────────────
        VBox userCard = new VBox(3);
        userCard.setPadding(new Insets(14, 16, 14, 16));
        userCard.setStyle("-fx-background-color: #1a2d40; -fx-background-radius: 10;");

        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(20);
        avatarCircle.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(roleColor())),
            new Stop(1, Color.web(Main.C_ACCENT2))));

        Text avatarInitial = new Text(user.getUsername().substring(0, 1).toUpperCase());
        avatarInitial.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        avatarInitial.setFill(Color.WHITE);

        avatar.getChildren().addAll(avatarCircle, avatarInitial);
        avatar.setMaxWidth(40);

        HBox userRow = new HBox(10, avatar);
        userRow.setAlignment(Pos.CENTER_LEFT);

        VBox userInfo = new VBox(2);
        Label userName = new Label(user.getUsername());
        userName.setStyle("-fx-text-fill: " + Main.C_TEXT + "; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label roleBadge = new Label(user.getRole());
        roleBadge.setStyle(
            "-fx-background-color: " + roleColor() + "33; -fx-text-fill: " + roleColor() + ";" +
            "-fx-border-color: " + roleColor() + "66; -fx-border-radius: 10; -fx-background-radius: 10;" +
            "-fx-padding: 1 8; -fx-font-size: 10px;"
        );

        userInfo.getChildren().addAll(userName, roleBadge);
        userRow.getChildren().add(userInfo);
        userCard.getChildren().add(userRow);

        if (user.getRole().equalsIgnoreCase("Student")) {
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            StackPane bellBox = buildBellButton();
            userRow.getChildren().addAll(sp, bellBox);
        }

        VBox userCardWrapper = new VBox(userCard);
        userCardWrapper.setPadding(new Insets(0, 12, 12, 12));
        userCardWrapper.setStyle("-fx-background-color: " + Main.C_BG + ";");

        // ── Navigation ────────────────────────────────────────────────────
        VBox nav = new VBox(2);
        nav.setPadding(new Insets(16, 10, 10, 10));
        VBox.setVgrow(nav, Priority.ALWAYS);

        nav.getChildren().add(sectionLabel("NAVIGATION"));

        Button btnHome = navBtn("🏠  Home", false);
        btnHome.setOnAction(e -> {
            setActive(btnHome);
            showWelcome();
        });
        nav.getChildren().add(btnHome);
        setActive(btnHome);

        // ── Admin ─────────────────────────────────────────────────────────
        if (user.getRole().equalsIgnoreCase("Admin")) {
            nav.getChildren().add(sectionLabel("ADMIN"));

            Button btnRegister = navBtn("👤  Manage Users", false);
            btnRegister.setOnAction(e -> {
                setActive(btnRegister);
                setCenter(new AdminRegistrationModule(user));
            });

            Button btnCourses = navBtn("Courses & Enrollment", false);
            btnCourses.setOnAction(e -> {
                setActive(btnCourses);
                setCenter(new CourseEnrollmentModule(user));
            });

            Button btnReport = navBtn("📊  Attendance Report", false);
            btnReport.setOnAction(e -> {
                setActive(btnReport);
                setCenter(new AttendanceReportView(user));
            });

            Button btnAnalytics = navBtn("📈  Analytics Dashboard", false);
            btnAnalytics.setOnAction(e -> {
                setActive(btnAnalytics);
                setCenter(new AnalyticsDashboardView(user));
            });

            Button btnEmailQ = navBtn("📧  Email Alerts Queue", false);
            btnEmailQ.setOnAction(e -> {
                setActive(btnEmailQ);
                setCenter(new EmailNotificationView());
            });

            Button btnAudit = navBtn("📋  Audit Log", false);
            btnAudit.setOnAction(e -> {
                setActive(btnAudit);
                setCenter(new AuditLogView());
            });

            nav.getChildren().addAll(btnRegister, btnCourses, btnReport, btnAnalytics, btnEmailQ, btnAudit);
        }

        // ── Instructor ────────────────────────────────────────────────────
        if (user.getRole().equalsIgnoreCase("Instructor")) {
            nav.getChildren().add(sectionLabel("INSTRUCTOR"));

            Button btnAttend = navBtn("✅  Mark Attendance", false);
            btnAttend.setOnAction(e -> {
                setActive(btnAttend);
                setCenter(new AttendanceModule(user));
            });

            Button btnEdit = navBtn("✏️  Edit Attendance", false);
            btnEdit.setOnAction(e -> {
                setActive(btnEdit);
                setCenter(new EditAttendanceView(user));
            });

            Button btnAssignments = navBtn("📝  Assignments", false);
            btnAssignments.setOnAction(e -> {
                setActive(btnAssignments);
                setCenter(new AssignmentManagementView(user));
            });

            Button btnGrade = navBtn("📊  Grade Assignments", false);
            btnGrade.setOnAction(e -> {
                setActive(btnGrade);
                setCenter(new GradeAssignmentsView(user));
            });

            Button btnAbsent = navBtn("⚠️  Frequent Absences", false);
            btnAbsent.setOnAction(e -> {
                setActive(btnAbsent);
                setCenter(new AbsentStudentsSummaryView(user));
            });

            nav.getChildren().addAll(btnAttend, btnEdit, btnAssignments, btnGrade, btnAbsent);
        }

        // ── Student ───────────────────────────────────────────────────────
        if (user.getRole().equalsIgnoreCase("Student")) {
            nav.getChildren().add(sectionLabel("STUDENT"));

            Button btnHistory = navBtn("📊  My Attendance", false);
            btnHistory.setOnAction(e -> {
                setActive(btnHistory);
                setCenter(new StudentAttendanceView(user));
            });

            Button btnCalendar = navBtn("📅  Calendar View", false);
            btnCalendar.setOnAction(e -> {
                setActive(btnCalendar);
                setCenter(new AttendanceCalendarView(user));
            });

            Button btnAlerts = navBtn("🔔  My Alerts", false);
            btnAlerts.setOnAction(e -> {
                setActive(btnAlerts);
                setCenter(new NotificationPanel(user));
                AttendanceAlertService.markAllRead(user.getUsername());
                refreshBellBadge();
            });

            Button btnAssignments = navBtn("📝  Assignments", false);
            btnAssignments.setOnAction(e -> {
                setActive(btnAssignments);
                setCenter(new StudentAssignmentsView(user));
            });

            Button btnGrades = navBtn("📈  My Grades", false);
            btnGrades.setOnAction(e -> {
                setActive(btnGrades);
                setCenter(new StudentGradesView(user));
            });

            nav.getChildren().addAll(btnHistory, btnCalendar, btnAlerts, btnAssignments, btnGrades);
        }

        // ── Logout ────────────────────────────────────────────────────────
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("⎋  Sign Out");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setStyle(
            "-fx-background-color: " + Main.C_DANGER + "22; -fx-text-fill: " + Main.C_DANGER + ";" +
            "-fx-border-color: " + Main.C_DANGER + "44; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 13px; -fx-padding: 10 14;"
        );
        btnLogout.setOnAction(e -> app.showLogin());

        VBox bottomNav = new VBox(8, spacer, btnLogout);
        bottomNav.setPadding(new Insets(0, 10, 16, 10));
        VBox.setVgrow(bottomNav, Priority.ALWAYS);

        nav.getChildren().add(bottomNav);

        sidebar.getChildren().addAll(brand, userCardWrapper, nav);
        this.setLeft(sidebar);
    }

    // ── US-06a : Notification bell button ─────────────────────────────────────

    private StackPane buildBellButton() {
        StackPane box = new StackPane();
        box.setMaxSize(32, 32);
        box.setMinSize(32, 32);
        box.setStyle("-fx-cursor: hand;");

        Label bell = new Label("🔔");
        bell.setStyle("-fx-font-size: 14px;");

        bellBadge = new Label("");
        bellBadge.setStyle(
            "-fx-background-color: " + Main.C_DANGER + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8;" +
            "-fx-font-size: 8px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 1 4;");
        bellBadge.setVisible(false);
        StackPane.setAlignment(bellBadge, Pos.TOP_RIGHT);

        refreshBellBadge();
        box.getChildren().addAll(bell, bellBadge);

        // Click → show popup panel
        box.setOnMouseClicked(e -> showNotificationPopup(box));
        return box;
    }

    private void refreshBellBadge() {
        if (bellBadge == null) return;
        int count = AttendanceAlertService.getUnreadCount(user.getUsername());
        if (count > 0) {
            bellBadge.setText(String.valueOf(count));
            bellBadge.setVisible(true);
        } else {
            bellBadge.setVisible(false);
        }
    }

    private void showNotificationPopup(StackPane anchor) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        NotificationPanel panel = new NotificationPanel(user);

        popup.getContent().add(panel);

        javafx.geometry.Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds != null) {
            popup.show(stage,
                bounds.getMinX() - 320,
                bounds.getMaxY() + 8);
        }

        // Refresh badge after closing
        popup.setOnHidden(ev -> refreshBellBadge());
    }

    // ════════════════════════════════════════════════════════════════════════
    // WELCOME SCREEN
    // ════════════════════════════════════════════════════════════════════════
    private void showWelcome() {
        VBox welcome = new VBox(24);
        welcome.setPadding(new Insets(40));
        welcome.setStyle("-fx-background-color: " + Main.C_BG + ";");

        Text greeting = new Text("Welcome back, " + user.getUsername() + " 👋");
        greeting.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        greeting.setFill(Color.web(Main.C_TEXT));

        Text sub = new Text("Here's an overview of your dashboard.");
        sub.setFont(Font.font("System", 14));
        sub.setFill(Color.web(Main.C_MUTED));

        VBox header = new VBox(6, greeting, sub);

        HBox stats = new HBox(16);
        stats.setAlignment(Pos.CENTER_LEFT);

        if (user.getRole().equalsIgnoreCase("Admin")) {
            stats.getChildren().addAll(
                statCard("Total Users",  countQuery("SELECT COUNT(*) FROM users"),                                    Main.C_ACCENT),
                statCard("Instructors",  countQuery("SELECT COUNT(*) FROM users WHERE role='Instructor'"),            Main.C_WARNING),
                statCard("Students",     countQuery("SELECT COUNT(*) FROM users WHERE role='Student'"),              Main.C_SUCCESS),
                statCard("Courses",      countQuery("SELECT COUNT(*) FROM courses"),                                  Main.C_ACCENT2)
            );
        } else if (user.getRole().equalsIgnoreCase("Instructor")) {
            stats.getChildren().addAll(
                statCard("My Courses",     countQuery("SELECT COUNT(*) FROM courses WHERE instructor_username='" + user.getUsername() + "'"), Main.C_ACCENT),
                statCard("Total Sessions", countQuery("SELECT COUNT(DISTINCT attendance_date) FROM attendance WHERE marked_by='" + user.getUsername() + "'"), Main.C_WARNING),
                statCard("Records Marked", countQuery("SELECT COUNT(*) FROM attendance WHERE marked_by='" + user.getUsername() + "'"), Main.C_SUCCESS)
            );
        } else {
            int[] pct = getStudentAttendancePct();
            int unread = AttendanceAlertService.getUnreadCount(user.getUsername());
            stats.getChildren().addAll(
                statCard("Enrolled Courses", countQuery("SELECT COUNT(*) FROM enrollments WHERE student_username='" + user.getUsername() + "'"), Main.C_ACCENT),
                statCard("Present",  String.valueOf(pct[0]), Main.C_SUCCESS),
                statCard("Absent",   String.valueOf(pct[1]), Main.C_DANGER),
                statCard("Late",     String.valueOf(pct[2]), Main.C_WARNING)
            );
            if (unread > 0) {
                statCard("⚠ Alerts", String.valueOf(unread), Main.C_DANGER);
                // Show a prominent alert banner
                Label alertBanner = new Label(
                    "🔔  You have " + unread + " unread attendance alert(s). " +
                    "Click '🔔 My Alerts' in the sidebar to view them.");
                alertBanner.setStyle(
                    "-fx-background-color: " + Main.C_DANGER + "22;" +
                    "-fx-text-fill: " + Main.C_DANGER + ";" +
                    "-fx-border-color: " + Main.C_DANGER + "55;" +
                    "-fx-border-radius: 8; -fx-background-radius: 8;" +
                    "-fx-padding: 10 16; -fx-font-size: 12px;");
                alertBanner.setWrapText(true);
                welcome.getChildren().add(alertBanner);
            }
        }

        Label hint = new Label("Use the sidebar to navigate to your modules.");
        hint.setStyle("-fx-text-fill: " + Main.C_MUTED + "; -fx-font-size: 12px;");

        welcome.getChildren().addAll(header, stats, hint);

        FadeTransition ft = new FadeTransition(Duration.millis(400), welcome);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        this.setCenter(welcome);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private VBox statCard(String title, String value, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-background-radius: 12;" +
            "-fx-border-color: " + color + "33; -fx-border-radius: 12; -fx-border-width: 1;");
        card.setMinWidth(140);

        Text val = new Text(value);
        val.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        val.setFill(Color.web(color));

        Text lbl = new Text(title);
        lbl.setFont(Font.font("System", 12));
        lbl.setFill(Color.web(Main.C_MUTED));

        card.getChildren().addAll(val, lbl);
        return card;
    }

    private String countQuery(String sql) {
        var conn = DBConnection.getConnection();
        if (conn == null) return "—";
        try (conn; var stmt = conn.createStatement(); var rs = stmt.executeQuery(sql)) {
            if (rs.next()) return String.valueOf(rs.getInt(1));
        } catch (Exception e) { e.printStackTrace(); }
        return "—";
    }

    private int[] getStudentAttendancePct() {
        int[] r = {0, 0, 0};
        var conn = DBConnection.getConnection();
        if (conn == null) return r;
        try (conn) {
            var ps = conn.prepareStatement(
                "SELECT status, COUNT(*) as cnt FROM attendance WHERE student_username=? GROUP BY status");
            ps.setString(1, user.getUsername());
            var rs = ps.executeQuery();
            while (rs.next()) {
                String s = rs.getString("status");
                int    c = rs.getInt("cnt");
                if (s.equalsIgnoreCase("Present"))     r[0] = c;
                else if (s.equalsIgnoreCase("Absent")) r[1] = c;
                else if (s.equalsIgnoreCase("Late"))   r[2] = c;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return r;
    }

    private Button navBtn(String text, boolean active) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        applyNavStyle(b, active);
        b.setOnMouseEntered(e -> { if (b != activeBtn) applyNavStyle(b, true); });
        b.setOnMouseExited(e ->  { if (b != activeBtn) applyNavStyle(b, false); });
        return b;
    }

    private void setActive(Button btn) {
        if (activeBtn != null) applyNavStyle(activeBtn, false);
        activeBtn = btn;
        btn.setStyle(
            "-fx-background-color: " + Main.C_ACCENT + "22; -fx-text-fill: " + Main.C_ACCENT + ";" +
            "-fx-border-color: " + Main.C_ACCENT + "55; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 13px; -fx-padding: 10 14; -fx-cursor: hand; -fx-alignment: center-left;");
    }

    private void applyNavStyle(Button b, boolean hover) {
        if (hover)
            b.setStyle(
                "-fx-background-color: #ffffff11; -fx-text-fill: " + Main.C_TEXT + ";" +
                "-fx-border-color: transparent; -fx-border-radius: 8; -fx-background-radius: 8;" +
                "-fx-font-size: 13px; -fx-padding: 10 14; -fx-cursor: hand; -fx-alignment: center-left;");
        else
            b.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: " + Main.C_MUTED + ";" +
                "-fx-border-color: transparent; -fx-border-radius: 8; -fx-background-radius: 8;" +
                "-fx-font-size: 13px; -fx-padding: 10 14; -fx-cursor: hand; -fx-alignment: center-left;");
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle(
            "-fx-text-fill: #3a5a70; -fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-padding: 10 14 4 14;");
        return l;
    }

    private String roleColor() {
        return switch (user.getRole()) {
            case "Admin"      -> Main.C_DANGER;
            case "Instructor" -> Main.C_WARNING;
            default           -> Main.C_SUCCESS;
        };
    }
}