package com.aigcs.e2generator.network;

import com.aigcs.e2generator.client.ClientShapeSync;
import com.aigcs.e2generator.core.WorldShape;
import com.aigcs.e2generator.world.ShapeManager;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 网络注册与同步入口。
 */
public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    private NetworkHandler() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        // 客户端未安装本模组时也能进服：payload 标记为 optional。
        event.registrar(PROTOCOL_VERSION)
                .optional()
                .playToClient(WorldShapePayload.TYPE, WorldShapePayload.STREAM_CODEC, NetworkHandler::handleShape);
    }

    private static void handleShape(WorldShapePayload payload, IPayloadContext context) {
        // 仅在客户端加载客户端状态类；专用服务端不会执行这里。
        if (FMLEnvironment.dist.isClient()) {
            ClientShapeSync.accept(payload);
        }
    }

    /** 玩家登录/换维度时同步形状。 */
    public static void syncTo(ServerPlayer player) {
        WorldShape shape = ShapeManager.shapeOf(player.serverLevel());
        PacketDistributor.sendToPlayer(player, WorldShapePayload.of(shape));
    }
}
