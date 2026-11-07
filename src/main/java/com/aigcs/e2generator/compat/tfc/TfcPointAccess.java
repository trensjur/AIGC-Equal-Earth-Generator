package com.aigcs.e2generator.compat.tfc;

/**
 * 由 {@code RegionPointMixin} 注入到 TFC {@code Region.Point} 上的访问器。
 * <p>
 * 这样 {@code RegionGeneratorMixin} 不需要在编译期引用 TFC 的类，也能读取网格坐标。
 */
public interface TfcPointAccess {

    int spherical$gridX();

    int spherical$gridZ();
}
