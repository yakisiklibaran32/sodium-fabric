package me.jellysquid.mods.sodium.client.world.cloned;

import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;

public interface SimpleBitStorageExtended {
    <T> void copyUsingPalette(T[] out, ClonedPalette<T> palette);
}
