package dev.byteide.web;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.byteide.runner.CodeRunner;
import dev.byteide.runner.Project;
import dev.byteide.runner.ProjectFile;
import dev.byteide.runner.RunResult;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class RunController {

    private static final Logger log = LoggerFactory.getLogger(RunController.class);

    private final CodeRunner runner;
    private final RequestLimits limits;

    public RunController(CodeRunner runner, RequestLimits limits) {
        this.runner = runner;
        this.limits = limits;
    }

    @PostMapping("/run")
    public RunResult run(@RequestBody RunRequest request, HttpServletRequest http) {
        Project project = limits.project(request.code(), request.files());
        limits.checkStdin(request.stdin());
        RunResult result = runner.run(project, request.stdin());
        log.info("run status={} compile={}ms run={}ms ip={}", result.status(), result.compileTimeMs(),
                result.runTimeMs(), ClientIp.masked(ClientIp.of(http)));
        return result;
    }

    /** {@code files} — проект из нескольких файлов; {@code code} — один Main.java (для старых клиентов). */
    public record RunRequest(String code, List<ProjectFile> files, String stdin) {
    }
}
