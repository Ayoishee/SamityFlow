package com.samityflow.ui;

import com.samityflow.AppContext;
import com.samityflow.service.ReportService.CollectionSummary;
import com.samityflow.service.ReportService.PortfolioSummary;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class ReportsController {
    private final AppContext context;
    private final Label portfolio = new Label();
    private final Label collection = new Label();
    private final ListView<String> members = new ListView<>();
    private final ListView<String> audit = new ListView<>();

    public ReportsController(AppContext context) { this.context = context; }

    public Node view() {
        VBox page=Ui.page("Reports & Overdue Monitoring");
        DatePicker date=new DatePicker(LocalDate.now());Button scan=new Button("Run overdue scan");
        scan.setOnAction(e->{try{int count=context.overdueService.scan(date.getValue());Ui.info(count+" overdue installment(s) updated");refresh();}catch(Exception ex){Ui.error(ex);}});
        page.getChildren().addAll(new HBox(8,date,scan),portfolio,collection,
                new Label("Member loan and savings statement"),members,
                new Label("Recent audit history"), audit);
        VBox.setVgrow(members,Priority.ALWAYS);
        VBox.setVgrow(audit,Priority.ALWAYS);refresh();return page;
    }

    public void refresh(){PortfolioSummary p=context.reportService.portfolioSummary();CollectionSummary c=context.reportService.collectionSummary();portfolio.setText("Active/defaulted loans: "+p.activeLoans()+" | Outstanding: "+p.outstanding()+" | Overdue installments: "+p.overdueInstallments()+" | Savings: "+p.savingsBalance());collection.setText(String.format("Expected: %s | Collected: %s | Collection rate: %.2f%%",c.expected(),c.collected(),c.ratePercent()));members.setItems(FXCollections.observableArrayList(context.reportService.memberFinancialReport().stream().map(Object::toString).toList()));audit.setItems(FXCollections.observableArrayList(context.reportService.recentAuditHistory().stream().map(Object::toString).toList()));}
}
