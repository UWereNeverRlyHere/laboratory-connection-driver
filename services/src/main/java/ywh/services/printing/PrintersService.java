package ywh.services.printing;

import com.sun.jna.platform.win32.Winspool;
import com.sun.jna.platform.win32.WinspoolUtil;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PrintersService {
    private PrintersService() {
    }
    public static PrintService[] getAvailablePrintServices() {
        List<String> availableNames = getAvailableActivePrintersJNA();
        PrintService[] allServices = PrintServiceLookup.lookupPrintServices(
                DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);

        return Arrays.stream(allServices)
                .filter(service -> availableNames.contains(service.getName()))
                .toArray(PrintService[]::new);
    }

    /**
     * Получить список принтеров, которые физически включены и доступны, используя JNA.
     * Работает только на Windows.
     */
    public static List<String> getAvailableActivePrintersJNA() {
        List<String> availablePrinters = new ArrayList<>();
        try {
            Winspool.PRINTER_INFO_4[] printers4 = WinspoolUtil.getPrinterInfo4();
            for (Winspool.PRINTER_INFO_4 pr4 : printers4) {
                String name = pr4.pPrinterName;
                Winspool.PRINTER_INFO_2 info2 = WinspoolUtil.getPrinterInfo2(name);
                int status = info2.Status;
                int attrs = info2.Attributes;

                if ((status & Winspool.PRINTER_STATUS_OFFLINE) == 0 &&
                        (attrs & Winspool.PRINTER_ATTRIBUTE_WORK_OFFLINE) == 0) {
                    availablePrinters.add(name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return availablePrinters;
    }

    /**
     * Получает список принтеров через PowerShell.
     * Работает только на Windows.
     * @return список имен принтеров
     * @throws IOException если возникает ошибка при выполнении команды PowerShell
     */
    public static List<String> getPrintersViaPowerShell() throws IOException {
        List<String> printers = new ArrayList<>();
        String command = "powershell.exe Get-Printer | Select-Object -ExpandProperty Name";

        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "cp866"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    printers.add(line);
                }
            }
        }

        try {
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Команда PowerShell була перервана", e);
        }

        return printers;
    }

    /**
     * Получает текущий принтер по умолчанию.
     */
    protected static String getCurrentDefaultPrinter() {
        try {
            return getDefaultPrinterViaPowerShell();
        } catch (IOException e) {
            return null;
        }
    }

    public static String getDefaultPrinter() {
       return PrintServiceLookup.lookupDefaultPrintService().getName();
    }

    /**
     * Получает имя принтера по умолчанию через PowerShell.
     *
     * @return имя принтера по умолчанию
     * @throws IOException если возникает ошибка при выполнении команды
     */
    public static String getDefaultPrinterViaPowerShell() throws IOException {
        String command = "powershell.exe -Command \"Get-WmiObject -Class Win32_Printer | Where-Object {$_.Default -eq $true} | Select-Object -ExpandProperty Name\"";

        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "cp866"))) {
            String defaultPrinter = reader.readLine();
            if (defaultPrinter != null) {
                defaultPrinter = defaultPrinter.trim();
            }

            process.waitFor(10, TimeUnit.SECONDS);
            return defaultPrinter;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Команда отримання принтера за замовчуванням була перервана", e);
        }
    }


}
