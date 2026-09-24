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
        operations.put("add", (x, y) -> x + y);
        operations.put("sub", (x, y) -> x - y);
        operations.put("mul", (x, y) -> x * y);
        operations.put("max", Math::max);
        operations.put("min", Math::min);
        operations.put("pow", (x, y) -> {
            long result = 1;
            for (int i = 0; i < y; i++) {
                result *= x;
            }
            return result;
        });

        while (in.hasNext()) {
            String name = in.next();
            BinaryOperator<Long> operation = operations.get(name);
            if (operation == null) {
                System.out.println(name + ": неизвестная операция");
            } else {
                System.out.println(name + ": " + operation.apply(a, b));
            }
        }
    }
}
