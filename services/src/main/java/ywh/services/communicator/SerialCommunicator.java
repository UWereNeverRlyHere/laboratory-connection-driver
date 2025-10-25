package ywh.services.communicator;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
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

    private final SerialPort serialPort;
    private final SerialParams params;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("serial-comm").factory());
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private Future<?> availabilityFuture;

    protected SerialCommunicator(SerialParams params, DeviceLogger logger) {
        super(logger);
        logger.log("Initializing SerialPortCommunicator for port " + params.getPortName());
        logger.log("Serial port params: [" + params.getBaudRate() + ", " + params.getDataBits() + ", " + params.getStopBits() + ", " + params.getParity() + "]");
        this.params = params;
        serialPort = new SerialPort(params.getPortName());
        notifyDeviceStatus(DeviceStatus.CONNECTING);
    }

    private class EventListener implements SerialPortEventListener {
        public void serialEvent(SerialPortEvent event) {
            if (event.isRXCHAR() && event.getEventValue() > 0) {
                byte[] entry = null;
                try {
                    entry = serialPort.readBytes(event.getEventValue());
                    for (byte b : entry) {
                        byteListener.get().onByte(b);
                    }
                } catch (SerialPortException e) {
                    logger.error("Error while reading data", e);
                }
            }
        }
    }

    @Override
    public void sendBytes(byte[] data) {
        try {
            serialPort.writeBytes(data);
        } catch (SerialPortException e) {
            logger.error("Error while sending data", e);
        }
    }

    @Override
    public void sendByte(byte data) {
        try {
            serialPort.writeByte(data);
        } catch (SerialPortException e) {
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
            serialPort.removeEventListener();
        } catch (SerialPortException ex) {
            logger.error("Cannot remove listener", ex);
        }

        try {
            serialPort.closePort();               // перша спроба
            logger.log("Port " + params.getPortName() + " closed");
        } catch (SerialPortException ex) {
            logger.error("Error while closing port (1-st try)", ex);
            try {
                serialPort.closePort();           // друга (примусова) спроба
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

    private void attemptOpen() throws SerialPortException {
        notifyDeviceStatus(DeviceStatus.CONNECTING);
        logger.log("Trying to open port " + params.getPortName());

        serialPort.openPort();
        serialPort.setParams(
                params.getBaudRate().toInt(),
                params.getDataBits().toInt(),
                params.getStopBits().toInt(),
                params.getParity().toInt());
        serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
        serialPort.addEventListener(new EventListener());

        notifyDeviceStatus(DeviceStatus.CONNECTED);
        logger.log("Port " + params.getPortName() + " opened successfully");
        startComPortAvailabilityCheck();
    }


    private void startComPortAvailabilityCheck() {
        availabilityFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (closed.get()) return;
                if (!serialPort.isOpened() || serialPort.getEventsMask() == -1) {
                    logger.log("Port lost, restarting...");
                    handlePortLost();
                }
            } catch (SerialPortException ex) {
                logger.error("Availability check error", ex);
            }
        }, AVAILABILITY_POLL.toMillis(), AVAILABILITY_POLL.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void handlePortLost() {
        safeClosePort();
        notifyDeviceStatus(DeviceStatus.CONNECTION_LOST);
        retryOpenPort();
    }
}
