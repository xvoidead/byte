package dev.byteide.runner;

public record ExecutionResult(RunStatus status, String stdout, String stderr, Integer exitCode, long timeMs) {
}
