package ywh.commons;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import ywh.commons.data.SerialPortMetaData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SerialPortUtil {
    private SerialPortUtil() {
    }

    public static CompletableFuture<List<SerialPortMetaData>> getAllSystemSerialPorts(String selected) {
        return Task.startDetached("com_port_scan", () -> {
            SerialPort sp;
            List<SerialPortMetaData> allPorts = new ArrayList<>();
            for (String comPort : SerialPortList.getPortNames()) {
                if (!TextUtils.isNullOrEmpty(selected) && comPort.equals(selected)) {
                    allPorts.add(new SerialPortMetaData(comPort, false, true));
                    continue;
                }
                sp = new SerialPort(comPort);
                try {
                    sp.openPort();
                    allPorts.add(new SerialPortMetaData(comPort, false, false));
                    sp.closePort();
                    Thread.sleep(100);
                } catch (Exception ignored) {
                    allPorts.add(new SerialPortMetaData(comPort, true, false));
                } finally {
                    try {
                        sp.closePort();
                    } catch (SerialPortException ignored) {
                    }
                }
            }
            return allPorts;
        });
    }
}
