package ywh.services.device.protocol.hl7;

import ywh.commons.DateTime;

public class HL7V231Helper {
    private String hl7Id;
    private String ackType;
    private String ack;
    private String ackMessage;
    private String ackCode;
    private String coding;
    private static final String START = "\u000B";
    private static final String END = "\u001C";
    private static final String CR = "\r";


    public void setCoding(String coding) {
        this.coding = coding;
    }


    private String getAckMessage() {
        return ackMessage;
    }

    public void setAckMessage(String ackMessage) {
        this.ackMessage = ackMessage;
    }

    private String getAckCode() {
        return ackCode;
    }

    public void setAckCode(String ackCode) {
        this.ackCode = ackCode;
    }

    public String getAck() {
        return ack;
    }

    public void setAck(String ack) {
        this.ack = ack;
    }



    public String getResponse() {
        setAAresponse();
        return START + "MSH|^~\\&|||||" + DateTime.getHl7DateTime() + "||ACK^R01|" + getHl7Id() + "|P|2.3.1||||" + getAckType() + "||" + coding + "|||" + CR +
                "MSA|" + getAck() + "|" + getHl7Id() + "|" + getAckMessage() + "|||" + getAckCode() + CR + END + CR;
    }

    private String getHl7Id() {
        return hl7Id;
    }

    public void setHl7Id(String hl7Id) {
        this.hl7Id = hl7Id;
    }

    private String getAckType() {
        return ackType;
    }

    public void setAckType(String ackType) {
        this.ackType = ackType;
    }

    private void set101ResponseError() {
        setAck("AE");
        setAckCode("101");
        setAckMessage("Required field is missing");
    }

    private void setAAresponse() {
        setAck("AA");
        setAckCode("0");
        setAckMessage("Message accepted");
    }



}
