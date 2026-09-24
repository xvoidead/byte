// Словарь методов стандартных классов с описаниями на русском — для автодополнения после точки
// и подсказок при наведении. Это не полноценный анализ Java, а практичная эвристика для учебного кода:
// тип переменной берётся из её объявления, а типы результатов методов — из этого словаря.

export interface Member {
  name: string;
  /** Параметры для подписи, например "int index". Пустая строка — без параметров. null — это поле. */
  params: string | null;
  /** Тип результата. Параметры типа (E, K, V, T) подставляются из объявления переменной. */
  returns: string;
  doc: string;
  isStatic?: boolean;
}

function m(name: string, params: string | null, returns: string, doc: string, isStatic = false): Member {
  return { name, params, returns, doc, isStatic };
}

/** Параметры типа обобщённых классов — чтобы из List<String> понять, что get() вернёт String. */
const TYPE_PARAMS: Record<string, string[]> = {
  List: ['E'],
  ArrayList: ['E'],
  LinkedList: ['E'],
  Set: ['E'],
  HashSet: ['E'],
  TreeSet: ['E'],
  LinkedHashSet: ['E'],
  Map: ['K', 'V'],
  HashMap: ['K', 'V'],
  TreeMap: ['K', 'V'],
  LinkedHashMap: ['K', 'V'],
  Stream: ['T'],
  Optional: ['T'],
  Iterator: ['E'],
  Entry: ['K', 'V'],
  'Map.Entry': ['K', 'V'],
};

const collection: Member[] = [
  m('add', 'E element', 'boolean', 'Добавляет элемент в конец.'),
  m('size', '', 'int', 'Количество элементов.'),
  m('isEmpty', '', 'boolean', 'true, если элементов нет.'),
  m('contains', 'Object element', 'boolean', 'true, если такой элемент есть.'),
  m('remove', 'Object element', 'boolean', 'Удаляет первое вхождение элемента.'),
  m('clear', '', 'void', 'Удаляет все элементы.'),
  m('addAll', 'Collection<E> other', 'boolean', 'Добавляет все элементы другой коллекции.'),
  m('stream', '', 'Stream<E>', 'Поток элементов для Stream API: filter, map, collect…'),
  m('forEach', 'Consumer<E> action', 'void', 'Выполняет действие для каждого элемента: list.forEach(x -> ...).'),
  m('iterator', '', 'Iterator<E>', 'Итератор для обхода элементов.'),
  m('removeIf', 'Predicate<E> filter', 'boolean', 'Удаляет элементы, для которых условие истинно.'),
  m('toArray', '', 'Object[]', 'Копирует элементы в массив.'),
];

const list: Member[] = [
  ...collection,
  m('get', 'int index', 'E', 'Элемент по индексу (с нуля).'),
  m('set', 'int index, E element', 'E', 'Заменяет элемент по индексу и возвращает старый.'),
  m('add', 'int index, E element', 'void', 'Вставляет элемент по индексу, сдвигая остальные.'),
  m('remove', 'int index', 'E', 'Удаляет элемент по индексу и возвращает его.'),
  m('indexOf', 'Object element', 'int', 'Индекс первого вхождения или -1.'),
  m('lastIndexOf', 'Object element', 'int', 'Индекс последнего вхождения или -1.'),
  m('sort', 'Comparator<E> comparator', 'void', 'Сортирует список; null — в естественном порядке.'),
  m('subList', 'int from, int to', 'List<E>', 'Часть списка с from по to - 1.'),
  m('getFirst', '', 'E', 'Первый элемент.'),
  m('getLast', '', 'E', 'Последний элемент.'),
];

const set: Member[] = [...collection];

const map: Member[] = [
  m('put', 'K key, V value', 'V', 'Кладёт значение по ключу, заменяя старое.'),
  m('get', 'Object key', 'V', 'Значение по ключу или null, если ключа нет.'),
  m('getOrDefault', 'Object key, V defaultValue', 'V', 'Значение по ключу или значение по умолчанию.'),
  m('containsKey', 'Object key', 'boolean', 'true, если такой ключ есть.'),
  m('containsValue', 'Object value', 'boolean', 'true, если такое значение есть.'),
  m('remove', 'Object key', 'V', 'Удаляет пару по ключу.'),
  m('size', '', 'int', 'Количество пар.'),
  m('isEmpty', '', 'boolean', 'true, если пар нет.'),
  m('keySet', '', 'Set<K>', 'Множество всех ключей.'),
  m('values', '', 'List<V>', 'Все значения.'),
  m('entrySet', '', 'Set<Map.Entry<K, V>>', 'Все пары ключ → значение для перебора.'),
  m('merge', 'K key, V value, BiFunction remap', 'V', 'Объединяет значения: map.merge(word, 1, Integer::sum) — счётчик.'),
  m('putIfAbsent', 'K key, V value', 'V', 'Кладёт значение, только если ключа ещё нет.'),
  m('computeIfAbsent', 'K key, Function mapping', 'V', 'Возвращает значение по ключу, создав его при отсутствии.'),
  m('forEach', 'BiConsumer<K, V> action', 'void', 'Действие для каждой пары: map.forEach((k, v) -> ...).'),
  m('clear', '', 'void', 'Удаляет все пары.'),
];

const treeMapExtra: Member[] = [
  m('firstKey', '', 'K', 'Наименьший ключ.'),
  m('lastKey', '', 'K', 'Наибольший ключ.'),
];

const stream: Member[] = [
  m('filter', 'Predicate<T> condition', 'Stream<T>', 'Оставляет элементы, для которых условие истинно.'),
  m('map', 'Function<T, R> mapper', 'Stream', 'Преобразует каждый элемент.'),
  m('mapToInt', 'ToIntFunction<T> mapper', 'IntStream', 'Преобразует в поток чисел int (для sum, average).'),
  m('sorted', '', 'Stream<T>', 'Сортирует элементы; можно передать Comparator.'),
  m('distinct', '', 'Stream<T>', 'Убирает повторы.'),
  m('limit', 'long n', 'Stream<T>', 'Первые n элементов.'),
  m('skip', 'long n', 'Stream<T>', 'Пропускает первые n элементов.'),
  m('forEach', 'Consumer<T> action', 'void', 'Действие для каждого элемента.'),
  m('collect', 'Collector collector', 'Object', 'Собирает результат: Collectors.toList(), groupingBy…'),
  m('toList', '', 'List<T>', 'Собирает элементы в неизменяемый список.'),
  m('count', '', 'long', 'Количество элементов.'),
  m('reduce', 'T identity, BinaryOperator<T> op', 'T', 'Сворачивает элементы в одно значение.'),
  m('anyMatch', 'Predicate<T> condition', 'boolean', 'true, если условие верно хотя бы для одного.'),
  m('allMatch', 'Predicate<T> condition', 'boolean', 'true, если условие верно для всех.'),
  m('noneMatch', 'Predicate<T> condition', 'boolean', 'true, если условие не верно ни для одного.'),
  m('findFirst', '', 'Optional<T>', 'Первый элемент, завёрнутый в Optional.'),
  m('max', 'Comparator<T> comparator', 'Optional<T>', 'Наибольший элемент.'),
  m('min', 'Comparator<T> comparator', 'Optional<T>', 'Наименьший элемент.'),
];

const intStream: Member[] = [
  m('sum', '', 'int', 'Сумма чисел.'),
  m('average', '', 'OptionalDouble', 'Среднее значение.'),
  m('max', '', 'OptionalInt', 'Наибольшее число.'),
  m('min', '', 'OptionalInt', 'Наименьшее число.'),
  m('count', '', 'long', 'Количество чисел.'),
  m('filter', 'IntPredicate condition', 'IntStream', 'Оставляет числа, для которых условие истинно.'),
  m('map', 'IntUnaryOperator mapper', 'IntStream', 'Преобразует каждое число.'),
  m('boxed', '', 'Stream<Integer>', 'Превращает в Stream<Integer>.'),
  m('toArray', '', 'int[]', 'Собирает в массив.'),
  m('forEach', 'IntConsumer action', 'void', 'Действие для каждого числа.'),
];

const optional: Member[] = [
  m('isPresent', '', 'boolean', 'true, если значение есть.'),
  m('isEmpty', '', 'boolean', 'true, если значения нет.'),
  m('get', '', 'T', 'Значение (бросает исключение, если его нет).'),
  m('orElse', 'T other', 'T', 'Значение или other, если значения нет.'),
  m('ifPresent', 'Consumer<T> action', 'void', 'Выполняет действие, если значение есть.'),
  m('map', 'Function<T, R> mapper', 'Optional', 'Преобразует значение, если оно есть.'),
];

const optionalNumber = (type: string): Member[] => [
  m('getAsInt', '', 'int', 'Значение (для OptionalInt).'),
  m('getAsDouble', '', 'double', 'Значение (для OptionalDouble).'),
  m('orElse', `${type} other`, type, 'Значение или other, если значения нет.'),
  m('isPresent', '', 'boolean', 'true, если значение есть.'),
];

const string: Member[] = [
  m('length', '', 'int', 'Длина строки.'),
  m('charAt', 'int index', 'char', 'Символ по индексу (с нуля).'),
  m('substring', 'int begin, int end', 'String', 'Подстрока с begin по end - 1. Без end — до конца строки.'),
  m('indexOf', 'String part', 'int', 'Позиция первого вхождения или -1.'),
  m('lastIndexOf', 'String part', 'int', 'Позиция последнего вхождения или -1.'),
  m('contains', 'CharSequence part', 'boolean', 'true, если строка содержит part.'),
  m('startsWith', 'String prefix', 'boolean', 'true, если строка начинается с prefix.'),
  m('endsWith', 'String suffix', 'boolean', 'true, если строка заканчивается на suffix.'),
  m('equals', 'Object other', 'boolean', 'Сравнивает содержимое строк. Для строк используйте его, а не ==.'),
  m('equalsIgnoreCase', 'String other', 'boolean', 'Сравнивает без учёта регистра.'),
  m('compareTo', 'String other', 'int', 'Сравнение по алфавиту: < 0, 0 или > 0.'),
  m('isEmpty', '', 'boolean', 'true, если длина 0.'),
  m('isBlank', '', 'boolean', 'true, если строка пустая или из одних пробелов.'),
  m('toUpperCase', '', 'String', 'Копия строки в верхнем регистре.'),
  m('toLowerCase', '', 'String', 'Копия строки в нижнем регистре.'),
  m('trim', '', 'String', 'Копия без пробелов по краям.'),
  m('strip', '', 'String', 'Копия без пробельных символов по краям (учитывает Unicode).'),
  m('replace', 'CharSequence from, CharSequence to', 'String', 'Заменяет все вхождения from на to.'),
  m('split', 'String regex', 'String[]', 'Делит строку на части по разделителю: "a b".split(" ").'),
  m('toCharArray', '', 'char[]', 'Массив символов строки.'),
  m('chars', '', 'IntStream', 'Поток кодов символов.'),
  m('repeat', 'int count', 'String', 'Строка, повторённая count раз.'),
  m('matches', 'String regex', 'boolean', 'true, если строка целиком подходит под регулярное выражение.'),
  m('formatted', 'Object... args', 'String', 'Подставляет значения: "%d лет".formatted(age).'),
  m('hashCode', '', 'int', 'Хеш-код строки.'),
];

const stringStatic: Member[] = [
  m('valueOf', 'Object value', 'String', 'Превращает значение в строку.', true),
  m('join', 'CharSequence separator, Iterable elements', 'String', 'Склеивает элементы через разделитель.', true),
  m('format', 'String format, Object... args', 'String', 'Строка по шаблону: String.format("%.2f", x).', true),
];

const stringBuilder: Member[] = [
  m('append', 'Object value', 'StringBuilder', 'Добавляет значение в конец. Можно вызывать цепочкой.'),
  m('insert', 'int index, Object value', 'StringBuilder', 'Вставляет значение по индексу.'),
  m('reverse', '', 'StringBuilder', 'Разворачивает строку задом наперёд.'),
  m('toString', '', 'String', 'Готовая строка.'),
  m('length', '', 'int', 'Текущая длина.'),
  m('charAt', 'int index', 'char', 'Символ по индексу.'),
  m('setCharAt', 'int index, char c', 'void', 'Заменяет символ по индексу.'),
  m('deleteCharAt', 'int index', 'StringBuilder', 'Удаляет символ по индексу.'),
  m('setLength', 'int length', 'void', 'Обрезает или дополняет до длины; setLength(0) — очистить.'),
];

const scanner: Member[] = [
  m('nextInt', '', 'int', 'Читает следующее целое число.'),
  m('nextLong', '', 'long', 'Читает следующее большое целое число.'),
  m('nextDouble', '', 'double', 'Читает следующее дробное число.'),
  m('next', '', 'String', 'Читает следующее слово (до пробела).'),
  m('nextLine', '', 'String', 'Читает строку целиком, до перевода строки.'),
  m('nextBoolean', '', 'boolean', 'Читает true или false.'),
  m('hasNext', '', 'boolean', 'true, если во вводе есть ещё слово.'),
  m('hasNextInt', '', 'boolean', 'true, если следующее во вводе — целое число.'),
  m('hasNextLine', '', 'boolean', 'true, если во вводе есть ещё строка.'),
  m('close', '', 'void', 'Закрывает Scanner.'),
];

const printStream: Member[] = [
  m('println', 'Object value', 'void', 'Печатает значение и переходит на новую строку.'),
  m('print', 'Object value', 'void', 'Печатает значение без перевода строки.'),
  m('printf', 'String format, Object... args', 'PrintStream', 'Печать по шаблону: %d — целое, %.2f — дробное, %s — строка, %n — перевод строки.'),
  m('flush', '', 'void', 'Сразу отправляет накопленный вывод.'),
];

const system: Member[] = [
  m('out', null, 'PrintStream', 'Стандартный вывод: System.out.println(...).', true),
  m('err', null, 'PrintStream', 'Вывод ошибок.', true),
  m('in', null, 'InputStream', 'Стандартный ввод — передайте его в new Scanner(System.in).', true),
  m('currentTimeMillis', '', 'long', 'Текущее время в миллисекундах.', true),
  m('nanoTime', '', 'long', 'Точный таймер в наносекундах — для замера времени.', true),
  m('lineSeparator', '', 'String', 'Перевод строки текущей системы.', true),
  m('exit', 'int status', 'void', 'Завершает программу с кодом status.', true),
];

const math: Member[] = [
  m('abs', 'int value', 'int', 'Модуль числа.', true),
  m('max', 'int a, int b', 'int', 'Большее из двух чисел.', true),
  m('min', 'int a, int b', 'int', 'Меньшее из двух чисел.', true),
  m('pow', 'double base, double exponent', 'double', 'Возведение в степень.', true),
  m('sqrt', 'double value', 'double', 'Квадратный корень.', true),
  m('cbrt', 'double value', 'double', 'Кубический корень.', true),
  m('round', 'double value', 'long', 'Округление до ближайшего целого.', true),
  m('floor', 'double value', 'double', 'Округление вниз.', true),
  m('ceil', 'double value', 'double', 'Округление вверх.', true),
  m('random', '', 'double', 'Случайное число от 0 (включительно) до 1 (не включительно).', true),
  m('hypot', 'double x, double y', 'double', 'Длина гипотенузы: √(x² + y²).', true),
  m('floorMod', 'int a, int b', 'int', 'Остаток от деления, всегда неотрицательный при b > 0.', true),
  m('PI', null, 'double', 'Число π.', true),
];

const arrays: Member[] = [
  m('sort', 'int[] array', 'void', 'Сортирует массив по возрастанию.', true),
  m('toString', 'int[] array', 'String', 'Массив в виде строки: [1, 2, 3].', true),
  m('fill', 'int[] array, int value', 'void', 'Заполняет массив одним значением.', true),
  m('copyOf', 'int[] array, int length', 'int[]', 'Копия массива нужной длины.', true),
  m('copyOfRange', 'int[] array, int from, int to', 'int[]', 'Копия части массива.', true),
  m('equals', 'int[] a, int[] b', 'boolean', 'true, если массивы совпадают поэлементно.', true),
  m('stream', 'int[] array', 'IntStream', 'Поток элементов массива.', true),
  m('asList', 'T... elements', 'List', 'Список фиксированного размера из элементов.', true),
  m('binarySearch', 'int[] sortedArray, int key', 'int', 'Двоичный поиск в отсортированном массиве.', true),
  m('deepToString', 'Object[] array', 'String', 'Многомерный массив в виде строки.', true),
];

const collections: Member[] = [
  m('sort', 'List list', 'void', 'Сортирует список.', true),
  m('reverse', 'List list', 'void', 'Разворачивает список.', true),
  m('shuffle', 'List list', 'void', 'Перемешивает список.', true),
  m('max', 'Collection c', 'Object', 'Наибольший элемент.', true),
  m('min', 'Collection c', 'Object', 'Наименьший элемент.', true),
  m('frequency', 'Collection c, Object element', 'int', 'Сколько раз элемент встречается.', true),
  m('unmodifiableList', 'List list', 'List', 'Неизменяемое представление списка.', true),
];

const integer: Member[] = [
  m('parseInt', 'String text', 'int', 'Превращает строку в число: Integer.parseInt("42").', true),
  m('valueOf', 'int value', 'Integer', 'Число-объект.', true),
  m('toString', 'int value', 'String', 'Число в виде строки.', true),
  m('toBinaryString', 'int value', 'String', 'Двоичная запись числа.', true),
  m('compare', 'int a, int b', 'int', 'Сравнение двух чисел: < 0, 0 или > 0.', true),
  m('sum', 'int a, int b', 'int', 'Сумма — удобно как Integer::sum.', true),
  m('MAX_VALUE', null, 'int', 'Наибольшее значение int: 2 147 483 647.', true),
  m('MIN_VALUE', null, 'int', 'Наименьшее значение int: −2 147 483 648.', true),
];

const doubleStatic: Member[] = [
  m('parseDouble', 'String text', 'double', 'Превращает строку в дробное число.', true),
  m('compare', 'double a, double b', 'int', 'Сравнение двух чисел.', true),
  m('MAX_VALUE', null, 'double', 'Наибольшее значение double.', true),
];

const character: Member[] = [
  m('isDigit', 'char c', 'boolean', 'true, если символ — цифра.', true),
  m('isLetter', 'char c', 'boolean', 'true, если символ — буква.', true),
  m('isLetterOrDigit', 'char c', 'boolean', 'true, если буква или цифра.', true),
  m('isWhitespace', 'char c', 'boolean', 'true, если пробельный символ.', true),
  m('isUpperCase', 'char c', 'boolean', 'true, если заглавная буква.', true),
  m('isLowerCase', 'char c', 'boolean', 'true, если строчная буква.', true),
  m('toUpperCase', 'char c', 'char', 'Символ в верхнем регистре.', true),
  m('toLowerCase', 'char c', 'char', 'Символ в нижнем регистре.', true),
  m('getNumericValue', 'char c', 'int', 'Числовое значение цифры: \'7\' → 7.', true),
];

const random: Member[] = [
  m('nextInt', 'int bound', 'int', 'Случайное число от 0 до bound - 1.'),
  m('nextDouble', '', 'double', 'Случайное число от 0 до 1.'),
  m('nextBoolean', '', 'boolean', 'Случайно true или false.'),
];

const entry: Member[] = [
  m('getKey', '', 'K', 'Ключ пары.'),
  m('getValue', '', 'V', 'Значение пары.'),
  m('setValue', 'V value', 'V', 'Заменяет значение пары.'),
];

const iterator: Member[] = [
  m('hasNext', '', 'boolean', 'true, если есть следующий элемент.'),
  m('next', '', 'E', 'Следующий элемент.'),
  m('remove', '', 'void', 'Удаляет последний полученный элемент.'),
];

const array: Member[] = [m('length', null, 'int', 'Длина массива.'), m('clone', '', 'Object', 'Копия массива.')];

const collectors: Member[] = [
  m('toList', '', 'Collector', 'Собирает элементы в список.', true),
  m('toSet', '', 'Collector', 'Собирает элементы в множество.', true),
  m('joining', 'CharSequence separator', 'Collector', 'Склеивает строки через разделитель.', true),
  m('groupingBy', 'Function classifier', 'Collector', 'Группирует элементы в Map по ключу.', true),
  m('counting', '', 'Collector', 'Считает элементы (для groupingBy).', true),
  m('toMap', 'Function key, Function value', 'Collector', 'Собирает в Map.', true),
];

/** Члены экземпляров по имени типа. */
const INSTANCE: Record<string, Member[]> = {
  String: string,
  StringBuilder: stringBuilder,
  Scanner: scanner,
  PrintStream: printStream,
  List: list,
  ArrayList: list,
  LinkedList: list,
  Set: set,
  HashSet: set,
  TreeSet: set,
  LinkedHashSet: set,
  Map: map,
  HashMap: map,
  LinkedHashMap: map,
  TreeMap: [...map, ...treeMapExtra],
  Stream: stream,
  IntStream: intStream,
  Optional: optional,
  OptionalInt: optionalNumber('int'),
  OptionalDouble: optionalNumber('double'),
  Random: random,
  'Map.Entry': entry,
  Entry: entry,
  Iterator: iterator,
};

/** Статические члены по имени класса. */
const STATIC: Record<string, Member[]> = {
  System: system,
  Math: math,
  Arrays: arrays,
  Collections: collections,
  Integer: integer,
  Double: doubleStatic,
  Character: character,
  String: stringStatic,
  Collectors: collectors,
};

/** Разобранный тип: базовое имя, аргументы типа и признак массива. */
interface TypeRef {
  base: string;
  args: string[];
  array: boolean;
}

export function parseType(text: string): TypeRef {
  const trimmed = text.replace(/\s+/g, '');
  const array = trimmed.endsWith('[]');
  const withoutArray = trimmed.replace(/(\[\])+$/, '');
  const lt = withoutArray.indexOf('<');
  if (lt < 0) return { base: withoutArray, args: [], array };
  return { base: withoutArray.slice(0, lt), args: splitTypeArgs(withoutArray.slice(lt + 1, -1)), array };
}

function splitTypeArgs(text: string): string[] {
  const result: string[] = [];
  let depth = 0;
  let current = '';
  for (const ch of text) {
    if (ch === '<') depth++;
    if (ch === '>') depth--;
    if (ch === ',' && depth === 0) {
      result.push(current);
      current = '';
    } else {
      current += ch;
    }
  }
  if (current) result.push(current);
  return result;
}

/** Подставляет аргументы типа владельца: E → String для List<String>. */
function substitute(returns: string, owner: TypeRef): string {
  const params = TYPE_PARAMS[owner.base];
  if (!params || owner.args.length === 0) return returns;
  let result = returns;
  params.forEach((param, i) => {
    const arg = owner.args[i];
    if (arg) result = result.replace(new RegExp(`\\b${param}\\b`, 'g'), arg);
  });
  return result;
}

export interface ResolvedMembers {
  members: Member[];
  /** Для отображения: тип, чьи члены показаны. */
  typeName: string;
  owner: TypeRef | null;
}

/** Члены типа (экземпляра). */
function membersOfType(type: TypeRef): Member[] | null {
  if (type.array) return array;
  return INSTANCE[type.base] ?? null;
}

const PRIMITIVES = new Set(['int', 'long', 'double', 'float', 'boolean', 'char', 'byte', 'short', 'var']);

/** Ищет объявление переменной в коде до позиции и возвращает её тип. */
export function declaredType(source: string, name: string): TypeRef | null {
  const escaped = name.replace(/[$]/g, '\\$');
  // Тип Имя = ... / Тип Имя; / (Тип Имя) / for (Тип Имя : ...)
  const declaration = new RegExp(
    `([A-Za-z_][\\w.]*(?:<[^;=(){}]*?>)?(?:\\s*\\[\\s*\\])*)\\s+${escaped}\\s*(?:=|;|:|,|\\))`,
    'g',
  );
  let found: TypeRef | null = null;
  for (const match of source.matchAll(declaration)) {
    const typeText = match[1];
    if (['return', 'new', 'else', 'case', 'throw'].includes(typeText)) continue;
    if (typeText === 'var') {
      // var list = new ArrayList<String>(); — тип справа от new
      const rest = source.slice((match.index ?? 0) + match[0].length);
      const created = /^\s*new\s+([A-Za-z_][\w.]*(?:<[^;()]*?>)?)/.exec(rest);
      found = created ? parseType(created[1].replace('<>', '')) : null;
      continue;
    }
    found = parseType(typeText);
  }
  if (found && PRIMITIVES.has(found.base) && !found.array) return null;
  return found;
}

/**
 * Разбирает выражение перед точкой, например {@code names.stream().filter(x -> x.isEmpty())},
 * и возвращает члены его типа.
 */
export function resolveReceiver(source: string, receiver: string): ResolvedMembers | null {
  const chain = splitChain(receiver);
  if (chain.length === 0) return null;

  const [head, ...rest] = chain;
  let type: TypeRef | null = null;
  let staticMembers: Member[] | null = null;

  if (head.kind === 'string') {
    type = { base: 'String', args: [], array: false };
  } else if (head.kind === 'ident') {
    const declared = declaredType(source, head.name);
    if (declared) {
      type = declared;
    } else if (STATIC[head.name]) {
      staticMembers = STATIC[head.name];
    } else if (INSTANCE[head.name]) {
      return null;
    } else {
      return null;
    }
  } else if (head.kind === 'new') {
    type = parseType(head.name.replace('<>', ''));
  } else {
    return null;
  }

  for (const segment of rest) {
    const members = staticMembers ?? (type ? membersOfType(type) : null);
    if (!members) return null;
    const member = members.find((x) => x.name === segment.name && (segment.kind === 'call') === (x.params !== null));
    if (!member) return null;
    const returned: string = type ? substitute(member.returns, type) : member.returns;
    type = parseType(returned);
    staticMembers = null;
  }

  if (staticMembers) {
    return { members: staticMembers, typeName: head.kind === 'ident' ? head.name : '', owner: null };
  }
  if (!type) return null;
  const members = membersOfType(type);
  if (!members) return null;
  return {
    members: members.map((x) => ({ ...x, returns: substitute(x.returns, type!) })),
    typeName: type.array ? `${type.base}[]` : type.base,
    owner: type,
  };
}

type ChainSegment =
  | { kind: 'ident'; name: string }
  | { kind: 'call'; name: string }
  | { kind: 'field'; name: string }
  | { kind: 'string'; name: string }
  | { kind: 'new'; name: string };

/**
 * Делит выражение перед точкой на звенья: {@code a.b().c} → ident a, call b, field c.
 * Аргументы вызовов пропускаются с учётом вложенных скобок и строк.
 */
export function splitChain(receiver: string): ChainSegment[] {
  const text = receiver.trim();
  const segments: ChainSegment[] = [];
  let i = 0;

  const newMatch = /^new\s+([A-Za-z_][\w.]*(?:<[^()]*>)?)\s*\(/.exec(text);
  if (newMatch) {
    const close = skipParens(text, newMatch[0].length - 1);
    if (close < 0) return [];
    segments.push({ kind: 'new', name: newMatch[1] });
    i = close + 1;
  } else if (text.startsWith('"')) {
    const end = text.indexOf('"', 1);
    if (end < 0) return [];
    segments.push({ kind: 'string', name: text.slice(0, end + 1) });
    i = end + 1;
  }

  while (i < text.length) {
    if (text[i] === '.') {
      i++;
      continue;
    }
    const ident = /^[A-Za-z_$][\w$]*/.exec(text.slice(i));
    if (!ident) return [];
    const name = ident[0];
    i += name.length;
    if (text[i] === '(') {
      const close = skipParens(text, i);
      if (close < 0) return [];
      segments.push(segments.length === 0 ? { kind: 'ident', name } : { kind: 'call', name });
      if (segments.length === 1) return [];
      i = close + 1;
    } else {
      segments.push(segments.length === 0 ? { kind: 'ident', name } : { kind: 'field', name });
    }
    if (i < text.length && text[i] !== '.') return [];
  }
  return segments;
}

function skipParens(text: string, open: number): number {
  let depth = 0;
  let inString = false;
  for (let i = open; i < text.length; i++) {
    const ch = text[i];
    if (inString) {
      if (ch === '\\') i++;
      else if (ch === '"') inString = false;
      continue;
    }
    if (ch === '"') inString = true;
    else if (ch === '(') depth++;
    else if (ch === ')') {
      depth--;
      if (depth === 0) return i;
    }
  }
  return -1;
}

/**
 * Текст выражения, которое стоит перед точкой в позиции {@code end} строки:
 * идём назад, пропуская скобки вызовов и строки.
 */
export function receiverBefore(line: string, dotIndex: number): string {
  let i = dotIndex - 1;
  let depth = 0;
  while (i >= 0) {
    const ch = line[i];
    if (ch === ')') depth++;
    else if (ch === '(') {
      if (depth === 0) break;
      depth--;
    } else if (ch === '"') {
      const start = line.lastIndexOf('"', i - 1);
      if (start < 0) break;
      i = start - 1;
      continue;
    } else if (depth === 0 && !/[\w$.]/.test(ch)) {
      // "new Foo()" — захватываем new
      const before = line.slice(0, i + 1);
      if (/new\s+$/.test(before) && line[i + 1] && /[A-Z]/.test(line[i + 1])) {
        i = before.search(/new\s+$/) - 1;
      }
      break;
    }
    i--;
  }
  return line.slice(i + 1, dotIndex);
}

export function signature(member: Member): string {
  if (member.params === null) return `${member.returns} ${member.name}`;
  return `${member.returns} ${member.name}(${member.params})`;
}
