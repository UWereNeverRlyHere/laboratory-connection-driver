package ywh.services.settings.data;

import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
@Data
public class FileResultProcessorSettings {
    private FtpSettings ftpSettings = new FtpSettings("", "", "");
    private boolean useFtp = false;

    private String templateFilePath = "templates/wordTemplate.docx";

    private String outputPathString = "output/";


    public FileResultProcessorSettings() {
        // Создаём директорию для файла шаблона
        File templateFile = new File(templateFilePath);
        File templateDir = templateFile.getParentFile();
        if (templateDir != null && !templateDir.exists()) {
            boolean dirsCreated = templateDir.mkdirs();
            if (!dirsCreated) {
                throw new RuntimeException("Не удалось создать директорию: " + templateDir.getAbsolutePath());
            }
        }
        
        // Если файла шаблона нет, создаём его
        if (!templateFile.exists()) {
            try {
                boolean fileCreated = templateFile.createNewFile();
                if (!fileCreated) {
                    throw new RuntimeException("Не удалось создать файл шаблона: " + templateFile.getAbsolutePath());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Создаём выходную директорию
        File outputDir = new File(outputPathString);
        if (!outputDir.exists()) {
            boolean dirsCreated = outputDir.mkdirs();
            if (!dirsCreated) {
                throw new RuntimeException("Не удалось создать выходную директорию: " + outputDir.getAbsolutePath());
            }
        }
    }




    // Методы для работы с File (конвертируем из String)
    public File getTemplateFile() {
        return new File(templateFilePath);
    }

    public FileResultProcessorSettings setTemplateFile(File templateFile) {
        this.templateFilePath = templateFile.getAbsolutePath();
        return this;
    }

    // Методы для работы с Path (конвертируем из String)
    public Path getOutputPath() {
        return Paths.get(outputPathString);
    }

    public FileResultProcessorSettings setOutputPath(Path outputPath) {
        this.outputPathString = outputPath.toString();
        return this;
    }

    // Геттеры/сеттеры для String полей (для прямого доступа если нужно)
    public String getTemplateFilePathString() {
        return templateFilePath;
    }

}