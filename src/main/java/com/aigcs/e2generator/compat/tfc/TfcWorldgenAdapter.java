package com.aigcs.e2generator.compat.tfc;

import javax.annotation.Nullable;

import com.aigcs.e2generator.AIGCsEqualEarthGenerator;
import com.aigcs.e2generator.api.WorldgenAdapter;
import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * 把 {@link WorldShape} 交给 {@link TfcShapeBridge} 的生成器适配器。
 * <p>
 * 只按类名识别 TFC 生成器，不建立编译期依赖；未安装 TFC 时
 * {@link #supports(ServerLevel, ChunkGenerator)} 永远返回 {@code false}。
 */
public final class TfcWorldgenAdapter implements WorldgenAdapter {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            AIGCsEqualEarthGenerator.MODID, "tfc");

    public static final TfcWorldgenAdapter INSTANCE = new TfcWorldgenAdapter();

    private TfcWorldgenAdapter() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public boolean supports(ServerLevel level, ChunkGenerator generator) {
        return TfcShapeBridge.isTfcGenerator(generator);
    }

    @Override
    public void attach(ServerLevel level, @Nullable WorldShape shape) {
        TfcShapeBridge.setShape(shape);
    }

    @Override
    public double maskAt(ServerLevel level, double x, double z) {
        WorldShape shape = TfcShapeBridge.shape();
        return shape == null ? 1.0 : shape.mask(x, z);
    }
}
