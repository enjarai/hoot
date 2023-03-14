package nl.enjarai.hoot.client.entity;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import nl.enjarai.hoot.entity.OwlEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OwlEntityRenderer extends GeoEntityRenderer<OwlEntity> {
    public static final Identifier COLLAR_TEXTURE = new Identifier("hoot", "textures/entity/owl/collar.png");

    public OwlEntityRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new OwlEntityModel());
        addRenderLayer(new CollarGeoLayer<>(this) {
            @Override
            protected Identifier getTextureResource(OwlEntity animatable) {
                return COLLAR_TEXTURE;
            }

            @Nullable
            @Override
            protected DyeColor getColor(OwlEntity animatable) {
                return animatable.isTamed() ? animatable.getCollarColor() : null;
            }
        });
    }

    // Fix for leash rendering, owl will be white when leashed otherwise
    @Override
    public <E extends Entity, M extends MobEntity> void renderLeash(M mob, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, E leashHolder) {
        super.renderLeash(mob, partialTick, poseStack, bufferSource, leashHolder);

        bufferSource.getBuffer(RenderLayer.getEntityCutoutNoCull(getTextureLocation(getAnimatable())));
    }
}
