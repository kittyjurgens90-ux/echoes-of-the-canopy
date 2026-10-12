package com.example.echoesofthecanopy.client;

import com.example.echoesofthecanopy.entity.HookRopeEntity;
import com.example.echoesofthecanopy.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/** Draws the hook head as a billboarded item sprite and a rope line from the owner's hand. */
public class HookRopeRenderer extends EntityRenderer<HookRopeEntity> {
    private static final int SEGMENTS = 16;
    private final ItemRenderer itemRenderer;

    public HookRopeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(HookRopeEntity hook, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        // --- hook head
        pose.pushPose();
        pose.scale(0.7F, 0.7F, 0.7F);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        this.itemRenderer.renderStatic(hook.getItem(), ItemDisplayContext.GROUND, light,
                OverlayTexture.NO_OVERLAY, pose, buffer, hook.level(), hook.getId());
        pose.popPose();

        // --- rope (owner may be null for a tick or two after spawn on the client)
        if (hook.getOwner() instanceof Player owner) {
            Vec3 hand = getHandPos(owner, partialTick);
            Vec3 hookPos = hook.getPosition(partialTick);
            float dx = (float) (hand.x - hookPos.x);
            float dy = (float) (hand.y - hookPos.y);
            float dz = (float) (hand.z - hookPos.z);
            float sag = hook.isLatched() ? 0.0F : 0.35F; // taut when latched

            VertexConsumer vc = buffer.getBuffer(RenderType.lineStrip());
            PoseStack.Pose last = pose.last();
            for (int i = 0; i <= SEGMENTS; i++) {
                rope(vc, last, dx, dy, dz, sag, i / (float) SEGMENTS, (i + 1) / (float) SEGMENTS);
            }
        }
        super.render(hook, yaw, partialTick, pose, buffer, light);
    }

    private static void rope(VertexConsumer vc, PoseStack.Pose pose, float dx, float dy, float dz,
                             float sag, float t0, float t1) {
        float x0 = dx * t0, y0 = dy * t0 - sag * Mth.sin(t0 * Mth.PI), z0 = dz * t0;
        float x1 = dx * t1, y1 = dy * t1 - sag * Mth.sin(t1 * Mth.PI), z1 = dz * t1;
        float nx = x1 - x0, ny = y1 - y0, nz = z1 - z0;
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-6F) { nx = 0; ny = 1; nz = 0; } else { nx /= len; ny /= len; nz /= len; }
        vc.vertex(pose.pose(), x0, y0, z0).color(110, 78, 40, 255).normal(pose.normal(), nx, ny, nz).endVertex();
    }

    /** Approximation of the vanilla fishing-line hand position (third-person formula, used for first person too). */
    private static Vec3 getHandPos(Player player, float pt) {
        HumanoidArm main = player.getMainArm();
        boolean mainHeld = player.getMainHandItem().is(ModItems.CANOPY_HOOK_ROPE.get());
        int side = (mainHeld ? main : main.getOpposite()) == HumanoidArm.RIGHT ? 1 : -1;
        float body = Mth.lerp(pt, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(body), cos = Mth.cos(body);
        double sideOff = side * 0.35D, fwd = 0.8D;
        double crouch = player.isCrouching() ? -0.1875D : 0.0D;
        return player.getEyePosition(pt).add(-cos * sideOff - sin * fwd, crouch - 0.45D, -sin * sideOff + cos * fwd);
    }

    @Override
    public ResourceLocation getTextureLocation(HookRopeEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
