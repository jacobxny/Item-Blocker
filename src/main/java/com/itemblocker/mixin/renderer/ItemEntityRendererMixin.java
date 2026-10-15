package com.itemblocker.mixin.renderer;

import com.itemblocker.api.keymapping.KeyHandler;
import com.itemblocker.api.screen.misc.Colors;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.gizmos.ArrowGizmo;
import net.minecraft.gizmos.CuboidGizmo;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("HEAD"))
    public void renderGizmos(ItemEntity entity, ItemEntityRenderState state, float partialTicks, CallbackInfo ci) {

        if (ItemBlockerClient.isDev) {
            Vec3 latestPosition = entity.position();
            Vec3 currentPosition = entity.getPosition(partialTicks);
            Vec3 offset = currentPosition.subtract(latestPosition);
            if (!BlockedItemsConfig.isBlocked(entity.getItem()) || ItemBlockerClient.getOptions().isTemporaryAllowPressed() || !ItemBlockerClient.getOptions().isToggled()) {
                Gizmos.cuboid(entity.getBoundingBox().inflate(1, 0, 1).move(offset), GizmoStyle.stroke(Colors.WHITE));
            }
            else {
                Gizmos.cuboid(entity.getBoundingBox().inflate(1, 0, 1).move(offset), GizmoStyle.strokeAndFill(Colors.WHITE, 1, 0x22FF5555));
            }
            Gizmos.cuboid(entity.getBoundingBox().inflate(0.25, 0, 0.25).move(offset), GizmoStyle.stroke(Colors.GREEN.color(false)));
        }
    }
}
