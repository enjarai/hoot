package nl.enjarai.hoot.registry;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import nl.enjarai.hoot.Hoot;

public class ModSoundEvents {
    public static final Identifier ENTITY_OWL_AMBIENT_ID = Hoot.id("entity.owl.ambient");
    public static final Identifier ENTITY_OWL_HURT_ID = Hoot.id("entity.owl.hurt");
    public static final Identifier ENTITY_OWL_DEATH_ID = Hoot.id("entity.owl.death");
    public static final Identifier ENTITY_OWL_EAT_ID = Hoot.id("entity.owl.eat");

    public static final SoundEvent ENTITY_OWL_AMBIENT = SoundEvent.of(ENTITY_OWL_AMBIENT_ID);
    public static final SoundEvent ENTITY_OWL_HURT = SoundEvent.of(ENTITY_OWL_HURT_ID);
    public static final SoundEvent ENTITY_OWL_DEATH = SoundEvent.of(ENTITY_OWL_DEATH_ID);
    public static final SoundEvent ENTITY_OWL_EAT = SoundEvent.of(ENTITY_OWL_EAT_ID);

    public static void init() {
        Registry.register(Registries.SOUND_EVENT, ENTITY_OWL_AMBIENT_ID, ENTITY_OWL_AMBIENT);
        Registry.register(Registries.SOUND_EVENT, ENTITY_OWL_HURT_ID, ENTITY_OWL_HURT);
        Registry.register(Registries.SOUND_EVENT, ENTITY_OWL_DEATH_ID, ENTITY_OWL_DEATH);
        Registry.register(Registries.SOUND_EVENT, ENTITY_OWL_EAT_ID, ENTITY_OWL_EAT);
    }
}
