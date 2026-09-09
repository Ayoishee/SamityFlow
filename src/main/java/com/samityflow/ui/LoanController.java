package com.samityflow.ui;

import com.samityflow.AppContext;
import com.samityflow.model.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LoanController {
    private final AppContext context;
    private final ComboBox<Member> applicant=new ComboBox<>();
    private final ComboBox<LoanProduct> product=new ComboBox<>();
    private final ComboBox<Member> guarantor=new ComboBox<>();
    private final ListView<LoanApplication> applications=new ListView<>();
    private final Label details=new Label("Select an application");
    public LoanController(AppContext context){this.context=context;}

    public Node view(){
        VBox page=Ui.page("Loan Application & Approval");
        TextField amount=field("Requested amount");TextField purpose=field("Purpose");
        applicant.setPromptText("Applicant");product.setPromptText("Loan product");guarantor.setPromptText("Guarantor");
        Button create=new Button("Create application");
        create.setOnAction(e->run(()->{LoanApplication a=context.loanService.createAndSubmit(selected(applicant,"Choose an applicant").id(),selected(product,"Choose a product").id(),Double.parseDouble(amount.getText()),purpose.getText());Ui.info("Created application #"+a.getId());amount.clear();purpose.clear();refresh();}));
        Button calculate=new Button("Preview total");
        calculate.setOnAction(e->run(()->{LoanProduct p=selected(product,"Choose a product");double value=Double.parseDouble(amount.getText());Ui.info(String.format("Interest: %.2f%nTotal payable: %.2f",context.calculationService.interest(p,value),context.calculationService.totalPayable(p,value)));}));
        HBox createRow=new HBox(8,applicant,product,amount,purpose,calculate,create);

        applications.getSelectionModel().selectedItemProperty().addListener((obs,oldValue,newValue)->showDetails(newValue));
        applications.setCellFactory(list->new ListCell<>(){protected void updateItem(LoanApplication a,boolean empty){super.updateItem(a,empty);setText(empty||a==null?null:"#"+a.getId()+"  "+memberName(a.getMemberId())+"  "+a.getAmount()+"  ["+a.getStatus()+"]");}});

        Button guarantee=new Button("Add guarantee");guarantee.setOnAction(e->run(()->{context.loanService.addGuarantee(selectedApplication().getId(),selected(guarantor,"Choose a guarantor").id());refresh();}));
        Button officer=new Button("Officer approve");officer.setOnAction(e->run(()->{context.loanService.officerApprove(selectedApplication().getId());refresh();}));
        TextField comment=field("Manager comment / rejection reason");
        Button approve=new Button("Manager approve");approve.setOnAction(e->run(()->{context.loanService.managerApprove(selectedApplication().getId(),comment.getText());refresh();}));
        Button reject=new Button("Reject");reject.setOnAction(e->run(()->{context.loanService.reject(selectedApplication().getId(),comment.getText());refresh();}));
        HBox actionRow=new HBox(8,guarantor,guarantee,officer,comment,approve,reject);
        details.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 10;");
        page.getChildren().addAll(new Label("1. Create"),createRow,new Label("2. Select an application and process it"),applications,details,actionRow);VBox.setVgrow(applications,Priority.ALWAYS);refresh();return page;
    }

    public void refresh(){
        applicant.setItems(FXCollections.observableArrayList(context.members.findAll()));
        guarantor.setItems(FXCollections.observableArrayList(context.members.findAll()));
        product.setItems(FXCollections.observableArrayList(context.products.findAll()));
        applications.setItems(FXCollections.observableArrayList(context.loanService.findAll()));
        showDetails(applications.getSelectionModel().getSelectedItem());
    }
    private void showDetails(LoanApplication a){if(a==null){details.setText("Select an application");return;}LoanProduct p=context.products.findById(a.getProductId()).orElse(null);int count=context.guarantees.countForApplication(a.getId());details.setText("Applicant: "+memberName(a.getMemberId())+" | Product: "+(p==null?"?":p.name())+" | Guarantees: "+count+"/"+(p==null?"?":p.requiredGuarantees())+" | Officer approved: "+a.isOfficerApproved()+" | Comment: "+a.getManagerComment());}
    private String memberName(int id){return context.members.findById(id).map(Member::name).orElse("Unknown");}
    private LoanApplication selectedApplication(){LoanApplication a=applications.getSelectionModel().getSelectedItem();if(a==null)throw new IllegalArgumentException("Select an application");return a;}
    private <T>T selected(ComboBox<T> box,String message){T value=box.getValue();if(value==null)throw new IllegalArgumentException(message);return value;}
    private TextField field(String prompt){TextField f=new TextField();f.setPromptText(prompt);return f;}
    private void run(Runnable action){try{action.run();Ui.info("Saved successfully");}catch(Exception e){Ui.error(e);}}
}
