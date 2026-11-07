package com.aigcs.e2generator.core;

/**
 * 把"星球半径"换算成实际生成的世界尺寸。
 * <p>
 * 思路（与设计文档一致）：
 * <ol>
 * <li>星球半径以<b>区块</b>为单位，先换算成方块半径 {@code R}；</li>
 * <li>球面面积 {@code 4πR²}；</li>
 * <li>Equal Earth 是<b>等面积</b>投影，因此要求投影后的地图面积等于球面面积；</li>
 * <li>地图面积与 {@code H²} 成正比（{@code A = A_unit · H²}），由此解出半子午线 {@code H}；</li>
 * <li>世界宽度 = {@code 2·H·ratio}，世界高度 = {@code 2·H}。</li>
 * </ol>
 * 这样"星球大小"与"地图大小"在面积上是严格对应的，而不是随便乘一个系数。
 */
public final class PlanetSizing {

    public static final double BLOCKS_PER_CHUNK = 16.0;
    /** 控制器预览使用的宽高比（与默认配置一致：完整世界 2:1）。 */
    public static final double DEFAULT_WIDTH_TO_HEIGHT_RATIO = 2.0;

    public static final int MIN_RADIUS_CHUNKS = 64;
    public static final int MAX_RADIUS_CHUNKS = 64000;
    public static final int DEFAULT_RADIUS_CHUNKS = 500;

    /** 单位投影（H = 1）的归一化面积缓存，避免拖动滑块时反复构建 LUT。 */
    private static volatile double cachedRatio = Double.NaN;
    private static volatile double cachedAreaPerUnitH2 = Double.NaN;

    private PlanetSizing() {
    }

    public record Result(
            double radiusChunks,
            double radiusBlocks,
            double halfMeridian,
            double worldWidthBlocks,
            double worldHeightBlocks,
            double mapArea,
            double sphereArea,
            double circumference) {

        public double worldWidthChunks() {
            return this.worldWidthBlocks / BLOCKS_PER_CHUNK;
        }

        public double worldHeightChunks() {
            return this.worldHeightBlocks / BLOCKS_PER_CHUNK;
        }
    }

    /**
     * 计算世界尺寸。
     *
     * @param radiusChunks       星球半径（区块）
     * @param widthToHeightRatio 赤道半宽 / H（2 = 完整世界，1 = 半世界）
     */
    public static Result compute(double radiusChunks, double widthToHeightRatio) {
        double radius = clampRadius(radiusChunks);
        double radiusBlocks = radius * BLOCKS_PER_CHUNK;
        double halfMeridian = halfMeridianFor(radius, widthToHeightRatio);
        EqualEarthProjection projection = new EqualEarthProjection(halfMeridian, widthToHeightRatio, 0.0);
        double sphereArea = 4.0 * Math.PI * radiusBlocks * radiusBlocks;
        return new Result(
                radius,
                radiusBlocks,
                halfMeridian,
                projection.equatorHalfWidth() * 2.0,
                halfMeridian * 2.0,
                projection.projectedArea(),
                sphereArea,
                2.0 * Math.PI * radiusBlocks);
    }

    /**
     * 由星球半径反推半子午线长度 H。
     */
    public static double halfMeridianFor(double radiusChunks, double widthToHeightRatio) {
        double radius = clampRadius(radiusChunks);
        double radiusBlocks = radius * BLOCKS_PER_CHUNK;
        double areaPerUnitH2 = areaPerUnitH2(widthToHeightRatio);
        // A_unit * H^2 = 4πR^2
        return radiusBlocks * Math.sqrt(4.0 * Math.PI / areaPerUnitH2);
    }

    /** 单位投影（H = 1）的地图面积，即 "每单位 H² 对应多少面积"。 */
    public static double areaPerUnitH2(double widthToHeightRatio) {
        double ratio = Math.max(0.05, widthToHeightRatio);
        if (Double.isNaN(cachedAreaPerUnitH2) || Math.abs(ratio - cachedRatio) > 1.0e-9) {
            EqualEarthProjection unit = new EqualEarthProjection(1.0, ratio, 0.0);
            cachedAreaPerUnitH2 = unit.projectedArea();
            cachedRatio = ratio;
        }
        return cachedAreaPerUnitH2;
    }

    /**
     * 由半子午线 H 反推星球半径（区块）。
     * <p>
     * 与 {@link #halfMeridianFor(double, double)} 互逆，供公开 API 查询
     * "当前世界使用的星球半径"。不会做 64 ~ 6400 的范围夹取，
     * 以便如实反映通过配置文件直接设定 {@code halfMeridian} 的世界。
     */
    public static double radiusChunksFor(double halfMeridian, double widthToHeightRatio) {
        double areaPerUnitH2 = areaPerUnitH2(widthToHeightRatio);
        double radiusBlocks = halfMeridian * Math.sqrt(areaPerUnitH2 / (4.0 * Math.PI));
        return radiusBlocks / BLOCKS_PER_CHUNK;
    }

    /** 由半子午线 H 反推星球半径（方块）。 */
    public static double radiusBlocksFor(double halfMeridian, double widthToHeightRatio) {
        return radiusChunksFor(halfMeridian, widthToHeightRatio) * BLOCKS_PER_CHUNK;
    }

    public static double clampRadius(double radiusChunks) {
        if (!Double.isFinite(radiusChunks)) {
            return DEFAULT_RADIUS_CHUNKS;
        }
        return Math.max(MIN_RADIUS_CHUNKS, Math.min(MAX_RADIUS_CHUNKS, radiusChunks));
    }
}
