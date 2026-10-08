# План дальнейшей работы

Состояние на 08.10.2026.

## Уже сделано

- **Песочница.** Ученические программы используют SQLite, PostgreSQL (PGlite), MongoDB, Paper API/MockBukkit и учебную симуляцию нужных callbacks Fabric. Сетевого доступа нет.
  - Мосты и симуляторы лежат в `backend/src/bridge/java` и доступны только в classpath дочерних программ.
  - Права MockBukkit ограничены `Sandbox.policy()`; программы с PostgreSQL запускаются в тяжёлом профиле.
- **12 уроков Bukkit/Paper:** номера 42–47 и 54–59. Темы: первый плагин, команды и разрешения, события и фильтры блоков, конфиги, планировщик, ограничение частоты команд, статистика и мини-квест.
- **12 уроков Fabric:** номера 48–53 и 60–65. Темы: entrypoints, события игроков, тики, блоки и награды, JSON-конфиг, клиентские клавиши.
  - Fabric-уроки запускаются на детерминированной учебной симуляции, а не на настоящем Minecraft client/server.
- **Фильтр уроков в `LessonsContentTest`:** `mvn test -Dtest=LessonsContentTest -Dlessons=slug1,slug2`.
- **Всего:** 65 уроков в 12 модулях.

## Следующие уроки: продвинутые базы данных

Трек «Базы данных» продолжается с урока 66.

- [ ] **66 `mongodb-aggregation`.** `orders.jsonl`: выручка по категориям (`$unwind` + `$group` + `$sum` от `$multiply`), лучший покупатель, топ-3 товара по количеству, число отменённых заказов. Скрытые тесты подменяют файл через `files`; требования: `aggregate`, `group`, `unwind`.
- [ ] **67 `postgres-upsert`.** Инвентарь игроков: `INSERT ... ON CONFLICT ... DO UPDATE ... RETURNING`, удаление предмета при нулевом количестве, просмотр инвентаря. Требования: `literal` `ON CONFLICT`, `RETURNING`.
- [ ] **68 `postgres-jsonb-window`.** Загрузка `matches.csv` через `addBatch`/`executeBatch`, отчёты `RANK() OVER`, `ROW_NUMBER() OVER (PARTITION BY ...)`, фильтр JSONB и массивов. Требования: `OVER (`, `PARTITION BY`, `->>`, `executeBatch`.

Особенности PostgreSQL в песочнице: запуск занимает около 2–2,5 с, поэтому достаточно 5–6 тестов; `current_user` и `current_database()` не выводить.

## Доработки и проверка

- [x] Добавить больше методов Bukkit/Fabric API в подсказки `frontend/src/ide/javaMembers.ts`; основные методы новых заданий теперь покрыты.
- [ ] Добавить сквозные проверки прохождения Bukkit- и Fabric-заданий в `e2e/tests/`.
- [ ] Полный прогон на окружении с JDK 21 и Maven: `cd backend && mvn verify`, `cd frontend && npm run build`, затем Playwright e2e.
- [ ] Проверить сборку Docker-образа после изменений моста.
