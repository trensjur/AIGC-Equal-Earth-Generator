package com.aigcs.e2generator.api;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.ToDoubleFunction;

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
 *
 * // 4) 查询本初子午线、赤道、极线、地图范围等地理信息
 * SphericalWorldApi.getPlanetGeography(level).ifPresent(geography -> {
 *     double primeMeridianX = geography.primeMeridianX();     // 恒为 0
 *     double antimeridianX = geography.antimeridianX();       // 赤道处的接缝
 *     double equatorZ = geography.equatorZ();
 *     MapBounds bounds = geography.bounds();
 * });
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

    // ------------------------------------------------------------------
    // 地理信息快照
    // ------------------------------------------------------------------

    /**
     * 当前世界的完整地理信息快照：半径、本初子午线、反子午线、赤道、极线、
     * 地图范围、球面/地图面积以及经纬度换算。
     * <p>
     * 快照不可变，可以缓存后重复使用，避免每次查询都重新计算。
     */
    public static Optional<PlanetGeography> getPlanetGeography(@Nullable LevelAccessor level) {
        WorldShape shape = getWorldShape(level);
        return shape == null ? Optional.empty() : Optional.of(PlanetGeography.of(shape));
    }

    /** 本初子午线（经度 0°）的 x 坐标，恒为 0。 */
    public static OptionalDouble getPrimeMeridianX(@Nullable LevelAccessor level) {
        return number(level, PlanetGeography::primeMeridianX);
    }

    /** 赤道处反子午线（经度 ±180°、接缝）的 x 坐标（正数；西侧为相反数）。 */
    public static OptionalDouble getAntimeridianX(@Nullable LevelAccessor level) {
        return number(level, PlanetGeography::antimeridianX);
    }

    /** 指定 z 处反子午线的 x 坐标（正数；OUTLINE 模式下随纬度收缩）。 */
    public static OptionalDouble getAntimeridianXAt(@Nullable LevelAccessor level, double z) {
        return number(level, geography -> geography.antimeridianXAt(z));
    }

    /** 本初子午线在地图上的长度（北极线到南极线，{@code 2H}，方块），不是星球真实弧长。 */
    public static OptionalDouble getMeridianLengthOnMap(@Nullable LevelAccessor level) {
        return number(level, PlanetGeography::meridianLengthOnMap);
    }

    /** 本初子午线在星球上的真实弧长（北极到南极，{@code πR}，方块）。 */
    public static OptionalDouble getMeridianArcLength(@Nullable LevelAccessor level) {
        return number(level, PlanetGeography::meridianArcLength);
    }

    /** 赤道在星球上的真实周长（方块），与 {@link #getPlanetCircumference} 相同。 */
    public static OptionalDouble getEquatorLength(@Nullable LevelAccessor level) {
        return number(level, PlanetGeography::equatorLength);
    }

    /** 赤道所在的 z 坐标。 */
    public static OptionalDouble getEquatorZ(@Nullable LevelAccessor level) {
        return number(level, PlanetGeography::equatorZ);
    }

    /** 赤道处的地图宽度（方块）。 */
    public static OptionalDouble getEquatorWidth(@Nullable LevelAccessor level) {
        return number(level, PlanetGeography::equatorWidth);
    }

    /** 北极线（地图北边界）的 z 坐标。 */
    public static OptionalDouble getNorthPoleZ(@Nullable LevelAccessor level) {
        return number(level, PlanetGeography::northPoleZ);
    }

    /** 南极线（地图南边界）的 z 坐标。 */
    public static OptionalDouble getSouthPoleZ(@Nullable LevelAccessor level) {
        return number(level, PlanetGeography::southPoleZ);
    }

    /** 地图的轴对齐包围盒（x / z 范围）。 */
    public static Optional<MapBounds> getMapBounds(@Nullable LevelAccessor level) {
        WorldShape shape = getWorldShape(level);
        return shape == null ? Optional.empty() : Optional.of(PlanetGeography.of(shape).bounds());
    }

    /** 球面面积（方块²），由星球半径计算。 */
    public static OptionalDouble getPlanetSurfaceArea(@Nullable LevelAccessor level) {
        return number(level, PlanetGeography::sphereArea);
    }

    /** 投影后地图面积（方块²）；等面积投影下应与球面面积一致。 */
    public static OptionalDouble getMapArea(@Nullable LevelAccessor level) {
        return number(level, PlanetGeography::mapArea);
    }

    // ------------------------------------------------------------------
    // 经纬度与陆地掩码
    // ------------------------------------------------------------------

    /** 给定 z 的纬度（度，北纬为正）。 */
    public static OptionalDouble getLatitude(@Nullable LevelAccessor level, double z) {
        return number(level, geography -> geography.latitudeAt(z));
    }

    /** 给定平面坐标的经度（度，东经为正）。 */
    public static OptionalDouble getLongitude(@Nullable LevelAccessor level, double x, double z) {
        return number(level, geography -> geography.longitudeAt(x, z));
    }

    /** 给定纬度的 z 坐标（纬度会被夹取到 ±90°）。 */
    public static OptionalDouble getZForLatitude(@Nullable LevelAccessor level, double latitude) {
        return number(level, geography -> geography.zAtLatitude(latitude));
    }

    /** 给定经度与 z 的 x 坐标。 */
    public static OptionalDouble getXForLongitude(@Nullable LevelAccessor level, double longitude, double z) {
        return number(level, geography -> geography.xAtLongitude(longitude, z));
    }

    /** 方块坐标 → 地理信息快照（经纬度、是否在地图内、陆地掩码）。 */
    public static Optional<GeographicPosition> getGeographicPosition(@Nullable LevelAccessor level,
            double x, double z) {
        WorldShape shape = getWorldShape(level);
        return shape == null ? Optional.empty()
                : Optional.of(PlanetGeography.of(shape).geographicPosition(x, z));
    }

    /** 坐标是否位于地图轮廓内（不判断缓冲带与陆地）。非球形世界返回 {@code false}。 */
    public static boolean isInsideMap(@Nullable LevelAccessor level, double x, double z) {
        WorldShape shape = getWorldShape(level);
        return shape != null && PlanetGeography.of(shape).isInsideMap(x, z);
    }

    /** 陆地掩码：0 = 纯海洋/地图外，1 = 完全陆地，中间值为软过渡。 */
    public static OptionalDouble getLandMask(@Nullable LevelAccessor level, double x, double z) {
        return number(level, geography -> geography.landMaskAt(x, z));
    }

    /** 两个经纬度之间的球面大圆距离（方块）。 */
    public static OptionalDouble getSurfaceDistance(@Nullable LevelAccessor level,
            double latitude1, double longitude1, double latitude2, double longitude2) {
        return number(level, geography -> geography.surfaceDistance(
                latitude1, longitude1, latitude2, longitude2));
    }

    private static OptionalDouble number(@Nullable LevelAccessor level, ToDoubleFunction<PlanetGeography> getter) {
        WorldShape shape = getWorldShape(level);
        return shape == null ? OptionalDouble.empty()
                : OptionalDouble.of(getter.applyAsDouble(PlanetGeography.of(shape)));
    }
}
