package ywh.services.data.models.observation;

import lombok.Getter;
import ywh.commons.DateTime;
import ywh.repository.animals.enteties.AnimalType;
import ywh.services.data.enums.ObservationKey;

import java.util.*;

public class ObservationData {
    @Getter
    private final Map<String, String> data = new HashMap<>();
    @Getter
    private final Map<String, ReferenceRangeResultModel> references = new HashMap<>();
    @Getter
    private final List<String> alerts = new ArrayList<>();
    @Getter
    private final Map<String, String> images = new LinkedHashMap<>();
    @Getter
    private AnimalType animalType = AnimalType.UNDEFINED;

    public ObservationData(ObservationData other) {
        this.data.putAll(other.data);
        this.references.putAll(other.references);
        this.alerts.addAll(other.alerts);
        this.images.putAll(other.images);
        this.animalType = other.animalType;
    }


    public ObservationData(String id) {
        putId(id);
    }

    public ObservationData() {
    }

    public Optional<String> getValue(String name) {
        String value = data.get(name);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public Optional<String> getValue(ObservationKey key) {
        return getValue(key.getName());
    }


    public void putAnimalType(String animal) {
        this.animalType = AnimalType.define(animal.trim());
        data.put(ObservationKey.ANIMAL_TYPE.getName(), animalType.getUaDefaultName());
    }

    public void putAnimalType(AnimalType animal) {
        this.animalType = animal;
        data.put(ObservationKey.ANIMAL_TYPE.getName(), animalType.getUaDefaultName());
    }

    public void putImage(String name, String value) {
        images.put(name, value);
    }

    public void put(String name, String value) {
        data.put(name, value.trim());
    }

    public void put(ObservationKey name, String value) {
        if (name == ObservationKey.ANIMAL_TYPE) {
            putAnimalType(value);
        } else
            data.put(name.getName(), value.trim());
    }


    public void putReference(String name, ReferenceRangeResultModel value) {
        references.put(name, value);
    }
    public Optional<ReferenceRangeResultModel> getReference(String name) {
        return Optional.ofNullable(references.get(name));
    }

    public void putAlert(String alert) {
        if (!alerts.contains(alert))
            alerts.add(alert);
    }

    public void putId(String id) {
        put(ObservationKey.ID, id);
    }

    public void putDate(String date) {
        put(ObservationKey.DATE, date);
        put(ObservationKey.PRINT_DATE, DateTime.toPattern("yyyy-MM-dd'T'HH:mm:ss", "dd.MM.yyyy HH:mm:ss", date));
    }

    public void putOwner(String owner) {
        put(ObservationKey.OWNER, owner);
    }

    public void putAnimalName(String animalName) {
        put(ObservationKey.ANIMAL_NAME, animalName);
    }

    public void putAge(String age) {
        put(ObservationKey.AGE, age);
    }

    public void putPhoneNumber(String phoneNumber) {
        put(ObservationKey.PHONE, phoneNumber);
    }

    public Optional<String> getId() {
        return getValue(ObservationKey.ID);
    }

    public Optional<String> getDate() {
        return getValue(ObservationKey.DATE);
    }

    public Optional<String> getOwner() {
        return getValue(ObservationKey.OWNER);
    }

    public Optional<String> getPhoneNumber() {
        return getValue(ObservationKey.PHONE);
    }

    public Optional<String> getPrintDate() {
        return getValue(ObservationKey.PRINT_DATE);
    }

    public Optional<String> getAnimalName() {
        return getValue(ObservationKey.ANIMAL_NAME);
    }

    public Optional<String> getAge() {
        return getValue(ObservationKey.AGE);
    }

    public Optional<String> getAnalyzerName() {
        return getValue(ObservationKey.ANALYZER);
    }
}
