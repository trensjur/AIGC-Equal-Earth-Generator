package com.aigcs.e2generator.api;

import java.util.OptionalDouble;

/**
 * 公开接口：查询"当前世界使用的星球半径"。
 * <p>
 * 该接口由区块生成器实现（Mixin 注入到原版 {@code NoiseBasedChunkGenerator}），
 * 因此其它模组可以直接 {@code instanceof} 查询，不必拿到 {@code Level}：
 *
 * <pre>{@code
 * if (level.getChunkSource().getGenerator() instanceof PlanetRadiusProvider provider) {
 *     provider.planetRadiusChunks().ifPresent(radius -> ...);
 * }
 * }</pre>
 *
 * 也可以用更方便的静态入口 {@link SphericalWorldApi}。
 */
public interface PlanetRadiusProvider {

    /** 当前世界使用的星球半径（区块）；不是球形世界时为空。 */
    OptionalDouble planetRadiusChunks();

    /** 当前世界使用的星球半径（方块）；不是球形世界时为空。 */
    OptionalDouble planetRadiusBlocks();
}
