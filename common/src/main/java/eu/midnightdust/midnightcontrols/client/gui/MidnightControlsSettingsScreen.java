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
import eu.midnightdust.midnightcontrols.MidnightControlsConstants;
import eu.midnightdust.midnightcontrols.client.MidnightControlsClient;
import eu.midnightdust.midnightcontrols.client.util.platform.NetworkUtil;
import org.thinkingstudio.obsidianui.background.Background;
import org.thinkingstudio.obsidianui.mixin.DrawContextAccessor;
import org.thinkingstudio.obsidianui.widget.SpruceWidget;
import eu.midnightdust.lib.util.MidnightColorUtil;
import eu.midnightdust.midnightcontrols.MidnightControls;
import eu.midnightdust.midnightcontrols.client.MidnightControlsConfig;
import eu.midnightdust.midnightcontrols.client.controller.Controller;
import eu.midnightdust.midnightcontrols.client.gui.widget.ControllerControlsWidget;
import org.thinkingstudio.obsidianui.Position;
import org.thinkingstudio.obsidianui.SpruceTexts;
import org.thinkingstudio.obsidianui.option.*;
import org.thinkingstudio.obsidianui.screen.SpruceScreen;
import org.thinkingstudio.obsidianui.widget.AbstractSpruceWidget;
import org.thinkingstudio.obsidianui.widget.SpruceLabelWidget;
import org.thinkingstudio.obsidianui.widget.container.SpruceContainerWidget;
import org.thinkingstudio.obsidianui.widget.container.SpruceOptionListWidget;
import org.thinkingstudio.obsidianui.widget.container.tabbed.SpruceTabbedWidget;
import eu.midnightdust.midnightcontrols.packet.ControlsModePayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

/**
 * Represents the midnightcontrols settings screen.
 */
public class MidnightControlsSettingsScreen extends SpruceScreen {
    private static final Text SDL2_GAMEPAD_TOOL = Text.literal("SDL2 Gamepad Tool").formatted(Formatting.GREEN);
    public static final String GAMEPAD_TOOL_URL = "https://generalarcade.com/gamepadtool/";
    private final Screen parent;
    // General options
    private final SpruceOption inputModeOption;
    private final SpruceOption autoSwitchModeOption;
    private final SpruceOption rotationSpeedOption;
    private final SpruceOption yAxisRotationSpeedOption;
    private final SpruceOption mouseSpeedOption;
    private final SpruceOption joystickAsMouseOption;
    private final SpruceOption eyeTrackingAsMouseOption;
    private final SpruceOption eyeTrackingDeadzone;
    private final SpruceOption virtualMouseOption;
    private final SpruceOption hideCursorOption;
    private final SpruceOption resetOption;
    private final SpruceOption advancedConfigOption;
    // Gameplay options
    private final SpruceOption analogMovementOption;
    private final SpruceOption doubleTapToSprintOption;
    private final SpruceOption autoJumpOption;
    private final SpruceOption controllerToggleSneakOption;
    private final SpruceOption controllerToggleSprintOption;
    private final SpruceOption fastBlockPlacingOption;
    private final SpruceOption frontBlockPlacingOption;
    private final SpruceOption verticalReacharoundOption;
    private final SpruceOption flyDriftingOption;
    private final SpruceOption flyVerticalDriftingOption;
    // Appearance options
    private final SpruceOption controllerTypeOption;
    private final SpruceOption virtualMouseSkinOption;
    private final SpruceOption hudEnableOption;
    private final SpruceOption hudSideOption;
    private final SpruceOption moveChatOption;
    // Controller options
    private final SpruceOption controllerOption =
            new SpruceCyclingOption("midnightcontrols.menu.controller",
                    amount -> {
                        int id = MidnightControlsConfig.getController().id();
                        id += amount;
                        if (id > GLFW.GLFW_JOYSTICK_LAST)
                            id = GLFW.GLFW_JOYSTICK_1;
                        id = searchNextAvailableController(id, false);
                        MidnightControlsConfig.setController(Controller.byId(id));
                        if (MidnightControlsConfig.debug) System.out.println(Controller.byId(id).getName() + "'s Controller GUID: " + Controller.byId(id).getGuid());
                    },
                    option -> {
                        var controller = MidnightControlsConfig.getController();
                        var controllerName = controller.getName();
                        if (!controller.isConnected())
                            return option.getDisplayText(Text.literal(controllerName).formatted(Formatting.RED));
                        else if (!controller.isGamepad())
                            return option.getDisplayText(Text.literal(controllerName).formatted(Formatting.GOLD));
                        else
                            return option.getDisplayText(Text.literal(controllerName));
                    }, null);
    private final SpruceOption secondControllerOption = new SpruceCyclingOption("midnightcontrols.menu.controller2",
            amount -> {
                int id = MidnightControlsConfig.getSecondController().map(Controller::id).orElse(-1);
                id += amount;
                if (id > GLFW.GLFW_JOYSTICK_LAST)
                    id = -1;
                id = searchNextAvailableController(id, true);
                MidnightControlsConfig.setSecondController(id == -1 ? null : Controller.byId(id));
            },
            option -> MidnightControlsConfig.getSecondController().map(controller -> {
                var controllerName = controller.getName();
                if (!controller.isConnected())
                    return option.getDisplayText(Text.literal(controllerName).formatted(Formatting.RED));
                else if (!controller.isGamepad())
                    return option.getDisplayText(Text.literal(controllerName).formatted(Formatting.GOLD));
                else
                    return option.getDisplayText(Text.literal(controllerName));
            }).orElse(option.getDisplayText(SpruceTexts.OPTIONS_OFF.copyContentOnly().formatted(Formatting.RED))),
            Text.translatable("midnightcontrols.menu.controller2.tooltip"));
    private final SpruceOption unfocusedInputOption;
    private final SpruceOption invertsRightXAxis;
    private final SpruceOption invertsRightYAxis;
    private final SpruceOption cameraModeOption;
    private final SpruceOption toggleControllerProfileOption;
    private final SpruceOption rightDeadZoneOption;
    private final SpruceOption leftDeadZoneOption;
    private final SpruceOption[] maxAnalogValueOptions = new SpruceOption[]{
            maxAnalogValueOption("midnightcontrols.menu.max_left_x_value", GLFW.GLFW_GAMEPAD_AXIS_LEFT_X),
            maxAnalogValueOption("midnightcontrols.menu.max_left_y_value", GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y),
            maxAnalogValueOption("midnightcontrols.menu.max_right_x_value", GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X),
            maxAnalogValueOption("midnightcontrols.menu.max_right_y_value", GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y)
    };

    private static SpruceOption maxAnalogValueOption(String key, int axis) {
        return new SpruceDoubleOption(key, .25f, 1.f, 0.05f,
                () -> MidnightControlsConfig.getAxisMaxValue(axis),
                newValue -> MidnightControlsConfig.setAxisMaxValue(axis, newValue),
                option -> option.getDisplayText(Text.literal(String.format("%.2f", option.get()))),
                Text.translatable(key.concat(".tooltip"))
        );
    }
    // Touch options
    private final SpruceOption touchWithControllerOption;
    private final SpruceOption touchSpeedOption;
    private final SpruceOption touchBreakDelayOption;
    private final SpruceOption invertTouchOption;
    private final SpruceOption touchTransparencyOption;
    private final SpruceOption touchModeOption;

    private final MutableText controllerMappingsUrlText = Text.literal("(")
            .append(Text.literal(GAMEPAD_TOOL_URL).formatted(Formatting.GOLD))
            .append("),");

    private static int searchNextAvailableController(int newId, boolean allowNone) {
        if ((allowNone && newId == -1) || newId == 0) return newId;

        Controller candidate = Controller.byId(newId);
        boolean connected = candidate.isConnected();
        if (MidnightControlsConfig.excludedControllers.stream().anyMatch(exclusion -> candidate.getName().matches(exclusion))) connected = false;
        if (!connected) {
            newId++;
        }

        if (newId > GLFW.GLFW_JOYSTICK_LAST)
            newId = allowNone ? -1 : GLFW.GLFW_JOYSTICK_1;

        return connected ? newId : searchNextAvailableController(newId, allowNone);
    }

    public MidnightControlsSettingsScreen(Screen parent, boolean hideControls) {
        super(Text.translatable("midnightcontrols.title.settings"));
        MidnightControlsConfig.isEditing = true;
        this.parent = parent;
        // General options
        this.inputModeOption = new SpruceCyclingOption("midnightcontrols.menu.controls_mode",
                amount -> {
                    var next = MidnightControlsConfig.controlsMode.next();
                    MidnightControlsConfig.controlsMode = next;
                    MidnightControlsConfig.save();

                    if (this.client != null && this.client.player != null) {
                        NetworkUtil.sendPayloadC2S(new ControlsModePayload(next.getName()));
                    }
                }, option -> option.getDisplayText(Text.translatable(MidnightControlsConfig.controlsMode.getTranslationKey())),
                Text.translatable("midnightcontrols.menu.controls_mode.tooltip"));
        this.autoSwitchModeOption = new SpruceToggleBooleanOption("midnightcontrols.menu.auto_switch_mode", () -> MidnightControlsConfig.autoSwitchMode,
                value -> MidnightControlsConfig.autoSwitchMode = value, Text.translatable("midnightcontrols.menu.auto_switch_mode.tooltip"));
        this.rotationSpeedOption = new SpruceDoubleOption("midnightcontrols.menu.rotation_speed", 0.0, 100.0, .5f,
                () -> MidnightControlsConfig.rotationSpeed,
                value -> MidnightControlsConfig.rotationSpeed = value, option -> option.getDisplayText(Text.literal(String.valueOf(option.get()))),
                Text.translatable("midnightcontrols.menu.rotation_speed.tooltip"));
        this.yAxisRotationSpeedOption = new SpruceDoubleOption("midnightcontrols.menu.y_axis_rotation_speed", 0.0, 100.0, .5f,
                () -> MidnightControlsConfig.yAxisRotationSpeed,
                value -> MidnightControlsConfig.yAxisRotationSpeed = value, option -> option.getDisplayText(Text.literal(String.valueOf(option.get()))),
                Text.translatable("midnightcontrols.menu.y_axis_rotation_speed.tooltip"));
        this.mouseSpeedOption = new SpruceDoubleOption("midnightcontrols.menu.mouse_speed", 0.0, 150.0, .5f,
                () -> MidnightControlsConfig.mouseSpeed,
                value -> MidnightControlsConfig.mouseSpeed = value, option -> option.getDisplayText(Text.literal(String.valueOf(option.get()))),
                Text.translatable("midnightcontrols.menu.mouse_speed.tooltip"));
        this.joystickAsMouseOption = new SpruceToggleBooleanOption("midnightcontrols.menu.joystick_as_mouse",
                () -> MidnightControlsConfig.joystickAsMouse, value -> MidnightControlsConfig.joystickAsMouse = value,
                Text.translatable("midnightcontrols.menu.joystick_as_mouse.tooltip"));
        this.eyeTrackingAsMouseOption = new SpruceToggleBooleanOption("midnightcontrols.menu.eye_tracker_as_mouse",
                () -> MidnightControlsConfig.eyeTrackerAsMouse, value -> MidnightControlsConfig.eyeTrackerAsMouse = value,
                Text.translatable("midnightcontrols.menu.eye_tracker_as_mouse.tooltip"));
        this.eyeTrackingDeadzone = new SpruceDoubleInputOption("midnightcontrols.menu.eye_tracker_deadzone",
                () -> MidnightControlsConfig.eyeTrackerDeadzone, value -> MidnightControlsConfig.eyeTrackerDeadzone = value,
                Text.translatable("midnightcontrols.menu.eye_tracker_deadzone.tooltip"));
        this.resetOption = SpruceSimpleActionOption.reset(btn -> {
            MidnightControlsConfig.reset();
            var client = MinecraftClient.getInstance();
            this.init(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        });
        this.advancedConfigOption = SpruceSimpleActionOption.of("midnightcontrols.midnightconfig.title", button -> client.setScreen(MidnightControlsConfig.getScreen(this, MidnightControlsConstants.NAMESPACE)));
        // Gameplay options
        this.analogMovementOption = new SpruceToggleBooleanOption("midnightcontrols.menu.analog_movement",
                () -> MidnightControlsConfig.analogMovement, value -> MidnightControlsConfig.analogMovement = value,
                Text.translatable("midnightcontrols.menu.analog_movement.tooltip"));
        this.doubleTapToSprintOption = new SpruceToggleBooleanOption("midnightcontrols.menu.double_tap_to_sprint",
                () -> MidnightControlsConfig.doubleTapToSprint, value -> MidnightControlsConfig.doubleTapToSprint = value,
                Text.translatable("midnightcontrols.menu.double_tap_to_sprint.tooltip"));
        this.autoJumpOption = new SpruceToggleBooleanOption("options.autoJump",
                () -> this.client.options.getAutoJump().getValue(),
                newValue -> this.client.options.getAutoJump().setValue(newValue),
                null);
        this.controllerToggleSneakOption = new SpruceToggleBooleanOption("midnightcontrols.menu.controller_toggle_sneak",
                () -> MidnightControlsConfig.controllerToggleSneak, value -> MidnightControlsConfig.controllerToggleSneak = value,
                null);
        this.controllerToggleSprintOption = new SpruceToggleBooleanOption("midnightcontrols.menu.controller_toggle_sprint",
                () -> MidnightControlsConfig.controllerToggleSprint, value -> MidnightControlsConfig.controllerToggleSprint = value,
                null);
        this.fastBlockPlacingOption = new SpruceToggleBooleanOption("midnightcontrols.menu.fast_block_placing", () -> MidnightControlsConfig.fastBlockPlacing,
                value -> MidnightControlsConfig.fastBlockPlacing = value, Text.translatable("midnightcontrols.menu.fast_block_placing.tooltip"));
        this.frontBlockPlacingOption = new SpruceToggleBooleanOption("midnightcontrols.menu.reacharound.horizontal", () -> MidnightControlsConfig.horizontalReacharound,
                value -> MidnightControlsConfig.horizontalReacharound = value, Text.translatable("midnightcontrols.menu.reacharound.horizontal.tooltip"));
        this.verticalReacharoundOption = new SpruceToggleBooleanOption("midnightcontrols.menu.reacharound.vertical", () -> MidnightControlsConfig.verticalReacharound,
                value -> MidnightControlsConfig.verticalReacharound = value, Text.translatable("midnightcontrols.menu.reacharound.vertical.tooltip"));
        this.flyDriftingOption = new SpruceToggleBooleanOption("midnightcontrols.menu.fly_drifting", () -> MidnightControlsConfig.flyDrifting,
                value -> MidnightControlsConfig.flyDrifting = value, Text.translatable("midnightcontrols.menu.fly_drifting.tooltip"));
        this.flyVerticalDriftingOption = new SpruceToggleBooleanOption("midnightcontrols.menu.fly_drifting_vertical", () -> MidnightControlsConfig.verticalFlyDrifting,
                value -> MidnightControlsConfig.verticalFlyDrifting = value, Text.translatable("midnightcontrols.menu.fly_drifting_vertical.tooltip"));
        // Appearance options
        this.controllerTypeOption = new SpruceCyclingOption("midnightcontrols.menu.controller_type",
                amount -> MidnightControlsConfig.controllerType = MidnightControlsConfig.controllerType.next(),
                option -> option.getDisplayText(MidnightControlsConfig.controllerType.getTranslatedText()),
                Text.translatable("midnightcontrols.menu.controller_type.tooltip"));
        this.virtualMouseSkinOption = new SpruceCyclingOption("midnightcontrols.menu.virtual_mouse.skin",
                amount -> MidnightControlsConfig.virtualMouseSkin = MidnightControlsConfig.virtualMouseSkin.next(),
                option -> option.getDisplayText(MidnightControlsConfig.virtualMouseSkin.getTranslatedText()),
                null);
        this.hudEnableOption = new SpruceToggleBooleanOption("midnightcontrols.menu.hud_enable", () -> MidnightControlsConfig.hudEnable,
                MidnightControlsClient::setHudEnabled, Text.translatable("midnightcontrols.menu.hud_enable.tooltip"));
        this.hudSideOption = new SpruceCyclingOption("midnightcontrols.menu.hud_side",
                amount -> MidnightControlsConfig.hudSide = MidnightControlsConfig.hudSide.next(),
                option -> option.getDisplayText(MidnightControlsConfig.hudSide.getTranslatedText()),
                Text.translatable("midnightcontrols.menu.hud_side.tooltip"));
        this.moveChatOption = new SpruceToggleBooleanOption("midnightcontrols.menu.move_chat", () -> MidnightControlsConfig.moveChat,
                value -> MidnightControlsConfig.moveChat = value, Text.translatable("midnightcontrols.menu.move_chat.tooltip"));
        // Controller options
        this.toggleControllerProfileOption = new SpruceToggleBooleanOption("midnightcontrols.menu.separate_controller_profile", () -> MidnightControlsConfig.controllerBindingProfiles.containsKey(MidnightControlsConfig.getController().getGuid()), value -> {
            if (value) {
                MidnightControlsConfig.controllerBindingProfiles.put(MidnightControlsConfig.getController().getGuid(), MidnightControlsConfig.getBindingsForController());
                MidnightControlsConfig.updateBindingsForController(MidnightControlsConfig.getController());
            } else {
                MidnightControlsConfig.controllerBindingProfiles.remove(MidnightControlsConfig.getController().getGuid());
                MidnightControlsConfig.updateBindingsForController(MidnightControlsConfig.getController());
            }

        }, Text.empty());
        this.cameraModeOption = new SpruceCyclingOption("midnightcontrols.menu.camera_mode",
                amount -> MidnightControlsConfig.cameraMode = MidnightControlsConfig.cameraMode.next(),
                option -> option.getDisplayText(MidnightControlsConfig.cameraMode.getTranslatedText()),
                Text.translatable("midnightcontrols.menu.camera_mode.tooltip"));
        this.rightDeadZoneOption = new SpruceDoubleOption("midnightcontrols.menu.right_dead_zone", 0.05, 1.0, .05f,
                () -> MidnightControlsConfig.rightDeadZone,
                value -> MidnightControlsConfig.rightDeadZone = value, option -> {
            var value = String.valueOf(option.get());
            return option.getDisplayText(Text.literal(value.substring(0, Math.min(value.length(), 5))));
        }, Text.translatable("midnightcontrols.menu.right_dead_zone.tooltip"));
        this.leftDeadZoneOption = new SpruceDoubleOption("midnightcontrols.menu.left_dead_zone", 0.05, 1.0, .05f,
                () -> MidnightControlsConfig.leftDeadZone,
                value -> MidnightControlsConfig.leftDeadZone = value, option -> {
            var value = String.valueOf(option.get());
            return option.getDisplayText(Text.literal(value.substring(0, Math.min(value.length(), 5))));
        }, Text.translatable("midnightcontrols.menu.left_dead_zone.tooltip"));
        this.invertsRightXAxis = new SpruceToggleBooleanOption("midnightcontrols.menu.invert_right_x_axis", () -> MidnightControlsConfig.invertRightXAxis,
                value -> MidnightControlsConfig.invertRightXAxis = value, null);
        this.invertsRightYAxis = new SpruceToggleBooleanOption("midnightcontrols.menu.invert_right_y_axis", () -> MidnightControlsConfig.invertRightYAxis,
                value -> MidnightControlsConfig.invertRightYAxis = value, null);
        this.unfocusedInputOption = new SpruceToggleBooleanOption("midnightcontrols.menu.unfocused_input", () -> MidnightControlsConfig.unfocusedInput,
                value -> MidnightControlsConfig.unfocusedInput = value, Text.translatable("midnightcontrols.menu.unfocused_input.tooltip"));
        this.virtualMouseOption = new SpruceToggleBooleanOption("midnightcontrols.menu.virtual_mouse", () -> MidnightControlsConfig.virtualMouse,
                value -> MidnightControlsConfig.virtualMouse = value, Text.translatable("midnightcontrols.menu.virtual_mouse.tooltip"));
        this.hideCursorOption = new SpruceToggleBooleanOption("midnightcontrols.menu.hide_cursor", () -> MidnightControlsConfig.hideNormalMouse,
                value -> MidnightControlsConfig.hideNormalMouse = value, Text.translatable("midnightcontrols.menu.hide_cursor.tooltip"));
        // Touch options
        this.touchModeOption = new SpruceCyclingOption("midnightcontrols.menu.touch_mode",
                amount -> MidnightControlsConfig.touchMode = MidnightControlsConfig.touchMode.next(),
                option -> option.getDisplayText(MidnightControlsConfig.touchMode.getTranslatedText()),
                Text.translatable("midnightcontrols.menu.touch_mode.tooltip"));
        this.touchWithControllerOption = new SpruceToggleBooleanOption("midnightcontrols.menu.touch_with_controller", () -> MidnightControlsConfig.touchInControllerMode,
                value -> MidnightControlsConfig.touchInControllerMode = value, Text.translatable("midnightcontrols.menu.touch_with_controller.tooltip"));
        this.touchSpeedOption = new SpruceDoubleOption("midnightcontrols.menu.touch_speed", 0.0, 150.0, .5f,
                () -> MidnightControlsConfig.touchSpeed,
                value -> MidnightControlsConfig.touchSpeed = value, option -> option.getDisplayText(Text.literal(String.valueOf(option.get()))),
                Text.translatable("midnightcontrols.menu.touch_speed.tooltip"));
        this.touchBreakDelayOption = new SpruceDoubleOption("midnightcontrols.menu.touch_break_delay", 50, 500, 1f,
                () -> (double) MidnightControlsConfig.touchBreakDelay,
                value -> MidnightControlsConfig.touchBreakDelay = value.intValue(), option -> option.getDisplayText(Text.literal(String.valueOf(option.get()))),
                Text.translatable("midnightcontrols.menu.touch_break_delay.tooltip"));
        this.touchTransparencyOption = new SpruceDoubleOption("midnightcontrols.menu.touch_transparency", 0, 100, 1f,
                () -> (double) MidnightControlsConfig.touchTransparency,
                value -> MidnightControlsConfig.touchTransparency = value.intValue(), option -> option.getDisplayText(Text.literal(String.valueOf(option.get()))),
                Text.translatable("midnightcontrols.menu.touch_break_delay.tooltip"));
        this.invertTouchOption = new SpruceToggleBooleanOption("midnightcontrols.menu.invert_touch", () -> MidnightControlsConfig.invertTouch,
                value -> MidnightControlsConfig.invertTouch = value, Text.translatable("midnightcontrols.menu.invert_touch.tooltip"));
    }

    @Override
    public void removed() {
        MidnightControlsConfig.isEditing = false;
        MidnightControlsConfig.save();
        super.removed();
    }

    @Override
    public void close() {
        MidnightControlsConfig.isEditing = false;
        MidnightControlsConfig.save();
        super.close();
    }

    private int getTextHeight() {
        return (5 + this.textRenderer.fontHeight) * 3 + 5;
    }

    @Override
    protected void init() {
        super.init();

        this.buildTabs();

        this.addDrawableChild(this.resetOption.createWidget(Position.of(this.width / 2 - 155, this.height - 29), 150));
        this.addDrawableChild(ButtonWidget.builder(SpruceTexts.GUI_DONE, btn -> this.client.setScreen(this.parent))
                .dimensions(this.width / 2 - 155 + 160, this.height - 29, 150, 20).build());
    }

    public void buildTabs() {
        var tabs = new SpruceTabbedWidget(Position.of(0, 24), this.width, this.height - 32 - 24,
                null,
                Math.max(116, this.width / 8), 0);
        tabs.getList().setBackground(new MidnightControlsBackground());
        this.addDrawableChild(tabs);

        tabs.addSeparatorEntry(Text.translatable("midnightcontrols.menu.separator.general"));
        tabs.addTabEntry(Text.translatable("midnightcontrols.menu.title.general"), null,
                this::buildGeneralTab);
        tabs.addTabEntry(Text.translatable("midnightcontrols.menu.title.gameplay"), null,
                this::buildGameplayTab);
        tabs.addTabEntry(Text.translatable("midnightcontrols.menu.title.visual"), null,
                this::buildVisualTab);

        tabs.addSeparatorEntry(Text.translatable("options.controls"));
        tabs.addTabEntry(Text.translatable("midnightcontrols.menu.title.controller_controls"), null,
                this::buildControllerControlsTab);

        tabs.addSeparatorEntry(Text.translatable("midnightcontrols.menu.separator.controller"));
        tabs.addTabEntry(Text.translatable("midnightcontrols.menu.title.controller"), null,
                this::buildControllerTab);
        tabs.addTabEntry(Text.translatable("midnightcontrols.menu.title.touch"), null,
                this::buildTouchTab);
        tabs.addTabEntry(Text.translatable("midnightcontrols.menu.title.mappings.string"), null,
                this::buildMappingsStringEditorTab);
    }

    public SpruceOptionListWidget buildGeneralTab(int width, int height) {
        var list = new SpruceOptionListWidget(Position.origin(), width, height);
        list.setBackground(new MidnightControlsBackground(130));
        list.addSingleOptionEntry(this.inputModeOption);
        list.addSingleOptionEntry(this.autoSwitchModeOption);
        list.addSingleOptionEntry(this.rotationSpeedOption);
        list.addSingleOptionEntry(this.yAxisRotationSpeedOption);
        list.addSingleOptionEntry(this.mouseSpeedOption);
        list.addSingleOptionEntry(this.virtualMouseOption);
        list.addSingleOptionEntry(this.hideCursorOption);
        list.addSingleOptionEntry(this.joystickAsMouseOption);
        list.addSingleOptionEntry(this.eyeTrackingAsMouseOption);
        list.addSingleOptionEntry(this.advancedConfigOption);
        return list;
    }

    public SpruceOptionListWidget buildGameplayTab(int width, int height) {
        var list = new SpruceOptionListWidget(Position.origin(), width, height);
        list.setBackground(new MidnightControlsBackground(130));
        list.addSingleOptionEntry(this.analogMovementOption);
        list.addSingleOptionEntry(this.doubleTapToSprintOption);
        list.addSingleOptionEntry(this.controllerToggleSneakOption);
        list.addSingleOptionEntry(this.controllerToggleSprintOption);
        if (MidnightControls.isExtrasLoaded) list.addSingleOptionEntry(this.fastBlockPlacingOption);
        if (MidnightControls.isExtrasLoaded) list.addSingleOptionEntry(this.frontBlockPlacingOption);
        if (MidnightControls.isExtrasLoaded) list.addSingleOptionEntry(this.verticalReacharoundOption);
        if (MidnightControls.isExtrasLoaded) list.addSingleOptionEntry(this.flyDriftingOption);
        if (MidnightControls.isExtrasLoaded) list.addSingleOptionEntry(this.flyVerticalDriftingOption);
        list.addSingleOptionEntry(this.autoJumpOption);
        return list;
    }

    public SpruceOptionListWidget buildVisualTab(int width, int height) {
        var list = new SpruceOptionListWidget(Position.origin(), width, height);
        list.setBackground(new MidnightControlsBackground(130));
        list.addSingleOptionEntry(this.controllerTypeOption);
        list.addSingleOptionEntry(this.virtualMouseSkinOption);
        list.addSingleOptionEntry(new SpruceSeparatorOption("midnightcontrols.menu.title.hud", true, null));
        list.addSingleOptionEntry(this.hudEnableOption);
        list.addSingleOptionEntry(this.hudSideOption);
        list.addSingleOptionEntry(this.moveChatOption);
        return list;
    }

    public ControllerControlsWidget buildControllerControlsTab(int width, int height) {
        return new ControllerControlsWidget(Position.origin(), width, height);
    }

    public AbstractSpruceWidget buildControllerTab(int width, int height) {
        var root = new SpruceContainerWidget(Position.origin(), width, height);

        var aboutMappings1 = new SpruceLabelWidget(Position.of(0, 2),
                Text.translatable("midnightcontrols.controller.mappings.1", SDL2_GAMEPAD_TOOL),
                width, true);

        var gamepadToolUrlLabel = new SpruceLabelWidget(Position.of(0, aboutMappings1.getHeight() + 4),
                this.controllerMappingsUrlText, width,
                label -> Util.getOperatingSystem().open(GAMEPAD_TOOL_URL), true);
        gamepadToolUrlLabel.setTooltip(Text.translatable("chat.link.open"));

        var aboutMappings3 = new SpruceLabelWidget(Position.of(0,
                aboutMappings1.getHeight() + gamepadToolUrlLabel.getHeight() + 6),
                Text.translatable("midnightcontrols.controller.mappings.3", Formatting.GREEN.toString(), Formatting.RESET.toString()),
                width, true);

        int listHeight = height - 8 - aboutMappings1.getHeight() - aboutMappings3.getHeight() - gamepadToolUrlLabel.getHeight();
        var labels = new SpruceContainerWidget(Position.of(0,
                listHeight),
                width, height - listHeight);
        labels.addChild(aboutMappings1);
        labels.addChild(gamepadToolUrlLabel);
        labels.addChild(aboutMappings3);

        var list = new SpruceOptionListWidget(Position.origin(), width, listHeight);
        list.setBackground(new MidnightControlsBackground(130));
        list.addSingleOptionEntry(this.controllerOption);
        list.addSingleOptionEntry(this.secondControllerOption);
        list.addSingleOptionEntry(this.toggleControllerProfileOption);
        list.addSingleOptionEntry(this.unfocusedInputOption);
        list.addSingleOptionEntry(this.cameraModeOption);
        list.addOptionEntry(this.invertsRightXAxis, this.invertsRightYAxis);
        list.addSingleOptionEntry(this.rightDeadZoneOption);
        list.addSingleOptionEntry(this.leftDeadZoneOption);
        for (var option : this.maxAnalogValueOptions) {
            list.addSingleOptionEntry(option);
        }

        root.addChild(list);
        root.addChild(labels);
        return root;
    }
    public SpruceOptionListWidget buildTouchTab(int width, int height) {
        var list = new SpruceOptionListWidget(Position.origin(), width, height);
        list.setBackground(new MidnightControlsBackground(130));
        list.addSingleOptionEntry(this.touchSpeedOption);
        list.addSingleOptionEntry(this.touchWithControllerOption);
        list.addSingleOptionEntry(this.invertTouchOption);
        list.addSingleOptionEntry(new SpruceSeparatorOption("midnightcontrols.menu.title.hud", true, null));
        list.addSingleOptionEntry(this.touchModeOption);
        list.addSingleOptionEntry(this.touchBreakDelayOption);
        list.addSingleOptionEntry(this.touchTransparencyOption);
        return list;
    }

    public SpruceContainerWidget buildMappingsStringEditorTab(int width, int height) {
        return new MappingsStringInputWidget(Position.origin(), width, height);
    }

    @Override
    public void renderTitle(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(this.textRenderer, I18n.translate("midnightcontrols.menu.title"), this.width / 2, 8, 16777215);
    }

    public static class MidnightControlsBackground implements Background {
        private static int transparency = 160;
        public MidnightControlsBackground() {}
        public MidnightControlsBackground(int transparency) {
            MidnightControlsBackground.transparency = transparency;
        }
        @Override
        public void render(DrawContext context, SpruceWidget widget, int vOffset, int mouseX, int mouseY, float delta) {
            fill(context, widget.getX(), widget.getY(), widget.getX() + widget.getWidth(), widget.getY() + widget.getHeight(), Color.black);
        }
        private static void fill(DrawContext context, int x2, int y2, int x1, int y1, Color color) {
            RenderLayer renderLayer = RenderLayer.getGui();
            VertexConsumer vertexConsumer = ((DrawContextAccessor)context).getVertexConsumers().getBuffer(renderLayer);

            float r = (float)(color.getRed()) / 255.0F;
            float g = (float)(color.getGreen()) / 255.0F;
            float b = (float)(color.getBlue()) / 255.0F;
            float t = (float)(transparency) / 255.0F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            vertexConsumer.vertex((float)x1, (float)y2, 0.0F).color(r, g, b, t);
            vertexConsumer.vertex((float)x2, (float)y2, 0.0F).color(r, g, b, t);
            vertexConsumer.vertex((float)x2, (float)y1, 0.0F).color(r, g, b, t);
            vertexConsumer.vertex((float)x1, (float)y1, 0.0F).color(r, g, b, t);
            RenderSystem.disableBlend();
            context.draw();
        }
    }
}
