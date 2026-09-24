import java.util.Scanner;

enum Direction {
    NORTH, EAST, SOUTH, WEST
}

record Position(int x, int y) {
}

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String commands = in.hasNextLine() ? in.nextLine().trim() : "";
        Position position = new Position(0, 0);
        Direction direction = Direction.NORTH;
        int steps = 0;

        for (char command : commands.toCharArray()) {
            // Обработайте команду
        }

        System.out.println("Позиция: (" + position.x() + ", " + position.y() + ")");
    }
}
