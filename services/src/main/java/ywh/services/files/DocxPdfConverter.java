package ywh.services.files;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для конвертации Word в PDF с универсальным определением имени выходного файла.
 */
public class DocxPdfConverter {
    private final File pdfFile;
    private final File docxFile;

    /**
     * Конструктор, принимающий путь к исходному файлу DOCX.
     * @param inputDocxPath путь к исходному файлу Word
     */
    public DocxPdfConverter(String inputDocxPath) {
        this.docxFile = new File(inputDocxPath);
        this.pdfFile = new File(replaceExtension(docxFile.getAbsolutePath()));
    }

    public DocxPdfConverter(File inputDocx) {
        this.docxFile =inputDocx;
        this.pdfFile = new File(replaceExtension(docxFile.getAbsolutePath()));
    }

    /**
     * Заменяет расширение файла на новое.
     * @param filename исходное имя файла
     * @return имя файла с заменённым расширением
     */
    private static String replaceExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) {
            return filename + ".pdf";
        } else {
            return filename.substring(0, dotIndex) + ".pdf";
        }
    }

    /**
     * Вилучає JAR з ресурсів у тимчасову папку.
     * @return шлях до тимчасового JAR файлу
     */
    private String extractJarFromResources() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/tools/docx2pdf.jar")) {
            if (is == null) {
                throw new IOException("docx2pdf.jar не знайдено в ресурсах по шляху /tools/docx2pdf.jar");
            }
            
            // Створюємо тимчасовий файл
            Path tempDir = Files.createTempDirectory("docx2pdf");
            File tempJar = new File(tempDir.toFile(), "docx2pdf.jar");
            
            // Копіюємо з ресурсів у тимчасовий файл
            try (FileOutputStream fos = new FileOutputStream(tempJar)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            // Встановлюємо видалення при завершенні програми
            tempJar.deleteOnExit();
            tempDir.toFile().deleteOnExit();
            
            return tempJar.getAbsolutePath();
        }
    }

    /**
     * Отримує шлях до Java виконавчого файлу.
     * @return шлях до java
     */
    private String getJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            return javaHome + File.separator + "bin" + File.separator + "java.exe";
        } else {
            return javaHome + File.separator + "bin" + File.separator + "java";
        }
    }

    /**
     * Конвертирует DOCX-файл в PDF.
     */
    public void convert() throws IOException, InterruptedException {
        // Вилучаємо JAR з ресурсів
        String jarPath = extractJarFromResources();
        
        // Отримуємо шлях до Java
        String javaExecutable = getJavaExecutable();
        
        ProcessBuilder pb = new ProcessBuilder(
            javaExecutable, "-jar", jarPath, 
            docxFile.getAbsolutePath(), 
            pdfFile.getAbsolutePath()
        );
        
        // Встановлюємо робочу директорію
        pb.directory(new File(System.getProperty("user.dir")));
        
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            while ((line = errorReader.readLine()) != null) {
                error.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Converter timeout");
        }

        if (process.exitValue() != 0) {
            throw new RuntimeException("Conversion failed: " + error.toString());
        }
    }



    public File getPdfFile() {
        return pdfFile;
    }

    public File getDocxFile() {
        return docxFile;
    }

}