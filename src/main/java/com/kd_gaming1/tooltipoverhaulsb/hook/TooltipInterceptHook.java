package com.kd_gaming1.tooltipoverhaulsb.hook;

import com.kd_gaming1.tooltipoverhaulsb.TooltipOverhaulConfig;
import com.kd_gaming1.tooltipoverhaulsb.data.SkyBlockItemData;
import com.kd_gaming1.tooltipoverhaulsb.parser.SkyBlockItemParser;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public final class TooltipInterceptHook {

    private static final Logger LOGGER = LoggerFactory.getLogger("TooltipOverhaulSB/Hook");
    private static final SkyBlockItemParser PARSER = new SkyBlockItemParser();
    private static @Nullable CapturedTooltip pendingCapture = null;

    private TooltipInterceptHook() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register(TooltipInterceptHook::onTooltip);
        LOGGER.info("[TooltipInterceptHook] Registered.");
    }

    private static void onTooltip(ItemStack stack, Item.TooltipContext context,
                                  TooltipFlag type, List<Component> lines) {
        pendingCapture = null;
        if (!TooltipOverhaulConfig.enabled || stack.isEmpty()) return;

        List<String> plainLines = lines.stream()
                .map(Component::getString)
                .collect(Collectors.toList());

        try {
            SkyBlockItemData data = PARSER.parse(stack, plainLines);
            if (TooltipOverhaulConfig.requireHypixelId && data.hypixelId() == null) return;
            pendingCapture = new CapturedTooltip(data, stack);
        } catch (Exception e) {
            LOGGER.error("[TooltipInterceptHook] Parse failed for '{}'",
                    stack.getHoverName().getString(), e);
        }
    }

    /** Consume-once: returns captured data then clears it. */
    public static @Nullable CapturedTooltip consumeCaptured() {
        CapturedTooltip result = pendingCapture;
        pendingCapture = null;
        return result;
    }

    public record CapturedTooltip(SkyBlockItemData data, ItemStack stack) {}
}