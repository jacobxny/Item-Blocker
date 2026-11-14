package com.itemblocker.api.dropping;

import com.itemblocker.api.keymapping.KeyOptions;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class DroppingUtils {

    private static final Minecraft client = Minecraft.getInstance();


    public static final int PLAYER_INVENTORY_SLOTS = 41;


    public static boolean isPlayerInventorySlot(Slot slot) {
        LocalPlayer player = client.player;
        return player != null
                && slot.container == player.getInventory()
                && slot.getContainerSlot() < PLAYER_INVENTORY_SLOTS;
    }

    /**
     * Whenever this method is called, it goes through the slots of the currently open menu, finds the ones that
     * belong to the player's inventory and holds a blocked item, and drops that item through the interaction
     * manager (pick the stack up, then click outside the window to throw it).
     * <p>
     * Each slot is addressed by its own {@code slot.index} in the open menu, so the correct item is dropped on
     * every screen without any per-screen offsets. Blocked items sitting in a container (chest, hopper, ...) are
     * left alone.
     * <p>
     * Does nothing while the creative inventory screen is open, since it handles slots differently.
     */
    public static void iterateInventory() {
        KeyOptions options = ItemBlockerClient.getOptions();

        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            return;
        }

        if (options.isTemporaryAllowPressed() || !options.isToggled()) {
            return;
        }

        if (client.gui.screen() instanceof CreativeModeInventoryScreen) {
            return;
        }

        for (Slot slot : player.containerMenu.slots) {
            if (!isPlayerInventorySlot(slot)) {
                continue;
            }

            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !BlockedItemsConfig.isBlocked(stack)) {
                continue;
            }

            Loggers.CLIENT_LOGGER.info("Dropping blocked item from slot {}: {}", slot.index, stack.getItem());
            try {
                dropSlot(player, slot);
            } catch (Exception e) {
                Loggers.CLIENT_LOGGER.error("Failed to drop item via interaction manager", e);
                return;
            }
        }
    }

    private static void dropSlot(LocalPlayer player, Slot slot) {
        int containerId = player.containerMenu.containerId;

        client.gameMode.handleContainerInput(containerId, slot.index, 0, ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(containerId, -999, 0, ContainerInput.PICKUP, player);
    }

}