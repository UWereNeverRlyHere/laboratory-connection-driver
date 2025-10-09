package ywh.repository.animals.enteties;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

public enum AnimalType {
    CAT(Arrays.asList("Кішка", "Кіт", "Кот", "Кошка", "Cat"), "котів"),
    DOG(Arrays.asList("Собака", "Пес", "Собака", "Пес", "Dog"),"собак"),
    HORSE(Arrays.asList("Кінь", "Кобила", "Лошадь", "Конь", "Horse"), "коней"),
    MONKEY(Arrays.asList("Мавпа", "Мавпи", "Обезьяна", "Примат", "Monkey"), "мавп"),
    RABBIT(Arrays.asList("Кролик", "Кріль", "Кролик", "Кроль", "Rabbit"), "кролів"),
    RODENT(Arrays.asList("Гризуни", "Гризун", "Грызуны", "Грызун","Крыса", "Rodent"), "гризунів"),
    BIRD(Arrays.asList("Пташка", "Птах", "Птица", "Птичка", "Bird"), "птахів"),
    OTHER(Arrays.asList("Інше", "Різне", "Другое", "Прочее", "Other"), "інших"),
    UNDEFINED(Arrays.asList("Не визначено", "Не відомо", "Неизвестно", "Undefined"), "не визначених")
    ;
    private final List<String> variables;
    @Getter
    private final String animalNormName;

    AnimalType(List<String> variables, String animalNormName) {
        this.variables = variables;
        this.animalNormName = animalNormName;
    }

    public static AnimalType define(String name) {
        return Arrays.stream(AnimalType.values())
                .filter(animalType -> animalType.variables.stream()
                        .map(String::toLowerCase)
                        .anyMatch(animal -> animal.equals(name.toLowerCase())))
                .findFirst()
                .orElse(OTHER);
    }

    public String getUaDefaultName() {
        return variables.getFirst();
    }

    public String getEnDefaultName() {
        return variables.getLast();
    }

    @Override
    public String toString() {
        return getUaDefaultName();
    }
}
