package com.aigcs.e2generator.compat.adastra.mixin;

import com.aigcs.e2generator.ClientConfig;
import com.aigcs.e2generator.client.sky.SkyVisualSettings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Ad Astra 自定义天空渲染兼容。
 * <p>
 * Ad Astra 的 {@code ModSkyRenderer#renderSkyRenderable} 负责绘制太阳、月亮、
 * 行星、背光等所有天体，贴图路径遵循 {@code ad_astra:textures/environment/*.png}。
 * 这里根据贴图路径套用日月 / 其它天体的缩放，并把当前的轨道倾斜叠加到
 * {@code globalRotation.z} 上。
 * <p>
 * 该类不直接引用任何 Ad Astra 类型，通过 {@code targets} 字符串定位；
 * Ad Astra 未安装时由
 * {@link com.aigcs.e2generator.compat.adastra.AdAstraMixinPlugin}
 * 跳过整个配置。
 */
@Mixin(targets = "earth.terrarium.adastra.client.dimension.ModSkyRenderer")
public abstract class AdAstraSkyRendererMixin {

    @ModifyVariable(method = "renderSkyRenderable", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float spherical$scaleSkyRenderable(float scale, Tesselator tesselator, PoseStack poseStack,
            Vec3 localRotation, Vec3 globalRotation, float originalScale, ResourceLocation texture,
            boolean blend) {
        if (!ClientConfig.AD_ASTRA_COMPAT.get()) {
            return scale;
        }
        return scale * SkyVisualSettings.scaleFor(texture);
    }

    @ModifyVariable(method = "renderSkyRenderable", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private Vec3 spherical$tiltSkyRenderable(Vec3 globalRotation) {
        if (!ClientConfig.AD_ASTRA_COMPAT.get()) {
            return globalRotation;
        }
        double tilt = SkyVisualSettings.tiltDegrees();
        return tilt == 0.0 ? globalRotation : globalRotation.add(0.0, 0.0, tilt);
    }
}
