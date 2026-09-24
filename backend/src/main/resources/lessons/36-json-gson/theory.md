YAML удобен для настроек, которые правит человек. А данные, которые пишет сама программа (игроков, очки, сохранения), чаще хранят в **JSON**. Это самый распространённый формат обмена данными: в нём общаются сайты, API и мобильные приложения.

## Формат JSON

```json
{
  "name": "Ада",
  "score": 100,
  "online": true,
  "friends": ["Боб", "Ева"]
}
```

Объект пишется в фигурных скобках, список — в квадратных. Строки всегда в двойных кавычках, ключи тоже. Комментариев в JSON нет.

## Gson: объект ↔ текст

Библиотека Gson превращает объекты Java в JSON и обратно. Имена полей становятся ключами:

```java
record Player(String name, int score) {}

Gson gson = new Gson();
String json = gson.toJson(new Player("Ада", 100));  // {"name":"Ада","score":100}
Player player = gson.fromJson(json, Player.class);
```

Если в JSON не хватает поля, Gson оставит значение по умолчанию: `0` для чисел и `null` для объектов.

```quiz
? Что выведет программа?
    import java.util.List;
    import com.google.gson.Gson;
    
    public class Main {
        public static void main(String[] args) {
            Gson gson = new Gson();
            System.out.println(gson.toJson(List.of(1, 2, 3)));
            System.out.println(gson.toJson("Ада"));
        }
    }
+ [1,2,3]\n"Ада"
- [1, 2, 3]\nАда
- {1,2,3}\n"Ада"
- [1,2,3]\nАда
> Список становится JSON-массивом без пробелов, а строка — JSON-строкой в кавычках. Кавычки — часть формата.
```

## Списки и TypeToken

С классом `Player` всё просто. А как прочитать `List<Player>`? Написать `List<Player>.class` нельзя: во время работы программы Java не помнит параметры типов. Если передать просто `List.class`, Gson не узнает, что внутри.

```quiz
? Что выведет программа?
    import java.util.List;
    import com.google.gson.Gson;
    
    public class Main {
        public static void main(String[] args) {
            Gson gson = new Gson();
            List<?> list = gson.fromJson("[1, 2]", List.class);
            System.out.println(list.get(0));
        }
    }
- 1
+ 1.0
- [1, 2]
- Ошибка: нельзя прочитать список
> Не зная типа элементов, Gson читает любое число как `Double`. С объектами ещё хуже: вместо `Player` получатся словари.
```

Полный тип передают через `TypeToken`. Фигурные скобки `{}` здесь обязательны: они создают маленький анонимный класс, и в нём Java запоминает `List<Player>`:

```java
Type type = new TypeToken<List<Player>>() {}.getType();
List<Player> players = gson.fromJson(text, type);
```

## Красивый вывод

`new Gson()` пишет всё в одну строку. Это компактно, но читать неудобно. `GsonBuilder` настраивает Gson перед созданием:

```java
Gson gson = new GsonBuilder()
        .setPrettyPrinting()      // отступы и переносы строк
        .create();
```

Ещё полезен `disableHtmlEscaping()`: без него символы вроде `<` и `'` превращаются в коды `<`, `'`.

## Повреждённый файл

Файл могли испортить руками или не дописать при сбое. Тогда `fromJson` бросит `JsonSyntaxException`, наследника `JsonParseException`. Главное правило: **не затирать** повреждённые данные. Если программа сохранит пустой список поверх сломанного файла, игроки пропадут навсегда. Лучше сообщить об ошибке и остановиться:

```java
try {
    players = load();
} catch (JsonParseException e) {
    System.out.println("Файл players.json повреждён");
    return;
}
```

И ещё мелочь: для пустого файла `fromJson` не бросает исключение, а возвращает `null`.

```quiz
? Файл с данными не читается. Что лучше сделать программе?
- Начать с пустого списка и сохранить его при выходе
+ Сообщить об ошибке и не перезаписывать файл
- Удалить файл
- Молча завершиться
> Пустой список поверх сломанного файла уничтожит данные, которые ещё можно восстановить руками. Понятное сообщение и нетронутый файл — самый безопасный вариант.
```
