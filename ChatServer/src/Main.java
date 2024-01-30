import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Main {

    private static byte[] incoming = new byte[1024];
    private static final int PORT = 5010;
    private static final int FORWARD_PORT = 6010;
    private static DatagramSocket socket;
    private static InetAddress address;
    private static ArrayList<Integer> users = new ArrayList<>();

    // Conjunto para almacenar Nicks registrados
    private static Set<String> registeredUsernames = new HashSet<>();

    static {
        try {
            socket = new DatagramSocket(PORT);
            address = InetAddress.getByName("localhost");
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        System.out.println("Server started on port " + PORT);

        while (true) {
            DatagramPacket packet = new DatagramPacket(incoming, incoming.length);

            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String message = new String(packet.getData(), 0, packet.getLength());

            if (message.equals("STOP")) {
                System.out.println("Servidor detenido por solicitud del cliente.");
                socket.close();
                System.exit(0);
            }else if (message.startsWith("validate;")) {
                System.out.println("validate;");
                String requestedUsername = message.substring(9);
                handleValidation(requestedUsername, packet.getAddress(), packet.getPort());
            } else if (message.contains("disconnect;")) {
                System.out.println("disconnect;");
                String[] parts = message.split(";");
                String disconnectedUser = parts[1];
                registeredUsernames.remove(disconnectedUser);  // Elimina al usuario desconectado de la lista
            } else if (message.startsWith("init;")) {
                System.out.println("init;");
                // Initialization message
                // Assuming the client sends its port as part of the initialization message
                users.add(packet.getPort());
            } else if (message.startsWith("img;")) {
                // Image message
                handleImageMessage(packet);
            } else {
                // Forward text messages
                forwardTextMessage(packet);
            }
        }
    }
    private static void handleValidation(String requestedUsername, InetAddress address, int port) {
        if (registeredUsernames.contains(requestedUsername)) {
            // Enviar mensaje al cliente indicando que el Nick no es válido
            sendMessage("invalid", address, port);
        } else {
            // Enviar mensaje al cliente indicando que el Nick es válido
            sendMessage("valid", address, port);
            // Registrar el Nick
            registeredUsernames.add(requestedUsername);
        }
    }

    private static void forwardTextMessage(DatagramPacket packet) {
        int userPort = packet.getPort();
        byte[] byteMessage = packet.getData();

        for (int FORWARD_PORT : users) {
            if (FORWARD_PORT != userPort) {
                // Construir el mensaje correctamente
                String messageText = new String(byteMessage, 0, packet.getLength());
                byte[] formattedMessageBytes = messageText.getBytes();

                DatagramPacket forward = new DatagramPacket(formattedMessageBytes, formattedMessageBytes.length, address, FORWARD_PORT);

                try {
                    socket.send(forward);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        /*
        int userPort = packet.getPort();
        byte[] byteMessage = packet.getData();

        for (int forwardPort : users) {
            if (forwardPort != userPort) {
                // Construir el mensaje correctamente
                String messageText = new String(byteMessage, 0, packet.getLength());
                byte[] formattedMessageBytes = messageText.getBytes();

                DatagramPacket forward = new DatagramPacket(formattedMessageBytes, formattedMessageBytes.length, address, FORWARD_PORT);

                try {
                    socket.send(forward);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
         */
    }

    private static void handleImageMessage(DatagramPacket packet) {
        int userPort = packet.getPort();
        byte[] imageData = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), 0, imageData, 0, packet.getLength());

        // Guardar la imagen en la carpeta Descargas
        String fileName = "C:\\Users\\aleja\\Downloads\\image_" + System.currentTimeMillis() + ".png";
        Path imagePath = Path.of(fileName);

        try {
            Files.write(imagePath, imageData);
            System.out.println("Image saved: " + fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Reenviar la imagen a otros usuarios
        for (int FORWARD_PORT : users) {
            if (FORWARD_PORT != userPort) {
                // Construir el mensaje de imagen correctamente
                String imageMessage = "img;" + userPort + ";" + fileName;
                byte[] imageMessageBytes = imageMessage.getBytes();

                DatagramPacket forward = new DatagramPacket(imageMessageBytes, imageMessageBytes.length, address, FORWARD_PORT);

                try {
                    socket.send(forward);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void sendMessage(String message, InetAddress address, int port) {
        byte[] messageBytes = message.getBytes();
        DatagramPacket validationResponse = new DatagramPacket(messageBytes, messageBytes.length, address, port);

        try {
            socket.send(validationResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
