import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.text.Text;

public class OnlineCountMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            int count = server.getPlayerManager().getPlayerList().size();
            server.getPlayerManager().broadcast(Text.literal("Сейчас онлайн: " + count), false);
        });
    }
}
