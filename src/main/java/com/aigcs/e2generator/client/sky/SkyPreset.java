package com.aigcs.e2generator.client.sky;

import java.util.Locale;

/**
 * 天空 / 环境视觉效果的可保存快照。
 * <p>
 * 预设只包含本模组的客户端视觉参数，不包含 HUD 位置等无关设置。
 * 构造函数会对数值做一次夹取，避免损坏的 JSON 导致渲染异常。
 */
public record SkyPreset(
        String name,
        boolean tiltEnabled,
        boolean tiltAuto,
        double tiltStrength,
        double manualTilt,
        double sunScale,
        double moonScale,
        double celestialScale,
        boolean adAstraCompat) {

    public SkyPreset {
        if (name == null || name.isBlank()) {
            name = "Preset";
        }
        tiltStrength = clamp(tiltStrength, -2.0, 2.0, 1.0);
        manualTilt = clamp(manualTilt, -90.0, 90.0, 0.0);
        sunScale = clamp(sunScale, 0.25, 4.0, 1.0);
        moonScale = clamp(moonScale, 0.25, 4.0, 1.0);
        celestialScale = clamp(celestialScale, 0.1, 4.0, 1.0);
    }

    /** 供界面显示的简短摘要（不参与渲染）。 */
    public String describe() {
        String tilt = this.tiltEnabled
                ? (this.tiltAuto ? String.format(Locale.ROOT, "auto x%.2f", this.tiltStrength)
                        : String.format(Locale.ROOT, "%.1f°", this.manualTilt))
                : "off";
        return String.format(Locale.ROOT, "tilt=%s sun=%.2fx moon=%.2fx others=%.2fx",
                tilt, this.sunScale, this.moonScale, this.celestialScale);
    }

    private static double clamp(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}
