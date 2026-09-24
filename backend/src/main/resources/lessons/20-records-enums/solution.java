import java.util.Scanner;

enum Direction {
    NORTH("север", 0, 1), EAST("восток", 1, 0), SOUTH("юг", 0, -1), WEST("запад", -1, 0);

    private final String title;
    final int dx;
    final int dy;

    Direction(String title, int dx, int dy) {
        this.title = title;
        this.dx = dx;
        this.dy = dy;
    }

    Direction right() {
        return values()[(ordinal() + 1) % 4];
    }

    Direction left() {
        return values()[(ordinal() + 3) % 4];
    }

    String title() {
        return title;
    }
}

record Position(int x, int y) {
    Position move(Direction d, int distance) {
        return new Position(x + d.dx * distance, y + d.dy * distance);
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String commands = in.hasNextLine() ? in.nextLine().trim() : "";
        Position position = new Position(0, 0);
        Direction direction = Direction.NORTH;
        int steps = 0;
        for (char command : commands.toCharArray()) {
            switch (command) {
                case 'F' -> {
                    position = position.move(direction, 1);
                    steps++;
                }
                case 'B' -> {
                    position = position.move(direction, -1);
                    steps++;
                }
                case 'L' -> direction = direction.left();
                case 'R' -> direction = direction.right();
                default -> {
                }
            }
        }
        System.out.println("Позиция: (" + position.x() + ", " + position.y() + ")");
        System.out.println("Направление: " + direction.title());
        System.out.println("Шагов: " + steps);
    }
}
