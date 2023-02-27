package nl.enjarai.hoot.registry;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.GlobalPos;
import nl.enjarai.hoot.Hoot;

import java.util.Optional;

public class ModBrainModules {
    public static final MemoryModuleType<GlobalPos> HOME_LOCATION = Registry.register(Registries.MEMORY_MODULE_TYPE,
            Hoot.id("home_location"), new MemoryModuleType<>(Optional.of(GlobalPos.CODEC)));

    public static void init() {
    }
}
