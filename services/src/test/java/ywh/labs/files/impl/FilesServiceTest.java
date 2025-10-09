package ywh.labs.files.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ywh.services.data.enums.ObservationKey;
import ywh.services.data.models.observation.ObservationData;
import ywh.services.settings.data.FileResultProcessorSettings;
import ywh.services.files.DocxPdfConverter;
import ywh.services.files.WordTemplateService;

import java.io.File;
import java.io.IOException;


class FilesServiceTest {

    @Test
    void shouldCreateWordAndConvertItToPdf() throws IOException, InterruptedException {
        var processorParams = new FileResultProcessorSettings();
        var service = new WordTemplateService(processorParams.getTemplateFile(),processorParams.getOutputPath());
        var parameters = new ObservationData();
        parameters.put(ObservationKey.OWNER,"Ivan Petrovich");
        parameters.put(ObservationKey.ANIMAL_TYPE,"Кіт");
        parameters.put(ObservationKey.DATE, "12.01.2022");
        parameters.put(ObservationKey.ANIMAL_NAME, "Сірко");
        parameters.put(ObservationKey.AGE, "3");
        parameters.put("WBC", "8.4");

        File file = new File(service.generate(parameters));
        Assertions.assertTrue(file.exists());
        var pdfService = new DocxPdfConverter(file.getAbsolutePath());
        pdfService.convert();
        Assertions.assertTrue(pdfService.getPdfFile().exists());
        Assertions.assertFalse(file.exists());

    }
}