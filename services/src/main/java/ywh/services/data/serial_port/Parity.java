package ywh.services.data.serial_port;

public enum Parity {
    PARITY_NONE(0),
    PARITY_ODD(1),
    PARITY_EVEN(2),
    PARITY_MARK(3),
    PARITY_SPACE(4);


    private final int parity;
    Parity(int parity) {
        this.parity = parity;
    }

    public int toInt() {
        return parity;
    }


    @Override
    public String toString() {
        return switch (this) {
            case PARITY_NONE -> "NONE (0)";
            case PARITY_ODD -> "ODD (1)";
            case PARITY_EVEN -> "EVEN (2)";
            case PARITY_MARK -> "MARK (3)";
            case PARITY_SPACE -> "SPACE (4)";
            default -> super.toString();
        };
    }
}
