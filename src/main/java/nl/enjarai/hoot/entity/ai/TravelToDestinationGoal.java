package nl.enjarai.hoot.entity.ai;

import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import nl.enjarai.hoot.entity.OwlEntity;

public class TravelToDestinationGoal extends Goal {
    private final OwlEntity entity;
    private final double speed;
    private final double teleportDistance;

    public TravelToDestinationGoal(OwlEntity entity, double speed, double teleportDistance) {
        this.entity = entity;
        this.speed = speed;
        this.teleportDistance = teleportDistance;
    }

    @Override
    public boolean canStart() {
        return entity.deliveryNavigation.getState() != DeliveryNavigation.State.IDLE;
    }

    @Override
    public void start() {
        entity.getNavigation().startMovingTo(
                entity.deliveryNavigation.getDestination().getX(),
                entity.deliveryNavigation.getDestination().getY(),
                entity.deliveryNavigation.getDestination().getZ(), speed);
    }

    @Override
    public boolean shouldContinue() {
        return entity.getNavigation().isFollowingPath();
    }

    @Override
    public void stop() {
        entity.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (entity.squaredDistanceTo(entity.deliveryNavigation.getSource().toCenterPos()) > teleportDistance) {
            var arrivalPos = entity.deliveryNavigation.getDestination().;
        }
    }

    public boolean isSafe(BlockView world, int maxY) {
        BlockPos blockPos = new BlockPos(this.x, (double)(this.getY(world, maxY) - 1), this.z);
        BlockState blockState = world.getBlockState(blockPos);
        Material material = blockState.getMaterial();
        return blockPos.getY() < maxY && !material.isLiquid() && material != Material.FIRE;
    }
}
