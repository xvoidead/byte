import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = in.nextInt();
        }
        int q = in.nextInt();
        for (int k = 0; k < q; k++) {
            int target = in.nextInt();
            // Найдите target двоичным поиском и посчитайте шаги
            System.out.println(target + ": не найдено, шагов 0");
        }
    }
}
