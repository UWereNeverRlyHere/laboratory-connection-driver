package ywh.services.device.protocol;

import ywh.services.data.enums.SpecialBytes;
import ywh.logging.DeviceLogger;
import ywh.services.device.IPauseTransport;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.concurrent.*;

public abstract class BufferedProtocolAbstract implements IProtocol, IPauseTransport, AutoCloseable {


    protected final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    private IFrameListener listener;
    protected ITransport transport;
    protected DeviceLogger logger;
    protected Duration sendPause = Duration.ofMillis(0);


    /* ─── idle-timeout ─── */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("proto-idle").factory());
    private final ScheduledExecutorService sendScheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("proto-send").factory());

    private long idleTimeoutMs;
    private volatile long lastByteTs = System.currentTimeMillis();

    protected BufferedProtocolAbstract(DeviceLogger logger, long idleTimeoutMs) {
        this.logger = logger;
        this.idleTimeoutMs = idleTimeoutMs;
        startIdleWatcher();
    }

    public BufferedProtocolAbstract(long idleTimeoutMs) {
        this.idleTimeoutMs = idleTimeoutMs;
        startIdleWatcher();
    }

    /* ─── робота з буфером ─── */
    protected void append(byte b) {
        buf.write(b);
        lastByteTs = System.currentTimeMillis();
    }


    protected void fireFrame() {
        var data = buf.toByteArray();
        if (listener != null) {
            listener.onFrame(data);
        }
        reset();
    }
    @Override
    public CompletableFuture<Void> send(byte[] data) {
        if (transport == null) {
            logger.error("Transport is not set, cannot send data");
            return CompletableFuture.failedFuture(new IllegalStateException("Transport is not set"));
        }

        if (sendPause.isZero() || sendPause.isNegative()) {
            try {
                transport.send(data);
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        } else {
            CompletableFuture<Void> future = new CompletableFuture<>();
            sendScheduler.schedule(() -> {
                try {
                    transport.send(data);
                    future.complete(null);
                } catch (Exception e) {
                    logger.error("Error during delayed send", e);
                    future.completeExceptionally(e);
                }
            }, sendPause.toMillis(), TimeUnit.MILLISECONDS);

            return future;
        }
    }
    @Override
    public CompletableFuture<Void> send(byte data) {
        return send(new byte[]{data});
    }
    @Override
    public CompletableFuture<Void> send(SpecialBytes data) {
        return send(new byte[]{data.getValue()});
    }


    /* ─── IProtocol ─── */
    @Override
    public void setIdleTimeoutMs(long idleTimeoutMs){
        this.idleTimeoutMs = idleTimeoutMs;
    }

    @Override
    public void setFrameListener(IFrameListener l) {
        this.listener = l;
    }

    @Override
    public void setTransport(ITransport transport) {
        this.transport = transport;
    }

    @Override
    public void reset() {
        buf.reset();
    }

    @Override
    public void setLogger(DeviceLogger logger) {
        this.logger = logger;
    }

    @Override
    public void clearFrameListener() {
        this.listener = null;
    }

    @Override
    public void clearTransport() {
        this.transport = null;
    }

    @Override
    public void close() {
        clearFrameListener();
        clearTransport();
        scheduler.shutdownNow();
    }

    /* ─── idle-watcher ─── */
    private void startIdleWatcher() {
        long idleCheckPeriodMs = 500;
        scheduler.scheduleWithFixedDelay(() -> {
            long idle = System.currentTimeMillis() - lastByteTs;
            if (idle >= idleTimeoutMs && buf.size() > 0) {
                byte[] incomplete = buf.toByteArray();
                try {
                    onIdleTimeout(incomplete);
                } catch (Exception ex) {
                    logger.error("Error while handling idle timeout", ex);
                }
                reset();
            }
        }, idleTimeoutMs, idleCheckPeriodMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setSendPause(Duration pause) {
        this.sendPause = pause;
    }

    /**
     * Викликається, коли минув тай-аут простою і буфер не порожній.
     * За замовчуванням просто логування; можна перевизначити.
     */
    //TODO добавить попытку парсить незавершённые байты

    protected void onIdleTimeout(byte[] incompleteFrame) {
        logger.log("Incomplete frame detected, " + incompleteFrame.length + " bytes. Will try to parse it anyway...");
        if (incompleteFrame.length < 50) return;
        fireFrame();
    }
}
