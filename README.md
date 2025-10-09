# laboratory-connection-driver
Додаток для інтеграції з різноманітними медичними аналізаторами

# Commons
Проект утилітними методами для спільного використання у проектах

# Services
Проект для реалізації сервісів для роботи з медичними аналізаторами. Містить всі процесори результатів

# Repository
Проект з репозиторіями даних. Наразі містить показники та норми для тварин 

# connection_driver_application
Проект для запуску додатку, саме UI частина. Залежить від сервісів та репозиторію...

# dicx2odf-converter
Проект для конвертації файлів з формату doc в pdf
Довелося винести в окремий jar, оскільки бібліотека не підтримує модульність

# Запуск через gradle
gradlew.bat run
:connection_driver_application:run --stacktrace

# Збірка exe
:connection_driver_application:clean :connection_driver_application:jpackage

