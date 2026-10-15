package com.itemblocker.mixin;

import com.itemblocker.api.dropping.DroppingUtils;
import com.itemblocker.api.logging.Loggers;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import com.itemblocker.api.item.ItemUtil;
import com.itemblocker.api.keymapping.KeyOptions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Debug(export = true)
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {


    @Unique
    private static final Minecraft client = Minecraft.getInstance();

    @Unique
    private static ItemStack stack;

    @Unique
    private static final KeyOptions options = ItemBlockerClient.getOptions();


    @Inject(method = "handleTakeItemEntity", at = @At("HEAD"))
    public void isFromGround(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
        ItemUtil.isItemFromGround = true;

        Entity entity = client.level.getEntity(packet.getItemId());
        if (entity instanceof AbstractArrow arrow) {
            stack = arrow.getPickupItem();
            ItemUtil.setCurrentItemInMemory(stack);
        }
        if (entity instanceof ItemEntity itemEntity) {
            stack = itemEntity.getItem();
            ItemUtil.setCurrentItemInMemory(stack);
        }
    }

    @ModifyArg(method = "handleTakeItemEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;add(Lnet/minecraft/client/particle/Particle;)V"))
    public Particle setParticle(Particle particle) {
        if (!options.isToggled()) {
            return particle;
        }
        if (options.isTemporaryAllowPressed()) {
            return particle;
        }
        if (stack != null) {
            if (BlockedItemsConfig.isBlocked(stack)) {
                if (client.player != null && client.level != null) {
                    return new NoRenderParticle(client.level, client.player.getX(), client.player.getY(), client.player.getZ());
                }
            }
        }
        return particle;
    }

    @WrapOperation(method = "handleTakeItemEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V", ordinal = 1))
    public void stopSound(ClientLevel instance, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay, Operation<Void> original) {
        if (!options.isToggled() || options.isTemporaryAllowPressed()) {
            original.call(instance, x, y, z, sound, source, volume, pitch, distanceDelay);
            return;
        }
        if (BlockedItemsConfig.isBlocked(stack)) {
            original.call(instance, x, -200D, z, sound, source, 0f, pitch, distanceDelay);
        }
        else {
            original.call(instance, x, y, z, sound, source, volume, pitch, distanceDelay);
        }
    }
}