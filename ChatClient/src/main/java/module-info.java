module org.example.chatclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml.crypto;

    opens org.example.chatclient.Client to javafx.graphics;

    exports org.example.chatclient.Client;
}