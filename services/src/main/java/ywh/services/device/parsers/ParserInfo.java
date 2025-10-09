package ywh.services.device.parsers;


import ywh.services.device.protocol.IProtocol;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ParserInfo {
    String name();
    Class<? extends IProtocol> defaultProtocol();
    String encoding() default "UTF-8";
    int sendPause()default 0;
    long defaultIdleTimeout()default 5000;
}

