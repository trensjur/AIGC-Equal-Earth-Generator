package com.aigcs.e2generator;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端配置：只影响 HUD 显示，不影响服务端逻辑。
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

    static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
