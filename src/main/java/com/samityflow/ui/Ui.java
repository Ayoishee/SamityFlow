package com.samityflow.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class Ui {
    private Ui() { }

    public static VBox page(String title) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        VBox box = new VBox(12, heading);
        box.setPadding(new Insets(18));
        return box;
    }

    public static void error(Exception exception) {
        String message = exception.getMessage();
        if (message == null && exception.getCause() != null) message = exception.getCause().getMessage();
        new Alert(Alert.AlertType.ERROR, message == null ? "Something went wrong" : message).showAndWait();
    }

    public static void info(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }
}
