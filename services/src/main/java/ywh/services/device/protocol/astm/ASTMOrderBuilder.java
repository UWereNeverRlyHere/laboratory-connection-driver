package ywh.services.device.protocol.astm;
import lombok.Getter;
import ywh.services.data.enums.SpecialBytes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ywh.services.data.enums.SpecialBytes.*;

public class ASTMOrderBuilder {
    @Getter
    private final List<String> astmPreparedOrderParts = new ArrayList<>();
    private final StringBuilder fullFrame = new StringBuilder();
    private int fn = 1;
    private boolean byLineMode = false;
    private boolean skipCRafterETX = false; // Режим без CR перед ETX/ETB (для MIURA)
    private int maxFrameSize = 220; // Значення за замовчуванням

    private void initializeFrame() {
        fullFrame.setLength(0);
        fn = 1;
        fullFrame.append(STX).append(fn);
    }

    private void addFrame(SpecialBytes specialByte) {
        if (skipCRafterETX || specialByte == ETB) {
            // Режим MIURA - без CR перед символом
            fullFrame.append(specialByte);
        } else {
            // Стандартний режим - з CR перед символом
            fullFrame.append(CR).append(specialByte);
        }
        appendWithCheckSum();
    }

    private void appendWithCheckSum() {
        String checkSum = getResponseCheckSum(fullFrame.toString());
        fullFrame.append(checkSum).append(CR).append(LF);
        astmPreparedOrderParts.add(fullFrame.toString());
        fullFrame.setLength(0);

        if (fn + 1 >= 8) {
            fn = 0;
        } else {
            fn++;
        }
        fullFrame.append(STX).append(fn);
    }

    private void buildAndProcessFrame(String[] frame) {
        if (byLineMode) {
            buildAndAddFrameByLine(frame);
        } else {
            buildAndAddFrame(frame);
        }
    }

    private void buildAndAddFrameByLine(String[] frame) {
        for (int i = 0; i < frame.length; i++) {
            char[] frameChars = frame[i].toCharArray();
            for (char frameChar : frameChars) {
                fullFrame.append(frameChar);
                if (fullFrame.length() >= maxFrameSize) {
                    addFrame(ETB);
                }
            }
            if (i < frame.length - 1) {
                fullFrame.append("|");
            }
        }
        addFrame(ETX);
    }

    private void buildAndAddFrame(String[] frame) {
        for (int i = 0; i < frame.length; i++) {
            fullFrame.append(frame[i]);
            if (i < frame.length - 1) {
                fullFrame.append("|");
                if (fullFrame.length() >= maxFrameSize) {
                    fullFrame.append(CR).append(ETB);
                    appendWithCheckSum();
                }
            }
        }
        fullFrame.append(CR);
    }

    private static String getResponseCheckSum(String response) {
        byte[] check = response.getBytes();
        int checksum = IntStream.range(1, check.length).map(i -> check[i]).sum();
        checksum = checksum % 256;
        return String.format("%02X", checksum);
    }

    private static String[] getEmptyArray(int size) {
        String[] array = new String[size];
        Arrays.fill(array, "");
        return array;
    }

    // Публічні методи білдера
    public ASTMOrderBuilder() {
        initializeFrame();
    }

    /**
     * Встановлює режим побайтового будування фреймів
     */
    public ASTMOrderBuilder byLine() {
        this.byLineMode = true;
        return this;
    }

    /**
     * Встановлює режим без CR перед ETX/ETB (для MIURA)
     */
    public ASTMOrderBuilder skipCRafterETX() {
        this.skipCRafterETX = true;
        return this;
    }

    /**
     * Встановлює максимальний розмір фрейму (за замовчуванням 220)
     */
    public ASTMOrderBuilder setMaxFrameSize(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
        return this;
    }

    /**
     * Додає заголовковий фрейм (H)
     */
    public ASTMOrderBuilder addHeader(String[] headerFrame) {
        buildAndProcessFrame(headerFrame);
        return this;
    }

    /**
     * Додає заголовковий фрейм (H) з використанням лямбда виразу
     * Спочатку парсер заповнює дані, потім автоматично додаються обов'язкові поля
     */
    public ASTMOrderBuilder addHeader(int arraySize, Consumer<String[]> headerConsumer) {
        String[] header = getEmptyArray(arraySize);
        headerConsumer.accept(header);  // Спочатку парсер заповнює що хоче
        header[0] = "H";                // Потім гарантуємо коректність
        header[1] = "\\^&";
        buildAndProcessFrame(header);
        return this;
    }

    /**
     * Додає фрейм пацієнта (P)
     */
    public ASTMOrderBuilder addPatient(String[] patientFrame) {
        buildAndProcessFrame(patientFrame);
        return this;
    }

    /**
     * Додає фрейм пацієнта (P) з використанням лямбда виразу
     * Спочатку парсер заповнює дані, потім автоматично додаються обов'язкові поля
     */
    public ASTMOrderBuilder addPatient(int arraySize, Consumer<String[]> patientConsumer) {
        String[] patient = getEmptyArray(arraySize);
        patientConsumer.accept(patient);  // Спочатку парсер заповнює що хоче
        patient[0] = "P";                 // Потім гарантуємо коректність
        buildAndProcessFrame(patient);
        return this;
    }

    /**
     * Додає фрейм замовлення (O)
     */
    public ASTMOrderBuilder addOrder(String[] orderFrame) {
        buildAndProcessFrame(orderFrame);
        return this;
    }

    /**
     * Додає фрейм замовлення (O) з використанням лямбда виразу
     * Спочатку парсер заповнює дані, потім автоматично додаються обов'язкові поля
     */
    public ASTMOrderBuilder addOrder(int arraySize, Consumer<String[]> orderConsumer) {
        String[] order = getEmptyArray(arraySize);
        orderConsumer.accept(order);  // Спочатку парсер заповнює що хоче
        order[0] = "O";               // Потім гарантуємо коректність
        buildAndProcessFrame(order);
        return this;
    }

    /**
     * Додає фрейм запиту (Q)
     */
    public ASTMOrderBuilder addQuery(String[] queryFrame) {
        buildAndProcessFrame(queryFrame);
        return this;
    }

    /**
     * Додає фрейм запиту (Q) з використанням лямбда виразу
     * Спочатку парсер заповнює дані, потім автоматично додаються обов'язкові поля
     */
    public ASTMOrderBuilder addQuery(int arraySize, Consumer<String[]> queryConsumer) {
        String[] query = getEmptyArray(arraySize);
        queryConsumer.accept(query);  // Спочатку парсер заповнює що хоче
        query[0] = "Q";               // Потім гарантуємо коректність
        buildAndProcessFrame(query);
        return this;
    }

    /**
     * Додає фрейм коментаря (C)
     */
    public ASTMOrderBuilder addComment(String[] commentFrame) {
        buildAndProcessFrame(commentFrame);
        return this;
    }

    /**
     * Додає фрейм коментаря (C) з використанням лямбда виразу
     * Спочатку парсер заповнює дані, потім автоматично додаються обов'язкові поля
     */
    public ASTMOrderBuilder addComment(int arraySize, Consumer<String[]> commentConsumer) {
        String[] comment = getEmptyArray(arraySize);
        commentConsumer.accept(comment);  // Спочатку парсер заповнює що хоче
        comment[0] = "C";                 // Потім гарантуємо коректність
        buildAndProcessFrame(comment);
        return this;
    }

    /**
     * Додає термінальний фрейм (L)
     */
    public ASTMOrderBuilder addTerminationRecord(String[] terminationFrame) {
        buildAndProcessFrame(terminationFrame);
        return this;
    }

    /**
     * Додає термінальний фрейм (L) з використанням лямбда виразу
     * Спочатку парсер заповнює дані, потім автоматично додаються обов'язкові поля
     */
    public ASTMOrderBuilder addTerminationRecord(int arraySize, Consumer<String[]> terminationConsumer) {
        String[] termination = getEmptyArray(arraySize);
        terminationConsumer.accept(termination);  // Спочатку парсер заповнює що хоче
        termination[0] = "L";                     // Потім гарантуємо коректність
        if (arraySize > 2) {
            // Додаємо дефолтні значення тільки якщо парсер їх не заповнив
            if (termination[1].isEmpty()) {
                termination[1] = "1";
            }
            if (termination[2].isEmpty()) {
                termination[2] = "N";
            }
        }
        buildAndProcessFrame(termination);
        return this;
    }

    /**
     * Завершує будування та повертає готову модель
     */
    public ASTMOrder buildWithEOT() {
        if (!byLineMode && !fullFrame.isEmpty()) {
            fullFrame.append(ETX);
            appendWithCheckSum();
            fn = 1;
            fullFrame.setLength(0);
        }
        astmPreparedOrderParts.add(EOT.toString());
   /*     for (int i = 0; i < astmPreparedOrderParts.size(); i++) {
            System.out.println("----------");
            System.out.println("part " + i);
            System.out.println(astmPreparedOrderParts.get(i).replace("\r", "\n"));
            System.out.println("----------");
        }*/
        return new ASTMOrder(astmPreparedOrderParts);
    }

    /**
     * Очищає білдер для повторного використання
     */
    public ASTMOrderBuilder clear() {
        astmPreparedOrderParts.clear();
        initializeFrame();
        return this;
    }

    /**
     * Повертає поточний список частин фрейму (для відладки)
     */
    public List<String> getCurrentParts() {
        return new ArrayList<>(astmPreparedOrderParts);
    }


    /**
     * Формує ASTM-рядок із переліку індикаторів.
     * @param indicators      список тестів / кодів
     * @param indicatorMapper перетворювач одного String у інший
     */
    public static String toAstmOrderString(List<String> indicators, UnaryOperator<String> indicatorMapper) {
        return indicators.stream()
                .map(indicatorMapper)
                .collect(Collectors.joining("\\"));
    }


}