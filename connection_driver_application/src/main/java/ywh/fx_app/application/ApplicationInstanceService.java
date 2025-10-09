package ywh.fx_app.application;

import javafx.application.Platform;
import javafx.stage.Stage;
import ywh.logging.MainLogger;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Сервіс для забезпечення запуску тільки одного екземпляра додатка
 */
public class ApplicationInstanceService {
    
    private static final String LOCK_FILE_SUFFIX = ".lock";
    private static final String ACTIVATE_FILE_SUFFIX = ".activate";
    private static final long MONITOR_INTERVAL_MS = 1500;
    private static final long NOTIFICATION_DELAY_SEC = 3;
    
    private final String appName;
    private final String lockFilePath;
    private final String activateFilePath;
    
    private FileChannel lockChannel;
    private FileLock fileLock;
    private Thread activationMonitorThread;
    private volatile boolean monitoringActive = false;
    
    private Stage primaryStage;
    private Runnable windowActivationCallback;

    public ApplicationInstanceService(String appName) {
        this.appName = appName;
        String tempDir = System.getProperty("java.io.tmpdir");
        String separator = System.getProperty("file.separator");
        
        this.lockFilePath = tempDir + separator + appName + LOCK_FILE_SUFFIX;
        this.activateFilePath = tempDir + separator + appName + ACTIVATE_FILE_SUFFIX;
    }

    /**
     * Намагається отримати блокування для single instance
     * @return true якщо блокування успішно отримано (перший запуск), false якщо додаток вже запущено
     */
    public boolean acquireLock() {
        try {
            Path lockPath = Paths.get(lockFilePath);
            lockChannel = FileChannel.open(lockPath, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.WRITE,
                StandardOpenOption.DELETE_ON_CLOSE);
            
            fileLock = lockChannel.tryLock();
            
            if (fileLock != null) {
                writeLockInfo();
                MainLogger.info("Application lock acquired: " + lockFilePath);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            MainLogger.error("Error acquiring application lock", e);
            return false;
        }
    }

    /**
     * Записує інформацію про процес у файл блокування
     */
    private void writeLockInfo() throws IOException {
        String lockInfo = String.format("PID: %d, Started: %s%n", 
            ProcessHandle.current().pid(),
            java.time.LocalDateTime.now());
            
        lockChannel.write(ByteBuffer.wrap(lockInfo.getBytes()));
        lockChannel.force(true);
    }

    /**
     * Повідомляє про спробу повторного запуску та намагається активувати існуюче вікно
     */
    public void notifyExistingInstance() {
        System.out.println("Додаток '" + appName + "' вже запущено! Активуємо існуюче вікно...");
        
        showNativeNotification();
        sendActivationSignal();
    }

    /**
     * Показує системне повідомлення про те, що додаток вже запущено
     */
    private void showNativeNotification() {
        if (!SystemTray.isSupported()) {
            System.err.println("Додаток '" + appName + "' вже запущено!");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            
            // Створюємо простий прозорий образ для іконки
            Image image = Toolkit.getDefaultToolkit().createImage(new byte[1]);
            TrayIcon trayIcon = new TrayIcon(image, appName);
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip(appName);
            
            tray.add(trayIcon);
            
            trayIcon.displayMessage(appName, 
                "Додаток вже запущено!", 
                TrayIcon.MessageType.INFO);
            
            // Прибираємо іконку через певний час
            CompletableFuture.delayedExecutor(NOTIFICATION_DELAY_SEC, TimeUnit.SECONDS)
                .execute(() -> tray.remove(trayIcon));
                
        } catch (Exception e) {
            MainLogger.error("Could not show system tray notification", e);
            System.err.println("Додаток '" + appName + "' вже запущено!");
        }
    }

    /**
     * Відправляє сигнал активації існуючому екземпляру
     */
    private void sendActivationSignal() {
        try {
            Path activatePath = Paths.get(activateFilePath);
            
            try (FileChannel channel = FileChannel.open(activatePath, 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                
                String message = "ACTIVATE_WINDOW:" + System.currentTimeMillis();
                channel.write(ByteBuffer.wrap(message.getBytes()));
            }
            
            MainLogger.info("Activation signal sent to existing instance");
            
        } catch (Exception e) {
            MainLogger.error("Could not send activation signal", e);
        }
    }

    /**
     * Запускає моніторинг сигналів активації
     * @param stage головне вікно додатка
     */
    public void startActivationMonitoring(Stage stage) {
        this.primaryStage = stage;
        startActivationMonitor();
    }

    /**
     * Запускає моніторинг сигналів активації з кастомним callback
     * @param activationCallback функція, яка викликається при отриманні сигналу активації
     */
    public void startActivationMonitoring(Runnable activationCallback) {
        this.windowActivationCallback = activationCallback;
        startActivationMonitor();
    }

    /**
     * Запускає фоновий моніторинг файлу активації
     */
    private void startActivationMonitor() {
        if (monitoringActive) {
            return; // Моніторинг вже активний
        }

        monitoringActive = true;
        
        activationMonitorThread = new Thread(() -> {
            Path activatePath = Paths.get(activateFilePath);
            
            while (monitoringActive && !Thread.currentThread().isInterrupted()) {
                try {
                    if (activatePath.toFile().exists()) {
                        // Активуємо вікно
                        if (windowActivationCallback != null) {
                            Platform.runLater(windowActivationCallback);
                        } else if (primaryStage != null) {
                            Platform.runLater(this::activateWindow);
                        }
                        
                        // Видаляємо файл після обробки
                        try {
                            activatePath.toFile().delete();
                        } catch (Exception e) {
                            MainLogger.error("Could not delete activation file", e);
                        }
                    }
                    
                    Thread.sleep(MONITOR_INTERVAL_MS);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    MainLogger.error("Error in activation monitor", e);
                }
            }
        });
        
        activationMonitorThread.setDaemon(true);
        activationMonitorThread.setName("ActivationMonitor-" + appName);
        activationMonitorThread.start();
        
        MainLogger.info("Activation monitoring started for " + appName);
    }

    /**
     * Активує головне вікно додатка
     */
    private void activateWindow() {
        if (primaryStage == null) {
            return;
        }

        // Показуємо вікно якщо воно приховане
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
        
        // Виводимо на передній план
        primaryStage.setAlwaysOnTop(true);
        primaryStage.requestFocus();
        primaryStage.toFront();
        
        // Прибираємо always on top після короткої затримки
        Platform.runLater(() -> {
            try {
                Thread.sleep(100);
                primaryStage.setAlwaysOnTop(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        MainLogger.info("Window activated by external request");
    }

    /**
     * Зупиняє моніторинг активації
     */
    public void stopActivationMonitoring() {
        monitoringActive = false;
        
        if (activationMonitorThread != null && activationMonitorThread.isAlive()) {
            activationMonitorThread.interrupt();
            try {
                activationMonitorThread.join(1000); // Чекаємо максимум 1 секунду
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        MainLogger.info("Activation monitoring stopped for " + appName);
    }

    /**
     * Звільняє блокування та очищує ресурси
     */
    public void releaseLock() {
        stopActivationMonitoring();
        
        try {
            if (fileLock != null && fileLock.isValid()) {
                fileLock.release();
                MainLogger.info("Application lock released");
            }
            if (lockChannel != null && lockChannel.isOpen()) {
                lockChannel.close();
            }
        } catch (Exception e) {
            MainLogger.error("Error releasing application lock", e);
        }
    }

    /**
     * Перевіряє чи активне блокування
     */
    public boolean isLockActive() {
        return fileLock != null && fileLock.isValid();
    }

    /**
     * Повертає шлях до файлу блокування
     */
    public String getLockFilePath() {
        return lockFilePath;
    }

    /**
     * Повертає назву додатка
     */
    public String getAppName() {
        return appName;
    }
}