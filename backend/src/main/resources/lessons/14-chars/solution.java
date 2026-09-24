import java.util.Scanner;

public class Main {

    static char shift(char c, int k) {
        if (c >= 'a' && c <= 'z') {
            return (char) ('a' + Math.floorMod(c - 'a' + k, 26));
        }
        if (c >= 'A' && c <= 'Z') {
            return (char) ('A' + Math.floorMod(c - 'A' + k, 26));
        }
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
