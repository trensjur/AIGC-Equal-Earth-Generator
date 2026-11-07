package com.aigcs.e2generator.core;

/**
 * 地图投影接口。
 * <p>
 * 约定：
 * <ul>
 * <li>x 轴 = 经度方向，周期为 {@link #equatorHalfWidth()} * 2（可环绕）。</li>
 * <li>z 轴 = 纬度方向，赤道在 {@link #equatorZ()}，向北为负。</li>
 * <li>投影只用于"哪里能生成陆地"和"经纬度显示"，<b>不会</b>用于重映射噪声采样坐标。</li>
 * </ul>
 */
public interface Projection {

    /** 赤道到极线的方块距离（半条子午线）。 */
    double halfMeridian();

    /** 赤道所在的 z 坐标。 */
    double equatorZ();

    /** 赤道半宽相对半子午线的比例（完整世界 = 2，半世界 = 1）。 */
    double widthToHeightRatio();

    /** 赤道处的半宽（方块）。 */
    double equatorHalfWidth();

    /** 给定 z 处的地图半宽（方块），即该纬度上接缝的 x 坐标。 */
    double halfWidthAtZ(double z);

    /** 给定 z 处的纬度（度，北纬为正）。 */
    double latitudeAtZ(double z);

    /** 给定纬度对应的 z 坐标（北极 z 更小）。 */
    double zAtLatitude(double latitude);

    /** 给定平面坐标对应的经度（度，东经为正）。 */
    double longitudeAt(double x, double z);

    /** 给定经度与 z 对应的 x 坐标。 */
    double xAtLongitude(double longitude, double z);
}
