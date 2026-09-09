package com.samityflow.ui;

import com.samityflow.AppContext;
import com.samityflow.model.LoanProduct;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ProductController {
    private final AppContext context;
    private final ListView<LoanProduct> products = new ListView<>();
    public ProductController(AppContext context){this.context=context;}

    public Node view(){
        VBox page=Ui.page("Loan Product Configuration");
        TextField name=field("Product name");
        ComboBox<String> interest=new ComboBox<>(FXCollections.observableArrayList("AGRICULTURAL","BUSINESS","EMERGENCY")); interest.setPromptText("Interest strategy");
        TextField interestRate=field("Interest rate %");
        ComboBox<String> penalty=new ComboBox<>(FXCollections.observableArrayList("NORMAL","GRACE")); penalty.setPromptText("Penalty strategy");
        TextField penaltyRate=field("Penalty rate %");
        TextField weeks=field("Duration in weeks");
        TextField required=field("Required guarantees");
        TextField savings=field("Weekly savings");
        Button add=new Button("Add loan product");
        add.setOnAction(event->{try{
            LoanProduct p=new LoanProduct(0,name.getText().trim(),interest.getValue(),Double.parseDouble(interestRate.getText()),penalty.getValue(),Double.parseDouble(penaltyRate.getText()),Integer.parseInt(weeks.getText()),Integer.parseInt(required.getText()),Double.parseDouble(savings.getText()));
            context.products.save(p); refresh();
        }catch(Exception e){Ui.error(e);}});
        GridPane form=new GridPane();form.setHgap(8);form.setVgap(8);
        Node[] controls={name,interest,interestRate,penalty,penaltyRate,weeks,required,savings};
        for(int i=0;i<controls.length;i++)form.add(controls[i],i%2,i/2);
        form.add(add,0,4,2,1);
        products.setCellFactory(list->new ListCell<>(){protected void updateItem(LoanProduct p,boolean empty){super.updateItem(p,empty);setText(empty||p==null?null:p.name()+" | "+p.interestRate()+"% | "+p.durationWeeks()+" weeks | "+p.requiredGuarantees()+" guarantees");}});
        page.getChildren().addAll(form,products);VBox.setVgrow(products,Priority.ALWAYS);refresh();return page;
    }
    public void refresh(){products.setItems(FXCollections.observableArrayList(context.products.findAll()));}
    private TextField field(String prompt){TextField f=new TextField();f.setPromptText(prompt);return f;}
}
