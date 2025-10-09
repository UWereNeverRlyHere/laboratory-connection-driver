package ywh.labs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ywh.services.printing.PrintersService;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PrinterServiceTest {

    @Test
    void getAllPrinters() {
       var allPrinters =  PrintersService.getAvailableActivePrintersJNA();
        for (String printer : allPrinters) {
            System.out.println(printer);
        }
        Assertions.assertNotNull(allPrinters);
        assertFalse(allPrinters.isEmpty());
    }
    //Microsoft Print to PDF
    //HP LaserJet Professional M1132 MFP
   /* @Test
    void print() {
        Assertions.assertDoesNotThrow(() -> PrintingService.printFile(
                //new File("D:\\Java projects\\Mini_Laboratory_Connection_Driver\\services\\output\\2025_06_16_00_42_11-Сірко.pdf"),
                new File("D:\\Java projects\\Mini_Laboratory_Connection_Driver\\services\\output\\2025_06_29_02_03_08-.docx"),
                new PrintingParams("Microsoft Print to PDF", PrintingMethod.POWERSHELL_COM, false)
        ));
    }*/
}