package dev.byteide.lessons;

import java.util.List;

/**
 * Требование к устройству решения — защита от «подгона» ответа под тесты.
 * Проверяется по синтаксическому дереву программы, без запуска.
 *
 * <p>Типы:
 * <ul>
 *   <li>{@code loop} — есть хотя бы один цикл; {@code noLoops} — циклов нет;</li>
 *   <li>{@code method} — объявлен метод {@code name} (с {@code returns} и {@code params}, если указаны);</li>
 *   <li>{@code call} — вызывается метод {@code name}; {@code forbidCall} — не вызывается метод {@code name}
 *       или ни один из {@code values} (имя можно указать с классом: {@code Arrays.sort});</li>
 *   <li>{@code forbidLiteral} — в коде нет литералов из {@code values} (готовых ответов);</li>
 *   <li>{@code literal} — в строках кода есть каждый из фрагментов {@code values} (или {@code name}) без учёта
 *       регистра и лишних пробелов: так уроки о базах данных требуют конструкции SQL, например {@code ON CONFLICT};</li>
 *   <li>{@code recursion} — метод {@code name} вызывает сам себя;</li>
 *   <li>{@code class} — объявлен класс {@code name}, при необходимости с {@code superclass}
 *       и {@code interfaceName}; {@code kind} — class, interface, enum или record;</li>
 *   <li>{@code privateField} — в классе {@code className} есть приватное поле {@code name};</li>
 *   <li>{@code uses} — в коде есть конструкция {@code construct}: lambda, try, throw, switch, stream,
 *       new (создаётся объект класса {@code name}); {@code avoid} — такой конструкции нет.</li>
 * </ul>
 */
public record Requirement(
        String type,
        String name,
        String returns,
        List<String> params,
        List<String> values,
        String superclass,
        String interfaceName,
        String kind,
        String className,
        String construct,
        String message) {

    public Requirement {
        if (type == null || message == null || message.isBlank()) {
            throw new IllegalArgumentException("У требования должны быть type и message");
        }
    }
}
