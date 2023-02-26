package nl.enjarai.hoot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import nl.enjarai.hoot.client.entity.OwlEntityRenderer;
import nl.enjarai.hoot.entity.ModEntities;

public class HootClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.OWL, OwlEntityRenderer::new);
    }
}
