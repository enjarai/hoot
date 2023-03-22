package nl.enjarai.hoot.entity.ai;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.ItemStack;
import nl.enjarai.hoot.entity.OwlEntity;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class ThrowAroundItemGoal extends Goal {
    public static final Predicate<ItemEntity> CAN_TAKE = item -> !item.cannotPickup() && item.isAlive();

    private final OwlEntity entity;
    private int nextThrow;

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
        }
        nextThrow = 0;
    }

    @Override
    public boolean shouldContinue() {
        List<ItemEntity> itemsNear = getItemsNear();
        return !itemsNear.isEmpty() && entity.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty();
    }

    @Override
    public void tick() {
        List<ItemEntity> itemsNear = getItemsNear();
        ItemStack itemStack = entity.getEquippedStack(EquipmentSlot.MAINHAND);
        if (!itemStack.isEmpty()) {
            entity.dropHeldItem();
        } else if (!itemsNear.isEmpty()) {
            entity.getNavigation().startMovingTo(itemsNear.get(0), 1.2f);
            entity.getLookControl().lookAt(itemsNear.get(0));
        }
    }

    @Override
    public void stop() {
        entity.dropHeldItem();
        nextThrow = entity.age + entity.getRandom().nextInt(1200);
    }
}
