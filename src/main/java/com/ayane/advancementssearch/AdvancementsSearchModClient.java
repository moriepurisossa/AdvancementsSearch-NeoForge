package com.ayane.advancementssearch;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = AdvancementsSearchMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AdvancementsSearchMod.MODID, value = Dist.CLIENT)
public class AdvancementsSearchModClient {

    public AdvancementsSearchModClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        AdvancementsSearchMod.LOGGER.info("Client setup complete.");
    }
}
