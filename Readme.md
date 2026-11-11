# Vuln Tracker

Внутренний инструмент для учёта уязвимостей артефактов проекта: вместо разрастающейся CSV-таблицы — веб-приложение с фильтрами, сортировкой, пагинацией и быстрым триажем.

## Стек

| Слой | Технологии |
|---|---|
| Backend | Kotlin 2.0, Spring Boot 3.3, Spring Data JPA, Flyway, PostgreSQL 15, OpenAPI (springdoc) |
| Frontend | React 18 (JavaScript, без TypeScript), Vite, React Router, `fetch` (без axios/TS/TanStack) |
| Инфра | Docker Compose (Postgres + backend + frontend), Testcontainers (интеграционные тесты) |
| Сборка | Gradle Kotlin DSL (backend), npm (frontend) |

Принцип фронтенда: **как можно проще**. Команда — бэкендеры, сложный фронт поддерживать некому. Никакого TS, минимум зависимостей, только `useState`/`useEffect` и `fetch`.


## Модель данных

```
artifact (id, name, ecosystem[MAVEN|NPM], created_at)
    ▲
    │ artifact_id (FK)
ticket (id, jira_url, artifact_id, deadline, actual BOOLEAN NULL, comment, created_at, updated_at)
    │
    │ ticket_vulnerability (ticket_id, vulnerability_id)   ← M:N
    ▼
vulnerability (id, cve_id UNIQUE, description NULL, created_at)
```

Ключевые решения:
- **`actual` — `Boolean?` на `ticket` целиком.** `null` = «не триажено», `true` = актуальна, `false` = не актуальна. Комментарий — свободный текст с обоснованием.
- **CVE — отдельная таблица, M:N с тикетом.** Один артефакт → много тикетов → много CVE. Не храним CVE строкой через запятую.
- **`description` у `vulnerability`** — пока пустое, заполняется постепенно (страница `/vulnerabilities`, `PATCH /api/vulnerabilities/{id}/description`).
- **`jira_url` НЕ уникален.** Один Jira-тикет может относиться к нескольким артефактам (в CSV такие строки размножаются в отдельные `ticket`).

## Быстрый старт (Docker Compose)

```bash
docker compose up --build
```
- Frontend: http://localhost:5173
- Backend (Swagger UI): http://localhost:8080/swagger-ui.html
- Postgres: `localhost:5432`, db/user/pass = `vuln`/`vuln`/`vuln`

Flyway применит миграции автоматически при старте backend.

## Локальная разработка (без Docker)

**Требования:** JDK 21, Node.js 20.19+ (или 22.12+), PostgreSQL 15 (можно только его поднять через `docker compose up postgres`).

```bash
# 1. Postgres
docker compose up -d postgres

# 2. Backend (из backend/)
./gradlew bootRun
# → http://localhost:8080, Swagger: /swagger-ui.html

# 3. Frontend (из frontend/)
npm install
npm run dev
# → http://localhost:5173 (Vite проксирует /api на :8080)
```

## Импорт CSV

Импорт — **одноразовый CLI-раннер** под профилем `import` (не публичный endpoint). Логика нормализации: несколько CVE в ячейке (через запятую и/или пробел), юникод-дефисы в CVE (`CVE‐2025‐14813` → `CVE-2025-14813`), `actual` `0/1/-/пусто` → `false/true/null`, дедлайны `dd.MM.yyyy` и `dd.MM` (год = текущий), отрезание `:version` от имени артефакта, размножение ячеек с несколькими артефактами (`helpers,runtime`, `tomcat-coyote / tomcat-catalina`) в отдельные тикеты.

```bash
# локально
./gradlew bootRun --args='--spring.profiles.active=import --import.file=../data.csv'

# docker
docker compose --profile import run --rm importer
```

Идемпотентность: `existsByJiraUrlAndArtifactId` — повторный импорт не создаёт дубли, но **не обновляет** существующие поля (actual/comment/deadline). Для обновления — upsert (не реализован).

## API

Полная спецификация — Swagger UI (`/swagger-ui.html`) или `/v3/api-docs`.

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/api/tickets?actual&ecosystem&artifact&cve&page&size&sort&dir` | Список с фильтрами, пагинацией, сортировкой (`sort` = `createdAt`\|`deadline`\|`actual`, `dir` = `asc`\|`desc`; `NULL` всегда в конце) |
| GET | `/api/tickets/{id}` | Один тикет |
| POST | `/api/tickets` | Создать (CVE из `cveIds` создаются автоматически, если нет) |
| PUT | `/api/tickets/{id}` | Обновить |
| PATCH | `/api/tickets/{id}/actual` | Быстрый триаж (`{ "actual": true\|false\|null }`) |
| DELETE | `/api/tickets/{id}` | Удалить |
| GET | `/api/artifacts?q&ecosystem` | Список/поиск артефактов |
| POST | `/api/artifacts` | Создать артефакт |
| PUT/DELETE | `/api/artifacts/{id}` | Обновить/удалить (удаление запрещено, если есть тикеты) |
| GET | `/api/vulnerabilities?q` | Список/поиск CVE |
| PATCH | `/api/vulnerabilities/{id}/description` | Заполнить описание |

Ошибки — Spring `ProblemDetail` (`application/problem+json`): 400 (валидация/битый JSON/дубль), 404 (не найдено), 500.

## Соглашения разработки

### Backend
- Контроллеры **тонкие**: только приём параметров + вызов сервиса. Вся логика и маппинг в DTO — в `service/`, внутри `@Transactional`. Это критично: `open-in-view: false`, поэтому маппинг сущностей в DTO **обязан** быть внутри транзакции, иначе `LazyInitializationException`.
- Ленивые коллекции (`Ticket.vulnerabilities`, `Ticket.artifact`) подгружаются через `@EntityGraph(attributePaths = [...])` на методах репозитория (`findAll(Specification, Pageable)`, `findById`). Не включай `EAGER` и не возвращай `open-in-view: true`.
- Новые поля/таблицы — только через Flyway-миграции (`V{N}__description.sql`), `hibernate.ddl-auto: validate`.
- DTO — `data class` в `web/dto/`, мапперы — extension-функции (`Ticket.toResponse()`).
- Enum'ы в БД — `VARCHAR` + `@Enumerated(EnumType.STRING)` + `CHECK` в миграции.
- Тесты — интеграционные на Testcontainers (`AbstractIntegrationTest`), Postgres 15.

### Frontend
- **JavaScript, без TypeScript.** Не добавляй `.ts`/`.tsx`.
- Минимум зависимостей: `react`, `react-dom`, `react-router-dom`. Новые библиотеки — только после обсуждения.
- Все запросы — через `src/api.js` (единая точка, `fetch`, обработка `ProblemDetail`).
- Состояние — локальное (`useState`/`useEffect`). Никаких Redux/TanStack Query, пока не станет действительно больно.
- Стили — один `styles.css`, без CSS-in-JS и UI-китов.

## Как добавить новую фичу (чек-лист для ИИ-агента)

1. **Новое поле в сущности** → Flyway-миграция `V{N}__add_xxx.sql` → поле в `data/model/*.kt` → поле в `web/dto/*Request/*Response` → маппинг в `TicketMapper.kt` → поле в форме `TicketForm.jsx` и колонке `TicketList.jsx`.
2. **Новый фильтр** → параметр в `TicketController.list` → учёт в `TicketService.list` (Specification) → поле в `filters` в `TicketList.jsx` + прокидывание в `api.listTickets`.
3. **Новый эндпоинт** → метод в контроллере (тонкий) → метод в сервисе с `@Transactional` → DTO → метод в `api.js` → использование на странице.
4. **Сортировка по новому полю** → ветка в `when (sort)` в `TicketController` (с `.nullsLast()` для nullable-полей) → заголовок с `toggleSort` в `TicketList.jsx`.

## Известные ограничения / TODO

- Импорт не делает upsert (повторный прогон не обновляет existing-тикеты).
- Нет страницы `/vulnerabilities` (только API для `description`).
- Нет аутентификации (внутренний инструмент в доверенном контуре).
- Дубли `jira_url` в списке визуально не группируются.
