module connection.driver.application {
    // Export packages
    exports ywh.fx_app.application;
    exports ywh.fx_app.app_exceptions;
    exports ywh.fx_app.tray;
    exports ywh.fx_app.device;
    exports ywh.fx_app.configs;
    exports ywh.fx_app.app_data;
    exports ywh.fx_app.app_managers;
    exports ywh.fx_app.app_custom_nodes;
    opens ywh.fx_app.app_custom_nodes;

    opens ywh.fx_app.configs to javafx.fxml;
    opens ywh.fx_app.application to javafx.graphics;
    opens ywh.fx_app.device to javafx.fxml;
    opens ywh.fx_app.tray to javafx.graphics;
    opens ywh.fx_app.clarification to javafx.fxml;


    // Required modules
    requires java.base;
    requires java.desktop;

    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing; // Додайте цей рядок!


    // Our services module
    requires ywh.labs.services;

    // UI libraries
    requires jdk.unsupported;

    requires com.jfoenix;
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    // Optional annotations
    requires static org.jetbrains.annotations;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires java.logging;
    requires ywh.labs.commons;
    requires ywh.labs.repository;
    requires static lombok;
    requires MaterialFX;
    requires atlantafx.base;
    requires javafx.graphics;
}
