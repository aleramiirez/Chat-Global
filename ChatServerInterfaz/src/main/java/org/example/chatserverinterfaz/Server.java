package org.example.chatserverinterfaz;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Server extends Application {

    // Puerto en el cual el servidor esta escuchando
    private static final int PORT = 5010;

    // Puerto al cual el servidor reenvia mensajes a los clientes
    private static final int FORWARD_PORT = 6010;

    // Se utiliza para la comunicacion a traves de datagramas. Envia o recibe paquetes de datos a traves de UDP
    private DatagramSocket socket;

    // Almacena los nombres de usuario registrados en el servidor
    private Set<String> registeredUsernames = new HashSet<>();

    // Almacena los puertos de los usuarios conectados al servidor
    private ArrayList<Integer> users = new ArrayList<>();

    // Este objeto se utiliza para mostrar mensajes en la interfaz grafica del servidor
    private TextArea logTextArea;

    // Almacena la direccion IP del cliente cuando se inicia la comunicacion
    private InetAddress address;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Server");

        logTextArea = new TextArea();

        // Configura el TextArea para que los usuarios no puedan escribir en el
        logTextArea.setEditable(false);

        primaryStage.setScene(new Scene(logTextArea, 400, 300));
        primaryStage.show();

        startServer();

        // Cuando se cierra la ventana llama al metodo stopServer()
        primaryStage.setOnCloseRequest(event -> stopServer());
    }

    private void startServer() {
        try {

            // Se crea un nuevo DatagramSocket que escuche por el puerto "PORT"
            socket = new DatagramSocket(PORT);
        } catch (SocketException e) {
            log("Error al iniciar el servidor en el puerto " + PORT);
            Platform.exit();
        }

        log("Servidor iniciado en el puerto " + PORT);

        // Se ejecuta un hilo para poder atender a multiples clientes simultaneamente sin bloquearse
        Thread serverThread = new Thread(this::runServer);

        // Permite cerrar la aplicacion sin que los hilos finalicen
        serverThread.setDaemon(true);

        // Inicia el hilo
        serverThread.start();
    }

    // Si el socket no es nulo y no esta cerrado cierra el servidor
    private void stopServer() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    // Representa el bucle principal del servidor que se ejecuta continuamente para manejar los paquetes entrantes
    private void runServer() {

        // Se crea un arreglo de bytes con una capacidad de 1024 bytes para almacenar los datos recibidos.
        byte[] incomingData = new byte[1024];

        while (true) {

            // Se utilizara para recibir datos del cliente. El paquete se configura para almacenar datos
            // en incomingData
            DatagramPacket packet = new DatagramPacket(incomingData, incomingData.length);

            try {

                // Se intenta recibir el paquete del cliente a traves del socket del servidor
                socket.receive(packet);
            } catch (IOException e) {
                log("Error al recibir datos: " + e.getMessage());
            }

            // Se convierten los datos del paquete en una cadena String
            String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

            // Se registra el mensaje en el area de registro del servidor
            log("Mensaje recibido: " + message);

            // Si el mensaje contiene la cadena "STOP", se para el servidor
            if (message.contains("STOP")) {

                // Se registra el mensaje en el area de registro del servidor
                log("Servidor detenido por solicitud del cliente.");

                // Se para el servidor
                stopServer();

                // Se cierra la aplicacion
                Platform.exit();
            }

            // Si el mensaje comienza con validate; valida que el nombre de usuario no este registrado en el chat
            else if (message.startsWith("validate;")) {

                // Se registra el mensaje en el area de registro del servidor
                log("Mensaje de validación recibido: " + message);

                // Se obtiene el nombre de usuario
                String requestedUsername = message.substring(9);

                // Se valida el nombre de usuario
                handleValidation(requestedUsername, packet.getAddress(), packet.getPort());
            }

            // Si el mensaje comienza con disconnect; elimina el nombre de usuario de la lista
            else if (message.contains("disconnect;")) {

                // Se registra el mensaje en el area de registro del servidor
                log("Mensaje de desconexión recibido: " + message);

                // Se divide la cadena para poder obtener el nombre de usuario
                String[] parts = message.split(";");

                // Se obtiene el nombre de usuario
                String disconnectedUser = parts[1];

                // Elimina al usuario desconectado de la lista
                registeredUsernames.remove(disconnectedUser);
            }

            // Si el mensaje empieza por init; entra un nuevo usuario al chat
            else if (message.startsWith("init;")) {

                // Se registra el mensaje en el area de registro del servidor
                log("Mensaje de inicialización recibido: " + message);

                // Se añade el puerto del cliente a la lista users
                users.add(packet.getPort());

                // Se añade la direccion IP a la lista address
                address = packet.getAddress();
            }

            // Si el mensaje empieza con img; se asume que es una imagen
            else if (message.startsWith("img;")) {

                // Se registra el mensaje en el area de registro del servidor
                log("Mensaje de imagen recibido: " + message);

                // Llama al metodo handleImageMessage() para manejar el mensaje de imagen
                handleImageMessage(packet);
            }

            // Si no es ninguna de las anteriores se asume que es un mensaje de texto normal
            else {
                // Reenvia el mensaje todos los clientes conectados menos al remitente
                forwardTextMessage(packet);
            }
        }
    }

    // Registra mensajes en el area de registro del servidor
    private void log(String message) {

        // Se utiliza Platform.runLater() para ejecutar la actualizacion de la interfaz de usuario en el hilo
        // de JavaFX. Se utiliza una expresion lambda para agregar el mensaje recibido, seguido de un salto de
        // linea al area de texto
        Platform.runLater(() -> logTextArea.appendText(message + "\n"));
    }

    // Maneja la validacion de nombres de usuarios cuando un cliente solicita unirse
    private void handleValidation(String requestedUsername, InetAddress address, int port) {

        // Si la lista registeredUsernames contiene el nombre de usuario envia que no es valido
        if (registeredUsernames.contains(requestedUsername)) {
            // Enviar mensaje al cliente indicando que el Nick no es válido
            sendMessage("invalid", address, port);
        }

        // Si la lista registeredUsernames no contiene el nombre de usuario envia que es valido
        else {
            // Enviar mensaje al cliente indicando que el Nick es válido
            sendMessage("valid", address, port);
            // Registrar el Nick
            registeredUsernames.add(requestedUsername);
        }
    }

    private void forwardTextMessage(DatagramPacket packet) {

        // Obtiene el puerto del cliente que envio el mensaje
        int userPort = packet.getPort();

        // Obtiene los datos del mensaje en forma de un array de bytes
        byte[] byteMessage = packet.getData();

        // Inicia un bucle que recorre todos los puertos de los clientes registrados en users
        for (int forwardPort : users) {

            // Verifica si el puerto actual (FORWARD_PORT) no es el mismo que el puerto del cliente que
            // envió el mensaje
            if (forwardPort != userPort) {

                // Convierte los datos del mensaje en un texto
                String messageText = new String(byteMessage, 0, packet.getLength());

                // Convierte el texto del mensaje en un array de bytes
                byte[] formattedMessageBytes = messageText.getBytes();

                //  Construye un nuevo paquete que contiene el mensaje formateado y está destinado al cliente actual
                DatagramPacket forward = new DatagramPacket(formattedMessageBytes, formattedMessageBytes.length,
                        address, forwardPort);

                try {

                    // Envia el paquete al cliente a traves del socket del servidor
                    socket.send(forward);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void handleImageMessage(DatagramPacket packet) {
        int userPort = packet.getPort();
        byte[] imageData = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), 0, imageData, 0, packet.getLength());

        // Guardar la imagen en la carpeta Descargas
        String fileName = "C:\\Users\\aleja\\Downloads" + System.currentTimeMillis() + ".png";
        Path imagePath = Path.of(fileName);

        try {
            Files.write(imagePath, imageData);
            log("Imagen guardada en: " + fileName);
        } catch (IOException e) {
            log("Error al guardar la imagen: " + e.getMessage());
        }
    }

    // Se utiliza para enviar mensajes desde el servidor a un cliente especifico
    private void sendMessage(String message, InetAddress address, int port) {

        // Convierte el mensaje de texto en un array de bytes
        byte[] messageBytes = message.getBytes();

        // Crea un nuevo paquete que contiene los bytes del mensaje, la longitud de los bytes,
        // la dirección IP del destinatario, y el puerto del destinatario
        DatagramPacket validationResponse = new DatagramPacket(messageBytes, messageBytes.length, address, port);

        try {

            // Intenta enviar el paquete a través del socket del servidor
            socket.send(validationResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
