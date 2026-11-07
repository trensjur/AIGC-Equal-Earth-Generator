package com.aigcs.e2generator.api.event;

import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.Event;

/**
 * 世界形状已挂载到某个维度的生成器上（{@code LevelEvent.Load} 之后）。
 * <p>
 * 此时形状已经写入生成器/群系源，只用于通知（例如地图模组、调试工具）。
 * 若要修改新世界的形状，请监听 {@link WorldShapeCreatedEvent}。
 */
public class WorldShapeAttachedEvent extends Event {

    private final ServerLevel level;
    private final ChunkGenerator generator;
    private final WorldShape shape;

    public WorldShapeAttachedEvent(ServerLevel level, ChunkGenerator generator, WorldShape shape) {
        this.level = level;
        this.generator = generator;
        this.shape = shape;
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public ChunkGenerator getGenerator() {
        return this.generator;
    }

    public WorldShape getShape() {
        return this.shape;
    }
}
