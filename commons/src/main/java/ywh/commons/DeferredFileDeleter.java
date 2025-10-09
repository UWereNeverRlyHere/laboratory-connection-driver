package ywh.commons;

import ywh.commons.data.ConsoleColor;
import ywh.logging.AppLogger;
import ywh.logging.AppLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public final class DeferredFileDeleter {

    private static final AppLogger LOGGER =
            AppLoggerFactory.createLogger(DeferredFileDeleter.class);
    private static final ConcurrentHashMap<File, FileTask> PENDING_FILES =
            new ConcurrentHashMap<>();
    private static final ReentrantLock LOCK = new ReentrantLock();

    private static ScheduledExecutorService scheduler;
    private static ScheduledFuture<?> mainTask;

    private static final Duration CHECK_INTERVAL = Duration.ofSeconds(10);
    private static final Duration DEFAULT_MAX_WAIT_TIME = Duration.ofMinutes(60);

    private DeferredFileDeleter() {
    }

    private static ScheduledExecutorService getScheduler() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(
                    1,
                    Thread.ofVirtual().name("file-deleter-scheduler").factory()
            );
        }
        return scheduler;
    }

    /**
     * Додає файл для відкладеного видалення з 10-секундним очікуванням
     */
    public static void scheduleForDeletion(File file) {
        scheduleForDeletion(file, DEFAULT_MAX_WAIT_TIME);
    }
    public static void scheduleForDeletion(Path path) {scheduleForDeletion(path.toFile());}

    /**
     * Додає файл для відкладеного видалення з кастомним часом очікування
     */
    public static void scheduleForDeletion(File file, Duration maxWaitTime) {
        LOCK.lock();
        try {
            LOGGER.info("Scheduling " + (file.isDirectory() ? "directory" : "file") + " for deletion: " + file.getName(), ConsoleColor.MAGENTA);
            getScheduler().schedule(() -> {
                LOCK.lock();
                try {
                    // Додаємо сам файл/папку до черги
                    PENDING_FILES.put(file, new FileTask(file, maxWaitTime));
                    LOGGER.info((file.isDirectory() ? "Directory" : "File") + " added to deletion queue: " + file.getName());
                    ensureMainTaskIsRunning();
                } finally {
                    LOCK.unlock();
                }
            }, 10, TimeUnit.SECONDS);
        } finally {
            LOCK.unlock();
        }

    }


    /**
     * Запускає головну задачу якщо вона не працює
     */
    private static void ensureMainTaskIsRunning() {
        LOCK.lock();
        try {
            if (mainTask == null || mainTask.isCancelled() || mainTask.isDone()) {
                mainTask = getScheduler().scheduleAtFixedRate(
                        DeferredFileDeleter::processAllFiles,
                        5,
                        CHECK_INTERVAL.toSeconds(),
                        TimeUnit.SECONDS
                );
                LOGGER.info("File deletion task started",
                        ConsoleColor.MAGENTA);
            }
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Обробляє всі файли в черзі
     */
    private static void processAllFiles() {
        LOCK.lock();
        try {
            if (PENDING_FILES.isEmpty()) {
                LOGGER.info("No files to process, stopping deletion task", ConsoleColor.CYAN);
                close();
                return;
            }
        } finally {
            LOCK.unlock();
        }

        LOGGER.info("Processing " + PENDING_FILES.size()
                        + " files for deletion",
                ConsoleColor.CYAN);
        PENDING_FILES.forEach(DeferredFileDeleter::processFile);
    }

    /**
     * Обробляє окремий файл
     */
    private static void processFile(File file, FileTask task) {
        try {
            if (System.currentTimeMillis() - task.startTime
                    > task.maxWaitTime.toMillis()) {
                LOGGER.warn("Max wait time exceeded for file: "
                                + file.getName(),
                        ConsoleColor.YELLOW);
                PENDING_FILES.remove(file);
                return;
            }
            DeletionResult result = file.isDirectory() ? tryDeleteDirectory(file) : tryDeleteFile(file);
            if (result.success()) {
                LOGGER.info("File deleted successfully: " + file.getName(),
                        ConsoleColor.CYAN);
                PENDING_FILES.remove(file);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing file: " + file.getName(), e);
            PENDING_FILES.remove(file);
        }
    }

    private static DeletionResult tryDeleteDirectory(File dir) {
        File[] contents = dir.listFiles();
        if (contents != null) {
            for (File content : contents) {
                if (content.isDirectory()) {
                    tryDeleteDirectory(content);
                }
                tryDeleteFile(content);
            }
        }
        return tryDeleteFile(dir);
    }

    /**
     * Пробує видалити файл або папку
     */
    private static DeletionResult tryDeleteFile(File file) {
        if (!file.exists()) {
            return new DeletionResult(true, "File does not exist");
        }

        if (isFileLocked(file)) {
            return new DeletionResult(false, "File is locked");
        }

        boolean deleted = file.delete();
        return new DeletionResult(
                deleted,
                deleted ? "Deleted successfully" : "Delete operation failed"
        );
    }


    /**
     * Перевіряє чи файл заблокований
     */
    private static boolean isFileLocked(File file) {
        if (!file.exists()) {
            return false;
        }
        File tempFile = new File(
                file.getParent(),
                file.getName() + ".tmp.delete"
        );
        try {
            if (file.renameTo(tempFile)) {
                tempFile.renameTo(file);
                return false;
            }
        } catch (Exception e) {
            return true;
        }
        try (FileChannel channel = FileChannel.open(
                file.toPath(),
                StandardOpenOption.WRITE
        )) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * Повертає кількість файлів в черзі
     */
    public static int getPendingFilesCount() {
        return PENDING_FILES.size();
    }

    /**
     * Закриває сервіс та очікує завершення всіх завдань
     */
    public static void close() {
        LOCK.lock();
        try {
            if (mainTask != null && !mainTask.isCancelled()) {
                mainTask.cancel(false);
                LOGGER.info("Main deletion task stopped");
            }
            if (scheduler != null) {
                scheduler.shutdown();
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                scheduler = null;
                LOGGER.info("DeferredFileDeleter closed",
                        ConsoleColor.MAGENTA);
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            LOCK.unlock();
        }
    }

    private record FileTask(File file,
                            Duration maxWaitTime,
                            long startTime) {
        FileTask(File file, Duration maxWaitTime) {
            this(file, maxWaitTime, System.currentTimeMillis());
        }
    }

    private record DeletionResult(boolean success, String message) {
    }
}