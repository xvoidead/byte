import java.util.List;

/** Конфиг с ошибками: в исключении сразу все найденные проблемы. */
public class ConfigException extends RuntimeException {
    private final List<String> errors;

    public ConfigException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
