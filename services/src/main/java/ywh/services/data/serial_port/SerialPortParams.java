package ywh.services.data.serial_port;

import java.io.Serializable;

public class SerialPortParams implements Serializable {
    private Parity parity = Parity.PARITY_NONE;
    private StopBits stopBits = StopBits.SB_1;
    private DataBits dataBits = DataBits.DT_8;
    private BaudRate baudRate = BaudRate.BR_9600;

    public SerialPortParams(Parity parity, StopBits stopBits, DataBits dataBits, BaudRate baudRate) {
        this.parity = parity;
        this.stopBits = stopBits;
        this.dataBits = dataBits;
        this.baudRate = baudRate;
    }

    public SerialPortParams() {
    }

    public SerialPortParams(BaudRate baudRate) {
        this.baudRate = baudRate;
    }

    public Parity getParity() {
        return parity;
    }

    public void setParity(Parity parity) {
        this.parity = parity;
    }

    public StopBits getStopBits() {
        return stopBits;
    }

    public void setStopBits(StopBits stopBits) {
        this.stopBits = stopBits;
    }

    public DataBits getDataBits() {
        return dataBits;
    }

    public void setDataBits(DataBits dataBits) {
        this.dataBits = dataBits;
    }

    public BaudRate getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(BaudRate baudRate) {
        this.baudRate = baudRate;
    }
}
