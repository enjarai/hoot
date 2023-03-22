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
    public static final float[] WHITE = new float[]{255, 255, 255};

    public CollarGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    protected RenderLayer getRenderType(T animatable) {
        return RenderLayer.getEntityCutoutNoCull(getTextureResource(animatable));
    }

    protected float[] getColor(T animatable) {
        return WHITE;
    }

    protected boolean shouldRender(T animatable) {
        return true;
    }

    @Override
    public void render(MatrixStack poseStack, T animatable, BakedGeoModel bakedModel, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!shouldRender(animatable)) return;

        RenderLayer layer = getRenderType(animatable);
        float[] color = getColor(animatable);

        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, layer,
                bufferSource.getBuffer(layer), partialTick, 15728640, OverlayTexture.DEFAULT_UV,
                color[0], color[1], color[2], 1);
    }
}
