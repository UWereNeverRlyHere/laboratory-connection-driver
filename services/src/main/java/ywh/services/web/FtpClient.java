package ywh.services.web;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import ywh.commons.data.ConsoleColor;
import ywh.services.exceptions.DeviceValidationException;
import ywh.services.settings.data.FtpSettings;
import ywh.logging.AppLogger;
import ywh.logging.AppLoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

public class FtpClient {
    private final FtpSettings settings;
    private static final AppLogger LOGGER = AppLoggerFactory.createLogger("ftp client", "ftp client", ConsoleColor.GRAY);

    public FtpClient(FtpSettings settings) {
        this.settings = settings;
    }

    //TODO use ftps bool parameter
    public static void checkConnection(FtpSettings settings) throws DeviceValidationException {
        var client = new FtpClient(settings).connect();

        if (client.isPresent()) {

            int reply = client.get().getReplyCode();
            String replyString = client.get().getReplyString();
            try {
                FTPClient ftpClient = client.get();
                settings.setFtps(ftpClient instanceof FTPSClient);
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException ex) {
                LOGGER.error("Error while disconnecting from FTP server (" + settings.getServer() + "): ", ex);
            }
            if (!FTPReply.isPositiveCompletion(reply)) {
                LOGGER.error("Can't connect to FTP server: " + replyString);
                throw new DeviceValidationException("Не вдалося підключитися до FTP серверу: " + replyString);
            }

        }
        if (client.isEmpty())
            throw new DeviceValidationException("Не вдалося підключитися до FTP серверу!");
    }

    private void login(FTPClient client) throws DeviceValidationException, IOException {
        String user = settings.getUser();
        String pass = settings.getPassword();
        int reply = client.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            throw new DeviceValidationException("Не вдалося підключитися до FTP серверу: " + client.getReplyString());
        }
        try {
            client.sendCommand("OPTS UTF8 ON");
            client.login(user, pass);
            client.enterLocalPassiveMode();
            client.setFileType(FTP.BINARY_FILE_TYPE);
            client.setFileTransferMode(FTP.BINARY_FILE_TYPE);
        } catch (Exception e) {
            try {
                client.logout();
                client.disconnect();
            } catch (IOException ex) {
                LOGGER.error("Error while disconnecting from FTP server (" + settings.getServer() + "): ", ex);
            }
            throw e;
        }
    }

    private Optional<FTPSClient> connectAsFtps() {
        String server = settings.getServer();
        FTPSClient ftpsClient = new FTPSClient();
        ftpsClient.setControlEncoding("UTF-8");
        ftpsClient.setCharset(StandardCharsets.UTF_8);
        ftpsClient.setConnectTimeout(30000);
        try {
            ftpsClient.connect(server);
            return Optional.of(ftpsClient);
        } catch (IOException e) {
            try {
                ftpsClient.disconnect();
            } catch (IOException ex) {
                LOGGER.error("Error while disconnecting from FTP server (" + settings.getServer() + "): ", ex);
            }
        }
        return Optional.empty();
    }

    private Optional<FTPClient> connectAsFtp() {
        String server = settings.getServer();
        FTPClient ftpClient = new FTPClient();
        ftpClient.setControlEncoding("UTF-8");
        ftpClient.setConnectTimeout(90000);
        ftpClient.setDataTimeout(Duration.ofSeconds(90));
        ftpClient.setCharset(StandardCharsets.UTF_8);
        try {
            ftpClient.connect(server);
            return Optional.of(ftpClient);
        } catch (IOException e) {
            try {
                ftpClient.disconnect();
            } catch (IOException ex) {
                LOGGER.error("Error while disconnecting from FTP server (" + settings.getServer() + "): ", ex);
            }
        }
        return Optional.empty();

    }

    private Optional<FTPClient> connect() {
      /*  var ftpsClient = connectAsFtps();
        if (ftpsClient.isPresent()) {
            return Optional.of(ftpsClient.get());
        }*/
        return connectAsFtp();
    }


    public boolean uploadAllFilesFromDir(Path path) {
        Optional<FTPClient> clientOpt = connect();
        if (clientOpt.isEmpty()) {
            LOGGER.error("Failed to connect to FTP server");
            return false;
        }

        FTPClient client = clientOpt.get();

        try {
            login(client);
        } catch (Exception e) {
            LOGGER.error("Error while logging in to FTP server (" + settings.getServer() + "): ", e);
            return false;
        }

        String folderName = path.getFileName().toString();

        try {
            // Переходимо в кореневу директорію
            client.changeWorkingDirectory("/");

            // Перевіряємо чи папка існує
            boolean directoryExists = client.changeWorkingDirectory(folderName);
            if (directoryExists) {
                LOGGER.info("Directory already exists, uploading to existing directory: " + folderName);
            } else {
                // Повертаємося в корінь та створюємо папку
                client.changeWorkingDirectory("/");
                boolean dirCreated = client.makeDirectory(folderName);
                if (!dirCreated) {
                    LOGGER.error("Failed to create directory: " + folderName);
                    return false;
                }
                LOGGER.info("Created new directory: " + folderName);

                // Переходимо в створену папку
                if (!client.changeWorkingDirectory(folderName)) {
                    LOGGER.error("Failed to change to created directory: " + folderName);
                    return false;
                }
            }

            // Отримуємо всі файли з локальної папки
            File dir = path.toFile();
            if (!dir.exists() || !dir.isDirectory()) {
                LOGGER.error("Local directory does not exist: " + path);
                return false;
            }

            File[] files = dir.listFiles();
            if (files == null) {
                LOGGER.error("Cannot read files from directory: " + path);
                return false;
            }

            boolean allUploaded = true;
            for (File file : files) {
                if (file.isFile()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        LOGGER.info("Uploading file: " + file.getName());

                        // storeFile автоматично перезаписує існуючі файли
                        boolean uploaded = client.storeFile(file.getName(), fis);
                        if (uploaded) {
                            LOGGER.info("File uploaded/overwritten successfully: " + file.getName());
                        } else {
                            LOGGER.error("Failed to upload file: " + file.getName() +
                                    " (Reply: " + client.getReplyString() + ")");
                            allUploaded = false;
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error uploading file " + file.getName() + ": ", e);
                        allUploaded = false;
                    }
                } else if (file.isDirectory()) {
                    LOGGER.info("Skipping subdirectory: " + file.getName());
                }
            }

            LOGGER.info("Upload completed. Success: " + allUploaded);
            return allUploaded;

        } catch (IOException e) {
            LOGGER.error("FTP operation error: ", e);
            return false;
        } finally {
            try {
                if (client.isConnected()) {
                    client.logout();
                    client.disconnect();
                }
            } catch (IOException ex) {
                LOGGER.error("Error while disconnecting from FTP server: ", ex);
            }
        }
    }

    public boolean uploadSingleFile(Path filePath) {
        Optional<FTPClient> clientOpt = connect();
        if (clientOpt.isEmpty()) {
            LOGGER.error("Failed to connect to FTP server");
            return false;
        }

        FTPClient client = clientOpt.get();

        try {
            login(client);
        } catch (Exception e) {
            LOGGER.error("Error while logging in to FTP server (" + settings.getServer() + "): ", e);
            return false;
        }

        try {
            // Переходимо в кореневу директорію
            client.changeWorkingDirectory("/");

            File file = filePath.toFile();
            if (!file.exists() || !file.isFile()) {
                LOGGER.error("File does not exist or is not a file: " + filePath);
                return false;
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                String fileName = file.getName();
                LOGGER.info("Uploading file to root directory: " + fileName);

                boolean uploaded = client.storeFile(fileName, fis);
                if (uploaded) {
                    LOGGER.info("File uploaded/overwritten successfully in root: " + fileName);
                    return true;
                } else {
                    LOGGER.error("Failed to upload file: " + fileName +
                            " (Reply: " + client.getReplyString() + ")");
                    return false;
                }
            } catch (IOException e) {
                LOGGER.error("Error uploading file " + file.getName() + ": ", e);
                return false;
            }

        } catch (IOException e) {
            LOGGER.error("FTP operation error: ", e);
            return false;
        } finally {
            try {
                if (client.isConnected()) {
                    client.logout();
                    client.disconnect();
                }
            } catch (IOException ex) {
                LOGGER.error("Error while disconnecting from FTP server: ", ex);
            }
        }
    }

    public boolean uploadSingleFile(File file) {
        return uploadSingleFile(file.toPath());
    }


}
