package com.aigcs.e2generator.world;

import java.util.EnumSet;

import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 地图外的"平坦海洋"捷径：不跑噪声，直接铺海床 + 水。
 * <p>
 * 这样做的收益：
 * <ul>
 * <li>地图外区块生成极快；</li>
 * <li>Distant Horizons 看到的是连续海面，而不是虚空或断崖；</li>
 * <li>玩家不会在接缝处看到地形断口。</li>
 * </ul>
 */
public final class OceanChunkFiller {

    private OceanChunkFiller() {
    }

    public static void fill(ChunkAccess chunk, WorldShape shape, int seaLevel) {
        int minY = chunk.getMinBuildHeight();
        int floorY = Math.max(minY, Math.min(shape.oceanFloorY(), seaLevel));

        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < seaLevel; y++) {
                    BlockState state;
                    if (y < floorY) {
                        state = y < 0 ? deepslate : stone;
                    } else {
                        state = water;
                    }
                    setBlock(chunk, x, y, z, state);
                }
            }
        }

        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
    }

    private static void setBlock(ChunkAccess chunk, int x, int y, int z, BlockState state) {
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
        section.setBlockState(x, y & 15, z, state, false);
    }
}
