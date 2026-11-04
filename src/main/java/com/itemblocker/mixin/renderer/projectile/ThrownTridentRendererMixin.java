package com.itemblocker.mixin.renderer.projectile;

import com.itemblocker.api.screen.misc.Colors;
import com.itemblocker.client.ItemBlockerClient;
import com.itemblocker.client.config.BlockedItemsConfig;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.client.renderer.entity.state.ThrownTridentRenderState;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTridentRenderer.class)
public class ThrownTridentRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/projectile/arrow/ThrownTrident;Lnet/minecraft/client/renderer/entity/state/ThrownTridentRenderState;F)V", at = @At("HEAD"))
    public void renderGizmos(ThrownTrident entity, ThrownTridentRenderState state, float partialTicks, CallbackInfo ci) {
        Vec3 latestPosition = entity.position();
        Vec3 currentPosition = entity.getPosition(partialTicks);
        Vec3 offset = currentPosition.subtract(latestPosition);
        if (ItemBlockerClient.isDev) {
            if (entity.getOwner() != null) {
                Gizmos.billboardText("Owner: " + entity.getOwner().getName().getString(), entity.position().add(0, 1, 0), TextGizmo.Style.whiteAndCentered());
            } else {
                Gizmos.billboardText("Owner: None", currentPosition.add(0, 1, 0), TextGizmo.Style.whiteAndCentered());
            }
            if (entity.isInGround()) {
                if (!BlockedItemsConfig.isBlocked(entity.getPickupItem()) || ItemBlockerClient.getOptions().isTemporaryAllowPressed() || !ItemBlockerClient.getOptions().isToggled()) {
                    Gizmos.cuboid(entity.getBoundingBox().inflate(1, -0.1, 1).move(offset), GizmoStyle.stroke(Colors.WHITE));
                } else {
                    Gizmos.cuboid(entity.getBoundingBox().inflate(1, -0.1, 1).move(offset), GizmoStyle.strokeAndFill(Colors.WHITE, 1, 0x22FF5555));
                }
            }
        }
    }
}
