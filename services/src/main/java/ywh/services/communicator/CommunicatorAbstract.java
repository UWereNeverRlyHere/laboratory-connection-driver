package ywh.services.communicator;

import ywh.services.data.enums.DeviceStatus;
import ywh.services.device.DeviceStatusListener;
import ywh.logging.DeviceLogger;

import java.util.concurrent.atomic.AtomicReference;

public abstract class CommunicatorAbstract implements ICommunicator {
    protected DeviceLogger logger;
    protected final AtomicReference<ByteListener> byteListener = new AtomicReference<>();

    protected CommunicatorAbstract(DeviceLogger logger) {
        this.logger = logger;
    }

    private final AtomicReference<DeviceStatusListener> deviceStatusListener = new AtomicReference<>();

    /**
     * Повідомляє про зміну статусу підключення
     */
    protected void notifyDeviceStatus(DeviceStatus newStatus) {
        DeviceStatusListener listener = deviceStatusListener.get();
        if (listener != null) {
            try {
                // Передаємо null як oldStatus, бо комунікатор не знає попередній статус Device
                listener.onStatusChanged(null, newStatus);
            } catch (Exception e) {
                logger.error("Error in device status listener", e);
            }
        }
    }
    @Override
    public void setByteListener(ByteListener listener) {
        this.byteListener.set(listener);
    }

    @Override
    public void setDeviceStatusListener(DeviceStatusListener listener) {
        this.deviceStatusListener.set(listener);
    }

    @Override
    public void clearDeviceStatusListener() {
        this.deviceStatusListener.set(null);
    }

    @Override
    public void clearByteListener() {
        this.byteListener.set(null);
    }

}
