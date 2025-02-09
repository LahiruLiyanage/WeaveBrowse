package lk.ijse.dep13.broser.weavebrowse.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;

import java.io.IOException;

public class MainSceneController {
    public AnchorPane root;
    public TextField txtAddress;
    public WebView wbDisplay;

    public void initialize() {
        txtAddress.setText("https://www.google.com");

        // Auto-select text when focused
        txtAddress.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) Platform.runLater(txtAddress::selectAll);
        });

    }

    String url;
    public void txtAddressOnAction(ActionEvent event) {
        url = txtAddress.getText();
        if (url.isBlank()) return;
        loadWebPage(url);
    }
    private void loadWebPage(String url) throws IOException {
        if (url.isBlank()) return;

        // Getting Protocol
        String protocol = "";
        int protocolIndex = url.indexOf("://");
        if (protocolIndex != -1) {
            String protocolName = url.substring(protocolIndex+3);
            protocol = url.substring(0, protocolIndex);
        } else {
            protocol = "http";
        }

        // Getting Host
        String host = "";
        if (protocolIndex != -1){
            host = url.substring(protocolIndex + 3);
        } else {
            host = url;
        }
        if (host.indexOf(":") != -1){
            host = host.substring(0, host.indexOf(":"));
        }else if (host.indexOf("/") == -1) {
            host = host;
        } else {
            host = host.substring(0, (host.indexOf("/")));
        }


    }


    }
