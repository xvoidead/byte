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

        // Отсортируйте массив пузырьком и посчитайте обмены

        System.out.println("Обменов: " + swaps);
    }
}
