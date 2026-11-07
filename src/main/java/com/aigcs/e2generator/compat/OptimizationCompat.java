package com.aigcs.e2generator.compat;

import com.aigcs.e2generator.AIGCsEqualEarthGenerator;
import com.aigcs.e2generator.Config;
import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;

/**
 * 优化模组兼容（Phase 5）。
 * <p>
 * 这里<b>不做任何 Mixin</b>：c2me 与 Distant Horizons 的内部类更新频繁，
 * 我们不与之竞争注入点。此处只做：
 * <ul>
 * <li>c2me：检测并记录当前的线程安全模式（不可变形状 + ThreadLocal 掩码缓存）；</li>
 * <li>Distant Horizons：根据世界半径给出推荐的 “Earth Curve Ratio”，
 * 并说明海洋缓冲带的视觉效果。</li>
 * </ul>
 */
public final class OptimizationCompat {

    public static final String C2ME_MODID = "c2me";
    public static final String DISTANT_HORIZONS_MODID = "distanthorizons";

    private OptimizationCompat() {
    }

    public static boolean isC2meLoaded() {
        return isLoaded(C2ME_MODID);
    }

    public static boolean isDistantHorizonsLoaded() {
        return isLoaded(DISTANT_HORIZONS_MODID);
    }

    /** 在通用初始化阶段调用一次，便于整合包作者确认兼容模式。 */
    public static void logStartupStatus() {
        AIGCsEqualEarthGenerator.LOGGER.info(
                "[e2generator] Compatibility mode: c2me={}, distantHorizons={}, densityMask={}, chunkMaskCache={}",
                isC2meLoaded(), isDistantHorizonsLoaded(), boolConfig(Config.DENSITY_MASK, true),
                boolConfig(Config.CHUNK_MASK_CACHE, true));
        if (isC2meLoaded()) {
            AIGCsEqualEarthGenerator.LOGGER.info(
                    "[e2generator] c2me detected: world shape is immutable, generator state is per-instance, "
                            + "and per-chunk mask caches use ThreadLocal with explicit cleanup.");
        }
    }

    /** 维度挂载形状后调用：对 DH 输出推荐设置。 */
    public static void onShapeAttached(ServerLevel level, WorldShape shape) {
        if (!isDistantHorizonsLoaded() || !boolConfig(Config.DISTANT_HORIZONS_COMPAT, true)) {
            return;
        }
        double ratio = recommendedEarthCurveRatio(shape);
        AIGCsEqualEarthGenerator.LOGGER.info(
                "[e2generator] Distant Horizons detected: for {} set 'Earth Curve Ratio' to about {} "
                        + "(half meridian H={} blocks). The seam/pole buffers are flat ocean, so LODs show sea "
                        + "instead of void; no DH classes are referenced by this mod.",
                level.dimension().location(), String.format(java.util.Locale.ROOT, "%.0f", ratio),
                String.format(java.util.Locale.ROOT, "%.0f", shape.halfMeridian()));
    }

    /**
     * DH 的 “Earth Curve Ratio” 与世界半子午线成反比。
     * <p>
     * 参考：TFC 默认 temperature scale 20 km 时推荐 160；半子午线翻倍则比例减半。
     * 因此 {@code ratio = referenceRatio * referenceHalfMeridian / H}。
     */
    public static double recommendedEarthCurveRatio(WorldShape shape) {
        double referenceHalfMeridian = 20_000.0;
        double referenceRatio = 160.0;
        try {
            referenceHalfMeridian = Config.DH_REFERENCE_HALF_MERIDIAN.get();
            referenceRatio = Config.DH_REFERENCE_CURVE_RATIO.get();
        } catch (IllegalStateException ignored) {
            // 配置尚未加载时使用默认值。
        }
        return referenceRatio * referenceHalfMeridian / Math.max(1.0, shape.halfMeridian());
    }

    private static boolean isLoaded(String modId) {
        try {
            ModList modList = ModList.get();
            return modList != null && modList.isLoaded(modId);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean boolConfig(net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value,
            boolean fallback) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return fallback;
        }
    }
}
