package ams;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class AssignmentRow {

    private SimpleIntegerProperty id;
    private SimpleStringProperty title;
    private SimpleStringProperty status;
    private SimpleStringProperty deadline;
    private SimpleStringProperty course;

    public AssignmentRow(int id, String title, String status, String deadline, String course) {
        this.id       = new SimpleIntegerProperty(id);
        this.title    = new SimpleStringProperty(title);
        this.status   = new SimpleStringProperty(status);
        this.deadline = new SimpleStringProperty(deadline);
        this.course   = new SimpleStringProperty(course);
    }

    // Backward-compatible constructor (no deadline/course)
    public AssignmentRow(int id, String title, String status) {
        this(id, title, status, "", "");
    }

    public int    getId()       { return id.get(); }

    // Plain getters — used directly in code (e.g. row.getStatus())
    public String getStatus()   { return status.get(); }
    public String getTitle()    { return title.get(); }
    public String getDeadline() { return deadline.get(); }
    public String getCourse()   { return course.get(); }

    // Property accessors — used by TableView cell value factories
    public SimpleStringProperty titleProperty()    { return title; }
    public SimpleStringProperty statusProperty()   { return status; }
    public SimpleStringProperty deadlineProperty() { return deadline; }
    public SimpleStringProperty courseProperty()   { return course; }
}