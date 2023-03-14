package nl.enjarai.hoot.client.entity;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class CollarGeoLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    public CollarGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    protected RenderLayer getRenderType(T animatable) {
        return RenderLayer.getEntityCutoutNoCull(getTextureResource(animatable));
    }

    @Nullable
    protected DyeColor getColor(T animatable) {
        return DyeColor.RED;
    }

    @Override
    public void render(MatrixStack poseStack, T animatable, BakedGeoModel bakedModel, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        RenderLayer layer = getRenderType(animatable);
        DyeColor color = getColor(animatable);

        if (color == null) return;

        float[] colorC = color.getColorComponents();

        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, layer,
                bufferSource.getBuffer(layer), partialTick, 15728640, OverlayTexture.DEFAULT_UV,
                colorC[0], colorC[1], colorC[2], 1);
    }
}
