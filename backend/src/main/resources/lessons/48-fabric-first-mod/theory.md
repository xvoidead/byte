Fabric — загрузчик модов для Minecraft. Он позволяет добавлять контент и менять поведение игры через события и API. Обычный Bukkit-плагин работает на сервере; Fabric-мод может запускаться на сервере, клиенте или обеих сторонах.

## Entry point мода

Fabric Loader создаёт класс, указанный в `fabric.mod.json`, и вызывает его метод инициализации. Общая точка входа — интерфейс `ModInitializer`:

```java
import net.fabricmc.api.ModInitializer;

public class FirstMod implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("Мод загружен");
    }
}
```

`onInitialize()` — место для регистрации событий и компонентов. Не запускайте длинный цикл и не ждите там ввода: игра управляет собственным главным потоком.

```quiz
? Какой метод вызывает Fabric у класса, реализующего ModInitializer?
- onEnable()
+ onInitialize()
- main()
> onInitialize — общая точка входа мода. У Bukkit-плагина похожую роль играет onEnable.
```

## Метаданные fabric.mod.json

Fabric Loader читает JSON-файл `fabric.mod.json` из ресурсов мода. В нём находятся id, версия, зависимости и entrypoints:

```json
{
  "schemaVersion": 1,
  "id": "byte_example",
  "version": "1.0.0",
  "name": "Byte Example",
  "entrypoints": {
    "main": ["FirstMod"]
  },
  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft": "~1.21.1",
    "java": ">=21",
    "fabric-api": "*"
  }
}
```

`id` — стабильный машинный идентификатор в нижнем регистре; `main` — список общих entrypoint-классов. В настоящем проекте Gradle Loom подготавливает Minecraft и Fabric API. В Byte эти метаданные остаются в проекте, а тестовый runtime проверяет Java-логику без запуска игры.

```quiz
? Какой ключ в fabric.mod.json указывает общий entrypoint?
- mainClass
+ entrypoints.main
- plugin.yml
> Fabric Loader ищет классы в массиве entrypoints.main.
```

## Настройка проекта и ограничение симулятора

Настоящий Fabric-мод собирают Gradle Loom с Minecraft и Fabric API. Byte запускает код в Java-песочнице, поэтому здесь доступны только lifecycle и игровые события, нужные упражнениям. Это позволяет проверять обработчики быстро и одинаково для всех, но не заменяет тест в реальной сборке игры.

В этом курсе мы будем добавлять серверные события, таймеры и клиентскую клавишу. Для публикации мода перенесите код в Loom-проект и укажите его настоящий пакет в `fabric.mod.json`.

```quiz
? Что запускает сайт Byte при проверке Fabric-урока?
- Полный клиент Minecraft
- Dedicated server со всеми модами
+ Небольшую симуляцию изучаемых Fabric callbacks
> Симулятор проверяет только те callbacks, которые нужны заданиям; он не загружает игровой движок.
```
