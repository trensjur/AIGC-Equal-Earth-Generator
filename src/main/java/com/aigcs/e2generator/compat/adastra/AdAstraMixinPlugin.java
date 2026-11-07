package com.aigcs.e2generator.compat.adastra;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

/**
 * Ad Astra 兼容 Mixin 配置插件。
 * <p>
 * Ad Astra 是可选模组：未安装时跳过 {@code e2generator.adastra.mixins.json}，
 * 不影响核心天空渲染。
 */
public final class AdAstraMixinPlugin implements IMixinConfigPlugin {

    private static final String AD_ASTRA_MODID = "ad_astra";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return isAdAstraPresent();
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

    private static boolean isAdAstraPresent() {
        try {
            ModList modList = ModList.get();
            if (modList != null) {
                return modList.isLoaded(AD_ASTRA_MODID);
            }
        } catch (RuntimeException ignored) {
            // 继续尝试 FMLLoader。
        }
        try {
            return FMLLoader.getLoadingModList() != null
                    && FMLLoader.getLoadingModList().getModFileById(AD_ASTRA_MODID) != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
