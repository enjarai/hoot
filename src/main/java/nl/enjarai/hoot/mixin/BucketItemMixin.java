package nl.enjarai.hoot.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @ModifyExpressionValue(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/BlockPos;offset(Lnet/minecraft/util/math/Direction;)Lnet/minecraft/util/math/BlockPos;"
            )
    )
    private BlockPos captureBlockPos(BlockPos original, @Share("blockPos") LocalRef<BlockPos> blockPosRef) {
        blockPosRef.set(original);
        return original;
    }

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void placeEntityBucketsWithEmptyLiquid(World world, PlayerEntity user, Hand hand,
                                                   CallbackInfoReturnable<TypedActionResult<ItemStack>> cir,
                                                   @Local ItemStack itemStack,
                                                   @Share("blockPos") LocalRef<BlockPos> blockPosRef) {
        if ((Object) this instanceof EntityBucketItem entityBucketItem) {
            entityBucketItem.onEmptied(user, world, itemStack, blockPosRef.get());
            if (user instanceof ServerPlayerEntity serverPlayer) {
                Criteria.PLACED_BLOCK.trigger(serverPlayer, blockPosRef.get(), itemStack);
            }
            user.incrementStat(Stats.USED.getOrCreateStat(entityBucketItem));
            cir.setReturnValue(TypedActionResult.success(BucketItem.getEmptiedStack(itemStack, user), world.isClient()));
        }
    }
}
