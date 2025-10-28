package ywh.services.communicator;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import ywh.logging.DeviceLogger;
import ywh.services.data.enums.DeviceStatus;
import ywh.services.data.serial_port.SerialParams;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SerialCommunicator extends CommunicatorAbstract implements AutoCloseable {
    private static final Duration RETRY_DELAY = Duration.ofSeconds(5);
    private static final Duration AVAILABILITY_POLL = Duration.ofMillis(500);

    private SerialPort serialPort;
    private final SerialParams params;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("serial-comm").factory()
    );
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private Future<?> availabilityFuture;

    protected SerialCommunicator(SerialParams params, DeviceLogger logger) {
        super(logger);
        logger.log("Initializing SerialPortCommunicator for port " + params.getPortName());
        logger.log("Serial port params: [" + params.getBaudRate() + ", " + params.getDataBits() +
                ", " + params.getStopBits() + ", " + params.getParity() + "]");
        this.params = params;

        // Знаходимо порт за назвою
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            if (port.getSystemPortName().equals(params.getPortName())) {
                serialPort = port;
                break;
            }
        }

        // Якщо порт не знайдено, створюємо його за назвою (для випадку, коли порт з'явиться пізніше)
        if (serialPort == null) {
            serialPort = SerialPort.getCommPort(params.getPortName());
        }

        notifyDeviceStatus(DeviceStatus.CONNECTING);
    }

    private class EventListener implements SerialPortDataListener {
        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                try {
                    int bytesAvailable = serialPort.bytesAvailable();
                    if (bytesAvailable > 0) {
                        byte[] buffer = new byte[bytesAvailable];
                        int numRead = serialPort.readBytes(buffer, buffer.length);

                        if (numRead > 0) {
                            for (int i = 0; i < numRead; i++) {
                                byteListener.get().onByte(buffer[i]);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error while reading data", e);
                }
            }
        }
    }

    @Override
    public void sendBytes(byte[] data) {
        try {
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.writeBytes(data, data.length);
            }
        } catch (Exception e) {
            logger.error("Error while sending data", e);
        }
    }

    @Override
    public void sendByte(byte data) {
        try {
            if (serialPort != null && serialPort.isOpen()) {
                byte[] buffer = new byte[]{data};
                serialPort.writeBytes(buffer, 1);
            }
        } catch (Exception e) {
            logger.error("Error while sending data", e);
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return; // already closed
        safeClosePort();
        if (availabilityFuture != null) availabilityFuture.cancel(true);
        scheduler.shutdownNow();
        notifyDeviceStatus(DeviceStatus.STOPPED);
    }

    private void safeClosePort() {
        if (serialPort == null) return;

        try {
            serialPort.removeDataListener();
        } catch (Exception ex) {
            logger.error("Cannot remove listener", ex);
        }

        try {
            if (serialPort.isOpen()) {
                serialPort.closePort();  // перша спроба
                logger.log("Port " + params.getPortName() + " closed");
            }
        } catch (Exception ex) {
            logger.error("Error while closing port (1-st try)", ex);
            try {
                serialPort.closePort();  // друга (примусова) спроба
            } catch (Exception ex2) {
                logger.error("Error while closing port (2-nd try)", ex2);
            }
        }
    }

    @Override
    public void run() {
        retryOpenPort();
    }

    private void retryOpenPort() {
        if (closed.get()) return;
        scheduler.schedule(() -> {
            try {
                attemptOpen();
            } catch (Exception ex) {
                logger.error("Open attempt failed, retry in " + RETRY_DELAY, ex);
                retryOpenPort();  // schedule next attempt
            }
        }, 1, TimeUnit.SECONDS);
    }

    private void attemptOpen() throws Exception {
        notifyDeviceStatus(DeviceStatus.CONNECTING);
        logger.log("Trying to open port " + params.getPortName());

        // Якщо порт ще не знайдено, спробуємо знайти його знову
        if (serialPort == null) {
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort port : ports) {
                if (port.getSystemPortName().equals(params.getPortName())) {
                    serialPort = port;
                    break;
                }
            }

            if (serialPort == null) {
                serialPort = SerialPort.getCommPort(params.getPortName());
            }
        }

        // Налаштування параметрів порту
        serialPort.setComPortParameters(
                params.getBaudRate().toInt(),
                params.getDataBits().toInt(),
                params.getStopBits().toInt(),
                params.getParity().toInt()
        );

        // Встановлюємо таймаути (0 = non-blocking)
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);

        // Відкриваємо порт
        if (!serialPort.openPort()) {
            throw new Exception("Failed to open port " + params.getPortName());
        }

        // Додаємо слухача подій
        serialPort.addDataListener(new EventListener());

        notifyDeviceStatus(DeviceStatus.CONNECTED);
        logger.log("Port " + params.getPortName() + " opened successfully");
        startComPortAvailabilityCheck();
    }

    private void startComPortAvailabilityCheck() {
        availabilityFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (closed.get()) return;

                // Перевірка доступності порту
                if (serialPort == null || !serialPort.isOpen()) {
                    logger.log("Port lost, restarting...");
                    handlePortLost();
                }
            } catch (Exception ex) {
                logger.error("Availability check error", ex);
                handlePortLost();
            }
        }, AVAILABILITY_POLL.toMillis(), AVAILABILITY_POLL.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void handlePortLost() {
        safeClosePort();
        notifyDeviceStatus(DeviceStatus.CONNECTION_LOST);
        retryOpenPort();
    }
}