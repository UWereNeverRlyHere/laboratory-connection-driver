package ywh.services.device;

import ywh.services.data.enums.DeviceStatus;

@FunctionalInterface
public interface DeviceStatusListener {
    void onStatusChanged(DeviceStatus oldStatus, DeviceStatus newStatus);
}
