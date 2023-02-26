package nl.enjarai.hoot.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import nl.enjarai.hoot.Hoot;

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
    }
}
