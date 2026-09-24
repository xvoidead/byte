import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = in.nextInt();
        }
        int swaps = 0;
        for (int pass = 0; pass < n - 1; pass++) {
            for (int i = 0; i < n - 1 - pass; i++) {
                if (a[i] > a[i + 1]) {
                    int temp = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = temp;
                    swaps++;
                }
            }
        }
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                line.append(' ');
            }
            line.append(a[i]);
        }
        System.out.println(line);
        System.out.println("Обменов: " + swaps);
    }
}
