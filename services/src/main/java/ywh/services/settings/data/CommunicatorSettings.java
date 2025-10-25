package ywh.services.settings.data;

import lombok.Data;
import ywh.services.data.enums.CommunicatorType;
import ywh.services.data.serial_port.SerialParams;

import java.util.concurrent.ThreadLocalRandom;

@Data
public class CommunicatorSettings {

    private CommunicatorType type;
    private String host = "127.0.0.1";
    private int port = ThreadLocalRandom.current().nextInt(1024, 65535);
    private SerialParams serialParams = new SerialParams();

    /**
     * Фабричный метод для создания параметров TCP_HOST с заданным портом и парсером.
     */
    public static CommunicatorSettings createTcpHostParams(int port) {
        CommunicatorSettings params = new CommunicatorSettings();
        params.setType(CommunicatorType.TCP_HOST);
        params.setPort(port);
        return params;
    }

    public static CommunicatorSettings createSerialParams(SerialParams serialParams) {
        CommunicatorSettings params = new CommunicatorSettings();
        params.setType(CommunicatorType.SERIAL);
        params.setSerialParams(serialParams);
        params.setHost("");
        params.setPort(0);
        return params;
    }

}