package com.ayane.advancementssearch;

import java.util.Locale;

import com.ayane.advancementssearch.injection.extension.AdvancementsScreenExtension;
import com.ayane.advancementssearch.injection.extension.BetterAdvancementsScreenExtension;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = AdvancementsSearchMod.MODID, value = Dist.CLIENT)
public class AdvancementsSearchCommands {

    private static final String COMMAND_NAME = "advancementssearch";

    private static final SuggestionProvider<CommandSourceStack> SEARCH_BY_SUGGESTIONS = (ctx, builder) -> {
        for (SearchByType type : SearchByType.values()) {
            builder.suggest(type.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> HIGHLIGHT_TYPE_SUGGESTIONS = (ctx, builder) -> {
        for (HighlightType type : HighlightType.values()) {
            builder.suggest(type.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal(COMMAND_NAME)
                .then(Commands.literal("search")
                    .then(Commands.argument("query", StringArgumentType.string())
                        .then(Commands.argument("by", StringArgumentType.word())
                            .suggests(SEARCH_BY_SUGGESTIONS)
                            .then(Commands.argument("autoHighlightSingle", BoolArgumentType.bool())
                                .then(Commands.argument("highlightType", StringArgumentType.word())
                                    .suggests(HIGHLIGHT_TYPE_SUGGESTIONS)
                                    .executes(AdvancementsSearchCommands::executeSearch)
                                )
                            )
                        )
                    )
                )
                .then(Commands.literal("highlight")
                    .then(Commands.argument("advancementId", ResourceLocationArgument.id())
                        .then(Commands.argument("highlightType", StringArgumentType.word())
                            .suggests(HIGHLIGHT_TYPE_SUGGESTIONS)
                            .executes(AdvancementsSearchCommands::executeHighlight)
                        )
                    )
                )
        );
        AdvancementsSearchMod.LOGGER.info("Registered /{} command.", COMMAND_NAME);
    }

    private static int executeSearch(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        String query = StringArgumentType.getString(ctx, "query");
        SearchByType by = SearchByType.map(StringArgumentType.getString(ctx, "by"));
        boolean auto = BoolArgumentType.getBool(ctx, "autoHighlightSingle");
        HighlightType highlightType = HighlightType.map(StringArgumentType.getString(ctx, "highlightType"));

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("ワールドに入ってから実行してください"));
            return 0;
        }

        AdvancementsScreen screen = new AdvancementsScreen(player.connection.getAdvancements());
        mc.setScreen(screen);
        // Better Advancements may intercept setScreen via ScreenEvent.Opening and swap the
        // instance — mc.screen may now be BetterAdvancementsScreen instead of AdvancementsScreen.
        if (mc.screen instanceof AdvancementsScreenExtension ext) {
            ext.advancementssearch$search(query, by, auto, highlightType);
        } else if (mc.screen instanceof BetterAdvancementsScreenExtension baExt) {
            baExt.advancementssearch$ba$search(query, by, auto, highlightType);
        }
        AdvancementsSearchMod.LOGGER.info(
                "[Cmd] search: query=\"{}\", by={}, auto={}, hl={}",
                query, by, auto, highlightType);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeHighlight(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ResourceLocation advancementId = ResourceLocationArgument.getId(ctx, "advancementId");
        HighlightType highlightType = HighlightType.map(StringArgumentType.getString(ctx, "highlightType"));

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("ワールドに入ってから実行してください"));
            return 0;
        }

        AdvancementsScreen screen = new AdvancementsScreen(player.connection.getAdvancements());
        mc.setScreen(screen);
        if (mc.screen instanceof AdvancementsScreenExtension ext) {
            ext.advancementssearch$highlightAdvancement(advancementId, highlightType);
        } else if (mc.screen instanceof BetterAdvancementsScreenExtension baExt) {
            baExt.advancementssearch$ba$highlightAdvancement(advancementId, highlightType);
        }
        AdvancementsSearchMod.LOGGER.info(
                "[Cmd] highlight: id={}, hl={}", advancementId, highlightType);
        return Command.SINGLE_SUCCESS;
    }
}
