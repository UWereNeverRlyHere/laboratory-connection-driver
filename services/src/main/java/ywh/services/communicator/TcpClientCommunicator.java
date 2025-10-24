package ywh.services.communicator;

import ywh.services.data.enums.DeviceStatus;
import ywh.logging.DeviceLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TCP-клиент, который:
 * • каждые 5 с пытается подключиться к указанному хосту/порту, пока не удастся;<br>
 * • после успешного подключения читает данные и отдаёт их {@link ByteListener};<br>
 * • при обрыве соединения снова переходит в режим повторного подключения.
 * <p>
 * Реализован блокирующим I/O — работает во виртуальных потоках, поэтому блокировки недороги.
 */
public final class TcpClientCommunicator extends CommunicatorAbstract
        implements ICommunicator, AutoCloseable {

    /*────────────── статическое ──────────────*/
    private static final int BUFFER_SIZE = 1024;
    private static final int RECONNECT_SEC = 5;

    /*────────────── поля ──────────────*/
    private final String host;
    private final int port;

    private final AtomicReference<SocketChannel> activeSocket = new AtomicReference<>();
    private final ReentrantLock socketLock = new ReentrantLock();
    private final AtomicBoolean running = new AtomicBoolean(true);


    /* thread-пулы (виртуальные) */
    private final ExecutorService connectorPool =
            Executors.newSingleThreadExecutor(Thread.ofVirtual().name("tcp-connector").factory());
    private final ExecutorService readerPool =
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("tcp-reader-").factory());

    /*────────────── ctor ──────────────*/
    public TcpClientCommunicator(String host, int port, DeviceLogger logger) {
        super(logger);
        this.host = host;
        this.port = port;
    }

    /*────────────── ICommunicator ──────────────*/


    @Override
    public void run() {
        connectorPool.submit(this::connectLoop);
    }

    @Override
    public void sendBytes(byte[] data) {
        if (data == null || data.length == 0) return;
        SocketChannel sc = activeSocket.get();
        if (sc == null || !sc.isOpen()) return;

        ByteBuffer buf = ByteBuffer.wrap(data);
        try {
            while (buf.hasRemaining()) sc.write(buf);
        } catch (IOException ex) {
            logger.error("Send bytes error", ex);
            closeSocket(sc, "Send failure – closing socket");
        }
    }

    @Override
    public void sendByte(byte data) {
        SocketChannel sc = activeSocket.get();
        if (sc == null || !sc.isOpen()) return;

        ByteBuffer buf = ByteBuffer.allocate(1).put(data);
        buf.flip();
        try {
            sc.write(buf);
        } catch (IOException ex) {
            logger.error("Send byte error", ex);
            closeSocket(sc, "Send failure – closing socket");
        }
    }

    @Override
    public void close() {
        running.set(false);
        closeSocket(activeSocket.getAndSet(null), "Client shutdown");
        connectorPool.shutdownNow();
        readerPool.shutdownNow();
    }

    /*────────────── internal ──────────────*/

    /**
     * Цикл попыток подключения.
     */
    /**
     * Цикл попыток подключения - з додатковою діагностикою
     */
    private void connectLoop() {
        while (running.get()) {
            if (activeSocket.get() != null) {
                sleepSec(1);
                continue;
            }

            logger.log("Trying to connect to " + host + ":" + port + " …");
            notifyDeviceStatus(DeviceStatus.CONNECTING);

            SocketChannel sc = null;
            try {
                // Додаткова діагностика
                logger.log("Creating SocketChannel...");
                sc = SocketChannel.open();
                logger.log("SocketChannel created successfully");

                sc.configureBlocking(true);
                logger.log("Configured as blocking");

                // Додаткові налаштування сокета
                sc.socket().setReuseAddress(true);
                sc.socket().setSoTimeout(10000); // 10 сек таймаут читання
                sc.socket().setTcpNoDelay(true);

                logger.log("Attempting to connect to " + host + ":" + port);
                boolean connected = sc.connect(new InetSocketAddress(host, port));
                logger.log("Connection result: " + connected);

                if (!connected) {
                    logger.log("Connection not immediately established, finishing...");
                    while (!sc.finishConnect()) {
                        Thread.sleep(100);
                    }
                }

                activeSocket.set(sc);
                logger.log("Connected to " + sc.getRemoteAddress());
                notifyDeviceStatus(DeviceStatus.CONNECTED);

                SocketChannel finalSc = sc;
                readerPool.submit(() -> readLoop(finalSc));

            } catch (IOException ex) {
                if (ex.getMessage().contains("Connection refused"))
                    logger.log("Connection refused. Will try again in " + RECONNECT_SEC + " sec...");
                else
                    logger.error("Connection attempt failed", ex);

                // Закриваємо канал при помилці
                if (sc != null && sc.isOpen()) {
                    try {
                        sc.close();
                    } catch (IOException closeEx) {
                        logger.error("Error closing failed channel", closeEx);
                    }
                }

                notifyDeviceStatus(DeviceStatus.RECONNECTING);
                sleepSec(RECONNECT_SEC);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


    /**
     * Чтение данных от сервера.
     */
    private void readLoop(SocketChannel sc) {
        ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);

        try {
            while (running.get() && sc.isOpen()) {
                int n = sc.read(buf);
                if (n == -1) break;              // сервер разорвал соединение

                buf.flip();
                while (buf.hasRemaining()) {
                    byte b = buf.get();
                    if (byteListener != null) byteListener.get().onByte(b);
                }
                buf.clear();
            }
        } catch (ClosedChannelException ignore) {
            // канал закрыли сами – нормально
        } catch (IOException ex) {
            logger.error("Read error", ex);
            notifyDeviceStatus(DeviceStatus.CONNECTION_LOST);
        } finally {
            closeSocket(sc, "Disconnected from server");
        }
    }

    /**
     * Безопасно закрывает socket и сбрасывает activeSocket.
     */
    private void closeSocket(SocketChannel sc, String reason) {
        if (sc == null) return;

        socketLock.lock();
        try {
            if (activeSocket.compareAndSet(sc, null)) {
                try {
                    sc.close();
                } catch (IOException ignored) {
                }
                logger.log(reason);
                if (running.get()) {
                    notifyDeviceStatus(DeviceStatus.RECONNECTING);
                }

            }
        } finally {
            socketLock.unlock();
        }
    }

    private static void sleepSec(int sec) {
        try {
            TimeUnit.SECONDS.sleep(sec);
        } catch (InterruptedException ignored) {
        }
    }
}