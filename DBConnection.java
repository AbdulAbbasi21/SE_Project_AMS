package ams;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL =
        "jdbc:sqlserver://localhost:1433;databaseName=ams_db;" +
        "encrypt=true;trustServerCertificate=true;";

    private static final String USER = "admin";
    private static final String PASS = "admin123";

    public static Connection getConnection() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            System.err.println("DB Connection Failed: " + e.getMessage());
            return null;
        }
    }
}