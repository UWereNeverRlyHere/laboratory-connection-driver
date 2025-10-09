package ywh.services.device.protocol;

import ywh.logging.DeviceLogger;


/** Відповідає лише за формування кадрів. */
public interface IProtocol {

    /** Викликається Communicator'ом для кожного отриманого байта. */
    void onByte(byte b);
    void setIdleTimeoutMs(long idleTimeoutMs);

    /** Слухач завершених кадрів. */
    @FunctionalInterface
    interface IFrameListener {
        void onFrame(byte[] frame);
    }

    @FunctionalInterface
    interface ITransport {
        void send(byte[] data);

        default void send(byte b) {
            send(new byte[]{b});
        }
    }

    void setTransport(ITransport transport);

    /** Реєстрація слухача. */
    void setFrameListener(IFrameListener listener);

    /** Скидання внутрішнього стану (за потреби). */
    void reset();

    void setLogger(DeviceLogger logger);

    void clearFrameListener();
    void clearTransport();


}
