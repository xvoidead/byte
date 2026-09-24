package dev.byteide.web;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.byteide.runner.Project;
import dev.byteide.runner.ProjectFile;
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

    /**
     * Проект из запроса: новые клиенты присылают список файлов, старые — один {@code code}.
     * Бросает {@link BadRequestException} с понятным ученику сообщением.
     */
    public Project project(String code, List<ProjectFile> files) {
        if (files == null || files.isEmpty()) {
            checkSource(code);
            return Project.single(code);
        }
        if (files.stream().anyMatch(f -> f == null || f.name() == null)) {
            throw new BadRequestException("У каждого файла должно быть имя.");
        }
        Project project = new Project(files);
        String problem = project.problem(properties.maxSourceLength());
        if (problem != null) {
            throw new BadRequestException(problem);
        }
        return project;
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
