package ywh.services.port_sender;

import ywh.services.port_sender.impl.SerialPortSenderImpl;
import ywh.services.port_sender.impl.TcpClientSenderImpl;

/**
 * FOR TESTS ONLY
 */
public interface IPortSender {
    void send(byte[] data);

    static IPortSender create(String port) throws Exception {
        return new SerialPortSenderImpl(port);
    }

    static IPortSender create(String ip, int port) {
        return new TcpClientSenderImpl(ip, port);
    }

    static IPortSender create(String ip, String port) throws NumberFormatException {
        return new TcpClientSenderImpl(ip, Integer.parseInt(port));
    }
}
