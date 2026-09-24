import java.util.Scanner;

public class Main {

    static int moves = 0;

    static void hanoi(int n, char from, char to, char via) {
        if (n == 0) {
            return;
        }
        hanoi(n - 1, from, via, to);
        System.out.println("Диск " + n + ": " + from + " → " + to);
        moves++;
        hanoi(n - 1, via, to, from);
    }

    public static void main(String[] args) {
        int n = new Scanner(System.in).nextInt();
        hanoi(n, 'A', 'C', 'B');
        System.out.println("Ходов: " + moves);
    }
}
