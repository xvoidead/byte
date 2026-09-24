Программы работают с данными из внешнего мира, а там бывает всё: пользователь вводит буквы вместо чисел, файл не найден, делитель оказался нулём. В таких случаях Java создаёт **исключение** — объект, описывающий ошибку, — и прерывает обычный ход программы.

## Как выглядит исключение

Если исключение никто не обработал, программа завершается и печатает стек вызовов:

```
Exception in thread "main" java.lang.NumberFormatException: For input string: "12a"
	at java.lang.Integer.parseInt(Integer.java:661)
	at Main.main(Main.java:5)
```

Читать его нужно так: тип ошибки и сообщение — в первой строке, место в вашем коде — в строке `at Main...`.

Частые исключения:

| Исключение | Когда возникает |
|------------|----------------|
| `NumberFormatException` | строку не удалось превратить в число |
| `ArithmeticException` | деление целого числа на ноль |
| `ArrayIndexOutOfBoundsException` | индекс за пределами массива |
| `NullPointerException` | обращение к переменной, в которой `null` |
| `InputMismatchException` | `Scanner` ждал число, а получил текст |

## try и catch

Опасный код помещают в блок `try`, а обработку ошибки — в `catch`:

```java
try {
    int age = Integer.parseInt(text);
    System.out.println("Возраст: " + age);
} catch (NumberFormatException e) {
    System.out.println("Это не число: " + text);
}
```

Если в `try` произошло исключение, оставшийся код блока пропускается и выполняется `catch`. Если нет — `catch` пропускается. В обоих случаях программа продолжает работу после конструкции.

```quiz
? Что выведет программа?
    try {
        int x = Integer.parseInt("12a");
        System.out.println("число " + x);
    } catch (NumberFormatException e) {
        System.out.println("не число");
    }
    System.out.println("конец");
- число 12\nконец
+ не число\nконец
- не число
- Программа завершится с ошибкой
> `parseInt` бросает исключение, строка «число …» не выполняется, управление переходит в `catch`, а потом программа продолжается.
```

## finally

Блок `finally` выполняется всегда — была ошибка или нет, и даже если в `try` стоит `return`. В нём освобождают ресурсы: закрывают файлы, соединения.

```quiz
? Что выведет программа?
    public class Main {
        static int f() {
            try {
                return 1;
            } finally {
                System.out.print("finally ");
            }
        }
        public static void main(String[] args) {
            System.out.println(f());
        }
    }
- 1
+ finally 1
- 1 finally
- finally
> `finally` выполняется перед тем, как метод действительно вернёт значение, поэтому «finally» печатается раньше единицы.
```

## Несколько catch

Разные ошибки можно обрабатывать по-разному:

```java
try {
    int[] data = new int[3];
    data[index] = 100 / divisor;
} catch (ArithmeticException e) {
    System.out.println("Делить на ноль нельзя");
} catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("Нет такого элемента");
}
```

Исключения образуют иерархию: все они наследники `Exception`. Блок `catch (Exception e)` поймает любое — но так легко скрыть настоящую ошибку в коде. Ловите только то, что умеете обработать.

## Проверяемые и непроверяемые

- **Непроверяемые** (наследники `RuntimeException`) — обычно ошибки в коде: `NullPointerException`, индекс за границами. Компилятор не заставляет их ловить.
- **Проверяемые** — внешние проблемы вроде `IOException`. Их нужно либо поймать, либо объявить в заголовке метода: `void load() throws IOException`.
