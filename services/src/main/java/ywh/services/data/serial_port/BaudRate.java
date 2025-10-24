package ywh.services.data.serial_port;

public enum BaudRate {
    BR_110(110),
    BR_300(300),
    BR_600(600),
    BR_1200(1200),
    BR_2400(2400),
    BR_4800(4800),
    BR_9600(9600),
    BR_14400(14400),
    BR_19200(19200),
    BR_38400(38400),
    BR_56000(56000),
    BR_57600(57600),
    BR_115200(115200),
    BR_128000(128000),
    BR_256000(256000);


    private final int value;

    BaudRate(int baudRate) {
        this.value = baudRate;
    }

    public int toInt() {
        return value;
    }

    @Override
    public String toString() {
        return value + " бод";
    }
}
