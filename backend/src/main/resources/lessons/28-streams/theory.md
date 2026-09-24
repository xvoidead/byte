Типичная задача: взять список, отобрать нужное, преобразовать и собрать результат. Циклами это делается в несколько строк с временными переменными. **Stream API** позволяет описать то же самое как конвейер.

```java
List<String> names = List.of("аня", "борис", "ваня", "григорий");

List<String> longNames = names.stream()
        .filter(name -> name.length() > 4)    // оставить длинные
        .map(String::toUpperCase)             // перевести в верхний регистр
        .sorted()                             // отсортировать
        .toList();                            // собрать в список

System.out.println(longNames);   // [БОРИС, ГРИГОРИЙ]
```

## Откуда берётся поток

```java
list.stream()                        // из любой коллекции
Arrays.stream(array)                 // из массива
Stream.of("a", "b", "c")             // из перечисленных значений
IntStream.rangeClosed(1, 100)        // числа от 1 до 100
text.lines()                         // строки текста
```

## Промежуточные операции

Они возвращают новый поток и их можно соединять в цепочку:

| Операция | Что делает |
|----------|-----------|
| `filter(условие)` | оставляет подходящие элементы |
| `map(функция)` | превращает каждый элемент |
| `sorted()` / `sorted(comparator)` | сортирует |
| `distinct()` | убирает повторы |
| `limit(n)` / `skip(n)` | первые n / пропустить n |
| `mapToInt(функция)` | превращает в поток чисел (для суммы, среднего) |

```quiz
? Что выведет программа?
    List<Integer> result = java.util.stream.Stream.of(5, 2, 8, 2, 9, 1)
            .filter(x -> x > 1)
            .distinct()
            .sorted()
            .map(x -> x * 10)
            .toList();
    System.out.println(result);
+ [20, 50, 80, 90]
- [50, 20, 80, 20, 90]
- [10, 20, 50, 80, 90]
- [20, 20, 50, 80, 90]
> После `filter` остаются 5, 2, 8, 2, 9; `distinct` убирает вторую двойку; `sorted` упорядочивает; `map` умножает на 10.
```

## Терминальные операции

Конвейер запускается только терминальной операцией, которая выдаёт результат:

```java
long count = names.stream().filter(n -> n.startsWith("а")).count();
int total = prices.stream().mapToInt(Integer::intValue).sum();
boolean hasEmpty = names.stream().anyMatch(String::isEmpty);
Optional<String> first = names.stream().filter(n -> n.length() > 10).findFirst();
String joined = String.join(", ", names.stream().sorted().toList());
Optional<Product> expensive = products.stream().max(Comparator.comparing(Product::price));
```

`Optional` — «коробка», в которой значение может быть, а может не быть: `first.isPresent()`, `first.orElse("никого")`.

## Ленивость

Промежуточные операции ничего не делают, пока не вызвана терминальная. А использованный поток нельзя запустить повторно — для новой обработки создают новый поток из коллекции.

```quiz
? Что произойдёт при повторной терминальной операции на том же потоке: `Stream<String> s = list.stream(); s.count(); s.count();`?
- Второй count вернёт то же число
- Второй count вернёт 0
+ Будет исключение IllegalStateException
- Код не скомпилируется
> Поток одноразовый: после терминальной операции он закрыт. Повторное использование бросает `IllegalStateException`.
```

## Когда поток, а когда цикл

Потоки хороши для отбора, преобразования и подсчётов. Если логика сложная — с несколькими изменяемыми переменными, ранними выходами и побочными эффектами, — обычный цикл часто читается проще. Выбирайте то, что понятнее.
