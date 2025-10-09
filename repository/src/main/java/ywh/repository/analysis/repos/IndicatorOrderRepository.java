package ywh.repository.analysis.repos;

import ywh.repository.repo_exceptions.LoadException;
import ywh.repository.repo_exceptions.SaveException;


public interface IndicatorOrderRepository {

    // Отримання порядку для конкретного індикатора
    int getPlaceForIndicator(String indicatorCode);

    // Додавання/оновлення місця для індикатора
    void setPlaceForIndicator(int place,String indicatorCode);

    // Перевірка чи є порядок для типу тварини
    boolean hasOrderForCode( int place, String indicatorCode);

    // Системні методи
    void reload() throws LoadException;
    void saveData() throws SaveException;


}
