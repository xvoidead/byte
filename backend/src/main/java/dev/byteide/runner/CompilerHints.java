package dev.byteide.runner;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Подсказки на русском для самых частых ошибок компиляции.
 * javac не переведён на русский, поэтому к оригинальному сообщению добавляем объяснение для новичка.
 */
final class CompilerHints {

    private static final Map<String, String> HINTS = new LinkedHashMap<>();

    static {
        HINTS.put("compiler.err.expected",
                "Компилятор ожидал здесь другой символ. Чаще всего забыта точка с запятой «;» или скобка.");
        HINTS.put("compiler.err.premature.eof",
                "Файл закончился раньше, чем ожидалось. Скорее всего, не хватает закрывающей фигурной скобки «}».");
        HINTS.put("compiler.err.cant.resolve",
                "Имя не найдено. Проверьте написание (Java различает заглавные и строчные буквы), "
                        + "объявлена ли переменная и подключён ли нужный import.");
        HINTS.put("compiler.err.prob.found.req",
                "Несовместимые типы: значение одного типа нельзя использовать там, где ожидается другой, "
                        + "без явного преобразования.");
        HINTS.put("compiler.err.var.might.not.have.been.initialized",
                "Переменная используется до того, как ей присвоили значение.");
        HINTS.put("compiler.err.missing.ret.stmt",
                "Метод объявлен как возвращающий значение, но не во всех ветках есть return.");
        HINTS.put("compiler.err.already.defined",
                "Такое имя уже объявлено в этой области видимости. Выберите другое имя.");
        HINTS.put("compiler.err.unclosed.str.lit",
                "Строка не закрыта — не хватает двойной кавычки «\"».");
        HINTS.put("compiler.err.unclosed.char.lit",
                "Символьный литерал не закрыт — не хватает одинарной кавычки «'».");
        HINTS.put("compiler.err.not.stmt",
                "Это выражение не может быть отдельной инструкцией. Возможно, вы хотели присвоить результат переменной.");
        HINTS.put("compiler.err.illegal.start.of.expr",
                "Недопустимое начало выражения. Проверьте скобки и пропущенные символы в этой и предыдущей строке.");
        HINTS.put("compiler.err.illegal.start.of.type",
                "Недопустимое начало объявления. Возможно, код находится вне метода или не хватает скобки.");
        HINTS.put("compiler.err.class.public.should.be.in.file",
                "В файле может быть только один публичный класс. Уберите слово public у остальных классов.");
        HINTS.put("compiler.err.unreported.exception",
                "Это исключение нужно обработать в блоке try/catch или объявить в сигнатуре метода через throws.");
        HINTS.put("compiler.err.non-static.cant.be.ref",
                "Из статического метода (например, main) нельзя напрямую обратиться к нестатическому полю или методу. "
                        + "Создайте объект или объявите член как static.");
        HINTS.put("compiler.err.cant.apply.symbol",
                "Метод вызван с неподходящими аргументами — проверьте их количество и типы.");
        HINTS.put("compiler.err.unreachable.stmt",
                "Этот код никогда не выполнится — например, он стоит после return или break.");
        HINTS.put("compiler.err.else.without.if",
                "«else» без соответствующего «if». Проверьте фигурные скобки и нет ли лишней «;» после условия.");
        HINTS.put("compiler.err.cant.deref",
                "У примитивных типов (int, double, boolean…) нет методов и полей.");
        HINTS.put("compiler.err.operator.cant.be.applied",
                "Этот оператор нельзя применить к значениям таких типов.");
        HINTS.put("compiler.err.incomparable.types",
                "Значения этих типов нельзя сравнивать между собой.");
        HINTS.put("compiler.err.does.not.override.abstract",
                "Класс должен реализовать все абстрактные методы родителя или интерфейса.");
        HINTS.put("compiler.err.report.access",
                "Этот член класса недоступен отсюда (например, он private).");
        HINTS.put("compiler.err.abstract.cant.be.instantiated",
                "Нельзя создать объект абстрактного класса или интерфейса через new.");
        HINTS.put("compiler.err.break.outside.switch.loop",
                "break можно использовать только внутри цикла или switch.");
    }

    private CompilerHints() {
    }

    static String forCode(String code) {
        if (code == null) {
            return null;
        }
        for (var entry : HINTS.entrySet()) {
            if (code.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
