package com.aigcs.e2generator.core;

/**
 * Equal Earth 投影（Šavrič, Patterson, Jenny, 2018）。
 * <p>
 * 公式来自 d3-geo 的 {@code equalEarth} 实现，与 TFC Eratosthenes 的投影层保持一致：
 * 
 * <pre>
 *   theta = asin(M * sin(lat))
 *   x     = lon * cos(theta) / (M * (A1 + 3A2*theta^2 + theta^6*(7A3 + 9A4*theta^2)))
 *   y     = theta * (A1 + A2*theta^2 + theta^6*(A3 + A4*theta^2))
 * </pre>
 * 
 * 其中 {@code M = sqrt(3)/2}。
 * <p>
 * 性能：反解（z → 纬度）需要牛顿迭代，逐方块调用代价太高，因此在构造时预生成
 * <b>不可变</b>查找表（LUT）。LUT 构造后只读，可安全用于 c2me 等并行生成线程。
 */
public final class EqualEarthProjection implements Projection {

    private static final double A1 = 1.340264;
    private static final double A2 = -0.081106;
    private static final double A3 = 0.000893;
    private static final double A4 = 0.003796;
    private static final double M = Math.sqrt(3.0) / 2.0;

    private static final int NEWTON_ITERATIONS = 12;
    private static final double NEWTON_EPSILON = 1.0e-12;
    private static final double THETA_LIMIT = Math.PI / 3.0;

    /** y(90°)：由多项式自身求出，保证极线精确对应 ±90° 与 distance = H。 */
    private static final double MAGIC_NUMBER = thetaY(THETA_LIMIT);
    /** 使赤道处的相对宽度归一化为 1（赤道半宽 = H * ratio）。 */
    private static final double MAGIC_NUMBER_2 = M * A1;

    /** LUT 采样点数（z 轴方向）。 */
    private static final int LUT_SIZE = 8192;

    private final double halfMeridian;
    private final double widthToHeightRatio;
    private final double equatorZ;
    private final double equatorHalfWidth;

    /** 索引 0 = 北极，索引 LUT_SIZE - 1 = 南极。 */
    private final double[] thetaLut;
    private final double[] halfWidthLut;
    /** 投影后地图（Equal Earth 叶形）的面积。等面积投影：该面积 = 球面面积。 */
    private final double projectedArea;

    public EqualEarthProjection(double halfMeridian, double widthToHeightRatio, double equatorZ) {
        if (!(halfMeridian > 0.0) || !Double.isFinite(halfMeridian)) {
            throw new IllegalArgumentException("halfMeridian must be positive: " + halfMeridian);
        }
        if (!(widthToHeightRatio > 0.0) || !Double.isFinite(widthToHeightRatio)) {
            throw new IllegalArgumentException("widthToHeightRatio must be positive: " + widthToHeightRatio);
        }
        this.halfMeridian = halfMeridian;
        this.widthToHeightRatio = widthToHeightRatio;
        this.equatorZ = equatorZ;
        this.equatorHalfWidth = halfMeridian * widthToHeightRatio;

        this.thetaLut = new double[LUT_SIZE];
        this.halfWidthLut = new double[LUT_SIZE];
        for (int i = 0; i < LUT_SIZE; i++) {
            double t = (double) i / (double) (LUT_SIZE - 1);
            double z = equatorZ + (t * 2.0 - 1.0) * halfMeridian;
            // theta 随 z 变化平滑且良态；直接插值 theta 可以避免极点附近
            // 纬度对 z 导数发散导致的插值误差。
            double theta = thetaAtZExact(z);
            this.thetaLut[i] = theta;
            this.halfWidthLut[i] = halfWidthAtTheta(theta);
        }

        // 数值积分（梯形法）：A = ∫ 2·halfWidth(z) dz
        double dz = 2.0 * halfMeridian / (double) (LUT_SIZE - 1);
        double area = 0.0;
        for (int i = 0; i < LUT_SIZE - 1; i++) {
            area += (this.halfWidthLut[i] + this.halfWidthLut[i + 1]) * dz;
        }
        this.projectedArea = area;
    }

    @Override
    public double halfMeridian() {
        return this.halfMeridian;
    }

    @Override
    public double equatorZ() {
        return this.equatorZ;
    }

    @Override
    public double widthToHeightRatio() {
        return this.widthToHeightRatio;
    }

    @Override
    public double equatorHalfWidth() {
        return this.equatorHalfWidth;
    }

    /** 投影后地图（叶形）的面积；Equal Earth 为等面积投影，故等于球面面积。 */
    public double projectedArea() {
        return this.projectedArea;
    }

    @Override
    public double halfWidthAtZ(double z) {
        return lookup(this.halfWidthLut, z);
    }

    @Override
    public double latitudeAtZ(double z) {
        return latitudeForTheta(lookup(this.thetaLut, z));
    }

    @Override
    public double zAtLatitude(double latitude) {
        double clamped = Math.max(-90.0, Math.min(90.0, latitude));
        double theta = thetaForLatitude(clamped);
        double distance = this.halfMeridian * distanceFractionForTheta(theta);
        return clamped >= 0.0 ? this.equatorZ - distance : this.equatorZ + distance;
    }

    @Override
    public double longitudeAt(double x, double z) {
        double halfWidth = halfWidthAtZ(z);
        if (halfWidth <= 1.0e-6) {
            return 0.0;
        }
        return x / halfWidth * 180.0;
    }

    @Override
    public double xAtLongitude(double longitude, double z) {
        return longitude / 180.0 * halfWidthAtZ(z);
    }

    // ------------------------------------------------------------------
    // 内部数学
    // ------------------------------------------------------------------

    private double lookup(double[] lut, double z) {
        double t = (z - (this.equatorZ - this.halfMeridian)) / (2.0 * this.halfMeridian) * (LUT_SIZE - 1);
        if (t <= 0.0) {
            return lut[0];
        }
        if (t >= LUT_SIZE - 1) {
            return lut[LUT_SIZE - 1];
        }
        int index = (int) t;
        double fraction = t - index;
        double a = lut[index];
        double b = lut[index + 1];
        return a + (b - a) * fraction;
    }

    private double thetaAtZExact(double z) {
        double y = (this.equatorZ - z) / this.halfMeridian * MAGIC_NUMBER;
        return invertTheta(y);
    }

    private static double latitudeForTheta(double theta) {
        return Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, Math.sin(theta) / M))));
    }

    private static double thetaY(double theta) {
        double theta2 = theta * theta;
        double theta6 = theta2 * theta2 * theta2;
        return theta * (A1 + A2 * theta2 + theta6 * (A3 + A4 * theta2));
    }

    /**
     * 用牛顿迭代求解 {@code theta*(A1 + A2*theta^2 + theta^6*(A3 + A4*theta^2)) = y}。
     */
    private static double invertTheta(double y) {
        double target = Math.max(-MAGIC_NUMBER, Math.min(MAGIC_NUMBER, y));
        double theta = target;
        for (int i = 0; i < NEWTON_ITERATIONS; i++) {
            double theta2 = theta * theta;
            double theta6 = theta2 * theta2 * theta2;
            double f = theta * (A1 + A2 * theta2 + theta6 * (A3 + A4 * theta2)) - target;
            double derivative = A1 + 3.0 * A2 * theta2 + theta6 * (7.0 * A3 + 9.0 * A4 * theta2);
            if (Math.abs(derivative) < 1.0e-12) {
                break;
            }
            double delta = f / derivative;
            theta -= delta;
            if (Math.abs(delta) < NEWTON_EPSILON) {
                break;
            }
        }
        return Math.max(-THETA_LIMIT, Math.min(THETA_LIMIT, theta));
    }

    private static double thetaForLatitude(double latitudeDegrees) {
        double sin = Math.sin(Math.toRadians(Math.abs(latitudeDegrees)));
        return Math.asin(Math.max(-1.0, Math.min(1.0, M * sin)));
    }

    /**
     * 返回"距赤道距离 / halfMeridian"的比例（0 ~ 1）。
     * 极线（theta = 60°）处 y = MAGIC_NUMBER，因此比例恰好为 1。
     */
    private static double distanceFractionForTheta(double theta) {
        return Math.abs(thetaY(theta)) / MAGIC_NUMBER;
    }

    private double halfWidthAtTheta(double theta) {
        double theta2 = theta * theta;
        double theta6 = theta2 * theta2 * theta2;
        double denominator = M * (A1 + 3.0 * A2 * theta2 + theta6 * (7.0 * A3 + 9.0 * A4 * theta2));
        double relativeWidth = Math.cos(theta) / denominator;
        return relativeWidth * MAGIC_NUMBER_2 * this.halfMeridian * this.widthToHeightRatio;
    }

    @Override
    public String toString() {
        return "EqualEarthProjection[H=" + this.halfMeridian + ", ratio=" + this.widthToHeightRatio
                + ", equatorZ=" + this.equatorZ + ", poleHalfWidth=" + this.halfWidthLut[0] + "]";
    }
}
