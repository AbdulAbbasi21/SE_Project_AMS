package ams;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDB {
    public static void main(String[] args) {
        System.out.println("Testing Connection...");
        
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                System.out.println("✅ SUCCESS! Connected to SSMS.");
                
                // Let's try to read the test user we inserted earlier
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT username FROM users");
                
                while (rs.next()) {
                    System.out.println("Found User in DB: " + rs.getString("username"));
                }
            } else {
                System.out.println("❌ FAILED! Connection was null.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}