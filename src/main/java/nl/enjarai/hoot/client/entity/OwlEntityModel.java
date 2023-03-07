package nl.enjarai.hoot.client.entity;

import net.minecraft.util.Identifier;
import nl.enjarai.hoot.Hoot;
import nl.enjarai.hoot.entity.OwlEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
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

    @Override
    public void setCustomAnimations(OwlEntity animatable, long instanceId, AnimationState<OwlEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        var head = getAnimationProcessor().getBone("head");

        var modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData != null) {
            head.setRotX(modelData.headPitch() * ((float) Math.PI / 180f));
            head.setRotY(modelData.netHeadYaw() * ((float) Math.PI / 180f));
        }
    }
}
