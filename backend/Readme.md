
Запуск проекта:
./gradlew bootRun

Запуск csv импортёра:
Вариант 1:
./gradlew bootRun --args='--spring.profiles.active=import --import.file=D:\data.csv'
Так советовала ИИшка, но не запустилось.

Вариант 2:
Выставить в параметрах Spring Boot'а профиль "import" и убрать костыльный хардкод пути

Swagger
http://localhost:8080/swagger-ui/index.html


TODO:
1. "open-in-view: true" - страшный костыль, чтобы не ловить LazyInitializationException, поправить. Нужен @EntityGraph в репозитории + маппинг внутри @Transactional



