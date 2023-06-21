package nl.enjarai.hoot;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import nl.enjarai.hoot.registry.ModEntities;
import nl.enjarai.hoot.registry.ModItems;
import nl.enjarai.hoot.registry.ModRegistries;
import nl.enjarai.hoot.registry.ModSoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hoot implements ModInitializer {
	public static final String MOD_ID = "hoot";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModRegistries.init();
		ModSoundEvents.init();
		ModEntities.init();
		ModItems.init();
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}