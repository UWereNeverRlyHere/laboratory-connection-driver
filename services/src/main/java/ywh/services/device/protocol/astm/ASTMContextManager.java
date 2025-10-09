package ywh.services.device.protocol.astm;

import ywh.logging.DeviceLogger;
import ywh.services.device.IPauseTransport;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static ywh.services.data.enums.SpecialBytes.*;

public class ASTMContextManager {
    private enum State {
        SENDER,
        RECEIVER,
        NEUTRAL
    }

    private DeviceLogger logger;
    private final BlockingQueue<ASTMOrder> ordersQueue = new LinkedBlockingQueue<>(50);
    private final ScheduledExecutorService ordersScheduler;
    private final AtomicReference<ScheduledFuture<?>> timeoutTask = new AtomicReference<>(null);
    private final AtomicReference<ASTMOrder> currentOrder = new AtomicReference<>();
    private volatile State state = State.NEUTRAL;
    private final IPauseTransport transport;

    public ASTMContextManager(DeviceLogger logger, IPauseTransport transport) {
        this.logger = logger;
        this.transport = transport;
        this.ordersScheduler = Executors.newSingleThreadScheduledExecutor(
                Thread.ofVirtual().name("astm-orders").factory()
        );
       // startOrdersScheduler();
    }

    protected void setLogger(DeviceLogger logger) {
        this.logger = logger;
        if (ordersScheduler != null && !ordersScheduler.isShutdown()) {
            cancelTimeout();
            startOrdersScheduler();
        }
    }

    protected void clearCurrentOrder() {
        currentOrder.set(null);
    }

    protected boolean hasNoCurrentOrder() {
        return currentOrder.get() == null;
    }

    protected void setNeutral() {
        state = State.NEUTRAL;
    }

    protected boolean isNeutral() {
        return state == State.NEUTRAL;
    }

    protected boolean isNotNeutral() {
        return state != State.NEUTRAL;
    }

    protected void setSender() {
        state = State.SENDER;
    }

    protected boolean isSender() {
        return state == State.SENDER;
    }

    protected boolean isNotSender() {
        return state != State.SENDER;
    }

    protected void setReceiver() {
        state = State.RECEIVER;
    }

    protected boolean isReceiver() {
        return state == State.RECEIVER;
    }

    protected boolean isNotReceiver() {
        return state != State.RECEIVER;
    }

    protected void sendEOT() {
        transport.send(EOT).thenRun(() -> logger.log("HOST ----------> [EOT]"));
    }

    protected void sendACK() {
        transport.send(ACK).thenRun(() -> logger.log("HOST ----------> [ACK]"));
    }

    protected void sendACK(Runnable runnable) {
        runnable.run();
        transport.send(ACK).thenRun(() -> logger.log("HOST ----------> [ACK]"));
    }

    protected void sendENQ() {
        transport.send(ENQ).thenRun(() -> logger.log("HOST ----------> [ENQ]"));
    }


    protected void handleNak(Runnable runnable) {
        runnable.run();
        setNeutral();
        cancelTimeout();
        ASTMOrder order = currentOrder.get();
        if (order != null) {
            if (order.getCurrentIndex() >= 1) {
                logger.log("Order failed, on part: " + (order.getCurrentIndex() + 1));
                currentOrder.set(null);
                sendEOT();
            } else {
                order.incrementTryIndex();
                logger.log("NAK received, incremented try index to: " + order.getTryIndex());
            }
        } else {
            logger.log("Received NAK but no current order exists");
        }
    }

    protected void addOrderToQueue(ASTMOrder order) {
        if (!ordersQueue.offer(order)) {
            logger.log("Orders queue is full, order rejected");
        }
    }

    protected void sendNextFrame(Runnable runnable) {
        runnable.run();
        if (hasNoCurrentOrder()) return;

        String frame = currentOrder.get().getNextFrame().orElse("");
        setNeutral();

        if (frame.isEmpty()) {
            logger.log("Got empty frame, order failed..... State is NEUTRAL.");
            logger.writeSeparator();
            cancelTimeout();
            return;
        }

        logger.log("HOST ----------> " + frame);
        transport.send(frame.getBytes());

        if (currentOrder.get().hasNoNextFrame()) {
            logger.log("Order send successfully. State is NEUTRAL.");
            cancelTimeout();
            clearCurrentOrder();
        } else {
            setSender();
            startTimeout();
        }
    }
    private volatile boolean schedulerStarted = false;

    protected boolean isSchedulerStarted() {
        return schedulerStarted;
    }

    protected void startOrdersScheduler() {
        if (schedulerStarted) {
            return;
        }
        schedulerStarted = true;
        ordersScheduler.scheduleWithFixedDelay(() -> {
            if (isSender()) return;

            try {
                if (currentOrder.get() == null) {
                    currentOrder.set(ordersQueue.take());
                    logger.log("Found order in queue, sending ENQ...");
                    logger.log("Full order is: \r\n" + currentOrder.get().getFullOrder());
                    logger.log("Full HEX order is: \r\n" + currentOrder.get().getFullHexOrder());
                } else {
                    if (currentOrder.get().getTryIndex() >= 7) {
                        logger.log("Order failed after 6 attempts... State is NEUTRAL... Order deleted");
                        cancelTimeout();
                        currentOrder.set(null);
                        return;
                    }
                    logger.log("Retrying to send order for " + currentOrder.get().getTryIndex() + " time");
                }
                sendENQ();
                setSender();
                startTimeout();
            } catch (InterruptedException e) {
                logger.error("Error while processing orders queue", e);
                Thread.currentThread().interrupt();
            }
        }, 5000, 300, TimeUnit.MILLISECONDS);
    }

    private void startTimeout() {
        cancelTimeout();
        timeoutTask.set(ordersScheduler.schedule(() -> {
            logger.log("No answer from host for 10 sec, will try later..");
            if (currentOrder.get() != null) {
                currentOrder.get().reset();
            }
            sendEOT();
            setNeutral();
        }, 10, TimeUnit.SECONDS));
    }

    protected void cancelTimeout() {
        ScheduledFuture<?> task = timeoutTask.get();
        if (task != null) {
            task.cancel(false);
            timeoutTask.set(null);
        }
    }

    protected void close() {
        logger.log("Closing ASTM Protocol Context...");

        if (!ordersScheduler.isShutdown()) {
            ordersScheduler.shutdownNow();
            logger.log("Orders scheduler shut down");
        }

        cancelTimeout();
        currentOrder.set(null);
        ordersQueue.clear();

        logger.log("ASTM Protocol Context closed");
    }

}
