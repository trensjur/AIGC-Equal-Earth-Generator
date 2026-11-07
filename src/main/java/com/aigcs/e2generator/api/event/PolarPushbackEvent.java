package com.aigcs.e2generator.api.event;

import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 实体越过极线、即将被推回可玩区域时触发。
 * <p>
 * 取消事件可以保留实体当前位置（例如自定义的极地玩法）。
 */
public class PolarPushbackEvent extends Event implements ICancellableEvent {

    private final ServerLevel level;
    private final Entity entity;
    private final WorldShape shape;
    private final double oldZ;
    private final double newZ;

    public PolarPushbackEvent(ServerLevel level, Entity entity, WorldShape shape, double oldZ, double newZ) {
        this.level = level;
        this.entity = entity;
        this.shape = shape;
        this.oldZ = oldZ;
        this.newZ = newZ;
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public WorldShape getShape() {
        return this.shape;
    }

    public double getOldZ() {
        return this.oldZ;
    }

    public double getNewZ() {
        return this.newZ;
    }
}
