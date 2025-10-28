package ywh.services.port_sender.impl;

import com.fazecast.jSerialComm.SerialPort;
import ywh.services.data.serial_port.BaudRate;
import ywh.services.data.serial_port.SerialParams;
import ywh.services.port_sender.IPortSender;

public class SerialPortSenderImpl implements IPortSender {
    private SerialPort serialPort;

    public SerialPortSenderImpl(String portName) throws Exception {
        // Знайти порт за назвою
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            if (port.getSystemPortName().equals(portName)) {
                serialPort = port;
                break;
            }
        }

        if (serialPort == null) {
            throw new Exception("Serial port " + portName + " not found");
        }

        SerialParams params = new SerialParams(BaudRate.BR_9600);

        // Налаштування порту
        serialPort.setComPortParameters(
                params.getBaudRate().toInt(),
                params.getDataBits().toInt(),
                params.getStopBits().toInt(),
                params.getParity().toInt()
        );

        // Відкриття порту
        if (!serialPort.openPort()) {
            throw new Exception("Failed to open serial port " + portName);
        }
    }

    @Override
    public void send(byte[] data) {
        try {
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.writeBytes(data, data.length);
                serialPort.closePort();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}