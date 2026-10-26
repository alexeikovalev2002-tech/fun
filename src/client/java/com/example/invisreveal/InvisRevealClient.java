package com.example.invisreveal;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Locale;

public class InvisRevealClient implements ClientModInitializer {
    private static final double RANGE = 100.0;

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.player == null) return;

            MatrixStack matrices = context.matrices();
            VertexConsumerProvider consumers = context.consumers();
            if (matrices == null || consumers == null) return;

            Camera camera = mc.gameRenderer.getCamera();
            Vec3d cam = camera.getCameraPos();
            float tickProgress = mc.getRenderTickCounter().getTickProgress(false);

            for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player || p.isSpectator() || !p.isInvisible()) continue;
                double dist = Math.sqrt(mc.player.squaredDistanceTo(p));
                if (dist > RANGE) continue;

                Vec3d pos = p.getLerpedPos(tickProgress);
                Box box = p.getDimensions(p.getPose()).getBoxAt(pos);

                // белый хитбокс
                matrices.push();
                matrices.translate(-cam.x, -cam.y, -cam.z);
                VertexRendering.drawBox(matrices.peek(), consumers.getBuffer(RenderLayers.lines()),
                        box, 1f, 1f, 1f, 1f);
                matrices.pop();

                // ник + HP
                float hp = p.getHealth() + p.getAbsorptionAmount();
                Text text = Text.literal(p.getName().getString())
                        .append(Text.literal(" "))
                        .append(Text.literal(String.format(Locale.ROOT, "%.1f", hp) + " \u2764")
                                .formatted(Formatting.RED));

                float scale = 0.025f * (float) Math.max(1.0, dist / 8.0);
                TextRenderer font = mc.textRenderer;

                matrices.push();
                matrices.translate(pos.x - cam.x, box.maxY + 0.5 - cam.y, pos.z - cam.z);
                matrices.multiply(camera.getRotation());
                matrices.scale(scale, -scale, scale);
                Matrix4f m = matrices.peek().getPositionMatrix();
                font.draw(text, -font.getWidth(text) / 2f, 0f, 0xFFFFFFFF, false, m,
                        consumers, TextRenderer.TextLayerType.SEE_THROUGH, 0x40000000, 15728880);
                matrices.pop();
            }
        });
    }
}
