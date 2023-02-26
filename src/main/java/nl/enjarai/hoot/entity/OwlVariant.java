package nl.enjarai.hoot.entity;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import nl.enjarai.hoot.Hoot;
import nl.enjarai.hoot.registry.ModRegistries;

public record OwlVariant(Identifier texture) {
    public static final RegistryKey<OwlVariant> WOOD_OWL = of("wood_owl");

    private static RegistryKey<OwlVariant> of(String id) {
        return RegistryKey.of(ModRegistries.OWL_VARIANT_KEY, Hoot.id(id));
    }

    public static void register(Registry<OwlVariant> registry) {
        register(registry, WOOD_OWL, "textures/entity/owl/wood_owl.png");
    }

    private static OwlVariant register(Registry<OwlVariant> registry, RegistryKey<OwlVariant> key, String textureId) {
        return Registry.register(registry, key, new OwlVariant(Hoot.id(textureId)));
    }
}
