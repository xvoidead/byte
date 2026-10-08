Fabric Loader предоставляет каталоги мода, в том числе каталог пользовательских настроек. Для простого примера сохраним параметры в JSON, а Gson уже доступен в Byte.

## Находим папку конфигов

Вызов `FabricLoader.getInstance().getConfigDir()` возвращает `Path` папки `config` внутри каталога игры. К имени файла добавляйте `resolve`, не склеивайте пути вручную:

```java
Path path = FabricLoader.getInstance().getConfigDir().resolve("byte.json");
```

Для создания вложенной папки используйте `Files.createDirectories(path.getParent())`. Она безопасна и в том случае, если каталог уже существует.

```quiz
? Что возвращает Path.resolve("byte.json")?
- Содержимое файла
+ Путь к файлу внутри исходного каталога
- JSON-объект
> resolve строит новый Path, присоединяя имя к базовому пути.
```

## JSON и Gson

Описать настройки удобно отдельным классом с полями:

```java
public class ModConfig {
    public boolean enabled = true;
    public String welcome = "Добро пожаловать, {player}!";
}
```

Gson преобразует объект в JSON и обратно. `Files.readString` читает текст, `fromJson` разбирает его в нужный класс:

```java
Gson gson = new Gson();
ModConfig config = gson.fromJson(Files.readString(path), ModConfig.class);
```

При записи используйте `Files.writeString(path, gson.toJson(config))`. В настоящем моде полезно обрабатывать повреждённый JSON и сохранять резервную копию настроек.

```quiz
? Что нужно передать в fromJson(text, ModConfig.class)?
- Путь к папке config
+ Текст JSON и класс результата
- Только имя мода
> Gson читает JSON-текст и создаёт объект указанного класса.
```

## Значения по умолчанию

Проверяйте `Files.exists(path)`. Если файла нет, создайте родительскую папку и запишите `new ModConfig()` до чтения:

```java
if (!Files.exists(path)) {
    Files.createDirectories(path.getParent());
    Files.writeString(path, gson.toJson(new ModConfig()));
}
```

Публичные поля с начальными значениями задают настройки для первого запуска. Далее проверьте `enabled` и подставьте имя игрока в `welcome`. Симуляция Byte хранит рабочую папку отдельно на каждый тест.

```quiz
? Что должен сделать мод, если enabled равно false?
- Отправить приветствие
- Удалить папку игры
+ Не выполнять включённую этой настройкой функцию
> Флаг конфигурации позволяет включать и выключать поведение без перекомпиляции.
```
