package ywh.services.device.parsers;

import ywh.services.communicator.ICommunicator;
import ywh.services.settings.data.CommunicatorSettings;
import ywh.services.data.models.ParsingResult;
import ywh.services.device.protocol.IProtocol;
import ywh.logging.DeviceLogger;
import ywh.services.settings.data.DeviceSettings;

import java.nio.charset.Charset;

/** Перетворює кадри в обʼєкти / відповіді. */
public interface IParser {
    IProtocol getProtocol();

    /** Викликається, коли протокол надав черговий кадр. */
    void parse(byte[] data);

    /* ─── Події ─── */

    @FunctionalInterface
    interface ResponseListener {
        void onResponse(ParsingResult result);
    }

    ICommunicator createDefaultCommunicator(CommunicatorSettings params, DeviceLogger logger);

    Charset getCharset();
    String getName();
    void setLogger(DeviceLogger logger);

    void addResponseListener(ResponseListener listener);
    void removeResponseListener(ResponseListener listener);
    void clearResponseListeners();
    void setDeviceSettings(DeviceSettings settings);
    String getServiceName();
}
