package net.fabricmc.fabric.api.event.player;

import dev.byteide.fabric.TestEvent;
import net.minecraft.block.BlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Block-break event subset used by Byte's Fabric lessons. */
public final class PlayerBlockBreakEvents {

    public static final TestEvent<Before> BEFORE = new TestEvent<>();
    public static final TestEvent<After> AFTER = new TestEvent<>();

    private PlayerBlockBreakEvents() {
    }

    @FunctionalInterface
    public interface Before {
        boolean before(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity);
    }

    @FunctionalInterface
    public interface After {
        void after(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity);
    }
}
