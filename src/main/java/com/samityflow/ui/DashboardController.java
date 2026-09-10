package com.samityflow.ui;

import com.samityflow.AppContext;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class DashboardController {
    private final AppContext context;
    private final Label members = new Label();
    private final Label samities = new Label();
    private final Label pending = new Label();
    private final Label guarantees = new Label();
    private final Label approved = new Label();
    private final Label activeLoans = new Label();
    private final Label overdue = new Label();
    private final Label savings = new Label();

    public DashboardController(AppContext context) { this.context = context; }

    public Node view() {
        VBox page = Ui.page("Dashboard");
        GridPane cards = new GridPane();
        cards.setHgap(25); cards.setVgap(20);
        cards.add(card("Total members", members), 0, 0);
        cards.add(card("Active samities", samities), 1, 0);
        cards.add(card("Pending applications", pending), 2, 0);
        cards.add(card("Awaiting guarantees", guarantees), 0, 1);
        cards.add(card("Approved loans", approved), 1, 1);
        cards.add(card("Active loans", activeLoans), 2, 1);
        cards.add(card("Overdue installments", overdue), 0, 2);
        cards.add(card("Total savings", savings), 1, 2);
        page.getChildren().add(cards);
        refresh();
        return page;
    }

    public void refresh() {
        members.setText(String.valueOf(context.members.count()));
        samities.setText(String.valueOf(context.samities.countActive()));
        pending.setText(String.valueOf(context.applications.countPending()));
        guarantees.setText(String.valueOf(context.applications.countAwaitingGuarantees()));
        approved.setText(String.valueOf(context.applications.countApproved()));
        var summary = context.reportService.portfolioSummary();
        activeLoans.setText(String.valueOf(summary.activeLoans()));
        overdue.setText(String.valueOf(summary.overdueInstallments()));
        savings.setText(summary.savingsBalance().toPlainString());
    }

    private VBox card(String title, Label value) {
        value.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        VBox card = new VBox(5, new Label(title), value);
        card.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 18; -fx-border-color: #81c784;");
        card.setPrefWidth(190);
        return card;
    }
}
