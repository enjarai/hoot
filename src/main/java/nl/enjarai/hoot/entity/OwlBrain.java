package nl.enjarai.hoot.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.*;
import nl.enjarai.hoot.registry.ModBrainModules;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class OwlBrain {
    protected static final List<SensorType<? extends Sensor<? super OwlEntity>>> SENSORS = List.of(
            SensorType.NEAREST_PLAYERS
    );
    protected static final List<MemoryModuleType<?>> MEMORY_MODULES = List.of(
            MemoryModuleType.LOOK_TARGET, MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.WALK_TARGET,
            ModBrainModules.HOME_LOCATION
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

    private static void addIdleActivities(Brain<OwlEntity> brain) {
        brain.setTaskList(Activity.IDLE,
                ImmutableList.of(
//                        Pair.of(4, new RandomTask<>(Map.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT), List.of(
//                                Pair.of(StrollTask.create(1.0f), 1),
//                                Pair.of(GoTowardsLookTargetTask.create(1.0f, 3), 1)
//                        )))
                )
        );
    }

    public static void updateActivity(OwlEntity owl) {
        owl.getBrain().resetPossibleActivities(ImmutableList.of(
                Activity.IDLE
        ));
    }
}
