import java.util.Scanner;

public class Main {

    static int moves = 0;

    static void hanoi(int n, char from, char to, char via) {
        // Перенесите n дисков со стержня from на to через via
    }

    public static void main(String[] args) {
        int n = new Scanner(System.in).nextInt();
        hanoi(n, 'A', 'C', 'B');
        System.out.println("Ходов: " + moves);
    }
}
