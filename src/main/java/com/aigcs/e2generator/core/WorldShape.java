package com.aigcs.e2generator.core;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * 世界的"球形"形状描述：一张有限的地图 + 可环绕的接缝 + 投影形状的大陆掩码。
 * <p>
 * 该对象<b>不可变</b>：创建后只读，所有查找表在构造时生成。可以安全地跨线程使用
 * （c2me 等并行区块生成模组）。
 * <p>
 * 坐标约定（与 Minecraft 一致）：z 向北为负，赤道位于 {@code equatorZ}。
 */
public final class WorldShape {

    public static final int FORMAT_VERSION = 1;

    private final BoundaryMode boundaryMode;
    private final EqualEarthProjection projection;
    /** 接缝海洋缓冲带宽度（方块）。 */
    private final double seamBuffer;
    /** 极线海洋缓冲带宽度（方块）。 */
    private final double polarBuffer;
    /** 陆地边缘的软过渡宽度（方块）。 */
    private final double softEdgeWidth;
    /** 结构/特征距离边界的最小余量（方块）。 */
    private final double structureMargin;
    /** 越过接缝多少格后触发包裹（避免在边缘反复触发）。 */
    private final double meridianGrace;
    /** 地图外海洋的海床高度。 */
    private final int oceanFloorY;
    /** 地图外的兜底海洋群系。 */
    private final ResourceLocation fallbackBiome;

    public WorldShape(BoundaryMode boundaryMode, EqualEarthProjection projection,
            double seamBuffer, double polarBuffer, double softEdgeWidth,
            double structureMargin, double meridianGrace,
            int oceanFloorY, ResourceLocation fallbackBiome) {
        this.boundaryMode = boundaryMode == null ? BoundaryMode.OUTLINE : boundaryMode;
        this.projection = projection;
        this.seamBuffer = Math.max(0.0, seamBuffer);
        this.polarBuffer = Math.max(0.0, polarBuffer);
        this.softEdgeWidth = Math.max(1.0, softEdgeWidth);
        this.structureMargin = Math.max(0.0, structureMargin);
        this.meridianGrace = Math.max(0.0, meridianGrace);
        this.oceanFloorY = oceanFloorY;
        this.fallbackBiome = fallbackBiome == null ? Biomes.DEEP_OCEAN.location() : fallbackBiome;
    }

    // ------------------------------------------------------------------
    // 基本属性
    // ------------------------------------------------------------------

    public BoundaryMode boundaryMode() {
        return this.boundaryMode;
    }

    public EqualEarthProjection projection() {
        return this.projection;
    }

    public double halfMeridian() {
        return this.projection.halfMeridian();
    }

    public double equatorZ() {
        return this.projection.equatorZ();
    }

    public double seamBuffer() {
        return this.seamBuffer;
    }

    public double polarBuffer() {
        return this.polarBuffer;
    }

    public double softEdgeWidth() {
        return this.softEdgeWidth;
    }

    public double structureMargin() {
        return this.structureMargin;
    }

    public double meridianGrace() {
        return this.meridianGrace;
    }

    public int oceanFloorY() {
        return this.oceanFloorY;
    }

    public ResourceLocation fallbackBiome() {
        return this.fallbackBiome;
    }

    public ResourceKey<Biome> fallbackBiomeKey() {
        return ResourceKey.create(Registries.BIOME, this.fallbackBiome);
    }

    /** 可玩区域的半高（已扣除极线缓冲）。 */
    public double playableHalfHeight() {
        return Math.max(0.0, this.projection.halfMeridian() - this.polarBuffer);
    }

    // ------------------------------------------------------------------
    // 几何
    // ------------------------------------------------------------------

    /**
     * 给定 z 处接缝的 x 坐标。
     * <ul>
     * <li>矩形模式：恒为赤道半宽；</li>
     * <li>投影轮廓模式：随纬度收缩的 Equal Earth 半宽。</li>
     * </ul>
     */
    public double seamX(double z) {
        return this.boundaryMode == BoundaryMode.RECTANGLE
                ? this.projection.equatorHalfWidth()
                : this.projection.halfWidthAtZ(z);
    }

    /**
     * 到"可生成陆地"边界的有符号距离（方块）。负值表示位于缓冲带或地图外。
     */
    public double landRoom(double x, double z) {
        double edge = seamX(z) - Math.abs(x) - this.seamBuffer;
        double pole = playableHalfHeight() - Math.abs(z - this.projection.equatorZ());
        return Math.min(edge, pole);
    }

    /**
     * 掩码：0 = 纯海洋，1 = 完全陆地，中间值 = 软过渡。
     */
    public double mask(double x, double z) {
        double room = landRoom(x, z);
        if (room <= 0.0) {
            return 0.0;
        }
        if (room >= this.softEdgeWidth) {
            return 1.0;
        }
        double t = room / this.softEdgeWidth;
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * 地图外的目标密度：海床以下为固体，海床以上为流体。
     * 与 {@code finalDensity > 0 = 固体} 的原版约定一致。
     */
    public double oceanDensity(int y) {
        return y < this.oceanFloorY ? 1.0 : -1.0;
    }

    /**
     * 按掩码把原版密度混合到"平坦海洋"。
     */
    public double blendDensity(double mask, double vanillaDensity, int y) {
        if (mask >= 1.0) {
            return vanillaDensity;
        }
        double ocean = oceanDensity(y);
        if (mask <= 0.0) {
            return ocean;
        }
        return ocean + (vanillaDensity - ocean) * mask;
    }

    /** 区块内是否（部分）包含陆地。 */
    public boolean intersectsLand(ChunkPos pos) {
        int minX = pos.getMinBlockX();
        int minZ = pos.getMinBlockZ();
        for (int dx = 0; dx <= 16; dx += 8) {
            for (int dz = 0; dz <= 16; dz += 8) {
                if (landRoom(minX + dx, minZ + dz) > 0.0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 区块是否完全位于地图外（可以走"平坦海洋"捷径）。 */
    public boolean isFullyOutside(ChunkPos pos) {
        return !intersectsLand(pos);
    }

    /** 是否允许在该区块放置结构（保留 {@link #structureMargin()} 余量）。 */
    public boolean allowsStructures(ChunkPos pos) {
        return minLandRoom(pos) > this.structureMargin;
    }

    /** 是否允许在该区块生成特征（树、矿脉等）。 */
    public boolean allowsDecoration(ChunkPos pos) {
        return intersectsLand(pos);
    }

    private double minLandRoom(ChunkPos pos) {
        int minX = pos.getMinBlockX();
        int minZ = pos.getMinBlockZ();
        double min = Double.MAX_VALUE;
        for (int dx = 0; dx <= 16; dx += 8) {
            for (int dz = 0; dz <= 16; dz += 8) {
                min = Math.min(min, landRoom(minX + dx, minZ + dz));
            }
        }
        return min;
    }

    // ------------------------------------------------------------------
    // 经纬度
    // ------------------------------------------------------------------

    public double latitudeAt(double z) {
        return this.projection.latitudeAtZ(z);
    }

    public double zAtLatitude(double latitude) {
        return this.projection.zAtLatitude(latitude);
    }

    public double longitudeAt(double x, double z) {
        return this.projection.longitudeAt(x, z);
    }

    public double xAtLongitude(double longitude, double z) {
        return this.projection.xAtLongitude(longitude, z);
    }

    // ------------------------------------------------------------------
    // 持久化
    // ------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("format", FORMAT_VERSION);
        tag.putString("boundary", this.boundaryMode.name());
        tag.putDouble("halfMeridian", this.projection.halfMeridian());
        tag.putDouble("ratio", this.projection.widthToHeightRatio());
        tag.putDouble("equatorZ", this.projection.equatorZ());
        tag.putDouble("seamBuffer", this.seamBuffer);
        tag.putDouble("polarBuffer", this.polarBuffer);
        tag.putDouble("softEdge", this.softEdgeWidth);
        tag.putDouble("structureMargin", this.structureMargin);
        tag.putDouble("grace", this.meridianGrace);
        tag.putInt("oceanFloorY", this.oceanFloorY);
        tag.putString("fallbackBiome", this.fallbackBiome.toString());
        return tag;
    }

    @Nullable
    public static WorldShape load(CompoundTag tag) {
        try {
            BoundaryMode boundary = tag.contains("boundary")
                    ? BoundaryMode.valueOf(tag.getString("boundary"))
                    : BoundaryMode.OUTLINE;
            double halfMeridian = tag.getDouble("halfMeridian");
            double ratio = tag.contains("ratio") ? tag.getDouble("ratio") : 2.0;
            double equatorZ = tag.getDouble("equatorZ");
            EqualEarthProjection projection = new EqualEarthProjection(halfMeridian, ratio, equatorZ);
            ResourceLocation fallback = ResourceLocation.tryParse(tag.getString("fallbackBiome"));
            return new WorldShape(
                    boundary,
                    projection,
                    tag.getDouble("seamBuffer"),
                    tag.getDouble("polarBuffer"),
                    tag.getDouble("softEdge"),
                    tag.getDouble("structureMargin"),
                    tag.getDouble("grace"),
                    tag.getInt("oceanFloorY"),
                    fallback);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public String describe() {
        return String.format(java.util.Locale.ROOT,
                "%s | H=%.0f | ratio=%.2f | equatorZ=%.0f | equatorWidth=%.0f | seamBuffer=%.0f | polarBuffer=%.0f",
                this.boundaryMode, this.projection.halfMeridian(), this.projection.widthToHeightRatio(),
                this.projection.equatorZ(), this.projection.equatorHalfWidth() * 2.0,
                this.seamBuffer, this.polarBuffer);
    }

    @Override
    public String toString() {
        return "WorldShape[" + describe() + "]";
    }
}
