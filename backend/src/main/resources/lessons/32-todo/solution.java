import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

class Task {
    private final int id;
    private final String title;
    private boolean done;

    Task(int id, String title) {
        this.id = id;
        this.title = title;
    }

    int id() {
        return id;
    }

    boolean isDone() {
        return done;
    }

    void setDone(boolean done) {
        this.done = done;
    }

    @Override
    public String toString() {
        return id + ". [" + (done ? "x" : " ") + "] " + title;
    }
}

public class Main {
    private static final Map<Integer, Task> tasks = new TreeMap<>();
    private static int nextId = 1;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            String line = in.nextLine().strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(" ", 2);
            String command = parts[0];
            String argument = parts.length > 1 ? parts[1].strip() : "";
            switch (command) {
                case "add" -> add(argument);
                case "list" -> list();
                case "done" -> find(argument, task -> task.setDone(true));
                case "undo" -> find(argument, task -> task.setDone(false));
                case "remove" -> find(argument, task -> {
                    tasks.remove(task.id());
                    System.out.println("Удалена задача " + task.id());
                });
                case "stats" -> {
                    long done = tasks.values().stream().filter(Task::isDone).count();
                    System.out.println("Выполнено: " + done + " из " + tasks.size());
                }
                default -> System.out.println("Неизвестная команда: " + command);
            }
        }
    }

    private static void add(String title) {
        if (title.isEmpty()) {
            System.out.println("Пустая задача");
            return;
        }
        Task task = new Task(nextId++, title);
        tasks.put(task.id(), task);
        System.out.println("Добавлена задача " + task.id());
    }

    private static void list() {
        if (tasks.isEmpty()) {
            System.out.println("Список пуст");
            return;
        }
        tasks.values().forEach(System.out::println);
    }

    private static void find(String argument, java.util.function.Consumer<Task> action) {
        if (argument.isEmpty()) {
            System.out.println("Укажите номер задачи");
            return;
        }
        try {
            Task task = tasks.get(Integer.parseInt(argument));
            if (task != null) {
                action.accept(task);
                return;
            }
        } catch (NumberFormatException ignored) {
            // не число — задачи точно нет
        }
        System.out.println("Задача " + argument + " не найдена");
    }
}
