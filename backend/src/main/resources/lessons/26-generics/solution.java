import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Stack<T> {
    private final List<T> items = new ArrayList<>();

    void push(T item) {
        items.add(item);
    }

    T pop() {
        return items.remove(items.size() - 1);
    }

    T peek() {
        return items.get(items.size() - 1);
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    int size() {
        return items.size();
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            System.out.println(check(in.nextLine()));
        }
    }

    static String check(String line) {
        Stack<Character> open = new Stack<>();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                open.push(c);
            } else if (c == ')' || c == ']' || c == '}') {
                if (open.isEmpty() || open.pop() != pairFor(c)) {
                    return "Ошибка в позиции " + (i + 1);
                }
            }
        }
        return open.isEmpty() ? "OK" : "Не закрыто скобок: " + open.size();
    }

    static char pairFor(char closing) {
        return switch (closing) {
            case ')' -> '(';
            case ']' -> '[';
            default -> '{';
        };
    }
}
