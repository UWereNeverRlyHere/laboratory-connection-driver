package ywh.labs.data_processor;

import org.junit.jupiter.api.Test;
import ywh.services.data.enums.ProcessResult;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.data_processor.ObservationResultProcessor;
import ywh.services.settings.data.DeviceSettings;
import ywh.logging.DeviceLogger;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbfResultProcessorTest {

    @Test
    void shouldCreateDbfFileFromResultModel() {
        var processorParams = new DeviceSettings();
        var processor = new ObservationResultProcessor(new DeviceLogger(),processorParams);
        var data = new ObservationData("123456790");
        data.put("WBC", "8.8");
        data.put("HB", "10.0");
        data.put("RBC", "1.1");
        data.put("PLT", "1.1");
        data.put("MCH", "1.1");
        data.putImage("WBC histogram","iVBORw0KGgoAAAANSUhEUgAAAJwAAABvCAIAAACW3D6tAAAAA3NCSVQICAjb4U/gAAAACXBIWXMAAAsTAAALEwEAmpwYAAADaUlEQVR4nO3dMWgTYRiA4a+S4QaHf+iQQpeCS8ShBx080MGUCqY4WHApKEhwKAGhxApS3IqC6FIySe2mg9AKQlKwtA7CRQicg9AOSiJEuEKHP4NDwcI5HFiN1drSXMjn+wwluQuXS958d0lbiAgAAAAAAAAAQLtnz59lx7Lx5eJMcWNz48cqv+pnx7JhMwybYb1RD94HhalCvMpxnNJ8KWyGdtsGtcAdcbuw6/iT2Xuzc/fn4stBLQhqgXfWE5H0YDpshu6wW/9YN8YYYzJnMmEzjPutvV4rzZccxxGRiSsT1lrTb7r4KP5zqbbr62/WHz94LCKm30hKni4+Hc+NV99Vsxey5dVyfJtWqxX/3NjcGBgYkBE5nTk9enE0Xrv8ctk56TgpJ8FHgb9Lid22kpLJa5OPHj4aOjUU1AIRWVhcmLg64Q671trSfKk0X1paXipXypKS/M380oulbu839pxoX7Arfs33RrzxS+OVlUrjU8P0G2NM9nx2fXVdRHZ2dvya79d8/62fyWRyYzmGsgcUp4vFmWK9UY+PzQtPFgpTBb/qi0h8Tv1xy/yNfLlS9s56Py8Ukdm7s945L9m9xp7fJlWkslrJX89vftiUXRGRykqlcKsQj+kvUuKd9xqfG9V31dbXVnG6GC92R9zbd25vfdnq7I7jsOy23fu4ctKJvkXxe2B32I2iyFprt621tvyqHL/LTQ+m/apfb9SDWhA2w9zlXDf3HsfIGJMeTHd7LwAAAI5JX9v1KIr6+toXxssPven9toME7PM5Fb2OqAoRVSGiKkRUhYiqEFEVIqpCHYx6hN9X4FgwqQoRVSGiKkRUhYiqEFEVIqpCRFWIqAoRVSGiKkRUhYiqEFEVIqpCRFWIqAoRVaF/iso/pvQWJlUhoirU2agct7uCSVWIqAoRVSGiKkRUhYiqEFEVIqpCRFWIqAoRVSGiKkRUhToelT/UJI9JVYioChFVoYOjclLsOUyqQkRVKImoHMATxqQqRFSFiKoQURVKKCrvlZLEpCpEVIWIqtABUY/xXMhpNTFMqkJEVSjRqByBk8GkKpR0VIY1AX+L2qEAdO207hx+oygibeek/rQigSf997uIv5v+5+V8W/0RtD9lDFDX9cbruBMvlP95mwfiI41CRAUAAAAAAAAAoAd8B7buIqpCIYJeAAAAAElFTkSuQmCC");
        data.putImage("RBC histogram","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");
        var result = processor.process(data);
        assertTrue(result.getFile().isPresent());
        System.err.println(result.getFile().get().getAbsolutePath());
        assertSame(ProcessResult.SUCCESS, result.getResult());
        assertTrue(result.getFile().get().exists());
    }
}