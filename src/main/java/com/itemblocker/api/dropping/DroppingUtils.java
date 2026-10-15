package com.itemblocker.api.dropping;

import com.itemblocker.api.enums.PickupMode;
import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.keymapping.KeyOptions;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.logging.Logger;

public class DroppingUtils {


    public static ArrayList<ItemStack> tempBlockedItems = new ArrayList<>();

    private static final Minecraft client = Minecraft.getInstance();

    public static CreativeModeTab creativeTab;


    /**
     * Whenever this method is called, it iterates through the client's inventory looking for a blocked item, and drops it through the interaction manager.
     * <p></p>
     * <p>
     * If dropping through the interaction manager fails, the item is dropped manually through {@link net.minecraft.client.player.LocalPlayer#drop(ItemStack, boolean, Prediction)} and a drop packet is sent to the server.
     * <p></p>
     * However, this way is very brittle and sometimes doesn't work as expected.
     */
    public static void iterateInventory() {
        KeyOptions options = ItemBlockerClient.getOptions();

        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        if (options.isTemporaryAllowPressed()) {
            return;
        }

        if (!options.isToggled()) {
            return;
        }

        for (int i = 0; i < player.containerMenu.slots.size(); i++) {
            ItemStack stack = player.containerMenu.getSlot(i).getItem();
            int defaultRange = i < 9 ? i + 36 : i;
            if (!stack.isEmpty() && (BlockedItemsConfig.isBlocked(stack) || tempBlockedItems.contains(stack))) {
                Loggers.CLIENT_LOGGER.info("Dropping blocked item from slot {}: {}", i, stack.getItem());
                if (client.gameMode != null) {
                    try {
                        int amount = getAmountToChange(client.gui.screen());

                        client.gameMode.handleContainerInput(
                                player.containerMenu.containerId,
                                defaultRange + amount,
                                0,
                                ContainerInput.PICKUP,
                                player
                        );

                        client.gameMode.handleContainerInput(
                                player.containerMenu.containerId,
                                -999,
                                0,
                                ContainerInput.PICKUP,
                                player
                        );




                    } catch (Exception e) {
                        Loggers.CLIENT_LOGGER.error("Failed to drop item via interaction manager", e);
                        player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.DROP_ITEM, player.blockPosition(), player.getDirection()));
                        player.drop(stack, false, Prediction.PREDICTED);
                        break;
                    }
                }
            }
        }
    }


    private static int getAmountToChange(Screen screen) {
        int amount = 0;
        if (screen instanceof ContainerScreen) {
            amount = 18;
        }
        if (screen instanceof CraftingScreen) {
            amount = 1;
        }
        if (screen instanceof AnvilScreen) {
            amount = -6;
        }
        if (screen instanceof HopperScreen) {
            amount = -4;
        }
        return amount;
    }

}
