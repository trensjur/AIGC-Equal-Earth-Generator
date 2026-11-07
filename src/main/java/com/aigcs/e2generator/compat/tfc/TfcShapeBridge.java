package com.aigcs.e2generator.compat.tfc;

import javax.annotation.Nullable;

import com.aigcs.e2generator.Config;
import com.aigcs.e2generator.core.WorldShape;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

/**
 * TFC 适配桥（Phase 4）。
 * <p>
 * TFC 的 {@code RegionGenerator} / {@code Region.Point} 在生成时拿不到
 * {@code ServerLevel}，
 * 因此这里保存当前世界形状。形状本身不可变，且 TFC 一次只生成一个主世界，使用
 * {@code volatile} 静态引用是安全的。
 * <p>
 * 本类<b>不引用任何 TFC 类型</b>，因此核心 jar 在没有 TFC 时也能正常加载。
 */
public final class TfcShapeBridge {

    public static final String TFC_MODID = "tfc";

    /** TFC 1.21.1 的 {@code Units.GRID_BITS = 7}，即 128 方块/格。 */
    public static final int DEFAULT_GRID_BLOCK_SIZE = 128;

    @Nullable
    private static volatile WorldShape shape;

    private TfcShapeBridge() {
    }

    public static boolean isTfcLoaded() {
        try {
            ModList modList = ModList.get();
            if (modList != null) {
                return modList.isLoaded(TFC_MODID);
            }
        } catch (RuntimeException ignored) {
            // ModList 尚未初始化：退回 FMLLoader / 类探测。
        }
        try {
            return FMLLoader.getLoadingModList() != null
                    && FMLLoader.getLoadingModList().getModFileById(TFC_MODID) != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** 当前维度是否应当启用 TFC 适配（TFC 已加载且配置开启）。 */
    public static boolean isEnabled() {
        if (!isTfcLoaded()) {
            return false;
        }
        try {
            return Config.TFC_COMPAT.get();
        } catch (IllegalStateException ignored) {
            // 配置尚未加载时默认启用；真正的生成发生在配置加载之后。
            return true;
        }
    }

    /** 由 {@code ShapeManager} 在维度加载时调用；传入 {@code null} 表示关闭。 */
    public static void setShape(@Nullable WorldShape newShape) {
        shape = newShape;
    }

    /** 当前形状；TFC 未加载或未启用时返回 {@code null}。 */
    @Nullable
    public static WorldShape shape() {
        return isEnabled() ? shape : null;
    }

    /** 是否是需要 TFC 适配的生成器（只比较类名，避免硬依赖）。 */
    public static boolean isTfcGenerator(@Nullable Object generator) {
        if (generator == null) {
            return false;
        }
        Class<?> type = generator.getClass();
        while (type != null) {
            if (type.getName().startsWith("net.dries007.tfc.")) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    /**
     * 供 {@code ChunkGeneratorMixin} 使用：TFC 生成器没有实现 {@code ShapeAware}，
     * 这里按类名回退到全局桥。
     */
    @Nullable
    public static WorldShape shapeForGenerator(@Nullable Object generator) {
        return isTfcGenerator(generator) ? shape() : null;
    }

    /** TFC region 网格坐标 → 方块坐标。 */
    public static int gridToBlock(int grid) {
        int scale = DEFAULT_GRID_BLOCK_SIZE;
        try {
            scale = Config.TFC_GRID_BLOCK_SIZE.get();
        } catch (IllegalStateException ignored) {
            // 使用 TFC 1.21.1 的默认值。
        }
        return grid * scale;
    }

    /** TFC region 网格点上的掩码（0 = 海洋，1 = 陆地）。 */
    public static double maskAtGrid(int gridX, int gridZ) {
        WorldShape current = shape();
        if (current == null) {
            return 1.0;
        }
        return current.mask(gridToBlock(gridX), gridToBlock(gridZ));
    }

    /** {@code RegionGenerator#continentFactor} 的替代值。 */
    public static float continentFactor(int gridX, int gridZ) {
        return (float) maskAtGrid(gridX, gridZ);
    }

    /** 该网格点是否位于地图外（应取消 setLand/setMountain 等）。 */
    public static boolean shouldCancelPoint(int gridX, int gridZ) {
        return shape() != null && maskAtGrid(gridX, gridZ) <= 0.0;
    }
}
