package nl.enjarai.hoot.registry;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import nl.enjarai.hoot.Hoot;

public class ModTags {
    public static final TagKey<Block> OWLS_SPAWNABLE_ON = TagKey.of(Registries.BLOCK.getKey(), Hoot.id("owls_spawnable_on"));
}
