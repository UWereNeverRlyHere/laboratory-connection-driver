module ywh.labs.services {
    // Export all packages
    exports ywh.services.communicator;
    exports ywh.services.data.enums;
    exports ywh.services.data.models;
    exports ywh.services.data.serial_port;
    exports ywh.services.data_processor;
    exports ywh.services.data.mapping;

    exports ywh.services.device;
    exports ywh.services.device.parsers;
    exports ywh.services.device.protocol;
    exports ywh.services.device.protocol.astm;
    exports ywh.services.device.protocol.hl7;
    exports ywh.services.exceptions;
    exports ywh.services.files;
    exports ywh.services.port_sender;
    exports ywh.services.port_sender.impl;
    exports ywh.services.settings;
    exports ywh.services.settings.data;
    exports ywh.services.printing;
    exports ywh.services.data.models.observation;
    exports ywh.services.device.parsers.mindray;


    opens ywh.services.data.models.api to com.google.gson,connection.driver.application;
    opens ywh.services.data.models to com.google.gson,connection.driver.application;
    opens ywh.services.data.models.observation to com.google.gson,connection.driver.application;
    opens ywh.services.settings.data to com.google.gson,connection.driver.application;
    opens ywh.services.data.serial_port to com.google.gson,connection.driver.application;

    exports ywh.services.device.parsers.fujifilm;
    exports ywh.services.device.parsers.ise;
    exports ywh.services.web;
    exports ywh.services.device.protocol.custom;
    exports ywh.services.tools;
    exports ywh.services.device.parsers.hti;


// Required modules
    requires java.base;
    requires java.desktop;

    // Apache POI
    requires org.apache.poi.ooxml;
    requires org.apache.poi.ooxml.schemas;

    // PDFBox
    requires org.apache.pdfbox;

    // Core Java modules
    requires java.logging;
    requires org.apache.commons.logging;

    // Logging
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;



    // Optional annotations
    requires static org.jetbrains.annotations;
    requires com.sun.jna.platform;
    requires com.google.gson;
    requires com.github.albfernandez.javadbf;
    requires ywh.labs.repository;
    requires ywh.labs.commons;
    requires static lombok;
    requires java.net.http;
    requires org.apache.commons.net;
    requires org.jfree.jfreechart;
    requires org.apache.commons.compress;
    requires com.fazecast.jSerialComm;
}
