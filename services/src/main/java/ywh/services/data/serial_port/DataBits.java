package ywh.services.data.serial_port;

public enum DataBits {
    DT_5(5),
    DT_6(6),
    DT_7(7),
    DT_8(8);


    private final int value;
    DataBits(int value) {
        this.value = value;
    }

    public int toInt() {
        return value;
    }


    @Override
    public String toString() {
        return value + " біт";
    }
}
