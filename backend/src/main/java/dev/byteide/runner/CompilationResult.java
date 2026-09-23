package dev.byteide.runner;

import java.util.List;

/**
 * Результат компиляции. При успехе {@link #program()} не null и должен быть закрыт вызывающим.
 */
public record CompilationResult(CompiledProgram program, List<Diagnostic> diagnostics, long timeMs) {

    public boolean success() {
        return program != null;
    }
}
