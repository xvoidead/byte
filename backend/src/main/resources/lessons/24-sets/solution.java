import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Set<String> a = words(in.hasNextLine() ? in.nextLine() : "");
        Set<String> b = words(in.hasNextLine() ? in.nextLine() : "");

        Set<String> both = new TreeSet<>(a);
        both.retainAll(b);
        Set<String> onlyA = new TreeSet<>(a);
        onlyA.removeAll(b);
        Set<String> onlyB = new TreeSet<>(b);
        onlyB.removeAll(a);
        Set<String> all = new TreeSet<>(a);
        all.addAll(b);

        System.out.println("Обе задачи: " + format(both));
        System.out.println("Только A: " + format(onlyA));
        System.out.println("Только B: " + format(onlyB));
        System.out.println("Всего разных: " + all.size());
    }

    static Set<String> words(String line) {
        Set<String> result = new TreeSet<>();
        for (String word : line.trim().split("\\s+")) {
            if (!word.isEmpty()) {
                result.add(word);
            }
        }
        return result;
    }

    static String format(Set<String> names) {
        return names.isEmpty() ? "—" : String.join(" ", names);
    }
}
