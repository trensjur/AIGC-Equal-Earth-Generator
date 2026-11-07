package com.aigcs.e2generator.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import com.aigcs.e2generator.AIGCsEqualEarthGenerator;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * {@link WorldgenAdapter} 的注册表（Phase 6）。
 * <p>
 * 线程安全：注册通常在模组构造期发生，查询发生在维度加载时。
 * 同名适配器会被后注册的替换，方便整合包覆盖默认行为。
 */
public final class WorldgenAdapters {

    private static final List<WorldgenAdapter> ADAPTERS = new CopyOnWriteArrayList<>();

    private WorldgenAdapters() {
    }

    public static void register(WorldgenAdapter adapter) {
        if (adapter == null || adapter.id() == null) {
            return;
        }
        ADAPTERS.removeIf(existing -> existing.id().equals(adapter.id()));
        ADAPTERS.add(adapter);
    }

    public static List<WorldgenAdapter> all() {
        return List.copyOf(ADAPTERS);
    }

    @Nullable
    public static WorldgenAdapter find(ResourceLocation id) {
        for (WorldgenAdapter adapter : ADAPTERS) {
            if (adapter.id().equals(id)) {
                return adapter;
            }
        }
        return null;
    }

    @Nullable
    public static WorldgenAdapter find(ServerLevel level, ChunkGenerator generator) {
        for (WorldgenAdapter adapter : ADAPTERS) {
            try {
                if (adapter.supports(level, generator)) {
                    return adapter;
                }
            } catch (RuntimeException exception) {
                AIGCsEqualEarthGenerator.LOGGER.error("[e2generator] Worldgen adapter {} failed in supports()",
                        adapter.id(), exception);
            }
        }
        return null;
    }
}
