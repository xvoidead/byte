Строки (`String`) — один из самых используемых типов в Java. Строка — это последовательность символов.

## Основные методы

```java
String s = "Привет, Java!";

s.length();              // 13 — длина
s.charAt(0);             // 'П' — символ по индексу (с нуля)
s.toUpperCase();         // "ПРИВЕТ, JAVA!"
s.toLowerCase();         // "привет, java!"
s.contains("Java");      // true
s.indexOf("Java");       // 8 — позиция подстроки, или -1
s.substring(8, 12);      // "Java" — с 8 по 11 включительно
s.replace("Java", "мир");// "Привет, мир!"
s.trim();                // убирает пробелы по краям
s.split(", ");           // массив {"Привет", "Java!"}
s.isEmpty();             // false
```

## Строки неизменяемы

Методы `String` не меняют исходную строку, а возвращают **новую**:

```java
String name = "java";
name.toUpperCase();           // результат потерян
name = name.toUpperCase();    // теперь name == "JAVA"
```

```quiz
? Что выведет программа?
    String s = "java";
    s.toUpperCase();
    System.out.println(s);
+ java
- JAVA
- Java
- Ошибка компиляции
> Строки неизменяемы: `toUpperCase()` вернула новую строку, но её никуда не сохранили. Нужно написать `s = s.toUpperCase();`.
```

## Сравнение строк

Строки сравнивают методом `equals`, а не `==`:

```java
String a = "кот";
String b = new String("кот");
System.out.println(a == b);       // false — разные объекты!
System.out.println(a.equals(b));  // true — одинаковый текст
System.out.println("Кот".equalsIgnoreCase("кот")); // true
```

`==` проверяет, что это один и тот же объект в памяти, а `equals` — что совпадает содержимое.

```quiz
? Как правильно проверить, что строка `answer` равна «да»?
- answer == "да"
+ answer.equals("да")
- answer = "да"
- answer.compare("да")
> `==` сравнивает ссылки на объекты, а не текст. Для сравнения содержимого строк используйте `equals`.
```

## Перебор символов

```java
String word = "код";
for (int i = 0; i < word.length(); i++) {
    char c = word.charAt(i);
    System.out.println(c);
}
```

## StringBuilder

Если строку нужно собирать по частям (например, в цикле), используйте `StringBuilder` — это быстрее, чем много раз склеивать `String`:

```java
StringBuilder sb = new StringBuilder();
for (int i = 1; i <= 3; i++) {
    sb.append(i).append(" ");
}
System.out.println(sb.toString()); // "1 2 3 "

new StringBuilder("abc").reverse().toString(); // "cba"
```

## Чтение строки целиком

```java
Scanner in = new Scanner(System.in);
String line = in.nextLine(); // вся строка, включая пробелы
```
