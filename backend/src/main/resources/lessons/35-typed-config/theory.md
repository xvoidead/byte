`Map<String, Object>` удобен для чтения, но неудобен в работе. Везде нужны приведения типов, а опечатка в ключе обнаружится, только когда программа упадёт. Хорошая практика — прочитать конфиг **один раз** при запуске, проверить его и превратить в обычный объект с типизированными полями.

## Конфиг как record

Record идеально подходит для настроек: поля неизменяемые, а геттеры и `toString` пишутся сами:

```java
public record ServerConfig(String serverName, int maxPlayers, Difficulty difficulty, int spawnProtection) {
}
```

Дальше программа работает только с `ServerConfig`. Она пишет `config.maxPlayers()` и получает `int`, без строковых ключей и приведений. Если ключ переименуют, компилятор покажет все места, которые нужно поправить.

## Проверяем тип значения

Пользователь может написать в конфиге что угодно: `max-players: двадцать`, `20.5` или `'20'`. Слепое приведение `(Integer) map.get(...)` в таких случаях бросит `ClassCastException` с непонятным сообщением. Лучше сначала проверить тип через `instanceof`:

```java
Object value = map.get("max-players");
if (value instanceof Integer number) {
    // здесь number — уже Integer
} else {
    errors.add("max-players должен быть целым числом");
}
```

```quiz
? Что выведет программа?
    Object[] values = {20, "20", 20.0};
    for (Object value : values) {
        if (value instanceof Integer n) {
        System.out.println("число " + (n + 1));
    } else {
        System.out.println("не целое: " + value);
    }
    }
- число 21\nчисло 21\nчисло 21
+ число 21\nне целое: 20\nне целое: 20.0
- число 21\nчисло 21\nне целое: 20.0
- число 21\nне целое: 20\nчисло 21
> Только первое значение — `Integer`. Строка `"20"` и дробное `20.0` (`Double`) проходят мимо проверки, хотя при печати выглядят почти одинаково.
```

## Перечисления вместо строк

Если у настройки несколько допустимых значений, опишите их через `enum`. Строку из конфига можно превратить в константу методом `valueOf`, а для неизвестного имени он бросит `IllegalArgumentException`:

```java
try {
    difficulty = Difficulty.valueOf(text.toUpperCase());
} catch (IllegalArgumentException e) {
    errors.add("difficulty должен быть одним из: peaceful, easy, normal, hard");
}
```

```quiz
? Что выведет программа?
    enum Difficulty { EASY, NORMAL, HARD }
    try {
        String text = "normal";
        System.out.println(Difficulty.valueOf(text.toUpperCase()));
        text = "hardcore";
        System.out.println(Difficulty.valueOf(text.toUpperCase()));
    } catch (IllegalArgumentException e) {
        System.out.println("нет такой сложности");
    }
- NORMAL\nHARD
- normal\nнет такой сложности
+ NORMAL\nнет такой сложности
- нет такой сложности
> Первая строка напечаталась до ошибки. `valueOf` ищет константу по точному имени, поэтому строку сначала переводим в верхний регистр. Константы `HARDCORE` нет, и второй вызов бросает исключение.
```

## Понятные ошибки

Сообщение `ClassCastException: class java.lang.String cannot be cast to class java.lang.Integer` пользователю ничего не скажет. Сообщение `max-players должен быть больше 0` сразу объясняет, что исправить.

Ещё удобнее показывать **все** ошибки сразу, а не по одной за запуск. Для этого соберём их в список и бросим одно исключение в конце:

```java
List<String> errors = new ArrayList<>();
// ...проверки добавляют сообщения в errors...
if (!errors.isEmpty()) {
    throw new ConfigException(errors);
}
```

```quiz
? Пользователь написал `max-players: 0`. Какое сообщение лучше?
- `NumberFormatException`
- `Ошибка в конфиге`
+ `max-players должен быть больше 0`
- Ничего не выводить и взять 20
> Хорошее сообщение называет ключ и объясняет, какое значение допустимо. Тихо подменять значение опасно: пользователь не узнает, что его настройка не сработала.
```

## Сломанный файл

Если в YAML ошибка, например незакрытая скобка `[`, `load` бросит `YAMLException` из пакета `org.yaml.snakeyaml.error`. А если в файле просто строка текста, `load` вернёт `String`, а не `Map`. Оба случая стоит превратить в понятную ошибку:

```java
Object data;
try {
    data = new Yaml().load(text);
} catch (YAMLException e) {
    throw new ConfigException(List.of("файл не читается как YAML"));
}
if (!(data instanceof Map)) { ... }
```
