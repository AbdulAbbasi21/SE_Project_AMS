package ams;

import java.sql.*;
import java.util.*;

/**
 * US-06 — Automatic attendance alert service.
 * US-06a — In-app notifications
 * US-06b — Email queue notifications
 * NEW    — Assignment published notifications
 */
public class AttendanceAlertService {

    public static final double WARNING_THRESHOLD = 75.0;

    // ── US-06: Check attendance and alert if below threshold ─────────────────
    public static void checkAndAlert(int courseId, String markedBy) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (conn) {
            // Get course name
            PreparedStatement coursePs = conn.prepareStatement(
                "SELECT course_name FROM courses WHERE id = ?");
            coursePs.setInt(1, courseId);
            ResultSet crs = coursePs.executeQuery();
            String courseName = crs.next() ? crs.getString("course_name") : "Unknown Course";

            // Get all enrolled students
            PreparedStatement enrolPs = conn.prepareStatement(
                "SELECT student_username FROM enrollments WHERE course_id = ?");
            enrolPs.setInt(1, courseId);
            ResultSet enrolRs = enrolPs.executeQuery();
            List<String> students = new ArrayList<>();
            while (enrolRs.next()) students.add(enrolRs.getString("student_username"));

            // Check each student's attendance %
            PreparedStatement statPs = conn.prepareStatement(
                "SELECT COUNT(*) AS total, " +
                "SUM(CASE WHEN status IN ('Present','Late') THEN 1 ELSE 0 END) AS attended " +
                "FROM attendance WHERE student_username = ? AND course_id = ?");

            for (String student : students) {
                statPs.setString(1, student);
                statPs.setInt(2, courseId);
                ResultSet statRs = statPs.executeQuery();
                if (!statRs.next()) continue;

                int total    = statRs.getInt("total");
                int attended = statRs.getInt("attended");
                if (total == 0) continue;

                double pct = (double) attended / total * 100.0;

                if (pct < WARNING_THRESHOLD) {
                    String msg = String.format(
                        "⚠ Your attendance in '%s' has dropped to %.0f%% " +
                        "(below the required 75%%). Please take corrective action.",
                        courseName, pct);

                    createInAppNotification(conn, student, courseId, msg, pct, "ATTENDANCE_ALERT");
                    queueEmailNotification(conn, student, courseId, courseName, pct);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── NEW: Notify all enrolled students when assignment is published ────────
    public static void notifyAssignmentPublished(int courseId, String assignmentTitle,
                                                  String deadline, String instructorUsername) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (conn) {
            // Get course name
            PreparedStatement coursePs = conn.prepareStatement(
                "SELECT course_name FROM courses WHERE id = ?");
            coursePs.setInt(1, courseId);
            ResultSet crs = coursePs.executeQuery();
            String courseName = crs.next() ? crs.getString("course_name") : "Unknown Course";

            // Get all enrolled students
            PreparedStatement enrolPs = conn.prepareStatement(
                "SELECT student_username FROM enrollments WHERE course_id = ?");
            enrolPs.setInt(1, courseId);
            ResultSet enrolRs = enrolPs.executeQuery();

            while (enrolRs.next()) {
                String student = enrolRs.getString("student_username");
                String msg = String.format(
                    "📝 New assignment published in '%s': \"%s\" — Due: %s",
                    courseName, assignmentTitle, deadline);
                createInAppNotification(conn, student, courseId, msg, -1, "ASSIGNMENT");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── NEW: Notify student when their assignment is graded ──────────────────
    public static void notifyGradePosted(String studentUsername, String assignmentTitle,
                                          int marks, String feedback) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (conn) {
            String msg = String.format(
                "📊 Your assignment \"%s\" has been graded: %d/100. Feedback: %s",
                assignmentTitle, marks,
                (feedback == null || feedback.isBlank()) ? "No feedback provided." : feedback);
            createInAppNotification(conn, studentUsername, null, msg, -1, "GRADE");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── US-06a: Insert or update in-app notification ─────────────────────────
    private static void createInAppNotification(
            Connection conn, String studentUsername,
            Integer courseId, String message,
            double pct, String type) throws SQLException {

        if (courseId != null) {
            // Check for existing unread notification of same type for same course
            PreparedStatement check = conn.prepareStatement(
                "SELECT id FROM notifications " +
                "WHERE student_username = ? AND course_id = ? AND type = ? AND is_read = 0");
            check.setString(1, studentUsername);
            check.setInt(2, courseId);
            check.setString(3, type);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                // Update existing instead of duplicating
                PreparedStatement upd = conn.prepareStatement(
                    "UPDATE notifications SET message = ?, attendance_pct = ?, created_at = GETDATE() " +
                    "WHERE student_username = ? AND course_id = ? AND type = ? AND is_read = 0");
                upd.setString(1, message);
                upd.setObject(2, pct < 0 ? null : pct);
                upd.setString(3, studentUsername);
                upd.setInt(4, courseId);
                upd.setString(5, type);
                upd.executeUpdate();
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO notifications (student_username, course_id, message, attendance_pct, is_read, type) " +
                "VALUES (?, ?, ?, ?, 0, ?)");
            ps.setString(1, studentUsername);
            ps.setInt(2, courseId);
            ps.setString(3, message);
            ps.setObject(4, pct < 0 ? null : pct);
            ps.setString(5, type);
            ps.executeUpdate();

        } else {
            // No course_id (e.g. grade notification)
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO notifications (student_username, message, is_read, type) " +
                "VALUES (?, ?, 0, ?)");
            ps.setString(1, studentUsername);
            ps.setString(2, message);
            ps.setString(3, type);
            ps.executeUpdate();
        }
    }

    // ── US-06b: Queue email notification ─────────────────────────────────────
    private static void queueEmailNotification(
            Connection conn, String studentUsername,
            int courseId, String courseName, double pct) throws SQLException {

        PreparedStatement emailPs = conn.prepareStatement(
            "SELECT email FROM users WHERE username = ?");
        emailPs.setString(1, studentUsername);
        ResultSet ers = emailPs.executeQuery();
        if (!ers.next()) return;
        String email = ers.getString("email");
        if (email == null || email.isBlank()) return;

        String subject = "Attendance Warning — " + courseName;
        String body = String.format(
            "Dear %s,\n\nYour attendance in '%s' has fallen to %.0f%%, " +
            "below the required 75%%.\n\nPlease contact your instructor immediately.\n\nAMS Team",
            studentUsername, courseName, pct);

        // Avoid duplicate pending emails
        PreparedStatement check = conn.prepareStatement(
            "SELECT id FROM email_queue WHERE recipient_username = ? AND course_id = ? AND is_sent = 0");
        check.setString(1, studentUsername);
        check.setInt(2, courseId);
        ResultSet rs = check.executeQuery();

        if (rs.next()) {
            PreparedStatement upd = conn.prepareStatement(
                "UPDATE email_queue SET subject=?, body=?, queued_at=GETDATE() " +
                "WHERE recipient_username=? AND course_id=? AND is_sent=0");
            upd.setString(1, subject); upd.setString(2, body);
            upd.setString(3, studentUsername); upd.setInt(4, courseId);
            upd.executeUpdate();
            return;
        }

        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO email_queue (recipient_username, recipient_email, course_id, subject, body, is_sent) " +
            "VALUES (?, ?, ?, ?, ?, 0)");
        ps.setString(1, studentUsername);
        ps.setString(2, email);
        ps.setInt(3, courseId);
        ps.setString(4, subject);
        ps.setString(5, body);
        ps.executeUpdate();
    }

    // ── Get unread count for bell badge ──────────────────────────────────────
    public static int getUnreadCount(String studentUsername) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return 0;
        try (conn) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM notifications WHERE student_username = ? AND is_read = 0");
            ps.setString(1, studentUsername);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) { e.printStackTrace(); return 0; }
    }

    // ── Mark all notifications as read ────────────────────────────────────────
    public static void markAllRead(String studentUsername) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (conn) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE notifications SET is_read = 1 WHERE student_username = ?");
            ps.setString(1, studentUsername);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}