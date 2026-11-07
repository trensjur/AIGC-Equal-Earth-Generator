package com.aigcs.e2generator.api;

import java.util.OptionalDouble;

import javax.annotation.Nullable;

import com.aigcs.e2generator.client.ClientWorldShape;
import com.aigcs.e2generator.compat.OptimizationCompat;
import com.aigcs.e2generator.core.PlanetSizing;
import com.aigcs.e2generator.core.WorldShape;
import com.aigcs.e2generator.world.ShapeManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * 本模组对其它模组提供的公开查询接口。
 * <p>
 * 典型用法：
 *
 * <pre>{@code
 * // 1) 直接查星球半径
 * OptionalDouble radius = SphericalWorldApi.getPlanetRadiusChunks(level);
 *
 * // 2) 判断当前世界是不是球形世界
 * if (SphericalWorldApi.isSphericalWorld(level)) { ... }
 *
 * // 3) 拿到完整的世界形状（接缝位置、掩码、经纬度换算等）
 * WorldShape shape = SphericalWorldApi.getWorldShape(level);
 * }</pre>
 *
 * <b>服务端</b>：从世界生成器上挂载的不可变 {@link WorldShape} 读取，结果权威。
 * <b>客户端</b>：使用登录/换维度时服务端同步过来的形状；未安装本模组时为 {@code null}。
 * <p>
 * 所有方法都允许传入 {@code null}，此时返回空值而不是抛异常，方便被其它模组安全地调用。
 */
public final class SphericalWorldApi {

    private SphericalWorldApi() {
    }

    /**
     * 当前世界使用的世界形状；不是球形世界（或数据尚未同步）时返回 {@code null}。
     */
    @Nullable
    public static WorldShape getWorldShape(@Nullable LevelAccessor level) {
        if (level == null) {
            return null;
        }
        if (level instanceof ServerLevel serverLevel) {
            return ShapeManager.shapeOf(serverLevel);
        }
        // 客户端：形状来自服务端同步；dist 判断避免专用服务端加载客户端类。
        return FMLEnvironment.dist.isClient() ? ClientWorldShape.get() : null;
    }

    /** 当前世界是否启用了球形世界形状。 */
    public static boolean isSphericalWorld(@Nullable LevelAccessor level) {
        return getWorldShape(level) != null;
    }

    /**
     * 当前世界使用的星球半径，单位<b>区块</b>。
     * <p>
     * 返回值由半子午线 H 与宽高比按等面积关系反推，和创建世界时选择的半径一致。
     */
    public static OptionalDouble getPlanetRadiusChunks(@Nullable LevelAccessor level) {
        WorldShape shape = getWorldShape(level);
        return shape == null ? OptionalDouble.empty() : OptionalDouble.of(radiusChunksOf(shape));
    }

    /**
     * 当前世界使用的星球半径，单位<b>方块</b>。
     */
    public static OptionalDouble getPlanetRadiusBlocks(@Nullable LevelAccessor level) {
        WorldShape shape = getWorldShape(level);
        return shape == null ? OptionalDouble.empty() : OptionalDouble.of(radiusBlocksOf(shape));
    }

    /** 由世界形状反推星球半径（区块）。 */
    public static double radiusChunksOf(WorldShape shape) {
        return PlanetSizing.radiusChunksFor(shape.halfMeridian(), shape.projection().widthToHeightRatio());
    }

    /** 由世界形状反推星球半径（方块）。 */
    public static double radiusBlocksOf(WorldShape shape) {
        return PlanetSizing.radiusBlocksFor(shape.halfMeridian(), shape.projection().widthToHeightRatio());
    }

    /**
     * 当前世界的赤道周长（方块）。与星球半径一样，由存档中的形状反推。
     */
    public static OptionalDouble getPlanetCircumference(@Nullable LevelAccessor level) {
        WorldShape shape = getWorldShape(level);
        return shape == null ? OptionalDouble.empty()
                : OptionalDouble.of(2.0 * Math.PI * radiusBlocksOf(shape));
    }

    /**
     * 给 Distant Horizons 的推荐 “Earth Curve Ratio”。
     * <p>
     * 该值与世界半子午线成反比；默认参考点是 20 km → 160（与 TFC 的惯例一致）。
     * 本模组不引用任何 DH 类，这里只是把值暴露给整合包/配置界面使用。
     */
    public static OptionalDouble getRecommendedDistantHorizonsCurveRatio(@Nullable LevelAccessor level) {
        WorldShape shape = getWorldShape(level);
        return shape == null ? OptionalDouble.empty()
                : OptionalDouble.of(OptimizationCompat.recommendedEarthCurveRatio(shape));
    }
}
