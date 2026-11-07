package com.aigcs.e2generator.command;

import java.util.List;
import java.util.Locale;

import com.aigcs.e2generator.core.ShapePreset;
import com.aigcs.e2generator.core.WorldShape;
import com.aigcs.e2generator.world.ShapeManager;
import com.aigcs.e2generator.world.ShapePresetManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /sphere} 命令：
 * <ul>
 * <li>{@code /sphere info} —— 显示形状参数；</li>
 * <li>{@code /sphere coords} —— 当前位置的经纬度；</li>
 * <li>{@code /sphere locate <lat> <lon>} —— 经纬度换算为 x/z；</li>
 * <li>{@code /sphere tp <lat> <lon>} —— 传送到经纬度（需要 OP）；</li>
 * <li>{@code /sphere preset list|info <id>} —— 查看数据包/内置形状预设。</li>
 * </ul>
 */
public final class SphereCommand {

    private SphereCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sphere")
                .then(Commands.literal("info")
                        .executes(SphereCommand::info))
                .then(Commands.literal("coords")
                        .executes(SphereCommand::coords))
                .then(Commands.literal("locate")
                        .then(Commands.argument("latitude", DoubleArgumentType.doubleArg(-90.0, 90.0))
                                .then(Commands.argument("longitude", DoubleArgumentType.doubleArg(-180.0, 180.0))
                                        .executes(SphereCommand::locate))))
                .then(Commands.literal("tp")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("latitude", DoubleArgumentType.doubleArg(-90.0, 90.0))
                                .then(Commands.argument("longitude", DoubleArgumentType.doubleArg(-180.0, 180.0))
                                        .executes(SphereCommand::teleport))))
                .then(Commands.literal("preset")
                        .then(Commands.literal("list")
                                .executes(SphereCommand::presetList))
                        .then(Commands.literal("info")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> {
                                            String remaining = builder.getRemainingLowerCase();
                                            for (ShapePreset preset : ShapePresetManager.all()) {
                                                String id = preset.id().toString();
                                                if (id.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                                                    builder.suggest(id);
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(SphereCommand::presetInfo)))));
    }

    private static WorldShape shape(CommandContext<CommandSourceStack> context) {
        return ShapeManager.shapeOf(context.getSource().getLevel());
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        WorldShape shape = shape(context);
        if (shape == null) {
            context.getSource().sendFailure(Component.translatable("e2generator.command.no_shape"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(shape.describe()), false);
        return 1;
    }

    private static int coords(CommandContext<CommandSourceStack> context) {
        WorldShape shape = shape(context);
        if (shape == null) {
            context.getSource().sendFailure(Component.translatable("e2generator.command.no_shape"));
            return 0;
        }
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            context.getSource().sendFailure(Component.translatable("e2generator.command.player_only"));
            return 0;
        }
        Vec3 position = player.position();
        double latitude = shape.latitudeAt(position.z);
        double longitude = shape.longitudeAt(position.x, position.z);
        context.getSource().sendSuccess(() -> Component.translatable("e2generator.command.coords",
                String.format(java.util.Locale.ROOT, "%.3f", latitude),
                String.format(java.util.Locale.ROOT, "%.3f", longitude)), false);
        return 1;
    }

    private static int locate(CommandContext<CommandSourceStack> context) {
        WorldShape shape = shape(context);
        if (shape == null) {
            context.getSource().sendFailure(Component.translatable("e2generator.command.no_shape"));
            return 0;
        }
        double latitude = DoubleArgumentType.getDouble(context, "latitude");
        double longitude = DoubleArgumentType.getDouble(context, "longitude");
        double z = shape.zAtLatitude(latitude);
        double x = shape.xAtLongitude(longitude, z);
        context.getSource().sendSuccess(() -> Component.translatable("e2generator.command.locate",
                String.format(java.util.Locale.ROOT, "%.1f", x),
                String.format(java.util.Locale.ROOT, "%.1f", z)), false);
        return 1;
    }

    private static int presetList(CommandContext<CommandSourceStack> context) {
        List<ShapePreset> presets = ShapePresetManager.all();
        context.getSource().sendSuccess(() -> Component.translatable(
                "e2generator.command.preset.list", presets.size()), false);
        for (ShapePreset preset : presets) {
            context.getSource().sendSuccess(() -> Component.literal(
                    " - " + preset.id() + " : " + preset.describe()), false);
        }
        return presets.size();
    }

    private static int presetInfo(CommandContext<CommandSourceStack> context) {
        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        ShapePreset preset = ShapePresetManager.get(id);
        if (preset == null) {
            context.getSource().sendFailure(Component.translatable(
                    "e2generator.command.preset.unknown", id.toString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(id + " - " + preset.description()), false);
        context.getSource().sendSuccess(() -> Component.literal(preset.describe()), false);
        return 1;
    }

    private static int teleport(CommandContext<CommandSourceStack> context) {
        WorldShape shape = shape(context);
        if (shape == null) {
            context.getSource().sendFailure(Component.translatable("e2generator.command.no_shape"));
            return 0;
        }
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            context.getSource().sendFailure(Component.translatable("e2generator.command.player_only"));
            return 0;
        }
        double latitude = DoubleArgumentType.getDouble(context, "latitude");
        double longitude = DoubleArgumentType.getDouble(context, "longitude");
        double z = shape.zAtLatitude(latitude);
        double x = shape.xAtLongitude(longitude, z);
        ServerLevel level = player.serverLevel();
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0.0, z));
        player.teleportTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        context.getSource().sendSuccess(() -> Component.translatable("e2generator.command.teleported",
                String.format(java.util.Locale.ROOT, "%.1f", latitude),
                String.format(java.util.Locale.ROOT, "%.1f", longitude)), false);
        return 1;
    }
}
