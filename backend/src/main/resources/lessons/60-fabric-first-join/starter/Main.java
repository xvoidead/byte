import java.util.Scanner;

import dev.byteide.fabric.FabricTestRuntime;

public class Main {
    public static void main(String[] args) {
        FabricTestRuntime runtime = new FabricTestRuntime();
        runtime.load(FirstJoinMod.class);
        Scanner input = new Scanner(System.in);
        while (input.hasNextLine()) {
            String line = input.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+", 2);
            String rest = parts.length == 2 ? parts[1] : "";
            switch (parts[0]) {
                case "join" -> runtime.join(rest);
                case "quit" -> runtime.quit(rest);
                case "tick" -> {
                    int ticks = Integer.parseInt(rest);
                    for (int i = 0; i < ticks; i++) {
                        runtime.tick();
                    }
                }
                case "break" -> {
                    String[] argsForBreak = rest.split("\\s+");
                    if (argsForBreak.length == 5) {
                        runtime.breakBlock(argsForBreak[0], Integer.parseInt(argsForBreak[1]),
                                Integer.parseInt(argsForBreak[2]), Integer.parseInt(argsForBreak[3]), argsForBreak[4]);
                    }
                }
                default -> throw new IllegalArgumentException("Неизвестная команда сценария: " + parts[0]);
            }
            runtime.drainMessages().forEach(System.out::println);
        }
    }
}
