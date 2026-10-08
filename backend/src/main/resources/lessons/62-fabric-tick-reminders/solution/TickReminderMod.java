import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

public class TickReminderMod implements ModInitializer {
    private int ticks;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
    }

    private void onTick(MinecraftServer server) {
        ticks++;
        if (ticks % 5 == 0) {
            server.getPlayerManager().broadcast(Text.literal("Прошло 5 тиков."), false);
        }
    }
}
