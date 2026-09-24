import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BinaryOperator;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        long a = in.nextLong();
        long b = in.nextLong();

        Map<String, BinaryOperator<Long>> operations = new HashMap<>();
        // Заполните таблицу операций лямбдами

        while (in.hasNext()) {
            String name = in.next();
            System.out.println(name + ": неизвестная операция");
        }
    }
}
