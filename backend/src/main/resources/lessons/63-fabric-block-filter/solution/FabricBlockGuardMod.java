import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.text.Text;

public class FabricBlockGuardMod implements ModInitializer {
    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (state.isOf(Blocks.BEDROCK)) {
                player.sendMessage(Text.literal("Бедрок защищён."), false);
                return false;
            }
            return true;
        });
    }
}
