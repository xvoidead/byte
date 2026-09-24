import java.util.Locale;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int[] numbers = new int[n];
        for (int i = 0; i < n; i++) {
            numbers[i] = in.nextInt();
        }

        // Найдите максимум, минимум и среднее

        System.out.printf(Locale.US, "Среднее: %.2f%n", 0.0);
    }
}
