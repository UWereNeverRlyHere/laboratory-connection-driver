package ywh.services.printing;

import ywh.commons.Task;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.DocFlavor;
import javax.print.SimpleDoc;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.DocPrintJob;
import javax.print.ServiceUI;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class PrintingServiceUtils {
    private PrintingServiceUtils() {
    }
    // ——————————————————————————————————————————————————————————————————————————
    // Допоміжні методи
    // ——————————————————————————————————————————————————————————————————————————

    /**
     * Визначає, чи принтер віртуальний (PDF, XPS, Fax тощо).
     */
    public static boolean isVirtualPrinter(String name) {
        if (name == null) return false;
        var lower = name.toLowerCase();
        return lower.contains("pdf") || lower.contains("xps") ||
                lower.contains("onenote") || lower.contains("fax");
    }


    /**
     * Запускає системну команду та очікує її завершення.
        Пока тестирую новый принт менеджер, потом нужно будет всё это поубирать
     */
    @Deprecated()
    protected static void executeCommand(String command) throws IOException {
        try {
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Команда завершилась з кодом помилки: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Команда була перервана", e);
        }
    }



    /**
     * Підкладає розширення з імені файлу.
     */
    protected static String getExtension(String name) {
        var idx = name.lastIndexOf('.');
        return idx > 0 ? name.substring(idx + 1) : "";
    }

    /**
     * Папка Documents\connection_driver_print.
     */
    protected static Path getConnectionPrintFolder() throws IOException {
        var docs = Paths.get(System.getProperty("user.home"), "Documents");
        var dir = docs.resolve("connection_driver_print");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }

    /**
     * Silent: копіює файл до папки connection_driver_print.
     *
     * @return шлях до скопійованого файлу
     */
    protected static File saveToConnectionFolder(File src) throws IOException {
        var target = getConnectionPrintFolder().resolve(src.getName());
        Files.copy(src.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toFile();
    }

    protected static boolean isFirstPrint = true;
    /**
     * Non-silent: відкриває діалог "Зберегти як" з підстановкою вихідного імені.
     */
    protected static void promptSaveDialog(File src) throws IOException {
        if (isFirstPrint) {
            promptPrintDialog(src, PrintServiceLookup.lookupDefaultPrintService().getName());
            isFirstPrint = false;
        }
        Task.invokeOnEDT(() ->{
            Desktop.getDesktop();
            JFileChooser chooser = new JFileChooser();
            Path baseDir = getConnectionPrintFolder();
            chooser.setCurrentDirectory(baseDir.toFile());
            chooser.setDialogTitle("Зберегти як");
            chooser.setSelectedFile(new File(baseDir.toFile(), src.getName()));

            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File chosen = chooser.getSelectedFile();
                // Якщо файл існує — перезаписуємо за замовчуванням
                // або, щоб створити «копію», замість REPLACE_EXISTING генеруємо нове ім'я:
                if (chosen.exists()) {
                    // --- варіант перезапису ---
                    Files.copy(src.toPath(), chosen.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    // --- або варіант: Files.copy(src, generateUniqueName(chosen), NO_REPLACE) ---
                } else {
                    Files.copy(src.toPath(), chosen.toPath());
                }

            }
        },true);

    }

    /**
     * Non-silent: відкриває стандартний діалог друку замість "Зберегти як".
     */
    protected static void promptPrintDialog(File src, String printerName) {
        Task.invokeOnEDT(() -> {
            try {
                // Знаходимо принтер
                PrintService printService = findPrintService(printerName);
                // Визначаємо формат файлу
                DocFlavor flavor = getDocFlavor(src);
                // Перевіряємо підтримку
                if (!printService.isDocFlavorSupported(flavor)) {
                    throw new RuntimeException("Принтер не підтримує формат файлу: " + src.getName());
                }

                // Показуємо стандартний діалог друку
                PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
                PrintService selectedService = ServiceUI.printDialog(
                    null, 100, 100,
                    PrintServiceLookup.lookupPrintServices(flavor, null), 
                    printService,
                    flavor, 
                    attributes
                );

                if (selectedService != null) {
                    // Друкуємо
                    try (var fis = Files.newInputStream(src.toPath())) {
                        Doc doc = new SimpleDoc(fis, flavor, null);
                        DocPrintJob job = selectedService.createPrintJob();
                        job.print(doc, attributes);
                    }
                } else {
                    throw new RuntimeException("Користувач скасував друк");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, true);
    }

    // Допоміжні методи
    private static PrintService findPrintService(String printerName) throws IOException {
        if (printerName == null || printerName.isEmpty()) {
            PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
            if (defaultService == null) {
                throw new IOException("Принтер за замовчуванням не знайдено");
            }
            return defaultService;
        }

        return Stream.of(PrintServiceLookup.lookupPrintServices(null, null))
                .filter(s -> s.getName().equalsIgnoreCase(printerName))
                .findFirst()
                .orElseThrow(() -> new IOException("Принтер не знайдено: " + printerName));
    }

    private static DocFlavor getDocFlavor(File file) {
        String fileName = file.getName().toLowerCase();
        
        if (fileName.endsWith(".pdf")) {
            return DocFlavor.INPUT_STREAM.PDF;
        } else if (fileName.endsWith(".txt")) {
            return DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_8;
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return DocFlavor.INPUT_STREAM.JPEG;
        } else if (fileName.endsWith(".png")) {
            return DocFlavor.INPUT_STREAM.PNG;
        } else {
            return DocFlavor.INPUT_STREAM.AUTOSENSE;
        }
    }


    protected static String makeWordComScript(File file, String printer, boolean silent) throws IOException {
        var path = file.getAbsolutePath().replace("'", "''").replace("\"", "\\\"");
        var printerName = printer.replace("'", "''");

        if (silent) {
            return String.format(
                    "powershell -Command \"" +
                            "try { " +
                            "$w=New-Object -ComObject Word.Application; " +
                            "$w.Visible=$false; " +
                            "$w.DisplayAlerts=0; " + // Вимкнути всі попередження
                            "$d=$w.Documents.Open('%s'); " +
                            "$w.ActivePrinter='%s'; " +
                            "$d.PrintOut($true,$false,0); " + // Спрощені параметри
                            "$d.Close($false); " +
                            "$w.Quit(); " +
                            "Write-Host 'Success' " +
                            "} catch { " +
                            "Write-Error $_.Exception.Message; " +
                            "if($w) { $w.Quit() } " +
                            "}\"",
                    path, printerName
            );
        } else {
            // Для не-silent просто відкриваємо діалог друку
            return String.format(
                    "powershell -Command \"" +
                            "try { " +
                            "$w=New-Object -ComObject Word.Application; " +
                            "$w.Visible=$true; " +
                            "$d=$w.Documents.Open('%s'); " +
                            "$w.ActivePrinter='%s'; " +
                            "$w.Dialogs.Item(88).Show(); " + // 88 = wdDialogFilePrint
                            "$d.Close($false); " +
                            "$w.Quit() " +
                            "} catch { " +
                            "Write-Error $_.Exception.Message; " +
                            "if($w) { $w.Quit() } " +
                            "}\"",
                    path, printerName
            );
        }
    }


    protected static String makeExcelComScript(File file, String printer, boolean silent) throws IOException {
        var path = file.getAbsolutePath().replace("'", "''");
        var outFile = silent && isVirtualPrinter(printer)
                ? getConnectionPrintFolder().resolve(file.getName()).toString().replace("\\", "\\\\")
                : "";
        return String.format(
                "powershell -Command \"" +
                        "$e=New-Object -ComObject Excel.Application; " +
                        "$e.Visible=$false; " +
                        "$b=$e.Workbooks.Open('%s'); " +
                        "$e.ActivePrinter='%s'; " +
                        (silent
                                ? "$b.PrintOut($null,$null,$null,$true,'%s');"
                                : "$e.Dialogs.Item([Microsoft.Office.Interop.Excel.XlBuiltInDialog]::xlDialogPrint).Show();") +
                        "$b.Close($false); $e.Quit()\"",
                path, printer.replace("'", "''"), outFile
        );
    }

}