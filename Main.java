package ams;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import java.sql.*;

public class Main extends Application {

    private Stage primaryStage;
    private static HostServices hostServices;

    // ── Colour palette ───────────────────────────────────────────────────────
    static final String C_BG      = "#0f1923";
    static final String C_PANEL   = "#162030";
    static final String C_ACCENT  = "#00c2cb";
    static final String C_ACCENT2 = "#0076ff";
    static final String C_TEXT    = "#e8edf2";
    static final String C_MUTED   = "#8aa8bc";   // lightened so it's readable
    static final String C_SUCCESS = "#00c48c";
    static final String C_WARNING = "#ffb547";
    static final String C_DANGER  = "#ff4d6d";
    static final String C_SIDEBAR = "#111d2b";

    @Override
    public void start(Stage stage) {

        this.primaryStage = stage;
        this.hostServices = getHostServices();

        stage.setTitle("Smart Academic Management System");
        stage.setMinWidth(420);
        stage.setMinHeight(580);

        showLogin();
    }

    // SAFE getter (non-static use recommended)
    public HostServices getAppHostServices() {
        return hostServices;
    }

    // ════════════════════════════════════════════════════════════════════════
    // LOGIN SCREEN
    // ════════════════════════════════════════════════════════════════════════
    void showLogin() {
        HBox root = new HBox();
        root.setMinSize(860, 560);

        StackPane leftPanel = buildLeftPanel();
        leftPanel.setPrefWidth(380);
        HBox.setHgrow(leftPanel, Priority.NEVER);

        VBox rightPanel = buildLoginForm();
        rightPanel.setPrefWidth(480);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        root.getChildren().addAll(leftPanel, rightPanel);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private StackPane buildLeftPanel() {
        StackPane panel = new StackPane();
        panel.setStyle("-fx-background-color: " + C_BG + ";");

        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(panel.widthProperty());
        bg.heightProperty().bind(panel.heightProperty());
        bg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#0f1923")),
            new Stop(1, Color.web("#0d2137"))));

        Circle c1 = new Circle(120, Color.web(C_ACCENT, 0.07));
        Circle c2 = new Circle(80,  Color.web(C_ACCENT2, 0.1));
        Circle c3 = new Circle(50,  Color.web(C_ACCENT, 0.12));
        StackPane.setMargin(c1, new Insets(-180, 0, 0, -100));
        StackPane.setMargin(c2, new Insets(200, 0, 0, 200));
        StackPane.setMargin(c3, new Insets(0, 0, 160, 0));

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(60, 40, 60, 40));

        StackPane logoBox = new StackPane();
        Rectangle logoRect = new Rectangle(80, 80);
        logoRect.setArcWidth(20); logoRect.setArcHeight(20);
        logoRect.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(C_ACCENT)),
            new Stop(1, Color.web(C_ACCENT2))));
        logoRect.setEffect(new DropShadow(20, Color.web(C_ACCENT, 0.5)));
        // CHANGED: removed "A+" — now shows "AMS"
        Text logoText = new Text("AMS");
        logoText.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        logoText.setFill(Color.WHITE);
        logoBox.getChildren().addAll(logoRect, logoText);

        Text appName = new Text("Smart Academic");
        appName.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        appName.setFill(Color.web(C_TEXT));

        Text appSub = new Text("Management System");
        appSub.setFont(Font.font("Georgia", 16));
        appSub.setFill(Color.web(C_ACCENT));

        Separator sep = new Separator();
        sep.setMaxWidth(160);
        sep.setStyle("-fx-background-color: " + C_ACCENT + "; opacity: 0.4;");

        Text tagline = new Text("Empowering academic excellence\nthrough intelligent management.");
        tagline.setFont(Font.font("System", 13));
        tagline.setFill(Color.web(C_MUTED));
        tagline.setTextAlignment(TextAlignment.CENTER);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER);
        for (String[] badge : new String[][]{
            {"Admin", C_DANGER}, {"Instructor", C_WARNING}, {"Student", C_SUCCESS}}) {
            Label l = new Label(badge[0]);
            l.setStyle("-fx-background-color: " + badge[1] + "22; -fx-text-fill: " + badge[1] + ";" +
                "-fx-border-color: " + badge[1] + "55; -fx-border-radius: 20; -fx-background-radius: 20;" +
                "-fx-padding: 4 12; -fx-font-size: 11px;");
            badges.getChildren().add(l);
        }

        content.getChildren().addAll(logoBox, appName, appSub, sep, tagline, badges);
        panel.getChildren().addAll(bg, c1, c2, c3, content);

        FadeTransition ft = new FadeTransition(Duration.millis(800), panel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        return panel;
    }

    private VBox buildLoginForm() {
        VBox panel = new VBox(0);
        panel.setAlignment(Pos.CENTER);
        panel.setStyle("-fx-background-color: " + C_PANEL + ";");
        panel.setPadding(new Insets(60, 60, 60, 60));

        Text heading = new Text("Welcome Back");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        heading.setFill(Color.web(C_TEXT));

        Text subheading = new Text("Sign in to your account");
        subheading.setFont(Font.font("System", 14));
        subheading.setFill(Color.web(C_MUTED));

        VBox header = new VBox(6, heading, subheading);
        header.setAlignment(Pos.CENTER_LEFT);

        Label userLbl = fieldLabel("Username");
        TextField userField = styledField("Enter your username", false);

        Label passLbl = fieldLabel("Password");
        PasswordField passField = (PasswordField) styledField("Enter your password", true);

        Label roleLbl = fieldLabel("Role");
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Admin", "Instructor", "Student");
        roleBox.setPromptText("Select your role");
        roleBox.setMaxWidth(Double.MAX_VALUE);
        roleBox.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-text-fill: " + C_TEXT + "; -fx-font-size: 13px; -fx-pref-height: 42px;");

        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill: " + C_DANGER + "; -fx-font-size: 12px;");
        errorLbl.setVisible(false);

        Button loginBtn = new Button("Sign In");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, " + C_ACCENT + ", " + C_ACCENT2 + ");" +
            "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-pref-height: 44px; -fx-cursor: hand;");

        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, " + C_ACCENT2 + ", " + C_ACCENT + ");" +
            "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-pref-height: 44px; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, " + C_ACCENT + "88, 12, 0, 0, 0);"));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, " + C_ACCENT + ", " + C_ACCENT2 + ");" +
            "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-pref-height: 44px; -fx-cursor: hand;"));

        loginBtn.setOnAction(e -> handleLogin(
            userField.getText().trim(),
            passField.getText(),
            roleBox.getValue(),
            errorLbl));

        passField.setOnAction(e -> loginBtn.fire());
        userField.setOnAction(e -> passField.requestFocus());

        HBox divider = new HBox(10);
        divider.setAlignment(Pos.CENTER);
        Separator s1 = new Separator(); HBox.setHgrow(s1, Priority.ALWAYS);
        Label orLbl = new Label("or");
        orLbl.setStyle("-fx-text-fill: " + C_MUTED + "; -fx-font-size: 11px;");
        Separator s2 = new Separator(); HBox.setHgrow(s2, Priority.ALWAYS);
        divider.getChildren().addAll(s1, orLbl, s2);

        Label hintLbl = new Label("Default: admin / admin123");
        hintLbl.setStyle("-fx-text-fill: " + C_MUTED + "; -fx-font-size: 11px;");
        hintLbl.setAlignment(Pos.CENTER);

        VBox form = new VBox(10);
        form.getChildren().addAll(
            header, vspace(12),
            userLbl, userField, vspace(4),
            passLbl, passField, vspace(4),
            roleLbl, roleBox,   vspace(6),
            errorLbl,           vspace(4),
            loginBtn,           vspace(8),
            divider,            vspace(4),
            hintLbl);

        panel.getChildren().add(form);

        TranslateTransition tt = new TranslateTransition(Duration.millis(500), panel);
        tt.setFromX(40); tt.setToX(0);
        FadeTransition ft = new FadeTransition(Duration.millis(500), panel);
        ft.setFromValue(0); ft.setToValue(1);
        new ParallelTransition(tt, ft).play();

        return panel;
    }

    // ── Login handler ────────────────────────────────────────────────────────
    private void handleLogin(String username, String password,
                             String selectedRole, Label errorLbl) {
        if (username.isEmpty() || password.isEmpty() || selectedRole == null) {
            showError(errorLbl, "Please fill in all fields.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                showError(errorLbl, "Cannot connect to database.");
                return;
            }

            PreparedStatement lockCheck = conn.prepareStatement(
                "SELECT is_locked FROM users WHERE username = ?");
            lockCheck.setString(1, username);
            ResultSet lrs = lockCheck.executeQuery();
            if (lrs.next() && lrs.getBoolean("is_locked")) {
                showError(errorLbl, "Account locked. Contact admin.");
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, role FROM users WHERE username = ? AND password = ?");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String dbRole = rs.getString("role");
                int    userId = rs.getInt("id");

                if (!dbRole.equalsIgnoreCase(selectedRole)) {
                    showError(errorLbl, "Role mismatch. You are registered as: " + dbRole);
                    return;
                }

                PreparedStatement reset = conn.prepareStatement(
                    "UPDATE users SET failed_attempts=0 WHERE username=?");
                reset.setString(1, username);
                reset.executeUpdate();

                PreparedStatement log = conn.prepareStatement(
                    "INSERT INTO audit_log (action_by, action_type, description) VALUES (?,?,?)");
                log.setString(1, username);
                log.setString(2, "LOGIN");
                log.setString(3, "User logged in as " + dbRole);
                log.executeUpdate();

                showDashboard(new User(userId, username, dbRole));

            } else {
                PreparedStatement inc = conn.prepareStatement(
                    "UPDATE users SET failed_attempts = failed_attempts + 1 WHERE username = ?");
                inc.setString(1, username);
                inc.executeUpdate();

                PreparedStatement lockUpdate = conn.prepareStatement(
                    "UPDATE users SET is_locked = 1 WHERE username = ? AND failed_attempts >= 5");
                lockUpdate.setString(1, username);
                lockUpdate.executeUpdate();

                showError(errorLbl, "Invalid username, password, or role.");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            showError(errorLbl, "Database error: " + ex.getMessage());
        }
    }

    private void showError(Label lbl, String msg) {
        lbl.setText("⚠  " + msg);
        lbl.setVisible(true);
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), lbl);
        shake.setFromX(-6); shake.setToX(6); shake.setCycleCount(4);
        shake.setAutoReverse(true); shake.play();
    }

    void showDashboard(User user) {
        DashboardView dashboard = new DashboardView(primaryStage, user, this);
        Scene scene = new Scene(dashboard, 1100, 700);
        primaryStage.setScene(scene);
        // CHANGED: removed "A+" from window title
        primaryStage.setTitle("Academic Management System — " + user.getRole() + " Portal");
        primaryStage.centerOnScreen();
    }

    // ── Shared helpers ───────────────────────────────────────────────────────
    static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + C_MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    static TextField styledField(String prompt, boolean isPassword) {
        TextField f = isPassword ? new PasswordField() : new TextField();
        f.setPromptText(prompt);
        f.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-text-fill: " + C_TEXT + "; -fx-prompt-text-fill: " + C_MUTED + ";" +
            "-fx-font-size: 13px; -fx-pref-height: 42px; -fx-padding: 0 12;");
        f.focusedProperty().addListener((obs, old, focused) -> {
            if (focused)
                f.setStyle(f.getStyle().replace("-fx-border-color: #2a4060;",
                                                "-fx-border-color: " + C_ACCENT + ";"));
            else
                f.setStyle(f.getStyle().replace("-fx-border-color: " + C_ACCENT + ";",
                                                "-fx-border-color: #2a4060;"));
        });
        return f;
    }

    static Region vspace(double h) {
        Region r = new Region(); r.setPrefHeight(h); return r;
    }

    public static void main(String[] args) { launch(args); }
}