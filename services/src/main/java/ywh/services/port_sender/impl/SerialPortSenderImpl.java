package ywh.services.port_sender.impl;

import jssc.SerialPort;
import jssc.SerialPortException;
import ywh.services.data.serial_port.BaudRate;
import ywh.services.data.serial_port.SerialPortParams;
import ywh.services.port_sender.IPortSender;

public class SerialPortSenderImpl implements IPortSender {
    SerialPort serialPort;

    public SerialPortSenderImpl(String port) throws SerialPortException {
        serialPort = new SerialPort(port);
        SerialPortParams params = new SerialPortParams(BaudRate.BR_9600);
        serialPort.openPort();
        serialPort.setParams(params.getBaudRate().toInt(), params.getDataBits().toInt(), params.getStopBits().toInt(), params.getParity().toInt());
    }

    public void send(byte[] data) {
        try {
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
            serialPort.writeBytes(data);
            serialPort.closePort();
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }
}
