Два вида классов встречаются так часто, что в Java для них есть специальный синтаксис: **записи** (`record`) для хранения данных и **перечисления** (`enum`) для набора заранее известных значений.

## record — класс для данных

Точка на плоскости — это просто два числа. Обычным классом пришлось бы писать поля, конструктор, методы доступа, `equals`, `hashCode` и `toString`. Запись делает всё это одной строкой:

```java
record Point(int x, int y) {
}

Point p = new Point(3, 4);
System.out.println(p.x());       // 3 — методы доступа без «get»
System.out.println(p);           // Point[x=3, y=4]
```

Поля записи неизменяемы: вместо изменения создают новую запись.

```java
record Point(int x, int y) {
    Point move(int dx, int dy) {
        return new Point(x + dx, y + dy);
    }
}
```

```quiz
? Что выведет программа?
    record Point(int x, int y) {
    }
    System.out.println(new Point(1, 2) + " " + new Point(1, 2).equals(new Point(1, 2)));
+ Point[x=1, y=2] true
- Point@1b6d3586 false
- (1, 2) true
- Point[x=1, y=2] false
> Запись сама получает понятный `toString` и `equals`, который сравнивает значения полей, а не ссылки.
```

## Проверки в записи

Если значения нужно проверить, используют **компактный конструктор** — без списка параметров:

```java
record Temperature(double celsius) {
    Temperature {
        if (celsius < -273.15) {
            throw new IllegalArgumentException("Ниже абсолютного нуля");
        }
    }
}
```

## enum — фиксированный набор значений

Дни недели, стороны света, статусы заказа — у таких величин заранее известный список вариантов:

```java
enum Direction {
    NORTH, EAST, SOUTH, WEST
}

Direction d = Direction.EAST;
if (d == Direction.EAST) {
    System.out.println("Идём на восток");
}
```

Значения перечисления сравнивают через `==`. Полезные методы:

```java
Direction.values()               // массив всех значений
Direction.valueOf("SOUTH")       // значение по имени
d.ordinal()                      // номер в списке, с нуля
d.name()                         // "EAST"
```

```quiz
? Что выведет программа?
    enum Size { S, M, L }
    System.out.println(Size.valueOf("M").ordinal() + " " + Size.values().length);
- 2 3
+ 1 3
- M 3
- 1 2
> `M` стоит в списке вторым — его номер 1 (счёт с нуля). Всего значений три.
```

## Поля и методы в enum

У перечисления могут быть поля, конструктор и методы:

```java
enum Planet {
    MERCURY(3.7), EARTH(9.8), MARS(3.7);

    private final double gravity;

    Planet(double gravity) {
        this.gravity = gravity;
    }

    double weight(double mass) {
        return mass * gravity;
    }
}
```

## enum и switch

Перечисления отлично работают с `switch` — компилятор даже подскажет, если вы забыли какое-то значение:

```java
String text = switch (d) {
    case NORTH -> "север";
    case EAST -> "восток";
    case SOUTH -> "юг";
    case WEST -> "запад";
};
```
