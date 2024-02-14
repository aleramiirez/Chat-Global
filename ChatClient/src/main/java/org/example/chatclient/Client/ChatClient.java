package org.example.chatclient.Client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.Base64;

/**
 * Esta clase representa el cliente de chat de la aplicacion. Extiende la clase Application de JavaFX
 * para proporcionar una interfaz grafica de usuario.
 */
public class ChatClient extends Application {

    // Declara un objeto DatagramSocket para la comunicacion con el servidor
    private static final DatagramSocket socket;

    // Puerto del servidor
    private static final int SERVER_PORT = 5010;

    // Puerto del cliente
    private static final int CLIENT_PORT = 6010;

    static {
        try {
            socket = new DatagramSocket(); // Crea un nuevo socket DatagramSocket
            //socket = new DatagramSocket(CLIENT_PORT);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    // Declara una direccion IP
    private static final InetAddress address;

    static {
        try {

            // Direccion IP del servidor
            //address = InetAddress.getByName("13.80.252.23");
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    // Declara un area de texto para mensajes
    private static final TextArea messageArea = new TextArea();

    // Declara un campo de texto para entrada de usuario
    private static final TextField inputBox = new TextField();

    // Declara un boton para enviar mensajes de texto
    private Button sendButton;

    // Declara un boton para enviar fotos
    private Button sendPhotoButton;

    // Declara una variable para almacenar el nombre de usuario
    private static String username;


    /**
     * Metodo principal para iniciar la aplicacion del cliente de chat.
     * @param args Los argumentos de la linea de comandos.
     * @throws IOException Si ocurre un error de entrada/salida al interactuar con el socket.
     */
    public static void main(String[] args) throws IOException {

        Scanner sc = new Scanner(System.in);
        System.out.println("Introduce el nombre de usuario: ");

        username = sc.nextLine();

        // Continua solicitando un nombre de usuario hasta que sea valido
        while (!validateUsername(username)) {
            System.out.println("El nombre de usuario ya está en uso. Por favor, elija otro nombre.");
            username = sc.nextLine();
        }

        // Hilo para recibir mensajes
        ClientThread clientThread = new ClientThread(socket, messageArea);

        // Inicia el hilo de cliente para recibir mensajes
        clientThread.start();

        // Enviar mensaje de inicializacion al servidor
        byte[] uuid = ("init;" + username).getBytes();

        // Convierte el nombre de usuario en bytes
        DatagramPacket initialize = new DatagramPacket(uuid, uuid.length, address, SERVER_PORT);

        // Envia el paquete de inicializacion al servidor
        socket.send(initialize);

        launch();

    }

    /**
     * Valida el nombre de usuario consultando al servidor.
     * @param username El nombre de usuario a validar.
     * @return true si el nombre de usuario es valido, false de lo contrario.
     */
    private static boolean validateUsername(String username) {

        // Convierte el nombre de usuario en bytes
        byte[] validationRequest = ("validate;" + username).getBytes();

        // Crea un nuevo paquete DatagramPacket para la validacion
        DatagramPacket validationPacket = new DatagramPacket(validationRequest, validationRequest.length, address, SERVER_PORT);

        try {

            // Envía el paquete de validación al servidor
            socket.send(validationPacket);

            // Crea un bufer para almacenar la respuesta del servidor
            byte[] responseBuffer = new byte[1024];

            // Crea un nuevo paquete DatagramPacket para la respuesta del servidor
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

            // Recibe la respuesta del servidor
            socket.receive(responsePacket);

            // Convierte la respuesta del servidor en una cadena
            String responseMessage = new String(responsePacket.getData(), 0, responsePacket.getLength());

            // Devuelve true si el nombre de usuario es válido según la respuesta del servidor
            return responseMessage.equals("valid");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Metodo llamado al iniciar la aplicacion.
     * Configura la interfaz de usuario, incluyendo el area de mensajes, el campo de entrada de texto y los botones.
     * Tambien establece los eventos para enviar mensajes cuando se presionan teclas o se hace clic en botones.
     * @param primaryStage El escenario principal de la aplicación.
     */
    @Override
    public void start(Stage primaryStage) {

        // Establece el ancho maximo del area de mensajes
        messageArea.setMaxWidth(500);

        // Hace que el area de mensajes no sea editable
        messageArea.setEditable(false);

        // Establece el color de fondo del area de mensajes
        messageArea.setStyle("-fx-control-inner-background: #0FC2C0;");

        // Establece el ancho máximo del campo de entrada de texto
        inputBox.setMaxWidth(400);

        // Establece el color de fondo del campo de entrada de texto
        inputBox.setStyle("-fx-background-color: #0FC2C0;");

        // Crea un boton con el texto "Enviar"
        sendButton = new Button("Enviar");

        // Crea un boton con el texto "Enviar Foto"
        sendPhotoButton = new Button("Enviar Foto");

        // Establece el color de fondo del boton "Enviar"
        sendButton.setStyle("-fx-background-color: #0CABA8;");

        // Establece el color de fondo del boton "Enviar Foto"
        sendPhotoButton.setStyle("-fx-background-color: #0CABA8;");

        // Establece el ancho minimo del boton "Enviar"
        sendButton.setMinWidth(100);

        // Establece el ancho minimo del boton "Enviar Foto"
        sendPhotoButton.setMinWidth(100);

        // Define un evento al presionar una tecla en el campo de entrada de texto
        inputBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage(); // Llama al metodo sendMessage() al presionar la tecla Enter
            }
        });

        // Define un evento al hacer clic en el boton "Enviar" para llamar al metodo sendMessage()
        sendButton.setOnAction(event -> sendMessage());

        // Crea un contenedor HBox para organizar el campo de entrada de texto y los botones horizontalmente
        HBox inputContainer = new HBox(10, inputBox, sendButton, sendPhotoButton);

        // Establece el ancho máximo del contenedor HBox
        inputContainer.setMaxWidth(500);

        // Establece la prioridad de crecimiento del campo de entrada de texto para expandirse horizontalmente
        HBox.setHgrow(inputBox, Priority.ALWAYS);

        // Crea un contenedor BorderPane y coloca el contenido en el centro
        BorderPane root = new BorderPane();

        // Crea un contenedor VBox para organizar verticalmente el área de mensajes y el contenedor de entrada
        root.setCenter(new VBox(35, messageArea, inputContainer));

        // Establece los margenes del contenedor BorderPane
        root.setPadding(new Insets(20));

        // Establece el color de fondo del contenedor BorderPane
        root.setStyle("-fx-background-color: #015958;");

        // Crea una escena con el contenedor BorderPane y el tamaño especificado
        Scene scene = new Scene(root, 550, 300);

        // Establece la escena en el escenario principal
        primaryStage.setScene(scene);

        // Muestra el escenario principal en la pantalla
        primaryStage.show();
    }

    /**
     * Envia un mensaje al servidor.
     * El mensaje se recupera del campo de entrada de texto, se formatea agregando el nombre de usuario
     * y luego se muestra en el area de mensajes. Luego, el mensaje se convierte en un arreglo de bytes
     * y se envia al servidor a traves de un DatagramPacket.
     */
    private void sendMessage() {

        // Obtiene el texto del campo de entrada y elimina los espacios en blanco al principio y al final
        String messageText = inputBox.getText().trim();

        // Verifica si el mensaje no esta vacio
        if (!messageText.isEmpty()) {

            // Formatea el mensaje con el nombre de usuario y lo muestra en el area de mensajes
            String formattedMessage = username + ": " + messageText;
            messageArea.appendText(formattedMessage + "\n");

            // Convierte el mensaje en un arreglo de bytes
            byte[] msg = (username + ": " + messageText).getBytes();

            // Limpia el campo de entrada de texto después de enviar el mensaje
            inputBox.clear();

            // Crea un DatagramPacket con el mensaje y lo envía al servidor
            DatagramPacket send = new DatagramPacket(msg, msg.length, address, SERVER_PORT);

            try {
                // Envía el paquete a través del socket
                socket.send(send);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Envia un mensaje de desconexion al servidor.
     * Crea un mensaje de desconexion concatenando "disconnect;" con el nombre de usuario,
     * lo convierte en un arreglo de bytes y lo envia al servidor a traves de un DatagramPacket.
     * Este mensaje se utiliza para notificar al servidor que el usuario se esta desconectando.
     */
    private void sendDisconnectMessage() {
        // Crea un mensaje de desconexión concatenando "disconnect;" con el nombre de usuario
        byte[] disconnectMessage = ("disconnect;" + username).getBytes();

        // Crea un DatagramPacket con el mensaje de desconexión y la dirección del servidor
        DatagramPacket disconnectPacket = new DatagramPacket(disconnectMessage, disconnectMessage.length, address, SERVER_PORT);

        try {
            // Envía el paquete al servidor
            socket.send(disconnectPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Se llama cuando la aplicación se cierra.
     * Este metodo envia un mensaje de desconexion al servidor utilizando el metodo sendDisconnectMessage().
     * Ademas, cierra la plataforma JavaFX utilizando Platform.exit().
     */
    @Override
    public void stop() {
        // Envía un mensaje de desconexión al servidor
        sendDisconnectMessage();

        // Cierra la plataforma JavaFX
        Platform.exit();
    }
}
