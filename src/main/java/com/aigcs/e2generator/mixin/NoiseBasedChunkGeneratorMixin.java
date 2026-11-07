package com.aigcs.e2generator.mixin;

import javax.annotation.Nullable;

import java.util.Optional;
import java.util.OptionalDouble;

import com.aigcs.e2generator.api.PlanetGeography;
import com.aigcs.e2generator.api.PlanetRadiusProvider;
import com.aigcs.e2generator.api.ShapeAware;
import com.aigcs.e2generator.api.SphericalWorldApi;
import com.aigcs.e2generator.core.WorldShape;
import com.aigcs.e2generator.world.OceanChunkFiller;
import com.aigcs.e2generator.world.SphericalContext;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版 {@code NoiseBasedChunkGenerator} 适配：
 * <ul>
 * <li>把形状压入 {@link SphericalContext}，供 {@code NoiseChunk} 包装密度函数；</li>
 * <li>地图外区块走"平坦海洋"捷径；</li>
 * <li>地图外跳过雕刻、表面与基础高度查询。</li>
 * </ul>
 * 注意：这里<b>不</b>修改噪声采样坐标，因此 Terralith / TerraBlender / 数据包完全兼容。
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin implements ShapeAware, PlanetRadiusProvider {

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

    // ------------------------------------------------------------------
    // 公开 API：星球半径查询
    // ------------------------------------------------------------------

    @Override
    public OptionalDouble planetRadiusChunks() {
        WorldShape shape = this.spherical$shape;
        return shape == null ? OptionalDouble.empty() : OptionalDouble.of(SphericalWorldApi.radiusChunksOf(shape));
    }

    @Override
    public OptionalDouble planetRadiusBlocks() {
        WorldShape shape = this.spherical$shape;
        return shape == null ? OptionalDouble.empty() : OptionalDouble.of(SphericalWorldApi.radiusBlocksOf(shape));
    }

    @Override
    public Optional<PlanetGeography> planetGeography() {
        WorldShape shape = this.spherical$shape;
        return shape == null ? Optional.empty() : Optional.of(PlanetGeography.of(shape));
    }

    @Unique
    private NoiseBasedChunkGenerator spherical$self() {
        return (NoiseBasedChunkGenerator) (Object) this;
    }

    // ------------------------------------------------------------------
    // NoiseChunk 上下文
    // ------------------------------------------------------------------

    @Inject(method = "createNoiseChunk", at = @At("HEAD"))
    private void spherical$pushShape(ChunkAccess chunk, StructureManager structureManager, Blender blender,
            RandomState random, CallbackInfoReturnable<NoiseChunk> cir) {
        SphericalContext.push(this.spherical$shape);
    }

    @Inject(method = "createNoiseChunk", at = @At("RETURN"))
    private void spherical$popShape(ChunkAccess chunk, StructureManager structureManager, Blender blender,
            RandomState random, CallbackInfoReturnable<NoiseChunk> cir) {
        SphericalContext.pop();
    }

    // ------------------------------------------------------------------
    // 平坦海洋捷径
    // ------------------------------------------------------------------

    @Inject(method = "fillFromNoise", at = @At("HEAD"), cancellable = true)
    private void spherical$beginFillAndFlatOcean(Blender blender, RandomState randomState,
            StructureManager structureManager, ChunkAccess chunk,
            CallbackInfoReturnable<java.util.concurrent.CompletableFuture<ChunkAccess>> cir) {
        WorldShape shape = this.spherical$shape;
        if (shape == null) {
            return;
        }
        // Phase 5：本区块的 16×16 掩码只计算一次，供密度包装器查表。
        SphericalContext.beginChunk(shape, chunk.getPos());
        if (!shape.isFullyOutside(chunk.getPos())) {
            return;
        }
        // 平坦海洋捷径：跳过噪声，并立即清理线程缓存（取消后 RETURN 注入不会再执行）。
        SphericalContext.endChunk();
        OceanChunkFiller.fill(chunk, shape, this.spherical$self().getSeaLevel());
        cir.setReturnValue(java.util.concurrent.CompletableFuture.completedFuture(chunk));
    }

    @Inject(method = "fillFromNoise", at = @At("RETURN"))
    private void spherical$endFill(Blender blender, RandomState randomState, StructureManager structureManager,
            ChunkAccess chunk, CallbackInfoReturnable<java.util.concurrent.CompletableFuture<ChunkAccess>> cir) {
        // 即使 shape 为 null 也清理，避免线程池复用时残留上一世界的缓存。
        SphericalContext.endChunk();
    }

    @Inject(method = "applyCarvers", at = @At("HEAD"), cancellable = true)
    private void spherical$skipCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
            StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step,
            CallbackInfo ci) {
        WorldShape shape = this.spherical$shape;
        if (shape != null && !shape.intersectsLand(chunk.getPos())) {
            ci.cancel();
        }
    }

    @Inject(method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V", at = @At("HEAD"), cancellable = true)
    private void spherical$skipSurface(WorldGenRegion level, StructureManager structureManager, RandomState random,
            ChunkAccess chunk, CallbackInfo ci) {
        WorldShape shape = this.spherical$shape;
        if (shape != null && !shape.intersectsLand(chunk.getPos())) {
            ci.cancel();
        }
    }

    // ------------------------------------------------------------------
    // 基础高度 / 列查询
    // ------------------------------------------------------------------

    @Inject(method = "getBaseHeight", at = @At("HEAD"), cancellable = true)
    private void spherical$baseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random,
            CallbackInfoReturnable<Integer> cir) {
        WorldShape shape = this.spherical$shape;
        if (shape != null && shape.mask(x, z) <= 0.0) {
            cir.setReturnValue(Math.max(level.getMinBuildHeight(), shape.oceanFloorY()));
        }
    }

    @Inject(method = "getBaseColumn", at = @At("HEAD"), cancellable = true)
    private void spherical$baseColumn(int x, int z, LevelHeightAccessor height, RandomState random,
            CallbackInfoReturnable<NoiseColumn> cir) {
        WorldShape shape = this.spherical$shape;
        if (shape == null || shape.mask(x, z) > 0.0) {
            return;
        }
        int minY = height.getMinBuildHeight();
        int total = height.getHeight();
        int seaLevel = this.spherical$self().getSeaLevel();
        int floorY = Math.max(minY, Math.min(shape.oceanFloorY(), seaLevel));
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState[] states = new BlockState[total];
        for (int i = 0; i < total; i++) {
            int y = minY + i;
            states[i] = y < floorY ? stone : (y < seaLevel ? water : air);
        }
        cir.setReturnValue(new NoiseColumn(minY, states));
    }
}
