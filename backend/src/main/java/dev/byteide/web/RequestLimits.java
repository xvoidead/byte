package dev.byteide.web;

import org.springframework.stereotype.Component;

import dev.byteide.runner.RunnerProperties;

@Component
public class RequestLimits {

    private final RunnerProperties properties;

    public RequestLimits(RunnerProperties properties) {
        this.properties = properties;
    }

    public void checkSource(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Код программы пуст.");
        }
        if (code.length() > properties.maxSourceLength()) {
            throw new BadRequestException("Код слишком длинный: максимум " + properties.maxSourceLength() + " символов.");
        }
    }

    public void checkStdin(String stdin) {
        if (stdin != null && stdin.length() > properties.maxStdinLength()) {
            throw new BadRequestException("Ввод слишком длинный: максимум " + properties.maxStdinLength() + " символов.");
        }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }
}
