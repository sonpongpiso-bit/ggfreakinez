package com.vibe.ggez;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * gg freakin ez: safe anchor macro.
 *
 *   2   = place anchor on the block you're looking at (and run the whole combo if AUTO_CHAIN)
 *   R   = charge the anchor + put a glowstone shield between you and it
 *   TAB = swap to the totem slot and detonate
 *
 * All keys are rebindable in Controls. Items are found in your hotbar automatically.
 */
public class GgFreakinEz implements ClientModInitializer {
    /** Run the full combo off the anchor key. */
    static final boolean AUTO_CHAIN = true;
    /** 0 = everything in one tick, 1 = place+charge+shield on tick 0, detonate on tick 1. */
    static final int DETONATE_DELAY_TICKS = 1;

    static KeyMapping ANCHOR, GLOWSTONE, TOTEM;

    static BlockPos anchorPos = null;
    static Direction anchorFace = Direction.UP;
    static int detonateIn = -1;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category cat = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("ggfreakinez", "main"));
        ANCHOR    = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ggfreakinez.anchor", InputConstants.Type.KEYSYM, InputConstants.KEY_2, cat));
        GLOWSTONE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ggfreakinez.glowstone", InputConstants.Type.KEYSYM, InputConstants.KEY_R, cat));
        TOTEM     = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ggfreakinez.totem", InputConstants.Type.KEYSYM, InputConstants.KEY_TAB, cat));

        ClientTickEvents.END_CLIENT_TICK.register(GgFreakinEz::tick);
    }

    static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        MultiPlayerGameMode gm = mc.gameMode;
        if (p == null || gm == null || mc.screen != null) return;

        if (detonateIn >= 0 && --detonateIn < 0) detonate(mc);

        while (ANCHOR.consumeClick()) {
            if (placeAnchor(mc) && AUTO_CHAIN) {
                chargeAndShield(mc);
                if (DETONATE_DELAY_TICKS <= 0) detonate(mc);
                else detonateIn = DETONATE_DELAY_TICKS;
            }
        }
        while (GLOWSTONE.consumeClick()) chargeAndShield(mc);
        while (TOTEM.consumeClick()) detonate(mc);
    }

    // ---- stages -------------------------------------------------------------

    static boolean placeAnchor(Minecraft mc) {
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return false;
        if (!select(mc, Items.RESPAWN_ANCHOR)) return false;
        use(mc, hit);
        anchorPos = hit.getBlockPos().relative(hit.getDirection());
        anchorFace = hit.getDirection();
        return true;
    }

    static void chargeAndShield(Minecraft mc) {
        if (anchorPos == null || !select(mc, Items.GLOWSTONE)) return;
        LocalPlayer p = mc.player;

        // 1) charge the anchor once (any charge makes it explode in the overworld)
        use(mc, new BlockHitResult(Vec3.atCenterOf(anchorPos), Direction.UP, anchorPos, false));

        // 2) shield: glowstone block next to the anchor on YOUR side, placed by clicking the
        //    top of the floor block there (works when the anchor sits on flat ground)
        Direction towardYou = p.getDirection().getOpposite();
        BlockPos floor = anchorPos.below().relative(towardYou);
        use(mc, new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false));
    }

    static void detonate(Minecraft mc) {
        if (anchorPos == null || !select(mc, Items.TOTEM_OF_UNDYING)) return;
        // right-clicking a charged anchor with a non-glowstone item blows it up
        use(mc, new BlockHitResult(Vec3.atCenterOf(anchorPos), Direction.UP, anchorPos, false));
    }

    // ---- helpers ------------------------------------------------------------

    static boolean select(Minecraft mc, Item item) {
        LocalPlayer p = mc.player;
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).is(item)) {
                p.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    static void use(Minecraft mc, BlockHitResult hit) {
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }
}
