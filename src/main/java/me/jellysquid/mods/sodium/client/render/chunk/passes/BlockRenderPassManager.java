package me.jellysquid.mods.sodium.client.render.chunk.passes;

import com.google.gson.Gson;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.resource.ResourceLoader;
import me.jellysquid.mods.sodium.client.resource.shader.json.BlockMapJson;
import me.jellysquid.mods.sodium.client.resource.shader.json.RenderPassJson;
import me.jellysquid.mods.sodium.client.resource.shader.json.ShaderJson;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps vanilla render layers to render passes used by Sodium. This provides compatibility with the render layers already
 * used by the base game.
 */
public class BlockRenderPassManager {
    private final Map<Block, BlockRenderPass> blocks;
    private final Map<Fluid, BlockRenderPass> fluids;

    private final BlockRenderPass defaultPass;

    private final Set<BlockRenderPass> totalPasses;

    public BlockRenderPassManager(Map<Block, BlockRenderPass> blocks,
                                  Map<Fluid, BlockRenderPass> fluids,
                                  BlockRenderPass defaultPass) {
        this.blocks = blocks;
        this.fluids = fluids;
        this.defaultPass = defaultPass;

        this.totalPasses = Stream.of(blocks.values(), fluids.values(), Collections.singletonList(defaultPass))
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    public BlockRenderPass getRenderPass(Block block) {
        return getRenderPass(this.blocks, block);
    }

    public BlockRenderPass getRenderPass(Fluid fluid) {
        return getRenderPass(this.fluids, fluid);
    }

    private <T> BlockRenderPass getRenderPass(Map<T, BlockRenderPass> registry, T key) {
        BlockRenderPass pass = registry.get(key);

        if (pass == null) {
            return this.defaultPass;
        }

        return pass;
    }

    /**
     * Creates a set of render pass mappings to vanilla render layers which closely mirrors the rendering
     * behavior of vanilla.
     */
    public static BlockRenderPassManager create() {
        RenderPassCache cache = new RenderPassCache();

        BlockRenderPass cutoutMipped = cache.get(new Identifier("sodium", "block_cutout_mipped"));
        BlockRenderPass cutout = cache.get(new Identifier("sodium", "block_cutout"));
        BlockRenderPass solid = cache.get(new Identifier("sodium", "block_solid"));
        BlockRenderPass translucent = cache.get(new Identifier("sodium", "block_translucent"));
        BlockRenderPass tripwire = cache.get(new Identifier("sodium", "block_tripwire"));

        Map<Block, BlockRenderPass> blocks = new Reference2ObjectOpenHashMap<>();
        Map<Fluid, BlockRenderPass> fluids = new Reference2ObjectOpenHashMap<>();

        for (Map.Entry<Block, RenderLayer> entry : RenderLayers.BLOCKS.entrySet()) {
            Block block = entry.getKey();
            RenderLayer layer = entry.getValue();

            BlockRenderPass pass;

            if (layer == RenderLayer.getCutoutMipped()) {
                pass = cutoutMipped;
            } else if (layer == RenderLayer.getCutout()) {
                pass = cutout;
            } else if (layer == RenderLayer.getTranslucent()) {
                pass = translucent;
            } else if (layer == RenderLayer.getTripwire()) {
                pass = tripwire;
            } else {
                pass = solid;
            }

            blocks.put(block, pass);
        }

        tryLoadMappings(new Identifier("sodium", "maps/block_map.json"), cache, blocks, fluids);

        for (Map.Entry<Fluid, RenderLayer> entry : RenderLayers.FLUIDS.entrySet()) {
            Fluid fluid = entry.getKey();
            RenderLayer layer = entry.getValue();

            BlockRenderPass pass;

            if (layer == RenderLayer.getTranslucent()) {
                pass = translucent;
            } else if (layer == RenderLayer.getSolid()) {
                pass = solid;
            } else {
                throw new IllegalStateException("Unknown render layer for fluid: " + fluid);
            }

            fluids.put(fluid, pass);
        }

        return new BlockRenderPassManager(blocks, fluids, solid);
    }

    private static void tryLoadMappings(Identifier id, RenderPassCache cache, Map<Block, BlockRenderPass> blocks, Map<Fluid, BlockRenderPass> fluids) {
        BlockMapJson json = tryLoadBlockMapJson(id);

        if (json == null) {
            return;
        }

        for (Map.Entry<String, String> entry : json.getBlockRedefinitions()) {
            Optional<Block> block = Registry.BLOCK.getOrEmpty(new Identifier(entry.getKey()));

            if (block.isEmpty()) {
                SodiumClientMod.logger().warn("Couldn't find block with name {}", entry.getKey());
                continue;
            }

            blocks.put(block.get(), cache.get(new Identifier(entry.getValue())));
        }
    }

    private static BlockMapJson tryLoadBlockMapJson(Identifier id) {
        try (InputStream in = ResourceLoader.EMBEDDED.open(id)) {
            if (in == null) {
                return null;
            }

            return GSON.fromJson(new InputStreamReader(in), BlockMapJson.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read block map json", e);
        }
    }

    public Iterable<BlockRenderPass> getRenderPasses() {
        return this.totalPasses;
    }

    public Iterable<BlockRenderPass> getSolidRenderPasses() {
        return () -> this.totalPasses.stream().filter(BlockRenderPass::isSolid).iterator();
    }

    public Iterable<BlockRenderPass> getTranslucentRenderPasses() {
        return () -> this.totalPasses.stream().filter(BlockRenderPass::isTranslucent).iterator();
    }

    private static final Gson GSON = new Gson();

    private static RenderPassShader createShaderInfo(ShaderJson json) {
        ShaderConstants.Builder constants = ShaderConstants.builder();

        if (json.getConstants() != null) {
            for (Map.Entry<String, String> entry : json.getConstants().entrySet()) {
                constants.add(entry.getKey(), entry.getValue());
            }
        }

        return new RenderPassShader(new Identifier(json.getSource()), constants.build());
    }

    private static RenderLayer getLayerByName(String name) {
        return switch (name) {
            case "minecraft:solid" -> RenderLayer.getSolid();
            case "minecraft:translucent" -> RenderLayer.getTranslucent();
            case "minecraft:cutout" -> RenderLayer.getCutout();
            case "minecraft:cutout_mipped" -> RenderLayer.getCutoutMipped();
            case "minecraft:tripwire" -> RenderLayer.getTripwire();
            default -> throw new IllegalArgumentException("Unknown layer name: " + name);
        };
    }

    private static class RenderPassCache {
        private final Map<Identifier, BlockRenderPass> cache = new Reference2ObjectOpenHashMap<>();

        public BlockRenderPass get(Identifier id) {
            return this.cache.computeIfAbsent(id, RenderPassCache::load);
        }

        private static BlockRenderPass load(Identifier name) {
            RenderPassJson json;
            Identifier path = new Identifier(name.getNamespace(), FilenameUtils.concat("shaders/pass", name.getPath() + ".json"));

            try (InputStream in = ResourceLoader.EMBEDDED.open(path)) {
                json = GSON.fromJson(new InputStreamReader(in), RenderPassJson.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read render pass specification", e);
            }

            return new BlockRenderPass(getLayerByName(json.getLayer()), json.isTranslucent(),
                    createShaderInfo(json.getShader("vertex")),
                    createShaderInfo(json.getShader("fragment")));
        }
    }
}
