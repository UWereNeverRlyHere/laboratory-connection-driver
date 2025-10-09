package ywh.services.files;

import org.apache.poi.xwpf.usermodel.*;
import ywh.commons.TextUtils;
import ywh.repository.analysis.entities.ReferenceRange;
import ywh.repository.animals.enteties.AnimalType;
import ywh.services.data.enums.ObservationKey;
import ywh.services.data.models.observation.Deviation;
import ywh.services.data.models.observation.DeviationType;
import ywh.services.data.models.observation.PrintIndicatorResultModel;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.data.mapping.LocalObservationMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordTemplateService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]+)\\}");

    private final File templateFile;
    private final Path outputPath;

    public WordTemplateService(File templateFile, Path outputPath) {
        this.templateFile = templateFile;
        this.outputPath = outputPath;
    }

    public String generate(ObservationData data) throws IOException {
        XWPFDocument doc = loadTemplate(templateFile);

        // 1) Разворачиваем таблицы с индикаторами
        for (XWPFTable tbl : doc.getTables()) {
            expandIndicatorTable(tbl, data);
        }

        // 2) Собираем глобальную карту для всех остальных плейсхолдеров
        Map<String, String> globalMap = buildObservationMap(data);
        // Если в ObservationData есть методы getOwner(), getPhoneNumber() и т.д.:
        data.getOwner().ifPresent(v -> globalMap.put(ObservationKey.OWNER.getName(), v));
        data.getPhoneNumber().ifPresent(v -> globalMap.put(ObservationKey.PHONE.getName(), v));
        data.getPrintDate().ifPresent(v -> globalMap.put(ObservationKey.PRINT_DATE.getName(), v));
        globalMap.put(ObservationKey.ANIMAL_TYPE.getName(), data.getAnimalType().getUaDefaultName());
        globalMap.put(ObservationKey.ANIMAL_NORM_NAME.getName(), data.getAnimalType().getAnimalNormName());
        data.getAnimalName().ifPresent(v -> globalMap.put(ObservationKey.ANIMAL_NAME.getName(), v));
        data.getAge().ifPresent(v -> globalMap.put(ObservationKey.AGE.getName(), v));

        // 3) Заменяем все оставшиеся {…} в документе
        replacePlaceholdersInDocument(doc, globalMap);
        // 4) Добавляем картинки
        WordTemplateImagesCreator.addImagesAfterTables(doc, data);
        // 4) Сохраняем
        String outName = LocalObservationMapper.getFileName(data);
        return saveDocument(doc, outName);
    }


    // —————— развернуть и заполнить таблицу индикаторов ——————

    private void expandIndicatorTable(XWPFTable table, ObservationData data) {
        XWPFTableRow tmpl = findTemplateRow(table);
        if (tmpl == null) return;
        int idx = table.getRows().indexOf(tmpl);
        List<PrintIndicatorResultModel> list = LocalObservationMapper.map(data);

        for (int i = 0; i < list.size(); i++) {
            XWPFTableRow row = copyRow(table, tmpl, idx + i);
            fillIndicatorRow(row, list.get(i), data.getAnimalType());
        }

        // удаляем сам шаблон
        table.removeRow(idx + list.size());
    }

    private XWPFTableRow findTemplateRow(XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            String text = getRowText(row);
            if (text.contains("{IndicatorCode}") ||
                    text.contains("{IndicatorName}") ||
                    text.contains("{Unit}") ||
                    text.contains("{Value}") ||
                    text.contains("{Norm}") ||
                    text.contains("{Deviation}")) {
                return row;
            }
        }
        return null;
    }

    private String getRowText(XWPFTableRow row) {
        StringBuilder sb = new StringBuilder();
        for (XWPFTableCell cell : row.getTableCells()) {
            sb.append(cell.getText()).append(" ");
        }
        return sb.toString();
    }


    private XWPFTableRow copyRow(XWPFTable table, XWPFTableRow src, int pos) {
        XWPFTableRow newRow = table.insertNewTableRow(pos);

        List<XWPFTableCell> srcCells = src.getTableCells();
        for (int i = 0; i < srcCells.size(); i++) {
            XWPFTableCell srcCell = srcCells.get(i);
            XWPFTableCell newCell = i < newRow.getTableCells().size()
                    ? newRow.getCell(i)
                    : newRow.createCell();

            // Копируем свойства ЯЧЕЙКИ (vertical align, shading, width…)
            if (srcCell.getCTTc().isSetTcPr()) {
                newCell.getCTTc().setTcPr(srcCell.getCTTc().getTcPr());
            }

            // Проходим по параграфам
            for (XWPFParagraph srcP : srcCell.getParagraphs()) {
                XWPFParagraph newP = newCell.addParagraph();

                // Копируем свойства ПАРАГРАФА (align, spacing, style …)
                if (srcP.getCTP().isSetPPr()) {
                    newP.getCTP().setPPr(srcP.getCTP().getPPr());
                }

                // Копируем runs (при необходимости – со всем RPr)
                for (XWPFRun srcRun : srcP.getRuns()) {
                    XWPFRun newRun = newP.createRun();
                    newRun.setText(srcRun.getText(0), 0);
                    if (srcRun.getCTR().isSetRPr()) {
                        newRun.getCTR().setRPr(srcRun.getCTR().getRPr());
                    }
                }
            }

            // Удаляем лишний пустой параграф, созданный POI по-умолчанию
            if (newCell.getParagraphs().size() > srcCell.getParagraphs().size()) {
                newCell.removeParagraph(0);
            }
        }
        // Копируем свойства СТРОКИ (высота, header, cantSplit…)
        newRow.getCtRow().setTrPr(src.getCtRow().getTrPr());

        return newRow;
    }

    private void fillIndicatorRow(XWPFTableRow row,
                                  PrintIndicatorResultModel ind,
                                  AnimalType type) {
        if (TextUtils.isNullOrEmpty(ind.value())) return;

        ReferenceRange rr = ind.indicator()
                .getReferenceRange(type)
                .orElse(new ReferenceRange());

        Map<String, String> map = new HashMap<>();
        map.put("IndicatorCode", ind.indicator().getCode());
        map.put("IndicatorName", ind.indicator().getName());
        map.put("Unit", rr.getUnit() != null ? rr.getUnit().getShortName() : "");
        map.put("Norm", TextUtils.isNotNullOrEmpty(rr.getText()) ? rr.getText() : "");
        map.put("Value", ind.value());
        map.put("Deviation", ind.deviation().text());

        String valueText = ind.value();
        DeviationType deviationType = ind.deviation().type();

        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph p : cell.getParagraphs()) {

                // 1) Заменяем все плейсхолдеры в этом параграфе
                replacePlaceholders(p, map);

                // 2) Пробегаемся по run’ам и увеличиваем шрифт
                //    только у тех, где текст == valueText (и только если есть отклонение)
                if (deviationType != DeviationType.NORMAL) {
                    for (XWPFRun run : p.getRuns()) {
                        String txt = run.getText(0);
                        if (valueText.equals(txt)) {
                            Double sz = run.getFontSizeAsDouble();
                            run.setFontSize((sz != null ? sz : 12) + 1);
                        }
                    }
                }

                // 3) Окрашиваем только описание отклонения
                styleDeviation(p, ind.deviation());
            }
        }
    }

    // —————— сборка глобальной карты для ObservationKey ——————

    private Map<String, String> buildObservationMap(ObservationData data) {
        Map<String, String> m = new HashMap<>();
        for (ObservationKey key : ObservationKey.values()) {
            data.getValue(key.name())
                    .ifPresent(v -> m.put(key.name(), v));
        }
        return m;
    }

    // —————— замена плейсхолдеров в документе целиком ——————

    private void replacePlaceholdersInDocument(XWPFDocument doc,
                                               Map<String, String> map) {
        // обычные параграфы
        for (XWPFParagraph p : doc.getParagraphs()) {
            replacePlaceholders(p, map);
        }
        // таблицы
        for (XWPFTable tbl : doc.getTables()) {
            for (XWPFTableRow row : tbl.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        replacePlaceholders(p, map);
                    }
                }
            }
        }
    }

    /**
     * Склеиваем runs, ищем все {ключи} и заменяем.
     */
    private void replacePlaceholders(XWPFParagraph p, Map<String, String> map) {
        List<XWPFRun> runs = p.getRuns();
        if (runs.isEmpty()) return;

        // объединяем соседние куски, если плейсхолдер «разорван»
        for (int i = 0; i < runs.size() - 1; i++) {
            String t = runs.get(i).getText(0);
            if (t != null && t.contains("{") && !t.contains("}")) {
                StringBuilder sb = new StringBuilder(t);
                int j = i + 1;
                while (j < runs.size()) {
                    String nt = runs.get(j).getText(0);
                    sb.append(nt == null ? "" : nt);
                    if (nt != null && nt.contains("}")) break;
                    j++;
                }
                runs.get(i).setText(sb.toString(), 0);
                for (int k = j; k > i; k--) {
                    p.removeRun(k);
                }
            }
        }

        // собственно, поиск и замена
        for (XWPFRun run : p.getRuns()) {
            String txt = run.getText(0);
            if (txt == null || !txt.contains("{")) continue;
            Matcher m = PLACEHOLDER.matcher(txt);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String key = m.group(1);
                String val = map.getOrDefault(key, "");
                m.appendReplacement(sb, Matcher.quoteReplacement(val));
            }
            m.appendTail(sb);
            run.setText(sb.toString(), 0);
        }
    }

    // если нужно подсветить/увеличить «отклонение»
    private void styleDeviation(XWPFParagraph p, Deviation dev) {
        if (dev.type() == DeviationType.NORMAL) return;
        String def = dev.text();
        for (XWPFRun run : p.getRuns()) {
            if (def.equals(run.getText(0))) {
                run.setColor(dev.type() == DeviationType.UPPER || dev.type() == DeviationType.NOT_NORMAL ? "8B0000" : "1E90FF");
            }
        }
    }

    private XWPFDocument loadTemplate(File f) throws IOException {
        try (FileInputStream is = new FileInputStream(f)) {
            return new XWPFDocument(is);
        }
    }

    private String saveDocument(XWPFDocument doc, String name) throws IOException {
        File dir = outputPath.toFile();
        if (!dir.exists()) dir.mkdirs();
        String out = dir.getAbsolutePath() + File.separator + name + ".docx";
        try (FileOutputStream os = new FileOutputStream(out)) {
            doc.write(os);
        }
        return out;
    }
}