Создайте клиентский мод с клавишей G. После нажатия клавиши выведите локальному игроку `Ты нажал G!`. Повторные нажатия, произошедшие до следующего client tick, тоже должны быть обработаны.

Используйте `ClientModInitializer`, `KeyBindingHelper.registerKeyBinding` и `ClientTickEvents.END_CLIENT_TICK`. Проверьте, что `client.player` не `null`, и вызывайте `wasPressed()` в цикле `while`. Тестовый сценарий понимает `press G`, `press F` и `tick`.
