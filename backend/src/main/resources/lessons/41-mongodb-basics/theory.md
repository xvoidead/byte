SQLite и PostgreSQL хранят данные в таблицах с заранее заданными столбцами. **MongoDB** устроена иначе: это **документная** база. Каждая запись — документ, похожий на JSON, и у разных документов могут быть разные поля, вложенные объекты и списки. Профиль игрока с инвентарём и статистикой хорошо ложится в один такой документ.

В песочнице сервер MongoDB работает прямо внутри вашей программы (это mongo-java-server), поэтому данные живут, пока программа работает. Код подключения при этом такой же, как для настоящего сервера.

## Документы

Документ в Java — объект `Document` из библиотеки BSON. Это словарь «ключ → значение», где значением может быть число, строка, список или другой документ:

```java
Document steve = new Document("name", "Steve")
        .append("level", 1)
        .append("items", List.of("меч"))
        .append("stats", new Document("kills", 0));

System.out.println(steve.getString("name"));
System.out.println(steve.toJson());
```

Документ можно получить и из текста JSON: `Document.parse("{\"name\": \"Alex\"}")`. Когда документ попадает в базу, MongoDB добавляет поле `_id` — уникальный идентификатор, если вы не задали его сами.

```quiz
? Что выведет программа?
    import org.bson.Document;
    import java.util.List;

    public class Main {
        public static void main(String[] args) {
            Document d = new Document("name", "Alex").append("level", 3);
            d.append("items", List.of("лук"));
            d.put("level", d.getInteger("level") + 1);
            System.out.println(d.toJson());
        }
    }
- {"name": "Alex", "level": 3, "items": ["лук"]}
+ {"name": "Alex", "level": 4, "items": ["лук"]}
- {"level": 4, "name": "Alex", "items": ["лук"]}
- {name: Alex, level: 4, items: [лук]}
> `Document` хранит ключи в порядке добавления. `put` заменил значение `level`, но место ключа не изменилось. `toJson` пишет ключи в кавычках.
```

## Подключение и вставка

Документы лежат в **коллекциях**, коллекции — в **базах**. Базу и коллекцию не нужно создавать заранее: они появятся при первой вставке.

```java
try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
    MongoCollection<Document> players = client.getDatabase("game").getCollection("players");
    players.insertOne(new Document("name", "Steve").append("level", 1));
}
```

`MongoClient` держит соединения, поэтому его закрывают в `try` с ресурсами. Для нескольких документов есть `insertMany(List.of(...))`.

## Поиск: фильтры, сортировка, лимит

`find` принимает **фильтр** — условие на документы. Фильтры удобно строить методами класса `Filters`:

```java
Document alex = players.find(Filters.eq("name", "Alex")).first();   // null, если не нашёлся

for (Document p : players.find(Filters.gte("level", 5))
        .sort(Sorts.orderBy(Sorts.descending("level"), Sorts.ascending("name")))
        .limit(3)) {
    System.out.println(p.getString("name"));
}
```

Есть `eq`, `ne`, `gt`, `gte`, `lt`, `lte`, `in`, а объединяют условия `Filters.and(...)` и `Filters.or(...)`. `find()` без аргументов вернёт все документы. Сортировку, как и в SQL, лучше отдать базе.

```quiz
? Что выведет программа?
    import com.mongodb.client.*;
    import com.mongodb.client.model.*;
    import org.bson.Document;
    import java.util.List;

    public class Main {
        public static void main(String[] args) {
            try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
                MongoCollection<Document> players = client.getDatabase("game").getCollection("players");
                players.insertMany(List.of(
                        new Document("name", "Steve").append("level", 5),
                        new Document("name", "Alex").append("level", 9),
                        new Document("name", "Kai").append("level", 5),
                        new Document("name", "Notch").append("level", 2)));
                for (Document p : players.find(Filters.gt("level", 2))
                        .sort(Sorts.orderBy(Sorts.ascending("level"), Sorts.descending("name")))) {
                    System.out.print(p.getString("name") + " ");
                }
            }
        }
    }
- Kai Steve Alex
+ Steve Kai Alex
- Alex Steve Kai
- Notch Kai Steve Alex
> Notch отсеял фильтр `level > 2`. Остальные идут по возрастанию уровня, а при равном уровне — по убыванию имени: Steve раньше Kai.
```

## Изменение документов

`updateOne(фильтр, изменение)` меняет первый подходящий документ. Изменения строят методами `Updates`:

- `Updates.set("level", 5)` — задать значение;
- `Updates.inc("coins", 10)` — прибавить (или отнять, если число отрицательное);
- `Updates.push("items", "лук")` — добавить в список;
- `Updates.addToSet("items", "лук")` — добавить, только если такого элемента ещё нет;
- `Updates.combine(...)` — несколько изменений сразу.

Результат `UpdateResult` говорит, что произошло: `getMatchedCount()` — сколько документов подошло под фильтр, `getModifiedCount()` — сколько изменилось на самом деле.

Если нужен документ *после* изменения, есть `findOneAndUpdate`:

```java
Document updated = players.findOneAndUpdate(
        Filters.eq("name", "Steve"),
        Updates.inc("level", 1),
        new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
```

Без `ReturnDocument.AFTER` метод вернёт документ в состоянии *до* изменения, а если документа нет — `null`. Удаляют документы `deleteOne` и `deleteMany`.

```quiz
? Что выведет программа?
    import com.mongodb.client.*;
    import com.mongodb.client.model.*;
    import com.mongodb.client.result.UpdateResult;
    import org.bson.Document;
    import java.util.List;

    public class Main {
        public static void main(String[] args) {
            try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
                MongoCollection<Document> players = client.getDatabase("game").getCollection("players");
                players.insertOne(new Document("name", "Steve").append("items", List.of("меч")));
                UpdateResult first = players.updateOne(Filters.eq("name", "Steve"), Updates.addToSet("items", "меч"));
                UpdateResult second = players.updateOne(Filters.eq("name", "Alex"), Updates.addToSet("items", "меч"));
                System.out.println(first.getMatchedCount() + " " + first.getModifiedCount());
                System.out.println(second.getMatchedCount() + " " + second.getModifiedCount());
            }
        }
    }
- 1 1\n0 0
+ 1 0\n0 0
- 1 0\n1 0
- 0 0\n0 0
> Steve нашёлся, но меч у него уже есть, и `addToSet` ничего не изменил: 1 и 0. Alex не нашёлся вовсе: 0 и 0. Так можно отличить «уже есть» от «нет игрока».
```
