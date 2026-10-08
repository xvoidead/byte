Bukkit (сегодня чаще используют совместимую реализацию Paper) позволяет расширять сервер Minecraft **плагинами**. Сервер загружает готовые Java-классы и вызывает их в нужные моменты. Плагин не запускает отдельный Minecraft: он работает внутри уже запущенного сервера.

## Главный класс и plugin.yml

Главный класс наследуется от `JavaPlugin`. Метод `onEnable()` вызывается при включении плагина; здесь обычно регистрируют слушателей и команды.

```java
public class WelcomePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("Плагин включён");
    }
}
```

Bukkit находит класс через `plugin.yml` — файл метаданных в папке `resources/`:

```yaml
name: Welcome
version: 1.0.0
main: WelcomePlugin
api-version: '1.21'
```

В настоящем проекте Gradle/Loom не нужен: для плагина обычно используют Paper API и собирают JAR обычным Gradle или Maven. В редакторе Byte файлы плагина запускаются в MockBukkit — небольшой тестовой версии сервера.

```quiz
? Когда сервер вызывает onEnable()?
+ При включении плагина
- При каждом входе игрока
- Раз в секунду
> onEnable — точка инициализации. Для входа игрока есть отдельное событие.
```

## События игроков

Событие — объект с данными о том, что только что произошло. Чтобы его получать, класс реализует `Listener`, а методы помечаются `@EventHandler`. Сам слушатель нужно зарегистрировать:

```java
public class WelcomePlugin extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.sendMessage("Привет, " + player.getName() + "!");
    }
}
```

`PlayerJoinEvent` вызывается после добавления игрока на сервер. Список онлайн-игроков доступен через `getServer().getOnlinePlayers()`. У события выхода свой тип — `PlayerQuitEvent`; стандартное сообщение можно убрать вызовом `event.setQuitMessage(null)`.

```quiz
? Что делает registerEvents(this, this)?
+ Подписывает текущий объект-плагин на его методы с @EventHandler
- Создаёт нового игрока
- Вызывает onEnable ещё раз
> Первый аргумент — объект Listener, второй — владеющий им плагин.
```

## Первый вход и состояние

Bukkit предоставляет `Player.hasPlayedBefore()`, но в упражнении мы отмечаем первое появление за время работы плагина: добавим имя в `Set<String>`. Метод `Set.add` возвращает `true`, только если элемента раньше не было.

```java
Set<String> seenPlayers = new HashSet<>();
boolean firstTime = seenPlayers.add(player.getName().toLowerCase(Locale.ROOT));
```

Состояние в обычных коллекциях исчезает при перезапуске. Позже его можно будет сохранять в конфиг или базу. Не путайте число подключений и размер `getOnlinePlayers()`: событие входа происходит уже после подключения.

```quiz
? Что вернёт seen.add("Steve") при первом вызове, если множества изначально пусты?
+ true
- false
- null
> Set.add возвращает true, если множество изменилось — то есть элемент был новым.
```
