package ywh.commons;

import com.fazecast.jSerialComm.SerialPort;
import ywh.commons.data.SerialPortMetaData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SerialPortUtil {
    private SerialPortUtil() {
    }

    public static CompletableFuture<List<SerialPortMetaData>> getAllSystemSerialPorts(String selected) {
        return Task.startDetached("com_port_scan", () -> {
            List<SerialPortMetaData> allPorts = new ArrayList<>();
            SerialPort[] commPorts = SerialPort.getCommPorts();

            for (SerialPort commPort : commPorts) {
                String portName = commPort.getSystemPortName();

                // Якщо це обраний порт, не перевіряємо його доступність
                if (!TextUtils.isNullOrEmpty(selected) && portName.equals(selected)) {
                    allPorts.add(new SerialPortMetaData(portName, false, true));
                    continue;
                }

                // Спроба відкрити порт для перевірки доступності
                boolean isBusy = false;
                try {
                    if (commPort.openPort()) {
                        // Порт успішно відкрився - він вільний
                        commPort.closePort();
                        Thread.sleep(2000);
                    } else {
                        // Не вдалося відкрити - порт зайнятий
                        isBusy = true;
                    }
                } catch (Exception ignored) {
                    // Будь-яка помилка означає, що порт зайнятий
                    isBusy = true;
                } finally {
                    // Переконуємося, що порт закритий
                    try {
                        if (commPort.isOpen()) {
                            commPort.closePort();
                        }
                    } catch (Exception ignored) {
                    }
                }

                allPorts.add(new SerialPortMetaData(portName, isBusy, false));
            }
            allPorts.sort(Comparator.comparing(
                    port -> {
                        Matcher m = Pattern.compile("(\\D+)(\\d+)").matcher(port.getName());
                        if (m.matches()) {
                            return m.group(1) + String.format("%010d", Integer.parseInt(m.group(2)));
                        }
                        return port.getName();
                    }
            ));
            return allPorts;
        });
    }
}