package nl.enjarai.hoot.entity.ai;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;
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
        DeliveryNavigation nav = entity.deliveryNavigation;
        return nav.getState() != DeliveryNavigation.State.IDLE &&
                (nav.getDestination().isPresent() || nav.getDestinationEntityUUID().isPresent()) &&
                nav.getSource().isPresent();
    }

    @Override
    public void start() {

    }

    @Override
    public boolean shouldContinue() {
        DeliveryNavigation nav = entity.deliveryNavigation;
        return nav.getState() != DeliveryNavigation.State.IDLE &&
                (nav.getDestination().isPresent() || nav.getDestinationEntityUUID().isPresent()) &&
                nav.getSource().isPresent();
    }

    @Override
    public void stop() {
        entity.getNavigation().stop();
    }

    @Override
    public void tick() {
        var nav = entity.deliveryNavigation;

        tryUpdateDestination();
        if (nav.getDestination().isEmpty() || nav.getSource().isEmpty()) return;

        var destination = nav.getDestination().get();
        var source = nav.getSource().get();

        entity.getNavigation().startMovingTo(destination.getX(), destination.getY(), destination.getZ(), speed);

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
                    entity.spawnTeleportParticles();
                    entity.teleport(checkPos.getX(), checkPos.getY(), checkPos.getZ());
                    entity.spawnTeleportParticles();
                    entity.getNavigation().recalculatePath();
                    return;
                }
            }

            nav.setState(DeliveryNavigation.State.IDLE);
        }
    }

    private void onDestinationReached() {
        var nav = entity.deliveryNavigation;

        entity.completeDelivery(true);
        entity.getNavigation().stop();
    }

    public void tryUpdateDestination() {
        var nav = entity.deliveryNavigation;

        if (nav.getDestinationEntityUUID().isPresent() && entity.getWorld() instanceof ServerWorld serverWorld) {
            var destinationEntity = serverWorld.getEntity(nav.getDestinationEntityUUID().get());
            if (destinationEntity != null) {
                nav.setDestination(destinationEntity.getBlockPos());
            }
        }
    }

    public boolean isSafe(BlockView world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.isAir();
    }
}
