package com.aigcs.e2generator.client;

import java.util.Locale;
import java.util.Random;

import javax.annotation.Nullable;

import com.aigcs.e2generator.core.EqualEarthProjection;
import com.aigcs.e2generator.core.PlanetSettings;
import com.aigcs.e2generator.core.PlanetSizing;
import com.aigcs.e2generator.world.PendingWorldSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 世界生成控制器（创建世界 → 更多选项 → 世界生成控制器）。
 * <p>
 * 玩家可以：
 * <ul>
 * <li>输入种子；留空时每次刷新（手动或参数变化）都会自动填入随机种子；</li>
 * <li>直接输入“星球半径（区块）”，实时预览由 Equal Earth 投影推算出的世界尺寸；</li>
 * <li>开关“允许极地生成大陆”“允许大陆跨边界生成”；</li>
 * <li>点“应用”把种子与星球参数写入即将创建的世界。</li>
 * </ul>
 */
public class WorldGenControllerScreen extends Screen {

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 240;
    private static final int PREVIEW_TOP = 100;
    private static final int PREVIEW_HEIGHT = 104;
    private static final int PREVIEW_WIDTH = 336;

    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM = 0xFFB8B8B8;
    private static final int COLOR_RULER = 0xFFA0A0A0;
    private static final int COLOR_LAND = 0x8030B060;
    private static final int COLOR_MAP_OUTLINE = 0xFFB0B0B0;
    private static final int COLOR_PANEL = 0xE8101010;
    private static final int COLOR_PANEL_BORDER = 0xFF808080;

    private final Screen parent;
    private final Random random = new Random();
    /** 单位投影（H = 1），用于绘制 Equal Earth 叶形轮廓。 */
    private final EqualEarthProjection unitProjection = new EqualEarthProjection(1.0,
            PlanetSizing.DEFAULT_WIDTH_TO_HEIGHT_RATIO, 0.0);

    private EditBox seedEdit;
    private EditBox radiusEdit;
    @Nullable
    private PlanetSizing.Result preview;

    private int left;
    private int top;

    // 记住用户的选择，界面重建（窗口缩放）时不丢失
    private int radiusChunks = PlanetSizing.DEFAULT_RADIUS_CHUNKS;
    private boolean allowPolarContinents;
    private boolean allowCrossBoundary;

    public WorldGenControllerScreen(Screen parent) {
        super(Component.translatable("e2generator.worldgen.title"));
        this.parent = parent;
    }

    /** 从创建世界界面的“更多选项”标签页打开。 */
    public static void open(Minecraft minecraft) {
        if (minecraft.screen instanceof CreateWorldScreen createWorldScreen) {
            minecraft.setScreen(new WorldGenControllerScreen(createWorldScreen));
        }
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;
        int x = this.left + 12;
        int y = this.top + 28;

        this.seedEdit = new EditBox(this.font, 190, 20, Component.translatable("e2generator.worldgen.seed"));
        this.seedEdit.setMaxLength(64);
        this.seedEdit.setValue(this.currentSeed());
        this.seedEdit.setPosition(x + 34, y);
        this.seedEdit.setTooltip(Tooltip.create(Component.translatable("e2generator.worldgen.seed.tooltip")));
        this.addRenderableWidget(this.seedEdit);

        this.addRenderableWidget(Button.builder(Component.translatable("e2generator.worldgen.random"),
                button -> {
                    this.seedEdit.setValue(Long.toString(this.random.nextLong()));
                    this.refresh(false);
                })
                .tooltip(Tooltip.create(Component.translatable("e2generator.worldgen.random.tooltip")))
                .bounds(x + 230, y, 94, 20)
                .build());

        // 整型数值栏：直接输入数字（不再使用滑块）
        this.radiusEdit = new EditBox(this.font, 100, 20, Component.translatable("e2generator.worldgen.radius_label"));
        this.radiusEdit.setMaxLength(5);
        this.radiusEdit.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        this.radiusEdit.setValue(Integer.toString(this.radiusChunks));
        this.radiusEdit.setPosition(x + 80, y + 26);
        this.radiusEdit.setTooltip(Tooltip.create(Component.translatable("e2generator.worldgen.radius.tooltip",
                PlanetSizing.MIN_RADIUS_CHUNKS, PlanetSizing.MAX_RADIUS_CHUNKS)));
        this.radiusEdit.setResponder(this::onRadiusTextChanged);
        this.addRenderableWidget(this.radiusEdit);

        this.addRenderableWidget(Checkbox.builder(Component.translatable("e2generator.worldgen.polar"), this.font)
                .pos(x, y + 54)
                .selected(this.allowPolarContinents)
                .tooltip(Tooltip.create(Component.translatable("e2generator.worldgen.polar.tooltip")))
                .onValueChange((checkbox, value) -> {
                    this.allowPolarContinents = value;
                    this.refresh(true);
                })
                .build());

        this.addRenderableWidget(
                Checkbox.builder(Component.translatable("e2generator.worldgen.cross_boundary"), this.font)
                        .pos(x + 172, y + 54)
                        .selected(this.allowCrossBoundary)
                        .tooltip(Tooltip.create(Component.translatable("e2generator.worldgen.cross_boundary.tooltip")))
                        .onValueChange((checkbox, value) -> {
                            this.allowCrossBoundary = value;
                            this.refresh(true);
                        })
                        .build());

        this.addRenderableWidget(Button.builder(Component.translatable("e2generator.worldgen.refresh"),
                button -> {
                    this.normalizeRadiusText();
                    this.refresh(true);
                })
                .tooltip(Tooltip.create(Component.translatable("e2generator.worldgen.refresh.tooltip")))
                .bounds(x, this.top + 210, 84, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("e2generator.worldgen.apply"),
                button -> this.applySettings())
                .tooltip(Tooltip.create(Component.translatable("e2generator.worldgen.apply.tooltip")))
                .bounds(x + 176, this.top + 210, 80, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL,
                button -> this.onClose())
                .tooltip(Tooltip.create(Component.translatable("e2generator.worldgen.cancel.tooltip")))
                .bounds(x + 260, this.top + 210, 76, 20)
                .build());

        this.refresh(true);
    }

    private String currentSeed() {
        if (this.parent instanceof CreateWorldScreen createWorldScreen) {
            return createWorldScreen.getUiState().getSeed();
        }
        return "";
    }

    /** 刷新预览；{@code fillSeedIfEmpty} 为真且种子为空时自动填入随机种子。 */
    private void refresh(boolean fillSeedIfEmpty) {
        if (fillSeedIfEmpty && this.seedEdit != null && this.seedEdit.getValue().isBlank()) {
            this.seedEdit.setValue(Long.toString(this.random.nextLong()));
        }
        this.preview = PlanetSizing.compute(this.radiusChunks, PlanetSizing.DEFAULT_WIDTH_TO_HEIGHT_RATIO);
    }

    /** 半径输入框的实时响应：输入值先夹取到合法范围再刷新预览。 */
    private void onRadiusTextChanged(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(text.trim());
        } catch (NumberFormatException exception) {
            return;
        }
        int clamped = (int) PlanetSizing.clampRadius(parsed);
        if (clamped != this.radiusChunks) {
            this.radiusChunks = clamped;
            this.refresh(true);
        }
    }

    /** 把手写输入规范化为 64 ~ 64000 的整数（手动刷新 / 应用时调用）。 */
    private void normalizeRadiusText() {
        int parsed = this.radiusChunks;
        if (this.radiusEdit != null) {
            String text = this.radiusEdit.getValue().trim();
            if (!text.isEmpty()) {
                try {
                    parsed = Integer.parseInt(text);
                } catch (NumberFormatException ignored) {
                    // 保持上一次的有效值
                }
            }
            this.radiusChunks = (int) PlanetSizing.clampRadius(parsed);
            String canonical = Integer.toString(this.radiusChunks);
            if (!canonical.equals(this.radiusEdit.getValue())) {
                this.radiusEdit.setValue(canonical);
            }
        } else {
            this.radiusChunks = (int) PlanetSizing.clampRadius(parsed);
        }
    }

    private String radiusRangeHint() {
        return PlanetSizing.MIN_RADIUS_CHUNKS + " ~ " + PlanetSizing.MAX_RADIUS_CHUNKS;
    }

    private void applySettings() {
        this.normalizeRadiusText();
        String seed = this.seedEdit.getValue().isBlank()
                ? Long.toString(this.random.nextLong())
                : this.seedEdit.getValue().trim();
        this.seedEdit.setValue(seed);

        PendingWorldSettings
                .set(new PlanetSettings(this.radiusChunks, this.allowPolarContinents, this.allowCrossBoundary));

        if (this.parent instanceof CreateWorldScreen createWorldScreen) {
            createWorldScreen.getUiState().setSeed(seed);
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(this.left, this.top, this.left + PANEL_WIDTH, this.top + PANEL_HEIGHT, COLOR_PANEL);
        graphics.renderOutline(this.left, this.top, PANEL_WIDTH, PANEL_HEIGHT, COLOR_PANEL_BORDER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.top + 9, COLOR_TEXT);

        int labelX = this.left + 12;
        int seedY = this.top + 28;
        graphics.drawString(this.font, Component.translatable("e2generator.worldgen.seed"),
                labelX, seedY + 6, COLOR_TEXT_DIM, false);
        graphics.drawString(this.font, Component.translatable("e2generator.worldgen.radius_label"),
                labelX, seedY + 32, COLOR_TEXT_DIM, false);
        boolean outOfRange = this.radiusEdit != null
                && !Integer.toString(this.radiusChunks).equals(this.radiusEdit.getValue());
        graphics.drawString(this.font, this.radiusRangeHint(), labelX + 186, seedY + 32,
                outOfRange ? 0xFFFF7070 : COLOR_TEXT_DIM, false);

        this.renderPreview(graphics);

        // 标尺区域悬停提示
        int px = this.left + 12;
        int py = this.top + PREVIEW_TOP;
        if (mouseX >= px && mouseX < px + PREVIEW_WIDTH && mouseY >= py && mouseY < py + PREVIEW_HEIGHT) {
            this.setTooltipForNextRenderPass(Component.translatable("e2generator.worldgen.preview.tooltip"));
        }
    }

    private void renderPreview(GuiGraphics graphics) {
        PlanetSizing.Result result = this.preview;

        int px = this.left + 12;
        int py = this.top + PREVIEW_TOP;
        graphics.fill(px, py, px + PREVIEW_WIDTH, py + PREVIEW_HEIGHT, 0x60000000);
        graphics.renderOutline(px, py, PREVIEW_WIDTH, PREVIEW_HEIGHT, 0xFF606060);

        int mapX = px + 10;
        int mapY = py + 10;
        int mapW = 150;
        int mapH = 64;
        int centerX = mapX + mapW / 2;
        double ratio = PlanetSizing.DEFAULT_WIDTH_TO_HEIGHT_RATIO;

        // Equal Earth 叶形轮廓：逐行按半宽填充
        for (int row = 0; row < mapH; row++) {
            double t = (row + 0.5) / mapH;
            double z = -1.0 + 2.0 * t;
            double halfWidth = this.unitProjection.halfWidthAtZ(z);
            int halfPixels = (int) Math.round(halfWidth / ratio * (mapW / 2.0));
            graphics.fill(centerX - halfPixels, mapY + row, centerX + halfPixels, mapY + row + 1, COLOR_LAND);
        }
        graphics.fill(mapX, mapY + mapH / 2, mapX + mapW, mapY + mapH / 2 + 1, 0x80FFFFFF);
        graphics.renderOutline(mapX, mapY, mapW, mapH, COLOR_MAP_OUTLINE);
        graphics.drawString(this.font, "N", mapX + 2, mapY + 1, COLOR_TEXT_DIM, false);
        graphics.drawString(this.font, "S", mapX + 2, mapY + mapH - 10, COLOR_TEXT_DIM, false);

        if (result == null) {
            return;
        }

        // 横向标尺（区块）
        int rulerY = mapY + mapH + 8;
        graphics.fill(mapX, rulerY, mapX + mapW, rulerY + 1, COLOR_RULER);
        for (int i = 0; i <= 4; i++) {
            int tickX = mapX + (mapW - 1) * i / 4;
            graphics.fill(tickX, rulerY - 2, tickX + 1, rulerY + 3, COLOR_RULER);
            if (i % 2 == 0) {
                String label = formatNumber(result.worldWidthChunks() * i / 4.0);
                int labelWidth = this.font.width(label);
                int labelX = Mth.clamp(tickX - labelWidth / 2, mapX, mapX + mapW - labelWidth);
                graphics.drawString(this.font, label, labelX, rulerY + 6, COLOR_TEXT_DIM, false);
            }
        }

        // 右侧数据
        int tx = mapX + mapW + 14;
        int ty = py + 8;
        this.drawStat(graphics, tx, ty, "e2generator.worldgen.stat.radius",
                formatNumber(result.radiusBlocks()), COLOR_TEXT);
        ty += 11;
        this.drawStat(graphics, tx, ty, "e2generator.worldgen.stat.radius_chunks",
                formatNumber(result.radiusChunks()), COLOR_TEXT_DIM);
        ty += 11;
        this.drawStat(graphics, tx, ty, "e2generator.worldgen.stat.size",
                formatNumber(result.worldWidthBlocks()), formatNumber(result.worldHeightBlocks()));
        ty += 11;
        this.drawStat(graphics, tx, ty, "e2generator.worldgen.stat.size_chunks",
                formatNumber(result.worldWidthChunks()), formatNumber(result.worldHeightChunks()));
        ty += 11;
        this.drawStat(graphics, tx, ty, "e2generator.worldgen.stat.circumference",
                formatNumber(result.circumference()), COLOR_TEXT_DIM);
        ty += 11;
        this.drawStat(graphics, tx, ty, "e2generator.worldgen.stat.area");
        ty += 11;
        this.drawStat(graphics, tx, ty, "e2generator.worldgen.stat.seed",
                this.shortenSeed(this.seedEdit == null ? "" : this.seedEdit.getValue()), COLOR_TEXT_DIM);
    }

    private void drawStat(GuiGraphics graphics, int x, int y, String key, Object... args) {
        graphics.drawString(this.font, Component.translatable(key, args), x, y, COLOR_TEXT_DIM, false);
    }

    private void drawStat(GuiGraphics graphics, int x, int y, String key, String value, int color) {
        graphics.drawString(this.font, Component.translatable(key, value), x, y, color, false);
    }

    private String shortenSeed(String seed) {
        if (seed == null || seed.isBlank()) {
            return "-";
        }
        return seed.length() <= 12 ? seed : seed.substring(0, 11) + "…";
    }

    private static String formatNumber(double value) {
        return String.format(Locale.ROOT, "%,d", Math.round(value));
    }
}
