package ams;

import javafx.beans.property.SimpleStringProperty;

public class GradeRow {

    private final SimpleStringProperty assignment;
    private final SimpleStringProperty course;
    private final SimpleStringProperty marks;
    private final SimpleStringProperty feedback;
    private final SimpleStringProperty gradedAt;

    public GradeRow(String assignment, String course, String marks, String feedback, String gradedAt) {
        this.assignment = new SimpleStringProperty(assignment);
        this.course     = new SimpleStringProperty(course == null ? "" : course);
        this.marks      = new SimpleStringProperty(marks == null ? "—" : marks);
        this.feedback   = new SimpleStringProperty(feedback == null ? "" : feedback);
        this.gradedAt   = new SimpleStringProperty(gradedAt == null ? "" : gradedAt);
    }

    // Backward-compatible 3-arg constructor
    public GradeRow(String assignment, String marks, String feedback) {
        this(assignment, "", marks, feedback, "");
    }

    public SimpleStringProperty assignmentProperty() { return assignment; }
    public SimpleStringProperty courseProperty()     { return course; }
    public SimpleStringProperty marksProperty()      { return marks; }
    public SimpleStringProperty feedbackProperty()   { return feedback; }
    public SimpleStringProperty gradedAtProperty()   { return gradedAt; }
}