package dev.byteide.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.byteide.runner.CodeRunner;
import dev.byteide.runner.RunResult;

@RestController
@RequestMapping("/api")
public class RunController {

    private final CodeRunner runner;
    private final RequestLimits limits;

    public RunController(CodeRunner runner, RequestLimits limits) {
        this.runner = runner;
        this.limits = limits;
    }

    @PostMapping("/run")
    public RunResult run(@RequestBody RunRequest request) {
        limits.checkSource(request.code());
        limits.checkStdin(request.stdin());
        return runner.run(request.code(), request.stdin());
    }

    public record RunRequest(String code, String stdin) {
    }
}
