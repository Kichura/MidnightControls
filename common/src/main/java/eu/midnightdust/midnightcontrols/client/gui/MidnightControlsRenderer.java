/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package eu.midnightdust.midnightcontrols.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import eu.midnightdust.midnightcontrols.ControlsMode;
import eu.midnightdust.midnightcontrols.client.enums.ControllerType;
import eu.midnightdust.midnightcontrols.client.MidnightControlsClient;
import eu.midnightdust.midnightcontrols.client.MidnightControlsConfig;
import eu.midnightdust.midnightcontrols.client.MidnightInput;
import eu.midnightdust.midnightcontrols.client.compat.MidnightControlsCompat;
import eu.midnightdust.midnightcontrols.client.controller.ButtonBinding;
import eu.midnightdust.midnightcontrols.client.enums.VirtualMouseSkin;
import eu.midnightdust.midnightcontrols.client.mixin.DrawContextAccessor;
import eu.midnightdust.midnightcontrols.client.util.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.Sprite;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;

import static eu.midnightdust.midnightcontrols.MidnightControls.id;

/**
 * Represents the midnightcontrols renderer.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.2.0
 */
public class MidnightControlsRenderer {
    public static final int ICON_SIZE = 20;
    private static final int BUTTON_SIZE = 15;
    private static final int AXIS_SIZE = 18;

    public static int getButtonSize(int button) {
        return switch (button) {
            case -1 -> 0;
            case GLFW.GLFW_GAMEPAD_AXIS_LEFT_X + 100, GLFW.GLFW_GAMEPAD_AXIS_LEFT_X + 200,
                    GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y + 100, GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y + 200,
                    GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X + 100, GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X + 200,
                    GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y + 100, GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y + 200 -> AXIS_SIZE;
            default -> BUTTON_SIZE;
        };
    }

    /**
     * Gets the binding icon width.
     *
     * @param binding the binding
     * @return the width
     */
    public static int getBindingIconWidth(@NotNull ButtonBinding binding) {
        return getBindingIconWidth(binding.getButton());
    }

    /**
     * Gets the binding icon width.
     *
     * @param buttons the buttons
     * @return the width
     */
    public static int getBindingIconWidth(int[] buttons) {
        int width = 0;
        for (int i = 0; i < buttons.length; i++) {
            width += ICON_SIZE;
            if (i + 1 < buttons.length) {
                width += 2;
            }
        }
        return width;
    }

    public static ButtonSize drawButton(DrawContext context, int x, int y, @NotNull ButtonBinding button, @NotNull MinecraftClient client) {
        return drawButton(context, x, y, button.getButton(), client);
    }

    public static ButtonSize drawButton(DrawContext context, int x, int y, int[] buttons, @NotNull MinecraftClient client) {
        int height = 0;
        int length = 0;
        int currentX = x;
        for (int i = 0; i < buttons.length; i++) {
            int btn = buttons[i];
            int size = drawButton(context, currentX, y, btn, client);
            if (size > height)
                height = size;
            length += size;
            if (i + 1 < buttons.length) {
                length += 2;
                currentX = x + length;
            }
        }
        return new ButtonSize(length, height);
    }

    public static int drawButton(DrawContext context, int x, int y, int button, @NotNull MinecraftClient client) {
        boolean second = false;
        if (button == -1)
            return 0;
        else if (button >= 500) {
            button -= 1000;
            second = true;
        }

        int controllerType = MidnightControlsConfig.controllerType == ControllerType.DEFAULT ? MidnightControlsConfig.matchControllerToType().getId() : MidnightControlsConfig.controllerType.getId();
        boolean axis = false;
        int buttonOffset = button * 15;
        switch (button) {
            case 15 -> buttonOffset = 0;
            case 16 -> buttonOffset = 18;
            case 17 -> buttonOffset = 36;
            case 18 -> buttonOffset = 54;
            case GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER -> buttonOffset = 7 * 15;
            case GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER -> buttonOffset = 8 * 15;
            case GLFW.GLFW_GAMEPAD_BUTTON_BACK -> buttonOffset = 4 * 15;
            case GLFW.GLFW_GAMEPAD_BUTTON_START -> buttonOffset = 6 * 15;
            case GLFW.GLFW_GAMEPAD_BUTTON_GUIDE -> buttonOffset = 5 * 15;
            case GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB -> buttonOffset = 15 * 15;
            case GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB -> buttonOffset = 16 * 15;
            case GLFW.GLFW_GAMEPAD_AXIS_LEFT_X + 100 -> {
                buttonOffset = 0;
                axis = true;
            }
            case GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y + 100 -> {
                buttonOffset = 18;
                axis = true;
            }
            case GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X + 100 -> {
                buttonOffset = 2 * 18;
                axis = true;
            }
            case GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y + 100 -> {
                buttonOffset = 3 * 18;
                axis = true;
            }
            case GLFW.GLFW_GAMEPAD_AXIS_LEFT_X + 200 -> {
                buttonOffset = 4 * 18;
                axis = true;
            }
            case GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y + 200 -> {
                buttonOffset = 5 * 18;
                axis = true;
            }
            case GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X + 200 -> {
                buttonOffset = 6 * 18;
                axis = true;
            }
            case GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y + 200 -> {
                buttonOffset = 7 * 18;
                axis = true;
            }
            case GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER + 100, GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER + 200 -> buttonOffset = 9 * 15;
            case GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER + 100, GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER + 200 -> buttonOffset = 10 * 15;
        }

        RenderSystem.disableDepthTest();

        int assetSize = axis || (button >= 15 && button <= 18) ? AXIS_SIZE : BUTTON_SIZE;

        RenderSystem.setShaderColor(1.f, second ? 0.f : 1.f, 1.f, 1.f);
        context.drawTexture(RenderLayer::getGuiTextured, axis ? MidnightControlsClient.CONTROLLER_AXIS : button >= 15 && button <= 19 ? MidnightControlsClient.CONTROLLER_EXPANDED :MidnightControlsClient.CONTROLLER_BUTTONS
                , x + (ICON_SIZE / 2 - assetSize / 2), y + (ICON_SIZE / 2 - assetSize / 2),
                (float) buttonOffset, (float) (controllerType * assetSize),
                assetSize, assetSize,
                256, 256);
        RenderSystem.enableDepthTest();

        return ICON_SIZE;
    }

    public static int drawButtonTip(DrawContext context, int x, int y, @NotNull ButtonBinding button, boolean display, @NotNull MinecraftClient client) {
        return drawButtonTip(context, x, y, button.getButton(), button.getTranslationKey(), display, client);
    }

    public static int drawButtonTip(DrawContext context, int x, int y, int[] button, @NotNull String action, boolean display, @NotNull MinecraftClient client) {
        if (display) {
            int buttonWidth = drawButton(context, x, y, button, client).length();

            var translatedAction = I18n.translate(action);
            int textY = (MidnightControlsRenderer.ICON_SIZE / 2 - client.textRenderer.fontHeight / 2) + 1;

            return context.drawTextWithShadow(client.textRenderer, translatedAction, (x + buttonWidth + 2), (y + textY), 14737632);
        }

        return -10;
    }

    private static int getButtonTipWidth(@NotNull String action, @NotNull TextRenderer textRenderer) {
        return 15 + 5 + textRenderer.getWidth(action);
    }
    public static void renderWaylandCursor(@NotNull DrawContext context, @NotNull MinecraftClient client) {
        if (MidnightControlsConfig.virtualMouse || client.currentScreen == null || MidnightControlsConfig.controlsMode != ControlsMode.CONTROLLER) return;

        float mouseX = (float) client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        float mouseY = (float) client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();

        try {
            Identifier spritePath = MidnightControlsClient.WAYLAND_CURSOR_TEXTURE_LIGHT;
            if (MidnightControlsConfig.virtualMouseSkin == VirtualMouseSkin.DEFAULT_DARK || MidnightControlsConfig.virtualMouseSkin == VirtualMouseSkin.SECOND_DARK)
                spritePath = MidnightControlsClient.WAYLAND_CURSOR_TEXTURE_DARK;
            Sprite sprite = client.getGuiAtlasManager().getSprite(spritePath);
            drawUnalignedTexturedQuad(RenderLayer::getGuiTextured, sprite.getAtlasId(), context, mouseX, mouseX + 8, mouseY, mouseY + 8, 999, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV());
        } catch (IllegalStateException ignored) {}
    }

    public static void renderVirtualCursor(@NotNull DrawContext context, @NotNull MinecraftClient client) {
        if (!MidnightControlsConfig.virtualMouse || (client.currentScreen == null
                || MidnightInput.isScreenInteractive(client.currentScreen)))
            return;

        float mouseX = (float) client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        float mouseY = (float) client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();

        boolean hoverSlot = false;

        if (client.currentScreen instanceof HandledScreenAccessor inventoryScreen) {
            int guiLeft = inventoryScreen.getX();
            int guiTop = inventoryScreen.getY();

            Slot slot = inventoryScreen.midnightcontrols$getSlotAt(mouseX, mouseY);

            if (slot != null) {
                mouseX = guiLeft + slot.x;
                mouseY = guiTop + slot.y;
                hoverSlot = true;
            }
        }

        if (!hoverSlot && client.currentScreen != null) {
            var slot = MidnightControlsCompat.getSlotAt(client.currentScreen, (int) mouseX, (int) mouseY);

            if (slot != null) {
                mouseX = slot.x();
                mouseY = slot.y();
                hoverSlot = true;
            }
        }

        if (!hoverSlot) {
            mouseX -= 8;
            mouseY -= 8;
        }

        try {
            Sprite sprite = client.getGuiAtlasManager().getSprite(id(MidnightControlsConfig.virtualMouseSkin.getSpritePath() + (hoverSlot ? "_slot" : "")));
            drawUnalignedTexturedQuad(RenderLayer::getGuiTextured, sprite.getAtlasId(), context, mouseX, mouseX + 16, mouseY, mouseY + 16, 999, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV());
        } catch (IllegalStateException ignored) {}
    }
    private static void drawUnalignedTexturedQuad(Function<Identifier, RenderLayer> renderLayers, Identifier texture, DrawContext context, float x1, float x2, float y1, float y2, float z, float u1, float u2, float v1, float v2) {
        RenderLayer renderLayer = renderLayers.apply(texture);
        //RenderSystem.setShaderTexture(0, texture);
        Matrix4f matrix4f = context.getMatrices().peek().getPositionMatrix();
        //BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        VertexConsumer vertexConsumer = ((DrawContextAccessor)context).getVertexConsumers().getBuffer(renderLayer);
        vertexConsumer.vertex(matrix4f, x1, y1, z).texture(u1, v1).color(ColorHelper.getWhite(1.0f));
        vertexConsumer.vertex(matrix4f, x1, y2, z).texture(u1, v2).color(ColorHelper.getWhite(1.0f));
        vertexConsumer.vertex(matrix4f, x2, y2, z).texture(u2, v2).color(ColorHelper.getWhite(1.0f));
        vertexConsumer.vertex(matrix4f, x2, y1, z).texture(u2, v1).color(ColorHelper.getWhite(1.0f));
        context.draw();
    }

    public record ButtonSize(int length, int height) {
    }
}
