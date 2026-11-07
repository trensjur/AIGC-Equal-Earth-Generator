package com.aigcs.e2generator;

import java.util.List;

import com.aigcs.e2generator.core.BoundaryMode;
import com.aigcs.e2generator.core.EqualEarthProjection;
import com.aigcs.e2generator.core.PlanetSettings;
import com.aigcs.e2generator.core.PlanetSizing;
import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 服务端配置。
 * <p>
 * 重要：配置只影响<b>新建世界</b>。已存在存档的形状以 {@code e2generator_world_shape.dat}
 * 为准，避免修改配置导致已生成区块错位。
 */
public class Config {

        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
                        .comment("Enable the spherical (Equal Earth) world shape.")
                        .define("enabled", true);

        public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS = BUILDER
                        .comment("Dimensions that use the spherical shape. Others behave as vanilla.")
                        .defineListAllowEmpty("dimensions", List.of(Level.OVERWORLD.location().toString()),
                                        () -> Level.OVERWORLD.location().toString(), Config::validateDimension);

        public static final ModConfigSpec.EnumValue<BoundaryMode> BOUNDARY_MODE = BUILDER
                        .comment("RECTANGLE = straight seams (simplest), OUTLINE = Equal Earth outline (default).")
                        .defineEnum("boundaryMode", BoundaryMode.OUTLINE);

        public static final ModConfigSpec.ConfigValue<String> SHAPE_PRESET = BUILDER
                        .comment("Shape preset used by new worlds, as a resource id (built-in: e2generator:earth,",
                                        "e2generator:half_world, e2generator:compact, e2generator:large, e2generator:rectangle;",
                                        "data packs can add data/<namespace>/e2generator/shape/<path>.json).",
                                        "Empty = use the individual settings below. Existing worlds always use their saved shape.")
                        .define("shapePreset", "", Config::validatePreset);

        public static final ModConfigSpec.IntValue HALF_MERIDIAN = BUILDER
                        .comment("Blocks from the equator to the pole line (H). Equator width = 2 * H * widthToHeightRatio.")
                        .defineInRange("halfMeridian", 8000, 512, 2_000_000);

        public static final ModConfigSpec.DoubleValue WIDTH_TO_HEIGHT_RATIO = BUILDER
                        .comment("Equator half width divided by H. 2 = full world (2:1), 1 = half world (1:1).")
                        .defineInRange("widthToHeightRatio", 2.0, 0.25, 8.0);

        public static final ModConfigSpec.DoubleValue EQUATOR_Z = BUILDER
                        .comment("Z coordinate of the equator. 0 puts the spawn point in the middle of the map.")
                        .defineInRange("equatorZ", 0.0, -1.0e7, 1.0e7);

        public static final ModConfigSpec.DoubleValue SEAM_BUFFER = BUILDER
                        .comment("Ocean band width next to the east/west seam, in blocks.")
                        .defineInRange("seamBuffer", 384.0, 0.0, 100_000.0);

        public static final ModConfigSpec.DoubleValue POLAR_BUFFER = BUILDER
                        .comment("Ocean/ice band width next to the north/south pole line, in blocks.")
                        .defineInRange("polarBuffer", 192.0, 0.0, 100_000.0);

        public static final ModConfigSpec.DoubleValue SOFT_EDGE_WIDTH = BUILDER
                        .comment("Width of the smooth land -> ocean transition, in blocks.")
                        .defineInRange("softEdgeWidth", 96.0, 1.0, 10_000.0);

        public static final ModConfigSpec.DoubleValue STRUCTURE_MARGIN = BUILDER
                        .comment("Extra margin between structures/features and the map border, in blocks.")
                        .defineInRange("structureMargin", 256.0, 0.0, 10_000.0);

        public static final ModConfigSpec.DoubleValue MERIDIAN_GRACE = BUILDER
                        .comment("How far beyond the seam an entity must move before it is wrapped.")
                        .defineInRange("meridianGrace", 32.0, 0.0, 4096.0);

        public static final ModConfigSpec.IntValue OCEAN_FLOOR_Y = BUILDER
                        .comment("Sea floor height of the ocean outside the map / inside the buffers.")
                        .defineInRange("oceanFloorY", 24, -64, 320);

        public static final ModConfigSpec.ConfigValue<String> FALLBACK_BIOME = BUILDER
                        .comment("Biome used outside the map (must be an ocean-like biome).")
                        .define("fallbackBiome", "minecraft:deep_ocean", Config::validateBiome);

        public static final ModConfigSpec.BooleanValue WRAP_ENTITIES = BUILDER
                        .comment("Wrap players, vehicles, projectiles and items around the seam.")
                        .define("wrapEntities", true);

        public static final ModConfigSpec.BooleanValue ANNOUNCE_CROSSING = BUILDER
                        .comment("Send an action bar message when a player crosses the seam.")
                        .define("announceCrossing", false);

        public static final ModConfigSpec.BooleanValue POLAR_PUSHBACK = BUILDER
                        .comment("Block movement across the pole line.")
                        .define("polarPushback", true);

        // ------------------------------------------------------------------
        // Phase 4: TFC 适配
        // ------------------------------------------------------------------

        public static final ModConfigSpec.BooleanValue TFC_COMPAT = BUILDER
                        .comment("Apply the world shape to TerraFirmaCraft region generation when TFC is installed.")
                        .define("tfcCompatibility", true);

        public static final ModConfigSpec.IntValue TFC_GRID_BLOCK_SIZE = BUILDER
                        .comment("Blocks per TFC region grid cell. TFC 1.21.1 uses 128; change only if TFC changes it.")
                        .defineInRange("tfcGridBlockSize", 128, 16, 4096);

        // ------------------------------------------------------------------
        // Phase 5: 性能与优化模组兼容
        // ------------------------------------------------------------------

        public static final ModConfigSpec.BooleanValue DENSITY_MASK = BUILDER
                        .comment("Wrap the vanilla density function with the shape mask.",
                                        "Disable only if a worldgen optimization mod conflicts (masking then falls back to",
                                        "the flat-ocean shortcut, biome/structure/feature filters and entity wrapping).")
                        .define("densityMask", true);

        public static final ModConfigSpec.BooleanValue CHUNK_MASK_CACHE = BUILDER
                        .comment("Precompute the 16x16 shape mask once per chunk while the noise chunk is filled.")
                        .define("chunkMaskCache", true);

        public static final ModConfigSpec.BooleanValue DISTANT_HORIZONS_COMPAT = BUILDER
                        .comment("Log a recommended Distant Horizons 'Earth Curve Ratio' when a shape is attached.")
                        .define("distantHorizonsCompat", true);

        public static final ModConfigSpec.DoubleValue DH_REFERENCE_HALF_MERIDIAN = BUILDER
                        .comment("Reference half meridian used by the Distant Horizons curve recommendation.")
                        .defineInRange("dhReferenceHalfMeridian", 20_000.0, 1_000.0, 1_000_000.0);

        public static final ModConfigSpec.DoubleValue DH_REFERENCE_CURVE_RATIO = BUILDER
                        .comment("Distant Horizons 'Earth Curve Ratio' for the reference half meridian (160 at 20 km).")
                        .defineInRange("dhReferenceCurveRatio", 160.0, 1.0, 10_000.0);

        static final ModConfigSpec SPEC = BUILDER.build();

        private Config() {
        }

        private static boolean validateDimension(final Object value) {
                return value instanceof String string && ResourceLocation.tryParse(string) != null;
        }

        private static boolean validateBiome(final Object value) {
                return value instanceof String string && ResourceLocation.tryParse(string) != null;
        }

        private static boolean validatePreset(final Object value) {
                return value instanceof String string
                                && (string.isBlank() || ResourceLocation.tryParse(string) != null);
        }

        /** 当前维度是否启用球形世界。 */
        public static boolean isDimensionEnabled(ResourceKey<Level> dimension) {
                if (!ENABLED.get()) {
                        return false;
                }
                String id = dimension.location().toString();
                for (String entry : DIMENSIONS.get()) {
                        if (entry.equals(id)) {
                                return true;
                        }
                }
                return false;
        }

        /**
         * 用当前配置创建一个新的世界形状。
         */
        public static WorldShape createShape() {
                EqualEarthProjection projection = new EqualEarthProjection(
                                HALF_MERIDIAN.get(),
                                WIDTH_TO_HEIGHT_RATIO.get(),
                                EQUATOR_Z.get());
                ResourceLocation fallback = ResourceLocation.tryParse(FALLBACK_BIOME.get());
                return new WorldShape(
                                BOUNDARY_MODE.get(),
                                projection,
                                SEAM_BUFFER.get(),
                                POLAR_BUFFER.get(),
                                SOFT_EDGE_WIDTH.get(),
                                STRUCTURE_MARGIN.get(),
                                MERIDIAN_GRACE.get(),
                                OCEAN_FLOOR_Y.get(),
                                fallback);
        }

        /**
         * 按“世界生成控制器”中选择的星球参数创建世界形状：
         * 半径推导世界尺寸，两个开关决定接缝/极地缓冲带宽度。
         */
        public static WorldShape createShapeFromPlanet(PlanetSettings settings) {
                double ratio = WIDTH_TO_HEIGHT_RATIO.get();
                double halfMeridian = PlanetSizing.halfMeridianFor(settings.radiusChunks(), ratio);
                EqualEarthProjection projection = new EqualEarthProjection(halfMeridian, ratio, EQUATOR_Z.get());

                // 允许大陆跨边界 = 去掉接缝海洋缓冲带；允许极地大陆 = 去掉极线缓冲带。
                double seamBuffer = settings.allowCrossBoundary() ? 0.0 : SEAM_BUFFER.get();
                double polarBuffer = settings.allowPolarContinents() ? 0.0 : POLAR_BUFFER.get();

                return new WorldShape(
                                BOUNDARY_MODE.get(),
                                projection,
                                seamBuffer,
                                polarBuffer,
                                SOFT_EDGE_WIDTH.get(),
                                STRUCTURE_MARGIN.get(),
                                MERIDIAN_GRACE.get(),
                                OCEAN_FLOOR_Y.get(),
                                ResourceLocation.tryParse(FALLBACK_BIOME.get()));
        }
}
