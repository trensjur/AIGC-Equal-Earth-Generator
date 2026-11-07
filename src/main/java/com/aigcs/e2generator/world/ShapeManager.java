package com.aigcs.e2generator.world;

import javax.annotation.Nullable;

import com.aigcs.e2generator.AIGCsEqualEarthGenerator;
import com.aigcs.e2generator.Config;
import com.aigcs.e2generator.api.ShapeAware;
import com.aigcs.e2generator.api.WorldgenAdapter;
import com.aigcs.e2generator.api.WorldgenAdapters;
import com.aigcs.e2generator.api.event.WorldShapeAttachedEvent;
import com.aigcs.e2generator.compat.OptimizationCompat;
import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 把 {@link WorldShape} 挂载到每个维度的生成器与群系源上。
 * <p>
 * 不使用全局静态单例（TFC Eratosthenes 的 {@code MapProjectionHolder} 在多世界下是隐患），
 * 而是每个 generator / biome source 实例各持一份不可变形状。
 */
public final class ShapeManager {

    private ShapeManager() {
    }

    /**
     * 读取（或首次创建）当前存档的世界形状。
     */
    @Nullable
    public static WorldShape resolve(ServerLevel level) {
        if (!Config.isDimensionEnabled(level.dimension())) {
            return null;
        }
        WorldShapeData data = level.getDataStorage().computeIfAbsent(WorldShapeData.FACTORY, WorldShapeData.DATA_NAME);
        return data.shape();
    }

    /**
     * 在维度加载时挂载形状。必须在区块生成前调用。
     */
    public static void attach(ServerLevel level) {
        WorldShape shape = resolve(level);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        boolean attached = false;
        if (generator instanceof ShapeAware aware) {
            aware.spherical$setShape(shape);
            attached = true;
        }
        BiomeSource biomeSource = generator.getBiomeSource();
        if (biomeSource instanceof ShapeAware aware) {
            aware.spherical$setShape(shape);
            attached = true;
        }

        // Phase 4/6：可选生成器适配器（例如 TFC）。即使 shape 为 null 也要调用，
        // 以便适配器清理自己保存的旧形状；未启用本模组的维度不触碰适配器。
        WorldgenAdapter adapter = WorldgenAdapters.find(level, generator);
        if (adapter != null && (shape != null || Config.isDimensionEnabled(level.dimension()))) {
            try {
                adapter.attach(level, shape);
                attached = true;
            } catch (RuntimeException exception) {
                AIGCsEqualEarthGenerator.LOGGER.error("[e2generator] Worldgen adapter {} failed to attach",
                        adapter.id(), exception);
            }
        }

        if (shape != null) {
            if (!attached) {
                AIGCsEqualEarthGenerator.LOGGER.warn("[e2generator] Generator {} in {} has no shape adapter; "
                        + "falling back to seam wrapping, pole pushback and the public API only.",
                        generator.getClass().getName(), level.dimension().location());
            }
            NeoForge.EVENT_BUS.post(new WorldShapeAttachedEvent(level, generator, shape));
            OptimizationCompat.onShapeAttached(level, shape);
            AIGCsEqualEarthGenerator.LOGGER.info("[e2generator] {} -> {}", level.dimension().location(),
                    shape.describe());
        }
    }

    /**
     * 读取已挂载的形状（热路径使用，避免访问 SavedData）。
     */
    @Nullable
    public static WorldShape shapeOf(LevelAccessor level) {
        if (level instanceof ServerLevel serverLevel) {
            ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
            if (generator instanceof ShapeAware aware) {
                return aware.spherical$getShape();
            }
        }
        return null;
    }
}
