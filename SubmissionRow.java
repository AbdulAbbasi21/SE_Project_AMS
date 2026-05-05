package ams;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class SubmissionRow {

    private final SimpleIntegerProperty id;
    private final SimpleStringProperty  student;
    private final SimpleStringProperty  file;
    private final SimpleStringProperty  marks;
    private final SimpleStringProperty  feedback;
    private final SimpleStringProperty  submittedAt;

    public SubmissionRow(int id, String student, String file) {
        this.id          = new SimpleIntegerProperty(id);
        this.student     = new SimpleStringProperty(student);
        this.file        = new SimpleStringProperty(file == null ? "" : file);
        this.marks       = new SimpleStringProperty("Not graded");
        this.feedback    = new SimpleStringProperty("");
        this.submittedAt = new SimpleStringProperty("");
    }

    public SubmissionRow(int id, String student, String file, String marks, String feedback, String submittedAt) {
        this.id          = new SimpleIntegerProperty(id);
        this.student     = new SimpleStringProperty(student);
        this.file        = new SimpleStringProperty(file == null ? "" : file);
        this.marks       = new SimpleStringProperty(marks == null ? "Not graded" : marks);
        this.feedback    = new SimpleStringProperty(feedback == null ? "" : feedback);
        this.submittedAt = new SimpleStringProperty(submittedAt == null ? "" : submittedAt);
    }

    public int getId()                               { return id.get(); }
    public SimpleStringProperty studentProperty()    { return student; }
    public SimpleStringProperty fileProperty()       { return file; }
    public SimpleStringProperty marksProperty()      { return marks; }
    public SimpleStringProperty feedbackProperty()   { return feedback; }
    public SimpleStringProperty submittedAtProperty(){ return submittedAt; }
}