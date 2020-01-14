/*
 * Copyright © 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of LambdaControls.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package me.lambdaurora.lambdacontrols.client.mixin;

import com.mojang.authlib.GameProfile;
import me.lambdaurora.lambdacontrols.client.LambdaControlsClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects the anti fly drifting feature.
 */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity
{
    private boolean lambdacontrols_drifting_prevented = false;

    @Shadow
    protected abstract boolean hasMovementInput();

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile)
    {
        super(world, profile);
    }

    @Inject(method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"))
    public void lambdacontrols_move(MovementType type, Vec3d movement, CallbackInfo ci)
    {
        LambdaControlsClient mod = LambdaControlsClient.get();
        if (type == MovementType.SELF) {
            if (this.abilities.flying && !mod.config.has_fly_drifting()) {
                if (!this.hasMovementInput()) {
                    if (!this.lambdacontrols_drifting_prevented) {
                        this.setVelocity(this.getVelocity().multiply(0, 1.0, 0));
                    }
                    this.lambdacontrols_drifting_prevented = true;
                } else
                    this.lambdacontrols_drifting_prevented = false;
            }
        }
    }
}
