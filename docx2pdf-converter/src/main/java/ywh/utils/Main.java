package ywh.utils;
import com.documents4j.api.DocumentType;
import com.documents4j.api.IConverter;
import com.documents4j.job.LocalConverter;

import java.io.File;

public class Main {
    private static final SimpleLogger LOGGER = new SimpleLogger("docx2pdf-converter");
    
    public static void main(String[] args) {
        LOGGER.info("docx2pdf-converter started");
        if (args.length != 2) {
            LOGGER.error("Usage: java -jar docx2pdf.jar <input.docx> <output.pdf>");
            System.err.println("Usage: java -jar docx2pdf.jar <input.docx> <output.pdf>");
            System.exit(1);
        }

        String inputPath = args[0];
        String outputPath = args[1];

        try {
            LOGGER.info("Trying to convert docx to pdf: " + inputPath + " -> " + outputPath + " ...");
            convertDocxToPdf(inputPath, outputPath);
            System.out.println("SUCCESS: " + outputPath);
        } catch (Exception e) {
            LOGGER.error("ERROR: " + e.getMessage(), e);
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void convertDocxToPdf(String inputPath, String outputPath) {
        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);

        if (!inputFile.exists()) {
            LOGGER.error("Input file not found: " + inputPath);
            throw new RuntimeException("Input file not found: " + inputPath);
        }

        // Створюємо директорію для output файлу
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            boolean mkdir = outputDir.mkdirs();
            LOGGER.info("Created output directory: " + mkdir);
        }

        IConverter converter = LocalConverter.builder().build();
        try {
            LOGGER.info("Converting docx to pdf: " + inputPath + " -> " + outputPath + " ...");
            converter.convert(inputFile).as(DocumentType.DOCX)
                    .to(outputFile).as(DocumentType.PDF)
                    .execute();
            LOGGER.info("Conversion completed successfully");
        } catch (Exception e) {
            LOGGER.error("ERROR while converting docx to pdf", e);
        } finally {
            converter.shutDown();
        }
    }
}