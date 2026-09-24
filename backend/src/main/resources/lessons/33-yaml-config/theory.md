Почти у любой программы есть настройки: адрес сервера, число игроков, язык. Их не пишут прямо в коде, а выносят в **конфиг** — отдельный файл, который можно поменять без перекомпиляции. В этом треке вы научитесь читать, проверять и обновлять конфиги в форматах YAML, JSON и `.properties`.

## Что такое YAML

YAML — формат, который легко читать человеку. Каждая строка — пара «ключ: значение», а вложенность задаётся отступами:

```yaml
# Комментарий начинается с решётки
server-name: Byte Craft
max-players: 20
pvp: true
spawn:          # раздел: внутри — свои ключи
  x: 100
  y: 64
motd:           # список: каждый элемент с дефиса
  - Добро пожаловать!
  - Не ломайте спавн
```

Отступы делаются пробелами, табуляция в YAML запрещена. Кавычки у строк обычно не нужны.

## Читаем файл

Библиотека SnakeYAML уже подключена. Класс `Yaml` превращает текст в обычные коллекции Java:

```java
String text = Files.readString(Path.of("config.yml"));
Map<String, Object> config = new Yaml().load(text);

System.out.println(config.get("server-name")); // Byte Craft
```

Файл `config.yml` лежит в рабочей папке программы — рядом с ней, поэтому достаточно имени без пути. Если файла нет, `readString` бросит исключение, так что его наличие удобно проверить заранее: `Files.exists(path)`.

## Типы значений

SnakeYAML сам определяет тип каждого значения:

| В YAML | В Java |
|--------|--------|
| `20` | `Integer` |
| `1.5` | `Double` |
| `true`, `false` | `Boolean` |
| `Byte Craft`, `'20'` | `String` |
| раздел с отступом | `Map<String, Object>` |
| список с дефисами | `List<Object>` |

В словаре всё хранится как `Object`, поэтому значение нужно **привести** к нужному типу:

```java
int maxPlayers = (Integer) config.get("max-players");
boolean pvp = (Boolean) config.get("pvp");
```

```quiz
? Что выведет программа?
    import java.util.Map;
    import org.yaml.snakeyaml.Yaml;
    
    public class Main {
        public static void main(String[] args) {
            String text = "port: 8080\nversion: '8080'";
            Map<String, Object> config = new Yaml().load(text);
            Object port = config.get("port");
            Object version = config.get("version");
            System.out.println(port.getClass().getSimpleName());
            System.out.println(version.getClass().getSimpleName());
        }
    }
+ Integer\nString
- Integer\nInteger
- String\nString
- Ошибка компиляции
> Число без кавычек становится `Integer`, а в кавычках — строкой, даже если внутри одни цифры.
```

## Да, нет, вкл, выкл

У YAML есть ловушка: кроме `true` и `false` логическими значениями считаются слова `yes`, `no`, `on` и `off`.

```quiz
? Что выведет программа?
    import java.util.Map;
    import org.yaml.snakeyaml.Yaml;
    
    public class Main {
        public static void main(String[] args) {
            String text = "pvp: yes\nanswer: no";
            Map<String, Object> config = new Yaml().load(text);
            Object answer = config.get("answer");
            System.out.println(config.get("pvp"));
            System.out.println(answer instanceof Boolean);
        }
    }
- yes\nfalse
+ true\ntrue
- yes\ntrue
- true\nfalse
> SnakeYAML читает `yes` как `true`, а `no` — как `false`. Если нужна именно строка `no` (например, код страны Норвегии), возьмите её в кавычки: `'no'`.
```

## Разделы и списки

Раздел — это словарь внутри словаря, а список — `List`:

```java
Map<String, Object> spawn = (Map<String, Object>) config.get("spawn");
int x = (Integer) spawn.get("x");

List<String> motd = (List<String>) config.get("motd");
for (String line : motd) {
    System.out.println(line);
}
```

Если ключа в файле нет, `get` вернёт `null`, и программа упадёт на первом же обращении. Для необязательных ключей есть `getOrDefault`:

```java
List<String> motd = (List<String>) config.getOrDefault("motd", List.of());
```

```quiz
? Как в конфиге выше получить координату `y` точки спавна?
- `config.get("spawn.y")`
- `config.get("y")`
+ `((Map<String, Object>) config.get("spawn")).get("y")`
- `config.get("spawn")[1]`
> Вложенный раздел — отдельный словарь. Сначала достаём его, потом берём из него ключ. Ключа `spawn.y` в верхнем словаре нет.
```
