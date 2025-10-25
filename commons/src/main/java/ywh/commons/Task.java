package ywh.commons;


import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Утилита для запуска задач в виртуальных/физических потоках
 * и для безопасного вызова Swing-кода в EDT.
 */
public final class Task {

    private Task() { }

    private static final ExecutorService SEQUENTIAL
            = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    private static final ExecutorService POOL
            = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
    private static final List<CompletableFuture<?>> RUNNING
            = Collections.synchronizedList(new ArrayList<>());

    /* ────────────────── KEY-LOCKED TASKS ────────────────── */

    /** семафори «одна задача на ключ» */
    private static final ConcurrentHashMap<String, Semaphore> KEYED = new ConcurrentHashMap<>();

    /**
     * Запускає задачу у {@link #POOL}, гарантуючи,
     * що одночасно виконується максимум одна задача з тим самим ключем.
     *
     * @param key   логічний ключ блокування (null/"" → "default")
     * @param task  постачальник результату
     */
    public static <T> CompletableFuture<T> startDetached(String key, Supplier<T> task) {
        final String k = (key == null || key.isEmpty()) ? "default" : key;

        return CompletableFuture.supplyAsync(() -> {
            final Semaphore sem = KEYED.computeIfAbsent(k, finalKey -> new Semaphore(1, true));
            try {
                sem.acquire();
                return task.get();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Task interrupted (key=" + k + ")", ie);
            } finally {
                sem.release();
                // clean-up, коли більше ніхто не чекає
                if (!sem.hasQueuedThreads() && sem.availablePermits() == 1) {
                    KEYED.remove(k, sem);
                }
            }
        }, POOL);
    }

    /**
     * ВЕРСІЯ без результату.
     */
    public static CompletableFuture<Void> startDetached(String key, Runnable task) {
        return startDetached(key, () -> { task.run(); return null; });
    }


    public static Thread startDetached(Runnable task) {
        return Thread.startVirtualThread(task);
    }

    public static void startSequential(Runnable task) {
        SEQUENTIAL.submit(task);
    }

    public static <T extends Runnable & AutoCloseable>
    Thread startWithShutdown(T task) {
        Thread thread = Thread.ofVirtual().start(task);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { task.close(); } catch (Exception ignored) { }
        }));
        return thread;
    }

    public static <T extends Runnable & AutoCloseable>
    AutoCloseable startService(T task) {
        Thread t = Thread.ofVirtual().start(task);
        return () -> {
            try { task.close(); } catch (Exception ignored) { }
            t.interrupt();
        };
    }

    public static Thread startWithShutdown(Runnable mainTask, Runnable onShutdown) {
        Thread thread = Thread.ofVirtual().start(mainTask);
        Runtime.getRuntime().addShutdownHook(new Thread(onShutdown));
        return thread;
    }

    public static void start(Runnable task) {
        var future = CompletableFuture.runAsync(task, POOL);
        RUNNING.add(future);
    }

    /**
     * Ждёт завершения всех задач, запущенных через {@link #start(Runnable)}.
     */
    public static void awaitAll() {
        List<CompletableFuture<?>> copy;
        synchronized (RUNNING) {
            copy = new ArrayList<>(RUNNING);
            RUNNING.clear();
        }
        copy.forEach(CompletableFuture::join);
    }

    /**
     * Отменяет все задачи, запущенные через {@link #start(Runnable)}.
     */
    public static void cancelAll() {
        List<CompletableFuture<?>> copy;
        synchronized (RUNNING) {
            copy = new ArrayList<>(RUNNING);
            RUNNING.clear();
        }
        copy.forEach(f -> f.cancel(true));
    }

    /**
     * Вызывает поставленный код в Swing-EDT.
     *
     * @param action     ваш код, может кидать checked-исключения
     * @param waitForEnd если true — блокирует вызывающий поток до завершения action,
     *                   иначе отдаёт задачу на invokeLater и сразу возвращает CompletableFuture
     * @return CompletableFuture, которое сигнализирует об окончании action в EDT
     */
    public static CompletableFuture<Void> invokeOnEDT(
            ThrowingRunnable action,
            boolean waitForEnd
    ) {
        // Если мы уже в EDT — выполняем сразу
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                action.run();
                return CompletableFuture.completedFuture(null);
            } catch (Exception ex) {
                CompletableFuture<Void> err = new CompletableFuture<>();
                err.completeExceptionally(ex);
                return err;
            }
        }

        // Если хотим дождаться — используем invokeAndWait + FutureTask
        if (waitForEnd) {
            FutureTask<Void> task = new FutureTask<>(() -> {
                action.run();
                return null;
            });
            try {
                SwingUtilities.invokeAndWait(task);
                task.get();
                return CompletableFuture.completedFuture(null);
            } catch (Exception ex) {
                CompletableFuture<Void> err = new CompletableFuture<>();
                err.completeExceptionally(ex.getCause() != null ? ex.getCause() : ex);
                return err;
            }
        }

        // Не ждём — invokeLater + CompletableFuture
        CompletableFuture<Void> cf = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            try {
                ConsoleUtil.printCyan("invoke from swing");
                action.run();
                cf.complete(null);
            } catch (Exception ex) {
                cf.completeExceptionally(ex);
            }
        });
        return cf;
    }
}