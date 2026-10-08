Fabric API предоставляет события действий с блоками. Для ломания есть `PlayerBlockBreakEvents`: до начала действия и после успешного завершения.

## BEFORE отменяет ломание

Callback `BEFORE` получает мир, игрока, координаты, состояние блока и, возможно, block entity. Возвращаемое значение определяет, разрешить ли действие:

```java
PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
    if (pos.getX() * pos.getX() + pos.getZ() * pos.getZ() <= 25) {
        player.sendMessage(Text.literal("Зона защищена"), false);
        return false;
    }
    return true;
});
```

`false` запрещает ломание. Если условие защиты не сработало, возвращайте `true`. В реальном API координаты `BlockPos` — целые числа.

```quiz
? Что означает false, возвращённое обработчиком PlayerBlockBreakEvents.BEFORE?
- Игроку выдали блок
+ Ломание отменено
- Мод выключен
> BEFORE использует boolean, чтобы разрешить или запретить действие.
```

## AFTER вызывается после успеха

`PlayerBlockBreakEvents.AFTER` сообщает, что блок действительно сломан. Используйте его для награды, статистики или звука:

```java
PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
    player.sendMessage(Text.literal("Блок добыт!"), false);
});
```

Если обработчик BEFORE вернул `false`, AFTER для этого блока не вызывается. В callback игрок имеет тип `PlayerEntity`; не полагайтесь на то, что это всегда `ServerPlayerEntity`.

```quiz
? Когда вызывается PlayerBlockBreakEvents.AFTER?
- Перед проверкой разрешения
- Даже если BEFORE запретил действие
+ После успешно завершившегося ломания
> AFTER предназначено для последствий уже выполненного действия.
```

## Геометрия защиты

Для круга вокруг точки (0, 0) не нужно вычислять квадратный корень. Если `x² + z² <= r²`, блок лежит внутри круга или на его границе. При радиусе 5 правая часть равна 25.

```java
int x = pos.getX();
int z = pos.getZ();
boolean protectedArea = x * x + z * z <= 25;
```

Не учитываем Y: защита распространяется на все высоты, но только на заданный горизонтальный радиус.

```quiz
? Попадает ли блок с координатами (x=3, z=4) в круг радиуса 5?
- Нет, расстояние больше 5
+ Да, он на границе
- Только если y равно нулю
> 3² + 4² = 25, значит расстояние ровно 5.
```
