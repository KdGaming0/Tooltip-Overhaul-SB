package com.kd_gaming1.tooltipoverhaulsb;

import com.kd_gaming1.tooltipoverhaulsb.debug.TooltipDataExtractor;
import com.kd_gaming1.tooltipoverhaulsb.hook.TooltipInterceptHook;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TooltipOverhaulSB implements ClientModInitializer {

    public static final String MOD_ID = "tooltip-overhaul-sb";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[TooltipOverhaulSB] Initialising...");

        MidnightConfig.init(MOD_ID, TooltipOverhaulConfig.class);

        TooltipInterceptHook.register();

        if (TooltipOverhaulConfig.enableDebugExtractor) {
            TooltipDataExtractor.register();
            LOGGER.info("[TooltipOverhaulSB] Debug extractor enabled.");
        }

        LOGGER.info("[TooltipOverhaulSB] Ready. Enabled={}", TooltipOverhaulConfig.enabled);
    }
}