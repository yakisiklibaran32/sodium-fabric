package me.jellysquid.mods.sodium.client.resource.shader.json;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class RenderPassJson {
    @SerializedName("name")
    private String name;

    @SerializedName("layer")
    private String layer;

    @SerializedName("translucent")
    private boolean translucent;

    @SerializedName("cutout")
    private boolean cutout;

    @SerializedName("shaders")
    private Map<String, ShaderJson> shaders;

    public String getLayer() {
        return this.layer;
    }

    public String getName() {
        return this.name;
    }

    public boolean isTranslucent() {
        return this.translucent;
    }

    public boolean isCutout() {
        return this.cutout;
    }

    public ShaderJson getShader(String name) {
        return this.shaders.get(name);
    }
}
