package de.hysky.skyblocker.skyblock.quicknav;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.mixins.accessors.HandledScreenAccessor;
import de.hysky.skyblocker.mixins.accessors.PopupBackgroundAccessor;
import de.hysky.skyblocker.utils.Constants;
import de.hysky.skyblocker.utils.scheduler.MessageScheduler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.PopupScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import java.time.Duration;

@Environment(value = EnvType.CLIENT)
public class QuickNavButton extends ClickableWidget {
    private static final long TOGGLE_DURATION = 1000;

    private final int index;
    private final boolean toggled;
    private final String command;
    private final ItemStack icon;

    private boolean temporaryToggled = false;
    private long toggleTime;

    // Stores whether the button is currently rendering in front of the main inventory background.
    private boolean renderInFront;
    private int alpha = 255;

    /**
     * Checks if the current tab is a top tab based on its index.
     *
     * @return true if the index is less than 7, false otherwise.
     */
    private boolean isTopTab() {
        return index < 7;
    }

    public boolean toggled() {
        return toggled || temporaryToggled;
    }

    public void setRenderInFront(boolean renderInFront) {
        this.renderInFront = renderInFront;
    }

    public int getAlpha() {
        return alpha;
    }

    /**
     * Constructs a new QuickNavButton with the given parameters.
     *
     * @param index   the index of the button.
     * @param toggled the toggled state of the button.
     * @param command the command to execute when the button is clicked.
     * @param icon    the icon to display on the button.
     * @param tooltip the tooltip to show when hovered
     */
    public QuickNavButton(int index, boolean toggled, String command, ItemStack icon, String tooltip) {
        super(0, 0, 26, 32, Text.empty());
        this.index = index;
        this.toggled = toggled;
        this.command = command;
        this.icon = icon;
        this.toggleTime = 0;
        if (tooltip == null || tooltip.isEmpty()) return;
        try {
            setTooltip(Tooltip.of(TextCodecs.CODEC.decode(JsonOps.INSTANCE, SkyblockerMod.GSON.fromJson(tooltip, JsonElement.class)).getOrThrow().getFirst()));
        } catch (Exception e) {
            setTooltip(Tooltip.of(Text.literal(tooltip)));
        }
        setTooltipDelay(Duration.ofMillis(100));
    }

    private void updateCoordinates() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
		while (screen instanceof PopupScreen) {
			if (!(screen instanceof PopupBackgroundAccessor popup)) {
				throw new IllegalStateException(
						"Current PopupScreen does not support AccessorPopupBackground"
				);
			}
			screen = popup.getUnderlyingScreen();
		}
        if (screen instanceof HandledScreen<?> handledScreen) {
            var accessibleScreen = (HandledScreenAccessor) handledScreen;
            int x = accessibleScreen.getX();
            int y = accessibleScreen.getY();
            int h = accessibleScreen.getBackgroundHeight();
			if (handledScreen instanceof GenericContainerScreen) h--; // they messed up the height on these.
            int w = accessibleScreen.getBackgroundWidth();
            this.setX(x + this.index % 7 * 25 + w / 2 - 176 / 2);
            this.setY(this.index < 7 ? y - 28 : y + h - 4);
        }
    }

    /**
     * Handles click events. If the button is not currently toggled,
     * it sets the toggled state to true and sends a message with the command after cooldown.
     *
     * @param mouseX the x-coordinate of the mouse click
     * @param mouseY the y-coordinate of the mouse click
     */
    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!this.temporaryToggled) {
            this.temporaryToggled = true;
            this.toggleTime = System.currentTimeMillis();
            if (command == null || command.isEmpty()) {
                MinecraftClient.getInstance().player.sendMessage(Constants.PREFIX.get().append(Text.literal("Quick Nav button index " + (index + 1) + " has no command!").formatted(Formatting.RED)), false);
            } else {
                MessageScheduler.INSTANCE.sendMessageAfterCooldown(command, true);
            }
            this.alpha = 0;
        }
    }

    /**
     * Renders the button on screen. This includes both its texture and its icon.
     * The method first updates the coordinates of the button,
     * then calculates appropriate values for rendering based on its current state,
     * and finally draws both the background and icon of the button on screen.
     *
     * @param context the context in which to render the button
     * @param mouseX  the x-coordinate of the mouse cursor
     * @param mouseY  the y-coordinate of the mouse cursor
     */
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        this.updateCoordinates();

		// Note that this changes the return value of `toggled()`, so do not call it after this point.
		// Instead, use `renderInFront` to determine whether the button is currently rendering in front of the main inventory background.
        if (this.temporaryToggled && System.currentTimeMillis() - this.toggleTime >= TOGGLE_DURATION) {
            this.temporaryToggled = false; // Reset toggled state
        }
        //"animation"
        if (alpha < 255) {
            alpha = Math.min(alpha + 10, 255);
        }

        // Construct the texture identifier based on the index and toggled state
        Identifier tabTexture = Identifier.ofVanilla("container/creative_inventory/tab_" + (isTopTab() ? "top" : "bottom") + "_" + (renderInFront ? "selected" : "unselected") + "_" + (index % 7 + 1));

        // Render the button texture, always with full alpha if it's not rendering in front
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, tabTexture, this.getX(), this.getY(), this.width, this.height, renderInFront ? ColorHelper.withAlpha(alpha, -1) : -1);
        // Render the button icon
        int yOffset = this.index < 7 ? 1 : -1;
        context.drawItem(this.icon, this.getX() + 5, this.getY() + 8 + yOffset);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}
