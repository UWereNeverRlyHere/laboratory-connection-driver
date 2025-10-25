package ywh.services.device.protocol.custom;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

public interface IAckStrategy {
    /**
     * @param b   черговий байт (той самий, що йде у onByte)
     * @param buf поточний вміст буфера (read-only!)
     * @return Optional<byte[]> – пакет, який слід надіслати у відповідь,
     *         або empty() якщо зараз нічого не відправляти
     */
    default Optional<byte[]> analyze(byte b, ByteArrayOutputStream buf) { return Optional.empty(); }

    default void reset() { }
}
