package com.aigcs.e2generator.mixin.client;

import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 {@link GridLayoutTab#layout}，便于向原版标签页追加控件。
 */
@Mixin(GridLayoutTab.class)
public interface GridLayoutTabAccessor {

    @Accessor("layout")
    GridLayout e2generator$layout();
}
