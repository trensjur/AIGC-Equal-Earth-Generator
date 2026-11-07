package com.aigcs.e2generator.mixin.client;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露标签栏的标签列表，用于向"更多选项"标签页追加内容。
 */
@Mixin(TabNavigationBar.class)
public interface TabNavigationBarAccessor {

    @Accessor("tabs")
    ImmutableList<Tab> e2generator$tabs();
}
