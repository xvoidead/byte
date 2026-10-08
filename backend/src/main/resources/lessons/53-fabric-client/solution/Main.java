import java.util.Scanner;

import dev.byteide.fabric.FabricClientTestRuntime;

public class Main {
    public static void main(String[] args) {
        FabricClientTestRuntime runtime = new FabricClientTestRuntime();
        runtime.load(ClientKeyMod.class);
        Scanner input = new Scanner(System.in);
        while (input.hasNextLine()) {
            String line = input.nextLine().trim();
            if (line.equals("press G")) {
                runtime.pressKey(71);
            } else if (line.equals("press F")) {
                runtime.pressKey(70);
            } else if (line.equals("tick")) {
                runtime.tick();
            }
            runtime.drainMessages().forEach(System.out::println);
        }
    }
}
