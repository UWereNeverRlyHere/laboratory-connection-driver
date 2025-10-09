package ywh.labs.analysis;
/* 2
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import ywh.repository.entities.analysis.Indicator;
import ywh.repository.entities.analysis.ReferenceRange;
import ywh.repository.impl.repos.analysis.JsonIndicatorRepositoryImpl;
import ywh.repository.enteties.animals.AnimalType;
import ywh.repository.repo_exceptions.LoadException;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonIndicatorRepositoryImplTest {

   private JsonIndicatorRepositoryImpl repository;
    private String testFilePath;

    @BeforeEach
    void setUp(TestInfo testInfo) throws LoadException {
        // Створюємо унікальну назву файлу з датою та часом
        if (testInfo.getDisplayName().equals("shouldLoadFromAnimalsJsonRepo()")) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String testFileName = "test_indicators_" + timestamp;

        // Отримуємо шлях до папки test/resources
        String resourcesPath = System.getProperty("user.dir") + "/src/test/resources";
        File resourcesDir = new File(resourcesPath);

        // Створюємо папку resources, якщо її немає
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }

        testFilePath = new File(resourcesDir, testFileName).getAbsolutePath();
        System.out.println("Створено тестовий файл: " + testFilePath);

        repository = new JsonIndicatorRepositoryImpl(Path.of(testFilePath));
    }

    @Test
    void shouldAddReadAndGet() {
        // Given - створюємо тестовий індикатор
        Indicator indicator = new Indicator();
        indicator.setName("Гемоглобін");
        indicator.setCode("HGB");
        indicator.setPrintName("Hemoglobin");

        // Додаємо референсний діапазон для кота
        ReferenceRange catRange = new ReferenceRange(AnimalType.CAT, 8.0, 15.0);
        indicator.addReferenceRange(AnimalType.CAT, catRange);

        // When - додаємо індикатор
        boolean addResult = repository.add(indicator);

        // Then - перевіряємо, що додавання пройшло успішно
        assertTrue(addResult, "Індикатор повинен бути успішно доданий");

        // Перевіряємо, що індикатор існує
        assertTrue(repository.exists("HGB"), "Індикатор повинен існувати в репозиторії");

        // Знаходимо індикатор по коду
        Optional<Indicator> foundByCode = repository.findByCode("HGB");
        assertTrue(foundByCode.isPresent(), "Індикатор повинен бути знайдений по коду");

        // Перевіряємо дані знайденого індикатора
        Indicator found = foundByCode.get();
        assertEquals("Гемоглобін", found.getName(), "Назва індикатора повинна співпадати");
        assertEquals("HGB", found.getCode(), "Код індикатора повинен співпадати");
        assertEquals("Hemoglobin", found.getPrintName(), "Назва для друку повинна співпадати");

        // Перевіряємо референсний діапазон
        assertTrue(found.hasReferenceRange(AnimalType.CAT), "Повинен мати референсний діапазон для кота");
        Optional<ReferenceRange> range = found.getReferenceRange(AnimalType.CAT);
        assertTrue(range.isPresent(), "Референсний діапазон повинен бути присутнім");
        assertEquals(8.0, range.get().getMin(), "Мінімальне значення повинно співпадати");
        assertEquals(15.0, range.get().getMax(), "Максимальне значення повинно співпадати");

        // Знаходимо індикатор по назві
        Optional<Indicator> foundByName = repository.findByName("Гемоглобін");
        assertTrue(foundByName.isPresent(), "Індикатор повинен бути знайдений по назві");
        assertEquals("HGB", foundByName.get().getCode(), "Код знайденого індикатора повинен співпадати");

        // Перевіряємо загальну кількість
        assertEquals(1, repository.findAll().size(), "В репозиторії повинен бути один індикатор");

        // Зберігаємо дані перед завершенням тесту
        try {
            repository.saveData();
            System.out.println("Дані збережено в файл: " + testFilePath);
        } catch (Exception e) {
            System.err.println("Помилка збереження даних: " + e.getMessage());
        }
    }

    @Test
    void shouldNotAddDuplicateCodesAndThrowException() {
        // Given - створюємо два індикатори з однаковими кодами
        Indicator indicator1 = new Indicator();
        indicator1.setName("Гемоглобін");
        indicator1.setCode("HGB");
        indicator1.setPrintName("Hemoglobin");

        Indicator indicator2 = new Indicator();
        indicator2.setName("Гематокрит");
        indicator2.setCode("HGB"); // Той самий код!
        indicator2.setPrintName("Hematocrit");

        // When - додаємо перший індикатор
        boolean firstAddResult = repository.add(indicator1);

        // Then - перший індикатор додається успішно
        assertTrue(firstAddResult, "Перший індикатор повинен бути успішно доданий");
        assertTrue(repository.exists("HGB"), "Індикатор повинен існувати в репозиторії");

        // When - намагаємося додати другий індикатор з тим самим кодом
        // Then - повинно виникнути виключення
        assertThrows(IllegalArgumentException.class, () -> {
            repository.add(indicator2);
        }, "Додавання індикатора з дублікатом коду повинно викинути IllegalArgumentException");

        // Перевіряємо, що в репозиторії залишився тільки перший індикатор
        assertEquals(1, repository.findAll().size(), "В репозиторії повинен залишитися один індикатор");

        Optional<Indicator> found = repository.findByCode("HGB");
        assertTrue(found.isPresent(), "Індикатор повинен бути знайдений");
        assertEquals("Гемоглобін", found.get().getName(), "Повинен залишитися перший індикатор");

        // Зберігаємо дані перед завершенням тесту
        try {
            repository.saveData();
            System.out.println("Дані збережено в файл: " + testFilePath);
        } catch (Exception e) {
            System.err.println("Помилка збереження даних: " + e.getMessage());
        }
    }


    @Test
    void shouldValidateCodeUniquenessInSaveAll() {
        // Given - створюємо список індикаторів з дублікатами кодів
        Indicator indicator1 = new Indicator();
        indicator1.setName("Гемоглобін");
        indicator1.setCode("HGB");
        indicator1.setPrintName("Hemoglobin");

        Indicator indicator2 = new Indicator();
        indicator2.setName("Гематокрит");
        indicator2.setCode("HGB"); // Дублікат коду
        indicator2.setPrintName("Hematocrit");

        // When & Then - повинно виникнути виключення при збереженні списку з дублікатами
        assertThrows(IllegalArgumentException.class, () -> {
            repository.saveAll(java.util.Arrays.asList(indicator1, indicator2));
        }, "Збереження списку з дублікатами кодів повинно викинути IllegalArgumentException");

        // Перевіряємо, що репозиторій залишився порожнім
        assertTrue(repository.findAll().isEmpty(), "Репозиторій повинен залишитися порожнім після невдалого збереження");

        // Зберігаємо дані перед завершенням тесту (навіть якщо порожній репозиторій)
        try {
            repository.saveData();
            System.out.println("Дані збережено в файл: " + testFilePath);
        } catch (Exception e) {
            System.err.println("Помилка збереження даних: " + e.getMessage());
        }
    }

    @Test
    void shouldCreateMultipleIndicatorsWithComplexData() {
        // Given - створюємо кілька індикаторів з різними референсними діапазонами
        Indicator hgb = new Indicator();
        hgb.setName("Гемоглобін");
        hgb.setCode("HGB");
        hgb.setPrintName("Hemoglobin");
        hgb.addReferenceRange(AnimalType.CAT, new ReferenceRange(AnimalType.CAT, 8.0, 15.0, "г/дл"));
        hgb.addReferenceRange(AnimalType.DOG, new ReferenceRange(AnimalType.DOG, 10.0, 18.0, "г/дл"));

        Indicator hct = new Indicator();
        hct.setName("Гематокрит");
        hct.setCode("HCT");
        hct.setPrintName("Hematocrit");
        hct.addReferenceRange(AnimalType.CAT, new ReferenceRange(AnimalType.CAT, 24.0, 45.0, "%"));
        hct.addReferenceRange(AnimalType.DOG, new ReferenceRange(AnimalType.DOG, 37.0, 55.0, "%"));

        Indicator wbc = new Indicator();
        wbc.setName("Лейкоцити");
        wbc.setCode("WBC");
        wbc.setPrintName("White Blood Cells");
        wbc.addReferenceRange(AnimalType.CAT, new ReferenceRange(AnimalType.CAT, 5.5, 19.5, "x10^9/L"));
        wbc.addReferenceRange(AnimalType.DOG, new ReferenceRange(AnimalType.DOG, 6.0, 17.0, "x10^9/L"));

        // When - додаємо всі індикатори
        assertTrue(repository.add(hgb), "HGB повинен бути доданий");
        assertTrue(repository.add(hct), "HCT повинен бути доданий");
        assertTrue(repository.add(wbc), "WBC повинен бути доданий");

        // Then - перевіряємо, що всі індикатори збережені
        assertEquals(3, repository.findAll().size(), "Повинно бути 3 індикатори");

        // Перевіряємо пошук за типом тварини
        assertEquals(3, repository.findByAnimalType(AnimalType.CAT).size(),
                "Повинно бути 3 індикатори для котів");
        assertEquals(3, repository.findByAnimalType(AnimalType.DOG).size(),
                "Повинно бути 3 індикатори для собак");

        // Перевіряємо конкретні референсні діапазони
        Optional<Indicator> foundHgb = repository.findByCode("HGB");
        assertTrue(foundHgb.isPresent(), "HGB повинен бути знайдений");
        assertTrue(foundHgb.get().hasReferenceRange(AnimalType.CAT), "HGB повинен мати діапазон для котів");
        assertTrue(foundHgb.get().hasReferenceRange(AnimalType.DOG), "HGB повинен мати діапазон для собак");

        // Зберігаємо дані перед завершенням тесту
        try {
            repository.saveData();
            System.out.println("Комплексні дані збережено в файл: " + testFilePath);
        } catch (Exception e) {
            System.err.println("Помилка збереження даних: " + e.getMessage());
        }
    }

    @Test
    void shouldLoadFromAnimalsJsonRepo() throws LoadException {
        String resourcesPath = System.getProperty("user.dir") + "/src/test/resources/jsonRepository";
        JsonIndicatorRepositoryImpl localRepository = new JsonIndicatorRepositoryImpl(Path.of(resourcesPath));
        localRepository.reload();
        Map<String, Indicator> cache = localRepository.getCache();
        assertNotEquals(0, cache.size());
    }

}*/