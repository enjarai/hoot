package nl.enjarai.hoot.entity.ai;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import nl.enjarai.hoot.entity.OwlEntity;
import nl.enjarai.hoot.entity.OwlVariant;

import java.util.EnumSet;
import java.util.Set;

public class TravelToDestinationGoal extends Goal {
    private final OwlEntity entity;
    private final double speed;
    private final double teleportDistance;
    private int ticksUntilForceTeleport;

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
        ticksUntilForceTeleport = 300;
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
        var destinationPos = destination.getPos();
        var source = nav.getSource().get();
        var sourcePos = source.getPos();

        if (entity.isInterdimensional() && !entity.getWorld().getRegistryKey().equals(destination.getDimension())) { // before a teleport
            entity.getNavigation().startMovingTo(entity.getX(), 4096 + 1 + teleportDistance, entity.getZ(), speed); // 4096 = max possible world height, so it will always move upwards
        } else {
            entity.getNavigation().startMovingTo(destinationPos.getX(), destinationPos.getY(), destinationPos.getZ(), speed);
        }

        if (entity.getWorld().getRegistryKey().equals(destination.getDimension()) && entity.squaredDistanceTo(destinationPos.toCenterPos()) < 2 * 2) {
            onDestinationReached();
            return;
        }

        boolean isTeleportingAcrossDimensions = entity.isInterdimensional() &&
                source.getDimension().equals(entity.getWorld().getRegistryKey()) &&
                !destination.getDimension().equals(entity.getWorld().getRegistryKey());

        if (ticksUntilForceTeleport <= 0 || entity.squaredDistanceTo(sourcePos.toCenterPos()) > teleportDistance * teleportDistance &&
                (isTeleportingAcrossDimensions ||
                        entity.squaredDistanceTo(destinationPos.toCenterPos()) > (teleportDistance * 2) * (teleportDistance * 2))) {
            ticksUntilForceTeleport = 300;
            BlockPos travelPos;
            if (isTeleportingAcrossDimensions) {
                travelPos = destinationPos.offset(Direction.UP, (int) teleportDistance);
            } else {
                var travelVec = sourcePos.toCenterPos().subtract(destinationPos.toCenterPos());
                travelPos = BlockPos.ofFloored(travelVec.normalize().multiply(teleportDistance).add(destinationPos.toCenterPos()));
            }

            var checkRange = (int) teleportDistance;
            for (var checkPos : BlockPos.iterateOutwards(travelPos, checkRange, checkRange, checkRange)) {
                if (isSafe(entity.getWorld(), checkPos)) {
                    entity.spawnTeleportParticles(isTeleportingAcrossDimensions);
                    entity.teleport(entity.getWorld().getServer().getWorld(destination.getDimension()),
                            checkPos.getX(),
                            checkPos.getY(),
                            checkPos.getZ(),
                            Set.of(),
                            entity.getYaw(),
                            entity.getPitch());
                    entity.spawnTeleportParticles(isTeleportingAcrossDimensions);
                    entity.getNavigation().recalculatePath();
                    return;
                }
            }

            nav.setState(DeliveryNavigation.State.IDLE);
        }

        ticksUntilForceTeleport--;
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
                nav.setDestination(DeliveryNavigation.entityPos(destinationEntity));
            }
        }
    }

    public boolean isSafe(BlockView world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.isAir();
    }
}
