package dlindustries.vigillant.system.gui.components;

import dlindustries.vigillant.system.gui.components.windows.StringBox;
import dlindustries.vigillant.system.gui.components.windows.ColorWheelSetting;
import dlindustries.vigillant.system.system;
import dlindustries.vigillant.system.gui.ClickGui;
import dlindustries.vigillant.system.gui.Window;
import dlindustries.vigillant.system.gui.components.settings.*;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.module.setting.*;
import dlindustries.vigillant.system.utils.*;
import net.minecraft.client.gui.DrawContext;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import static dlindustries.vigillant.system.system.mc;

public final class ModuleButton {
    public List<RenderableSetting> settings = new ArrayList<>();
    public Window parent;
    public Module module;
    public int offset;
    public boolean extended;
    public int settingOffset;
    public Color currentColor;
    public Color defaultColor = Color.WHITE;
    public Color currentAlpha;
    private Color searchHighlightColor = new Color(255, 255, 255, 0);
    public AnimationUtils animation = new AnimationUtils(0);

    public ModuleButton(Window parent, Module module, int offset) {
        this.parent   = parent;
        this.module   = module;
        this.offset   = offset;
        this.extended = false;
        settingOffset = parent.getHeight();

        for (Setting<?> setting : module.getSettings()) {
            if      (setting instanceof BooleanSetting  s) settings.add(new CheckBox         (this, s, settingOffset));
            else if (setting instanceof NumberSetting   s) settings.add(new Slider            (this, s, settingOffset));
            else if (setting instanceof ModeSetting<?>  s) settings.add(new ModeBox           (this, s, settingOffset));
            else if (setting instanceof KeybindSetting  s) settings.add(new KeybindBox        (this, s, settingOffset));
            else if (setting instanceof ColorSetting    s) settings.add(new ColorWheelSetting (this, s, settingOffset));
            else if (setting instanceof StringSetting   s) settings.add(new StringBox         (this, s, settingOffset));
            else if (setting instanceof MinMaxSetting   s) settings.add(new MinMaxSlider      (this, s, settingOffset));
            settingOffset += parent.getHeight();
        }
    }

    private ClickGui getClickGui() {
        if (parent.parent instanceof ClickGui cg) return cg;
        return null;
    }

    private String getSearchQuery() {
        ClickGui cg = getClickGui();
        return cg != null ? cg.getSearchQuery() : "";
    }

    private boolean matchesSearch(String query) {
        if (query.isEmpty()) return false;
        return module.getName().toString().toLowerCase().contains(query.toLowerCase());
    }

    private Color getAccentColor(int alpha, int index) {
        ClickGui cg = getClickGui();
        return cg != null ? cg.getAccentColor(alpha, index) : Utils.getMainColor(alpha, index);
    }

    public int getTotalSettingsHeight() {
        if (!extended) return 0;
        int total = 0;
        for (RenderableSetting rs : settings) {
            total += parent.getHeight();
            if (rs instanceof ColorWheelSetting cw) {
                total += cw.expandedExtraHeight();
            }
        }
        return total;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (RenderableSetting rs : settings) rs.onUpdate();

        if (currentColor == null) currentColor = new Color(0, 0, 0, 0);
        else currentColor = new Color(0, 0, 0, currentColor.getAlpha());
        currentColor = ColorUtils.smoothAlphaTransition(0.05F, ClickGUI.alphaWindow.getValueInt(), currentColor);

        int idx = system.INSTANCE.getModuleManager()
                .getModulesInCategory(module.getCategory()).indexOf(module);

        Color toColor = module.isEnabled()
                ? getAccentColor(255, idx)
                : Color.WHITE;
        if (!defaultColor.equals(toColor))
            defaultColor = ColorUtils.smoothColorTransition(0.12F, toColor, defaultColor);

        context.fill(parent.getX(), parent.getY() + offset,
                parent.getX() + parent.getWidth(), parent.getY() + parent.getHeight() + offset,
                currentColor.getRGB());

        String query    = getSearchQuery();
        boolean matches = matchesSearch(query);

        if (!query.isEmpty()) {
            if (matches) {
                searchHighlightColor = ColorUtils.smoothAlphaTransition(0.10F, 55, searchHighlightColor);
                context.fillGradient(
                        parent.getX(), parent.getY() + offset,
                        parent.getX() + parent.getWidth(), parent.getY() + parent.getHeight() + offset,
                        getAccentColor(searchHighlightColor.getAlpha(), idx).getRGB(),
                        getAccentColor(searchHighlightColor.getAlpha(), idx + 1).getRGB());
                context.fillGradient(
                        parent.getX(), parent.getY() + offset,
                        parent.getX() + 3, parent.getY() + parent.getHeight() + offset,
                        getAccentColor(255, idx).getRGB(),
                        getAccentColor(255, idx + 1).getRGB());
            } else {
                searchHighlightColor = ColorUtils.smoothAlphaTransition(0.10F, 0, searchHighlightColor);
                context.fill(parent.getX(), parent.getY() + offset,
                        parent.getX() + parent.getWidth(), parent.getY() + parent.getHeight() + offset,
                        new Color(0, 0, 0, 115).getRGB());
                context.fillGradient(
                        parent.getX(), parent.getY() + offset,
                        parent.getX() + 2, parent.getY() + parent.getHeight() + offset,
                        getAccentColor(90, idx).getRGB(),
                        getAccentColor(90, idx + 1).getRGB());
            }
        } else {
            searchHighlightColor = ColorUtils.smoothAlphaTransition(0.10F, 0, searchHighlightColor);
            context.fillGradient(
                    parent.getX(), parent.getY() + offset,
                    parent.getX() + 2, parent.getY() + parent.getHeight() + offset,
                    getAccentColor(255, idx).getRGB(),
                    getAccentColor(255, idx + 1).getRGB());
        }

        if (module.isEnabled()) {
            context.fillGradient(
                    parent.getX(), parent.getY() + offset,
                    parent.getX() + parent.getWidth(), parent.getY() + parent.getHeight() + offset,
                    getAccentColor(18, idx).getRGB(),
                    getAccentColor(8, idx + 2).getRGB());
        }

        if (parent.moduleButtons.get(parent.moduleButtons.size() - 1) == this) {
            context.fillGradient(
                    parent.getX(), parent.getY() + offset + parent.getHeight() - 1,
                    parent.getX() + parent.getWidth(), parent.getY() + offset + parent.getHeight(),
                    getAccentColor(160, idx).getRGB(),
                    getAccentColor(160, idx + 1).getRGB());
        }

        CharSequence name = module.getName();
        int tw    = TextRenderer.getWidth(name);
        int textX = parent.getX() + parent.getWidth() / 2 - tw / 2;
        int nameColor = (!query.isEmpty() && !matches)
                ? new Color(135, 135, 140, 165).getRGB()
                : defaultColor.getRGB();
        TextRenderer.drawString(name, context, textX, parent.getY() + offset + 8, nameColor);

        renderHover(context, mouseX, mouseY, delta);
        renderSettings(context, mouseX, mouseY, delta);

        for (RenderableSetting rs : settings)
            if (extended) rs.renderDescription(context, mouseX, mouseY, delta);

        if (isHovered(mouseX, mouseY) && !parent.dragging) {
            CharSequence desc = module.getDescription();
            int dtw    = TextRenderer.getWidth(desc);
            int center = mc.getWindow().getFramebufferWidth() / 2;
            int dtx    = center - dtw / 2;
            int dty    = mc.getWindow().getFramebufferHeight() / 2 + 294;
            RenderUtils.renderRoundedQuad(context.getMatrices(), new Color(12, 12, 14, 190),
                    dtx - 6, dty - 3, dtx + dtw + 6, dty + 16, 3, 10);
            RenderUtils.renderRoundedOutline(context, new Color(65, 65, 70, 160),
                    dtx - 6, dty - 3, dtx + dtw + 6, dty + 16, 3, 3, 3, 3, 1, 12);
            TextRenderer.drawString(desc, context, dtx, dty, new Color(195, 195, 200, 240).getRGB());
        }
    }

    private void renderHover(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!parent.dragging) {
            int toHoverAlpha = isHovered(mouseX, mouseY) ? 22 : 0;
            if (currentAlpha == null) currentAlpha = new Color(255, 255, 255, toHoverAlpha);
            else currentAlpha = new Color(255, 255, 255, currentAlpha.getAlpha());
            if (currentAlpha.getAlpha() != toHoverAlpha)
                currentAlpha = ColorUtils.smoothAlphaTransition(0.08F, toHoverAlpha, currentAlpha);
            context.fill(parent.getX(), parent.getY() + offset,
                    parent.getX() + parent.getWidth(), parent.getY() + parent.getHeight() + offset,
                    currentAlpha.getRGB());
        }
    }

    private void renderSettings(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!extended) return;

        int extraOffset = 0;
        for (RenderableSetting rs : settings) {
            rs.offset = settingOffsetFor(rs) + extraOffset;
            rs.render(context, mouseX, mouseY, delta);
            if (rs instanceof ColorWheelSetting cw) {
                extraOffset += cw.expandedExtraHeight();
            }
        }

        for (RenderableSetting rs : settings) {
            if (rs instanceof Slider slider) {
                RenderUtils.renderCircle(context.getMatrices(), new Color(0, 0, 0, 175),
                        slider.parentX() + Math.max(slider.lerpedOffsetX, 2.5),
                        slider.parentY() + slider.offset + slider.parentOffset() + 27.5, 6, 15);
                RenderUtils.renderCircle(context.getMatrices(), slider.currentColor1.brighter(),
                        slider.parentX() + Math.max(slider.lerpedOffsetX, 2.5),
                        slider.parentY() + slider.offset + slider.parentOffset() + 27.5, 5, 15);
            } else if (rs instanceof MinMaxSlider slider) {
                RenderUtils.renderCircle(context.getMatrices(), new Color(0, 0, 0, 175),
                        slider.parentX() + Math.max(slider.lerpedOffsetMinX, 2.5),
                        slider.parentY() + slider.offset + slider.parentOffset() + 27.5, 6, 15);
                RenderUtils.renderCircle(context.getMatrices(), slider.currentColor1.brighter(),
                        slider.parentX() + Math.max(slider.lerpedOffsetMinX, 2.5),
                        slider.parentY() + slider.offset + slider.parentOffset() + 27.5, 5, 15);
                RenderUtils.renderCircle(context.getMatrices(), new Color(0, 0, 0, 175),
                        slider.parentX() + Math.max(slider.lerpedOffsetMaxX, 2.5),
                        slider.parentY() + slider.offset + slider.parentOffset() + 27.5, 6, 15);
                RenderUtils.renderCircle(context.getMatrices(), slider.currentColor1.brighter(),
                        slider.parentX() + Math.max(slider.lerpedOffsetMaxX, 2.5),
                        slider.parentY() + slider.offset + slider.parentOffset() + 27.5, 5, 15);
            }
        }
    }

    private int settingOffsetFor(RenderableSetting target) {
        int off = parent.getHeight();
        for (RenderableSetting rs : settings) {
            if (rs == target) return off;
            off += parent.getHeight();
        }
        return off;
    }

    public void onExtend() {
        for (ModuleButton mb : parent.moduleButtons) mb.extended = false;
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (RenderableSetting rs : settings) rs.keyPressed(keyCode, scanCode, modifiers);
    }

    public void mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (extended)
            for (RenderableSetting rs : settings) rs.mouseDragged(mx, my, button, dx, dy);
    }

    public void mouseClicked(double mx, double my, int button) {
        if (isHovered(mx, my)) {
            if (button == 0) module.toggle();
            if (button == 1) {
                if (module.getSettings().isEmpty()) return;
                if (!extended) onExtend();
                extended = !extended;
            }
        }
        if (extended)
            for (RenderableSetting rs : settings) rs.mouseClicked(mx, my, button);
    }

    public void onGuiClose() {
        currentAlpha         = null;
        currentColor         = null;
        searchHighlightColor = new Color(255, 255, 255, 0);
        for (RenderableSetting rs : settings) rs.onGuiClose();
    }

    public void mouseReleased(double mx, double my, int button) {
        for (RenderableSetting rs : settings) rs.mouseReleased(mx, my, button);
    }

    public boolean isHovered(double mx, double my) {
        return mx > parent.getX()
                && mx < parent.getX() + parent.getWidth()
                && my > parent.getY() + offset
                && my < parent.getY() + offset + parent.getHeight();
    }
}