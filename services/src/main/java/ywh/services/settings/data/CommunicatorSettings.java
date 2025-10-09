package ywh.services.settings.data;

import lombok.Data;
import ywh.services.data.enums.CommunicatorType;

import java.util.concurrent.ThreadLocalRandom;

@Data
public class CommunicatorSettings {

    private CommunicatorType type;
    private String host = "127.0.0.1";
    private int port = ThreadLocalRandom.current().nextInt(1024, 65535);


    /**
     * Фабричный метод для создания параметров TCP_HOST с заданным портом и парсером.
     */
    public static CommunicatorSettings createTcpHostParams(int port) {
        CommunicatorSettings params = new CommunicatorSettings();
        params.setType(CommunicatorType.TCP_HOST);
        params.setPort(port);
        return params;
    }



}