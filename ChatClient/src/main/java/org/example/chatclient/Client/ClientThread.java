package org.example.chatclient.Client;

import javafx.scene.control.TextArea;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Esta clase representa un hilo de cliente que recibe mensajes del servidor y los muestra en un TextArea.
 *
 * @author Alejandro Ramírez
 */
public class ClientThread extends Thread {

    // Socket para la comunicación con el servidor
    private DatagramSocket socket;

    // Almacena los datos recibidos del servidor en bytes
    private byte[] incoming = new byte[256];

    // TextArea donde se mostrarán los mensajes recibidos
    private TextArea textArea;

    /**
     * Constructor de la clase ClientThread.
     *
     * @param socket   Socket para la comunicación con el servidor.
     * @param textArea TextArea donde se mostrarán los mensajes recibidos.
     */
    public ClientThread(DatagramSocket socket, TextArea textArea) {
        this.socket = socket;
        this.textArea = textArea;
    }

    /**
     * Método que se ejecuta cuando se inicia el hilo.
     * Este método recibe continuamente mensajes del servidor y los muestra en el TextArea.
     */
    @Override
    public void run() {
        System.out.println("starting thread");

        // Bucle infinito para recibir mensajes continuamente
        while (true) {

            // Crea un nuevo paquete DatagramPacket para recibir datos
            DatagramPacket packet = new DatagramPacket(incoming, incoming.length);
            try {
                // Recibe un paquete del servidor
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Filtra los mensajes de inicialización para que no se muestren en la interfaz del usuario
            // Convierte los datos recibidos en una cadena de texto
            String message = new String(packet.getData(), 0, packet.getLength());

            // Obtiene el texto actual del TextArea
            String current = textArea.getText();

            // Agrega el mensaje al TextArea y lo muestra
            textArea.setText(current + message + "\n");
        }
    }
}
