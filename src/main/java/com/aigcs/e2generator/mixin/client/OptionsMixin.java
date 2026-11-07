package com.aigcs.e2generator.mixin.client;

import com.aigcs.e2generator.client.sky.DefaultSkyResourcePack;

import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.PackRepository;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 首次启动时默认启用内置的 {@code gameassets} 资源包。
 * <p>
 * 注入在读取 {@code options.txt} 之后、真正构建选中列表之前，
 * 因此不需要额外触发一次资源重载。
 */
@Mixin(Options.class)
public abstract class OptionsMixin {

    @Inject(method = "loadSelectedResourcePacks", at = @At("HEAD"))
    private void spherical$enableDefaultResourcePack(PackRepository repository, CallbackInfo ci) {
        DefaultSkyResourcePack.markDefaultSelection((Options) (Object) this, repository);
    }

    @Inject(method = "loadSelectedResourcePacks", at = @At("RETURN"))
    private void spherical$finishDefaultResourcePack(PackRepository repository, CallbackInfo ci) {
        DefaultSkyResourcePack.finishDefaultSelection();
    }
}
