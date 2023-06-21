package nl.enjarai.hoot.entity.ai;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import nl.enjarai.hoot.entity.OwlEntity;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class ThrowAroundItemGoal extends Goal {
    public static final Predicate<ItemEntity> CAN_TAKE = item -> !item.cannotPickup() && item.isAlive();

    private final OwlEntity entity;
    private int nextThrow;
    private int targetItem;

    public ThrowAroundItemGoal(OwlEntity entity) {
        this.entity = entity;
        setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    
    private List<ItemEntity> getItemsNear() {
        return entity.world.getEntitiesByClass(ItemEntity.class,
                entity.getBoundingBox().expand(8.0, 8.0, 8.0), CAN_TAKE);
    }

    @Override
    public boolean canStart() {
        if (nextThrow > entity.age) {
            return false;
        }
        List<ItemEntity> itemsNear = getItemsNear();
        return !itemsNear.isEmpty() && entity.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty();
    }

    @Override
    public void start() {
        List<ItemEntity> itemsNear = getItemsNear();
        if (!itemsNear.isEmpty()) {
            entity.getNavigation().startMovingTo(itemsNear.get(0), 1.2f);
            entity.getLookControl().lookAt(itemsNear.get(0));
            targetItem = itemsNear.get(0).getId();
        }
        nextThrow = 0;
    }

    @Override
    public boolean shouldContinue() {
        var item = entity.getWorld().getEntityById(targetItem);
        return item != null && nextThrow < entity.age && item.squaredDistanceTo(entity) < 16 * 16;
    }

    @Override
    public void tick() {
        var item = entity.getWorld().getEntityById(targetItem);
        if (item != null) {
            entity.getNavigation().startMovingTo(item, 1.2f);
            entity.getLookControl().lookAt(item);

            if (item.squaredDistanceTo(entity) < 1) {
                item.setVelocity(entity.getRandom().nextDouble() * 0.1, 0.2, entity.getRandom().nextDouble() * 0.1);

                nextThrow = entity.age + entity.getRandom().nextInt(1200);
            }
        }
    }

    @Override
    public void stop() {
        nextThrow = entity.age + entity.getRandom().nextInt(1200);
    }
}
