package ywh.services.settings.data;


import lombok.Data;

@Data
public final class FtpSettings {
    private final String server;
    private final String user;
    private final String password;
    private boolean isFtps ;

    public FtpSettings(String server, String user, String password) {
        this.server = server;
        this.user = user;
        this.password = password;
    }



    public String getUiString(){
        return server + ":" + user;
    }
}
