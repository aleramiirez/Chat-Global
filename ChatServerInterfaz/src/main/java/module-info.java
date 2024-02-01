module org.example.chatserverinterfaz {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.chatserverinterfaz to javafx.fxml;
    exports org.example.chatserverinterfaz;
}