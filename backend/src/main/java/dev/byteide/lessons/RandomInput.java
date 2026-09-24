package dev.byteide.lessons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Генерирует случайный ввод для скрытых тестов по шаблону. Ожидаемый вывод потом получается
 * запуском эталонного решения, поэтому заучить ответы на тесты нельзя.
 *
 * <p>Заполнители:
 * <ul>
 *   <li>{@code {int:a..b}} — целое число от a до b;</li>
 *   <li>{@code {n=int:a..b}} — то же, но значение запоминается под именем n;</li>
 *   <li>{@code {ints:N:a..b}} — N чисел через пробел; N — число, диапазон «a..b» или имя сохранённого значения;</li>
 *   <li>{@code {sortedints:N:a..b}} — N разных чисел по возрастанию;</li>
 *   <li>{@code {word:a..b}} — слово из строчных латинских букв длиной от a до b;</li>
 *   <li>{@code {choice:x|y|z}} — один из вариантов;</li>
 *   <li>{@code {words:N:x|y|z}} — N слов из списка через пробел;</li>
 *   <li>{@code {mixcase:шаблон}} — шаблон со случайным регистром букв;</li>
 *   <li>{@code {repeat:N:шаблон}} — шаблон, повторённый N раз.</li>
 * </ul>
 */
public final class RandomInput {

    private final String template;

    public RandomInput(String template) {
        this.template = template;
        // Проверяем шаблон сразу, чтобы ошибка в уроке обнаруживалась при запуске, а не на проверке.
        render(RandomGenerator.of("L64X128MixRandom"));
    }

    public String render(RandomGenerator random) {
        return expand(template, random, new HashMap<>());
    }

    private static String expand(String text, RandomGenerator random, Map<String, Integer> vars) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '{') {
                int close = matchingBrace(text, i);
                out.append(placeholder(text.substring(i + 1, close), random, vars));
                i = close + 1;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static int matchingBrace(String text, int open) {
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            if (text.charAt(i) == '{') {
                depth++;
            } else if (text.charAt(i) == '}' && --depth == 0) {
                return i;
            }
        }
        throw new IllegalArgumentException("Не закрыта фигурная скобка в шаблоне: " + text);
    }

    private static String placeholder(String body, RandomGenerator random, Map<String, Integer> vars) {
        String variable = null;
        int eq = body.indexOf('=');
        int colon = body.indexOf(':');
        if (eq > 0 && (colon < 0 || eq < colon)) {
            variable = body.substring(0, eq);
            body = body.substring(eq + 1);
            colon = body.indexOf(':');
        }
        String kind = colon < 0 ? body : body.substring(0, colon);
        String args = colon < 0 ? "" : body.substring(colon + 1);
        return switch (kind) {
            case "int" -> {
                int value = range(args, random, vars);
                if (variable != null) {
                    vars.put(variable, value);
                }
                yield String.valueOf(value);
            }
            case "ints" -> {
                String[] parts = splitFirst(args);
                int count = range(parts[0], random, vars);
                List<String> values = new ArrayList<>();
                for (int k = 0; k < count; k++) {
                    values.add(String.valueOf(range(parts[1], random, vars)));
                }
                yield String.join(" ", values);
            }
            case "sortedints" -> {
                String[] parts = splitFirst(args);
                int count = range(parts[0], random, vars);
                String bounds = parts[1].strip();
                int dots = bounds.indexOf("..");
                int from = Integer.parseInt(bounds.substring(0, dots).strip());
                int to = Integer.parseInt(bounds.substring(dots + 2).strip());
                if ((long) to - from + 1 < count) {
                    throw new IllegalArgumentException("В диапазоне " + bounds + " меньше " + count + " разных чисел");
                }
                java.util.TreeSet<Integer> values = new java.util.TreeSet<>();
                while (values.size() < count) {
                    values.add(from + random.nextInt(to - from + 1));
                }
                yield String.join(" ", values.stream().map(String::valueOf).toList());
            }
            case "word" -> {
                int length = range(args, random, vars);
                StringBuilder word = new StringBuilder();
                for (int k = 0; k < length; k++) {
                    word.append((char) ('a' + random.nextInt(26)));
                }
                yield word.toString();
            }
            case "choice" -> {
                List<String> options = splitOptions(args);
                yield expand(options.get(random.nextInt(options.size())), random, vars);
            }
            case "words" -> {
                String[] parts = splitFirst(args);
                int count = range(parts[0], random, vars);
                List<String> options = splitOptions(parts[1]);
                List<String> words = new ArrayList<>();
                for (int k = 0; k < count; k++) {
                    words.add(expand(options.get(random.nextInt(options.size())), random, vars));
                }
                yield String.join(" ", words);
            }
            case "mixcase" -> {
                String inner = expand(args, random, vars);
                StringBuilder mixed = new StringBuilder();
                for (char ch : inner.toCharArray()) {
                    mixed.append(random.nextBoolean() ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
                }
                yield mixed.toString();
            }
            case "repeat" -> {
                String[] parts = splitFirst(args);
                int count = range(parts[0], random, vars);
                StringBuilder repeated = new StringBuilder();
                for (int k = 0; k < count; k++) {
                    repeated.append(expand(parts[1], random, vars));
                }
                yield repeated.toString();
            }
            default -> throw new IllegalArgumentException("Неизвестный заполнитель {" + body + "}");
        };
    }

    /** Делит варианты по «|» верхнего уровня: вложенные шаблоны в вариантах не разрезаются. */
    private static List<String> splitOptions(String text) {
        List<String> options = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            if (c == '|' && depth == 0) {
                options.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        options.add(current.toString());
        return options;
    }

    /** "3..8:остальное" → ["3..8", "остальное"]. */
    private static String[] splitFirst(String args) {
        int colon = args.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("Ожидалось «количество:значение» в " + args);
        }
        return new String[]{args.substring(0, colon), args.substring(colon + 1)};
    }

    /** "5", "-10..10" или имя сохранённого значения. */
    private static int range(String spec, RandomGenerator random, Map<String, Integer> vars) {
        spec = spec.strip();
        if (vars.containsKey(spec)) {
            return vars.get(spec);
        }
        int dots = spec.indexOf("..");
        if (dots < 0) {
            return Integer.parseInt(spec);
        }
        int from = Integer.parseInt(spec.substring(0, dots).strip());
        int to = Integer.parseInt(spec.substring(dots + 2).strip());
        if (to < from) {
            throw new IllegalArgumentException("Пустой диапазон " + spec);
        }
        return from + random.nextInt(to - from + 1);
    }
}
