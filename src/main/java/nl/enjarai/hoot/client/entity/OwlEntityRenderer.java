package nl.enjarai.hoot.client.entity;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import nl.enjarai.hoot.entity.OwlEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class OwlEntityRenderer extends GeoEntityRenderer<OwlEntity> {
    public static final Identifier COLLAR_TEXTURE = new Identifier("hoot", "textures/entity/owl/collar.png");
    public static final Identifier HOME_COLLAR_TEXTURE = new Identifier("hoot", "textures/entity/owl/home_collar.png");

    public OwlEntityRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new OwlEntityModel());
        addRenderLayer(new CollarGeoLayer<>(this) {
            @Override
            protected Identifier getTextureResource(OwlEntity animatable) {
                return COLLAR_TEXTURE;
            }

            @Override
            protected float[] getColor(OwlEntity animatable) {
                return animatable.getCollarColor().getColorComponents();
            }

            @Override
            protected boolean shouldRender(OwlEntity animatable) {
                return animatable.isTamed();
            }
        });
//        addRenderLayer(new CollarGeoLayer<>(this) {
//            @Override
//            protected Identifier getTextureResource(OwlEntity animatable) {
//                return HOME_COLLAR_TEXTURE;
//            }
//
//            @Override
//            protected boolean shouldRender(OwlEntity animatable) {
//                return animatable.isTamed() && animatable.getHome() != null;
//            }
//        });
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, OwlEntity animatable) {
                if (bone.getName().equals("held_item")) {
                    return animatable.getEquippedStack(EquipmentSlot.MAINHAND);
                }
                return null;
            }

            @Override
            protected ModelTransformation.Mode getTransformTypeForStack(GeoBone bone, ItemStack stack, OwlEntity animatable) {
                return ModelTransformation.Mode.GROUND;
            }

            @Override
            protected void renderStackForBone(MatrixStack matrices, GeoBone bone, ItemStack stack, OwlEntity animatable, VertexConsumerProvider bufferSource, float partialTick, int packedLight, int packedOverlay) {
                matrices.translate(0, -0.15, 0);
                matrices.scale(0.7f, 0.7f, 0.7f);

                super.renderStackForBone(matrices, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            }
        });
    }

    // Fix for leash rendering, owl will be white when leashed otherwise
    @Override
    public <E extends Entity, M extends MobEntity> void renderLeash(M mob, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, E leashHolder) {
        super.renderLeash(mob, partialTick, poseStack, bufferSource, leashHolder);

        bufferSource.getBuffer(RenderLayer.getEntityCutoutNoCull(getTextureLocation(getAnimatable())));
    }

    @Override
    public void render(OwlEntity entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        poseStack.push();
        if (entity.isBaby()) {
            poseStack.scale(0.5f, 0.5f, 0.5f);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.pop();
    }
}
