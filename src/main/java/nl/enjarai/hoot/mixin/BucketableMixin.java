package nl.enjarai.hoot.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import nl.enjarai.hoot.entity.OwlEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Bucketable.class)
public interface BucketableMixin {
    @ModifyExpressionValue(
            method = "tryBucket",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/item/Items;WATER_BUCKET:Lnet/minecraft/item/Item;"
            )
    )
    private static Item useEmptyBucketForOwl(Item original, @Local(argsOnly = true) LivingEntity entity) {
        if (entity instanceof OwlEntity) {
            return Items.BUCKET;
        }

        return original;
    }
}
