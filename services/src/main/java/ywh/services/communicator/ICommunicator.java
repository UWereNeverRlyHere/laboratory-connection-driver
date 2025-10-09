package ywh.services.communicator;


import ywh.services.communicator.impl.TcpClientCommunicatorImpl;
import ywh.services.communicator.impl.TcpHostCommunicatorImpl;
import ywh.services.settings.data.CommunicatorSettings;
import ywh.services.device.DeviceStatusListener;
import ywh.logging.DeviceLogger;


public interface ICommunicator extends Runnable, AutoCloseable {
    static ICommunicator create(CommunicatorSettings params, DeviceLogger logger)  {
        switch (params.getType()) {
            case TCP_HOST -> {
                return new TcpHostCommunicatorImpl(params.getPort(), logger);
            }
            case TCP_CLIENT -> {
                return new TcpClientCommunicatorImpl(params.getHost(), params.getPort(), logger);
            }
            case COM -> {
                //TODO createApi communicator
            }
            case FILE -> {
                //TODO createApi communicator
            }
        }
        return null;
    }

    /* ───── byte-listener ───── */
    @FunctionalInterface
    interface ByteListener {
        void onByte(byte b);
    }
    /**
     * Регистрирует слушателя; передаётся один байт за раз.
     */
    void setByteListener(ByteListener listener);

    /* ───── существующие методы ───── */
    void sendBytes(byte[] data);

    void sendByte(byte data);

    void setDeviceStatusListener(DeviceStatusListener listener);
    void clearDeviceStatusListener();
    void clearByteListener();

}