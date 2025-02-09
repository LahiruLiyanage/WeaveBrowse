package lk.ijse.dep13.broser.weavebrowse.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;

import java.io.*;
import java.net.Socket;

public class MainSceneController {
    public AnchorPane root;
    public TextField txtAddress;
    public WebView wbDisplay;
    String url;

    public void initialize() {
        txtAddress.setText("https://www.google.com");

        // Auto-select text when focused
        txtAddress.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) Platform.runLater(txtAddress::selectAll);
        });

    }

    public void txtAddressOnAction(ActionEvent event) throws IOException {
        try {
            String url = txtAddress.getText().trim();
            if (url.isBlank()) return;
            loadWebPage(url);
        } catch (Exception e) {
            showError("Error", "Failed to load webpage", e.getMessage());
        }
    }

    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void loadWebPage(String url) throws IOException {
        if (url.isBlank()) return;

        // Getting Protocol
        String protocol = "";
        int protocolIndex = url.indexOf("://");
        if (protocolIndex != -1) {
            String protocolName = url.substring(protocolIndex + 3);
            protocol = url.substring(0, protocolIndex);
        } else {
            protocol = "http";
        }

        // Getting Host
        String host = "";
        if (protocolIndex != -1) {
            host = url.substring(protocolIndex + 3);
        } else {
            host = url;
        }
        if (host.indexOf(":") != -1) {
            host = host.substring(0, host.indexOf(":"));
        } else if (host.indexOf("/") == -1) {
            host = host;
        } else {
            host = host.substring(0, (host.indexOf("/")));
        }

        // Getting Port
        String forGetPort = protocol + "://" + host + ":";

        int portIndex = url.indexOf(forGetPort);

        String port = "";
        if (portIndex == -1) {
            if (protocol.equals("http")) {
                port = "80";
            } else if (protocol.equals("https")) {
                port = "443";
            } else if (protocol.equals("jdbc:mysql")) {
                port = "3306";
            } else if (protocol.equals("jdbc:postgresql")) {
                port = "5432";
            }
        } else {
            port = url.substring(forGetPort.length());
            int subIndex = port.indexOf("/");
            if (subIndex != -1) {
                port = port.substring(0, subIndex);
            }
        }

        // Getting Path
        int pathIndex = url.indexOf(host + port + "/");
        String path = "";
        if (pathIndex == -1) {
            path = "/";
        } else {
            path = url.substring(pathIndex);
        }

        // URL validation
        if (port.isBlank() || host.isBlank()) {
            throw new RuntimeException("Invalid web page address");
        } else {
            System.out.println("Protocol: " + protocol);
            System.out.println("Host: " + host);
            System.out.println("Port: " + port);
            System.out.println("Path: " + path);

            int portInt = Integer.parseInt(port);
            Socket socket = new Socket(host, portInt);
            System.out.println("Connected to " + socket.getRemoteSocketAddress());

            OutputStream os = socket.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(os);

            String request = """
                GET %s HTTP/1.1
                Host: %s
                User Agent: dep-browser/1
                Connection: close
                Accept: text/html;
                
                """.formatted(path, host);

            bos.write(request.getBytes());
            bos.flush();

            new Thread(() -> {
                try {
                    InputStream is = socket.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);

                    // Reading the Status Line
                    String statusLine = br.readLine();
                    int statusCode = Integer.parseInt(statusLine.split(" ")[1]);
                    System.out.println("Status Code: " + statusCode);
                    boolean redirection = statusCode >= 300 && statusCode <= 399;

                    // Read Request headers
                    String contentType = null;
                    String line;
                    while ((line = br.readLine()) != null && !line.isBlank()) {
                        String header = line.split(":")[0].strip();
                        String value = line.substring(line.indexOf(":") + 1);

                        if (redirection) {
                            if (!header.equalsIgnoreCase("Location")) continue;
                            System.out.println("Redirection: " + value);
                            Platform.runLater(() -> txtAddress.setText(value));
                            loadWebPage(value);
                            return;
                        } else {
                            if (!header.equalsIgnoreCase("content-type")) continue;
                            contentType = value;
                        }
                    }
                    System.out.println("Content type: " + contentType);

                    String content = "";
                    while ((line = br.readLine()) != null) {
                        content += line + "\n";
                    }
                    System.out.println("Content: " + content);
                    String finalContent = content;
                    Platform.runLater(() -> {
                        wbDisplay.getEngine().loadContent(finalContent);
                    });

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();

        }




    }
}
