package com.samityflow.ui;

import com.samityflow.AppContext;
import com.samityflow.model.LoanProduct;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProductController {
    private final AppContext context;
    private final ListView<LoanProduct> products = new ListView<>();

    public ProductController(AppContext context) { this.context = context; }

    public Node view() {
        VBox page = Ui.page("Loan Product Configuration");
        TextField name=field("Product name"), interestRate=field("Interest rate %"),
                penaltyRate=field("Penalty rate %"), weeks=field("Duration in weeks"),
                required=field("Required guarantees"), savings=field("Weekly savings");
        ComboBox<String> interest=choice("Interest strategy", "AGRICULTURAL","BUSINESS","EMERGENCY");
        ComboBox<String> penalty=choice("Penalty strategy", "NORMAL","GRACE");

        products.getSelectionModel().selectedItemProperty().addListener((o,old,p)->{
            if(p!=null){name.setText(p.name());interest.setValue(p.interestStrategy());interestRate.setText(String.valueOf(p.interestRate()));penalty.setValue(p.penaltyStrategy());penaltyRate.setText(String.valueOf(p.penaltyRate()));weeks.setText(String.valueOf(p.durationWeeks()));required.setText(String.valueOf(p.requiredGuarantees()));savings.setText(String.valueOf(p.weeklySavings()));}
        });
        Button add=new Button("Add product");add.setOnAction(e->run(()->context.products.save(read(0,name,interest,interestRate,penalty,penaltyRate,weeks,required,savings))));
        Button update=new Button("Update product");update.setOnAction(e->run(()->{LoanProduct selected=products.getSelectionModel().getSelectedItem();if(selected==null)throw new IllegalArgumentException("Select a product");context.products.update(read(selected.id(),name,interest,interestRate,penalty,penaltyRate,weeks,required,savings));}));
        Button delete=new Button("Delete product");delete.setOnAction(e->run(()->{LoanProduct selected=products.getSelectionModel().getSelectedItem();if(selected==null)throw new IllegalArgumentException("Select a product");context.products.delete(selected.id());}));

        GridPane form=new GridPane();form.setHgap(8);form.setVgap(8);Node[] controls={name,interest,interestRate,penalty,penaltyRate,weeks,required,savings};for(int i=0;i<controls.length;i++)form.add(controls[i],i%2,i/2);form.add(add,0,4);form.add(update,1,4);form.add(delete,0,5);
        products.setCellFactory(list->new ListCell<>(){protected void updateItem(LoanProduct p,boolean empty){super.updateItem(p,empty);setText(empty||p==null?null:p.name()+" | "+p.interestRate()+"% | "+p.durationWeeks()+" weeks | "+p.requiredGuarantees()+" guarantees");}});
        page.getChildren().addAll(form,products);VBox.setVgrow(products,Priority.ALWAYS);refresh();return page;
    }

    public void refresh(){products.setItems(FXCollections.observableArrayList(context.products.findAll()));}
    private void run(Runnable action){try{action.run();refresh();}catch(Exception e){Ui.error(e);}}
    private TextField field(String prompt){TextField f=new TextField();f.setPromptText(prompt);return f;}
    private ComboBox<String> choice(String prompt,String...items){ComboBox<String> c=new ComboBox<>(FXCollections.observableArrayList(items));c.setPromptText(prompt);return c;}
    private LoanProduct read(int id,TextField name,ComboBox<String> interest,TextField interestRate,ComboBox<String> penalty,TextField penaltyRate,TextField weeks,TextField required,TextField savings){
        if(name.getText().isBlank()||interest.getValue()==null||penalty.getValue()==null)throw new IllegalArgumentException("Complete every product field");
        return new LoanProduct(id,name.getText().trim(),interest.getValue(),Double.parseDouble(interestRate.getText()),penalty.getValue(),Double.parseDouble(penaltyRate.getText()),Integer.parseInt(weeks.getText()),Integer.parseInt(required.getText()),Double.parseDouble(savings.getText()));
    }
}
