package com.aigcs.e2generator.api;

/**
 * 球形世界地图的轴对齐包围盒（方块坐标）。
 * <p>
 * 地图在 x 方向以本初子午线（x = 0）为中心，在 z 方向以赤道（{@link #centerZ()}）为中心。
 * 注意 {@code OUTLINE} 边界模式下的实际可玩区域随纬度收缩：极点附近只有 x = 0
 * 一条线，{@link #contains(double, double)} 只表示"是否在包围盒内"。
 * 要判断精确的地图范围，请使用 {@link PlanetGeography#isInsideMap(double, double)}。
 */
public record MapBounds(double minX, double maxX, double minZ, double maxZ) {

    public double width() {
        return this.maxX - this.minX;
    }

    public double height() {
        return this.maxZ - this.minZ;
    }

    public double centerX() {
        return (this.minX + this.maxX) * 0.5;
    }

    public double centerZ() {
        return (this.minZ + this.maxZ) * 0.5;
    }

    /** 是否位于包围盒内部（不判断投影轮廓与缓冲带）。 */
    public boolean contains(double x, double z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
    }

    @Override
    public String toString() {
        return "MapBounds[x=" + this.minX + ".." + this.maxX
                + ", z=" + this.minZ + ".." + this.maxZ + "]";
    }
}
