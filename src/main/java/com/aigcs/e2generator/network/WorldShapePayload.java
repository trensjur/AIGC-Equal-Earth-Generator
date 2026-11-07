package com.aigcs.e2generator.network;

import javax.annotation.Nullable;

import com.aigcs.e2generator.AIGCsEqualEarthGenerator;
import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 把服务端的世界形状同步给客户端（HUD/地图模组用）。
 * <p>
 * 使用一个可空的 NBT 数据块，方便以后扩展字段而不用改协议版本。
 * 客户端未安装模组时不会收到任何影响；服务端不依赖客户端。
 */
public record WorldShapePayload(@Nullable CompoundTag shape) implements CustomPacketPayload {

    public static final Type<WorldShapePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AIGCsEqualEarthGenerator.MODID, "world_shape"));

    public static final StreamCodec<FriendlyByteBuf, WorldShapePayload> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, WorldShapePayload>() {
        @Override
        public WorldShapePayload decode(FriendlyByteBuf buffer) {
            CompoundTag tag = buffer.readNullable(buf -> buf.readNbt());
            return new WorldShapePayload(tag);
        }

        @Override
        public void encode(FriendlyByteBuf buffer, WorldShapePayload payload) {
            buffer.writeNullable(payload.shape(), (buf, tag) -> buf.writeNbt(tag));
        }
    };

    public static WorldShapePayload of(@Nullable WorldShape shape) {
        return new WorldShapePayload(shape == null ? null : shape.save());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
