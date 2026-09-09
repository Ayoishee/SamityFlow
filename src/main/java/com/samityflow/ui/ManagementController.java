package com.samityflow.ui;

import com.samityflow.AppContext;
import com.samityflow.model.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

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
        TextField name = new TextField(); name.setPromptText("Samity name");
        TextField day = new TextField(); day.setPromptText("Meeting day");
        Button add = new Button("Add samity");
        add.setOnAction(event -> run(() -> { context.samities.save(name.getText().trim(), day.getText().trim()); name.clear(); day.clear(); refresh(); }));
        Button toggle = new Button("Activate / deactivate");
        toggle.setOnAction(event -> run(() -> { Samity s = selected(samityList, "Select a samity"); context.samities.setActive(s.id(), !s.active()); refresh(); }));
        return column("Samities", samityList, name, day, add, toggle);
    }

    private VBox groupBox() {
        TextField name = new TextField(); name.setPromptText("Group name");
        Button add = new Button("Add group");
        add.setOnAction(event -> run(() -> { Samity s = selected(samityChoice, "Choose a samity"); context.groups.save(s.id(), name.getText().trim()); name.clear(); refresh(); }));
        return column("Group units", groupList, samityChoice, name, add);
    }

    private VBox memberBox() {
        TextField name = new TextField(); name.setPromptText("Member name");
        TextField phone = new TextField(); phone.setPromptText("Phone");
        Button add = new Button("Add member");
        add.setOnAction(event -> run(() -> { GroupUnit g = selected(groupChoice, "Choose a group"); context.members.save(g.id(), name.getText().trim(), phone.getText().trim()); name.clear(); phone.clear(); refresh(); }));
        Button check = new Button("Check eligibility");
        check.setOnAction(event -> run(() -> { Member m = selected(memberList, "Select a member"); boolean ok = new com.samityflow.service.EligibilityService(context.members).isEligible(m); Ui.info(m.name() + (ok ? " is eligible" : " is not eligible")); }));
        return column("Members", memberList, groupChoice, name, phone, add, check);
    }

    public void refresh() {
        samityList.setItems(FXCollections.observableArrayList(context.samities.findAll()));
        groupList.setItems(FXCollections.observableArrayList(context.groups.findAll()));
        memberList.setItems(FXCollections.observableArrayList(context.members.findAll()));
        samityChoice.setItems(FXCollections.observableArrayList(context.samities.findAll()));
        groupChoice.setItems(FXCollections.observableArrayList(context.groups.findAll()));
    }

    private VBox column(String title, Node... nodes) {
        VBox box = new VBox(8, new Label(title)); box.getChildren().addAll(nodes); box.setPrefWidth(300);
        for (Node node : nodes) if (node instanceof ListView<?>) VBox.setVgrow(node, Priority.ALWAYS);
        return box;
    }
    private <T> T selected(ListView<T> list, String message) { T value=list.getSelectionModel().getSelectedItem(); if(value==null)throw new IllegalArgumentException(message); return value; }
    private <T> T selected(ComboBox<T> combo, String message) { T value=combo.getValue(); if(value==null)throw new IllegalArgumentException(message); return value; }
    private void run(Runnable action) { try { action.run(); } catch (Exception e) { Ui.error(e); } }
}
