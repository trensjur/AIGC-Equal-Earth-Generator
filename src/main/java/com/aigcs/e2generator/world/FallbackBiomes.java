package com.aigcs.e2generator.world;

import javax.annotation.Nullable;

import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 缓存的兜底海洋群系查询。地图外统一返回同一个 {@link Holder}。
 */
public final class FallbackBiomes {

    private static volatile ResourceLocation cachedId;
    private static volatile Holder<Biome> cached;

    private FallbackBiomes() {
    }

    @Nullable
    public static Holder<Biome> get(WorldShape shape) {
        ResourceLocation id = shape.fallbackBiome();
        Holder<Biome> local = cached;
        if (local != null && id.equals(cachedId)) {
            return local;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        Holder<Biome> holder = server.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolder(shape.fallbackBiomeKey())
                .orElse(null);
        if (holder != null) {
            cachedId = id;
            cached = holder;
        }
        return holder;
    }
}
