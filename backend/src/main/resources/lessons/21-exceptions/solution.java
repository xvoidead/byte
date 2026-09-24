import java.util.Locale;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        long sum = 0;
        int count = 0;
        int errors = 0;
        while (in.hasNextLine()) {
            String line = in.nextLine().strip();
            if (line.isEmpty()) {
                continue;
            }
            try {
                sum += Integer.parseInt(line);
                count++;
            } catch (NumberFormatException e) {
                errors++;
            }
        }
        System.out.println("Сумма: " + sum);
        System.out.println("Чисел: " + count);
        System.out.println("Ошибок: " + errors);
        if (count == 0) {
            System.out.println("Среднее: нет данных");
        } else {
            System.out.printf(Locale.US, "Среднее: %.2f%n", (double) sum / count);
        }
    }
}
