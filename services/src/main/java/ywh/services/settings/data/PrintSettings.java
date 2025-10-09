package ywh.services.settings.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ywh.services.printing.PrintingMethod;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class PrintSettings {
    private String printerName ="";

    private PrintingMethod printingMethod = PrintingMethod.DESKTOP_API;
    private boolean silentPrint = false;
    private boolean docxPrint = true;

    public boolean isPrintFromPDF() {
        return !docxPrint;
    }


}
