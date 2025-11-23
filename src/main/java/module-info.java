module com.ether.society {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;

    opens com.ether.society to javafx.fxml;

    exports com.ether.society;
    exports com.ether.society.model;
    exports com.ether.society.ui;
}
