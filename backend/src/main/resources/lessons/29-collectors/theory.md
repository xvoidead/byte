Метод `collect` превращает поток в готовый результат. Что именно получится, решает **коллектор** — а готовые коллекторы лежат в классе `Collectors`.

## Списки, множества и строки

```java
List<String> list = stream.collect(Collectors.toList());
Set<String> set = stream.collect(Collectors.toSet());
String line = stream.collect(Collectors.joining(", "));
```

`joining` умеет добавлять начало и конец:

```quiz
? Что выведет программа?
    System.out.println(java.util.stream.Stream.of("a", "b", "c")
            .collect(java.util.stream.Collectors.joining(", ", "[", "]")));
+ [a, b, c]
- a, b, c
- [a][b][c]
- [abc]
> Первый аргумент — разделитель, второй — начало, третий — конец строки.
```

## Группировка

`groupingBy` раскладывает элементы по «корзинам»: ключом становится результат функции, значением — список элементов с этим ключом.

```java
List<String> words = List.of("кот", "слон", "ёж", "мышь", "лев");
Map<Integer, List<String>> byLength = words.stream()
        .collect(Collectors.groupingBy(String::length));
// {2=[ёж], 3=[кот, лев], 4=[слон, мышь]}
```

Чтобы ключи шли по порядку, передайте фабрику `TreeMap`:

```java
Collectors.groupingBy(String::length, TreeMap::new, Collectors.toList())
```

## Считаем внутри групп

Вторым аргументом `groupingBy` можно сказать, что делать с каждой группой:

```java
Map<String, Long> count = orders.stream()
        .collect(Collectors.groupingBy(Order::city, Collectors.counting()));

Map<String, Integer> total = orders.stream()
        .collect(Collectors.groupingBy(Order::city, Collectors.summingInt(Order::amount)));

Map<String, Double> average = orders.stream()
        .collect(Collectors.groupingBy(Order::city, Collectors.averagingInt(Order::amount)));
```

## Два лагеря: partitioningBy

`partitioningBy` делит элементы на две группы по условию — с ключами `false` и `true`:

```quiz
? Что выведет программа?
    Map<Boolean, Long> parts = java.util.stream.Stream.of(1, 2, 3, 4, 5)
            .collect(java.util.stream.Collectors.partitioningBy(x -> x % 2 == 0,
                    java.util.stream.Collectors.counting()));
    System.out.println(parts);
+ {false=3, true=2}
- {true=2, false=3}
- {false=[1, 3, 5], true=[2, 4]}
- {1=3, 0=2}
> Нечётных (false) три, чётных (true) два. `counting()` заменяет списки на количество.
```

## Словарь из потока

`toMap` строит словарь по двум функциям — для ключа и для значения. Если ключи могут повторяться, нужна третья функция, объясняющая, как объединять значения:

```java
Map<String, Integer> totalByProduct = sales.stream()
        .collect(Collectors.toMap(Sale::product, Sale::quantity, Integer::sum));
```

## Перебор словаря по порядку

Результат группировки часто выводят отсортированным по ключу:

```java
new TreeMap<>(byCity).forEach((city, value) -> System.out.println(city + ": " + value));
```
