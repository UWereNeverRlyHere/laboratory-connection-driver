package ywh.services.data_processor;

import ywh.commons.data.ConsoleColor;
import ywh.logging.IServiceLogger;
import ywh.services.data.enums.ProcessResult;
import ywh.services.data.mapping.ApiObservationMapper;
import ywh.services.data.models.ProcessorResult;
import ywh.services.data.models.api.Order;
import ywh.services.data.models.api.ResultResponse;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.settings.data.DeviceSettings;
import ywh.services.web.ApiClient;
import ywh.logging.DeviceLogger;
import ywh.logging.ServiceLoggerFactory;

import java.time.Duration;
import java.util.Optional;

public class APIProcessor {

    private final DeviceSettings settings;
    private final IServiceLogger apiClientLogger;

    public APIProcessor(DeviceSettings settings) {
        String loggerName = settings.getServiceName() + " API client";
        this.apiClientLogger = ServiceLoggerFactory.createLogger(loggerName, ConsoleColor.BLUE);
        this.settings = settings;
    }

    public ProcessorResult process(ObservationData data) {
        ApiClient client = ApiClient.create(
                        settings.getApiSettings().getResultUrl(),
                        Duration.ofMillis(settings.getApiSettings().getTimeOut()))
                .logger(this.apiClientLogger);

        var requestModel = ApiObservationMapper.map(data, settings.getCachedParser().getName(), settings.getSerialNumber());

        Optional<ResultResponse> responseModel = client.post(requestModel, ResultResponse.class);
        if (responseModel.isEmpty()) {
            return ProcessorResult.createApi(ProcessResult.FAILURE, responseModel);
        }
        return ProcessorResult.createApi(ProcessResult.SUCCESS, responseModel);
    }

    public Order getOrderById(String id, DeviceLogger logger){
        try {
            logger.log("Got enquiry for order with id: " + id + " sending request to API...");
            var client = ApiClient.create(
                            settings.getApiSettings().getOrderUrl(),
                            Duration.ofMillis(settings.getApiSettings().getOrderTimeOut()))
                    .addUriParam("barcode", id)
                    .addUriParam("serialNumber", settings.getSerialNumber())
                    .logger(this.apiClientLogger)
                    ;

            var order =  client.get(Order.class);
            if (order.isEmpty()) logger.error("Error during getting order from API... See API logs for details");
            else {
                logger.log("Got order from API: " + "id=" + order.get().getId() + "indicators=" + String.join(",", order.get().getIndicators()));
                logger.log("See API logs for details");
            }
            var resultOrder = order.orElse(new Order());
            resultOrder.distinctIndicators();
            return resultOrder;
        }catch (Exception e){
            logger.error("Error during getting order from API: ", e);
            return new Order();
        }
    }
}
