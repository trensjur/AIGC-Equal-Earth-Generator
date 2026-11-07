package com.aigcs.e2generator.api.event;

import javax.annotation.Nullable;

import com.aigcs.e2generator.core.WorldShape;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 新世界的 {@link WorldShape} 刚刚生成时触发（在写入 {@code SavedData} 之前）。
 * <p>
 * 整合包或其它模组可以：
 * <ul>
 * <li>调用 {@link #setShape(WorldShape)} 完全替换形状；</li>
 * <li>取消事件以创建一个<b>没有</b>球形形状的存档（等价于对本存档关闭模组）。</li>
 * </ul>
 * 事件在服务端主线程触发，触发于 {@code WorldShapeData} 首次创建时，
 * 此时世界尚未生成任何区块。
 */
public class WorldShapeCreatedEvent extends Event implements ICancellableEvent {

    public enum Source {
        /** 由单独的配置项生成。 */
        CONFIG,
        /** 由世界生成控制器里的星球参数生成。 */
        PLANET_SETTINGS,
        /** 由数据包/内置形状预设生成。 */
        PRESET,
        /** 其它来源（例如 API 直接创建）。 */
        UNKNOWN
    }

    @Nullable
    private final MinecraftServer server;
    @Nullable
    private WorldShape shape;
    private final Source source;

    public WorldShapeCreatedEvent(@Nullable MinecraftServer server, @Nullable WorldShape shape, Source source) {
        this.server = server;
        this.shape = shape;
        this.source = source == null ? Source.UNKNOWN : source;
    }

    @Nullable
    public MinecraftServer getServer() {
        return this.server;
    }

    @Nullable
    public WorldShape getShape() {
        return this.shape;
    }

    /** 替换新世界的形状；传入 {@code null} 等价于取消。 */
    public void setShape(@Nullable WorldShape shape) {
        this.shape = shape;
    }

    public Source getSource() {
        return this.source;
    }
}
