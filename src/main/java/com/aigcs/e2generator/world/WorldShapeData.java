package com.aigcs.e2generator.world;

import javax.annotation.Nullable;

import com.aigcs.e2generator.AIGCsEqualEarthGenerator;
import com.aigcs.e2generator.Config;
import com.aigcs.e2generator.api.event.WorldShapeCreatedEvent;
import com.aigcs.e2generator.core.PlanetSettings;
import com.aigcs.e2generator.core.ShapePreset;
import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 世界形状的持久化。
 * <p>
 * 形状<b>必须跟存档走</b>：配置只影响新世界；已有存档一律读取本数据，
 * 否则已生成区块会与新形状错位。
 */
public class WorldShapeData extends SavedData {

    public static final String DATA_NAME = "e2generator_world_shape";

    public static final SavedData.Factory<WorldShapeData> FACTORY = new SavedData.Factory<>(
            WorldShapeData::createDefault, WorldShapeData::load);

    @Nullable
    private final WorldShape shape;

    public WorldShapeData(@Nullable WorldShape shape) {
        this.shape = shape;
    }

    @Nullable
    public WorldShape shape() {
        return this.shape;
    }

    private static WorldShapeData createDefault() {
        // 优先级：待应用预设 > 世界生成控制器的星球参数 > 配置中的预设 > 单独的配置项。
        ResourceLocation presetId = PendingWorldSettings.consumePreset();
        PlanetSettings pending = PendingWorldSettings.consume();

        WorldShape shape;
        WorldShapeCreatedEvent.Source source;
        if (presetId != null) {
            ShapePreset preset = ShapePresetManager.get(presetId);
            if (preset != null) {
                shape = preset.toShape();
                source = WorldShapeCreatedEvent.Source.PRESET;
            } else {
                AIGCsEqualEarthGenerator.LOGGER.warn(
                        "[e2generator] Pending shape preset '{}' was not found; using the next available settings",
                        presetId);
                shape = pending != null ? Config.createShapeFromPlanet(pending) : Config.createShape();
                source = pending != null
                        ? WorldShapeCreatedEvent.Source.PLANET_SETTINGS
                        : WorldShapeCreatedEvent.Source.CONFIG;
            }
        } else if (pending != null) {
            shape = Config.createShapeFromPlanet(pending);
            source = WorldShapeCreatedEvent.Source.PLANET_SETTINGS;
        } else {
            ShapePreset configured = ShapePresetManager.fromConfig();
            if (configured != null) {
                shape = configured.toShape();
                source = WorldShapeCreatedEvent.Source.PRESET;
            } else {
                shape = Config.createShape();
                source = WorldShapeCreatedEvent.Source.CONFIG;
            }
        }

        // Phase 6：允许整合包/其它模组替换或取消新世界的形状。
        WorldShapeCreatedEvent event = new WorldShapeCreatedEvent(
                ServerLifecycleHooks.getCurrentServer(), shape, source);
        NeoForge.EVENT_BUS.post(event);
        shape = event.isCanceled() ? null : event.getShape();
        if (shape == null) {
            AIGCsEqualEarthGenerator.LOGGER.info(
                    "[e2generator] Spherical world shape disabled for this new world by an event listener");
        }

        WorldShapeData data = new WorldShapeData(shape);
        data.setDirty(true);
        return data;
    }

    private static WorldShapeData load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldShape shape = tag.contains("shape") ? WorldShape.load(tag.getCompound("shape")) : null;
        return new WorldShapeData(shape);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.shape != null) {
            tag.put("shape", this.shape.save());
        }
        tag.putInt("version", WorldShape.FORMAT_VERSION);
        return tag;
    }
}
