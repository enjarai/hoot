package nl.enjarai.hoot.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import nl.enjarai.hoot.registry.ModBrainModules;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class OwlBrain {
    protected static final List<SensorType<? extends Sensor<? super OwlEntity>>> SENSORS = List.of(
            SensorType.NEAREST_PLAYERS, SensorType.NEAREST_LIVING_ENTITIES
    );
    protected static final List<MemoryModuleType<?>> MEMORY_MODULES = List.of(
            MemoryModuleType.LOOK_TARGET, MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.WALK_TARGET,
            ModBrainModules.HOME_LOCATION, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH,
            MemoryModuleType.IS_PANICKING
    );

    public static Brain<OwlEntity> create(Brain<OwlEntity> brain) {
        addCoreActivities(brain);
        addIdleActivities(brain);
        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.resetPossibleActivities();
        return brain;
    }

    private static void addCoreActivities(Brain<OwlEntity> brain) {
        brain.setTaskList(Activity.CORE, 0, ImmutableList.of(
                new StayAboveWaterTask(0.8f),
                new WalkTask(2.5f),
                new LookAroundTask(45, 90),
                new WanderAroundTask()
        ));
    }

    @SuppressWarnings("deprecation")
    private static void addIdleActivities(Brain<OwlEntity> brain) {
        brain.setTaskList(Activity.IDLE, 0,
                ImmutableList.of(
                        FollowMobWithIntervalTask.follow(EntityType.PLAYER, 6.0f, UniformIntProvider.create(120, 240))
                )
        );
    }

    public static void updateActivity(OwlEntity owl) {
        owl.getBrain().resetPossibleActivities(ImmutableList.of(
                Activity.IDLE
        ));
    }
}
