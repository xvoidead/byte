import java.util.Scanner;

public class Main {

    static boolean isPrime(int n) {
        if (n < 2) {
            return false;
        }
        for (int d = 2; d * d <= n; d++) {
            if (n % d == 0) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        int n = new Scanner(System.in).nextInt();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 2; i <= n; i++) {
            if (isPrime(i)) {
                if (count > 0) {
                    sb.append(' ');
                }
                sb.append(i);
                count++;
            }
        }
        if (count == 0) {
            System.out.println("Простых чисел нет");
        } else {
            System.out.println(sb);
            System.out.println("Всего: " + count);
        }
    }
}
