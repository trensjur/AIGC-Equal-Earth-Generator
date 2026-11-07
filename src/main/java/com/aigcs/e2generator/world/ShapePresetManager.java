package com.aigcs.e2generator.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.aigcs.e2generator.AIGCsEqualEarthGenerator;
import com.aigcs.e2generator.Config;
import com.aigcs.e2generator.core.BoundaryMode;
import com.aigcs.e2generator.core.PlanetSizing;
import com.aigcs.e2generator.core.ShapePreset;
import com.aigcs.e2generator.core.WorldShape;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * 世界形状预设管理器（Phase 6）。
 * <p>
 * 预设来源：
 * <ol>
 * <li>内置预设（代码中定义，任何环境下都可用）；</li>
 * <li>数据包：{@code data/<namespace>/e2generator/shape/<path>.json}，可覆盖同名内置预设。</li>
 * </ol>
 * 通过 {@code AddReloadListenerEvent} 注册，{@code /reload} 时会重新扫描数据包。
 * 预设只影响<b>新建世界</b>（由 {@link Config#SHAPE_PRESET} 或世界创建流程选择）。
 */
public final class ShapePresetManager extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "e2generator/shape";

    private static final Gson GSON = new GsonBuilder().create();

    private static final Map<ResourceLocation, ShapePreset> BUILT_INS = createBuiltIns();

    private static volatile Map<ResourceLocation, ShapePreset> presets = BUILT_INS;

    public static final ShapePresetManager INSTANCE = new ShapePresetManager();

    private ShapePresetManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        Map<ResourceLocation, ShapePreset> merged = new LinkedHashMap<>(BUILT_INS);
        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                if (!entry.getValue().isJsonObject()) {
                    throw new JsonSyntaxException("Shape preset must be a JSON object: " + id);
                }
                merged.put(id, ShapePreset.fromJson(id, entry.getValue().getAsJsonObject()));
            } catch (RuntimeException exception) {
                AIGCsEqualEarthGenerator.LOGGER.error("[e2generator] Skipping invalid shape preset {}", id,
                        exception);
            }
        }
        presets = Map.copyOf(merged);
        AIGCsEqualEarthGenerator.LOGGER.info("[e2generator] Loaded {} shape preset(s)", presets.size());
    }

    /** 按 id 查询预设；不存在时返回 {@code null}。 */
    @Nullable
    public static ShapePreset get(@Nullable ResourceLocation id) {
        return id == null ? null : presets.get(id);
    }

    /** 所有可用预设（按 id 排序）。 */
    public static List<ShapePreset> all() {
        List<ShapePreset> list = new ArrayList<>(presets.values());
        list.sort(Comparator.comparing(preset -> preset.id().toString()));
        return list;
    }

    /**
     * 读取配置 {@code shapePreset} 指定的预设。留空或无效时返回 {@code null}。
     */
    @Nullable
    public static ShapePreset fromConfig() {
        String raw;
        try {
            raw = Config.SHAPE_PRESET.get();
        } catch (IllegalStateException exception) {
            return null;
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(raw.trim());
        if (id == null) {
            AIGCsEqualEarthGenerator.LOGGER.warn("[e2generator] Invalid shapePreset '{}' in server config", raw);
            return null;
        }
        ShapePreset preset = get(id);
        if (preset == null) {
            AIGCsEqualEarthGenerator.LOGGER.warn("[e2generator] Shape preset '{}' was not found; "
                    + "falling back to the individual shape settings", id);
        }
        return preset;
    }

    @Nullable
    public static WorldShape createShape(@Nullable ResourceLocation id) {
        ShapePreset preset = get(id);
        return preset == null ? null : preset.toShape();
    }

    private static Map<ResourceLocation, ShapePreset> createBuiltIns() {
        Map<ResourceLocation, ShapePreset> map = new LinkedHashMap<>();
        map.put(id("earth"), preset(id("earth"),
                "Default Earth-like world (500 chunk radius, full Equal Earth outline)",
                BoundaryMode.OUTLINE, 500.0, 2.0, 384.0, 192.0));
        map.put(id("half_world"), preset(id("half_world"),
                "Half world (1:1 width/height) for old-world / new-world style maps",
                BoundaryMode.OUTLINE, 500.0, 1.0, 384.0, 192.0));
        map.put(id("compact"), preset(id("compact"),
                "Compact world (200 chunk radius) for small servers",
                BoundaryMode.OUTLINE, 200.0, 2.0, 256.0, 128.0));
        map.put(id("large"), preset(id("large"),
                "Large world (1500 chunk radius) for long expeditions",
                BoundaryMode.OUTLINE, 1500.0, 2.0, 512.0, 256.0));
        map.put(id("rectangle"), preset(id("rectangle"),
                "Rectangular boundary with straight seams (most compatible)",
                BoundaryMode.RECTANGLE, 500.0, 2.0, 384.0, 192.0));
        return Map.copyOf(map);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("e2generator", path);
    }

    private static ShapePreset preset(ResourceLocation id, String description, BoundaryMode boundary,
            double radiusChunks, double ratio, double seamBuffer, double polarBuffer) {
        return new ShapePreset(
                id,
                description,
                boundary,
                PlanetSizing.halfMeridianFor(radiusChunks, ratio),
                ratio,
                0.0,
                seamBuffer,
                polarBuffer,
                96.0,
                256.0,
                32.0,
                24,
                ResourceLocation.fromNamespaceAndPath("minecraft", "deep_ocean"));
    }

    /** 供资源重载器调试用：当前预设数量。 */
    public static int size() {
        return presets.size();
    }
}
