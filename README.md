# Smart Academic Management System (AMS)

## Overview
The Smart Academic Management System (AMS) is a JavaFX desktop application that manages academic workflows including attendance tracking, reporting, and notifications.

---

## Technologies Used
- Java (JDK 17)
- JavaFX
- SQL Server
- JDBC
- iText (PDF)
- JavaMail API

---

## Project Structure
AMS/
│
├── src/
│   ├── Main.java
│   ├── DBConnection.java
│   │
│   ├── modules/
│   │   ├── AdminRegistrationModule.java
│   │   ├── CourseEnrollmentModule.java
│   │   ├── AttendanceModule.java
│   │   ├── EditAttendanceView.java
│   │   ├── StudentAttendanceView.java
│   │   ├── AttendanceCalendarView.java
│   │   ├── AttendanceReportView.java
│   │   ├── AuditLogView.java
│   │   ├── EmailNotificationView.java
│   │   └── NotificationPanel.java
│   │
│   ├── services/
│   │   └── AttendanceAlertService.java
│   │
│   └── utils/
│       └── Helpers.java
│
├── database/
│   └── setup_database.sql
│
├── resources/
│   ├── styles.css
│   └── icons/
│
└── README.md

---

## How to Run

1. Setup Database:
   Run setup_database.sql in SQL Server

2. Configure DB:
   Update DBConnection.java

3. Run:
   Execute Main.java

---

## Features
- Authentication (RBAC)
- Attendance marking & editing
- Notifications (in-app + email)
- Reports (CSV/PDF)
- Analytics dashboard

---

## Default Credentials
Admin: admin / admin123
