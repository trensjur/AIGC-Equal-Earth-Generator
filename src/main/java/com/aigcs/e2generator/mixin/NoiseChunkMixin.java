package com.aigcs.e2generator.mixin;

import com.aigcs.e2generator.Config;
import com.aigcs.e2generator.core.WorldShape;
import com.aigcs.e2generator.world.MaskedDensity;
import com.aigcs.e2generator.world.SphericalContext;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseRouter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 把 {@code NoiseRouter} 的最终密度包装成按世界形状裁剪的版本。
 * <p>
 * 只影响 {@code finalDensity} 与 {@code initialDensityWithoutJaggedness}；
 * 气候采样（温度、湿度、侵蚀等）完全不动，因此群系分布保持不变。
 */
@Mixin(NoiseChunk.class)
public abstract class NoiseChunkMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;finalDensity()Lnet/minecraft/world/level/levelgen/DensityFunction;"))
    private DensityFunction spherical$maskFinalDensity(NoiseRouter router) {
        DensityFunction delegate = router.finalDensity();
        WorldShape shape = SphericalContext.current();
        return shape == null || !spherical$densityMaskEnabled() ? delegate : new MaskedDensity(delegate, shape);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;initialDensityWithoutJaggedness()Lnet/minecraft/world/level/levelgen/DensityFunction;"))
    private DensityFunction spherical$maskInitialDensity(NoiseRouter router) {
        DensityFunction delegate = router.initialDensityWithoutJaggedness();
        WorldShape shape = SphericalContext.current();
        return shape == null || !spherical$densityMaskEnabled() ? delegate : new MaskedDensity(delegate, shape);
    }

    /** c2me 等模组的退化开关：关闭后仍保留平坦海洋、群系/结构/特征过滤与实体包裹。 */
    private static boolean spherical$densityMaskEnabled() {
        try {
            return Config.DENSITY_MASK.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }
}
