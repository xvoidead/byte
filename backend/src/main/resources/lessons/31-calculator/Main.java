import java.util.Scanner;

class CalcException extends RuntimeException {
    CalcException(String message) {
        super(message);
    }
}

class Parser {
    private final String text;
    private int pos = 0;

    Parser(String text) {
        this.text = text;
    }

    long parse() {
        long value = parseExpression();
        skipSpaces();
        if (pos < text.length()) {
            throw new CalcException("неверное выражение");
        }
        return value;
    }

    long parseExpression() {
        // выражение = слагаемое { ("+" | "-") слагаемое }
        return parseTerm();
    }

    long parseTerm() {
        // слагаемое = множитель { ("*" | "/") множитель }
        return parseFactor();
    }

    long parseFactor() {
        // множитель = число | "(" выражение ")" | "-" множитель
        throw new CalcException("неверное выражение");
    }

    private void skipSpaces() {
        while (pos < text.length() && text.charAt(pos) == ' ') {
            pos++;
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            String line = in.nextLine();
            try {
                System.out.println(new Parser(line).parse());
            } catch (CalcException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }
}
