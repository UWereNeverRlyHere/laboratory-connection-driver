package ywh.services.data.serial_port;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@Builder
public class SerialParams implements Serializable {
    private String portName = "";
    private Parity parity = Parity.PARITY_NONE;
    private StopBits stopBits = StopBits.SB_1;
    private DataBits dataBits = DataBits.DT_8;
    private BaudRate baudRate = BaudRate.BR_9600;

    public SerialParams(Parity parity, StopBits stopBits, DataBits dataBits, BaudRate baudRate) {
        this.parity = parity;
        this.stopBits = stopBits;
        this.dataBits = dataBits;
        this.baudRate = baudRate;
    }


    public SerialParams() {
    }

    public SerialParams(BaudRate baudRate) {
        this.baudRate = baudRate;
    }

    public SerialParams(String portName, BaudRate baudRate) {
        this(baudRate);
        this.portName = portName;
    }
}
