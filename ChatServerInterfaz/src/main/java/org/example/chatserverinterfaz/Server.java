package org.example.chatserverinterfaz;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;


public class Server extends Application {

    // Puerto en el cual el servidor esta escuchando
    private static final int PORT = 5010;

    // Puerto al cual el servidor reenvia mensajes a los clientes
    private static final int FORWARD_PORT = 6010;

    // Utilizado para esperar a que todos los hilos terminen antes de cerrar el socket
    private CountDownLatch shutdownLatch = new CountDownLatch(1);

    // Se utiliza para la comunicacion a traves de datagramas. Envia o recibe paquetes de datos a traves de UDP
    private DatagramSocket socket;

    // Almacena los nombres de usuario registrados en el servidor
    private Set<String> registeredUsernames = new HashSet<>();

    // Almacena los puertos de los usuarios conectados al servidor
    private static ArrayList<Integer> users = new ArrayList<>();

    // Este objeto se utiliza para mostrar mensajes en la interfaz grafica del servidor
    private TextArea logTextArea;

    // Almacena la direccion IP del cliente cuando se inicia la comunicacion
    private static List<InetAddress> clientAddresses = new ArrayList<>();

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
            System.out.println("Error al iniciar el servidor en el puerto " + PORT);
            Platform.exit();
        }

        log("Servidor iniciado en el puerto " + PORT);
        System.out.println("Servidor iniciado en el puerto " + PORT);

        // Se ejecuta un hilo para poder atender a multiples clientes simultaneamente sin bloquearse
        Thread serverThread = new Thread(this::runServer);

        // Permite cerrar la aplicacion sin que los hilos finalicen
        serverThread.setDaemon(true);

        // Inicia el hilo
        serverThread.start();
    }

    private void stopServer() {
        if (socket != null && !socket.isClosed()) {
            // Se establece el latch para permitir que los hilos completen sus operaciones
            shutdownLatch.countDown();
            // Se espera a que todos los hilos completen antes de cerrar el socket
            try {
                shutdownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            socket.close();
        }
    }

    private void runServer() {
        // Se crea un arreglo de bytes con una capacidad de 1024 bytes para almacenar los datos recibidos.
        byte[] incomingData = new byte[1024];

        while (true) {
            // Se utilizara para recibir datos del cliente. El paquete se configura para almacenar datos
            // en incomingData
            DatagramPacket packet = new DatagramPacket(incomingData, incomingData.length);

            try {
                // Se intenta recibir el paquete del cliente a través del socket del servidor
                socket.receive(packet);
            } catch (IOException e) {
                log("Error al recibir datos: " + e.getMessage());
            }

            // Se convierten los datos del paquete en una cadena String
            String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

            // Si el mensaje contiene la cadena "STOP", se para el servidor
            if (message.endsWith(": STOP")) {
                // Se registra el mensaje en el área de registro del servidor
                log("Servidor detenido por solicitud del cliente");
                System.out.println("Servidor detenido por solicitud del cliente");

                // Se establece el latch para permitir que los hilos completen sus operaciones
                shutdownLatch.countDown();

                // Se para el servidor
                stopServer();

                // Se cierra la aplicación
                Platform.exit();
            }

            // Si el mensaje comienza con validate; valida que el nombre de usuario no esté registrado en el chat
            else if (message.startsWith("validate;")) {


                // Se obtiene el nombre de usuario
                String requestedUsername = message.substring(9);

                // Se valida el nombre de usuario
                handleValidation(requestedUsername, packet.getAddress(), packet.getPort());
            }

            else if (message.contains("disconnect;")) {
                // Se registra el mensaje en el área de registro del servidor
                log("Mensaje de desconexión recibido: " + message);

                // Se divide la cadena para poder obtener el nombre de usuario
                String[] parts = message.split(";");

                // Se obtiene el nombre de usuario
                String disconnectedUser = parts[1];

                // Elimina al usuario desconectado de la lista
                registeredUsernames.remove(disconnectedUser);

                String newUser = message.substring(11);

                // Modificar para reenviar el mensaje de inicialización a todos los usuarios conectados
                for (int forwardPort : users) {
                    if (forwardPort != packet.getPort()) {
                        // Construye un nuevo paquete que contiene el mensaje de inicialización y está destinado al
                        // cliente actual
                        byte[] activeUser = (newUser + " ha abandonado el chat!").getBytes();
                        DatagramPacket forward =
                                new DatagramPacket(activeUser, activeUser.length, packet.getAddress(), forwardPort);

                        try {
                            // Envia el paquete al cliente a través del socket del servidor
                            socket.send(forward);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            // Si el mensaje comienza con init; entra un nuevo usuario al chat
            else if (message.startsWith("init;")) {
                // Se registra el mensaje en el área de registro del servidor
                String newUser = message.substring(5);
                log(newUser + " está activo");

                // Se añade la dirección IP a la lista address
                clientAddresses.add(packet.getAddress());

                // Se añade el puerto del cliente a la lista users
                users.add(packet.getPort());

                // Modificar para reenviar el mensaje de inicialización a todos los usuarios conectados
                for (int forwardPort : users) {
                    if (forwardPort != packet.getPort()) {
                        // Construye un nuevo paquete que contiene el mensaje de inicialización y está destinado al
                        // cliente actual
                        byte[] activeUser = (newUser + " ha entrado al chat!").getBytes();
                        DatagramPacket forward =
                                new DatagramPacket(activeUser, activeUser.length, packet.getAddress(), forwardPort);

                        try {
                            // Envia el paquete al cliente a través del socket del servidor
                            socket.send(forward);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }


            // Si el mensaje empieza con img; se asume que es una imagen
            else if (message.startsWith("img;")) {
                // Se registra el mensaje en el área de registro del servidor
                log("Mensaje de imagen recibido: " + message);

                // Llama al método handleImageMessage() para manejar el mensaje de imagen
                //receiveImage(packet, socket);
            }

            // Si no es ninguna de las anteriores se asume que es un mensaje de texto normal
            else {
                // Reenvía el mensaje a todos los clientes conectados menos al remitente
                forwardTextMessage(packet);
                log(message);
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

        // Si la lista registeredUsernames contiene el nombre de usuario, envía que no es válido
        if (registeredUsernames.contains(requestedUsername)) {
            // Enviar mensaje al cliente indicando que el Nick no es válido
            sendMessage("invalid", address, port);

            log("Nick de usuario en uso: " + requestedUsername +
                    "\nSe solicita a cliente que introduzca nick alternativo");
        }

        // Si la lista registeredUsernames no contiene el nombre de usuario, envía que es válido
        else {
            // Registrar el Nick
            registeredUsernames.add(requestedUsername);

            // Enviar mensaje al cliente indicando que el Nick es válido
            sendMessage("valid", address, port);

            // Solo imprimir el mensaje en el servidor si el Nick es válido
            log("Usuario conectado correctamente: " + requestedUsername);
        }
    }


    // Método para reenviar mensajes de texto a todos los clientes excepto al remitente
    private void forwardTextMessage(DatagramPacket packet) {
        // Si el socket está cerrado, no se realiza ningún reenvío
        if (socket.isClosed()) {
            return;
        }

        // Obtiene el puerto del remitente del paquete recibido
        int userPort = packet.getPort();

        // Obtiene los datos del paquete recibido como un arreglo de bytes
        byte[] byteMessage = packet.getData();

        // Itera sobre los puertos de los usuarios conectados
        for (int forwardPort : users) {
            // Evita reenviar el mensaje al remitente original
            if (forwardPort != userPort) {
                // Convierte los datos del paquete en una cadena de texto
                String messageText = new String(byteMessage, 0, packet.getLength());

                // Convierte el texto del mensaje en un arreglo de bytes
                byte[] formattedMessageBytes = messageText.getBytes();

                // Crea un nuevo paquete con el mensaje formateado y lo envía al usuario correspondiente
                DatagramPacket forward =
                        new DatagramPacket(formattedMessageBytes, formattedMessageBytes.length, packet.getAddress(), forwardPort);
                try {
                    socket.send(forward);
                } catch (IOException e) {
                    // Maneja la excepción según tus necesidades (por ejemplo, regístrala)
                    e.printStackTrace();
                }
            }
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
