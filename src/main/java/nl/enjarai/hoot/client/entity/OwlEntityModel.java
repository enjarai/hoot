package nl.enjarai.hoot.client.entity;

import net.minecraft.util.Identifier;
import nl.enjarai.hoot.Hoot;
import nl.enjarai.hoot.entity.OwlEntity;
import software.bernie.geckolib.model.GeoModel;

public class OwlEntityModel extends GeoModel<OwlEntity> {
    public static final Identifier MODEL = Hoot.id("geo/owl.geo.json");
    public static final Identifier ANIMATION = Hoot.id("animations/owl.animation.json");

    @Override
    public Identifier getModelResource(OwlEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(OwlEntity animatable) {
        return animatable.getTexture();
    }

    @Override
    public Identifier getAnimationResource(OwlEntity animatable) {
        return ANIMATION;
    }
}
