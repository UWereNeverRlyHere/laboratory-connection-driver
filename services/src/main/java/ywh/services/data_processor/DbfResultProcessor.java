package ywh.services.data_processor;

import com.linuxense.javadbf.DBFDataType;
import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFWriter;
import ywh.commons.ConsoleUtil;
import ywh.commons.Environment;
import ywh.commons.ImageUtils;
import ywh.commons.Task;
import ywh.services.data.enums.ObservationKey;
import ywh.services.data.enums.ProcessResult;
import ywh.services.data.mapping.LocalObservationMapper;
import ywh.services.data.models.ProcessorResult;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.settings.data.FileResultProcessorSettings;
import ywh.logging.DeviceLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class DbfResultProcessor {

    private final Path outputPath;

    public DbfResultProcessor(FileResultProcessorSettings settings) {
        if (settings.isUseFtp()) {
            this.outputPath = Path.of("output/");
        } else
            this.outputPath = settings.getOutputPath();
        Environment.createDirectoryIfNotExists(outputPath);
    }


    public ProcessorResult process(ObservationData data, DeviceLogger logger) {

        File dbfFile = getDbfFile(data);
        Path imagesDir = dbfFile.toPath().getParent();
        var result = new ProcessorResult(ProcessResult.SUCCESS, Optional.of(dbfFile));
        data.getImages().forEach((name, image) -> Task.start(() -> {
            try {
                logger.log("Trying to process image" + name + "...");
                ImageUtils.saveBase64Image(image, imagesDir.resolve(name));
                logger.log("Image " + name + " processed successfully");
            } catch (IOException e) {
                logger.error("Error while saving an image", e);
            }
        }));
        logger.log("Trying to process DBF file " + dbfFile.getName() + "...");
        List<String> onTop = List.of(ObservationKey.ANIMAL_TYPE.getName(),
                ObservationKey.AGE.getName(), ObservationKey.DATE.getName(), ObservationKey.ID.getName());
        try (FileOutputStream fos = new FileOutputStream(dbfFile);
             DBFWriter writer = new DBFWriter(fos, Charset.forName("Windows-1251"))) {
            writer.setFields(createDbfFields());

            data.getData().forEach((k, v) -> {
                if (onTop.contains(k)) return;
                writer.addRecord(new Object[]{k, v});
            });
            writer.addRecord(new Object[]{"Animal", data.getAnimalType().getUaDefaultName()});
            writer.addRecord(new Object[]{"Age", data.getValue(ObservationKey.AGE).orElse("")});
            writer.addRecord(new Object[]{"Date", data.getValue(ObservationKey.DATE).orElse("")});
            writer.addRecord(new Object[]{"Id", data.getValue(ObservationKey.ID).orElse("")});

        } catch (IOException e) {
            logger.error("Error while creating DBF file", e);
            result = new ProcessorResult(ProcessResult.FAILURE, ObservationResultProcessor.determineResultFile(dbfFile));
            Task.cancelAll();
            return result;
        }
        Task.awaitAll();
        ConsoleUtil.printGreen("DBF таблиця створена");
        logger.log("DBF file " + dbfFile.getName() + " processed successfully");
        return result;
    }

    private File getDbfFile(ObservationData data) {
        File outputFolder = new File(outputPath + File.separator + LocalObservationMapper.getDbfName(data));
        Environment.createDirectoryIfNotExists(outputFolder);
        return new File(outputFolder, "data.dbf");
    }

    private DBFField[] createDbfFields() {
        DBFField[] fields = new DBFField[2];
        DBFField keyField = createDbField("Key", 50);
        DBFField valueField = createDbField("Value", 200);
        fields[0] = keyField;
        fields[1] = valueField;
        return fields;
    }

    private DBFField createDbField(String name, int length) {
        DBFField field = new DBFField();
        field.setName(name);
        field.setType(DBFDataType.CHARACTER);
        field.setLength(length);
        return field;
    }

}
