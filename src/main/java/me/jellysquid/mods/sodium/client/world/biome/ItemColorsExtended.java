package me.jellysquid.mods.sodium.client.world.biome;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

public interface ItemColorsExtended {
    ItemColor getColorProvider(ItemStack stack);
}
