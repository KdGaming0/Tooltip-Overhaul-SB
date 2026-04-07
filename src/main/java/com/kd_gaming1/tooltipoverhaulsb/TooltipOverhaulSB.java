package com.kd_gaming1.tooltipoverhaulsb;

import com.kd_gaming1.tooltipoverhaulsb.debug.TooltipDataExtractor;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TooltipOverhaulSB implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("template");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello Fabric world!");
        TooltipDataExtractor.register();
    }
}