package com.aigcs.e2generator.api;

import javax.annotation.Nullable;

import com.aigcs.e2generator.core.WorldShape;

/**
 * 由 Mixin 注入到 {@code NoiseBasedChunkGenerator} / {@code MultiNoiseBiomeSource}
 * 的挂载点。
 * <p>
 * 设计原则：形状挂在<b>每个实例</b>上，而不是全局静态单例。
 * 这样多世界、并行生成（c2me）下不会互相串线。
 */
public interface ShapeAware {

    @Nullable
    WorldShape spherical$getShape();

    void spherical$setShape(@Nullable WorldShape shape);
}
