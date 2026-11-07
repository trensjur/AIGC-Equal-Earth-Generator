package com.aigcs.e2generator.mixin;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.aigcs.e2generator.api.ShapeAware;
import com.aigcs.e2generator.core.WorldShape;
import com.aigcs.e2generator.world.FallbackBiomes;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 地图外统一返回兜底海洋群系。
 * <p>
 * 这段注入是"可取消的 HEAD"，与 TerraBlender 的注入位于同一方法上。
 * 我们的优先级并不改动其它模组：掩码内直接放行，让 TerraBlender 正常选群系。
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin implements ShapeAware {

    @Unique
    @Nullable
    private WorldShape spherical$shape;

    @Override
    @Nullable
    public WorldShape spherical$getShape() {
        return this.spherical$shape;
    }

    @Override
    public void spherical$setShape(@Nullable WorldShape shape) {
        this.spherical$shape = shape;
    }

    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;", at = @At("HEAD"), cancellable = true)
    private void spherical$fallbackBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> cir) {
        WorldShape shape = this.spherical$shape;
        if (shape == null) {
            return;
        }
        // 参数是 quart 坐标，掩码需要方块坐标。
        if (shape.mask(quartX << 2, quartZ << 2) > 0.0) {
            return;
        }
        Holder<Biome> fallback = FallbackBiomes.get(shape);
        if (fallback != null) {
            cir.setReturnValue(fallback);
        }
    }

    @Inject(method = "collectPossibleBiomes", at = @At("RETURN"), cancellable = true)
    private void spherical$collectFallback(CallbackInfoReturnable<Stream<Holder<Biome>>> cir) {
        WorldShape shape = this.spherical$shape;
        if (shape == null) {
            return;
        }
        Holder<Biome> fallback = FallbackBiomes.get(shape);
        if (fallback != null) {
            cir.setReturnValue(Stream.concat(cir.getReturnValue(), Stream.of(fallback)));
        }
    }
}
