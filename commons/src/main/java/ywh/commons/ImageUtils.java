package ywh.commons;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;

public final class ImageUtils {


    private ImageUtils() {
    }



    public static Path saveBase64Image(String base64, Path target) throws IOException {
        ConsoleUtil.printGreen(DateTime.getDateTimeForJson()+ " Конвертацію почато: " + target);
        byte[] bytes = decodeBase64(base64);

        String format = detectImageFormat(bytes);
        if (format == null) {
            throw new IOException("Не вдалося виявити формат зображення");
        }
        Path outputPath = Files.isDirectory(target)
                ? target.resolve(defaultFileName(format))
                : target.resolveSibling(target.getFileName() + "." + format.toLowerCase(Locale.ROOT));

        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("Файл не є зображенням");
            }
            ImageIO.write(image, format, outputPath.toFile());
        }
         ConsoleUtil.printBlue(DateTime.getDateTimeForJson()+ " Конвертацію завершено: " + target);
        return outputPath;
    }


    public static BufferedImage decodeBase64ToBufferedImage(String base64) throws IOException {
        byte[] bytes = decodeBase64(base64);
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("Не вдалося декодувати зображення");
            }
            return image;
        }
    }
    public static byte[] decodeBase64ToImageBytes(String base64) throws IOException {
        byte[] bytes = decodeBase64(base64);
        String format = detectImageFormat(bytes);
        if (format == null) {
            throw new IOException("Не вдалося визначити формат зображення");
        }

        return bytes;
    }


    public static byte[] decodeBase64(String base64) {
        int comma = base64.indexOf(',');
        String pureBase64 = comma >= 0 ? base64.substring(comma + 1) : base64;
        return Base64.getDecoder().decode(pureBase64);
    }

    public static String detectImageFormat(byte[] bytes) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                return reader.getFormatName();
            }
        }
        return null;
    }

    private static String defaultFileName(String format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "image_" + timestamp + "." + format.toLowerCase(Locale.ROOT);
    }
}