/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package eu.midnightdust.midnightcontrols.client;

import eu.midnightdust.midnightcontrols.MidnightControlsFeature;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static eu.midnightdust.midnightcontrols.client.MidnightControlsClient.client;

/**
 * Represents the reach-around API of midnightcontrols.
 *
 * @version 1.7.0
 * @since 1.3.2
 */
public class MidnightReacharound {
    private BlockHitResult lastReacharoundResult = null;
    private boolean lastReacharoundVertical = false;
    private boolean onSlab = false;

    public void tick() {
        this.lastReacharoundResult = this.tryVerticalReachAround();
        if (this.lastReacharoundResult == null) {
            this.lastReacharoundResult = this.tryHorizontalReachAround();
            this.lastReacharoundVertical = false;
        } else this.lastReacharoundVertical = true;
    }

    /**
     * Returns the last reach around result.
     *
     * @return the last reach around result
     */
    public @Nullable BlockHitResult getLastReacharoundResult() {
        return this.lastReacharoundResult;
    }

    /**
     * Returns whether the last reach around is vertical.
     *
     * @return {@code true} if the reach around is vertical
     */
    public boolean isLastReacharoundVertical() {
        return this.lastReacharoundVertical;
    }

    /**
     * Returns whether reacharound is available or not.
     *
     * @return {@code true} if reacharound is available, else {@code false}
     */
    public boolean isReacharoundAvailable() {
        return MidnightControlsFeature.HORIZONTAL_REACHAROUND.isAvailable() || MidnightControlsFeature.VERTICAL_REACHAROUND.isAvailable();
    }

    public static float getPlayerRange(@NotNull MinecraftClient client) {
        return client.player != null ? Double.valueOf(client.player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE)).floatValue() : 0.f;
    }

    /**
     * Returns a nullable block hit result if vertical reach-around is possible.
     *
     * @return a block hit result if vertical reach-around is possible, else {@code null}
     */
    public @Nullable BlockHitResult tryVerticalReachAround() {
        if (!MidnightControlsFeature.VERTICAL_REACHAROUND.isAvailable())
            return null;
        if (client.player == null || client.world == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.MISS
                || !client.player.isOnGround() || client.player.getPitch(0.f) < 80.0F
                || client.player.isRiding())
            return null;

        Vec3d pos = client.player.getCameraPosVec(1.0F);
        Vec3d rotationVec = client.player.getRotationVec(1.0F);
        float range = getPlayerRange(client);
        var rayVec = pos.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range).add(0, 0.75, 0);
        var result = client.world.raycast(new RaycastContext(pos, rayVec, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));

        if (result.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = result.getBlockPos().down();
            BlockState state = client.world.getBlockState(blockPos);

            if (client.player.getBlockPos().getY() - blockPos.getY() > 1 && (client.world.isAir(blockPos) || state.isReplaceable())) {
                return new BlockHitResult(result.getPos(), Direction.DOWN, blockPos, false);
            }
        }

        return null;
    }

    /**
     * Returns a nullable block hit result if horizontal reach-around is possible.
     *
     * @return a block hit result if horizontal reach-around is possible
     */
    public @Nullable BlockHitResult tryHorizontalReachAround() {
        if (!MidnightControlsFeature.HORIZONTAL_REACHAROUND.isAvailable())
            return null;

        if (client.world != null && client.player != null && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.MISS
                && client.player.isOnGround() && client.player.getPitch(0.f) >= 35.f) {
            if (client.player.isRiding())
                return null;
            // Temporary pos, do not use
            Vec3d playerPosi = client.player.getPos();

            // Imitates var playerPos = client.player.getBlockPos().down();
            Vec3d playerPos = new Vec3d(playerPosi.getX(), playerPosi.getY() - 1.0, playerPosi.getZ());
            if (client.player.getY() - playerPos.getY() - 1.0 >= 0.25) {
                // Imitates playerPos = playerPos.up();
                playerPos = playerPosi;
                this.onSlab = true;
            } else {
                this.onSlab = false;
            }
            var targetPos = new Vec3d(client.crosshairTarget.getPos().getX(), client.crosshairTarget.getPos().getY(), client.crosshairTarget.getPos().getZ()).subtract(playerPos);
            var vector = new Vec3d(MathHelper.clamp(targetPos.getX(), -1, 1), 0, MathHelper.clamp(targetPos.getZ(), -1, 1));
            var blockPos = playerPos.add(vector);

            // Some functions still need BlockPos, so this is here to let that happen
            var blockyPos = BlockPos.ofFloored(blockPos);

            var direction = client.player.getHorizontalFacing();

            var state = client.world.getBlockState(blockyPos);
            if (!state.isAir())
                return null;
            var adjacentBlockState = client.world.getBlockState(blockyPos.offset(direction.getOpposite()));
            if (adjacentBlockState.isAir() || adjacentBlockState.getBlock() instanceof FluidBlock || (vector.getX() == 0 && vector.getZ() == 0)) {
                return null;
            }

            return new BlockHitResult(blockPos, direction, blockyPos, false);
        }
        return null;
    }

    public @NotNull BlockHitResult withSideForReacharound(@NotNull BlockHitResult result, @Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
            return result;
        return withSideForReacharound(result, Block.getBlockFromItem(stack.getItem()));
    }

    public @NotNull BlockHitResult withSideForReacharound(@NotNull BlockHitResult result, @NotNull Block block) {
        if (block instanceof SlabBlock) {
            if (this.onSlab) result = result.withSide(Direction.UP);
            else result = result.withSide(Direction.DOWN);
        }
        return result;
    }
}
