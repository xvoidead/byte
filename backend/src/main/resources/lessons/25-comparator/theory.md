Числа и строки Java умеет сортировать сама. Но как отсортировать учеников, товары или события? Нужно объяснить, в каком порядке они должны идти.

## Естественный порядок: Comparable

Класс может сам определить свой «естественный» порядок, реализовав интерфейс `Comparable` и метод `compareTo`:

```java
record Version(int major, int minor) implements Comparable<Version> {
    @Override
    public int compareTo(Version other) {
        if (major != other.major) {
            return Integer.compare(major, other.major);
        }
        return Integer.compare(minor, other.minor);
    }
}
```

`compareTo` возвращает отрицательное число, если `this` должен идти раньше, положительное — если позже, и 0 — если элементы равны по порядку. Строки и числа уже реализуют `Comparable`, поэтому `Collections.sort(names)` работает сразу.

```quiz
? Что должен вернуть `a.compareTo(b)`, если `a` должен стоять в отсортированном списке раньше `b`?
+ Отрицательное число
- Положительное число
- Ноль
- true
> Отрицательный результат означает «меньше», то есть раньше в порядке сортировки.
```

## Comparator: порядок снаружи

Часто нужен не один порядок: товары сортируют то по цене, то по названию. Для этого есть `Comparator` — объект, который сравнивает два элемента. Проще всего создать его через `Comparator.comparing`:

```java
record Product(String name, int price) {
}

List<Product> products = new ArrayList<>(...);
products.sort(Comparator.comparing(Product::price));      // по цене
products.sort(Comparator.comparing(Product::name));       // по названию
```

`Product::price` — это ссылка на метод: «возьми у товара цену». Подробнее о таких выражениях — в уроке про лямбды.

```quiz
? Что выведет программа?
    List<String> words = new ArrayList<>(List.of("кот", "слон", "ёж", "мышь"));
    words.sort(Comparator.comparing(String::length));
    System.out.println(words);
+ [ёж, кот, слон, мышь]
- [ёж, кот, мышь, слон]
- [кот, мышь, слон, ёж]
- [слон, мышь, кот, ёж]
> Слова сортируются по длине. У «слон» и «мышь» длина одинаковая, а сортировка в Java **стабильная** — равные элементы сохраняют исходный порядок.
```

## Несколько критериев

Если по первому признаку элементы равны, решает второй — `thenComparing`:

```java
students.sort(Comparator.comparing(Student::grade)
        .thenComparing(Student::name));
```

## По убыванию

`reversed()` переворачивает порядок:

```java
products.sort(Comparator.comparing(Product::price).reversed());   // от дорогих к дешёвым
```

Осторожно с `reversed()` в цепочке: он переворачивает **всё**, что написано до него. Чтобы по убыванию шёл только один критерий, передайте порядок вторым аргументом:

```java
students.sort(Comparator.comparing(Student::score, Comparator.reverseOrder())
        .thenComparing(Student::name));    // баллы по убыванию, имена по алфавиту
```

## Где ещё пригодится Comparator

```java
Collections.max(products, Comparator.comparing(Product::price));  // самый дорогой
new TreeSet<>(Comparator.comparing(String::length));             // множество, упорядоченное по длине
```
