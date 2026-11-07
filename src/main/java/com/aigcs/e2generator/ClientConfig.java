package com.aigcs.e2generator;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端配置：HUD 与天空/环境视觉效果，不影响服务端逻辑。
 */
public class ClientConfig {

        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public static final ModConfigSpec.BooleanValue SHOW_HUD = BUILDER
                        .comment("Show latitude / longitude on the HUD when the server provides a spherical shape.")
                        .define("showHud", true);

        public static final ModConfigSpec.IntValue HUD_X = BUILDER
                        .comment("HUD X offset from the top-left corner.")
                        .defineInRange("hudX", 4, 0, 4096);

        public static final ModConfigSpec.IntValue HUD_Y = BUILDER
                        .comment("HUD Y offset from the top-left corner.")
                        .defineInRange("hudY", 4, 0, 4096);

        public static final ModConfigSpec.BooleanValue SHOW_DECIMALS = BUILDER
                        .comment("Show three decimals instead of whole degrees.")
                        .define("showDecimals", true);

        // ------------------------------------------------------------------
        // 0.9.6 天空 / 环境
        // ------------------------------------------------------------------

        public static final ModConfigSpec.BooleanValue SKY_TILT_ENABLED = BUILDER
                        .comment("Enable the additional sun/moon orbital tilt.")
                        .define("skyTiltEnabled", true);

        public static final ModConfigSpec.BooleanValue SKY_TILT_AUTO = BUILDER
                        .comment("Derive the tilt from the player's latitude (spherical worlds) or dimension position.",
                                        "When false, skyManualTilt is used instead.")
                        .define("skyTiltAuto", true);

        public static final ModConfigSpec.DoubleValue SKY_TILT_STRENGTH = BUILDER
                        .comment("Multiplier applied to the automatic tilt. 1.0 means tilt = latitude in degrees.")
                        .defineInRange("skyTiltStrength", 1.0, -2.0, 2.0);

        public static final ModConfigSpec.DoubleValue SKY_MANUAL_TILT = BUILDER
                        .comment("Fixed sun/moon tilt in degrees when skyTiltAuto is false.")
                        .defineInRange("skyManualTilt", 0.0, -90.0, 90.0);

        public static final ModConfigSpec.DoubleValue SUN_TEXTURE_SCALE = BUILDER
                        .comment("Rendered size multiplier for sun textures (vanilla and supported mods).")
                        .defineInRange("sunTextureScale", 1.0, 0.25, 4.0);

        public static final ModConfigSpec.DoubleValue MOON_TEXTURE_SCALE = BUILDER
                        .comment("Rendered size multiplier for moon textures (vanilla and supported mods).")
                        .defineInRange("moonTextureScale", 1.0, 0.25, 4.0);

        public static final ModConfigSpec.DoubleValue CELESTIAL_TEXTURE_SCALE = BUILDER
                        .comment("Rendered size multiplier for other celestial bodies added by mods (e.g. Ad Astra planets).")
                        .defineInRange("celestialTextureScale", 1.0, 0.1, 4.0);

        public static final ModConfigSpec.BooleanValue AD_ASTRA_COMPAT = BUILDER
                        .comment("Apply tilt and texture scale to Ad Astra's custom sky renderer when it is installed.")
                        .define("adAstraCompatibility", true);

        static final ModConfigSpec SPEC = BUILDER.build();

        private ClientConfig() {
        }

        /** 把当前客户端配置写回 e2generator-client.toml。 */
        public static void save() {
                SPEC.save();
        }
}
