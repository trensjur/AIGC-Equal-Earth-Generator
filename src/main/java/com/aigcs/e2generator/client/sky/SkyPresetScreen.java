package com.aigcs.e2generator.client.sky;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * 8 槽位本地预设库界面。
 * <p>
 * 每个槽位可以保存当前天空 / 环境设置、加载或删除。
 */
public final class SkyPresetScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_WIDTH = 300;

    private final Screen parent;
    @Nullable
    private Component status;

    public SkyPresetScreen(Screen parent) {
        super(Component.translatable("e2generator.sky.presets.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (this.width - ROW_WIDTH) / 2;
        int top = 34;

        for (int i = 0; i < SkyPresetStore.CAPACITY; i++) {
            int slot = i;
            SkyPreset preset = SkyPresetStore.get(i);
            Component label = preset == null
                    ? Component.translatable("e2generator.sky.preset.empty", i + 1)
                    : Component.translatable("e2generator.sky.preset.entry", i + 1, preset.describe());
            Button load = Button.builder(label, button -> load(slot))
                    .bounds(left, top + i * ROW_HEIGHT, 190, 18)
                    .tooltip(Tooltip.create(Component.translatable("e2generator.sky.preset.load.tooltip")))
                    .build();
            load.active = preset != null;
            this.addRenderableWidget(load);

            this.addRenderableWidget(Button.builder(
                    Component.translatable("e2generator.sky.preset.save"), button -> save(slot))
                    .bounds(left + 196, top + i * ROW_HEIGHT, 50, 18)
                    .tooltip(Tooltip.create(Component.translatable("e2generator.sky.preset.save.tooltip")))
                    .build());

            Button delete = Button.builder(
                    Component.translatable("e2generator.sky.preset.delete"), button -> delete(slot))
                    .bounds(left + 250, top + i * ROW_HEIGHT, 50, 18)
                    .tooltip(Tooltip.create(Component.translatable("e2generator.sky.preset.delete.tooltip")))
                    .build();
            delete.active = preset != null;
            this.addRenderableWidget(delete);
        }

        int bottom = this.height - 28;
        this.addRenderableWidget(Button.builder(
                Component.translatable("e2generator.sky.reset"), button -> reset())
                .bounds(this.width / 2 - 152, bottom, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("e2generator.sky.reset.tooltip")))
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 + 2, bottom, 150, 20)
                .build());
    }

    private void save(int slot) {
        String name = Component.translatable("e2generator.sky.preset.default_name", slot + 1).getString();
        SkyPresetStore.set(slot, SkyVisualSettings.capture(name));
        this.status = Component.translatable("e2generator.sky.preset.saved", slot + 1);
        this.rebuildWidgets();
    }

    private void load(int slot) {
        SkyPreset preset = SkyPresetStore.get(slot);
        if (preset == null) {
            return;
        }
        SkyVisualSettings.apply(preset);
        this.status = Component.translatable("e2generator.sky.preset.loaded", slot + 1);
        this.rebuildWidgets();
    }

    private void delete(int slot) {
        SkyPresetStore.clear(slot);
        this.status = Component.translatable("e2generator.sky.preset.deleted", slot + 1);
        this.rebuildWidgets();
    }

    private void reset() {
        SkyVisualSettings.resetToDefaults();
        this.status = Component.translatable("e2generator.sky.reset.done");
        this.rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        if (this.status != null) {
            graphics.drawCenteredString(this.font, this.status, this.width / 2, this.height - 44, 0xA0FFA0);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
