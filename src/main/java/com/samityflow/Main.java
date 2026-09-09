package com.samityflow;

import com.samityflow.database.Database;
import com.samityflow.ui.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        AppContext context = new AppContext(Database.applicationDatabase());
        DashboardController dashboard = new DashboardController(context);
        ManagementController management = new ManagementController(context);
        LoanController loans = new LoanController(context);
        ProductController products = new ProductController(context);

        Tab dashboardTab = tab("Dashboard", dashboard.view());
        Tab managementTab = tab("Samities & Members", management.view());
        Tab loanTab = tab("Applications & Approval", loans.view());
        Tab productTab = tab("Loan Products", products.view());
        TabPane tabs = new TabPane(dashboardTab, managementTab, loanTab, productTab);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            dashboard.refresh(); management.refresh(); loans.refresh(); products.refresh();
        });

        stage.setTitle("SamityFlow");
        stage.setScene(new Scene(tabs, 1100, 700));
        stage.show();
    }

    private Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    public static void main(String[] args) { launch(args); }
}
//mvn javafx:run
