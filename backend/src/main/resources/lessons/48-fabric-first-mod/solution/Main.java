import dev.byteide.fabric.FabricTestRuntime;

public class Main {
    public static void main(String[] args) {
        FabricTestRuntime runtime = new FabricTestRuntime();
        runtime.load(FirstMod.class);
    }
}
