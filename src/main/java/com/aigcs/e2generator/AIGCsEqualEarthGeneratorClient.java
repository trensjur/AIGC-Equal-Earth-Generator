package com.aigcs.e2generator;

import com.aigcs.e2generator.client.ClientWorldShape;
import com.aigcs.e2generator.client.SphericalHud;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

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

        // Allows NeoForge to create a config screen for this mod's configs.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
