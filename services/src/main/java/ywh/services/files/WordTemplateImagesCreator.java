package ywh.services.files;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import ywh.commons.ImageUtils;
import ywh.services.data.models.observation.ObservationData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

public class WordTemplateImagesCreator {
    private WordTemplateImagesCreator() {
    }

    protected static void addImagesAfterTables(XWPFDocument doc, ObservationData data) {
        if (data.getImages().isEmpty()) return;

        // создаём «пустой» абзац после всех таблиц (doc.createParagraph() всегда добавляется в конец)
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(10);
        p.setSpacingBefore(100);
        p.setSpacingBetween(100);
        p.setIndentationLeft(0);
        p.setIndentationRight(0);

        p.setAlignment(ParagraphAlignment.CENTER);
        // один run на каждое изображение, чтобы Word ставил их подряд в одной строке
        data.getImages().forEach((key, base64) -> {
            try {
                byte[] bytes = ImageUtils.decodeBase64(base64);

                // формат и тип картинки для Apache POI
                String format = ImageUtils.detectImageFormat(bytes);
                int poiType = getPoiPictureType(format);   // новый метод
                int width = 125;
                int height = 115;

                XWPFRun run = p.createRun();
                // Units.toEMU переводит px → EMU (англ. «English Metric Unit»)
                run.setText(" ");
                run.addPicture(
                        new ByteArrayInputStream(bytes),
                        poiType,
                        key + "." + format.toLowerCase(Locale.ROOT),
                        Units.toEMU(width),
                        Units.toEMU(height)
                );
            } catch (Exception ex) {
                // не валим генерацию документа — просто логируем/игнорируем
                ex.printStackTrace();
            }
        });
    }

    private static int getPoiPictureType(String format) throws IOException {
        if (format == null) throw new IOException("Невідомий формат зображення");
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "jpeg", "jpg" -> Document.PICTURE_TYPE_JPEG;
            case "png" -> Document.PICTURE_TYPE_PNG;
            case "bmp" -> Document.PICTURE_TYPE_BMP;
            case "gif" -> Document.PICTURE_TYPE_GIF;
            case "tiff", "tif" -> Document.PICTURE_TYPE_TIFF;
            case "emf" -> Document.PICTURE_TYPE_EMF;
            case "wmf" -> Document.PICTURE_TYPE_WMF;
            default -> throw new IOException("Формат зображення \"" + format + "\" не підтримується POI");
        };
    }
}
