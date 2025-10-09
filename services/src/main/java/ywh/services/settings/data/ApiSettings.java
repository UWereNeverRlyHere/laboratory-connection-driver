package ywh.services.settings.data;

import lombok.Data;


@Data
public final class ApiSettings {
    private final String resultUrl;
    private final String orderUrl;
    private final int timeOut;
    private int orderTimeOut = 10000;

    public ApiSettings(String resultUrl, String orderUrl, int timeOut) {
        this.resultUrl = resultUrl;
        this.orderUrl = orderUrl;
        this.timeOut = timeOut;
    }

    public ApiSettings(String resultUrl, String orderUrl, int timeOut, int orderTimeOut) {
        this.resultUrl = resultUrl;
        this.orderUrl = orderUrl;
        this.timeOut = timeOut;
        this.orderTimeOut = orderTimeOut;
    }
}
