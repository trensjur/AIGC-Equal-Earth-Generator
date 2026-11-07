package com.aigcs.e2generator.api;

import java.util.Locale;
import java.util.Objects;

import com.aigcs.e2generator.core.BoundaryMode;
import com.aigcs.e2generator.core.WorldShape;

/**
 * 当前世界的"星球地理信息"只读快照。
 * <p>
 * 除了星球半径，还包含本初子午线、反子午线、赤道、极线、地图范围以及经纬度换算。
 * 记录创建后不可变，可以安全缓存或跨线程使用。
 *
 * <pre>{@code
 * PlanetGeography geography = SphericalWorldApi.getPlanetGeography(level).orElse(null);
 * if (geography != null) {
 *     double x = geography.xAtLongitude(116.4, geography.zAtLatitude(39.9)); // 北京
 *     double lon = geography.longitudeAt(player.getX(), player.getZ());
 * }
 * }</pre>
 *
 * <h2>坐标约定</h2>
 * <ul>
 * <li><b>本初子午线</b>（经度 0°）恒为 {@code x = 0}；</li>
 * <li><b>反子午线</b>（经度 ±180°）位于 {@code |x| = shape.seamX(z)}，也就是接缝，在
 * {@code OUTLINE} 模式下随纬度收缩，在 {@code RECTANGLE} 模式下为常数；</li>
 * <li>跨过反子午线会环绕回本初子午线，因此经度是周期性的；</li>
 * <li>z 轴向南为负、向北为正与 Minecraft 一致，赤道位于 {@link #equatorZ()}。</li>
 * </ul>
 *
 * <b>地图长度 ≠ 星球长度</b>：Equal Earth 是等面积投影，地图上的子午线长度
 * {@link #meridianLengthOnMap()}（{@code 2H}）不等于星球上的真实弧长
 * {@link #meridianArcLength()}（{@code πR}）。需要真实距离时请使用
 * {@link #surfaceDistance(double, double, double, double)}。
 */
public record PlanetGeography(
        WorldShape shape,
        double radiusChunks,
        double radiusBlocks,
        double circumference,
        double halfMeridian,
        double widthToHeightRatio,
        double equatorZ,
        double equatorWidth,
        double northPoleZ,
        double southPoleZ,
        double sphereArea,
        double mapArea) {

    /** 本初子午线（经度 0°）在地图上的 x 坐标。 */
    public static final double PRIME_MERIDIAN_X = 0.0;

    public static final double MIN_LATITUDE = -90.0;
    public static final double MAX_LATITUDE = 90.0;
    public static final double MIN_LONGITUDE = -180.0;
    public static final double MAX_LONGITUDE = 180.0;

    /** 由世界形状构建快照；{@code shape} 不可为 {@code null}。 */
    public static PlanetGeography of(WorldShape shape) {
        Objects.requireNonNull(shape, "shape");
        double radiusChunks = SphericalWorldApi.radiusChunksOf(shape);
        double radiusBlocks = SphericalWorldApi.radiusBlocksOf(shape);
        double halfMeridian = shape.halfMeridian();
        return new PlanetGeography(
                shape,
                radiusChunks,
                radiusBlocks,
                2.0 * Math.PI * radiusBlocks,
                halfMeridian,
                shape.projection().widthToHeightRatio(),
                shape.equatorZ(),
                shape.projection().equatorHalfWidth() * 2.0,
                shape.equatorZ() - halfMeridian,
                shape.equatorZ() + halfMeridian,
                4.0 * Math.PI * radiusBlocks * radiusBlocks,
                shape.projection().projectedArea());
    }

    /** 边界模式（{@code OUTLINE} 叶形轮廓 / {@code RECTANGLE} 矩形）。 */
    public BoundaryMode boundaryMode() {
        return this.shape.boundaryMode();
    }

    // ------------------------------------------------------------------
    // 本初子午线 / 反子午线
    // ------------------------------------------------------------------

    /** 本初子午线（经度 0°）的 x 坐标，恒为 {@value #PRIME_MERIDIAN_X}。 */
    public double primeMeridianX() {
        return PRIME_MERIDIAN_X;
    }

    /** 赤道处反子午线（经度 ±180°、接缝）的 x 坐标（正数；西侧为相反数）。 */
    public double antimeridianX() {
        return antimeridianXAt(this.equatorZ);
    }

    /** 指定 z 处反子午线的 x 坐标（正数；西侧为相反数）。 */
    public double antimeridianXAt(double z) {
        return this.shape.seamX(z);
    }

    /**
     * 本初子午线在地图上的长度（北极线到南极线，方块），即 {@code 2H}。
     * <p>
     * 这是投影后的长度，不是星球上的真实弧长。
     */
    public double meridianLengthOnMap() {
        return 2.0 * this.halfMeridian;
    }

    /** 本初子午线在星球上的真实弧长（北极到南极，方块），即 {@code πR}。 */
    public double meridianArcLength() {
        return Math.PI * this.radiusBlocks;
    }

    // ------------------------------------------------------------------
    // 赤道 / 极线 / 地图范围
    // ------------------------------------------------------------------

    /** 赤道处的地图半宽（方块）。 */
    public double equatorHalfWidth() {
        return this.equatorWidth * 0.5;
    }

    /** 赤道在星球上的真实周长（方块），与 {@link #circumference()} 相同。 */
    public double equatorLength() {
        return this.circumference;
    }

    public double mapMinX() {
        return -equatorHalfWidth();
    }

    public double mapMaxX() {
        return equatorHalfWidth();
    }

    public double mapMinZ() {
        return this.northPoleZ;
    }

    public double mapMaxZ() {
        return this.southPoleZ;
    }

    /** 地图包围盒。 */
    public MapBounds bounds() {
        return new MapBounds(mapMinX(), mapMaxX(), mapMinZ(), mapMaxZ());
    }

    /**
     * 是否位于地图轮廓内（不含接缝/极线缓冲带，也不判断陆地）。
     * <p>
     * {@code OUTLINE} 模式下极点附近只有 x = 0 一条线；需要"是否能生成陆地"
     * 请改用 {@link #landMaskAt(double, double)} 或 {@code WorldShape#landRoom}。
     */
    public boolean isInsideMap(double x, double z) {
        return Math.abs(z - this.equatorZ) <= this.halfMeridian
                && Math.abs(x) <= this.shape.seamX(z);
    }

    // ------------------------------------------------------------------
    // 经纬度
    // ------------------------------------------------------------------

    /** 给定 z 的纬度（度，北纬为正）。 */
    public double latitudeAt(double z) {
        return this.shape.latitudeAt(z);
    }

    /** 给定平面坐标的经度（度，东经为正）。 */
    public double longitudeAt(double x, double z) {
        return this.shape.longitudeAt(x, z);
    }

    /** 给定纬度的 z 坐标；纬度会被夹取到 ±90°。 */
    public double zAtLatitude(double latitude) {
        return this.shape.zAtLatitude(latitude);
    }

    /** 给定经度与 z 的 x 坐标；经度不做夹取（可按 360° 周期理解）。 */
    public double xAtLongitude(double longitude, double z) {
        return this.shape.xAtLongitude(longitude, z);
    }

    /** 经纬度 → x/z；返回的 {@code x} 在地图外时仍按投影公式计算。 */
    public double xAt(double latitude, double longitude) {
        return xAtLongitude(longitude, zAtLatitude(latitude));
    }

    /** 经纬度 → z；与 {@link #zAtLatitude(double)} 相同。 */
    public double zAt(double latitude) {
        return zAtLatitude(latitude);
    }

    /** 方块坐标 → 地理信息快照。 */
    public GeographicPosition geographicPosition(double x, double z) {
        return new GeographicPosition(
                x, z,
                latitudeAt(z),
                longitudeAt(x, z),
                isInsideMap(x, z),
                landMaskAt(x, z));
    }

    // ------------------------------------------------------------------
    // 陆地掩码与距离
    // ------------------------------------------------------------------

    /**
     * 陆地掩码：0 = 纯海洋/地图外，1 = 完全陆地，中间值为软过渡。
     */
    public double landMaskAt(double x, double z) {
        return this.shape.mask(x, z);
    }

    /**
     * 两个经纬度之间的球面大圆距离（方块）。
     * <p>
     * 使用 {@link #radiusBlocks()} 作为球半径，适合计算传送距离、航线长度等。
     */
    public double surfaceDistance(double latitude1, double longitude1,
            double latitude2, double longitude2) {
        return surfaceDistanceBlocks(this.radiusBlocks, latitude1, longitude1, latitude2, longitude2);
    }

    /** 独立于世界形状的 Haversine 大圆距离（方块）。 */
    public static double surfaceDistanceBlocks(double radiusBlocks,
            double latitude1, double longitude1, double latitude2, double longitude2) {
        double phi1 = Math.toRadians(latitude1);
        double phi2 = Math.toRadians(latitude2);
        double deltaPhi = Math.toRadians(latitude2 - latitude1);
        double deltaLambda = Math.toRadians(longitude2 - longitude1);
        double sinPhi = Math.sin(deltaPhi * 0.5);
        double sinLambda = Math.sin(deltaLambda * 0.5);
        double a = sinPhi * sinPhi
                + Math.cos(phi1) * Math.cos(phi2) * sinLambda * sinLambda;
        double angle = 2.0 * Math.asin(Math.min(1.0, Math.sqrt(Math.max(0.0, a))));
        return radiusBlocks * angle;
    }

    public String describe() {
        return String.format(Locale.ROOT,
                "R=%.1f chunks (%.0f blocks) | meridian x=%.0f | antimeridian x=+/-%.0f | "
                        + "equatorZ=%.0f | poles z=%.0f/%.0f | map %.0fx%.0f | mask=%s",
                this.radiusChunks, this.radiusBlocks,
                primeMeridianX(), antimeridianX(),
                this.equatorZ, this.northPoleZ, this.southPoleZ,
                this.equatorWidth, meridianLengthOnMap(),
                this.boundaryMode());
    }

    @Override
    public String toString() {
        return "PlanetGeography[" + describe() + "]";
    }
}
