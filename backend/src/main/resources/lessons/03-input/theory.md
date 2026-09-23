Программы становятся интереснее, когда могут получать данные от пользователя. Для чтения ввода в Java используется класс **Scanner**.

## Подключение Scanner

`Scanner` лежит в пакете `java.util`, поэтому его нужно **импортировать** в начале файла:

```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        // теперь можно читать данные
    }
}
```

`new Scanner(System.in)` создаёт объект, который читает стандартный ввод — то, что пользователь печатает с клавиатуры.

## Чтение данных

```java
int n = in.nextInt();         // следующее целое число
long big = in.nextLong();     // большое целое число
double d = in.nextDouble();   // дробное число
String word = in.next();      // следующее слово (до пробела)
String line = in.nextLine();  // вся строка целиком
```

Числа и слова могут разделяться пробелами или переводами строк — `nextInt()` сам их пропустит.

## Пример

```java
Scanner in = new Scanner(System.in);
String name = in.next();
int year = in.nextInt();
System.out.println("Привет, " + name + "! Тебе примерно " + (2026 - year) + " лет.");
```

## Ввод в нашей IDE

Во вкладке **«Ввод»** под редактором можно заранее написать данные, которые программа получит при запуске. Например:

```
Анна 2001
```

## Переполнение

Тип `int` вмещает числа только до 2 147 483 647. Если результат может быть больше, используйте `long`:

```java
int a = 100000;
int b = 300000;
System.out.println(a * b);          // -64771072 — переполнение!
System.out.println((long) a * b);   // 30000000000
```
