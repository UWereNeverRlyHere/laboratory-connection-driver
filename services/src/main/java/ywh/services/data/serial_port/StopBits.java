package ywh.services.data.serial_port;

public enum StopBits {
    SB_1(1),
    SB_2(2);

    private final int stopBit;

    StopBits(int stopBit) {
        this.stopBit = stopBit;
    }

    public int toInt() {
        return stopBit;
    }


    @Override
    public String toString() {
        return switch (this) {
            case SB_1 -> "1 стоп біт";
            case SB_2 -> "2 стоп біта";
            default -> super.toString();
        };
    }
}
