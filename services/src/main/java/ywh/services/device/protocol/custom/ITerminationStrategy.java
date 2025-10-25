package ywh.services.device.protocol.custom;

import java.io.ByteArrayOutputStream;
@FunctionalInterface
public interface ITerminationStrategy {
    boolean analyze(byte b, ByteArrayOutputStream buf);
    default void reset() { /* no-op by default */ }
}