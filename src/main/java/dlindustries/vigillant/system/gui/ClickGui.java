package dlindustries.vigillant.system.gui;

import dlindustries.vigillant.system.module.modules.Misc.NameProtect;
import dlindustries.vigillant.system.system;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.utils.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import static dlindustries.vigillant.system.system.mc;

public final class ClickGui extends Screen {

    public List<Window> windows = new ArrayList<>();
    public Color currentColor;

    private TextFieldWidget searchField;
    private static final int SEARCH_BAR_WIDTH  = 140;
    private static final int SEARCH_BAR_HEIGHT = 18;
    private static final int SEARCH_BAR_Y      = 6;

    private static final Identifier BACKGROUND_IMAGE = Identifier.of("system", "images/ren.png");

    private static final int WINDOW_START_X      = 50;
    private static final int WINDOW_OFFSET_X     = 250;
    private static final int WINDOW_Y            = 50;
    private static final int WINDOW_WIDTH        = 230;
    private static final int WINDOW_TITLE_HEIGHT = 30;

    public ClickGui() {
        super(Text.empty());
        int offsetX = WINDOW_START_X;
        for (Category category : Category.values()) {
            windows.add(new Window(offsetX, WINDOW_Y, WINDOW_WIDTH, WINDOW_TITLE_HEIGHT, category, this));
            offsetX += WINDOW_OFFSET_X;
        }
    }

    @Override
    public void init() {
        int bx = (mc.getWindow().getScaledWidth() - SEARCH_BAR_WIDTH) / 2;
        searchField = new TextFieldWidget(mc.textRenderer, bx, SEARCH_BAR_Y,
                SEARCH_BAR_WIDTH, SEARCH_BAR_HEIGHT, Text.literal("Search..."));
        searchField.setMaxLength(50);
        searchField.setDrawsBackground(false);
        searchField.setPlaceholder(Text.literal("Search..."));
        addSelectableChild(searchField);
    }

    public String getSearchQuery() {
        return searchField != null ? searchField.getText() : "";
    }

    public boolean isDraggingAlready() {
        for (Window w : windows) if (w.dragging) return true;
        return false;
    }

    public Color getAccentColor(int alpha, int index) {
        if (ClickGUI.useCustomColor.getValue()) {
            Color base = ClickGUI.accentColor.getValue();
            float shift = (index * 8f) / 360f;
            float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
            Color shifted = Color.getHSBColor((hsb[0] + shift) % 1f, hsb[1], hsb[2]);
            return new Color(shifted.getRed(), shifted.getGreen(), shifted.getBlue(),
                    Math.max(0, Math.min(255, alpha)));
        }
        return Utils.getMainColor(alpha, index);
    }

    private void renderSearchBar(DrawContext context, int mouseX, int mouseY) {
        if (searchField == null) return;
        int bx = searchField.getX();
        int by = searchField.getY();
        int bw = searchField.getWidth();
        int bh = searchField.getHeight();
        boolean focused = searchField.isFocused();

        RenderUtils.renderRoundedQuad(context.getMatrices(),
                focused ? new Color(25, 25, 28, 230) : new Color(14, 14, 16, 200),
                bx, by, bx + bw, by + bh, 5, 50);
        RenderUtils.renderRoundedOutline(context,
                focused ? getAccentColor(200, 0) : new Color(60, 60, 65, 160),
                bx, by, bx + bw, by + bh, 5, 5, 5, 5, 1.3, 30);

        if (focused) {
            context.fillGradient(bx + 6, by + bh - 1, bx + bw - 6, by + bh,
                    getAccentColor(190, 0).getRGB(),
                    getAccentColor(190, 4).getRGB());
        }

        searchField.render(context, mouseX, mouseY, 0);
    }

    private void renderInfoTags(DrawContext context) {
        if (mc.player == null || mc.getWindow() == null) return;

        String playerName = mc.player.getName().getString();
        NameProtect np =
                system.INSTANCE.getModuleManager().getModule(
                        NameProtect.class);
        if (np != null) playerName = np.replaceName(playerName);

        int sw  = mc.getWindow().getWidth();
        int sh  = mc.getWindow().getHeight();
        int th  = mc.textRenderer.fontHeight;
        int pad = 5;

        renderTag(context, "Player: " + playerName,
                sw - TextRenderer.getWidth("Player: " + playerName) - pad * 2 - 2,
                sh - th - pad * 2);

        renderTag(context, "System Fork by Qr3",
                pad,
                sh - th - pad * 2);
    }

    private void renderTag(DrawContext context, String text, int x, int y) {
        int tw  = TextRenderer.getWidth(text);
        int th  = mc.textRenderer.fontHeight;
        int pad = 5;
        RenderUtils.renderRoundedQuad(context.getMatrices(), new Color(10, 10, 12, 200),
                x - pad, y - 3, x + tw + pad, y + th + 3, 3, 10);
        RenderUtils.renderRoundedOutline(context, new Color(55, 55, 60, 150),
                x - pad, y - 3, x + tw + pad, y + th + 3, 3, 3, 3, 3, 1, 12);
        TextRenderer.drawString(text, context, x, y, new Color(185, 185, 190, 230).getRGB());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (mc.currentScreen != this) return;
        if (ClickGUI.blur.getValue()) mc.gameRenderer.renderBlur();
        if (system.INSTANCE.previousScreen != null)
            system.INSTANCE.previousScreen.render(context, 0, 0, delta);

        if (currentColor == null) currentColor = new Color(0, 0, 0, 0);
        else currentColor = new Color(0, 0, 0, currentColor.getAlpha());
        int targetAlpha = ClickGUI.background.getValue() ? ClickGUI.backgroundAlpha.getValueInt() : 0;
        if (currentColor.getAlpha() != targetAlpha)
            currentColor = ColorUtils.smoothAlphaTransition(0.05F, targetAlpha, currentColor);

        float scaleFactor = (float) mc.getWindow().getScaleFactor();
        float invScale    = 1.0f / scaleFactor;

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(invScale, invScale);

        if (mc.currentScreen instanceof ClickGui)
            context.fill(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight(), currentColor.getRGB());

        if (ClickGUI.backgroundImage.getValue()) {
            int iw = 699, ih = 357;
            int sw = mc.getWindow().getWidth(), sh = mc.getWindow().getHeight();
            context.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND_IMAGE,
                    (sw - iw) / 2, sh - ih, 0f, 0f, iw, ih, iw, ih);
        }

        int px = (int)(mouseX * scaleFactor);
        int py = (int)(mouseY * scaleFactor);

        for (Window window : windows) {
            window.updatePosition(px, py, delta);
            window.render(context, px, py, delta);
        }

        renderInfoTags(context);
        context.getMatrices().popMatrix();
        renderSearchBar(context, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (searchField != null && searchField.isFocused()) {
            if (keyInput.key() == 256) {
                searchField.setText("");
                searchField.setFocused(false);
                setFocused(null);
                return true;
            }
            if (searchField.keyPressed(keyInput)) return true;
        }
        for (Window w : windows) w.keyPressed(keyInput.key(), keyInput.scancode(), keyInput.modifiers());
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (searchField != null) {
            boolean over = click.x() >= searchField.getX()
                    && click.x() <= searchField.getX() + searchField.getWidth()
                    && click.y() >= searchField.getY()
                    && click.y() <= searchField.getY() + searchField.getHeight();
            if (over) {
                setFocused(searchField);
                searchField.setFocused(true);
                searchField.mouseClicked(click, doubled);
                return true;
            } else {
                setFocused(null);
                searchField.setFocused(false);
            }
        }
        double sf = mc.getWindow().getScaleFactor();
        int px = (int)(click.x() * sf);
        int py = (int)(click.y() * sf);
        for (Window w : windows) w.mouseClicked(px, py, click.button());
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double sf = mc.getWindow().getScaleFactor();
        int px = (int)(click.x() * sf);
        int py = (int)(click.y() * sf);
        for (Window w : windows) w.mouseDragged(px, py, click.button(), deltaX, deltaY);
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        double sf = mc.getWindow().getScaleFactor();
        int px = (int)(mx * sf);
        int py = (int)(my * sf);
        for (Window w : windows) {
            if (w.isContentHovered(px, py)) { w.mouseScrolled(px, py, h, v); return true; }
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean mouseReleased(Click click) {
        double sf = mc.getWindow().getScaleFactor();
        int px = (int)(click.x() * sf);
        int py = (int)(click.y() * sf);
        for (Window w : windows) w.mouseReleased(px, py, click.button());
        return super.mouseReleased(click);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        ClickGUI m = system.INSTANCE.getModuleManager().getModule(ClickGUI.class);
        if (m != null && m.isEnabled()) { m.setEnabled(false); return; }
        onGuiClose();
    }

    public void onGuiClose() {
        mc.setScreenAndRender(system.INSTANCE.previousScreen);
        currentColor = null;
        if (searchField != null) {
            searchField.setText("");
            searchField.setFocused(false);
        }
        for (Window w : windows) w.onGuiClose();
    }
}