package com.aigcs.e2generator.client.sky;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.aigcs.e2generator.AIGCsEqualEarthGenerator;

import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.PackRepository;
import net.neoforged.fml.loading.FMLPaths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 首次启动时把内置的 {@code gameassets} 资源包加入默认选中列表。
 * <p>
 * 标记文件 {@code config/e2generator-resourcepack-default.json} 只写入一次：
 * 玩家之后在“资源包”界面手动关闭后，不会在下次启动时被重新打开。
 */
public final class DefaultSkyResourcePack {

    /** 与 {@code AddPackFindersEvent#addPackFinders} 生成的 Pack id 一致。 */
    public static final String PACK_ID = "mod/" + AIGCsEqualEarthGenerator.MODID + ":gameassets";

    private static final String MARKER_FILE = "e2generator-resourcepack-default.json";
    private static final Logger LOGGER = LoggerFactory.getLogger("e2generator/resourcepack");

    private static boolean checked;
    private static boolean pendingMarker;

    private DefaultSkyResourcePack() {
    }

    public static void markDefaultSelection(Options options, PackRepository repository) {
        if (checked) {
            return;
        }

        Path marker = FMLPaths.CONFIGDIR.get().resolve(MARKER_FILE);
        if (Files.exists(marker)) {
            checked = true;
            return;
        }
        LOGGER.debug("Default gameassets pack: available={}, selectedBefore={}, ids={}",
                repository.isAvailable(PACK_ID), options.resourcePacks.contains(PACK_ID),
                repository.getAvailableIds());
        if (!repository.isAvailable(PACK_ID)) {
            // 资源包注册失败时不要写标记，保留 checked=false 以便后续调用重试。
            LOGGER.warn("Resource pack {} is not available; default selection skipped", PACK_ID);
            return;
        }
        checked = true;
        if (!options.resourcePacks.contains(PACK_ID)) {
            options.resourcePacks.add(PACK_ID);
        }
        pendingMarker = true;
    }

    /** 在 {@code Options#loadSelectedResourcePacks} 完成后写入标记，避免中途崩溃导致永久跳过。 */
    public static void finishDefaultSelection() {
        if (!pendingMarker) {
            return;
        }
        pendingMarker = false;
        Path marker = FMLPaths.CONFIGDIR.get().resolve(MARKER_FILE);
        try {
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "{\"defaultPackApplied\":true}\n", StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.warn("Failed to write resource pack marker {}", marker, exception);
        }
    }
}
