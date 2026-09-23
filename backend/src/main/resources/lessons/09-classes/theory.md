Java — **объектно-ориентированный** язык. Программа описывается как набор объектов, у которых есть **состояние** (данные) и **поведение** (методы).

## Класс и объект

**Класс** — это чертёж, а **объект** — конкретная вещь, построенная по чертежу.

```java
class Cat {
    String name;   // поля — состояние объекта
    int age;

    void meow() {  // метод — поведение
        System.out.println(name + ": Мяу!");
    }
}

public class Main {
    public static void main(String[] args) {
        Cat barsik = new Cat();   // создаём объект
        barsik.name = "Барсик";
        barsik.age = 3;
        barsik.meow();            // Барсик: Мяу!

        Cat murka = new Cat();    // другой объект со своими полями
        murka.name = "Мурка";
        murka.meow();             // Мурка: Мяу!
    }
}
```

Заметьте: у методов объекта нет слова `static` — они работают с полями конкретного объекта.

## Конструктор

**Конструктор** задаёт начальное состояние объекта. Его имя совпадает с именем класса, и у него нет типа результата:

```java
class Cat {
    String name;
    int age;

    Cat(String name, int age) {
        this.name = name;  // this.name — поле, name — параметр
        this.age = age;
    }
}

Cat cat = new Cat("Барсик", 3);
```

## Инкапсуляция

Хорошая практика — скрывать поля (`private`) и давать доступ к ним только через методы. Так объект сам следит, чтобы его состояние оставалось корректным:

```java
class Counter {
    private int value;

    void increment() {
        value++;
    }

    int getValue() {
        return value;
    }
}
```

Теперь никто снаружи не сможет записать в `value` отрицательное число — только увеличить его через `increment()`.

## Несколько классов в одном файле

В нашей IDE вся программа находится в одном файле. В нём может быть несколько классов, но `public` — только у одного, и он должен содержать метод `main`.

## Метод toString

Если объявить метод `toString`, объект можно будет печатать напрямую:

```java
class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}

System.out.println(new Point(3, 4)); // (3, 4)
```
