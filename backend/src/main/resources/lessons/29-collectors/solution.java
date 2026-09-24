import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

record Grade(String subject, String student, int value) {
    static Grade parse(String line) {
        String[] parts = line.trim().split("\\s+");
        return new Grade(parts[0], parts[1], Integer.parseInt(parts[2]));
    }
}

public class Main {
    public static void main(String[] args) {
        List<Grade> grades = new BufferedReader(new InputStreamReader(System.in)).lines()
                .filter(line -> !line.isBlank())
                .map(Grade::parse)
                .toList();

        Map<String, List<Grade>> bySubject = grades.stream()
                .collect(Collectors.groupingBy(Grade::subject, TreeMap::new, Collectors.toList()));
        bySubject.forEach((subject, list) -> {
            double average = list.stream().mapToInt(Grade::value).average().orElse(0);
            Grade best = list.stream()
                    .min(Comparator.comparingInt(Grade::value).reversed().thenComparing(Grade::student))
                    .orElseThrow();
            System.out.printf(Locale.US, "%s: средний %.2f, лучший — %s (%d)%n",
                    subject, average, best.student(), best.value());
        });

        long excellent = grades.stream()
                .collect(Collectors.groupingBy(Grade::student,
                        Collectors.mapping(Grade::value, Collectors.toList())))
                .values().stream()
                .filter(values -> values.stream().allMatch(v -> v == 5))
                .count();
        System.out.println("Отличников: " + excellent);
    }
}
