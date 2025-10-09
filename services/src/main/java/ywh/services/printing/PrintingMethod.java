package ywh.services.printing;

public enum PrintingMethod {
    /**
     * Стандартний метод друку через Desktop API Java
     */
    DESKTOP_DOC_FLAVOR("Desktop DocFlavor", "Використовує Java DocFlavor"),

    /**
     * Друк через PowerShell з Out-Printer cmdlet
     */
    POWERSHELL("PowerShell", "Використовує PowerShell Out-Printer"),

    /**
     * Друк через PDFBox бібліотеку
     */
    //  PDF_BOX("PDFBox", "Використовує Apache PDFBox PrinterJob"),

    /**
     * Друк через Windows командну строку
     */
    CMD("Windows CMD", "Використовує rundll32 та системні команди"),

    /**
     * Друк через PowerShell COM об'єкти Office
     */
    POWERSHELL_COM("PowerShell COM", "Використовує COM об'єкти Word/Excel через PowerShell"),

    /**
     * Друк через Adobe Reader командну строку
     */
    // ADOBE_READER("Adobe Reader", "Використовує AcroRd32.exe для PDF"),

    /**
     * Друк через SumatraPDF командну строку
     */
    //  SUMATRA_PDF("SumatraPDF", "Використовує SumatraPDF.exe для PDF"),

    /**
     * Автоматичний вибір найкращого методу на основі типу файлу
     */
    AUTO("Auto", "Автоматично обирає найкращий метод"),

    DESKTOP_API("Desktop API","Використовує Java Desktop.print()");

    private final String displayName;
    private final String description;

    PrintingMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}