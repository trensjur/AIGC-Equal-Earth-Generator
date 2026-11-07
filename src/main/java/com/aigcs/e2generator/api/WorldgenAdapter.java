package com.aigcs.e2generator.api;

import javax.annotation.Nullable;

import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * 世界生成适配器 SPI（Phase 6）。
 * <p>
 * 原版 {@code NoiseBasedChunkGenerator} 由核心 Mixin 直接支持；其它自定义生成器
 * （TFC 的 {@code TFCChunkGenerator}、整合包私有的生成器等）可以通过本接口接入。
 * <p>
 * 注册方式（在模组构造期调用）：
 *
 * <pre>{@code
 * WorldgenAdapters.register(new MyAdapter());
 * }</pre>
 *
 * 适配器必须保证：
 * <ul>
 * <li>{@link #supports(ServerLevel, ChunkGenerator)} 是纯查询，不修改生成器；</li>
 * <li>{@link #attach(ServerLevel, WorldShape)} 可以在 {@code LevelEvent.Load}
 * 时安全调用；</li>
 * <li>形状可以为 {@code null}（表示该维度未启用球形世界），此时应清理自身状态。</li>
 * </ul>
 */
public interface WorldgenAdapter {

    /** 唯一的适配器 id（仅用于日志/去重）。 */
    ResourceLocation id();

    /** 该适配器是否处理这个维度/生成器。 */
    boolean supports(ServerLevel level, ChunkGenerator generator);

    /**
     * 挂载（或清除）世界形状。{@code shape} 为 {@code null} 时表示关闭。
     */
    void attach(ServerLevel level, @Nullable WorldShape shape);

    /**
     * 可选的掩码查询，供其它模组/调试命令使用。
     * 默认返回 1（完全可生成陆地）。
     */
    default double maskAt(ServerLevel level, double x, double z) {
        return 1.0;
    }
}
