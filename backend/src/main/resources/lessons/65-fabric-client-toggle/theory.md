Клавиатурные действия принадлежат клиентской стороне. Отделяйте client entrypoint и client API от общего серверного кода.

## Клиентский entrypoint

Реализуйте `ClientModInitializer`, а регистрацию поместите в `onInitializeClient()`. В `fabric.mod.json` класс указывается в `entrypoints.client`.

```java
public class ClientToggleMod implements ClientModInitializer {
    @Override public void onInitializeClient() { }
}
```

```quiz
? Какой метод вызывает ClientModInitializer?
- onInitialize()
+ onInitializeClient()
- onServerTick()
> У клиента отдельный entrypoint, чтобы client API не загружались на сервере.
```

## Регистрируем клавишу G

`KeyBindingHelper.registerKeyBinding` добавляет биндинг в настройки управления. Код клавиши G — 71, тип `InputUtil.Type.KEYSYM`.

```quiz
? Какой код в сценарии Byte соответствует G?
- 70
+ 71
- 72
> В учебном runtime используются числовые коды клавиш G и F.
```

## Переключаем значение

На `ClientTickEvents.END_CLIENT_TICK` проверяйте `wasPressed()` в цикле `while`, чтобы не потерять быстро повторённые нажатия. Меняйте boolean и отправляйте сообщение только если `client.player` существует.

```quiz
? Зачем здесь while, а не if?
- Чтобы пропустить первый тик
+ Чтобы обработать все накопленные нажатия
- Чтобы отключить клавиатуру
> wasPressed извлекает по одному событию нажатия.
```
