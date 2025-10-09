package ywh.services.data.enums;

public enum SpecialBytes {
    SOH(0x01), STX(0x02), ETX(0x03), ETB(23),
    EOT(0x04), ENQ(0x05), ACK(0x06), NAK(21),
    CR(0x0D), LF(0x0A), GS(0x1D), RS(0x1E), SPACE(32), FS(28),
    HL7START(0x0B), HL7END(0x1C);

    private final byte specialByte;

    SpecialBytes(int specialByte) {
        this.specialByte = (byte) specialByte;
    }

    public byte getValue() {
        return specialByte;
    }

    @Override
    public String toString() {
        return new String(new byte[]{specialByte});
    }


}
