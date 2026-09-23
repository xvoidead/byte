package dev.byteide.runner;

import java.util.List;

public record RunResult(
        RunStatus status,
        String stdout,
        String stderr,
        Integer exitCode,
        List<Diagnostic> diagnostics,
        long compileTimeMs,
        long runTimeMs) {

    static RunResult compilationFailed(CompilationResult compilation) {
        return new RunResult(RunStatus.COMPILATION_ERROR, "", "", null,
                compilation.diagnostics(), compilation.timeMs(), 0);
    }

    static RunResult executed(CompilationResult compilation, ExecutionResult execution) {
        return new RunResult(execution.status(), execution.stdout(), execution.stderr(), execution.exitCode(),
                compilation.diagnostics(), compilation.timeMs(), execution.timeMs());
    }
}
