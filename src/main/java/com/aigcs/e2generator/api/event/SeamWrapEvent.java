package com.aigcs.e2generator.api.event;

import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 实体即将因为越过接缝而被包裹（传送）到地图另一侧时触发。
 * <p>
 * 取消事件可以阻止本次包裹（例如把自己的载具/传送逻辑接管过去）。
 * 事件在服务端主线程触发，{@link #getEntity()} 是<b>根载具</b>，
 * 其乘客会跟随一起传送。
 */
public class SeamWrapEvent extends Event implements ICancellableEvent {

    private final ServerLevel level;
    private final Entity entity;
    private final WorldShape shape;
    private final double oldX;
    private final double newX;
    private final double z;

    public SeamWrapEvent(ServerLevel level, Entity entity, WorldShape shape,
            double oldX, double newX, double z) {
        this.level = level;
        this.entity = entity;
        this.shape = shape;
        this.oldX = oldX;
        this.newX = newX;
        this.z = z;
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

    public double getOldX() {
        return this.oldX;
    }

    public double getNewX() {
        return this.newX;
    }

    public double getZ() {
        return this.z;
    }
}
