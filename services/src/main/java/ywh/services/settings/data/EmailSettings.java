package ywh.services.settings.data;

import lombok.Data;

@Data
public final class EmailSettings {
    private final String smtpHost;
    private final int smtpPort;
    private final String senderEmail;
    private final String senderPassword;

    public EmailSettings(String smtpHost, int smtpPort, String senderEmail, String senderPassword) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.senderEmail = senderEmail;
        this.senderPassword = senderPassword;
    }


}
