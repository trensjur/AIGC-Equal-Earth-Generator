package com.aigcs.e2generator.world;

import com.aigcs.e2generator.core.WorldShape;
import com.mojang.serialization.MapCodec;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * 把原版密度函数按世界形状掩码裁剪到"地图内的陆地 / 地图外的平坦海洋"。
 * <p>
 * 这里<b>不重映射采样坐标</b>，因此 Terralith / TerraBlender / 数据包密度函数
 * 依旧按原坐标工作，只是在地图外被替换为海洋。
 */
public final class MaskedDensity implements DensityFunction {

    private final DensityFunction delegate;
    private final WorldShape shape;

    public MaskedDensity(DensityFunction delegate, WorldShape shape) {
        this.delegate = delegate;
        this.shape = shape;
    }

    public DensityFunction delegate() {
        return this.delegate;
    }

    public WorldShape shape() {
        return this.shape;
    }

    @Override
    public double compute(FunctionContext context) {
        double mask = SphericalContext.maskAt(context.blockX(), context.blockZ(), this.shape);
        if (mask <= 0.0) {
            return this.shape.oceanDensity(context.blockY());
        }
        return this.shape.blendDensity(mask, this.delegate.compute(context), context.blockY());
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        this.delegate.fillArray(array, contextProvider);
        for (int i = 0; i < array.length; i++) {
            FunctionContext context = contextProvider.forIndex(i);
            double mask = SphericalContext.maskAt(context.blockX(), context.blockZ(), this.shape);
            array[i] = this.shape.blendDensity(mask, array[i], context.blockY());
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new MaskedDensity(this.delegate.mapAll(visitor), this.shape));
    }

    @Override
    public double minValue() {
        return Math.min(this.delegate.minValue(), -1.0);
    }

    @Override
    public double maxValue() {
        return Math.max(this.delegate.maxValue(), 1.0);
    }

    /**
     * 该包装器只存在于运行时，永远不会被序列化到世界生成设置里。
     */
    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KeyDispatchDataCodec.of(MapCodec.unit(this));
    }
}
