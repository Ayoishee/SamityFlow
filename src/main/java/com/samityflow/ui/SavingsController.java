package com.samityflow.ui;

import com.samityflow.AppContext;
import com.samityflow.model.Member;
import com.samityflow.model.SavingsTransaction;
import com.samityflow.repository.SavingsTransactionRepository;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.Connection;

public class SavingsController {
    private final AppContext context;
    private final ComboBox<Member> members = new ComboBox<>();
    private final Label balance = new Label("Balance: 0");
    private final ListView<SavingsTransaction> transactions = new ListView<>();

    public SavingsController(AppContext context) { this.context = context; }

    public Node view() {
        VBox page=Ui.page("Savings Management");
        members.setPromptText("Member");members.setOnAction(e->refreshMember());
        TextField amount=field("Amount"), reference=field("Unique reference");
        Button deposit=new Button("Deposit");deposit.setOnAction(e->run(()->context.savingsService.deposit(member().id(),Double.parseDouble(amount.getText()),required(reference))));
        Button withdraw=new Button("Withdraw");withdraw.setOnAction(e->run(()->context.savingsService.withdraw(member().id(),Double.parseDouble(amount.getText()),required(reference))));
        balance.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        page.getChildren().addAll(new HBox(8,members,balance),new HBox(8,amount,reference,deposit,withdraw),new Label("Transaction history"),transactions);
        VBox.setVgrow(transactions,Priority.ALWAYS);refresh();return page;
    }

    public void refresh(){Member selected=members.getValue();members.setItems(FXCollections.observableArrayList(context.members.findAll()));if(selected!=null)members.getItems().stream().filter(m->m.id()==selected.id()).findFirst().ifPresent(members::setValue);refreshMember();}
    private void refreshMember(){if(members.getValue()==null){balance.setText("Balance: 0");transactions.getItems().clear();return;}balance.setText(String.format("Balance: %.2f",context.savingsService.balance(member().id())));try(Connection c=context.database.connect()){transactions.setItems(FXCollections.observableArrayList(new SavingsTransactionRepository(c).findByMember(member().id())));}catch(Exception e){Ui.error(e);}}
    private Member member(){if(members.getValue()==null)throw new IllegalArgumentException("Choose a member");return members.getValue();}
    private void run(Runnable action){try{action.run();refreshMember();}catch(Exception e){Ui.error(e);}}
    private TextField field(String prompt){TextField f=new TextField();f.setPromptText(prompt);return f;}
    private String required(TextField f){if(f.getText().isBlank())throw new IllegalArgumentException(f.getPromptText()+" is required");return f.getText().trim();}
}
