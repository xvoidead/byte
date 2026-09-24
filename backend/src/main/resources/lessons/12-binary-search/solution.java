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
            int left = 0;
            int right = n - 1;
            int steps = 0;
            int found = -1;
            while (left <= right) {
                int mid = (left + right) / 2;
                steps++;
                if (a[mid] == target) {
                    found = mid;
                    break;
                } else if (a[mid] < target) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
            if (found >= 0) {
                System.out.println(target + ": индекс " + found + ", шагов " + steps);
            } else {
                System.out.println(target + ": не найдено, шагов " + steps);
            }
        }
    }
}
