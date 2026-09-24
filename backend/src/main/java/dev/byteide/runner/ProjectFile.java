package dev.byteide.runner;

/**
 * Файл проекта ученика: исходник {@code .java}, ресурс {@code resources/...} (попадает в classpath)
 * или любой другой файл (лежит в рабочей папке программы — например, {@code config.yml}).
 */
public record ProjectFile(String name, String content) {

    public ProjectFile {
        content = content == null ? "" : content;
    }

    public boolean isSource() {
        return name.endsWith(".java") && !isResource();
    }

    public boolean isResource() {
        return name.startsWith(Project.RESOURCES);
    }

    public boolean isWorkFile() {
        return !isSource() && !isResource();
    }
}
