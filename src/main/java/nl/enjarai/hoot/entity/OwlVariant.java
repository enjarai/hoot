package nl.enjarai.hoot.entity;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import nl.enjarai.hoot.Hoot;
import nl.enjarai.hoot.registry.ModRegistries;

public record OwlVariant(Identifier texture) {
    public static final RegistryKey<OwlVariant> WOOD_OWL_KEY = of("wood_owl");
    public static final RegistryKey<OwlVariant> SNOW_OWL_KEY = of("snow_owl");
    public static final RegistryKey<OwlVariant> INTERDIMENSIONAL_OWL_KEY = of("interdimensional_owl");

    public static OwlVariant WOOD_OWL;
    public static OwlVariant SNOW_OWL;
    public static OwlVariant INTERDIMENSIONAL_OWL;

    private static RegistryKey<OwlVariant> of(String id) {
        return RegistryKey.of(ModRegistries.OWL_VARIANT_KEY, Hoot.id(id));
    }

    public static void register(Registry<OwlVariant> registry) {
        WOOD_OWL = register(registry, WOOD_OWL_KEY, "textures/entity/owl/wood_owl.png");
        SNOW_OWL = register(registry, SNOW_OWL_KEY, "textures/entity/owl/snow_owl.png");
        INTERDIMENSIONAL_OWL = register(registry, INTERDIMENSIONAL_OWL_KEY, "textures/entity/owl/interdimensional_owl.png");
    }

    private static OwlVariant register(Registry<OwlVariant> registry, RegistryKey<OwlVariant> key, String textureId) {
        return Registry.register(registry, key, new OwlVariant(Hoot.id(textureId)));
    }

    public static OwlVariant fromBiome(RegistryEntry<Biome> biome, BlockPos pos) {
        return biome.value().getPrecipitation(pos) == Biome.Precipitation.SNOW ? SNOW_OWL : WOOD_OWL;
    }
}
