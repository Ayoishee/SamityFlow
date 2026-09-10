package com.samityflow.ui;

import com.samityflow.AppContext;
import com.samityflow.model.ApplicationStatus;
import com.samityflow.model.LoanApplication;
import com.samityflow.service.CollectionResult;
import com.samityflow.service.ReportService.InstallmentRow;
import com.samityflow.service.ReportService.PaymentRow;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ServicingController {
    private final AppContext context;
    private final ComboBox<LoanApplication> approvedApplications = new ComboBox<>();
    private final ComboBox<InstallmentRow> installments = new ComboBox<>();
    private final ListView<PaymentRow> payments = new ListView<>();

    public ServicingController(AppContext context) { this.context = context; }

    public Node view() {
        VBox page = Ui.page("Disbursement & Weekly Collection");

        DatePicker firstDueDate = new DatePicker(LocalDate.now().plusWeeks(1));
        ComboBox<String> schedule = new ComboBox<>(FXCollections.observableArrayList("WEEKLY", "GRACE", "SEASONAL"));
        schedule.setValue("WEEKLY"); approvedApplications.setPromptText("Approved application");
        Button disburse = new Button("Disburse");
        disburse.setOnAction(event -> run(() -> {
            LoanApplication application = required(approvedApplications, "Choose an approved application");
            context.disbursementService.disburse(application.getId(), firstDueDate.getValue(), schedule.getValue());
            Ui.info("Loan disbursed and repayment schedule created");
        }));

        installments.setPromptText("Outstanding installment"); installments.setPrefWidth(440);
        TextField paymentAmount = field("Payment amount");
        TextField savingsAmount = field("Savings amount (optional)");
        TextField reference = field("Unique collection reference");
        Button collect = new Button("Post collection");
        collect.setOnAction(event -> run(() -> {
            InstallmentRow item = required(installments, "Choose an installment");
            BigDecimal savings = savingsAmount.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(savingsAmount.getText());
            CollectionResult result = context.collectionService.collectPayment(
                    item.installmentId(), item.loanId(), item.memberId(), new BigDecimal(paymentAmount.getText()),
                    savings, LocalDate.now(), required(reference));
            if (!result.isSuccess()) throw new IllegalStateException(result.getMessage());
            Ui.info(result.getMessage());
        }));

        Button reverse = new Button("Reverse selected payment");
        reverse.setOnAction(event -> run(() -> {
            PaymentRow payment = payments.getSelectionModel().getSelectedItem();
            if (payment == null) throw new IllegalArgumentException("Select a payment");
            context.reversalService.reversePayment(payment.paymentId());
            Ui.info("Payment reversed; linked savings was also corrected");
        }));

        page.getChildren().addAll(
                new Label("1. Disburse an approved application"),
                new HBox(8, approvedApplications, firstDueDate, schedule, disburse),
                new Separator(), new Label("2. Post installment and savings collection"),
                installments, new HBox(8, paymentAmount, savingsAmount, reference, collect),
                new Separator(), new Label("3. Audited payment correction"), payments, reverse);
        VBox.setVgrow(payments, Priority.ALWAYS);
        refresh();
        return page;
    }

    public void refresh() {
        approvedApplications.setItems(FXCollections.observableArrayList(
                context.applications.findAll().stream()
                        .filter(a -> a.getStatus() == ApplicationStatus.APPROVED).toList()));
        installments.setItems(FXCollections.observableArrayList(context.reportService.outstandingInstallments()));
        payments.setItems(FXCollections.observableArrayList(context.reportService.recentPayments()));
    }

    private void run(Runnable action) { try { action.run(); refresh(); } catch (Exception e) { Ui.error(e); } }
    private TextField field(String prompt) { TextField f=new TextField();f.setPromptText(prompt);return f; }
    private String required(TextField f) { if(f.getText().isBlank())throw new IllegalArgumentException(f.getPromptText()+" is required");return f.getText().trim(); }
    private <T>T required(ComboBox<T> box,String message){if(box.getValue()==null)throw new IllegalArgumentException(message);return box.getValue();}
}
