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
    BR_57600(57600),
    BR_115200(115200),
    BR_128000(128000),
    BR_256000(256000);


    private final int BAUD_RATE;

    BaudRate(int baudRate) {
        this.BAUD_RATE = baudRate;
    }

    public int toInt() {
        return BAUD_RATE;
    }


    @Override
    public String toString() {
        switch (this) {
            case BR_110:
                return "110 бод";
            case BR_300:
                return "300 бод";
            case BR_600:
                return "600 бод";
            case BR_1200:
                return "1200 бод";
            case BR_2400:
                return "2400 бод";
            case BR_4800:
                return "4800 бод";
            case BR_9600:
                return "9600 бод";
            case BR_14400:
                return "14400 бод";
            case BR_19200:
                return "19200 бод";
            case BR_38400:
                return "38400 бод";
            case BR_57600:
                return "57600 бод";
            case BR_115200:
                return "115200 бод";
            case BR_128000:
                return "128000 бод";
            case BR_256000:
                return "256000 бод";
            default:
                return super.toString();
        }
    }
}
