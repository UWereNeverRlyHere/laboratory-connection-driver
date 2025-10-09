module ywh.labs.commons {
    exports ywh.commons;
    exports ywh.logging;

    requires java.desktop;
    requires ch.qos.logback.classic;
    requires org.slf4j;
    requires ch.qos.logback.core;
    requires static lombok;

    opens ywh.logging to ch.qos.logback.classic, ch.qos.logback.core;
    exports ywh.commons.data;
    opens ywh.commons.data to ch.qos.logback.classic, ch.qos.logback.core;

}