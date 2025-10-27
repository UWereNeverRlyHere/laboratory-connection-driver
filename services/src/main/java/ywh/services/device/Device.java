package ywh.services.device;

import lombok.Getter;
import org.slf4j.MDC;
import ywh.commons.Task;
import ywh.services.communicator.ICommunicator;
import ywh.services.data.enums.DeviceStatus;
import ywh.services.data.enums.ObservationKey;
import ywh.services.data.models.DeviceConfig;
import ywh.services.data.models.ParsingResult;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.data_processor.ObservationResultProcessor;
import ywh.services.device.parsers.IParser;
import ywh.services.device.protocol.IProtocol;
import ywh.services.exceptions.DeviceRuntimeException;
import ywh.logging.DeviceLogger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

public final class Device implements Runnable, AutoCloseable {

    /* ───────────── тип-обёртка для возврата девайса и потока ───────────── */
    public record DeviceHandle(Device device, Future<?> future) {
    }

    /* ───────────── статический ExecutorService для виртуальных потоков ───────────── */
    private static final ExecutorService EXECUTOR = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

    /**
     * Метод для завершения работы статического ExecutorService.
     * Его следует вызвать при завершении работы приложения.
     */
    public static void shutdownExecutor() {
        EXECUTOR.shutdownNow();
    }

    /* ───────────── статическая фабрика ───────────── */

    /**
     * Создаёт экземпляр устройства, запускает его в виртуальном потоке с именем потока и возвращает обёртку с девайсом и Future,
     * чтобы клиент мог в дальнейшем корректно отлавливать ошибки и останавливать устройство.
     */
    public static void closeDevice(DeviceHandle deviceHandle) {
        if (deviceHandle == null) return;
        deviceHandle.device().close();
        deviceHandle.future().cancel(true);
    }

    public static DeviceHandle createAndStart(DeviceConfig config) {
        Device device = new Device(config);
        Future<?> future = EXECUTOR.submit(device);
        return new DeviceHandle(device, future);
    }


    /* ───────────── API для статусу ───────────── */
    private volatile DeviceStatus currentStatus = DeviceStatus.STOPPED;
    private final List<DeviceStatusListener> statusListeners = new CopyOnWriteArrayList<>();

    public void addStatusListener(DeviceStatusListener listener) {
        statusListeners.add(listener);
    }
    public void addProcessListener(ObservationResultProcessor.ProcessListener listener) {
        this.resultProcessor.addProcessListener(listener);
    }

    public void removeProcessListener(ObservationResultProcessor.ProcessListener listener) {
        this.resultProcessor.removeProcessListener(listener);
    }

    public void removeStatusListener(DeviceStatusListener listener) {
        statusListeners.remove(listener);
    }

    public DeviceStatus getStatus() {
        return currentStatus;
    }

    /**
     * Змінює статус і повідомляє всіх слухачів
     */
    private void changeStatus(DeviceStatus newStatus) {
        DeviceStatus oldStatus = this.currentStatus;
        if (oldStatus != newStatus) {
            this.currentStatus = newStatus;
            logger.log("Device status changed: " + oldStatus + " -> " + newStatus);
            // Повідомляємо всіх слухачів асинхронно
            for (DeviceStatusListener listener : statusListeners) {
                Task.startDetached(() -> {
                    try {
                        listener.onStatusChanged(oldStatus, newStatus);
                    } catch (Exception e) {
                        logger.error("Error in status listener", e);
                    }
                });
            }
        }
    }

    private void handleCommunicatorStatus(DeviceStatus oldStatus, DeviceStatus newStatus) {
        // Комунікатор передає null як oldStatus, тому ігноруємо його
        logger.log("Communicator status update: " + newStatus);
        // Проксуємо статуси підключення від комунікатора
        switch (newStatus) {
            case CONNECTING, CONNECTED, RECONNECTING, CONNECTION_LOST -> changeStatus(newStatus);
            default -> logger.log("Ignoring communicator status: " + newStatus);
        }
    }

    /**
     * /* ───────────── экземплярные поля ─────────────
     */
    @Getter
    private final ICommunicator communicator;
    @Getter
    private final IProtocol protocol;
    @Getter
    private final IParser parser;
    @Getter
    private final Optional<IClarificationProvider> clarificationProvider;
    private final ObservationResultProcessor resultProcessor;
    private final DeviceLogger logger;
    private final CountDownLatch startedLatch = new CountDownLatch(1);


    /* ───────────── конструктор (приватный!) ───────────── */


    public Device(DeviceConfig config) {
        this.parser = config.getParser();
        this.communicator = config.getCommunicator();
        this.protocol = this.parser.getProtocol();
        this.logger = config.getLogger();
        this.parser.setLogger(logger);
        this.parser.setDeviceSettings(config.getDeviceSettings());
        this.resultProcessor = new ObservationResultProcessor(logger, config.getDeviceSettings());
        this.clarificationProvider = config.getClarificationProvider();
    }


    /**
     * Ожидает запуска устройства до указанного таймаута.
     *
     * @param timeout максимальное время ожидания
     * @param unit    единицы измерения времени
     * @throws InterruptedException если поток был прерван
     * @throws RuntimeException     если устройство не начало работу вовремя
     */
    public void awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
        if (!startedLatch.await(timeout, unit)) {
            throw new InterruptedException("Device did not start within the specified timeout");
        }
    }

    /* ───────────── внутренняя логика ───────────── */
    private void handleParsingResult(ParsingResult result) {
        byte[] ack = result.ack();
        if (ack.length > 0) {
            communicator.sendBytes(ack);
        }
        if (result.data() != null && !result.data().getData().isEmpty()) {
            Task.startDetached(() -> {
                var data = result.data();
                data.put(ObservationKey.ANALYZER, parser.getServiceName());
                if (clarificationProvider.isPresent()) {
                    try {
                        logger.log("Requesting clarification from user...");
                        CompletableFuture<ObservationData> clarificationFuture =
                                clarificationProvider.get().requestClarification(result.data());
                        data = clarificationFuture.get();
                        logger.log("Clarification from user received...");

                    } catch (Exception e) {
                        logger.error("Error during clarification process", e);
                    }
                }
                logger.log("Trying to process message …");
                if (data.getData().isEmpty()) {
                    logger.log("Message is empty, skipping processing");
                    return;
                }
                var procResult = resultProcessor.process(data);
                logger.log("Message process result: " + procResult.getResult());
                logger.log("Message processed finished \n---------------------------------------------");
            });
        }

    }

    @Override
    public void run() throws RuntimeException {
        try (var ignored = MDC.putCloseable("device", parser.getName())) {
            changeStatus(DeviceStatus.TRY_START);
            communicator.setDeviceStatusListener(this::handleCommunicatorStatus);
            communicator.setByteListener(protocol::onByte);
            protocol.setFrameListener(data -> {
                try {
                    logger.log("Message received. Trying to parse it ...");
                    logger.message(data);
                    changeStatus(DeviceStatus.TRY_PARSE);
                    parser.parse(data);
                    logger.log("Message parsed successfully");
                    changeStatus(DeviceStatus.WORKING);
                } catch (Exception e) {
                    logger.error("Error while parsing data", e);
                    changeStatus(DeviceStatus.PARSING_ERROR);
                }
            });
            protocol.setTransport(communicator::sendBytes);
            parser.addResponseListener(this::handleParsingResult);
            // Завершена конфигурация – устройство считается запущенным.
            logger.log("Device " + parser.getName() + " started successfully");
            changeStatus(DeviceStatus.WORKING);
            startedLatch.countDown();
            communicator.run();   // блокирует до завершения
        } catch (Exception e) {
            logger.error("Error while starting device...", e);
            changeStatus(DeviceStatus.ERROR);
            throw new DeviceRuntimeException("Device " + parser.getName() +
                    " start error: " + e.getMessage());
        }

    }

    /* ───────────── AutoCloseable ───────────── */
    @Override
    public void close() throws RuntimeException {
        try {
            // ✅ ОЧИЩУЄМО ВСІ СЛУХАЧІ ПЕРЕД ЗАКРИТТЯМ

            // 1. Очищуємо слухачів parser
            if (parser != null) {
                parser.clearResponseListeners();
            }

            // 2. Очищуємо слухачів defaultProtocol
            if (protocol != null) {
                protocol.clearFrameListener();
                protocol.clearTransport();
                if (protocol instanceof AutoCloseable closeable) {
                    closeable.close();
                }
            }

            // 3. Очищуємо слухачів communicator
            if (communicator != null) {
                communicator.clearDeviceStatusListener();
                communicator.clearByteListener();
            }

            // 4. Очищуємо слухачів resultProcessor
            if (resultProcessor != null) {
                resultProcessor.clearProcessListeners();
            }

            // 5. Закриваємо communicator
            if (communicator != null)
                communicator.close();

            if (logger != null)
                logger.close();

            changeStatus(DeviceStatus.STOPPED);
        } catch (Exception e) {
            logger.error("Error while closing device", e);
            changeStatus(DeviceStatus.ERROR);
            throw new DeviceRuntimeException("Device " + Objects.requireNonNull(parser).getName() +
                    " close error: " + e.getMessage());
        } finally {
            // 6. Очищуємо власних слухачів статусу
            statusListeners.clear();
        }
    }

}