package com.itemblocker.api.raycasting;

import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.config.BlockedItemsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Collection;


public abstract class RaycastUtils {
    private static final Minecraft client = Minecraft.getInstance();

    private static boolean sendOverlay = true;

    private static final double RAY_DISTANCE = 20.0;

    private static final ArrayList<Block> specialBlockList = new ArrayList<>();


    /**
     * Performs a raycast from the camera, and finds the item ID's
     */

    public static void raycast() {
        Entity camera = client.getCameraEntity();
        if (camera != null) {
            addSpecialBlocksToList();
            float a = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            HitResult blockHit = camera.pick(RAY_DISTANCE, a, false);
            boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;

            if (hitBlock) {
                BlockPos blockPos = ((BlockHitResult) blockHit).getBlockPos();
                BlockState blockState = client.level.getBlockState(blockPos);
                Block block = blockState.getBlock();
                ItemStack itemStack = block.asItem().getDefaultInstance();
                sendOverlay = itemStack != Items.AIR.getDefaultInstance();
                if (specialBlockList.contains(block)) {
                    itemStack = specialBlockCheck(blockState);
                }
                if (sendOverlay) {
                    sendOverlay(itemStack, BlockedItemsConfig.isBlocked(itemStack));
                }

            }
            Loggers.CLIENT_LOGGER.info("Ray distance: {}, Hit block: {}", RAY_DISTANCE, hitBlock);
        }
    }




    public static void sendOverlay(ItemStack itemStack, boolean shouldSave) {
        String visualId = ItemUtil.getItemDetails(itemStack, BlockedItemsConfig.getItemIdMode());
        if (itemStack.getItem() == Items.AIR) {
            return;
        }
        if (shouldSave) {
            client.player.sendOverlayMessage(Component.literal("§e" + visualId + "§r is already in your blocked items list!"));
        } else {
            client.player.sendOverlayMessage(Component.literal("Added§e " + visualId + "§r to your blocked items list!"));
            BlockedItemsConfig.addBlockedItem(itemStack);
        }
    }

    private static ItemStack specialBlockCheck(BlockState blockState) {
        Block block = blockState.getBlock();
        sendOverlay = true;

        if (CaveVines.hasGlowBerries(blockState)) {
            Loggers.CLIENT_LOGGER.info("This block has glow berries");
            return Items.GLOW_BERRIES.getDefaultInstance();
        }

        else if (block == Blocks.KELP_PLANT) {
            return Items.KELP.getDefaultInstance();
        }
        else {
            sendOverlay = false;
            return Items.AIR.getDefaultInstance();
        }
    }


    private static void addSpecialBlocksToList() {
        specialBlockList.add(Blocks.CAVE_VINES);
        specialBlockList.add(Blocks.CAVE_VINES_PLANT);
        specialBlockList.add(Blocks.KELP_PLANT);
    }
}
