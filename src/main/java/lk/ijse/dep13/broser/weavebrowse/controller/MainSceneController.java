package lk.ijse.dep13.broser.weavebrowse.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.*;
import java.net.Socket;

public class MainSceneController {
    public AnchorPane root;
    public TextField txtAddress;
    public WebView wbDisplay;

    private record URLComponents(
            String protocol,
            String host,
            String port,
            String path
    ) {}

    public void initialize() {
        txtAddress.setText("https://www.google.com");
        txtAddress.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) Platform.runLater(txtAddress::selectAll);
        });
    }

    public void txtAddressOnAction(ActionEvent event) {
        try {
            String url = txtAddress.getText();
            if (url.isBlank()) return;
            loadWebPage(url);
        } catch (Exception e) {
            showError("Error", "Failed to load webpage", e.getMessage());
        }
    }

    private URLComponents parseURL(String url) {
        // Step 1: Parse Protocol
        String protocol = "http";
        String remainingUrl = url;
        int protocolIndex = url.indexOf("://");

        if (protocolIndex != -1) {
            protocol = url.substring(0, protocolIndex);
            remainingUrl = url.substring(protocolIndex + 3);
        }

        // Step 2: Parse Host
        String host = remainingUrl;
        int hostEndIndex = remainingUrl.indexOf('/');
        if (hostEndIndex != -1) {
            host = remainingUrl.substring(0, hostEndIndex);
        }

        // Check for port in host
        int portIndex = host.indexOf(':');
        if (portIndex != -1) {
            host = host.substring(0, portIndex);
        }

        // Step 3: Parse Port
        String port = switch (protocol) {
            case "http" -> "80";
            case "https" -> "443";
            case "jdbc:mysql" -> "3306";
            case "jdbc:postgresql" -> "5432";
            default -> "80";
        };

        if (portIndex != -1) {
            String portPart = remainingUrl.substring(portIndex + 1);
            int portEndIndex = portPart.indexOf('/');
            port = portEndIndex != -1 ? portPart.substring(0, portEndIndex) : portPart;
        }

        // Step 4: Parse Path
        String path = "/";
        if (hostEndIndex != -1) {
            path = remainingUrl.substring(hostEndIndex);
        }

        return new URLComponents(protocol, host, port, path);
    }

    private void loadWebPage(String url) throws IOException {
        URLComponents components = parseURL(url);

        // Validate components
        if (components.host().isBlank() || components.port().isBlank()) {
            throw new RuntimeException("Invalid web page address");
        }

        // Log URL components
        System.out.println("Protocol: " + components.protocol());
        System.out.println("Host: " + components.host());
        System.out.println("Port: " + components.port());
        System.out.println("Path: " + components.path());

        // Handle HTTPS differently
        if (components.protocol().equalsIgnoreCase("https")) {
            handleHttpsRequest(url);
            return;
        }

        // Handle HTTP requests
        handleHttpRequest(components);
    }

    private void handleHttpsRequest(String url) {
        // Ensure URL starts with https://
        if (!url.startsWith("https://")) {
            url = "https://" + url;
        }

        // Use WebView's engine for HTTPS
        String finalUrl = url;
        Platform.runLater(() -> {
            System.out.println("Loading HTTPS URL: " + finalUrl);
            wbDisplay.getEngine().load(finalUrl);
        });
    }

    private void handleHttpRequest(URLComponents components) {
        new Thread(() -> {
            try (Socket socket = new Socket(components.host(), Integer.parseInt(components.port()))) {
                System.out.println("Connected to " + socket.getRemoteSocketAddress());

                // Send HTTP request
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream()));

                String request = """
                    GET %s HTTP/1.1
                    Host: %s
                    User-Agent: WeaveBrowse/1.0
                    Connection: close
                    Accept: text/html
                    
                    """.formatted(components.path(), components.host());

                writer.write(request);
                writer.flush();

                // Read response
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                // Read status line
                String statusLine = reader.readLine();
                if (statusLine != null) {
                    int statusCode = Integer.parseInt(statusLine.split(" ")[1]);
                    System.out.println("Status Code: " + statusCode);

                    if (statusCode >= 300 && statusCode <= 399) {
                        handleRedirection(reader);
                        return;
                    }
                }

                // Read headers and content
                StringBuilder content = new StringBuilder();
                boolean headersEnded = false;
                String line;

                while ((line = reader.readLine()) != null) {
                    if (!headersEnded) {
                        if (line.isBlank()) {
                            headersEnded = true;
                            continue;
                        }
                        // Process headers
                        if (line.toLowerCase().startsWith("content-type:")) {
                            System.out.println(line);
                        }
                    } else {
                        content.append(line).append("\n");
                    }
                }

                String finalContent = content.toString();
                System.out.println("Content length: " + finalContent.length());

                Platform.runLater(() ->
                        wbDisplay.getEngine().loadContent(finalContent));

            } catch (IOException e) {
                Platform.runLater(() ->
                        showError("Error", "Failed to load webpage", e.getMessage()));
            }
        }).start();
    }

    private void handleRedirection(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null && !line.isBlank()) {
            String[] headerParts = line.split(":", 2);
            if (headerParts.length == 2 &&
                    headerParts[0].strip().equalsIgnoreCase("Location")) {
                String newLocation = headerParts[1].strip();
                Platform.runLater(() -> {
                    txtAddress.setText(newLocation);
                    try {
                        loadWebPage(newLocation);
                    } catch (IOException e) {
                        showError("Error", "Failed to follow redirect", e.getMessage());
                    }
                });
                break;
            }
        }
    }

    private void showError(String title, String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}