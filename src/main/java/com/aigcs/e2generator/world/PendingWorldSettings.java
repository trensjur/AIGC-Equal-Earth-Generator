package com.aigcs.e2generator.world;

import javax.annotation.Nullable;

import com.aigcs.e2generator.core.PlanetSettings;

import net.minecraft.resources.ResourceLocation;

/**
 * 世界生成控制器与"新建世界"之间的桥接。
 * <p>
 * 单机（集成服务器）与客户端共享同一个 JVM，因此客户端在控制器里点"应用"后，
 * 新建世界时 {@link WorldShapeData} 会取用这里的参数；取用后立即清空，
 * 不会影响之后新建的其它世界。
 * <p>
 * 除了控制器里的星球参数，还可以单独指定一个数据包/内置形状预设 id；
 * 两者同时存在时预设优先。
 */
public final class PendingWorldSettings {

    @Nullable
    private static volatile PlanetSettings pending;

    @Nullable
    private static volatile ResourceLocation pendingPreset;

    private PendingWorldSettings() {
    }

    public static void set(@Nullable PlanetSettings settings) {
        pending = settings;
    }

    /** 指定下一个新建世界使用的形状预设。 */
    public static void setPreset(@Nullable ResourceLocation preset) {
        pendingPreset = preset;
    }

    @Nullable
    public static PlanetSettings consume() {
        PlanetSettings current = pending;
        pending = null;
        return current;
    }

    @Nullable
    public static ResourceLocation consumePreset() {
        ResourceLocation current = pendingPreset;
        pendingPreset = null;
        return current;
    }

    public static void clear() {
        pending = null;
        pendingPreset = null;
    }
}
