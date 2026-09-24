До сих пор программы хранили данные в переменных, коллекциях и файлах. Это удобно, пока данных немного. Когда игроков тысячи, а нужно найти лучших, посчитать средний результат или выбрать тех, кто не заходил месяц, пригодится **база данных**. В этом треке вы поработаете с тремя: SQLite, MongoDB и PostgreSQL.

Начнём с **SQLite**: вся база — это один файл, а сам движок встроен прямо в программу. SQLite работает в каждом телефоне, в браузерах и во многих играх.

## Таблицы и SQL

Данные в SQLite лежат в **таблицах**: у каждой таблицы есть столбцы с типами, а каждая строка — одна запись. С базой говорят на языке **SQL**:

```sql
CREATE TABLE results (name TEXT, score INTEGER);

INSERT INTO results (name, score) VALUES ('Steve', 95);

SELECT name, score FROM results WHERE score > 50 ORDER BY score DESC LIMIT 3;
```

- `CREATE TABLE` создаёт таблицу. Основные типы SQLite: `INTEGER`, `REAL` (дробные), `TEXT`.
- `INSERT` добавляет строку.
- `SELECT` выбирает данные: `WHERE` фильтрует, `ORDER BY` сортирует (`DESC` — по убыванию), `LIMIT` оставляет первые строки.

Строки в SQL пишутся в одинарных кавычках, а ключевые слова можно писать любыми буквами: `select` и `SELECT` — одно и то же.

```quiz
? Какой запрос выберет трёх игроков с наибольшим счётом?
- SELECT name FROM results LIMIT 3 ORDER BY score DESC
+ SELECT name FROM results ORDER BY score DESC LIMIT 3
- SELECT name FROM results ORDER BY score LIMIT 3
- SELECT TOP 3 name FROM results
> Сначала сортировка по убыванию (`DESC`), потом `LIMIT` берёт первые три строки. Порядок частей запроса строгий: `LIMIT` всегда в конце. Без `DESC` сортировка идёт по возрастанию, и мы получили бы худших.
```

## Подключаемся через JDBC

В Java с базами данных работают через **JDBC** — стандартный набор интерфейсов из пакета `java.sql`. Для каждой базы есть свой драйвер, а код почти не меняется. Драйвер SQLite уже подключён.

```java
try (Connection connection = DriverManager.getConnection("jdbc:sqlite:game.db");
     Statement statement = connection.createStatement()) {
    statement.execute("CREATE TABLE IF NOT EXISTS results (name TEXT, score INTEGER)");
}
```

Строка `jdbc:sqlite:game.db` — адрес базы: драйвер `sqlite`, файл `game.db` в рабочей папке. Если файла нет, SQLite создаст его. После запуска `game.db` появится среди файлов проекта.

`Connection`, `Statement` и `ResultSet` держат ресурсы, поэтому их открывают в `try` с ресурсами — тогда они закроются сами, даже если случится ошибка.

`IF NOT EXISTS` не даст упасть, если таблица уже есть. А если нужна временная база, подойдёт адрес `jdbc:sqlite::memory:` — она живёт, пока открыто соединение.

## Читаем результат: ResultSet

Запрос, который возвращает строки, выполняют через `executeQuery`. Он возвращает `ResultSet` — курсор, который двигается по строкам результата:

```java
try (ResultSet rows = statement.executeQuery("SELECT name, score FROM results ORDER BY score DESC")) {
    while (rows.next()) {
        String name = rows.getString("name");
        int score = rows.getInt("score");
        System.out.println(name + ": " + score);
    }
}
```

Сначала курсор стоит *перед* первой строкой, и `next()` переводит его на следующую. Когда строки кончились, `next()` возвращает `false`. Значения берут по имени столбца или по номеру — **номера начинаются с 1**, а не с 0.

```quiz
? Что выведет программа?
    import java.sql.*;

    public class Main {
        public static void main(String[] args) throws Exception {
            try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
                 Statement s = c.createStatement()) {
                s.execute("CREATE TABLE results (name TEXT, score INTEGER)");
                s.execute("INSERT INTO results VALUES ('Steve', 40), ('Alex', 90), ('Notch', 70)");
                ResultSet rows = s.executeQuery("SELECT name FROM results WHERE score >= 50 ORDER BY name");
                while (rows.next()) {
                    System.out.println(rows.getString(1));
                }
            }
        }
    }
- Alex\nNotch\nSteve
+ Alex\nNotch
- Notch\nAlex
- Steve\nAlex\nNotch
> `WHERE score >= 50` отбрасывает Steve с 40 очками. `ORDER BY name` сортирует оставшихся по имени: Alex, затем Notch. `getString(1)` — первый столбец результата.
```

## Считаем в базе: агрегатные функции

SQL умеет не только выбирать строки, но и считать по ним: `COUNT(*)` — число строк, `MAX(score)` и `MIN(score)` — наибольшее и наименьшее, `SUM`, `AVG`. `COUNT(DISTINCT name)` считает разные значения.

Такой запрос возвращает ровно одну строку, поэтому `next()` вызывают один раз:

```java
ResultSet row = statement.executeQuery("SELECT COUNT(*), MAX(score) FROM results");
row.next();
int games = row.getInt(1);
```

А `GROUP BY` разбивает строки на группы и считает функцию для каждой отдельно. Вот лучший результат каждого игрока:

```sql
SELECT name, MAX(score) AS best FROM results GROUP BY name ORDER BY best DESC
```

`AS best` даёт столбцу имя, чтобы по нему можно было сортировать и читать `getInt("best")`.

```quiz
? Что выведет программа?
    import java.sql.*;

    public class Main {
        public static void main(String[] args) throws Exception {
            try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
                 Statement s = c.createStatement()) {
                s.execute("CREATE TABLE results (name TEXT, score INTEGER)");
                s.execute("INSERT INTO results VALUES ('Steve', 40), ('Alex', 90), ('Steve', 70)");
                ResultSet r = s.executeQuery("SELECT COUNT(*), COUNT(DISTINCT name), SUM(score) FROM results");
                r.next();
                System.out.println(r.getInt(1) + " " + r.getInt(2) + " " + r.getInt(3));
            }
        }
    }
- 3 3 200
+ 3 2 200
- 2 2 200
- 3 2 110
> Строк три, а разных имён два: Steve встречается дважды. Сумма всех очков — 40 + 90 + 70 = 200.
```

## Пустая таблица и NULL

Если таблица пуста, `COUNT(*)` вернёт 0, а `MAX(score)` — особое значение `NULL`: «значения нет». `getInt` превращает `NULL` в 0, и отличить «нет рекорда» от рекорда 0 уже нельзя. Поэтому сначала проверяют число строк или вызывают `rows.wasNull()` сразу после `getInt`.

```quiz
? Почему сортировать и обрезать список лучше в SQL, а не в Java?
- Java не умеет сортировать строки
+ База отдаёт только нужные строки и умеет сортировать быстро, не загружая всю таблицу в память программы
- Так короче, других причин нет
- `ResultSet` нельзя сохранить в список
> В настоящей базе могут быть миллионы строк. Если вытащить их все в Java и отсортировать там, программа потратит время и память. С `ORDER BY ... LIMIT 3` база сама найдёт три нужные строки и передаст только их.
```
