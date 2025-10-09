package ywh.services.communicator.impl;

import ywh.services.data.enums.DeviceStatus;
import ywh.services.exceptions.DeviceRuntimeException;
import ywh.logging.DeviceLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TCP-хост, що підтримує тільки ОДНЕ клієнтське з’єднання.
 * Після розриву – очікує наступного клієнта.
 */
public final class TcpHostCommunicatorImpl extends CommunicatorAbstract implements AutoCloseable {

    private static final int BUFFER_SIZE = 1024;

    // Теперь сервер создаётся не в конструкторе, а в run()
    private ServerSocketChannel server;
    private final int port;

    private final AtomicReference<SocketChannel> activeClient = new AtomicReference<>();
    private final ReentrantLock clientLock = new ReentrantLock();
    private final AtomicBoolean isAccepting = new AtomicBoolean(false);
    private volatile boolean running = true;

    // пули потоків (всі – віртуальні)
    private final ExecutorService acceptorPool = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("tcp-acceptor").factory());
    private final ExecutorService clientPool = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("tcp-client-").factory());

    // ────────────────────────── конструктор ──────────────────────────
    public TcpHostCommunicatorImpl(int port, DeviceLogger logger) {
        super(logger);
        this.port = port;
        logger.log("Initialized TcpHostCommunicatorImpl for port " + port + " (server not started yet)");
    }

    // ────────────────────────── ICommunicator ──────────────────────────
    @Override
    public void run() {
        // Переносим инициализацию серверного сокета в run(), чтобы обработать исключения тут
        try {
            logger.log("Trying to start device tcp host server on port " + port + " ...");
            server = ServerSocketChannel.open();
            notifyDeviceStatus(DeviceStatus.CONNECTING);

            server.bind(new InetSocketAddress("0.0.0.0", port));
            server.configureBlocking(true); // блокуючий режим (OK у віртуальному потоці)
        } catch (IOException ex) {
            logger.error("Error while initializing server socket on port " + port, ex);
            throw new DeviceRuntimeException("Errpr while initializing server socket on port " + port + ": " + ex.getMessage() + " (see log for details");
        }
        // После успешной инициализации запускаем прием клиентов
        acceptorPool.submit(this::acceptLoop);
    }



    @Override
    public void sendBytes(byte[] data) {
        if (data == null || data.length == 0) return;
        SocketChannel client = activeClient.get();
        if (client == null || !client.isOpen()) return;

        ByteBuffer buf = ByteBuffer.wrap(data);
        try {
            while (buf.hasRemaining()) client.write(buf);
        } catch (IOException ex) {
            logger.error("Send bytes error: ", ex);
            closeClient(client, "Помилка надсилання даних: " + ex.getMessage());
        }
    }

    @Override
    public void sendByte(byte data) {
        SocketChannel client = activeClient.get();
        if (client == null || !client.isOpen()) return;

        ByteBuffer buf = ByteBuffer.allocate(1).put(data);
        buf.flip();
        try {
            client.write(buf);
        } catch (IOException ex) {
            logger.error("Send byte error: ", ex);
            closeClient(client, "Помилка надсилання байта: " + ex.getMessage());
        }
    }

    @Override
    public void close() {
        running = false;

        SocketChannel client = activeClient.getAndSet(null);
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                logger.error("Error while closing client socket in close method", e);
            }
        }

        if (server != null) {
            try {
                server.close();
            } catch (IOException ex) {
                logger.error("Error while closing server socket: ", ex);
            }
        }

        acceptorPool.shutdownNow();
        clientPool.shutdownNow();
    }

    // ─────────────────── цикл приймання клієнтів ────────────────────
    private void acceptLoop() {
        while (running) {
            // ───── проверка, можно ли принимать ещё одного ─────
            if (activeClient.get() != null || !isAccepting.compareAndSet(false, true)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); // вернуть флаг
                    logger.error("Acceptor thread interrupted", ex);
                    break; // выходим из цикла
                }
                continue;
            }

            try {
                SocketChannel client = server.accept(); // блокирует поток
                client.configureBlocking(true);

                if (activeClient.compareAndSet(null, client)) {
                    clientPool.submit(() -> handleClient(client));
                    notifyDeviceStatus(DeviceStatus.CONNECTED);
                    logger.log("Client connected: " + client.getRemoteAddress());
                } else {
                    client.close(); // уже есть активный клиент
                }

            } catch (ClosedChannelException ex) {
                // Серверный сокет закрыт извне -> пора завершаться
                logger.log("Server socket closed, stopping accept loop");
                break;

            } catch (IOException ex) {
                logger.error("I/O error in acceptLoop", ex);
                // продолжаем цикл

            } finally {
                isAccepting.set(false);
            }
        }
    }

    // ─────────────────── обробка клієнта ────────────────────────────
    private void handleClient(SocketChannel client) {
        ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);

        try {
            while (running && client.isOpen()) {
                int n = client.read(buf);
                if (n == -1) break; // клиент закрылся

                buf.flip();
                while (buf.hasRemaining()) {
                    byte b = buf.get();
                    if (byteListener != null) byteListener.onByte(b);
                }
                buf.clear();
            }
        } catch (IOException ex) {
            logger.error("Error while handling client: ", ex);
        } finally {
            notifyDeviceStatus(DeviceStatus.CONNECTION_LOST);
            closeClient(client, "Клієнт від’єднався");
        }
    }

    // ─────────────────── закриття клієнта ───────────────────────────
    private void closeClient(SocketChannel client, String reason) {
        if (client == null) return;

        clientLock.lock();
        try {
            if (activeClient.compareAndSet(client, null)) {
                try {
                    client.close();
                } catch (IOException ex) {
                    logger.error("Error while closing client socket: ", ex);
                }
                System.out.println(reason);
            }
        } finally {
            clientLock.unlock();
        }
    }
}