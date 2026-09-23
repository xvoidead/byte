package dev.byteide.runner;

/** Песочница не работает — выполнять код без неё нельзя. */
public class SandboxUnavailableException extends RuntimeException {

    public SandboxUnavailableException(String details) {
        super("Сервер временно не может запускать программы. " + details);
    }
}
