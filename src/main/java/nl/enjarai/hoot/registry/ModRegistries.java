package nl.enjarai.hoot.registry;

import com.mojang.serialization.Lifecycle;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import nl.enjarai.hoot.Hoot;
import nl.enjarai.hoot.entity.OwlVariant;

public class ModRegistries {
    public static final RegistryKey<Registry<OwlVariant>> OWL_VARIANT_KEY = RegistryKey.ofRegistry(Hoot.id("owl_variant"));
    public static final Registry<OwlVariant> OWL_VARIANT = new SimpleRegistry<>(OWL_VARIANT_KEY, Lifecycle.stable());

    public static final TrackedDataHandler<OwlVariant> OWL_VARIANT_DATA = TrackedDataHandler.of(OWL_VARIANT);

    public static void init() {
        OwlVariant.register(OWL_VARIANT);
        TrackedDataHandlerRegistry.register(OWL_VARIANT_DATA);
    }
}
