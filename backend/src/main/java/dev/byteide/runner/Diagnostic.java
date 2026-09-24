package dev.byteide.runner;

/**
 * Сообщение компилятора, привязанное к позиции в исходном коде.
 * Строки и колонки нумеруются с 1; 0 означает, что позиция неизвестна.
 *
 * @param file имя файла проекта или null, если сообщение относится к проекту целиком
 */
public record Diagnostic(Severity severity, long line, long column, long endColumn, String message, String hint,
                         String code, String file) {

    public enum Severity { ERROR, WARNING }

    /** Сообщение без позиции — например, «не найден метод main». */
    public static Diagnostic general(String message, String hint, String code) {
        return new Diagnostic(Severity.ERROR, 0, 0, 0, message, hint, code, null);
    }
}
