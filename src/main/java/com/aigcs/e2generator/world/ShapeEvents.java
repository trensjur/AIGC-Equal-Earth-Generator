package com.aigcs.e2generator.world;

import java.util.List;
import java.util.Locale;

import com.aigcs.e2generator.Config;
import com.aigcs.e2generator.api.event.PolarPushbackEvent;
import com.aigcs.e2generator.api.event.SeamWrapEvent;
import com.aigcs.e2generator.command.SphereCommand;
import com.aigcs.e2generator.core.WorldShape;
import com.aigcs.e2generator.network.NetworkHandler;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 服务端运行时事件：维度挂载、接缝包裹、经纬度同步、命令注册。
 */
public final class ShapeEvents {

    private ShapeEvents() {
    }

    // ------------------------------------------------------------------
    // 维度加载
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ShapeManager.attach(level);
        }
    }

    // ------------------------------------------------------------------
    // 接缝包裹 / 极线推回
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (entity.isRemoved() || entity.isPassenger() || entity.getRootVehicle() != entity) {
            return;
        }
        if (entity instanceof ServerPlayer player && player.isChangingDimension()) {
            return;
        }

        WorldShape shape = ShapeManager.shapeOf(level);
        if (shape == null) {
            return;
        }

        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        // --- 极线推回 ---
        double poleLimit = shape.playableHalfHeight();
        double poleOffset = z - shape.equatorZ();
        if (Config.POLAR_PUSHBACK.get() && Math.abs(poleOffset) > poleLimit) {
            double clampedZ = shape.equatorZ() + Math.signum(poleOffset) * Math.max(0.0, poleLimit - 1.0);
            PolarPushbackEvent pushback = new PolarPushbackEvent(level, entity, shape, z, clampedZ);
            if (!NeoForge.EVENT_BUS.post(pushback).isCanceled()) {
                teleportTree(entity, x, clampedZ, shape, false);
            }
            return;
        }

        // --- 接缝包裹 ---
        if (!Config.WRAP_ENTITIES.get()) {
            return;
        }
        double seam = shape.seamX(z);
        if (seam <= 1.0e-6) {
            return;
        }
        double overshoot = Math.abs(x) - seam;
        if (overshoot <= shape.meridianGrace()) {
            return;
        }
        // 周期为 2 * seam 的环绕，一次映射即可处理任意深度的越界。
        double period = seam * 2.0;
        double wrappedX = x - period * Math.rint(x / period);
        SeamWrapEvent wrap = new SeamWrapEvent(level, entity, shape, x, wrappedX, z);
        if (!NeoForge.EVENT_BUS.post(wrap).isCanceled()) {
            teleportTree(entity, wrappedX, z, shape, true);
        }
    }

    private static void teleportTree(Entity root, double newX, double newZ, WorldShape shape, boolean crossedSeam) {
        Vec3 movement = root.getKnownMovement();
        double dx = newX - root.getX();
        double dz = newZ - root.getZ();
        List<Entity> passengers = List.copyOf(root.getPassengers());

        root.teleportTo(newX, root.getY(), newZ);
        root.setDeltaMovement(movement);
        root.hurtMarked = true;

        for (Entity passenger : passengers) {
            if (passenger instanceof ServerPlayer player) {
                player.connection.teleport(player.getX() + dx, player.getY(), player.getZ() + dz,
                        player.getYRot(), player.getXRot());
                player.setDeltaMovement(movement);
                player.hurtMarked = true;
            }
        }

        if (crossedSeam && root instanceof ServerPlayer player && Config.ANNOUNCE_CROSSING.get()) {
            player.displayClientMessage(Component.translatable("e2generator.message.crossing",
                    format(shape.latitudeAt(newZ), true),
                    format(shape.longitudeAt(newX, newZ), false)), true);
        }
    }

    private static String format(double degrees, boolean latitude) {
        String hemisphere;
        if (latitude) {
            hemisphere = degrees >= 0.0 ? "N" : "S";
        } else {
            hemisphere = degrees >= 0.0 ? "E" : "W";
        }
        return String.format(Locale.ROOT, "%.1f°%s", Math.abs(degrees), hemisphere);
    }

    // ------------------------------------------------------------------
    // 形状同步
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.syncTo(player);
        }
    }

    // ------------------------------------------------------------------
    // 数据包形状预设（Phase 6）
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ShapePresetManager.INSTANCE);
    }

    // ------------------------------------------------------------------
    // 命令
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SphereCommand.register(event.getDispatcher());
    }
}
