Иногда в метод нужно передать не данные, а **поведение**: как сравнивать, что проверять, что сделать с каждым элементом. Для этого в Java есть **лямбда-выражения** — короткая запись функции.

## Синтаксис

```java
x -> x * x                         // один параметр — скобки не обязательны
(a, b) -> a + b                    // несколько параметров
() -> System.out.println("Привет") // без параметров
s -> {                             // несколько действий — фигурные скобки и return
    String trimmed = s.trim();
    return trimmed.toUpperCase();
}
```

Слева от стрелки — параметры, справа — результат или блок кода.

## Функциональные интерфейсы

Лямбду можно сохранить в переменную, если её тип — **функциональный интерфейс**, то есть интерфейс с одним абстрактным методом. Самые полезные уже есть в пакете `java.util.function`:

| Интерфейс | Что делает | Метод | Пример |
|-----------|-----------|-------|--------|
| `Function<T, R>` | превращает T в R | `apply` | `s -> s.length()` |
| `Predicate<T>` | проверяет условие | `test` | `n -> n > 0` |
| `Consumer<T>` | что-то делает с T | `accept` | `s -> System.out.println(s)` |
| `Supplier<T>` | выдаёт значение | `get` | `() -> new ArrayList<>()` |
| `BinaryOperator<T>` | из двух T делает T | `apply` | `(a, b) -> a * b` |

```java
Predicate<String> isLong = s -> s.length() > 5;
System.out.println(isLong.test("лямбда"));    // true

BinaryOperator<Integer> multiply = (a, b) -> a * b;
System.out.println(multiply.apply(6, 7));     // 42
```

Функции можно соединять:

```quiz
? Что выведет программа?
    java.util.function.Function<Integer, Integer> twice = x -> x * 2;
    java.util.function.Function<Integer, Integer> plusOne = x -> x + 1;
    System.out.println(twice.andThen(plusOne).apply(5) + " " + twice.compose(plusOne).apply(5));
+ 11 12
- 12 11
- 11 11
- 12 12
> `andThen`: сначала удвоить, потом прибавить единицу — 11. `compose`: сначала прибавить единицу, потом удвоить — 12.
```

## Ссылки на методы

Если лямбда просто вызывает готовый метод, её можно записать ещё короче:

```java
s -> s.length()              →  String::length
s -> Integer.parseInt(s)     →  Integer::parseInt
x -> System.out.println(x)   →  System.out::println
() -> new ArrayList<>()      →  ArrayList::new
```

## Лямбды и коллекции

Многие методы коллекций принимают поведение:

```java
List<Integer> numbers = new ArrayList<>(List.of(3, -1, 4, -5));
numbers.removeIf(n -> n < 0);               // удалить отрицательные
numbers.forEach(n -> System.out.println(n));
numbers.sort(Comparator.comparing(n -> -n)); // по убыванию

Map<String, Integer> ages = Map.of("Аня", 25, "Боря", 31);
ages.forEach((name, age) -> System.out.println(name + ": " + age));
```

## Переменные из окружения

Лямбда может использовать переменные из метода, где она создана, но только те, которые после присваивания больше не меняются (**effectively final**):

```java
int limit = 10;
Predicate<Integer> small = n -> n < limit;   // можно
// limit = 20;  — после этого строка выше перестанет компилироваться
```

```quiz
? Какие локальные переменные метода можно использовать внутри лямбды?
- Любые
- Только объявленные с final
+ Те, что не меняются после присваивания (final или effectively final)
- Никакие — только параметры лямбды
> Лямбда захватывает значение переменной, поэтому переменная не должна меняться — ни до, ни после создания лямбды.
```

## Таблица поведения

Лямбды можно хранить в коллекциях. Например, вместо длинного `switch` — словарь «название → действие»:

```java
Map<String, BinaryOperator<Integer>> operations = new HashMap<>();
operations.put("+", (a, b) -> a + b);
operations.put("*", (a, b) -> a * b);

int result = operations.get("*").apply(6, 7);   // 42
```

Добавить новую операцию — значит добавить одну строку, не трогая остальной код.
