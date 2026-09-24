`.properties` — самый старый формат настроек в Java. Он встроен прямо в JDK, и библиотеки для него не нужны. Такие файлы до сих пор встречаются повсюду: `server.properties` у сервера Minecraft, `application.properties` в Spring.

## Формат .properties

```properties
# Комментарий
server-name=Byte Craft
max-players = 20
pvp=true
```

Каждая строка — `ключ=значение`. Пробелы вокруг `=` не важны, а вложенности и списков нет. Главное отличие от YAML в том, что **все значения — строки**.

## Класс Properties

`Properties` — это словарь строк с методами для чтения и записи файлов:

```java
Properties props = new Properties();
try (Reader reader = Files.newBufferedReader(Path.of("server.properties"))) {
    props.load(reader);
}
String name = props.getProperty("server-name");
String distance = props.getProperty("view-distance", "10"); // со значением по умолчанию
props.setProperty("pvp", "false");
```

Есть ловушка с кодировкой. `load` принимает и `Reader`, и `InputStream`, но с `InputStream` Java по старой традиции читает файл в кодировке ISO-8859-1, и кириллица превращается в кракозябры. `Files.newBufferedReader` читает в UTF-8.

```quiz
? Что выведет программа?
    Properties props = new Properties();
    props.setProperty("slots", "20");
    System.out.println(props.getProperty("slots") + 1);
    System.out.println(props.getProperty("pvp", "false"));
- 21\nfalse
+ 201\nfalse
- 21\nnull
- 201\nnull
> Значение — строка `"20"`, и `+ 1` склеивает строки. Для арифметики нужен `Integer.parseInt`. Ключа `pvp` нет, поэтому вернулось значение по умолчанию.
```

```quiz
? Что выведет программа?
    Properties props = new Properties();
    String text = "# комментарий\nname = Byte Craft\nslots=20\nslots=30";
    props.load(new java.io.StringReader(text));
    String name = props.getProperty("name");
    System.out.println(name + "|" + props.getProperty("slots"));
    System.out.println(props.size());
- name = Byte Craft|20\n3
- Byte Craft|20\n2
+ Byte Craft|30\n2
- Byte Craft|30\n4
> Пробелы вокруг `=` отбрасываются. Комментарий не считается ключом, а повторный ключ перезаписывает прежнее значение — остаётся последнее.
```

## Запись файла

У `Properties` есть метод `store`, но первой строкой он всегда пишет текущую дату, например `#Thu Sep 24 12:00:00 UTC 2026`. Файл меняется при каждом сохранении, а ключи идут в случайном порядке. Поэтому в задании файл пишется своим методом: строка `ключ=значение` на каждый ключ, ключи по алфавиту.

## Версия конфига

Программа развивается: ключи переименовывают, меняют формат значений, добавляют новые. Но у пользователей остаются старые конфиги. Чтобы программа понимала, с каким форматом имеет дело, в конфиг записывают номер формата:

```properties
config-version=3
```

Если поля нет, значит, файл появился раньше, чем его стали записывать, то есть это версия 1.

## Миграции

**Миграция** — это шаг, который переводит конфиг с версии N на N+1. Для каждой новой версии пишется один маленький метод:

```java
static void migrateTo2(Properties props) {
    String value = (String) props.remove("name");  // remove возвращает старое значение
    if (value != null) {
        props.setProperty("server-name", value);
    }
}
```

Миграции применяются **по очереди**, начиная с версии файла:

```java
while (version < CURRENT_VERSION) {
    switch (version) {
        case 1 -> migrateTo2(props);
        case 2 -> migrateTo3(props);
    }
    version++;
}
props.setProperty("config-version", String.valueOf(CURRENT_VERSION));
```

```quiz
? Зачем применять миграции цепочкой 1 → 2 → 3, а не писать отдельную миграцию «1 → 3»?
+ Каждый шаг пишется один раз, а новая версия добавляет всего один метод
- Так программа работает быстрее
- Иначе Properties не сохранит файл
- Миграцию «1 → 3» написать невозможно
> Если писать прямые миграции, при выходе версии 4 понадобятся три новых метода: 1 → 4, 2 → 4, 3 → 4. Цепочка растёт на один шаг за версию, и каждый шаг проверен.
```

## Что если версия новее

Пользователь может запустить старую программу с конфигом от новой. Старая программа не знает, что изменилось, и если «починит» такой конфиг, то испортит его. Правильно — сообщить об этом и ничего не трогать.
