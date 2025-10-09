package ywh.services.printing;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import ywh.commons.ConsoleUtil;
import ywh.services.settings.data.PrintSettings;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.PrintQuality;
import java.awt.*;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static ywh.services.printing.PrintingServiceUtils.isVirtualPrinter;

public class PrintServiceManager {


    private PrintServiceManager() {
    }

    public static CompletableFuture<Void> printFileAsync(File file, PrintSettings params) {
        return CompletableFuture.runAsync(() -> printFile(file, params));
    }

    public static void printFile(File file, PrintSettings params) throws PrinterServiceException {
        if (!file.exists()) {
            throw new PrinterServiceException("Файл не існує: " + file.getAbsolutePath());
        }
        try {
            if (Objects.requireNonNull(params.getPrintingMethod()) == PrintingMethod.DESKTOP_API) {
                printViaDesktopApi(file, params.getPrinterName(), params.isSilentPrint());
            } else {
                PrintingService.printFile(file, params);
            }
        } catch (Exception ex) {
            ConsoleUtil.printRed(ex.getMessage());
            throw new PrinterServiceException(ex.getMessage());
        }
    }

    private static void printViaDesktopApi(File file, String printerName, boolean silent) throws Exception {
        if (silent && !isVirtualPrinter(printerName) && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.PRINT)) {
            Desktop.getDesktop().print(file);
        } else printTo(file, printerName, silent);

    }


    /**
     * Печать на указанный по имени принтер. Если не найден — печать на дефолт
     */
    private static void printTo(File file, String printerName, boolean silentPrint) throws IOException, InterruptedException, PrinterException {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService target = null;

        for (PrintService service : services) {
            if (service.getName().equalsIgnoreCase(printerName)) {
                target = service;
                break;
            }
        }

        if (target == null) {
            System.out.println("Принтер \"" + printerName + "\" не найден. Используем принтер по умолчанию.");
            target = PrintServiceLookup.lookupDefaultPrintService();
        }

        if (target == null) {
            System.out.println("Принтер по умолчанию не найден.");
            return;
        }

        printToService(target, file, silentPrint);
    }

    /**
     * Основной метод печати с выбранным принтером
     */
    private static void printToService(PrintService printer, File file, boolean silentPrint) throws IOException, InterruptedException, PrinterException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            printPdf(file, printer, silentPrint);
        } else if (name.endsWith(".docx")) {
            printDocxViaPowerShell(file, printer.getName(), silentPrint);
        } else {
            System.out.println("Неподдерживаемый формат файла: " + file.getName());
        }
    }

    /**
     * Печать PDF-файла через PDFBox
     */
    private static void printPdf(File file, PrintService printer, boolean silentPrint) throws InterruptedException, PrinterException, IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(printer);
            job.setPageable(new PDFPageable(document));
            job.setJobName(file.getName());
            job.setCopies(1);

            if (silentPrint) {
                job.print();
                return;
            }
            Desktop.getDesktop().open(file);
            Thread.sleep(2000);
            PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();
            attributeSet.add(PrintQuality.HIGH);
            attributeSet.add(new JobName("Result print " + file.getName(), Locale.getDefault()));
            job.printDialog(attributeSet);
           /* try {
                previewPdfViaPowerShell(file);
            } catch (InterruptedException | IOException e) {
                PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();
                attributeSet.add(PrintQuality.HIGH);
                attributeSet.add(new JobName("Result print " + file.getName(), Locale.getDefault()));
                job.printDialog(attributeSet);
            }*/
        }
    }

    private static void previewPdfViaPowerShell(File file) throws IOException, InterruptedException {
        // можно пробовать опен, а потом принт
        // Экранируем обратные слэши и одиночные кавычки
        String path = file.getAbsolutePath().replace("\\", "\\\\").replace("'", "''");

        // Собираем многострочный PS-скрипт в одну строку
        String psCommand = String.join(";",
                // Запуск и возвращение процесса
                "$proc = Start-Process -FilePath '" + path + "' -PassThru -WindowStyle Normal",
                "Start-Sleep -Seconds 5",
                // Инициализация WScript.Shell
                "$wshell = New-Object -ComObject WScript.Shell",
                // Активация окна просмотра
                "$wshell.AppActivate($proc.Id)",
                "Start-Sleep -Milliseconds 500",
                // Вызов диалога печати и подтверждение
                "$wshell.SendKeys('^p')"
        );

        executePowerShellCommand(psCommand, true);
    }



    /**
     * Печать DOCX-файла через PowerShell (только Windows)
     */
    private static void printDocxViaPowerShell(File file, String printerName, boolean silentPrint) throws InterruptedException, IOException {

        String path = file.getAbsolutePath().replace("\\", "\\\\");
        String safePrinterName = printerName.replace("\"", "`\""); // экранируем кавычки

        String psCommand;

        if (silentPrint) {
            psCommand = String.format(
                    "$w=New-Object -ComObject Word.Application; " +
                            "$w.Visible=$false; " +
                            "$doc=$w.Documents.Open('%s'); " +
                            "$w.ActivePrinter='%s'; " +
                            "$doc.PrintOut(); " +
                            "$doc.Close(); $w.Quit();",
                    path.replace("'", "''"),
                    safePrinterName
            );
        } else {
            psCommand = String.format(
                    "$w=New-Object -ComObject Word.Application; " +
                            "$w.Visible=$true; " +
                            "$doc=$w.Documents.Open('%s'); " +
                            "$w.ActivePrinter='%s'; " +
                            "$w.Dialogs.Item(88).Display();",
                    path.replace("'", "''"),
                    safePrinterName
            );
        }

        executePowerShellCommand(psCommand,false);

    }
    private static void executePowerShellCommand(String psCommand, boolean noProfile) throws IOException, InterruptedException {
        ProcessBuilder builder;
        if (noProfile) {
            builder = new ProcessBuilder("powershell", "-NoProfile", "-Command", psCommand);
        }else
            builder = new ProcessBuilder("powershell", "-Command", psCommand);
        builder.inheritIO();
        Process process = builder.start();
        process.waitFor(15, TimeUnit.SECONDS);

    }
}
