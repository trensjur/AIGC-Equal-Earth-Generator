package com.aigcs.e2generator.mixin;

import javax.annotation.Nullable;

import com.aigcs.e2generator.api.ShapeAware;
import com.aigcs.e2generator.compat.tfc.TfcShapeBridge;
import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在区块级过滤结构与特征：地图外/缓冲带内不生成结构，也不生成树木、矿脉等装饰。
 * <p>
 * 采用"chunk 级直接取消"而不是"生成后再删除"，避免跨接缝的半个结构，也节省性能。
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    @Inject(method = "createStructures", at = @At("HEAD"), cancellable = true)
    private void spherical$skipStructures(RegistryAccess registryAccess,
            net.minecraft.world.level.chunk.ChunkGeneratorStructureState structureState,
            StructureManager structureManager, ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager, CallbackInfo ci) {
        WorldShape shape = spherical$shape();
        if (shape != null && !shape.allowsStructures(chunk.getPos())) {
            ci.cancel();
        }
    }

    @Inject(method = "applyBiomeDecoration", at = @At("HEAD"), cancellable = true)
    private void spherical$skipDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager,
            CallbackInfo ci) {
        WorldShape shape = spherical$shape();
        if (shape != null && !shape.allowsDecoration(chunk.getPos())) {
            ci.cancel();
        }
    }

    @Nullable
    private WorldShape spherical$shape() {
        if (this instanceof ShapeAware aware) {
            WorldShape shape = aware.spherical$getShape();
            if (shape != null) {
                return shape;
            }
        }
        // TFC 的生成器不实现 ShapeAware，使用可选的 TFC 桥。
        return TfcShapeBridge.shapeForGenerator(this);
    }
}
