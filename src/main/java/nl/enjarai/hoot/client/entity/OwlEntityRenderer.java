package nl.enjarai.hoot.client.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import nl.enjarai.hoot.entity.OwlEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OwlEntityRenderer extends GeoEntityRenderer<OwlEntity> {
    public OwlEntityRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new OwlEntityModel());
    }
}
