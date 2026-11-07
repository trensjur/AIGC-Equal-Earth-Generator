package com.aigcs.e2generator.core;

/**
 * 世界边界模式。
 */
public enum BoundaryMode {
    /**
     * 矩形模式：可玩区域是 x ∈ [-W/2, W/2]、z ∈ [-H, H] 的矩形。
     * 接缝是直线，实现最简单，适合第一版与兼容性优先的整合包。
     */
    RECTANGLE,

    /**
     * 投影轮廓模式（默认）：可玩区域由 Equal Earth 的叶形轮廓决定，
     * 接缝是曲线 x = ±halfWidth(z)。视觉上就是一张世界地图。
     */
    OUTLINE
}
