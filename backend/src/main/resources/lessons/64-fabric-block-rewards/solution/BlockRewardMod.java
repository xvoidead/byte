import java.util.HashMap;
import java.util.Map;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.text.Text;

public class BlockRewardMod implements ModInitializer {
    private final Map<String, Integer> scores = new HashMap<>();

    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            int reward;
            if (state.isOf(Blocks.DIAMOND_ORE)) {
                reward = 5;
            } else if (state.isOf(Blocks.STONE)) {
                reward = 1;
            } else {
                return;
            }
            String name = player.getName().getString();
            int total = scores.merge(name, reward, Integer::sum);
            player.sendMessage(Text.literal("Очки: +" + reward + ". Всего: " + total), false);
        });
    }
}
