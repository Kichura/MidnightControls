/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package eu.midnightdust.midnightcontrols.client.mixin;

import eu.midnightdust.midnightcontrols.client.gui.MidnightControlsSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects the new controls settings button.
 */
@Mixin(GameOptionsScreen.class)
public abstract class GameOptionsScreenMixin extends Screen {
    @Shadow @Nullable protected OptionListWidget body;
    @Unique TextIconButtonWidget midnightcontrols$button = TextIconButtonWidget.builder(Text.translatable("midnightcontrols.menu.title.controller"), (button -> this.client.setScreen(new MidnightControlsSettingsScreen(this, false))), true)
            .dimension(20,20).texture(Identifier.of("midnightcontrols", "icon/controller"), 20, 20).build();

    protected GameOptionsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initTabNavigation", at = @At("HEAD"))
    public void addMidnightButton(CallbackInfo ci) {
        if (this.getClass().toString().equals(ControlsOptionsScreen.class.toString())) {
            this.midnightcontrols$setupButton();
            this.addDrawableChild(midnightcontrols$button);
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        this.midnightcontrols$setupButton();
    }
    @Unique
    public void midnightcontrols$setupButton() {
        assert body != null;
        midnightcontrols$button.setPosition(body.getWidth() / 2 + 158, body.getY() + 4);
    }
}