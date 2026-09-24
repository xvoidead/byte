**Метод** — именованный блок кода, который можно вызывать много раз. Методы помогают разбить большую задачу на маленькие понятные шаги и не повторять один и тот же код.

## Объявление метода

```java
public class Main {

    static int square(int x) {
        return x * x;
    }

    public static void main(String[] args) {
        System.out.println(square(5));   // 25
        System.out.println(square(12));  // 144
    }
}
```

Заголовок метода состоит из:

- `static` — метод принадлежит классу, и его можно вызвать из `main` без создания объекта (об объектах — в следующем уроке);
- `int` — **тип возвращаемого значения**;
- `square` — имя метода;
- `(int x)` — **параметры**: значения, которые метод получает при вызове.

`return` завершает метод и возвращает результат.

```quiz
? Что выведет программа?
    public class Main {
        static int twice(int x) {
            return x * 2;
        }
        public static void main(String[] args) {
            System.out.println(twice(twice(3)));
        }
    }
- 6
+ 12
- 9
- Ошибка компиляции
> Сначала вычисляется внутренний вызов `twice(3)` = 6, потом `twice(6)` = 12.
```

## Методы без результата

Если метод ничего не возвращает, его тип — `void`:

```java
static void greet(String name) {
    System.out.println("Привет, " + name + "!");
}

greet("Аня");  // Привет, Аня!
```

## Несколько параметров

```java
static double average(int a, int b, int c) {
    return (a + b + c) / 3.0;
}
```

## Методы, возвращающие boolean

Удобно выносить проверки в отдельные методы с понятными именами:

```java
static boolean isEven(int n) {
    return n % 2 == 0;
}

if (isEven(10)) {
    System.out.println("Чётное");
}
```

## Перегрузка

Можно объявить несколько методов с одним именем, но разными параметрами:

```java
static int max(int a, int b) { return a > b ? a : b; }
static double max(double a, double b) { return a > b ? a : b; }
```

## Локальные переменные

Переменные, объявленные внутри метода, видны только в нём. Параметры — это тоже локальные переменные: изменение параметра внутри метода не меняет переменную, которую передали при вызове.

```quiz
? Что выведет программа?
    public class Main {
        static void change(int x) {
            x = 100;
        }
        public static void main(String[] args) {
            int a = 5;
            change(a);
            System.out.println(a);
        }
    }
+ 5
- 100
- 0
- Ошибка компиляции
> В метод передаётся копия значения. Изменение параметра `x` внутри `change` не влияет на переменную `a`.
```

## Рекурсия

Метод может вызывать сам себя:

```java
static long factorial(int n) {
    if (n <= 1) {
        return 1;
    }
    return n * factorial(n - 1);
}
```
