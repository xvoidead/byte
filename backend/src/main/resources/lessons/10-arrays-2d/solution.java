import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int m = in.nextInt();
        int[][] table = new int[n][m];
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < m; col++) {
                table[row][col] = in.nextInt();
            }
        }
        for (int col = 0; col < m; col++) {
            StringBuilder line = new StringBuilder();
            for (int row = 0; row < n; row++) {
                if (row > 0) {
                    line.append(' ');
                }
                line.append(table[row][col]);
            }
            System.out.println(line);
        }
        StringBuilder sums = new StringBuilder("Суммы строк:");
        for (int[] row : table) {
            int sum = 0;
            for (int value : row) {
                sum += value;
            }
            sums.append(' ').append(sum);
        }
        System.out.println(sums);
    }
}
