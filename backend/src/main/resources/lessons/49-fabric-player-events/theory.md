Fabric API предоставляет события для серверного жизненного цикла и подключений игроков. Callback-и регистрируются из `ModInitializer.onInitialize()`.

## Событие входа

`ServerPlayConnectionEvents.JOIN` вызывается, когда игрок готов к игре. Callback получает обработчик подключения, отправителя пакетов и сервер:

```java
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
    ServerPlayerEntity player = handler.player;
    String name = player.getName().getString();
    player.sendMessage(Text.literal("Привет, " + name + "!"), false);
});
```

В Yarn mappings у `ServerPlayNetworkHandler` поле `player` — игрок, связанный с этим соединением. `Text.literal` создаёт простой текст Minecraft; параметр `false` означает, что сообщение не показывается поверх игрового интерфейса.

```quiz
? Что вернёт player.getName().getString() для игрока с именем Alex?
- Объект ServerPlayerEntity
+ Строку "Alex"
- UUID сервера
> getName() возвращает Text, а getString() — обычный String с именем.
```

## Событие отключения

`ServerPlayConnectionEvents.DISCONNECT` сообщает, что соединение закрывается. Оно получает обработчик и сервер:

```java
ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
    String name = handler.player.getName().getString();
    handler.player.sendMessage(Text.literal("До встречи, " + name + "!"), false);
});
```

Не путайте callback-ы: у `JOIN` есть параметр `PacketSender`, у `DISCONNECT` его нет. Обычно событие выхода используют, чтобы очистить временное состояние или сохранить данные игрока.

```quiz
? Сколько параметров у callback-а ServerPlayConnectionEvents.DISCONNECT?
- 1
+ 2
- 3
> Он получает ServerPlayNetworkHandler и MinecraftServer.
```

## Регистрация и сторона сервера

Callback регистрируют один раз при загрузке мода, а не при каждом входе игрока. Используйте `ServerPlayConnectionEvents.JOIN.register(...)` внутри `onInitialize`. Импорты находятся в пакетах `net.fabricmc.fabric.api.networking.v1` и `net.minecraft.*`.

Серверные события доступны и в dedicated server, и во встроенном сервере одиночной игры. Если код использует только серверный API, его не нужно загружать как client-only entrypoint.

```quiz
? Где следует регистрировать обработчик JOIN?
- В цикле, который запускается при каждом входе
+ Один раз в onInitialize()
- В fabric.mod.json как текст
> Entry point вызывается при загрузке мода; именно там регистрируют callbacks.
```
