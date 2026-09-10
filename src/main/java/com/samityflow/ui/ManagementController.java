package com.samityflow.ui;

import com.samityflow.AppContext;
import com.samityflow.model.GroupUnit;
import com.samityflow.model.Member;
import com.samityflow.model.Samity;
import com.samityflow.service.EligibilityService;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ManagementController {
    private final AppContext context;
    private final ListView<Samity> samityList = new ListView<>();
    private final ListView<GroupUnit> groupList = new ListView<>();
    private final ListView<Member> memberList = new ListView<>();
    private final ComboBox<Samity> samityChoice = new ComboBox<>();
    private final ComboBox<GroupUnit> groupChoice = new ComboBox<>();

    public ManagementController(AppContext context) { this.context = context; }

    public Node view() {
        VBox page = Ui.page("Samity & Member Management");
        HBox columns = new HBox(15, samityBox(), groupBox(), memberBox());
        HBox.setHgrow(columns, Priority.ALWAYS);
        page.getChildren().add(columns);
        refresh();
        return page;
    }

    private VBox samityBox() {
        TextField name = field("Samity name");
        TextField day = field("Meeting day");
        samityList.getSelectionModel().selectedItemProperty().addListener((o, old, item) -> {
            if (item != null) { name.setText(item.name()); day.setText(item.meetingDay()); }
        });
        Button add = button("Add", () -> context.samities.save(required(name), required(day)));
        Button update = button("Update", () -> {
            Samity item = selected(samityList, "Select a samity");
            context.samities.update(item.id(), required(name), required(day));
        });
        Button toggle = button("Activate / deactivate", () -> {
            Samity item = selected(samityList, "Select a samity");
            context.samities.setActive(item.id(), !item.active());
        });
        Button delete = button("Delete", () -> context.samities.delete(selected(samityList, "Select a samity").id()));
        return column("Samities", samityList, name, day, new HBox(6, add, update), toggle, delete);
    }

    private VBox groupBox() {
        TextField name = field("Group name");
        groupList.getSelectionModel().selectedItemProperty().addListener((o, old, item) -> {
            if (item != null) { name.setText(item.name()); selectSamity(item.samityId()); }
        });
        Button add = button("Add", () -> context.groups.save(selected(samityChoice, "Choose a samity").id(), required(name)));
        Button update = button("Update", () -> context.groups.update(
                selected(groupList, "Select a group").id(), selected(samityChoice, "Choose a samity").id(), required(name)));
        Button toggle = button("Activate / deactivate", () -> {
            GroupUnit item = selected(groupList, "Select a group"); context.groups.setActive(item.id(), !item.active());
        });
        Button delete = button("Delete", () -> context.groups.delete(selected(groupList, "Select a group").id()));
        return column("Group units", groupList, samityChoice, name, new HBox(6, add, update), toggle, delete);
    }

    private VBox memberBox() {
        TextField name = field("Member name");
        TextField phone = field("Phone");
        CheckBox eligible = new CheckBox("Eligible"); eligible.setSelected(true);
        CheckBox active = new CheckBox("Active"); active.setSelected(true);
        memberList.getSelectionModel().selectedItemProperty().addListener((o, old, item) -> {
            if (item != null) {
                name.setText(item.name()); phone.setText(item.phone());
                eligible.setSelected(item.eligible()); active.setSelected(item.active());
                selectGroup(item.groupUnitId());
            }
        });
        Button add = button("Add", () -> context.members.save(
                selected(groupChoice, "Choose a group").id(), required(name), required(phone)));
        Button update = button("Update", () -> context.members.update(
                selected(memberList, "Select a member").id(), selected(groupChoice, "Choose a group").id(),
                required(name), required(phone), eligible.isSelected(), active.isSelected()));
        Button check = new Button("Check eligibility");
        check.setOnAction(event -> run(() -> {
            Member item = selected(memberList, "Select a member");
            boolean valid = new EligibilityService(context.members).isEligible(item);
            Ui.info(item.name() + (valid ? " is eligible" : " is not eligible"));
        }));
        Button delete = button("Delete", () -> context.members.delete(selected(memberList, "Select a member").id()));
        return column("Members", memberList, groupChoice, name, phone,
                new HBox(8, eligible, active), new HBox(6, add, update), check, delete);
    }

    public void refresh() {
        var samities = FXCollections.observableArrayList(context.samities.findAll());
        var groups = FXCollections.observableArrayList(context.groups.findAll());
        samityList.setItems(samities); samityChoice.setItems(samities);
        groupList.setItems(groups); groupChoice.setItems(groups);
        memberList.setItems(FXCollections.observableArrayList(context.members.findAll()));
    }

    private Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(event -> run(() -> { action.run(); refresh(); }));
        return button;
    }
    private TextField field(String prompt) { TextField f = new TextField(); f.setPromptText(prompt); return f; }
    private String required(TextField field) { if(field.getText().isBlank())throw new IllegalArgumentException(field.getPromptText()+" is required");return field.getText().trim(); }
    private VBox column(String title, Node... nodes) { VBox box=new VBox(8,new Label(title));box.getChildren().addAll(nodes);box.setPrefWidth(310);for(Node n:nodes)if(n instanceof ListView<?>)VBox.setVgrow(n,Priority.ALWAYS);return box; }
    private <T>T selected(ListView<T> list,String message){T v=list.getSelectionModel().getSelectedItem();if(v==null)throw new IllegalArgumentException(message);return v;}
    private <T>T selected(ComboBox<T> box,String message){T v=box.getValue();if(v==null)throw new IllegalArgumentException(message);return v;}
    private void selectSamity(int id){samityChoice.getItems().stream().filter(x->x.id()==id).findFirst().ifPresent(samityChoice::setValue);}
    private void selectGroup(int id){groupChoice.getItems().stream().filter(x->x.id()==id).findFirst().ifPresent(groupChoice::setValue);}
    private void run(Runnable action){try{action.run();}catch(Exception e){Ui.error(e);}}
}
