package net.minecraft.block;

import java.util.Objects;

/** Minimal block state used by Fabric block-event lessons. */
public final class BlockState {

    private final String blockName;

    public BlockState(String blockName) {
        this.blockName = Objects.requireNonNull(blockName);
    }

    public String getBlockName() {
        return blockName;
    }
}
