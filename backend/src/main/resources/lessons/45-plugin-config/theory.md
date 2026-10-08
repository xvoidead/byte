В предыдущих уроках настройки были записаны прямо в Java-коде. Тогда любое изменение требует пересборки плагина. В Bukkit стандартные настройки удобно хранить в `config.yml`.

## Конфиг по умолчанию

`saveDefaultConfig()` копирует `resources/config.yml` в папку плагина, если там ещё нет пользовательского файла. Обычно вызов находится в `onEnable()`:

```java
@Override
public void onEnable() {
    saveDefaultConfig();
}
```

В редакторе Byte папка `resources/` становится classpath-ресурсом, а `config.yml` в рабочей папке имитирует пользовательские настройки сервера. Для упражнения тестовый сервер загружает конфиг через MockBukkit.

```quiz
? Когда saveDefaultConfig() заменяет уже существующий конфиг пользователя?
- При каждом входе игрока
+ Не заменяет его; копирует шаблон, если файла ещё нет
- Каждую секунду
> Иначе администратор терял бы собственные настройки при обновлении плагина.
```

## Читаем значения

Bukkit разбирает YAML и предоставляет методы `getConfig()`. Для строки есть `getString(path, defaultValue)`, для последовательности — `getStringList(path)`:

```yaml
prefix: "[Byte]"
welcome: "Добро пожаловать, %player%!"
motd:
  - "Paper + Bukkit"
  - "Настройте сообщения в config.yml"
```

```java
String prefix = getConfig().getString("prefix", "[Server]");
String welcome = getConfig().getString("welcome", "Привет, %player%!");
List<String> motd = getConfig().getStringList("motd");
```

Путь указывает вложенность через точку: `messages.join`. Значение по умолчанию защищает от отсутствующего ключа, а `getStringList` вернёт пустой список, если список не задан.

```quiz
? Что вернёт getStringList("motd"), если в config.yml есть три элемента списка?
- Строку с YAML-текстом
- Число 3
+ Список из трёх строк
> Bukkit уже разобрал YAML; цикл for-each может пройти по готовому List<String>.
```

## Подстановка и проверка настроек

Шаблон — обычная строка Java, замените метку вызовом `replace`:

```java
String message = welcome.replace("%player%", player.getName());
player.sendMessage(prefix + " " + message);
```

Не включайте цветовые коды в урок: здесь важнее отделить текст сообщения от логики плагина. На реальном сервере для сложных настроек полезно проверять, что обязательные значения есть и имеют ожидаемый формат.

```quiz
? Что вернёт "Hi, %player%!".replace("%player%", "Alex")?
+ Hi, Alex!
- Hi, %player%!
- Alex Hi
> String.replace возвращает новую строку с заменённым фрагментом.
```
