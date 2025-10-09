package ywh.services.printing;

import ywh.services.settings.data.PrintSettings;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static ywh.services.printing.PrintingServiceUtils.isVirtualPrinter;

/**
 * Служба для роботи з принтерами та друку файлів.
 */
public class PrintingService {

    private PrintingService() {
    }

    /**
     * Основний метод друку.
     *
     * @param file   файл, що друкуємо
     * @param params параметри друку
     */
    public static void printFile(File file, PrintSettings params) throws PrinterServiceException {
        if (!file.exists()) {
            throw new PrinterServiceException("Файл не існує: " + file.getAbsolutePath());
        }

        logPrintAttempt(file, params.getPrinterName(), params.getPrinterName());
        var start = System.currentTimeMillis();

        try {
            switch (params.getPrintingMethod()) {
                case DESKTOP_DOC_FLAVOR -> printViaDesktopDocFlavor(file, params.getPrinterName(), params.isSilentPrint());
                case POWERSHELL -> printViaPowerShell(file, params.getPrinterName(), params.isSilentPrint());
                case CMD -> printViaWindowsCommand(file, params.getPrinterName(), params.isSilentPrint());
                case POWERSHELL_COM -> printOfficeViaCom(file, params.getPrinterName(), params.isSilentPrint());
                case DESKTOP_API -> printViaDesktopApi(file, params.getPrinterName(), params.isSilentPrint());
                case AUTO -> autoPrint(file, params.getPrinterName(), params.isSilentPrint());
                default -> throw new PrinterServiceException("Непідтримуваний метод: " + params.getPrintingMethod());
            }
            logPrintSuccess(file, params.getPrinterName(), params.getPrintingMethod(),
                    System.currentTimeMillis() - start);
        } catch (Exception ex) {
            logPrintFailure(file, params.getPrinterName(), params.getPrintingMethod(),
                    System.currentTimeMillis() - start, ex.getMessage());
            throw new PrinterServiceException(ex.getMessage());
        }
    }

    private static void printViaDesktopApi(File file, String printerName, boolean silent) throws Exception {
        if (silent && !isVirtualPrinter(printerName) && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.PRINT)) {
            Desktop.getDesktop().print(file);
        }
        else autoPrint(file,printerName,silent);
   /*     PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
        defaultService.createPrintJob().print();*/
    }




    /**
     * Друк через Desktop API.
     * Фізичні: викликає діалог; silent: посилає у спулер.
     */
    private static void printViaDesktopDocFlavor(File file, String printerName, boolean silent)
            throws IOException, PrintException {
        if (isVirtualPrinter(printerName)) {
            // Для віртуальних принтерів
            if (silent) {
                PrintingServiceUtils.saveToConnectionFolder(file);
            } else {
                PrintingServiceUtils.promptSaveDialog(file);
            }
            return;
        }

        // Для фізичних принтерів
        if (silent) {
            // Silent друк через Java Print Service
            printViaPrintService(file, printerName, true);
        } else {
            PrintingServiceUtils.promptPrintDialog(file, printerName);

        }

    }
    /**
     * Допоміжний метод для друку через Java Print Service
     */
    private static void printViaPrintService(File file, String printerName, boolean silent)
            throws IOException, PrintException {

        // Знаходимо потрібний принтер
        PrintService printService;
        if (printerName != null && !printerName.isEmpty()) {
            printService = Stream.of(PrintServiceLookup.lookupPrintServices(null, null))
                    .filter(s -> s.getName().equalsIgnoreCase(printerName))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Принтер не знайдено: " + printerName));
        } else {
            printService = PrintServiceLookup.lookupDefaultPrintService();
            if (printService == null) {
                throw new IOException("Принтер за замовчуванням не знайдено");
            }
        }

        // Визначаємо тип файлу та DocFlavor
        String fileName = file.getName().toLowerCase();
        DocFlavor flavor;

        if (fileName.endsWith(".pdf")) {
            flavor = DocFlavor.INPUT_STREAM.PDF;
        } else if (fileName.endsWith(".txt")) {
            flavor = DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_8;
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            flavor = DocFlavor.INPUT_STREAM.JPEG;
        } else if (fileName.endsWith(".png")) {
            flavor = DocFlavor.INPUT_STREAM.PNG;
        } else {
            // Для інших типів файлів (включаючи DOCX) використовуємо AUTOSENSE
            flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
        }

        // Перевіряємо, чи підтримує принтер цей формат
        if (!printService.isDocFlavorSupported(flavor)) {
            throw new IOException("Принтер не підтримує формат файлу: " + fileName);
        }

        try (var fis = Files.newInputStream(file.toPath())) {
            Doc doc = new SimpleDoc(fis, flavor, null);
            DocPrintJob job = printService.createPrintJob();
            if (silent) {
                // Silent друк без діалогу
                job.print(doc, null);
            } else {
                // Non-silent друк з діалогом
                PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
                // Відкриваємо діалог друку
                PrintingServiceUtils.promptPrintDialog(file, printerName);
            }
        }
    }

    /**
     * Друк через PowerShell.
     * Silent фізичний: Out-Printer; non-silent фізичний: Desktop.print.
     */
    private static void printViaPowerShell(File file, String printerName, boolean silent)
            throws IOException {
        var path = file.getAbsolutePath().replace("\\", "\\\\");
        if (isVirtualPrinter(printerName)) {
            if (silent) {
                PrintingServiceUtils.saveToConnectionFolder(file);
            } else {
                PrintingServiceUtils.promptSaveDialog(file);
            }
            return;  // нічого більше не друкуємо
        }

        if (silent) {
            // фізичний silent: Out-Printer
            var cmd = printerName != null
                    ? String.format("powershell.exe -Command \"Get-Content '%s' -Raw | Out-Printer -Name '%s'\"",
                    path, printerName.replace("'", "''"))
                    : String.format("powershell.exe -Command \"Get-Content '%s' -Raw | Out-Printer\"",
                    path);
            PrintingServiceUtils.executeCommand(cmd);
        } else {
            // фізичний non-silent – використовуємо Desktop API
            PrintingServiceUtils.promptPrintDialog(file, printerName);
        }
    }


    /**
     * Друк офісних документів через COM-автоматизацію Word/Excel.
     */
    /**
     * Усередині PrintingService, у методі printOfficeViaCom замінити логіку для віртуальних принтерів (.doc/.docx),
     * щоб Word через COM відразу експортував документ у PDF (без “Save As” діалогу).
     */
    private static void printOfficeViaCom(File file, String printerName, boolean silent)
            throws IOException {
        String ext = PrintingServiceUtils.getExtension(file.getName()).toLowerCase();
        boolean virtual = isVirtualPrinter(printerName);

        // 1) Обробка Word → PDF через COM, тільки для віртуального принтера
        if (virtual && (ext.equals("doc") || ext.equals("docx"))) {
            // Визначаємо базове ім’я з .pdf
            String base = file.getName().replaceAll("\\.(?i)docx?$", "");
            Path connDir = PrintingServiceUtils.getConnectionPrintFolder();
            AtomicReference<File> outFile = new AtomicReference<>();

            if (silent) {
                // Автоматичний silent: одразу у папку connection_driver_print
                outFile.set(connDir.resolve(base + ".pdf").toFile());
            } else {
                PrintingServiceUtils.promptPrintDialog(file, printerName);
            }
            ;
            // Формуємо та виконуємо PowerShell-скрипт експорту
            String psCommand = String.format(
                    "powershell -Command \"" +
                            "$w=New-Object -ComObject Word.Application; " +
                            "$w.Visible=$false; " +
                            "$doc=$w.Documents.Open('%s'); " +
                            "$doc.ExportAsFixedFormat('%s', %d); " +   // 17 = wdExportFormatPDF
                            "$doc.Close(); $w.Quit()\"",
                    file.getAbsolutePath().replace("'", "''"),
                    outFile.get().getAbsolutePath().replace("\\", "\\\\"),
                    17
            );
            PrintingServiceUtils.executeCommand(psCommand);
            return;
        }

        // 2) Інші випадки віртуальних принтерів (не-Word) — як раніше
        if (virtual) {
            if (silent) {
                PrintingServiceUtils.saveToConnectionFolder(file);
            } else {
                PrintingServiceUtils.promptSaveDialog(file);
            }
            return;
        }

        // 3) Фізичні принтери — стандартна гілка COM-PrintOut
        String script = switch (ext) {
            case "doc", "docx" -> PrintingServiceUtils.makeWordComScript(file, printerName, silent);
            case "xls", "xlsx" -> PrintingServiceUtils.makeExcelComScript(file, printerName, silent);
            default -> throw new IOException("COM друк не підтримує тип: " + ext);
        };
        PrintingServiceUtils.executeCommand(script);
    }


    /**
     * Друк через командний рядок Windows.
     */
    private static void printViaWindowsCommand(File file, String printerName, boolean silent)
            throws IOException {
        if (isVirtualPrinter(printerName)) {
            if (silent) {
                PrintingServiceUtils.saveToConnectionFolder(file);
            } else {
                PrintingServiceUtils.promptSaveDialog(file);
            }
            return;
        }
        // фізичний
        var filePath = file.getAbsolutePath();
        String cmd = printerName != null
                ? String.format("print /D:\"%s\" \"%s\"", printerName, filePath)
                : String.format("print \"%s\"", filePath);
        PrintingServiceUtils.executeCommand(cmd);
    }

    /**
     * Автоматичний вибір: PDF→Sumatra/Adobe, Office→COM, інші→Desktop.
     */
    private static void autoPrint(File file, String printerName, boolean silent)
            throws Exception {
        var ext = PrintingServiceUtils.getExtension(file.getName());
        if (ext.equalsIgnoreCase("pdf")) {
            // якщо хочете специфічний метод – можна змінити тут
            printViaDesktopDocFlavor(file, printerName, silent);
        } else if (ext.matches("doc|docx|xls|xlsx")) {
            printOfficeViaCom(file, printerName, silent);
        } else {
            PrintingServiceUtils.promptPrintDialog(file, printerName);
        }
    }


    // Заглушки для логів
    private static void logPrintAttempt(File f, String p, Object m) { /*…*/ }

    private static void logPrintSuccess(File f, String p, Object m, long t) { /*…*/ }

    private static void logPrintFailure(File f, String p, Object m, long t, String e) { /*…*/ }
}