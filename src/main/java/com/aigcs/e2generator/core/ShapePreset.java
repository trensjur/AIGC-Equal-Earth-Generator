package com.aigcs.e2generator.core;

import java.util.Locale;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.biome.Biomes;

/**
 * 数据包中的世界形状预设（Phase 6）。
 * <p>
 * 文件位置：{@code data/<namespace>/e2generator/shape/<path>.json}。
 * 预设只在<b>新建世界</b>时使用；已有存档始终以 {@code e2generator_world_shape.dat} 为准。
 * <p>
 * 尺寸可以用两种方式指定（二选一，优先 {@code planetRadiusChunks}）：
 * <ul>
 * <li>{@code planetRadiusChunks}：与创建世界控制器相同的“星球半径（区块）”，
 * 由等面积关系反推出半子午线 H；</li>
 * <li>{@code halfMeridian}：直接给出赤道到极线的方块距离。</li>
 * </ul>
 */
public record ShapePreset(
        ResourceLocation id,
        String description,
        BoundaryMode boundaryMode,
        double halfMeridian,
        double widthToHeightRatio,
        double equatorZ,
        double seamBuffer,
        double polarBuffer,
        double softEdgeWidth,
        double structureMargin,
        double meridianGrace,
        int oceanFloorY,
        ResourceLocation fallbackBiome) {

    public static final double MIN_HALF_MERIDIAN = 512.0;
    public static final double MAX_HALF_MERIDIAN = 2_000_000.0;
    public static final double MIN_RATIO = 0.25;
    public static final double MAX_RATIO = 8.0;

    public ShapePreset {
        if (id == null) {
            throw new IllegalArgumentException("preset id must not be null");
        }
        if (!(halfMeridian > 0.0) || !Double.isFinite(halfMeridian)) {
            throw new IllegalArgumentException("halfMeridian must be positive: " + halfMeridian);
        }
        description = description == null ? "" : description;
        boundaryMode = boundaryMode == null ? BoundaryMode.OUTLINE : boundaryMode;
        halfMeridian = clamp(halfMeridian, MIN_HALF_MERIDIAN, MAX_HALF_MERIDIAN);
        widthToHeightRatio = clamp(widthToHeightRatio, MIN_RATIO, MAX_RATIO);
        equatorZ = Double.isFinite(equatorZ) ? equatorZ : 0.0;
        seamBuffer = Math.max(0.0, seamBuffer);
        polarBuffer = Math.max(0.0, polarBuffer);
        softEdgeWidth = Math.max(1.0, softEdgeWidth);
        structureMargin = Math.max(0.0, structureMargin);
        meridianGrace = Math.max(0.0, meridianGrace);
        oceanFloorY = Math.max(-64, Math.min(320, oceanFloorY));
        fallbackBiome = fallbackBiome == null ? Biomes.DEEP_OCEAN.location() : fallbackBiome;
    }

    /**
     * 解析预设 JSON。语义错误（例如未知的 {@code boundaryMode}）会抛出
     * {@link JsonSyntaxException}，由资源重载器记录并跳过该文件。
     */
    public static ShapePreset fromJson(ResourceLocation id, JsonObject json) {
        String description = GsonHelper.getAsString(json, "description", "");
        BoundaryMode boundary = parseBoundary(GsonHelper.getAsString(json, "boundaryMode", "OUTLINE"));
        double ratio = clamp(GsonHelper.getAsDouble(json, "widthToHeightRatio", 2.0), MIN_RATIO, MAX_RATIO);

        double halfMeridian;
        if (json.has("planetRadiusChunks")) {
            double radiusChunks = GsonHelper.getAsDouble(json, "planetRadiusChunks");
            if (!Double.isFinite(radiusChunks) || radiusChunks <= 0.0) {
                throw new JsonSyntaxException("planetRadiusChunks must be positive: " + radiusChunks);
            }
            halfMeridian = PlanetSizing.halfMeridianFor(radiusChunks, ratio);
        } else {
            halfMeridian = GsonHelper.getAsDouble(json, "halfMeridian", 8000.0);
        }

        ResourceLocation fallback = ResourceLocation.tryParse(
                GsonHelper.getAsString(json, "fallbackBiome", "minecraft:deep_ocean"));
        if (fallback == null) {
            throw new JsonSyntaxException("Invalid fallbackBiome in shape preset " + id);
        }

        return new ShapePreset(
                id,
                description,
                boundary,
                halfMeridian,
                ratio,
                GsonHelper.getAsDouble(json, "equatorZ", 0.0),
                GsonHelper.getAsDouble(json, "seamBuffer", 384.0),
                GsonHelper.getAsDouble(json, "polarBuffer", 192.0),
                GsonHelper.getAsDouble(json, "softEdgeWidth", 96.0),
                GsonHelper.getAsDouble(json, "structureMargin", 256.0),
                GsonHelper.getAsDouble(json, "meridianGrace", 32.0),
                GsonHelper.getAsInt(json, "oceanFloorY", 24),
                fallback);
    }

    /** 由预设创建不可变的世界形状。 */
    public WorldShape toShape() {
        EqualEarthProjection projection = new EqualEarthProjection(this.halfMeridian, this.widthToHeightRatio,
                this.equatorZ);
        return new WorldShape(
                this.boundaryMode,
                projection,
                this.seamBuffer,
                this.polarBuffer,
                this.softEdgeWidth,
                this.structureMargin,
                this.meridianGrace,
                this.oceanFloorY,
                this.fallbackBiome);
    }

    public String describe() {
        return String.format(Locale.ROOT,
                "%s | H=%.0f | ratio=%.2f | equatorWidth=%.0f | seamBuffer=%.0f | polarBuffer=%.0f",
                this.boundaryMode, this.halfMeridian, this.widthToHeightRatio,
                this.halfMeridian * this.widthToHeightRatio * 2.0, this.seamBuffer, this.polarBuffer);
    }

    private static BoundaryMode parseBoundary(String value) {
        try {
            return BoundaryMode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new JsonSyntaxException("Unknown boundaryMode: " + value, exception);
        }
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            throw new JsonSyntaxException("Expected a finite number but got " + value);
        }
        return Math.max(min, Math.min(max, value));
    }
}
