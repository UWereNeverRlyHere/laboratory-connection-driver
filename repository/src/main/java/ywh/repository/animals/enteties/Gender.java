package ywh.repository.animals.enteties;

public enum Gender {
    MALE("Самець", "Male"),
    FEMALE("Самка", "Female"),
    ANY("Будь-який", "Any");

    private final String uaName;
    private final String enName;

    Gender(String uaName, String enName) {
        this.uaName = uaName;
        this.enName = enName;
    }

    public String getUaName() {
        return uaName;
    }

    public String getEnName() {
        return enName;
    }
}
