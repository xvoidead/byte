import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            String[] parts = in.nextLine().strip().split(" ", 2);
            String command = parts[0];
            String argument = parts.length > 1 ? parts[1].strip() : "";

            // Обработайте команды add, list, done, undo, remove и stats

        }
    }
}
