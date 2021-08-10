package me.jellysquid.mods.sodium.client.resource.shader.json;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class BlockMapJson {
    @SerializedName("tags")
    private Map<String, String> tags;

    @SerializedName("blocks")
    private Map<String, String> blocks;

    public Iterable<? extends Map.Entry<String, String>> getTagRedefinitions() {
        return this.tags.entrySet();
    }

    public Iterable<? extends Map.Entry<String, String>> getBlockRedefinitions() {
        return this.blocks.entrySet();
    }
}
