Массивы имеют фиксированный размер. На практике гораздо чаще используют **коллекции** из пакета `java.util` — они растут по мере необходимости и умеют многое «из коробки».

## ArrayList — список

```java
import java.util.ArrayList;
import java.util.List;

List<String> fruits = new ArrayList<>();
fruits.add("яблоко");
fruits.add("банан");
fruits.add("вишня");

System.out.println(fruits.size());      // 3
System.out.println(fruits.get(1));      // банан
fruits.remove("банан");
System.out.println(fruits.contains("вишня")); // true
System.out.println(fruits);             // [яблоко, вишня]

for (String fruit : fruits) {
    System.out.println(fruit);
}
```

В угловых скобках `<String>` указывается тип элементов. Коллекции хранят только объекты, поэтому для чисел используются классы-обёртки: `Integer` вместо `int`, `Double` вместо `double`:

```java
List<Integer> numbers = new ArrayList<>();
numbers.add(42);             // int автоматически превращается в Integer
int first = numbers.get(0);  // и обратно
```

## HashMap — словарь

`Map` хранит пары **ключ → значение**. По ключу можно быстро найти значение:

```java
import java.util.HashMap;
import java.util.Map;

Map<String, Integer> ages = new HashMap<>();
ages.put("Аня", 25);
ages.put("Боря", 31);
ages.put("Аня", 26);                  // значение по ключу «Аня» заменится

System.out.println(ages.get("Аня"));  // 26
System.out.println(ages.get("Вика")); // null — ключа нет
System.out.println(ages.containsKey("Боря")); // true
System.out.println(ages.getOrDefault("Вика", 0)); // 0
```

Перебор всех пар:

```java
for (Map.Entry<String, Integer> entry : ages.entrySet()) {
    System.out.println(entry.getKey() + " → " + entry.getValue());
}
```

## Порядок элементов

- `HashMap` **не гарантирует** порядок ключей.
- `LinkedHashMap` сохраняет порядок добавления.
- `TreeMap` хранит ключи **отсортированными**.

Все они реализуют интерфейс `Map`, поэтому заменить одну реализацию на другую можно, поменяв одно слово:

```java
Map<String, Integer> sorted = new TreeMap<>();
```

## Подсчёт с помощью Map

Частая задача — посчитать, сколько раз встречается каждый элемент:

```java
Map<Character, Integer> counts = new TreeMap<>();
for (char c : "мама".toCharArray()) {
    counts.put(c, counts.getOrDefault(c, 0) + 1);
}
System.out.println(counts); // {а=2, м=2}
```

Или короче — методом `merge`:

```java
counts.merge(c, 1, Integer::sum);
```
