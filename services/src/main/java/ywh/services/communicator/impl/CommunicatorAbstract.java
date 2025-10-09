package ywh.services.communicator.impl;

import ywh.services.communicator.ICommunicator;
import ywh.services.data.enums.DeviceStatus;
import ywh.services.device.DeviceStatusListener;
import ywh.logging.DeviceLogger;

public abstract class CommunicatorAbstract implements ICommunicator {
    protected DeviceLogger logger;
    protected volatile ByteListener byteListener;

    protected CommunicatorAbstract(DeviceLogger logger) {
        this.logger = logger;
    }

    private volatile DeviceStatusListener deviceStatusListener; // НОВИЙ
    /**
     * Повідомляє про зміну статусу підключення
     */
    protected void notifyDeviceStatus(DeviceStatus newStatus) {
        if (deviceStatusListener != null) {
            try {
                // Передаємо null як oldStatus, бо комунікатор не знає попередній статус Device
                deviceStatusListener.onStatusChanged(null, newStatus);
            } catch (Exception e) {
                logger.error("Error in device status listener", e);
            }
        }
    }
    @Override
    public void setByteListener(ByteListener listener) {
        this.byteListener = listener;
    }

    @Override
    public void setDeviceStatusListener(DeviceStatusListener listener) {
        this.deviceStatusListener = listener;
    }
    @Override
    public void clearDeviceStatusListener() {
        this.deviceStatusListener = null;
    }

    @Override
    public void clearByteListener() {
        this.byteListener = null;
    }

}
