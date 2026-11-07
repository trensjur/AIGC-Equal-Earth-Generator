package com.aigcs.e2generator.core;

/**
 * 世界生成控制器中选择的"星球参数"。
 * <p>
 * 这些参数只在<b>新建世界</b>时生效；创建完成后世界形状写入存档，不再改变。
 */
public record PlanetSettings(double radiusChunks, boolean allowPolarContinents, boolean allowCrossBoundary) {
}
