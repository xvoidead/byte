import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        int n = new Scanner(System.in).nextInt();
        for (int row = 1; row <= n; row++) {
            for (int space = 0; space < n - row; space++) {
                System.out.print(' ');
            }
            for (int star = 0; star < 2 * row - 1; star++) {
                System.out.print('*');
            }
            System.out.println();
        }
        for (int space = 0; space < n - 1; space++) {
            System.out.print(' ');
        }
        System.out.println('|');
    }
}
