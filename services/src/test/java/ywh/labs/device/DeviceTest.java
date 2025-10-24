
package ywh.labs.device;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ywh.repository.analysis.repos.RepositoryProvider;
import ywh.repository.analysis.repos.impl.JsonIndicatorOrderRepositoryImpl;
import ywh.repository.analysis.repos.impl.JsonIndicatorRepositoryImpl;
import ywh.services.communicator.TcpHostCommunicator;
import ywh.services.data.enums.FileResultActions;
import ywh.services.data.models.DeviceConfig;
import ywh.services.device.Device;
import ywh.services.device.parsers.DymindDF50Vet;
import ywh.services.device.parsers.ise.MIURA;
import ywh.services.settings.data.CommunicatorSettings;
import ywh.services.data.models.ParsingResult;
import ywh.services.data.models.ProcessorResult;
import ywh.services.settings.data.DeviceSettings;
import ywh.services.settings.data.FileResultProcessorSettings;

import ywh.logging.DeviceLogger;
import ywh.services.port_sender.IPortSender;
import ywh.commons.ConsoleUtil;
import ywh.repository.repo_exceptions.LoadException;

import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {
    @BeforeAll
    static void setUp() throws URISyntaxException, LoadException {
        // Отримуємо URL ресурсу і конвертуємо в Path
        URL resourceUrl = DeviceTest.class.getClassLoader().getResource("jsonRepository");
        Path resourcePath = Paths.get(resourceUrl.toURI());
        RepositoryProvider.initialize(new JsonIndicatorRepositoryImpl(resourcePath),new JsonIndicatorOrderRepositoryImpl(resourcePath));

    }


    @Test
    void mindrayBc30ShouldReadAndParse() throws Exception {
        byte[] data = HexFormat.of().parseHex(getTextFromFile("miura multi hex.txt", false, false).replaceAll("[^0-9A-Fa-f]", ""));
        int port = 5100;
        var parser = new MIURA();

        var communicatorParams = CommunicatorSettings.createTcpHostParams(port);
        var settings = new DeviceSettings()
                .setParser(parser)
                .setCommunicatorSettings(communicatorParams);

        var logger = new DeviceLogger(settings.getLogFileName(),settings.getCachedParser().getCharset());
        var communicator = new TcpHostCommunicator(port, logger);
        List<ParsingResult> results = new ArrayList<>();
        List<ProcessorResult> processorResults = new ArrayList<>();
        CountDownLatch parsed = new CountDownLatch(1);
        CountDownLatch handled = new CountDownLatch(1);
        URL resourceUrl = DeviceTest.class.getClassLoader().getResource("wordTemplate.docx");

        assertNotNull(resourceUrl);
        var processorParams = new FileResultProcessorSettings()
                .setTemplateFile(new File(resourceUrl.toURI()))
                .setOutputPath(Paths.get(resourceUrl.toURI()).getParent())
                ;
        var devSettings = new DeviceSettings()
                .setActions(List.of(FileResultActions.SAVE_DOCX))
                .setFileResultProcessorSettings(processorParams);

        // ✅ Створюємо DeviceConfigModel
        var deviceConfig = new DeviceConfig()
                .setCommunicator(communicator)
                .setParser(parser)
                .setLogger(logger)
                .setDeviceSettings(devSettings);

        var device = Device.createAndStart(deviceConfig);

        parser.addResponseListener(r -> {
            results.add(r);
            parsed.countDown();
        });
        device.device().addProcessListener(r -> {
            processorResults.add(r);
            handled.countDown();
        });
        device.device().awaitStarted(5, TimeUnit.SECONDS);
        IPortSender.create("127.0.0.1", port).send(data);
        Thread.sleep(1000);
        boolean parserAwait = parsed.await(2, TimeUnit.SECONDS);
        boolean handledAwait = handled.await(500, TimeUnit.SECONDS);
        ProcessorResult result = processorResults.get(0);
        Desktop.getDesktop().open(result.getFile().get());
        ConsoleUtil.printMagenta(result.getFile().get().getAbsolutePath());
        assertTrue(parserAwait, "парсинг не завершился");
        assertTrue(handledAwait, "обработка результата не завершилась");
        assertEquals(1, results.size(), "Ожидаем один ParsingResult");
        assertEquals(1, processorResults.size(), "Ожидаем один ProcessorResult");

        // Закрываем устройство
        device.device().close();
        try {
            device.future().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void shouldReadDataParseAndGenerateResultDbf() throws Exception {
        int port = 5500;
        var parser = new DymindDF50Vet();
        var communicatorParams = CommunicatorSettings.createTcpHostParams(port);
        var settings = new DeviceSettings()
                .setParser(parser)
                .setCommunicatorSettings(communicatorParams);
        var logger = new DeviceLogger(settings.getLogFileName(),settings.getCachedParser().getCharset());
        var communicator = parser.createDefaultCommunicator(communicatorParams, logger);

        List<ParsingResult> results = new ArrayList<>();
        List<ProcessorResult> processorResults = new ArrayList<>();
        CountDownLatch parsed = new CountDownLatch(1);
        CountDownLatch handled = new CountDownLatch(1);
        var devSettings = new DeviceSettings()
                .setActions(List.of(FileResultActions.CREATE_DBF_FILE));

        // ✅ Створюємо DeviceConfigModel
        var deviceConfig = new DeviceConfig()
                .setCommunicator(communicator)
                .setParser(parser)
                .setLogger(logger)
                .setDeviceSettings(devSettings);

        var device = Device.createAndStart(deviceConfig);

        parser.addResponseListener(r -> {
            results.add(r);
            parsed.countDown();
        });
        device.device().addProcessListener(r -> {
            processorResults.add(r);
            handled.countDown();
        });

        // --- отправляем данные ---
        byte[] data = HexFormat.of().parseHex(getTextFromFile("dymindDF50 hex.txt", false, true).replaceAll("[^0-9A-Fa-f]", ""));

        device.device().awaitStarted(5, TimeUnit.SECONDS);
        IPortSender.create("127.0.0.1", port).send(data);

        boolean parserAwait = parsed.await(2, TimeUnit.SECONDS);
        boolean handledAwait = handled.await(10, TimeUnit.SECONDS);

        assertTrue(parserAwait, "парсинг не завершился");
        assertTrue(handledAwait, "обработка результата не завершилась");
        assertEquals(1, results.size(), "Ожидаем один ParsingResult");
        assertEquals(1, processorResults.size(), "Ожидаем один ProcessorResult");

        var processorResult = processorResults.getFirst();
        var res = results.getFirst();

        // Проверяем parsing result
        assertNotNull(res.data(), "data не должна быть null");
        assertTrue(res.ack().length > 0, "ACK должен быть сгенерирован");
        assertEquals(25, res.data().getData().size());
        assertEquals(7, res.data().getImages().size());
        assertEquals("134894", res.data().getId());
        assertEquals("2025-05-25T10:35:20", res.data().getDate());

        // ✅ Работаем с Optional<File> безопасно
        assertTrue(processorResult.getFile().isPresent(), "Файл должен быть создан");

        var resultFile = processorResult.getFile().get();
        assertTrue(resultFile.exists(), "Файл должен существовать");

        var directoryFiles = resultFile.getParentFile().listFiles();
        assertNotNull(directoryFiles, "Директория должна существовать");
        assertEquals(8, directoryFiles.length, "Ожидаем 8 файлов в папке output");

        var dbfFiles = Arrays.stream(directoryFiles)
                .filter(File::isFile)
                .filter(f -> f.getName().toLowerCase().endsWith(".dbf"))
                .toList();

        var pngFiles = Arrays.stream(directoryFiles)
                .filter(File::isFile)
                .filter(f -> f.getName().toLowerCase().endsWith(".png"))
                .toList();

        assertEquals(1, dbfFiles.size(), "Ожидаем 1 файл .dbf");
        assertEquals(7, pngFiles.size(), "Ожидаем 7 файлов .png");

        // Вывод информации
        ConsoleUtil.printBlue(new String(res.ack(), StandardCharsets.UTF_8).replace("\r", "\n"));
        ConsoleUtil.printGreen(res.data().getId().get());
        ConsoleUtil.printGreen(res.data().getDate().get());
        res.data().getData().forEach((key, value) -> {
            ConsoleUtil.printYellow(key + " : " + value);
        });

        // Завершаем работу устройства
        device.device().close();
        try {
            device.future().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTextFromFile(String name, boolean appendNewLine, boolean show) {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            if (in == null) throw new IllegalStateException("Resource not found: " + name);

            try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                    if (appendNewLine) sb.append('\n');
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (appendNewLine && !sb.isEmpty()) sb.setLength(sb.length() - 1);
        if (show) ConsoleUtil.printYellow(sb.toString());
        return sb.toString();
    }
}