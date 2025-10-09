package ywh.repository.animals.enteties;

public class AgeRange {
    private final int minMonths;
    private final int maxMonths;

    public AgeRange(int minMonths, int maxMonths) {
        this.minMonths = minMonths;
        this.maxMonths = maxMonths;
    }

    public boolean contains(int ageInMonths) {
        return ageInMonths >= minMonths && ageInMonths <= maxMonths;
    }

    // Зручні статичні методи
    public static AgeRange puppy() { return new AgeRange(0, 12); }
    public static AgeRange adult() { return new AgeRange(12, 84); }
    public static AgeRange senior() { return new AgeRange(84, Integer.MAX_VALUE); }
}
