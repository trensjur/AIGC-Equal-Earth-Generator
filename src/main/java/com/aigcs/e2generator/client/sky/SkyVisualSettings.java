package com.aigcs.e2generator.client.sky;

import java.util.Locale;
import java.util.OptionalDouble;

import javax.annotation.Nullable;

import com.aigcs.e2generator.ClientConfig;
import com.aigcs.e2generator.api.SphericalWorldApi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 天空 / 环境视觉参数读取与应用。
 * <p>
 * 所有数值都在这里集中处理，Mixin 只负责在渲染时调用。
 */
public final class SkyVisualSettings {

    /**
     * 非球形维度没有赤道信息，用 z 坐标估算一个纬度：
     * {@code z = 30000} 对应 ±90°。球形维度优先使用真实纬度。
     */
    private static final double FALLBACK_LATITUDE_BLOCKS = 30000.0;

    private SkyVisualSettings() {
    }

    /**
     * 当前应施加的日月轨道倾斜角（度）。
     * <p>
     * 自动模式下取玩家当前纬度乘以强度；非球形维度退化为
     * {@code z / 30000 * 90°}。手动模式直接返回配置的固定角度。
     */
    public static double tiltDegrees() {
        if (!ClientConfig.SKY_TILT_ENABLED.get()) {
            return 0.0;
        }
        if (!ClientConfig.SKY_TILT_AUTO.get()) {
            return Mth.clamp(ClientConfig.SKY_MANUAL_TILT.get(), -90.0, 90.0);
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return 0.0;
        }

        OptionalDouble sphericalLatitude = SphericalWorldApi.getLatitude(level, player.getZ());
        double latitude = sphericalLatitude.isPresent()
                ? sphericalLatitude.getAsDouble()
                : Mth.clamp(player.getZ() / FALLBACK_LATITUDE_BLOCKS * 90.0, -90.0, 90.0);
        return Mth.clamp(latitude * ClientConfig.SKY_TILT_STRENGTH.get(), -89.0, 89.0);
    }

    /**
     * 根据贴图路径返回缩放倍数：
     * 路径包含 {@code moon} → 月亮缩放，包含 {@code sun} → 太阳缩放，其它天体使用通用缩放。
     */
    public static float scaleFor(@Nullable ResourceLocation texture) {
        if (texture != null) {
            String path = texture.getPath().toLowerCase(Locale.ROOT);
            if (path.contains("moon")) {
                return ClientConfig.MOON_TEXTURE_SCALE.get().floatValue();
            }
            if (path.contains("sun")) {
                return ClientConfig.SUN_TEXTURE_SCALE.get().floatValue();
            }
        }
        return ClientConfig.CELESTIAL_TEXTURE_SCALE.get().floatValue();
    }

    /** 当前配置的快照，用于保存到预设槽位。 */
    public static SkyPreset capture(String name) {
        return new SkyPreset(
                name,
                ClientConfig.SKY_TILT_ENABLED.get(),
                ClientConfig.SKY_TILT_AUTO.get(),
                ClientConfig.SKY_TILT_STRENGTH.get(),
                ClientConfig.SKY_MANUAL_TILT.get(),
                ClientConfig.SUN_TEXTURE_SCALE.get(),
                ClientConfig.MOON_TEXTURE_SCALE.get(),
                ClientConfig.CELESTIAL_TEXTURE_SCALE.get(),
                ClientConfig.AD_ASTRA_COMPAT.get());
    }

    /** 应用预设并立即写回客户端配置文件。 */
    public static void apply(@Nullable SkyPreset preset) {
        if (preset == null) {
            return;
        }
        ClientConfig.SKY_TILT_ENABLED.set(preset.tiltEnabled());
        ClientConfig.SKY_TILT_AUTO.set(preset.tiltAuto());
        ClientConfig.SKY_TILT_STRENGTH.set(preset.tiltStrength());
        ClientConfig.SKY_MANUAL_TILT.set(preset.manualTilt());
        ClientConfig.SUN_TEXTURE_SCALE.set(preset.sunScale());
        ClientConfig.MOON_TEXTURE_SCALE.set(preset.moonScale());
        ClientConfig.CELESTIAL_TEXTURE_SCALE.set(preset.celestialScale());
        ClientConfig.AD_ASTRA_COMPAT.set(preset.adAstraCompat());
        ClientConfig.save();
    }

    /** 把天空 / 环境设置恢复为默认值（不重置 HUD 设置）。 */
    public static void resetToDefaults() {
        ClientConfig.SKY_TILT_ENABLED.set(ClientConfig.SKY_TILT_ENABLED.getDefault());
        ClientConfig.SKY_TILT_AUTO.set(ClientConfig.SKY_TILT_AUTO.getDefault());
        ClientConfig.SKY_TILT_STRENGTH.set(ClientConfig.SKY_TILT_STRENGTH.getDefault());
        ClientConfig.SKY_MANUAL_TILT.set(ClientConfig.SKY_MANUAL_TILT.getDefault());
        ClientConfig.SUN_TEXTURE_SCALE.set(ClientConfig.SUN_TEXTURE_SCALE.getDefault());
        ClientConfig.MOON_TEXTURE_SCALE.set(ClientConfig.MOON_TEXTURE_SCALE.getDefault());
        ClientConfig.CELESTIAL_TEXTURE_SCALE.set(ClientConfig.CELESTIAL_TEXTURE_SCALE.getDefault());
        ClientConfig.AD_ASTRA_COMPAT.set(ClientConfig.AD_ASTRA_COMPAT.getDefault());
        ClientConfig.save();
    }
}
