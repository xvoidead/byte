Когда программа запускается впервые, конфига ещё нет. А когда выходит новая версия, в ней появляются новые настройки, которых нет в старом конфиге пользователя. Хорошая программа справляется с обоими случаями сама и не заставляет человека править файл руками.

## Настройки по умолчанию в ресурсах

Значения по умолчанию удобно хранить в самой программе, в **ресурсах**. Файлы из папки `resources/` попадают в classpath вместе со скомпилированными классами и читаются так:

```java
try (InputStream in = Main.class.getResourceAsStream("/config.yml")) {
    Map<String, Object> defaults = new Yaml().load(in);
}
```

Слеш в начале означает «от корня classpath». Если ресурса нет, метод вернёт `null`, а не бросит исключение.

Ресурс нельзя изменить: он «зашит» в программу. Пользователь меняет свою копию — `config.yml` в рабочей папке.

## Первый запуск: копируем файл

Если конфига нет, проще всего скопировать ресурс целиком:

```java
Path file = Path.of("config.yml");
if (!Files.exists(file)) {
    try (InputStream in = Main.class.getResourceAsStream("/config.yml")) {
        Files.copy(in, file);
    }
}
```

`Files.copy` переносит байты как есть, поэтому комментарии из ресурса тоже попадут в файл. Они подскажут пользователю, что означает каждая настройка.

```quiz
? Почему файл лучше скопировать, а не загрузить через `Yaml` и сохранить через `dump`?
- `dump` работает медленнее
+ `dump` записывает только данные, и комментарии пропадут
- `dump` не умеет сохранять числа
- Разницы нет
> SnakeYAML при чтении отбрасывает комментарии: в `Map` их просто негде хранить. Копия байтов сохраняет файл в точности.
```

## Дописываем недостающие ключи

В новой версии программы появился ключ `language`, а у пользователя старый конфиг без него. Пройдём по значениям по умолчанию и добавим то, чего не хватает:

```java
for (Map.Entry<String, Object> entry : defaults.entrySet()) {
    if (!config.containsKey(entry.getKey())) {
        config.put(entry.getKey(), entry.getValue());
    }
}
```

Проверяем именно `containsKey`, а не `get(...) == null`: значения `false` или `0` — законный выбор пользователя, трогать их нельзя.

```quiz
? Что выведет программа?
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("max-players", 10);
    config.putIfAbsent("max-players", 20);
    config.putIfAbsent("pvp", true);
    System.out.println(config);
+ {max-players=10, pvp=true}
- {max-players=20, pvp=true}
- {pvp=true, max-players=10}
- {max-players=10}
> `putIfAbsent` кладёт значение, только если ключа ещё нет, поэтому 10 осталось. `LinkedHashMap` хранит ключи в порядке добавления, и новый ключ оказывается в конце.
```

## Ловушка getOrDefault

Иногда файл не дописывают, а просто подставляют значение при чтении: `config.getOrDefault("pvp", true)`. Но у этого метода есть тонкость.

```quiz
? Что выведет программа?
    Map<String, Object> config = new HashMap<>();
    config.put("pvp", null);
    System.out.println(config.getOrDefault("pvp", true));
    System.out.println(config.getOrDefault("motd", "Привет"));
- true\nПривет
+ null\nПривет
- null\nnull
- true\nnull
> Значение по умолчанию подставляется, только если ключа нет совсем. Ключ `pvp` есть, просто со значением `null`, поэтому вернулся `null`. В YAML так получается из строки `pvp:` без значения.
```

## Сохраняем, только если что-то изменилось

Записывать файл стоит, только если в нём что-то поменялось. Иначе при каждом запуске программы комментарии пользователя будут пропадать. Для записи используем `dump` с блочным стилем, чтобы каждый ключ оказался на своей строке:

```java
DumperOptions options = new DumperOptions();
options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
Files.writeString(file, new Yaml(options).dump(config));
```

И ещё один крайний случай: если файл пустой, `load` вернёт `null`, а не пустой словарь. Такой конфиг нужно дополнить всеми ключами.
