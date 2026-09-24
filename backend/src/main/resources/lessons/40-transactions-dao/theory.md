Перевод монет от одного игрока другому — это два изменения: у одного убавить, другому прибавить. Если программа упадёт между ними или второе изменение не получится, монеты пропадут или появятся из ниоткуда. Базы данных решают эту проблему **транзакциями**.

## Всё или ничего

**Транзакция** — группа команд, которая выполняется целиком или не выполняется вовсе. Пока транзакция не завершена, её изменения видит только она сама. Если что-то пошло не так, базу можно вернуть к состоянию до начала транзакции.

В JDBC по умолчанию включён режим **autocommit**: каждая команда — отдельная транзакция, которая сразу сохраняется. Для перевода это опасно:

```java
credit.executeUpdate();  // Steve получил 50 монет — уже сохранено
debit.executeUpdate();   // а тут ошибка: у Alex не хватает монет
```

Первая команда успела сохраниться, вторая — нет. Монеты появились из ниоткуда.

```quiz
? Что выведет программа?
    import java.sql.*;

    public class Main {
        public static void main(String[] args) throws Exception {
            try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:")) {
                Statement s = c.createStatement();
                s.execute("CREATE TABLE accounts (name TEXT, balance INTEGER CHECK (balance >= 0))");
                s.execute("INSERT INTO accounts VALUES ('Alex', 10), ('Steve', 0)");
                try {
                    s.executeUpdate("UPDATE accounts SET balance = balance + 50 WHERE name = 'Steve'");
                    s.executeUpdate("UPDATE accounts SET balance = balance - 50 WHERE name = 'Alex'");
                } catch (SQLException e) {
                    System.out.println("Ошибка");
                }
                ResultSet r = s.executeQuery("SELECT SUM(balance) FROM accounts");
                r.next();
                System.out.println(r.getInt(1));
            }
        }
    }
- Ошибка\n10
+ Ошибка\n60
- 10
- Ошибка\n-40
> `CHECK (balance >= 0)` не дал увести баланс Alex в минус, и вторая команда упала. Но первая уже сохранилась: Steve получил 50 монет, и всего в игре стало 60 вместо 10.
```

## commit и rollback

Чтобы объединить команды в транзакцию, выключают autocommit, а в конце вызывают `commit()` — сохранить всё — или `rollback()` — отменить всё:

```java
db.setAutoCommit(false);
try {
    debit.executeUpdate();
    credit.executeUpdate();
    db.commit();
} catch (SQLException e) {
    db.rollback();
    throw e;
} finally {
    db.setAutoCommit(true);
}
```

Отменять нужно при *любой* ошибке, в том числе при вашей собственной — например, когда `executeUpdate` вернул 0 и получателя не оказалось. `finally` возвращает соединению обычный режим, чтобы следующие команды не застряли в незавершённой транзакции.

```quiz
? Что выведет программа?
    import java.sql.*;

    public class Main {
        public static void main(String[] args) throws Exception {
            try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:")) {
                Statement s = c.createStatement();
                s.execute("CREATE TABLE accounts (name TEXT, balance INTEGER)");
                s.execute("INSERT INTO accounts VALUES ('Alex', 100)");
                c.setAutoCommit(false);
                s.executeUpdate("UPDATE accounts SET balance = 0 WHERE name = 'Alex'");
                c.rollback();
                s.executeUpdate("UPDATE accounts SET balance = balance + 5 WHERE name = 'Alex'");
                c.commit();
                ResultSet r = s.executeQuery("SELECT balance FROM accounts");
                r.next();
                System.out.println(r.getInt(1));
            }
        }
    }
- 5
+ 105
- 100
- 0
> `rollback()` отменил обнуление, баланс снова 100. Следующая команда началась в новой транзакции, и `commit()` сохранил +5.
```

## Ограничения в таблице

Проверки лучше доверить самой базе. `CHECK` задаёт условие, которое должно выполняться для каждой строки:

```sql
CREATE TABLE accounts (
    name    TEXT PRIMARY KEY,
    balance INTEGER NOT NULL CHECK (balance >= 0)
)
```

Теперь `UPDATE`, который увёл бы баланс в минус, бросит `SQLException`, и ни одна ошибка в коде не сделает баланс отрицательным. Вместе с транзакцией это надёжная защита: база откажет, а `rollback()` отменит всё, что успело измениться.

## DAO: SQL в одном месте

Когда запросов становится много, их собирают в отдельный класс — **DAO** (Data Access Object, «объект доступа к данным»). Остальная программа вызывает понятные методы вроде `dao.transfer("Alex", "Steve", 50)` и не знает, что внутри SQL:

```java
public class AccountDao {
    private final Connection db;

    public AccountDao(Connection db) {
        this.db = db;
    }

    public List<Account> all() throws SQLException {
        // SELECT ... и превращение строк в объекты Account
    }
}
```

Строки таблицы удобно превращать в `record`: `new Account(rows.getString("name"), rows.getInt("balance"))`.

```quiz
? Чем полезен DAO?
- Запросы через DAO выполняются быстрее
+ SQL собран в одном месте: его проще менять и проверять, а логика программы не зависит от устройства базы
- Без DAO нельзя использовать транзакции
- DAO защищает от SQL-инъекций вместо PreparedStatement
> DAO — способ организовать код, а не особая возможность JDBC. Если завтра база сменится на другую, переписать придётся только DAO. Защиту от инъекций по-прежнему дают параметры.
```
