package com.aigcs.e2generator.client.sky;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.neoforged.fml.loading.FMLPaths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本地天空 / 环境预设库。
 * <p>
 * 容量固定为 {@value #CAPACITY} 个槽位，文件保存在
 * {@code config/e2generator-sky-presets.json}。空槽位在 JSON 中为 {@code null}。
 */
public final class SkyPresetStore {

    public static final int CAPACITY = 8;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    private static final Logger LOGGER = LoggerFactory.getLogger("e2generator/sky-presets");
    private static final String FILE_NAME = "e2generator-sky-presets.json";

    private static final List<SkyPreset> PRESETS = new ArrayList<>(Collections.nCopies(CAPACITY, null));
    private static boolean loaded;

    private SkyPresetStore() {
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        load();
    }

    @Nullable
    public static synchronized SkyPreset get(int slot) {
        ensureLoaded();
        return isValidSlot(slot) ? PRESETS.get(slot) : null;
    }

    public static synchronized void set(int slot, SkyPreset preset) {
        ensureLoaded();
        if (!isValidSlot(slot) || preset == null) {
            return;
        }
        PRESETS.set(slot, preset);
        save();
    }

    public static synchronized void clear(int slot) {
        ensureLoaded();
        if (!isValidSlot(slot)) {
            return;
        }
        PRESETS.set(slot, null);
        save();
    }

    /** 8 个槽位的只读快照，空槽位为 {@code null}。 */
    public static synchronized List<SkyPreset> snapshot() {
        ensureLoaded();
        return Collections.unmodifiableList(new ArrayList<>(PRESETS));
    }

    public static int capacity() {
        return CAPACITY;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < CAPACITY;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private static void load() {
        Path path = file();
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement rootElement = JsonParser.parseString(json);
            if (!rootElement.isJsonObject()) {
                return;
            }
            JsonElement presetsElement = rootElement.getAsJsonObject().get("presets");
            if (presetsElement == null || !presetsElement.isJsonArray()) {
                return;
            }
            JsonArray array = presetsElement.getAsJsonArray();
            for (int i = 0; i < Math.min(CAPACITY, array.size()); i++) {
                JsonElement element = array.get(i);
                if (element == null || element.isJsonNull()) {
                    PRESETS.set(i, null);
                    continue;
                }
                SkyPreset preset = GSON.fromJson(element, SkyPreset.class);
                if (preset != null) {
                    // 通过规范构造器重新创建，确保夹取校验生效。
                    PRESETS.set(i, new SkyPreset(
                            preset.name(), preset.tiltEnabled(), preset.tiltAuto(),
                            preset.tiltStrength(), preset.manualTilt(),
                            preset.sunScale(), preset.moonScale(), preset.celestialScale(),
                            preset.adAstraCompat()));
                }
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Failed to read sky presets from {}", path, exception);
        }
    }

    private static void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            JsonArray array = new JsonArray();
            for (SkyPreset preset : PRESETS) {
                if (preset == null) {
                    array.add(JsonNull.INSTANCE);
                } else {
                    array.add(GSON.toJsonTree(preset));
                }
            }
            root.add("presets", array);
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Failed to save sky presets to {}", path, exception);
        }
    }
}
