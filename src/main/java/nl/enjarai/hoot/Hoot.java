package nl.enjarai.hoot;

import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;
import nl.enjarai.hoot.entity.ModEntities;
import nl.enjarai.hoot.registry.ModBrainModules;
import nl.enjarai.hoot.registry.ModRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hoot implements ModInitializer {
	public static final String MOD_ID = "hoot";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModRegistries.init();
		ModBrainModules.init();
		ModEntities.init();
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}