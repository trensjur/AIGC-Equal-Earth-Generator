package com.aigcs.e2generator.compat.tfc.mixin;

import com.aigcs.e2generator.compat.tfc.TfcPointAccess;
import com.aigcs.e2generator.compat.tfc.TfcShapeBridge;
import com.aigcs.e2generator.core.WorldShape;

import net.dries007.tfc.world.region.Region;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 用世界形状的掩码替换 TFC 的 {@code RegionGenerator#continentFactor}。
 * <p>
 * 只在 TFC 安装时由 {@code TfcMixinPlugin} 应用；TFC 是 {@code compileOnly}
 * 依赖，核心 jar 不把 TFC 作为运行时依赖。
 */
@Mixin(targets = "net.dries007.tfc.world.region.RegionGenerator")
public abstract class RegionGeneratorMixin {

    @Inject(method = "continentFactor", at = @At("HEAD"), cancellable = true)
    private void spherical$continentFactor(Region.Point point, CallbackInfoReturnable<Float> cir) {
        if (!TfcShapeBridge.isEnabled() || !(point instanceof TfcPointAccess access)) {
            return;
        }
        WorldShape shape = TfcShapeBridge.shape();
        if (shape == null) {
            return;
        }
        cir.setReturnValue(TfcShapeBridge.continentFactor(access.spherical$gridX(), access.spherical$gridZ()));
    }
}
