package com.aigcs.e2generator.client;

import javax.annotation.Nullable;

import com.aigcs.e2generator.core.WorldShape;

/**
 * 客户端持有的世界形状快照。会话内不会改变，因此无需高频同步。
 */
public final class ClientWorldShape {

    @Nullable
    private static volatile WorldShape shape;

    private ClientWorldShape() {
    }

    public static void set(@Nullable WorldShape newShape) {
        shape = newShape;
    }

    @Nullable
    public static WorldShape get() {
        return shape;
    }
}
