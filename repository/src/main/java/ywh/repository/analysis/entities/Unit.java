package ywh.repository.analysis.entities;

import lombok.Getter;

@Getter
public enum Unit {

    MILLIGRAM("Milligram", "mg", "Міліграм", "мг"),
    MICROGRAM("Microgram", "µg", "Мікрограм", "мкг"),
    GRAM("Gram", "g", "Грам", "г"),
    MILLILITER("Milliliter", "ml", "Мілілітр", "мл"),
    MICROLITER("Microliter", "µl", "Мікролітр", "мкл"),
    LITER("Liter", "L", "Літр", "л"),
    MMOL_L("Millimole per liter", "mmol/L", "Мілімоль на літр", "ммоль/л"),
    UMOL_L("Micromole per liter", "µmol/L", "Мікромоль на літр", "мкмоль/л"),
    GRAM_PER_LITER("Gram per liter", "g/L", "Грам на літр", "г/л"),
    PERCENT("%", "%", "Відсоток", "%"),
    PERMILLE("Permille", "‰", "Проміле", "‰"),
    SECOND("Second", "sec", "Секунда", "с"),
    MINUTE("Minute", "min", "Хвилина", "хв"),
    HOUR("Hour", "h", "Година", "год"),
    UNIT("Unit", "U", "Одиниця", "од"),
    UNIT_PER_LITER("Unit/L", "U/L", "Одиниця/літр", "од/л"),
    IU("International Unit", "IU", "Міжнародна одиниця", "МО"),
    
    CELLS_10_9_PER_L("10⁹ cells per liter", "10⁹/L", "10⁹ клітин на літр", "10⁹/л"),
    CELLS_10_12_PER_L("10¹² cells per liter", "10¹²/L", "10¹² клітин на літр", "10¹²/л"),
    FEMTOLITER("Femtoliter", "fL", "Фемтолітр", "фл"),
    PICOGRAM("Picogram", "pg", "Пікограм", "пг"),
    MILLILITER_PER_LITER("Milliliter per liter", "mL/L", "Мілілітр на літр", "мл/л"),
    DIMENSIONLESS("Dimensionless", "", "Безрозмірна", "");

    private final String name;
    private final String shortName;
    private final String uaName;
    private final String uaShortName;

    Unit(String name, String shortName, String uaName, String uaShortName) {
        this.name = name;
        this.shortName = shortName;
        this.uaName = uaName;
        this.uaShortName = uaShortName;
    }
    
    /**
     * Знайти Unit за коротким ім'ям (з JSON)
     */
    public static Unit findByShortName(String shortName) {
        if (shortName == null || shortName.isEmpty()) {
            return DIMENSIONLESS;
        }
        
        for (Unit unit : values()) {
            if (unit.shortName.equals(shortName)) {
                return unit;
            }
        }
        
        // Якщо не знайдено, повертаємо null або кидаємо виключення
        throw new IllegalArgumentException("Невідома одиниця вимірювання: " + shortName);
    }
    
    /**
     * Перевірити, чи існує одиниця з таким коротким ім'ям
     */
    public static boolean existsByShortName(String shortName) {
        try {
            findByShortName(shortName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}