До сих пор исключения бросала стандартная библиотека. Но и ваш код может сообщать о проблемах: «на счёте недостаточно средств», «пользователь не найден», «неверный формат». Для этого есть оператор `throw`.

## throw

```java
static int divide(int a, int b) {
    if (b == 0) {
        throw new IllegalArgumentException("делить на ноль нельзя");
    }
    return a / b;
}
```

`throw` немедленно прерывает метод, как `return`, но вместо значения «выбрасывает» исключение вызывающему коду. Тот может его поймать:

```quiz
? Что выведет программа?
    public class Main {
        static int divide(int a, int b) {
            if (b == 0) {
                throw new IllegalArgumentException("делить на ноль нельзя");
            }
            return a / b;
        }
        public static void main(String[] args) {
            try {
                System.out.println(divide(10, 0));
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }
- 0
+ Ошибка: делить на ноль нельзя
- Ошибка: java.lang.IllegalArgumentException
- Exception in thread "main" ...
> `divide` бросает исключение, `println` в `try` не выполняется, а `catch` печатает сообщение, переданное в конструктор исключения.
```

Для типичных ситуаций подходят готовые исключения: `IllegalArgumentException` — неверный аргумент, `IllegalStateException` — объект в неподходящем состоянии.

## Свой класс исключения

Когда ошибка относится к предметной области, лучше создать свой класс — так код понятнее, а ошибки можно ловить избирательно:

```java
class InsufficientFundsException extends RuntimeException {
    InsufficientFundsException(String message) {
        super(message);
    }
}
```

Исключение — обычный класс: у него могут быть поля и методы. Сообщение передаётся родителю через `super(message)` и потом доступно через `getMessage()`.

## Иерархия своих исключений

Ошибки одной области удобно собрать под общим родителем:

```java
class BankException extends RuntimeException {
    BankException(String message) {
        super(message);
    }
}

class AccountNotFoundException extends BankException { ... }
class InsufficientFundsException extends BankException { ... }
```

Теперь можно поймать любую банковскую ошибку одним блоком `catch (BankException e)`, а при необходимости — только конкретную.

## Проверяемые свои исключения

Если наследоваться не от `RuntimeException`, а от `Exception`, исключение становится **проверяемым**: компилятор потребует либо поймать его, либо объявить в заголовке метода.

```java
class LimitException extends Exception { ... }

void withdraw(int amount) throws LimitException {
    if (amount > limit) {
        throw new LimitException("превышен лимит");
    }
}
```

```quiz
? Что нужно сделать, чтобы метод мог бросить проверяемое исключение `LimitException extends Exception`?
- Ничего, компилятор не проверяет исключения
+ Объявить его в заголовке метода: throws LimitException
- Унаследовать метод от Exception
- Поставить перед методом слово static
> Проверяемые исключения должны быть либо пойманы внутри метода, либо объявлены после параметров через `throws`.
```

## Когда бросать, а когда ловить

- Бросайте исключение там, где обнаружили проблему, но **не знаете, как её исправить**.
- Ловите там, где **знаете, что делать**: показать сообщение пользователю, повторить попытку, выбрать другой вариант.
- Не глотайте исключения молча: пустой `catch` прячет ошибки.
