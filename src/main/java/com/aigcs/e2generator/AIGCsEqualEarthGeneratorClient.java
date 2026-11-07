package com.aigcs.e2generator;

import com.aigcs.e2generator.client.ClientWorldShape;
import com.aigcs.e2generator.client.SphericalHud;
import com.aigcs.e2generator.client.sky.SkyConfigurationSectionScreen;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = AIGCsEqualEarthGenerator.MODID, dist = Dist.CLIENT)
public class AIGCsEqualEarthGeneratorClient {

    public AIGCsEqualEarthGeneratorClient(IEventBus modEventBus, ModContainer container) {
        // 经纬度 HUD
        modEventBus.addListener((RegisterGuiLayersEvent event) -> event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(AIGCsEqualEarthGenerator.MODID, "lat_lon"),
                new SphericalHud()));

        // 断开连接时清空同步来的形状，避免连到未安装本模组的服务器时显示旧数据。
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientWorldShape.set(null));

        // 内置天空 / 环境资源包：出现在“资源包”界面，首次启动默认启用，可随时关闭。
        modEventBus.addListener((AddPackFindersEvent event) -> {
            AIGCsEqualEarthGenerator.LOGGER.debug("[e2generator] AddPackFindersEvent for {}", event.getPackType());
            if (event.getPackType() == PackType.CLIENT_RESOURCES) {
                event.addPackFinders(
                        ResourceLocation.fromNamespaceAndPath(AIGCsEqualEarthGenerator.MODID, "gameassets"),
                        PackType.CLIENT_RESOURCES,
                        Component.translatable("e2generator.resourcepack.gameassets"),
                        PackSource.DEFAULT,
                        false,
                        Pack.Position.TOP);
            }
        });

        // 客户端配置页：底部增加预设库与重置按钮。
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (minecraft, parent) -> new ConfigurationScreen(container, parent,
                        (screen, type, modConfig, title) -> type == ModConfig.Type.CLIENT
                                ? new SkyConfigurationSectionScreen(screen, type, modConfig, title)
                                : new ConfigurationScreen.ConfigurationSectionScreen(screen, type, modConfig, title)));
    }
}
