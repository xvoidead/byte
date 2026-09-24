package dev.byteide.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.byteide.runner.CodeRunner;
import dev.byteide.runner.JavaCompilationService;

/** Проверка кода на ошибки во время набора — без запуска. */
@RestController
@RequestMapping("/api")
public class CompileController {

    private final CodeRunner runner;
    private final RequestLimits limits;

    public CompileController(CodeRunner runner, RequestLimits limits) {
        this.runner = runner;
        this.limits = limits;
    }

    @PostMapping("/compile")
    public JavaCompilationService.DiagnosticsResult compile(@RequestBody CompileRequest request) {
        limits.checkSource(request.code());
        return runner.diagnose(request.code());
    }

    public record CompileRequest(String code) {
    }
}
