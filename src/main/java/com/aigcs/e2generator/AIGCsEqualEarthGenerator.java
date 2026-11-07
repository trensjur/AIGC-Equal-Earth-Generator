package com.aigcs.e2generator;

import org.slf4j.Logger;

import com.aigcs.e2generator.api.WorldgenAdapters;
import com.aigcs.e2generator.compat.OptimizationCompat;
import com.aigcs.e2generator.compat.tfc.TfcWorldgenAdapter;
import com.aigcs.e2generator.network.NetworkHandler;
import com.aigcs.e2generator.world.ShapeEvents;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(AIGCsEqualEarthGenerator.MODID)
public class AIGCsEqualEarthGenerator {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "e2generator";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod
    // is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and
    // pass them in automatically.
    public AIGCsEqualEarthGenerator(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // 网络：世界形状同步
        modEventBus.addListener(NetworkHandler::register);

        // Phase 4/6：可选生成器适配器（TFC）。未安装 TFC 时 supports() 永远为 false。
        WorldgenAdapters.register(TfcWorldgenAdapter.INSTANCE);

        // 服务端配置：世界形状参数（只影响新建世界）
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        // 客户端配置：HUD 显示
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        // 游戏事件：维度加载、接缝包裹、命令注册
        NeoForge.EVENT_BUS.register(ShapeEvents.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("AIGC's Equal Earth Generator loaded (spherical world shape for NeoForge 1.21.1)");
        OptimizationCompat.logStartupStatus();
    }
}
