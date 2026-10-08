Fabric позволяет создавать не только серверные, но и клиентские моды. Клиентские API нельзя загружать на dedicated server, поэтому код и entrypoint должны быть помечены как клиентские.

## ClientModInitializer

Для клиентской логики реализуйте `ClientModInitializer`. Fabric Loader вызывает `onInitializeClient()` после загрузки клиента:

```java
public class ClientKeyMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Регистрация клавиш и client callbacks.
    }
}
```

В `fabric.mod.json` клиентский entrypoint указывается отдельно в массиве `entrypoints.client`; поле `environment` можно ограничить значением `client`.

```quiz
? Какой метод вызывает Fabric у ClientModInitializer?
- onInitialize()
+ onInitializeClient()
- onClientStart(String[] args)
> Клиентский entrypoint отделён от общего ModInitializer.
```

## Регистрируем клавишу

Используйте `KeyBindingHelper.registerKeyBinding`, чтобы создать настройку клавиши в меню управления:

```java
KeyBinding wave = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.byte.wave", InputUtil.Type.KEYSYM, 71, "category.byte.general"));
```

`KEYSYM` означает физическую клавишу клавиатуры; код 71 в этом примере соответствует G. Переводимые ключи и категория в реальном моде задаются в `assets/<modid>/lang/en_us.json`.

```quiz
? Зачем вызывать KeyBindingHelper.registerKeyBinding?
- Чтобы зарегистрировать серверную команду
+ Чтобы добавить настройку клавиши в систему управления
- Чтобы запускать tick callback на сервере
> KeyBindingHelper подключает биндинг к экрану управления Minecraft.
```

## Читаем нажатия на client tick

`ClientTickEvents.END_CLIENT_TICK` вызывается каждый тик клиента. `wasPressed()` возвращает `true` для накопленного нажатия, пока очередь нажатий не исчерпана:

```java
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    while (wave.wasPressed()) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("Ты нажал G!"), false);
        }
    }
});
```

Используйте `while`, а не `if`: если пользователь нажал клавишу несколько раз до следующего тика, обработайте каждое нажатие. Проверка `client.player != null` нужна во время загрузки и на экранах, где игрок ещё не создан.

```quiz
? Почему здесь while (wave.wasPressed()), а не одиночный if?
- Чтобы отключить клавишу
+ Чтобы обработать все накопленные нажатия
- Чтобы замедлить client tick
> wasPressed потребляет по одному событию; цикл забирает все события из очереди.
```
