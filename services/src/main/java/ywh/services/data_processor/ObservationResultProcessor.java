package ywh.services.data_processor;

import ywh.commons.DeferredFileDeleter;
import ywh.commons.Task;
import ywh.services.data.enums.FileResultActions;
import ywh.services.data.enums.ProcessResult;
import ywh.services.settings.data.DeviceSettings;
import ywh.services.settings.data.PrintSettings;
import ywh.services.data.models.ProcessorResult;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.files.DocxPdfConverter;
import ywh.services.files.WordTemplateService;
import ywh.services.printing.PrintServiceManager;
import ywh.services.web.FtpClient;
import ywh.logging.DeviceLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;

public class ObservationResultProcessor {
    @FunctionalInterface
    public interface ProcessListener {
        void onProcessed(ProcessorResult result);
    }

    private File wordFile = new File("");
    private File pdfFile = new File("");

    private final List<ProcessListener> listeners = new CopyOnWriteArrayList<>();
    protected final DeviceLogger logger;
    protected final DeviceSettings deviceSettings;

    public ObservationResultProcessor(DeviceLogger logger, DeviceSettings deviceSettings) {
        this.logger = logger;
        this.deviceSettings = deviceSettings;
    }


    public void addProcessListener(ProcessListener listener) {
        if (listener != null) listeners.add(listener);
    }

    protected void fireProcessed(ProcessorResult result) {
        listeners.forEach(l -> {
            try {
                l.onProcessed(result);
            } catch (Exception ex) {
                logger.error("Error while processing result", ex);
            }
        });
    }

    @SuppressWarnings("IsNeverUsed")
    public void removeProcessListener(ProcessListener listener) {
        listeners.remove(listener);
    }

    public void clearProcessListeners() {
        listeners.clear();
    }

    private Optional<ProcessorResult> processDbf(List<FileResultActions> actions, ObservationData data) {
        if (actions.contains(FileResultActions.CREATE_DBF_FILE)) {
            boolean hasOtherActions = actions.size() > 1;
            DbfResultProcessor dbfProcessor = new DbfResultProcessor(deviceSettings.getFileResultProcessorSettings());
            var result = dbfProcessor.process(data, logger);
            fireProcessed(result);
            if (deviceSettings.getFileResultProcessorSettings().isUseFtp() && result.getFile().isPresent()) {
                logger.log("Uploading files to FTP server...");
                FtpClient ftpClient = new FtpClient(deviceSettings.getFileResultProcessorSettings().getFtpSettings());
                Path path = result.getFile().get().getParentFile().toPath();
                ftpClient.uploadAllFilesFromDir(path);
                logger.log("Uploading files to FTP finished");
                DeferredFileDeleter.scheduleForDeletion(path);
            }
            if (!hasOtherActions) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }


    private Optional<ProcessorResult> processApi(List<FileResultActions> actions, ObservationData data) {
        if (actions.contains(FileResultActions.API)) {
            boolean hasOtherActions = actions.size() > 1;
            if (hasOtherActions) {
                Task.start(() -> {
                    APIProcessor apiProcessor = new APIProcessor(deviceSettings);
                    var result = apiProcessor.process(data);
                    fireProcessed(result);
                });
            } else {
                APIProcessor apiProcessor = new APIProcessor(deviceSettings);
                var result = apiProcessor.process(data);
                fireProcessed(result);
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    /**
     * Універсальний метод для обробки даних на основі списку дій
     *
     * @param data Дані для обробки
     * @return Результат обробки
     */
    public ProcessorResult process(ObservationData data) {
        if (data.getData().isEmpty()) {
            return new ProcessorResult(ProcessResult.SUCCESS, Optional.empty());
        }
        List<FileResultActions> actions = deviceSettings.getActions();
        try {
            // 1. Запускаємо DBF обробку в окремому потоці, якщо потрібно
            var apiResult = processApi(actions, data);
            if (apiResult.isPresent()) {
                return apiResult.get();
            }

            var dbfResult = processDbf(actions, data);
            if (dbfResult.isPresent()) {
                return dbfResult.get();
            }

            // 2. Генеруємо Word документ
            var result = generateWordFile(data);
            if (result.getResult() != ProcessResult.SUCCESS) {
                return result;
            }

            var wordFile = result.getFile();
            var printFile = wordFile;
            // 3. Створюємо PDF, якщо потрібно
            boolean needsPdf = actions.contains(FileResultActions.SAVE_PDF)
                    || actions.contains(FileResultActions.SEND)
                    || deviceSettings.getPrintSettings().isPrintFromPDF();
            // 4. Друк, якщо потрібно

            if (needsPdf && wordFile.isPresent()) {
                var pdfResult = generatePdfFile(wordFile.get());
                if (pdfResult.getResult() != ProcessResult.SUCCESS) {
                    return pdfResult;
                }
                if (deviceSettings.getPrintSettings().isPrintFromPDF() && pdfResult.getFile().isPresent()) {
                    printFile = pdfResult.getFile();
                }
                // 5. Відправка на email, якщо потрібно
                if (actions.contains(FileResultActions.SEND)) {
                    logger.log("Sending file via email...");
                    // TODO: Реалізувати EmailSenderService
                    // ProcessResult sendResult = sendEmail(pdfConverter.getPdfFile(), params);
                    logger.log("Email sending not implemented yet");
                }
            }
            if (actions.contains(FileResultActions.PRINT)) {
                Thread.sleep(100);
                printFile.ifPresent(f -> performPrint(f, deviceSettings.getPrintSettings()));
            }

        } catch (Exception e) {
            logger.error("Error during file processing", e);
            return new ProcessorResult(ProcessResult.FAILURE, Optional.empty());
        }
        // 6. Видалення файлів, якщо не потрібно зберігати
        var file = cleanupFiles();
        return new ProcessorResult(ProcessResult.SUCCESS, file);

    }


    public ProcessorResult generateWordFile(ObservationData data) {
        logger.log("Try to generate Word file...");
        var word = new WordTemplateService(deviceSettings.getFileResultProcessorSettings().getTemplateFile(), deviceSettings.getFileResultProcessorSettings().getOutputPath());
        try {
            String docPath = word.generate(data);
            logger.log("Word file generated successfully");
            wordFile = new File(docPath);
            ProcessorResult result = new ProcessorResult(ProcessResult.SUCCESS, determineResultFile(wordFile));
            fireProcessed(result);
            return result;
        } catch (Exception e) {
            logger.error("Word file generation failed", e);
            return fireFailure(new File(""));
        }
    }


    public ProcessorResult generatePdfFile(File docxFile) {
        logger.log("Try to createApi PDF file...");
        var pdf = new DocxPdfConverter(docxFile.getAbsolutePath());
        try {
            pdf.convert();
            logger.log("PDF file created successfully");
            pdfFile = pdf.getPdfFile();
            ProcessorResult result = new ProcessorResult(ProcessResult.SUCCESS, determineResultFile(pdfFile));
            fireProcessed(result);
            return result;
        } catch (IOException | InterruptedException e) {
            logger.error("Error during PDF conversion", e);
            return fireFailure(new File(""));
        }
    }

    CompletableFuture<Void> printFuture = CompletableFuture.completedFuture(null);

    @SuppressWarnings("UnusedReturnValue")
    public ProcessorResult performPrint(File file, PrintSettings printParams) {
        logger.log("Starting print job for file: " + file.getName());
        // Зберігаємо Future для очікування в cleanupFiles
        printFuture = PrintServiceManager.printFileAsync(file, printParams);

        // Додаємо обробку завершення для логування
        printFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error("Error while printing file", throwable);
                ProcessorResult failureResult = new ProcessorResult(ProcessResult.FAILURE, determineResultFile(file));
                fireProcessed(failureResult);
            } else {
                logger.log("Print job completed for file: " + file.getName());
                ProcessorResult successResult = new ProcessorResult(ProcessResult.SUCCESS, determineResultFile(file));
                fireProcessed(successResult);
            }
        });
        // Повертаємо успішний результат (друк розпочато)
        return new ProcessorResult(ProcessResult.SUCCESS, determineResultFile(file));
    }


    public Optional<File> cleanupFiles() {

        if (printFuture != null && !printFuture.isDone()) {
            logger.log("Waiting for print job to complete before cleanup...");
            try {
                printFuture.join();
                logger.log("Print job completed, proceeding with cleanup");
            } catch (CompletionException ex) {
                logger.error("Print job failed, but proceeding with cleanup", ex.getCause());
            }
        }

        List<FileResultActions> actions = deviceSettings.getActions();
        boolean saveDocx = actions.contains(FileResultActions.SAVE_DOCX);
        boolean savePdf = actions.contains(FileResultActions.SAVE_PDF);

        if (deviceSettings.getFileResultProcessorSettings().isUseFtp()) {
            logger.log("Uploading files to FTP server...");
            FtpClient ftpClient = new FtpClient(deviceSettings.getFileResultProcessorSettings().getFtpSettings());
            if (wordFile != null && wordFile.exists()) {
                ftpClient.uploadSingleFile(wordFile);
                DeferredFileDeleter.scheduleForDeletion(wordFile);
            }
            if (pdfFile != null && pdfFile.exists()) {
                ftpClient.uploadSingleFile(pdfFile);
                DeferredFileDeleter.scheduleForDeletion(pdfFile);
            }
            logger.log("Uploading files to FTP finished");

        } else {
            // Видаляємо файли які не потрібно зберігати через DeferredFileDeleter
            if (!saveDocx && wordFile != null && wordFile.exists()) {
                DeferredFileDeleter.scheduleForDeletion(wordFile);
            }
            if (!savePdf && pdfFile != null && pdfFile.exists()) {
                DeferredFileDeleter.scheduleForDeletion(pdfFile);
            }
        }

        var resultFile = determineResultFile();
        printFuture = CompletableFuture.completedFuture(null);
        return resultFile;

    }

    private Optional<File> determineResultFile() {
        if (pdfFile != null && pdfFile.exists()) {
            return Optional.of(pdfFile);
        } else if (wordFile != null && wordFile.exists()) {
            return Optional.of(wordFile);
        }
        return Optional.empty();
    }

    protected static Optional<File> determineResultFile(File file) {
        if (file != null && file.exists()) {
            return Optional.of(file);
        }
        return Optional.empty();
    }

    private ProcessorResult fireFailure(File file) {
        ProcessorResult failureResult = new ProcessorResult(ProcessResult.FAILURE, determineResultFile(file));
        fireProcessed(failureResult);
        return failureResult;
    }
}
