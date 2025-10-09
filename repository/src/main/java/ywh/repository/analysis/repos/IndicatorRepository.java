package ywh.repository.analysis.repos;

import ywh.repository.analysis.entities.Indicator;
import ywh.repository.animals.enteties.AnimalType;
import ywh.repository.repo_exceptions.LoadException;
import ywh.repository.repo_exceptions.SaveException;

import java.util.List;
import java.util.Optional;

public interface IndicatorRepository {
    List<Indicator> findAll();
    Optional<Indicator> findByCode(String code);
    Optional<Indicator> findByVariation(String code);
    Optional<Indicator> findByName(String name);
    List<Indicator> findByAnimalType(AnimalType animalType);
    public boolean isEmpty();
    // Методи з виключеннями
    boolean add(Indicator indicator) throws IllegalArgumentException;
    boolean update(Indicator indicator) throws IllegalArgumentException;

    void saveAll(List<Indicator> indicators) throws IllegalArgumentException;
    void delete(String code) throws IllegalArgumentException;

    boolean exists(String code);
    void reload() throws LoadException;

    // Мануальне збереження
    void saveData() throws SaveException;

}
