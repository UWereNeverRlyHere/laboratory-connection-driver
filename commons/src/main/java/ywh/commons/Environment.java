package ywh.commons;

import java.io.File;
import java.nio.file.Path;

public class Environment {

    private Environment() {
    }

    public static void createDirectoryIfNotExists(String path) {
        createDirectoryIfNotExists(new File(path));
    }

    public static void createDirectoryIfNotExists(Path path) {
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }
    }

    public static void createDirectoryIfNotExists(File directory) {
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                System.err.println("Не вдалося створити папку: " + directory.getAbsolutePath());
            }
        }
    }
}
