import java.util.Locale;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = in.nextInt();
        }
        int max = a[0], min = a[0];
        long sum = 0;
        for (int x : a) {
            max = Math.max(max, x);
            min = Math.min(min, x);
            sum += x;
        }
        System.out.println("Максимум: " + max);
        System.out.println("Минимум: " + min);
        System.out.printf(Locale.US, "Среднее: %.2f%n", (double) sum / n);
    }
}
