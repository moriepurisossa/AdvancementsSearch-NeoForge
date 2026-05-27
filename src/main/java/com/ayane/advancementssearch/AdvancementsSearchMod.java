package com.ayane.advancementssearch;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(AdvancementsSearchMod.MODID)
public class AdvancementsSearchMod {

    public static final String MODID = "advancementssearch_neoforge";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation ADVANCEMENTS_SEARCH_ID =
            ResourceLocation.fromNamespaceAndPath(MODID, MODID + "/root");

    public AdvancementsSearchMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("AdvancementsSearch [NeoForge] initialized.");
    }

    public static boolean isSearch(AdvancementNode root) {
        return root != null && ADVANCEMENTS_SEARCH_ID.equals(root.holder().id());
    }
}
