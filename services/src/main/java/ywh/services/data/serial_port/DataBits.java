package ywh.services.data.serial_port;

public enum DataBits {
    DT_5(5),
    DT_6(6),
    DT_7(7),
    DT_8(8);


    private final int dataBit;
    DataBits(int dataBit) {
        this.dataBit = dataBit;
    }

    public int toInt() {
        return dataBit;
    }


    @Override
    public String toString() {
        switch (this){
            case DT_5:
                return "5 біт";
            case DT_6:
                return "6 біт";
            case DT_7:
                return "7 біт";
            case DT_8:
                return "8 біт";
            default:
                return super.toString();
        }
    }
}
