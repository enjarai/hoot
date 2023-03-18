package nl.enjarai.hoot.entity.ai;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import nl.enjarai.hoot.entity.OwlEntity;

import java.util.EnumSet;

public class TravelToDestinationGoal extends Goal {
    private final OwlEntity entity;
    private final double speed;
    private final double teleportDistance;

    public TravelToDestinationGoal(OwlEntity entity, double speed, double teleportDistance) {
        this.entity = entity;
        this.speed = speed;
        this.teleportDistance = teleportDistance;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return entity.deliveryNavigation.getState() != DeliveryNavigation.State.IDLE &&
                entity.deliveryNavigation.getDestination().isPresent() &&
                entity.deliveryNavigation.getSource().isPresent();
    }

    @Override
    public void start() {

    }

    @Override
    public boolean shouldContinue() {
        return entity.deliveryNavigation.getState() != DeliveryNavigation.State.IDLE &&
                entity.deliveryNavigation.getDestination().isPresent() &&
                entity.deliveryNavigation.getSource().isPresent();
    }

    @Override
    public void stop() {
        entity.getNavigation().stop();
    }

    @Override
    public void tick() {
        var nav = entity.deliveryNavigation;

        if (nav.getDestination().isEmpty() || nav.getSource().isEmpty()) return;

        var destination = nav.getDestination().get();
        var source = nav.getSource().get();

        entity.getNavigation().startMovingTo(
                destination.getX(),
                destination.getY(),
                destination.getZ(),
                speed
        );

        if (entity.squaredDistanceTo(destination.toCenterPos()) < 2 * 2) {
            onDestinationReached();
            return;
        }

        if (entity.squaredDistanceTo(source.toCenterPos()) > teleportDistance * teleportDistance &&
                entity.squaredDistanceTo(destination.toCenterPos()) > (teleportDistance * 2) * (teleportDistance * 2)) {
            var travelVec = source.toCenterPos().subtract(destination.toCenterPos());
            var travelPos = new BlockPos(
                    travelVec.normalize().multiply(teleportDistance).add(destination.toCenterPos()));

            var checkRange = (int) teleportDistance;
            for (var checkPos : BlockPos.iterateOutwards(travelPos, checkRange, checkRange, checkRange)) {
                if (isSafe(entity.world, checkPos)) {
                    entity.teleport(checkPos.getX(), checkPos.getY(), checkPos.getZ());
                    return;
                }
            }

            nav.setState(DeliveryNavigation.State.IDLE);
        }
    }

    private void onDestinationReached() {
        var nav = entity.deliveryNavigation;

        if (nav.getState() == DeliveryNavigation.State.DELIVERING) {
            entity.onDeliver();

            if (entity.getHome() != null) {
                nav.setDestination(entity.getHome());
            } else if (entity.getOwner() != null) {
                nav.setDestination(entity.getOwner().getBlockPos());
            } else if (nav.getSource().isPresent()) {
                nav.setDestination(nav.getSource().get());
            }

            nav.setSource(entity.getBlockPos());
            nav.setState(DeliveryNavigation.State.RETURNING);
        } else if (nav.getState() == DeliveryNavigation.State.RETURNING) {
            entity.onReturn();
            nav.setDestination(null);
            nav.setSource(null);
            nav.setState(DeliveryNavigation.State.IDLE);
        }
        entity.getNavigation().stop();
    }

    public boolean isSafe(BlockView world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.isAir();
    }
}
