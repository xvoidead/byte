В прошлом уроке данные попадали в базу через `PreparedStatement`, и мы обещали разобраться, зачем он нужен. Сейчас узнаете: это главное правило работы с SQL, и нарушение этого правила — одна из самых частых уязвимостей в мире.

## Опасная склейка строк

Самый очевидный способ подставить значение в запрос — склеить строки:

```java
String sql = "SELECT price FROM items WHERE name = '" + name + "'";
ResultSet row = statement.executeQuery(sql);
```

Пока `name` — это `Меч`, всё работает. Но что если товар называется `Д'Артаньян`? Получится запрос `... WHERE name = 'Д'Артаньян'`: апостроф закрыл строку раньше времени, и база сообщит о синтаксической ошибке.

Хуже, если текст приходит от пользователя. Введите вместо имени `' OR '1'='1`, и запрос станет таким:

```sql
SELECT price FROM items WHERE name = '' OR '1'='1'
```

Условие истинно для каждой строки — пользователь увидит все товары. Это **SQL-инъекция**: данные стали частью кода запроса. Так злоумышленники читают чужие пароли и удаляют таблицы.

```quiz
? Какой запрос получится, если `name` равно `x'; DROP TABLE items; --`?
    String sql = "DELETE FROM items WHERE name = '" + name + "'";
+ DELETE FROM items WHERE name = 'x'; DROP TABLE items; --'
- DELETE FROM items WHERE name = 'x; DROP TABLE items; --'
- DELETE FROM items WHERE name = "x'; DROP TABLE items; --"
- Java не даст склеить такую строку
> Апостроф из имени закрыл строку, точка с запятой завершила первую команду, а дальше идёт вторая — удаление таблицы. `--` превращает остаток строки в комментарий, чтобы лишний апостроф не мешал.
```

## PreparedStatement

Правильный способ — **параметры**. В тексте запроса вместо значений ставят знаки `?`, а значения передают отдельно:

```java
try (PreparedStatement find = db.prepareStatement("SELECT price FROM items WHERE name = ?")) {
    find.setString(1, name);
    try (ResultSet row = find.executeQuery()) {
        if (row.next()) {
            System.out.println(row.getInt("price"));
        }
    }
}
```

База получает запрос и значения раздельно, поэтому значение никогда не станет кодом: `' OR '1'='1` будет просто странным названием товара. Кавычки вокруг `?` не нужны.

Параметры нумеруются с 1, для каждого типа свой метод: `setString`, `setInt`, `setDouble`, `setBoolean`. Один `PreparedStatement` можно выполнять много раз с разными значениями — база разберёт запрос только однажды.

```quiz
? Что выведет программа?
    import java.sql.*;

    public class Main {
        public static void main(String[] args) throws Exception {
            try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:")) {
                c.createStatement().execute("CREATE TABLE items (name TEXT, price INTEGER)");
                PreparedStatement add = c.prepareStatement("INSERT INTO items VALUES (?, ?)");
                add.setString(1, "Д'Артаньян");
                add.setInt(2, 300);
                add.executeUpdate();
                PreparedStatement find = c.prepareStatement("SELECT COUNT(*) FROM items WHERE name = ?");
                find.setString(1, "' OR '1'='1");
                ResultSet r = find.executeQuery();
                r.next();
                System.out.println(r.getInt(1));
            }
        }
    }
- 1
+ 0
- Ошибка синтаксиса SQL
- 300
> Параметр передаётся как значение, а не как часть запроса. Товара с названием `' OR '1'='1` нет, поэтому счётчик равен 0. Апостроф в «Д'Артаньян» тоже не помешал вставке.
```

## executeUpdate и число строк

Команды, которые меняют данные (`INSERT`, `UPDATE`, `DELETE`), выполняют через `executeUpdate`. Он возвращает число изменённых строк. Так легко понять, нашлось ли что удалять:

```java
PreparedStatement remove = db.prepareStatement("DELETE FROM items WHERE name = ?");
remove.setString(1, name);
if (remove.executeUpdate() == 0) {
    System.out.println("Не найден: " + name);
}
```

```quiz
? Что выведет программа?
    import java.sql.*;

    public class Main {
        public static void main(String[] args) throws Exception {
            try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:")) {
                Statement s = c.createStatement();
                s.execute("CREATE TABLE items (name TEXT, price INTEGER)");
                s.execute("INSERT INTO items VALUES ('Меч', 150), ('Щит', 90), ('Лук', 60)");
                PreparedStatement sale = c.prepareStatement("UPDATE items SET price = price / 2 WHERE price < ?");
                sale.setInt(1, 100);
                System.out.println(sale.executeUpdate());
                ResultSet r = s.executeQuery("SELECT SUM(price) FROM items");
                r.next();
                System.out.println(r.getInt(1));
            }
        }
    }
- 1\n300
+ 2\n225
- 2\n300
- 3\n150
> Дешевле 100 два товара — щит и лук, их цены стали 45 и 30. Сумма: 150 + 45 + 30 = 225.
```

## Первичный ключ и дубликаты

Чтобы в таблице не было двух товаров с одним названием, столбец объявляют **первичным ключом**:

```sql
CREATE TABLE items (name TEXT PRIMARY KEY, price INTEGER NOT NULL)
```

Теперь повторная вставка того же имени бросит `SQLException` с сообщением про нарушение уникальности. В SQLite есть и мягкий вариант: `INSERT OR IGNORE` молча пропускает дубликат, а `executeUpdate` при этом вернёт 0. `NOT NULL` запрещает оставлять столбец пустым.

```quiz
? Какой код безопасен, если `name` ввёл пользователь?
- `statement.executeQuery("SELECT * FROM items WHERE name = '" + name.trim() + "'")`
- `statement.executeQuery("SELECT * FROM items WHERE name = \"" + name + "\"")`
+ `prepareStatement("SELECT * FROM items WHERE name = ?")` и `setString(1, name)`
- `prepareStatement("SELECT * FROM items WHERE name = '" + name + "'")`
> Безопасны только параметры. `trim` и другие кавычки не спасают от инъекции. А `prepareStatement` со склейкой внутри ничем не лучше `Statement`: значение всё равно стало частью текста запроса.
```
