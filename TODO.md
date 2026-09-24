# Что осталось: треки «Базы данных» и «Плагины для Minecraft»

Состояние на 25.09.2026.

## Уже сделано

- **Песочница.** Программам учеников доступны SQLite (sqlite4j), PostgreSQL (pgjdbc + PGlite), MongoDB (драйвер + mongo-java-server), Paper API и MockBukkit. Всё работает внутри программы, без сети.
  - Мосты к библиотекам лежат в `backend/src/bridge/java`.
  - Права прописаны в `Sandbox.policy()`.
  - Программы с PostgreSQL запускаются в тяжёлом профиле (`byte.runner.heavy-*`).
  - Тесты: `LibrariesRunTest` и новые случаи в `SandboxSecurityTest`.
- **Новое требование `literal`.** Проверяет, что в строках кода есть фрагменты SQL, например `ON CONFLICT`.
- **Фильтр уроков в `LessonsContentTest`:** `mvn test -Dtest=LessonsContentTest -Dlessons=slug1,slug2`.
- **Уроки 38–41 готовы и проверены:**
  - 38 `sqlite-jdbc`
  - 39 `prepared-statements`
  - 40 `transactions-dao`
  - 41 `mongodb-basics`

## Уроки

### Трек «Базы данных» (module `Базы данных`)

- [ ] **42 `mongodb-aggregation`.** Файл `orders.jsonl` в рабочей папке, по строке на заказ: `customer`, `status`, `items[{name, category, price, qty}]`. Импорт через `Document.parse`, дальше отчёт агрегациями:
  - выручка по категориям (только `paid`): `$unwind` + `$group` + `$sum` от `$multiply`;
  - лучший покупатель;
  - топ-3 товаров по количеству;
  - число отменённых заказов.

  Скрытые тесты подставляют другие файлы через `files`. Требования: `call aggregate`, `group`, `unwind`. Всё это в mongo-java-server проверено и работает.
- [ ] **43 `postgres-upsert`.** Инвентарь игроков в PostgreSQL:
  - `pick` делает `INSERT ... ON CONFLICT (player, item) DO UPDATE ... RETURNING qty`;
  - `drop` уменьшает количество и удаляет строку при нуле;
  - `show` выводит инвентарь.

  В теории: `SERIAL` / `GENERATED AS IDENTITY`, `RETURNING`, upsert, чем PostgreSQL отличается от SQLite. Требования: `literal` `ON CONFLICT`, `RETURNING`.
- [ ] **44 `postgres-jsonb-window`.** Матчи из `matches.csv` загружаются через `addBatch`/`executeBatch`. В отчёте: `RANK() OVER`, лучший игрок на каждой карте через `ROW_NUMBER() OVER (PARTITION BY ...)`, фильтр по `meta->>'mode'` и `'ranked' = ANY(tags)`. Требования: `literal` `OVER (`, `PARTITION BY`, `->>`; `call executeBatch`.

Особенности PostgreSQL в песочнице:
- программа выполняется около 2–2,5 с, поэтому тестов лучше 5–6;
- `current_user` = postgres, `current_database()` = template1 — их не выводить.

### Трек «Плагины для Minecraft» (module `Плагины Minecraft`)

Общий подход:
- Ученик пишет плагин: класс `extends JavaPlugin` и `resources/plugin.yml`.
- `Main.java` в стартовом проекте — готовый сценарий на MockBukkit. Он читает строки из stdin и после каждой печатает сообщения игроков в виде `[Имя] текст`. Текст без цветов: `PlainTextComponentSerializer.plainText().serialize(player.nextComponentMessage())`.
- Команды сценария:
  - `join <имя>`, `quit <имя>`, `op <имя>`;
  - `cmd <имя> <команда>`, `chat <имя> <текст>`;
  - `break <имя> x y z` и `place <имя> x y z MATERIAL` — через `new PlayerSimulation(player)`;
  - `wait <тики>` — через `server.getScheduler().performTicks(n)`.
- Журнал MockBukkit идёт в stderr без времени: `[INFO] ...`. Тесты сравнивают только stdout.

Уроки:
- [ ] **45 `first-plugin`.** `onEnable`, `plugin.yml`, `Listener`, `PlayerJoinEvent` / `PlayerQuitEvent`: приветствие, объявление о первом входе, «Сейчас на сервере: N».
- [ ] **46 `plugin-commands`.** `CommandExecutor`, аргументы, проверка прав (op): `/msg <игрок> <текст>`, `/broadcast` только для op, сообщения об ошибках.
- [ ] **47 `plugin-events`.** Отмена `BlockBreakEvent` / `BlockPlaceEvent` для защиты спавна (радиус, op обходит защиту).
- [ ] **48 `plugin-config`.** `saveDefaultConfig`, `getConfig().getString` / `getStringList`, шаблоны сообщений. Для тестов: `MockBukkit.loadWithConfig(Plugin.class, new File("config.yml"))`, если файл есть в рабочей папке.
- [ ] **49 `plugin-scheduler`.** `BukkitRunnable`, `runTaskLater` / `runTaskTimer`, `cancel`: `/countdown N`, сообщения по тикам.
- [ ] **50 `plugin-economy`.** Итоговый проект: монеты за сломанные блоки (награды из конфига), `/balance`, `/pay`, `/baltop`.

## После уроков

- [ ] Счётчики уроков обновить до итогового числа (сейчас 41):
  - `ApiTest`, `AnalyticsTest`;
  - `e2e/tests/site.spec.ts`;
  - `frontend/src/pages/HomePage.tsx` (там ещё «37 уроков»);
  - `README.md`: список возможностей, отметить пункты плана.
- [ ] Фронтенд, `frontend/src/ide/javaCompletions.ts`:
  - добавить в `CLASSES` автоимпорт для `java.sql.*` (`Connection`, `DriverManager`, `PreparedStatement`, `ResultSet`, `SQLException`, `Statement`);
  - для MongoDB: `MongoClient`, `MongoClients`, `MongoCollection`, `Document`, `Filters`, `Updates`, `Sorts`, `Aggregates`, `Accumulators`, `FindOneAndUpdateOptions`, `ReturnDocument`;
  - для Bukkit и MockBukkit: `JavaPlugin`, `Listener`, `EventHandler`, `PlayerJoinEvent`, `CommandExecutor`, `Player`, `Bukkit`, `MockBukkit`, `ServerMock`, `PlayerMock`;
  - по желанию — методы в `javaMembers.ts`.
- [ ] Лендинг: секции новых треков (по образцу `ConfigShowcase.tsx`).
- [ ] README:
  - раздел про библиотеки и мосты `src/bridge`;
  - тяжёлый профиль: по умолчанию `max-concurrent-heavy-runs: 1`, куча 640 МБ; сервер в Docker должен выдерживать +~700 МБ на тяжёлую программу;
  - фильтр `-Dlessons`.
- [ ] Dockerfile: `mvn dependency:go-offline` теперь тянет репозиторий PaperMC. Проверить сборку образа.
- [ ] Сквозной тест урока с базой данных или плагином (`e2e/tests/`).
- [ ] Полный прогон: `mvn verify`, `npm run build`, e2e.
- [ ] Пуш на GitHub.
