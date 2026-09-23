package dev.byteide.runner;

public enum RunStatus {
    /** Программа завершилась с кодом 0. */
    SUCCESS,
    /** Код не скомпилировался. */
    COMPILATION_ERROR,
    /** Программа завершилась с ненулевым кодом (исключение, System.exit и т.п.). */
    RUNTIME_ERROR,
    /** Программа не уложилась в лимит времени. */
    TIMEOUT,
    /** Программа напечатала слишком много. */
    OUTPUT_LIMIT
}
