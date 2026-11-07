package com.aigcs.e2generator.client.sky;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * 客户端配置页：在标准 NeoForge 配置界面底部追加
 * “本地预设库”和“重置天空设置”按钮。
 */
public final class SkyConfigurationSectionScreen extends ConfigurationScreen.ConfigurationSectionScreen {

    public SkyConfigurationSectionScreen(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title) {
        super(parent, type, modConfig, title);
    }

    @Override
    protected void addFooter() {
        super.addFooter();
        LinearLayout row = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        row.addChild(Button.builder(
                Component.translatable("e2generator.sky.presets.open"),
                button -> this.minecraft.setScreen(new SkyPresetScreen(this)))
                .width(150)
                .build());
        row.addChild(Button.builder(
                Component.translatable("e2generator.sky.reset"),
                button -> {
                    SkyVisualSettings.resetToDefaults();
                    this.rebuild();
                })
                .width(150)
                .build());
    }
}
