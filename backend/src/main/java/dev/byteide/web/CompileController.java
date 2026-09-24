package dev.byteide.web;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.byteide.runner.CodeRunner;
import dev.byteide.runner.JavaCompilationService;
import dev.byteide.runner.ProjectFile;

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
        return runner.diagnose(limits.project(request.code(), request.files()));
    }

    public record CompileRequest(String code, List<ProjectFile> files) {
    }
}
