**Массив** — это набор значений одного типа, у каждого из которых есть номер (**индекс**).

## Создание массива

```java
int[] numbers = new int[5];            // пять нулей
int[] primes = {2, 3, 5, 7, 11};       // сразу со значениями
String[] names = {"Аня", "Боря", "Вика"};
```

Размер массива задаётся при создании и больше не меняется.

## Доступ к элементам

Индексы начинаются с **нуля**:

```java
int[] primes = {2, 3, 5, 7, 11};
System.out.println(primes[0]);      // 2
System.out.println(primes[4]);      // 11
primes[1] = 13;                     // замена элемента
System.out.println(primes.length);  // 5 — длина массива
```

Обращение к несуществующему индексу, например `primes[5]`, приведёт к ошибке `ArrayIndexOutOfBoundsException`.

## Перебор массива

Обычный цикл `for` по индексам:

```java
for (int i = 0; i < primes.length; i++) {
    System.out.println(i + ": " + primes[i]);
}
```

Цикл **for-each**, если индекс не нужен:

```java
int sum = 0;
for (int p : primes) {
    sum += p;
}
```

## Заполнение массива из ввода

```java
Scanner in = new Scanner(System.in);
int n = in.nextInt();
int[] a = new int[n];
for (int i = 0; i < n; i++) {
    a[i] = in.nextInt();
}
```

## Полезные методы класса Arrays

```java
import java.util.Arrays;

int[] a = {5, 2, 9, 1};
Arrays.sort(a);                            // сортировка
System.out.println(Arrays.toString(a));    // [1, 2, 5, 9]
```

## Форматированный вывод

Чтобы вывести дробное число с двумя знаками после точки, используйте `printf`:

```java
double avg = 3.4;
System.out.printf("Среднее: %.2f%n", avg);  // Среднее: 3.40
```

`%n` — перевод строки. На компьютере с русскими настройками `printf` может вывести запятую вместо точки (`3,40`). Чтобы результат не зависел от настроек системы, укажите локаль явно:

```java
import java.util.Locale;

System.out.printf(Locale.US, "Среднее: %.2f%n", avg);  // всегда 3.40
```
