package ams;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.sql.*;

public class AdminRegistrationModule extends VBox {

    private final User admin;
    private TableView<String[]> userTable;
    private ObservableList<String[]> userData = FXCollections.observableArrayList();

    public AdminRegistrationModule(User admin) {
        this.admin = admin;
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setStyle("-fx-background-color: " + Main.C_BG + ";");
        build();
    }

    private void build() {
        Text title = new Text("User Management");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(Color.web(Main.C_TEXT));

        // ── Register form ──────────────────────────────────────────────────
        Label formLbl = new Label("Add New User");
        formLbl.setStyle(
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        TextField usernameField = (TextField) Main.styledField("Username", false);
        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");
        passField.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-prompt-text-fill: " + Main.C_MUTED + ";" +
            "-fx-font-size: 13px; -fx-pref-height: 42px;");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Admin", "Instructor", "Student");
        roleBox.setPromptText("Select Role");
        roleBox.setMaxWidth(Double.MAX_VALUE);
        roleBox.setStyle(
            "-fx-background-color: #1e2f42; -fx-border-color: #2a4060;" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-pref-height: 42px;");

        Button addBtn = new Button("Add User");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setStyle(
            "-fx-background-color: " + Main.C_ACCENT + "33; -fx-text-fill: " + Main.C_ACCENT + ";" +
            "-fx-border-color: " + Main.C_ACCENT + "55; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-font-size: 13px; -fx-padding: 10; -fx-cursor: hand;");

        Label feedbackLbl = new Label("");
        feedbackLbl.setStyle("-fx-font-size: 12px;");

        addBtn.setOnAction(e -> {
            String uname = usernameField.getText().trim();
            String pass  = passField.getText();
            String role  = roleBox.getValue();

            if (uname.isEmpty() || pass.isEmpty() || role == null) {
                feedbackLbl.setStyle("-fx-text-fill: " + Main.C_DANGER + ";");
                feedbackLbl.setText("⚠  All fields are required.");
                return;
            }

            Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in AdminRegistrationModule.java"); return; }
            try (conn) {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password, role) VALUES (?, ?, ?)");
                ps.setString(1, uname);
                ps.setString(2, pass);
                ps.setString(3, role);
                ps.executeUpdate();

                PreparedStatement log = conn.prepareStatement(
                    "INSERT INTO audit_log (action_by, action_type, description) VALUES (?,?,?)");
                log.setString(1, admin.getUsername());
                log.setString(2, "ADD_USER");
                log.setString(3, "Added new " + role + ": " + uname);
                log.executeUpdate();

                feedbackLbl.setStyle("-fx-text-fill: " + Main.C_SUCCESS + ";");
                feedbackLbl.setText("✅ User '" + uname + "' registered as " + role);
                usernameField.clear(); passField.clear(); roleBox.setValue(null);
                loadUsers();

            } catch (SQLIntegrityConstraintViolationException ex) {
                feedbackLbl.setStyle("-fx-text-fill: " + Main.C_DANGER + ";");
                feedbackLbl.setText("⚠  Username already exists.");
            } catch (Exception ex) {
                ex.printStackTrace();
                feedbackLbl.setStyle("-fx-text-fill: " + Main.C_DANGER + ";");
                feedbackLbl.setText("Error: " + ex.getMessage());
            }
        });

        VBox formBox = new VBox(10,
            formLbl,
            Main.fieldLabel("USERNAME"), usernameField,
            Main.fieldLabel("PASSWORD"), passField,
            Main.fieldLabel("ROLE"),     roleBox,
            addBtn, feedbackLbl);
        formBox.setPadding(new Insets(20));
        formBox.setStyle(
            "-fx-background-color: " + Main.C_PANEL + "; -fx-background-radius: 12;" +
            "-fx-border-color: #2a4060; -fx-border-radius: 12;");
        formBox.setMaxWidth(380);

        // ── User table ─────────────────────────────────────────────────────
        Label tableLbl = new Label("Registered Users");
        tableLbl.setStyle(
            "-fx-text-fill: " + Main.C_TEXT + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        userTable = new TableView<>(userData);
        applyDarkTableStyle(userTable);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(userTable, Priority.ALWAYS);

        TableColumn<String[], String> idCol   = tcol("ID",       0, Main.C_MUTED);
        TableColumn<String[], String> nameCol = tcol("Username", 1, Main.C_TEXT);

        TableColumn<String[], String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(
            p -> new javafx.beans.property.SimpleStringProperty(p.getValue()[2]));
        roleCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + Main.C_PANEL + ";");
                    return;
                }
                setText(item);
                String color = switch (item) {
                    case "Admin"      -> Main.C_DANGER;
                    case "Instructor" -> Main.C_WARNING;
                    default           -> Main.C_SUCCESS;
                };
                setStyle(
                    "-fx-text-fill: "        + color        + ";" +
                    "-fx-font-weight: bold;"                      +
                    "-fx-background-color: " + Main.C_PANEL + ";");
            }
        });

        TableColumn<String[], String> lockedCol = tcol("Status", 3, Main.C_TEXT);
        userTable.getColumns().addAll(idCol, nameCol, roleCol, lockedCol);

        Button unlockBtn = new Button("Unlock Selected User");
        unlockBtn.setStyle(
            "-fx-background-color: " + Main.C_WARNING + "33; -fx-text-fill: " + Main.C_WARNING + ";" +
            "-fx-border-color: " + Main.C_WARNING + "55; -fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 8 16; -fx-cursor: hand;");
        unlockBtn.setOnAction(e -> {
            String[] selected = userTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in AdminRegistrationModule.java"); return; }
            try (conn) {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET is_locked=0, failed_attempts=0 WHERE id=?");
                ps.setInt(1, Integer.parseInt(selected[0]));
                ps.executeUpdate();
                feedbackLbl.setStyle("-fx-text-fill: " + Main.C_SUCCESS + ";");
                feedbackLbl.setText("✅ Account unlocked.");
                loadUsers();
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        loadUsers();

        HBox content = new HBox(24, formBox,
            new VBox(10, tableLbl, userTable, unlockBtn));
        HBox.setHgrow(content.getChildren().get(1), Priority.ALWAYS);
        VBox.setVgrow(content, Priority.ALWAYS);

        this.getChildren().addAll(title, new Separator(), content);
    }

    private void loadUsers() {
        userData.clear();
        Connection conn = DBConnection.getConnection();
            if (conn == null) { System.err.println("DB connection null in AdminRegistrationModule.java"); return; }
            try (conn) {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT id, username, role, is_locked FROM users ORDER BY role, username");
            while (rs.next()) {
                userData.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getBoolean("is_locked") ? "🔒 Locked" : "✓ Active"
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
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