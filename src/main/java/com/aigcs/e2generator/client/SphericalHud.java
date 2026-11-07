package com.aigcs.e2generator.client;

import java.util.Locale;

import com.aigcs.e2generator.ClientConfig;
import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * 经纬度 HUD。客户端没有收到形状同步时不显示任何内容。
 */
public final class SphericalHud implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        if (!ClientConfig.SHOW_HUD.get()) {
            return;
        }
        WorldShape shape = ClientWorldShape.get();
        if (shape == null) {
            return;
        }

        double x = minecraft.player.getX();
        double z = minecraft.player.getZ();
        Component line = Component.translatable("e2generator.hud.line",
                format(shape.latitudeAt(z), true),
                format(shape.longitudeAt(x, z), false));

        graphics.drawString(minecraft.font, line,
                ClientConfig.HUD_X.get(), ClientConfig.HUD_Y.get(), 0xFFFFFF, true);
    }

    private static String format(double degrees, boolean latitude) {
        String pattern = ClientConfig.SHOW_DECIMALS.get() ? "%.3f" : "%.0f";
        String number = String.format(Locale.ROOT, pattern, Math.abs(degrees));
        String hemisphere;
        if (latitude) {
            hemisphere = degrees >= 0.0 ? "N" : "S";
        } else {
            hemisphere = degrees >= 0.0 ? "E" : "W";
        }
        return number + "°" + hemisphere;
    }
}
