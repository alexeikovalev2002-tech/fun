package com.example.invisreveal;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Locale;

public class InvisRevealClient implements ClientModInitializer {
    private static final double RANGE = 100.0;

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            PoseStack pose = context.matrices();
            MultiBufferSource consumers = context.consumers();
            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 cam = camera.position();
            float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

            for (AbstractClientPlayer p : mc.level.players()) {
                if (p == mc.player || p.isSpectator() || !p.isInvisible()) continue;
                double dist = Math.sqrt(mc.player.distanceToSqr(p));
                if (dist > RANGE) continue;

                Vec3 pos = p.getPosition(pt);
                AABB box = p.getDimensions(p.getPose()).makeBoundingBox(pos);

                // белый хитбокс
                pose.pushPose();
                pose.translate(-cam.x, -cam.y, -cam.z);
                ShapeRenderer.renderLineBox(pose, consumers.getBuffer(RenderType.lines()),
                        box, 1f, 1f, 1f, 1f);
                pose.popPose();

                // ник + HP
                float hp = p.getHealth() + p.getAbsorptionAmount();
                Component text = Component.literal(p.getName().getString())
                        .append(Component.literal(" "))
                        .append(Component.literal(String.format(Locale.ROOT, "%.1f", hp) + " ❤")
                                .withStyle(ChatFormatting.RED));

                float scale = 0.025f * (float) Math.max(1.0, dist / 8.0);
                Font font = mc.font;

                pose.pushPose();
                pose.translate(pos.x - cam.x, box.maxY + 0.5 - cam.y, pos.z - cam.z);
                pose.mulPose(camera.rotation());
                pose.scale(scale, -scale, scale);
                Matrix4f m = pose.last().pose();
                font.drawInBatch(text, -font.width(text) / 2f, 0, 0xFFFFFFFF, false, m,
                        consumers, Font.DisplayMode.SEE_THROUGH, 0x40000000, 15728880);
                pose.popPose();
            }
        });
    }
}
