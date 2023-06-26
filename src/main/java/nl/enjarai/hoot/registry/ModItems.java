package nl.enjarai.hoot.registry;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvents;
import nl.enjarai.hoot.Hoot;

public class ModItems {
    public static final Item OWL_SPAWN_EGG = new SpawnEggItem(
            ModEntities.OWL, 0x643c10, 0xcb9655,
            new FabricItemSettings()
    );
    public static final Item OWL_BUCKET = new EntityBucketItem(
            ModEntities.OWL, Fluids.EMPTY, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
            new FabricItemSettings().maxCount(1)
    );

    public static void init() {
        Registry.register(Registries.ITEM, Hoot.id("owl_spawn_egg"), OWL_SPAWN_EGG);
        Registry.register(Registries.ITEM, Hoot.id("owl_bucket"), OWL_BUCKET);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(content -> {
            content.add(OWL_SPAWN_EGG);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(OWL_BUCKET);
        });
    }
}
