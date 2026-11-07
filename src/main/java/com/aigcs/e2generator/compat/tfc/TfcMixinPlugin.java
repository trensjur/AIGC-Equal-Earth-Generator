package com.aigcs.e2generator.compat.tfc;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

/**
 * TFC Mixin 配置插件（Phase 4）。
 * <p>
 * TFC 是可选的：未安装时直接跳过 {@code e2generator.tfc.mixins.json} 中的所有 Mixin，
 * 核心 jar 不会因为缺少 TFC 而崩溃。
 */
public final class TfcMixinPlugin implements IMixinConfigPlugin {

    private static final String TFC_MODID = "tfc";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return isTfcPresent();
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isTfcPresent() {
        try {
            ModList modList = ModList.get();
            if (modList != null) {
                return modList.isLoaded(TFC_MODID);
            }
        } catch (RuntimeException ignored) {
            // 继续尝试 FMLLoader。
        }
        try {
            return FMLLoader.getLoadingModList() != null
                    && FMLLoader.getLoadingModList().getModFileById(TFC_MODID) != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
