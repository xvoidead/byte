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

        // Сгруппируйте оценки по предметам и по ученикам

        System.out.println("Отличников: " + 0);
    }
}
