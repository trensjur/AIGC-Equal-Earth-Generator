package com.aigcs.e2generator.mixin.client;

import com.aigcs.e2generator.client.WorldGenControllerScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在创建世界界面的"更多选项（More）"标签页末尾追加"世界生成控制器"入口。
 * <p>
 * 这里只在 {@code init()} 结束后往原版标签页的 {@code GridLayout} 里加一行按钮，
 * 不修改任何原版字段或方法体：
 * {@code init()} 每次都会重建标签页，因此不会重复添加。
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    @Shadow
    private TabNavigationBar tabNavigationBar;

    @Inject(method = "init", at = @At("TAIL"))
    private void e2generator$addWorldGenControllerButton(CallbackInfo ci) {
        if (this.tabNavigationBar == null) {
            return;
        }
        for (Tab tab : ((TabNavigationBarAccessor) (Object) this.tabNavigationBar).e2generator$tabs()) {
            if (tab instanceof GridLayoutTab && tab.getClass().getName().endsWith("$MoreTab")) {
                // 原版 MoreTab 已有 3 行（游戏规则 / 实验性功能 / 数据包），新按钮放在第 4 行。
                ((GridLayoutTabAccessor) tab).e2generator$layout().addChild(
                        Button.builder(Component.translatable("e2generator.worldgen.open"),
                                button -> WorldGenControllerScreen.open(Minecraft.getInstance()))
                                .width(210)
                                .build(),
                        3, 0);
                return;
            }
        }
    }
}
