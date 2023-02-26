package nl.enjarai.hoot.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.entity.passive.FrogBrain;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class OwlBrain {
    protected static final List<SensorType<? extends Sensor<? super OwlEntity>>> SENSORS = List.of(
            SensorType.NEAREST_PLAYERS
    );
    protected static final List<MemoryModuleType<?>> MEMORY_MODULES = List.of(
            MemoryModuleType.LOOK_TARGET, MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.WALK_TARGET
    );

    public static Brain<OwlEntity> create(Brain<OwlEntity> brain) {
        addCoreActivities(brain);
        addIdleActivities(brain);
        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.resetPossibleActivities();
        return brain;
    }

    private static void addCoreActivities(Brain<FrogEntity> brain) {
        brain.setTaskList(Activity.CORE, 0, ImmutableList.of(
                new WalkTask(2.0f),
                new LookAroundTask(45, 90),
                new WanderAroundTask()
        ));
    }

    private static void addIdleActivities(Brain<FrogEntity> brain) {
        brain.setTaskList(Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, FollowMobTask.create(entity ->, 6.0f, UniformIntProvider.create(30, 60))),
                        Pair.of(0, new BreedTask(EntityType.FROG, 1.0f)), Pair.of(1, new TemptTask(frog -> Float.valueOf(1.25f))),
                        Pair.of(3, WalkTowardsLandTask.create(6, 1.0f)),
                        Pair.of(4, new RandomTask<>(Map.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT), List.of(
                                Pair.of(StrollTask.create(1.0f), 1),
                                Pair.of(GoTowardsLookTargetTask.create(1.0f, 3), 1),
                                Pair.of(TaskTriggerer.predicate(Entity::isOnGround), 2)
                        )))
                ),
                ImmutableSet.of(
                        Pair.of(MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryModuleState.VALUE_ABSENT),
                        Pair.of(MemoryModuleType.IS_IN_WATER, MemoryModuleState.VALUE_ABSENT)
                )
        );
    }
}
