package nl.enjarai.hoot.entity.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import nl.enjarai.hoot.entity.OwlEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class WanderNearHomeGoal extends Goal {
    private final OwlEntity owl;
    @Nullable
    private BlockPos target;
    private double x;
    private double y;
    private double z;
    private final double speed;
    private final float maxDistance;

    public WanderNearHomeGoal(OwlEntity owl, double speed, float maxDistance) {
        this.owl = owl;
        this.speed = speed;
        this.maxDistance = maxDistance;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (!owl.isTamed() || owl.isLeashed()) {
            return false;
        }
        if (owl.getHome() == null || !owl.getHome().getDimension().equals(owl.getWorld().getRegistryKey())) {
            return false;
        }
        target = owl.getHome().getPos();
        var squaredDistance = owl.squaredDistanceTo(target.toCenterPos());
        if (squaredDistance < maxDistance * maxDistance || squaredDistance > maxDistance * maxDistance * 4) {
            return false;
        }
//        Vec3d vec3d = NoPenaltyTargeting.findTo(owl, 16, 7, target.toCenterPos(), 1.5);
//        if (vec3d == null) {
//            return false;
//        }
//        x = vec3d.x;
//        y = vec3d.y;
//        z = vec3d.z;
        x = target.getX();
        y = target.getY();
        z = target.getZ();
        return true;
    }

    @Override
    public boolean shouldContinue() {
        if (target != null) {
            return !this.owl.getNavigation().isIdle() && owl.squaredDistanceTo(target.toCenterPos()) > maxDistance * maxDistance;
        }
        return false;
    }

    @Override
    public void stop() {
        target = null;
    }

    @Override
    public void start() {
        owl.getNavigation().startMovingTo(x, y, z, speed);
    }
}
