Часто классы похожи: у кошки и собаки есть имя и возраст, у круга и квадрата — площадь. **Наследование** позволяет описать общее один раз в родительском классе, а в дочерних — только различия.

## extends

```java
class Animal {
    String name;

    void describe() {
        System.out.println("Это " + name);
    }
}

class Dog extends Animal {
    void bark() {
        System.out.println(name + ": Гав!");
    }
}

Dog dog = new Dog();
dog.name = "Шарик";
dog.describe();   // Это Шарик — метод унаследован от Animal
dog.bark();       // Шарик: Гав!
```

`Dog extends Animal` значит «собака — это животное». Всё, что есть у `Animal`, есть и у `Dog`. У класса может быть только один родитель.

## super и конструкторы

Конструктор дочернего класса сначала вызывает конструктор родителя — через `super(...)`:

```java
class Animal {
    String name;
    Animal(String name) {
        this.name = name;
    }
}

class Cat extends Animal {
    Cat(String name) {
        super(name);           // вызывается первым
    }
}
```

Если `super(...)` не написан явно, Java вызывает конструктор родителя без параметров.

```quiz
? Что выведет программа?
    class A {
        A() {
            System.out.print("A");
        }
    }
    class B extends A {
        B() {
            System.out.print("B");
        }
    }
    public class Main {
        public static void main(String[] args) {
            new B();
        }
    }
+ AB
- B
- BA
- A
> Конструктор `B` неявно начинается с `super()`, поэтому сначала выполняется конструктор `A`, а потом — остальная часть конструктора `B`.
```

## Переопределение методов

Дочерний класс может заменить поведение родителя, **переопределив** метод. Аннотация `@Override` просит компилятор проверить, что такой метод у родителя действительно есть:

```java
class Animal {
    String sound() {
        return "...";
    }
}

class Cat extends Animal {
    @Override
    String sound() {
        return "Мяу";
    }
}
```

Самое интересное: переменная типа `Animal` может хранить кошку, и вызовется метод **кошки**. Это называется **полиморфизм**:

```java
Animal pet = new Cat();
System.out.println(pet.sound()); // Мяу
```

```quiz
? Что выведет программа?
    class Animal {
        String sound() {
            return "...";
        }
    }
    class Cat extends Animal {
        @Override
        String sound() {
            return "Мяу";
        }
    }
    public class Main {
        public static void main(String[] args) {
            Animal a = new Cat();
            System.out.println(a.sound());
        }
    }
- ...
+ Мяу
- Ошибка компиляции
- null
> Какой метод вызвать, решается по настоящему объекту, а не по типу переменной. Объект — кошка, поэтому выполняется `Cat.sound()`.
```

## Абстрактные классы

Иногда общий родитель — это идея, а не конкретная вещь. Нет «просто фигуры», есть круг и квадрат. Такой класс объявляют `abstract`: создать его объект нельзя, зато можно потребовать, чтобы наследники реализовали нужные методы.

```java
abstract class Shape {
    abstract double area();      // без тела — реализуют наследники

    String describe() {
        return "Площадь: " + area();
    }
}

class Circle extends Shape {
    private final double r;

    Circle(double r) {
        this.r = r;
    }

    @Override
    double area() {
        return Math.PI * r * r;
    }
}
```

## Наследование наследника

Цепочка может быть длиннее: квадрат — это прямоугольник, у которого стороны равны.

```java
class Square extends Rectangle {
    Square(double side) {
        super(side, side);
    }
}
```

Модификатор `protected` делает поле видимым для наследников, но не для всех остальных классов.
