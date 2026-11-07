package com.aigcs.e2generator.world;

import javax.annotation.Nullable;

import com.aigcs.e2generator.Config;
import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.world.level.ChunkPos;

/**
 * 生成期间的"当前形状"上下文（Phase 1 + Phase 5 性能优化）。
 * <p>
 * {@code NoiseChunk} 的构造函数拿不到所属的区块生成器，因此我们在
 * {@code NoiseBasedChunkGenerator#createNoiseChunk} 中压入形状、在
 * {@code NoiseChunk.forChunk} 返回后弹出。
 * <p>
 * Phase 5 增加了<b>每区块 16×16 掩码缓存</b>：在 {@code fillFromNoise} 开始时
 * 预计算一次，密度包装器只查表；缓存使用 {@code ThreadLocal}，并在填充结束时
 * 显式清理，兼容 c2me 的 worker 线程池复用。
 */
public final class SphericalContext {

    private static final ThreadLocal<WorldShape> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<ChunkMask> CHUNK = new ThreadLocal<>();

    private SphericalContext() {
    }

    public static void push(@Nullable WorldShape shape) {
        CURRENT.set(shape);
    }

    public static void pop() {
        CURRENT.remove();
    }

    @Nullable
    public static WorldShape current() {
        return CURRENT.get();
    }

    /** 开始填充一个区块：预计算 16×16 掩码。 */
    public static void beginChunk(@Nullable WorldShape shape, ChunkPos pos) {
        if (shape == null || !chunkCacheEnabled()) {
            CHUNK.remove();
            return;
        }
        CHUNK.set(ChunkMask.build(shape, pos));
    }

    /** 结束填充：必须调用，避免 c2me 的工作线程保留上一区块的缓存。 */
    public static void endChunk() {
        CHUNK.remove();
    }

    /**
     * 掩码查询（方块坐标）。若当前区块缓存命中则查表，否则回退到直接计算。
     */
    public static double maskAt(int x, int z) {
        WorldShape shape = CURRENT.get();
        return maskAt(x, z, shape);
    }

    /**
     * 掩码查询（方块坐标），缓存未命中时使用 {@code fallback} 形状。
     * <p>
     * {@code MaskedDensity} 在 {@code fillArray} 时形状上下文可能已经弹出，
     * 因此必须把包装器自己的形状传进来，保证跨区块边界的采样也正确。
     */
    public static double maskAt(int x, int z, @Nullable WorldShape fallback) {
        ChunkMask cache = CHUNK.get();
        if (cache != null) {
            double cached = cache.mask(x, z);
            if (!Double.isNaN(cached)) {
                return cached;
            }
        }
        WorldShape shape = CURRENT.get();
        if (shape == null) {
            shape = fallback;
        }
        return shape == null ? 1.0 : shape.mask(x, z);
    }

    private static boolean chunkCacheEnabled() {
        try {
            return Config.CHUNK_MASK_CACHE.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    /** 一个区块的不可变掩码快照（256 个 double，约 2 KB）。 */
    private static final class ChunkMask {

        private final int minX;
        private final int minZ;
        private final double[] masks = new double[16 * 16];

        private ChunkMask(int minX, int minZ) {
            this.minX = minX;
            this.minZ = minZ;
        }

        static ChunkMask build(WorldShape shape, ChunkPos pos) {
            ChunkMask cache = new ChunkMask(pos.getMinBlockX(), pos.getMinBlockZ());
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    cache.masks[z * 16 + x] = shape.mask(cache.minX + x, cache.minZ + z);
                }
            }
            return cache;
        }

        /** 命中返回掩码，未命中返回 {@link Double#NaN}。 */
        double mask(int x, int z) {
            int localX = x - this.minX;
            int localZ = z - this.minZ;
            if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) {
                return Double.NaN;
            }
            return this.masks[localZ * 16 + localX];
        }
    }
}
