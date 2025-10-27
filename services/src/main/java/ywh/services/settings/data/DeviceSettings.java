package ywh.services.settings.data;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ywh.services.data.enums.Delimiter;
import ywh.services.data.enums.FileResultActions;
import ywh.services.device.parsers.IParser;
import ywh.services.device.parsers.IParserWithFixedPort;
import ywh.services.printing.PrintersService;
import ywh.services.printing.PrintingMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceSettings {
    private String serviceName = "Unknown";
    private String serialNumber = "Unknown";
    private boolean useDeviceDateTime = true;
    private boolean clarificationWindow = false;
    private Delimiter delimiter = Delimiter.DOT;
    private CommunicatorSettings communicatorSettings = new CommunicatorSettings();
    private PrintSettings printSettings = new PrintSettings(PrintersService.getDefaultPrinter(), PrintingMethod.AUTO,false, true);
    private ApiSettings apiSettings = new ApiSettings("http://localhost:8080/result", "http://localhost:8080/order", 30000);
    private List<FileResultActions> actions = List.of(FileResultActions.PRINT, FileResultActions.SAVE_DOCX, FileResultActions.SAVE_PDF);
    private FileResultProcessorSettings fileResultProcessorSettings = new FileResultProcessorSettings();

    @Expose(serialize = false, deserialize = false)
    private IParser cachedParser;
    private String parserClassName;


    public synchronized IParser getCachedParser() {
        return cachedParser;
    }
    public synchronized IParser getParser() throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        if (cachedParser != null) {
            return cachedParser;
        }
        cachedParser = (IParser) Class.forName(parserClassName)
                .getDeclaredConstructor()
                .newInstance();
        return cachedParser;
    }

    /**
     * Сохраняет имя класса из переданного объекта парсера и кэширует его.
     * @param parser объект, реализующий IParser; не должен быть null.
     */
    public  synchronized DeviceSettings setParser(IParser parser) {
        if (parser == null) {
            throw new IllegalArgumentException("Parser must not be null");
        }
        if (cachedParser instanceof IParserWithFixedPort parserWithFixedPort){
            this.communicatorSettings.setPort(parserWithFixedPort.getDefaultPort());
        }
        this.parserClassName = parser.getClass().getName();
        this.cachedParser = parser;
        this.serviceName = parser.getServiceName();
        return this;
    }

    public DeviceSettings setPrintSettings(PrintSettings printSettings) {
        this.printSettings = printSettings;
        return this;
    }


    public DeviceSettings setPort(int port) {
        this.communicatorSettings.setPort(port);
        return this;
    }
    public DeviceSettings setHost(String host) {
        this.communicatorSettings.setHost(host);
        return this;
    }
    public DeviceSettings setCommunicatorSettings(CommunicatorSettings communicatorSettings) {
        this.communicatorSettings = communicatorSettings;
        return this;
    }

    public DeviceSettings setActions(List<FileResultActions> actions) {
        this.actions = actions;
        return this;
    }
    public DeviceSettings setFileResultProcessorSettings(FileResultProcessorSettings fileResultProcessorSettings) {
        this.fileResultProcessorSettings = fileResultProcessorSettings;
        return this;
    }

    public String getLogFileName() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return getParser().getName() + " [" +communicatorSettings.getPort()+ "]";
    }

}
