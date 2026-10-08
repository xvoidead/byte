import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.text.Text;

public class SpawnGuardMod implements ModInitializer {
    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            int x = pos.getX();
            int z = pos.getZ();
            if (x * x + z * z <= 25) {
                player.sendMessage(Text.literal("У спавна нельзя ломать блоки."), false);
                return false;
            }
            return true;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) ->
                player.sendMessage(Text.literal("Блок добыт!"), false));
    }
}
