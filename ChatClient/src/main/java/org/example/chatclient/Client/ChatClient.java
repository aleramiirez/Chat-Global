package org.example.chatclient.Client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.util.Scanner;

public class ChatClient extends Application {

    private static final DatagramSocket socket;
    private static final int SERVER_PORT = 5010;
    private static final int CLIENT_PORT = 6010;

    static {
        try {
            socket = new DatagramSocket();
            //socket = new DatagramSocket(CLIENT_PORT);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    private static final InetAddress address;

    static {
        try {
            //address = InetAddress.getByName("13.80.252.23");
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static final TextArea messageArea = new TextArea();
    private static final TextField inputBox = new TextField();
    private Button sendButton;
    private Button sendPhotoButton;
    private static String username;

    public static void main(String[] args) throws IOException {

        Scanner sc = new Scanner(System.in);
        System.out.println("Introduce el nombre de usuario: ");

        username = sc.nextLine();

        while (!validateUsername(username)) {
            System.out.println("El nombre de usuario ya está en uso. Por favor, elija otro nombre.");
            username = sc.nextLine();
        }

        // thread for receiving messages
        ClientThread clientThread = new ClientThread(socket, messageArea);
        clientThread.start();

        // send initialization message to the server
        byte[] uuid = ("init;" + username).getBytes();
        DatagramPacket initialize = new DatagramPacket(uuid, uuid.length, address, SERVER_PORT);
        socket.send(initialize);

        launch(); // launch GUI

    }

    private static boolean validateUsername(String username) {
        byte[] validationRequest = ("validate;" + username).getBytes();
        DatagramPacket validationPacket = new DatagramPacket(validationRequest, validationRequest.length, address, SERVER_PORT);

        try {
            socket.send(validationPacket);

            // Esperar la respuesta del servidor
            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);

            String responseMessage = new String(responsePacket.getData(), 0, responsePacket.getLength());
            return responseMessage.equals("valid");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        messageArea.setMaxWidth(500);
        messageArea.setEditable(false);
        messageArea.setStyle("-fx-control-inner-background: #0FC2C0;");

        inputBox.setMaxWidth(400);
        inputBox.setStyle("-fx-background-color: #0FC2C0;");

        sendButton = new Button("Enviar");
        sendPhotoButton = new Button("Enviar Foto");
        sendButton.setStyle("-fx-background-color: #0CABA8;");
        sendPhotoButton.setStyle("-fx-background-color: #0CABA8;");

        sendButton.setMinWidth(100);
        sendPhotoButton.setMinWidth(100);

        inputBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });

        sendButton.setOnAction(event -> sendMessage());
        sendPhotoButton.setOnAction(event -> sendPhoto());

        // Create an HBox to hold the TextField and Buttons
        HBox inputContainer = new HBox(10, inputBox, sendButton, sendPhotoButton);
        inputContainer.setMaxWidth(500);

        // Set Hgrow for TextField to make it expand
        HBox.setHgrow(inputBox, Priority.ALWAYS);

        // Create a BorderPane and set the content in the center
        BorderPane root = new BorderPane();
        root.setCenter(new VBox(35, messageArea, inputContainer));
        root.setPadding(new Insets(20));

        root.setStyle("-fx-background-color: #015958;");

        // put everything on screen
        Scene scene = new Scene(root, 550, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void sendMessage() {
        String messageText = inputBox.getText().trim();
        if (!messageText.isEmpty()) {
            String formattedMessage = username + ": " + messageText;
            messageArea.appendText(formattedMessage + "\n");

            byte[] msg = (username + ": " + messageText).getBytes();
            inputBox.clear();

            DatagramPacket send = new DatagramPacket(msg, msg.length, address, SERVER_PORT);
            try {
                socket.send(send);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecciona una imagen");
        Stage stage = new Stage();
        stage.initOwner(new Stage());

        // Filtrar solo archivos de imagen
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos de imagen", "*.png", "*.jpg", "*.gif");
        fileChooser.getExtensionFilters().add(extFilter);

        // Mostrar el cuadro de diálogo para elegir un archivo
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                // Leer la imagen como un arreglo de bytes
                byte[] imageBytes = Files.readAllBytes(((File) file).toPath());

                // Enviar la imagen al servidor
                DatagramPacket photoPacket = new DatagramPacket(imageBytes, imageBytes.length, address, SERVER_PORT);
                socket.send(photoPacket);

                // Puedes mostrar un mensaje en la interfaz del cliente indicando que la imagen fue enviada
                String photoMessage = username + ": Imagen enviada: " + file.getName();
                messageArea.appendText(photoMessage + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendDisconnectMessage() {
        // Envia un mensaje de desconexión al servidor
        byte[] disconnectMessage = ("disconnect;" + username).getBytes();
        DatagramPacket disconnectPacket = new DatagramPacket(disconnectMessage, disconnectMessage.length, address, SERVER_PORT);

        try {
            socket.send(disconnectPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        // Este método se llama cuando la aplicación se cierra
        sendDisconnectMessage();
        // Puedes agregar más lógica aquí según sea necesario
        Platform.exit();
    }
}
