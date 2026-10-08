package net.minecraft.block;

import java.util.Objects;

/** Minimal block identity used by the Fabric lesson simulator. */
public final class Block {

    private final String name;

    Block(String name) {
        this.name = Objects.requireNonNull(name);
    }

    boolean matches(String blockName) {
        return name.equals(blockName);
    }
}
