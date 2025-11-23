module com.ether.society {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    opens com.ether.society to javafx.fxml;
    opens com.ether.society.ui to javafx.fxml;
    opens com.ether.society.model to javafx.base;

    exports com.ether.society;
    exports com.ether.society.ui;
    exports com.ether.society.model;
    exports com.ether.society.config;
    exports com.ether.society.core;
    exports com.ether.society.h3;
    exports com.ether.society.gpu;
    exports com.ether.society.data;
    exports com.ether.society.util;
    exports com.ether.society.database;
}
