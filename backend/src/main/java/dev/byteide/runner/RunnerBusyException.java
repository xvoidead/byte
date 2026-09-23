package dev.byteide.runner;

public class RunnerBusyException extends RuntimeException {

    public RunnerBusyException() {
        super("Сервер сейчас выполняет слишком много программ. Попробуйте через несколько секунд.");
    }
}
