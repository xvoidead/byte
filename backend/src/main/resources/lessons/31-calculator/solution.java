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
        long value = parseTerm();
        while (peek() == '+' || peek() == '-') {
            char op = next();
            long right = parseTerm();
            value = op == '+' ? value + right : value - right;
        }
        return value;
    }

    long parseTerm() {
        long value = parseFactor();
        while (peek() == '*' || peek() == '/') {
            char op = next();
            long right = parseFactor();
            if (op == '*') {
                value *= right;
            } else {
                if (right == 0) {
                    throw new CalcException("деление на ноль");
                }
                value /= right;
            }
        }
        return value;
    }

    long parseFactor() {
        char c = peek();
        if (c == '-') {
            next();
            return -parseFactor();
        }
        if (c == '(') {
            next();
            long value = parseExpression();
            if (peek() != ')') {
                throw new CalcException("неверное выражение");
            }
            next();
            return value;
        }
        if (Character.isDigit(c)) {
            skipSpaces();
            long value = 0;
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
                value = value * 10 + (text.charAt(pos) - '0');
                pos++;
            }
            return value;
        }
        throw new CalcException("неверное выражение");
    }

    private char peek() {
        skipSpaces();
        return pos < text.length() ? text.charAt(pos) : '\0';
    }

    private char next() {
        char c = peek();
        pos++;
        return c;
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
