import java.util.Scanner;

public class Main {

    static char shift(char c, int k) {
        // Сдвиньте латинскую букву по кругу, остальные символы верните как есть
        return c;
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int k = Integer.parseInt(in.nextLine().trim());
        String text = in.nextLine();

        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(shift(c, k));
        }
        System.out.println(result);
    }
}
