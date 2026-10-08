Команда — ещё один способ взаимодействия с плагином. В Bukkit её обработчик обычно реализует `CommandExecutor`; метод `onCommand` получает отправителя, команду и массив аргументов.

## Регистрируем команды

Описание команд лежит в `resources/plugin.yml`. Имя здесь становится доступным через `getCommand`:

```yaml
commands:
  msg:
    description: Личное сообщение
  broadcast:
    description: Объявление для сервера
```

Плагин может сам стать обработчиком:

```java
public class MessagePlugin extends JavaPlugin implements CommandExecutor {
    @Override
    public void onEnable() {
        getCommand("msg").setExecutor(this);
        getCommand("broadcast").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("Команда: " + command.getName());
        return true;
    }
}
```

`true` означает, что команда обработана. `false` сообщает Bukkit показать usage из `plugin.yml`, но для учебного упражнения проще печатать понятную инструкцию самим.

```quiz
? Где объявляют команду, чтобы getCommand("msg") её нашёл?
+ В plugin.yml
- В config.yml
- В имени Java-файла
> Командные описания загружаются из plugin.yml вместе с главным классом.
```

## Аргументы и адресаты

Введённая команда `/msg Alex привет мир` даст `args = ["Alex", "привет", "мир"]`. Первый аргумент — имя игрока, остальное надо склеить:

```java
String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
Player target = getServer().getPlayerExact(args[0]);
```

Проверяйте длину массива **до** чтения `args[0]`. `getPlayerExact` возвращает `null`, если такого игрока нет онлайн. Отправить строку игроку и отправителю можно через `sendMessage`.

```quiz
? Сколько элементов будет в args для команды /msg Alex привет мир?
- 2
+ 3
- 4
> Имя, слово «привет» и слово «мир» — три элемента. Пробелы между словами сохраняются при последующем String.join.
```

## Права и рассылка

У `CommandSender` есть `isOp()`. Для разрешений с тонкой настройкой обычно используют `hasPermission("byte.broadcast")`, а `isOp()` удобен для первого примера.

`getServer().broadcastMessage(text)` рассылает сообщение всем онлайн-игрокам. Не ограничивайтесь проверкой прав в интерфейсе: её нужно делать и внутри обработчика команды.

```java
if (!sender.isOp()) {
    sender.sendMessage("Недостаточно прав.");
    return true;
}
getServer().broadcastMessage("[Объявление] " + text);
```

```quiz
? Что вернёт getPlayerExact("Herobrine"), если игрок офлайн?
- Новый Player
- Пустую строку
+ null
> Не вызывайте методы у результата, пока не проверили его на null.
```
