package com.aigcs.e2generator.api;

import java.util.Locale;

/**
 * 某个方块坐标的地理信息快照：经纬度、是否在地图内、陆地掩码。
 * <p>
 * 经纬度约定与本模组其它部分一致：北纬为正、东经为正，单位为度。
 * 地图外（掩码为 0 的海洋或缓冲区）的经度可能超出 ±180°，此时应按周期性取模理解。
 */
public record GeographicPosition(
        double x,
        double z,
        double latitude,
        double longitude,
        boolean insideMap,
        double landMask) {

    public boolean isNorthernHemisphere() {
        return this.latitude >= 0.0;
    }

    public boolean isEasternHemisphere() {
        return this.longitude >= 0.0;
    }

    /** {@code N} 或 {@code S}。 */
    public String latitudeHemisphere() {
        return this.latitude >= 0.0 ? "N" : "S";
    }

    /** {@code E} 或 {@code W}。 */
    public String longitudeHemisphere() {
        return this.longitude >= 0.0 ? "E" : "W";
    }

    /**
     * 格式化为 {@code "12.345°N 67.890°E"} 形式。
     *
     * @param decimals 小数位数（负数按 0 处理）
     */
    public String format(int decimals) {
        int digits = Math.max(0, decimals);
        String pattern = "%." + digits + "f";
        return String.format(Locale.ROOT, pattern + "°%s " + pattern + "°%s",
                Math.abs(this.latitude), latitudeHemisphere(),
                Math.abs(this.longitude), longitudeHemisphere());
    }

    @Override
    public String toString() {
        return "GeographicPosition[" + format(3) + ", x=" + this.x + ", z=" + this.z
                + ", insideMap=" + this.insideMap + ", landMask=" + this.landMask + "]";
    }
}
