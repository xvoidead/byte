Завершите первый Fabric-мод. При инициализации он должен один раз напечатать:

```text
Мод Byte загружен!
```

В `resources/fabric.mod.json` главный entrypoint `FirstMod` уже указан. Класс должен реализовать `ModInitializer`, а сообщение — печататься из `onInitialize()`. В Byte запускается облегчённая симуляция Fabric lifecycle; здесь не стартует настоящий клиент или сервер Minecraft.
