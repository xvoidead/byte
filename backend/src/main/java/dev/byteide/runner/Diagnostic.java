package dev.byteide.runner;

/**
 * Сообщение компилятора, привязанное к позиции в исходном коде.
 * Строки и колонки нумеруются с 1; 0 означает, что позиция неизвестна.
 */
public record Diagnostic(Severity severity, long line, long column, long endColumn, String message, String hint,
                         String code) {

    public enum Severity { ERROR, WARNING }
}
