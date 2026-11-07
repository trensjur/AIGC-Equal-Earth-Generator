package com.aigcs.e2generator.mixin.client;

import com.aigcs.e2generator.ClientConfig;
import com.aigcs.e2generator.client.sky.SkyVisualSettings;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LevelRenderer;

import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 原版天空的日月缩放与轨道倾斜。
 * <p>
 * 只在 {@code DimensionSpecialEffects.SkyType.NORMAL} 且使用原版天空时生效；
 * Ad Astra 等自定义天空由对应的兼容 Mixin 处理。
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    /** 原版太阳四边形半边长 30。 */
    @ModifyConstant(method = "renderSky", constant = @Constant(floatValue = 30.0F), require = 1)
    private float spherical$sunTextureScale(float original) {
        return original * ClientConfig.SUN_TEXTURE_SCALE.get().floatValue();
    }

    /** 原版月亮四边形半边长 20。 */
    @ModifyConstant(method = "renderSky", constant = @Constant(floatValue = 20.0F), require = 1)
    private float spherical$moonTextureScale(float original) {
        return original * ClientConfig.MOON_TEXTURE_SCALE.get().floatValue();
    }

    /**
     * 在原版日月围绕 X 轴旋转之后追加 Z 轴倾斜。
     * <p>
     * {@code renderSky} 中共有 5 次 {@code Axis.rotationDegrees(...)} 调用：
     * 日出光晕 3 次、日月轨道 Y 轴 1 次、日月轨道 X 轴 1 次；这里取最后一个，
     * 因此只倾斜日月轨道，不影响天空盒与日出光晕。
     */
    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 4))
    private Quaternionf spherical$tiltCelestialPath(Axis axis, float degrees) {
        Quaternionf rotation = axis.rotationDegrees(degrees);
        double tilt = SkyVisualSettings.tiltDegrees();
        if (tilt != 0.0) {
            rotation.mul(Axis.ZP.rotationDegrees((float) tilt));
        }
        return rotation;
    }
}
