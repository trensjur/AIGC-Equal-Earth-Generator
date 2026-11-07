package com.aigcs.e2generator.client;

import com.aigcs.e2generator.core.WorldShape;
import com.aigcs.e2generator.network.WorldShapePayload;

import net.minecraft.nbt.CompoundTag;

/**
 * 客户端接收形状同步的入口。
 */
public final class ClientShapeSync {

    private ClientShapeSync() {
    }

    public static void accept(WorldShapePayload payload) {
        CompoundTag tag = payload.shape();
        ClientWorldShape.set(tag == null ? null : WorldShape.load(tag));
    }
}
