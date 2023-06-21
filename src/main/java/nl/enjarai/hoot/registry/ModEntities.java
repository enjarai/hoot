package nl.enjarai.hoot.registry;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;
import nl.enjarai.hoot.Hoot;
import nl.enjarai.hoot.entity.OwlEntity;

public class ModEntities {
    public static final EntityType<OwlEntity> OWL = Registry.register(
            Registries.ENTITY_TYPE,
            Hoot.id("owl"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, OwlEntity::new)
                    .dimensions(EntityDimensions.fixed(1.0f / 16.0f * 10.0f, 1.0f))
                    .build()
    );

    public static void init() {
        FabricDefaultAttributeRegistry.register(OWL, OwlEntity.createOwlAttributes());

        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(
                        BiomeKeys.FOREST, BiomeKeys.BIRCH_FOREST, BiomeKeys.DARK_FOREST,
                        BiomeKeys.SNOWY_PLAINS, BiomeKeys.SNOWY_TAIGA, BiomeKeys.SNOWY_SLOPES
                ),
                SpawnGroup.CREATURE, OWL, 5, 1, 2
        );

        SpawnRestriction.register(OWL, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING, OwlEntity::canSpawn);
    }
}
