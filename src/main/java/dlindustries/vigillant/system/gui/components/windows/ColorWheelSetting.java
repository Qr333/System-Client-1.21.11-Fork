package dlindustries.vigillant.system.gui.components.windows;

import dlindustries.vigillant.system.gui.components.settings.RenderableSetting;
import dlindustries.vigillant.system.gui.components.ModuleButton;
import dlindustries.vigillant.system.module.setting.Setting;
import dlindustries.vigillant.system.module.setting.ColorSetting;
import dlindustries.vigillant.system.utils.RenderUtils;
import dlindustries.vigillant.system.utils.ColorUtils;
import dlindustries.vigillant.system.utils.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import net.minecraft.client.MinecraftClient;

public final class ColorWheelSetting extends RenderableSetting {

    private static final int WHEEL_SIZE  = 110;
    private static final int BAR_HEIGHT  = 12;
    private static final int PADDING     = 10;
    private static final int SWATCH_SIZE = 14;

    /** Full-brightness HS wheel; brightness applied via draw tint (one draw call instead of ~12k fills/frame). */
    private static final Identifier HS_WHEEL_TEX = Identifier.of("system_client", "gui/color_wheel_hs");
    private static volatile boolean hsWheelTextureReady;

    private float hue;
    private float saturation;
    private float brightness;

    public boolean expanded;
    private boolean draggingWheel;
    private boolean draggingBar;

    private final ColorSetting setting;
    private Color currentAlpha;

    public ColorWheelSetting(ModuleButton parent, Setting<?> setting, int offset) {
        super(parent, setting, offset);
        this.setting = (ColorSetting) setting;
        float[] hsb = Color.RGBtoHSB(
                this.setting.getValue().getRed(),
                this.setting.getValue().getGreen(),
                this.setting.getValue().getBlue(), null);
        hue        = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
    }

    private int collapsedHeight() { return parentHeight(); }

    public int expandedExtraHeight() {
        return expanded ? PADDING + WHEEL_SIZE + PADDING + BAR_HEIGHT + PADDING : 0;
    }

    private int wheelLeft() { return parentX() + (parentWidth() - WHEEL_SIZE) / 2; }
    private int wheelTop()  { return parentY() + parentOffset() + offset + collapsedHeight() + PADDING; }
    private int barLeft()   { return wheelLeft(); }
    private int barTop()    { return wheelTop() + WHEEL_SIZE + PADDING; }
    private int barRight()  { return barLeft() + WHEEL_SIZE; }
    private int barBottom() { return barTop() + BAR_HEIGHT; }

    private static void ensureHsWheelTexture() {
        if (hsWheelTextureReady) return;
        synchronized (ColorWheelSetting.class) {
            if (hsWheelTextureReady) return;
            try {
                BufferedImage bi = new BufferedImage(WHEEL_SIZE, WHEEL_SIZE, BufferedImage.TYPE_INT_ARGB);
                float cx = WHEEL_SIZE / 2f;
                float cy = WHEEL_SIZE / 2f;
                for (int py = 0; py < WHEEL_SIZE; py++) {
                    for (int px = 0; px < WHEEL_SIZE; px++) {
                        float dx   = px - cx;
                        float dy   = py - cy;
                        float dist = (float) Math.sqrt(dx * dx + dy * dy);
                        if (dist > cx) {
                            bi.setRGB(px, py, 0);
                            continue;
                        }
                        float h   = (float)((Math.atan2(dy, dx) / (2 * Math.PI) + 1.0) % 1.0);
                        float s   = dist / cx;
                        int   rgb = Color.HSBtoRGB(h, s, 1f);
                        bi.setRGB(px, py, (rgb & 0x00FFFFFF) | 0xFF000000);
                    }
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bi, "PNG", baos);
                NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
                NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "color_wheel_hs", nativeImage);
                MinecraftClient.getInstance().getTextureManager().registerTexture(HS_WHEEL_TEX, texture);
                hsWheelTextureReady = true;
            } catch (Exception e) {
                throw new RuntimeException("ColorWheelSetting: failed to upload HS wheel texture", e);
            }
        }
    }

    private Color currentColor() {
        return Color.getHSBColor(hue, saturation, brightness);
    }

    private void pushColorToSetting() {
        setting.setValue(currentColor());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int rx = parentX();
        int ry = parentY() + parentOffset() + offset;
        int rw = parentWidth();
        int rh = parentHeight();

        Color cur   = currentColor();
        int swatchX = rx + rw - SWATCH_SIZE - 8;
        int swatchY = ry + (rh - SWATCH_SIZE) / 2;

        RenderUtils.renderRoundedQuad(context.getMatrices(),
                new Color(0, 0, 0, 160),
                swatchX - 2, swatchY - 2,
                swatchX + SWATCH_SIZE + 2, swatchY + SWATCH_SIZE + 2,
                3, 3, 3, 3, 10);
        RenderUtils.renderRoundedQuad(context.getMatrices(),
                cur,
                swatchX, swatchY,
                swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE,
                2, 2, 2, 2, 10);

        float scalable = 0.8F;
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scalable, scalable);

        CharSequence name = setting.getName();
        TextRenderer.drawString(name, context,
                (int)((rx + 5) / scalable),
                (int)((ry + 9) / scalable),
                new Color(245, 245, 245, 255).getRGB());




        context.getMatrices().popMatrix();

        if (!parent.parent.dragging) {
            int toAlpha = isHoveredHeader(mouseX, mouseY) ? 18 : 0;
            if (currentAlpha == null) currentAlpha = new Color(255, 255, 255, toAlpha);
            else currentAlpha = new Color(255, 255, 255, currentAlpha.getAlpha());
            if (currentAlpha.getAlpha() != toAlpha)
                currentAlpha = ColorUtils.smoothAlphaTransition(0.05f, toAlpha, currentAlpha);
            context.fill(rx, ry, rx + rw, ry + rh, currentAlpha.getRGB());
        }

        if (!expanded) return;

        int panelTop    = ry + rh;
        int panelBottom = panelTop + expandedExtraHeight();

        RenderUtils.renderRoundedQuad(context.getMatrices(),
                new Color(10, 10, 10, 200),
                rx, panelTop, rx + rw, panelBottom,
                0, 0, 4, 4, 20);

        int wl = wheelLeft();
        int wt = wheelTop();

        ensureHsWheelTexture();
        int br = Math.min(255, Math.max(0, Math.round(brightness * 255f)));
        int brightnessTint = 0xFF000000 | (br << 16) | (br << 8) | br;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, HS_WHEEL_TEX,
                wl, wt, 0f, 0f, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE, WHEEL_SIZE, brightnessTint);

        float cx     = wl + WHEEL_SIZE / 2f;
        float cy     = wt + WHEEL_SIZE / 2f;
        float radius = WHEEL_SIZE / 2f;

        double selAngle = hue * 2 * Math.PI;
        int selX = (int)(cx + saturation * radius * Math.cos(selAngle));
        int selY = (int)(cy + saturation * radius * Math.sin(selAngle));

        RenderUtils.renderCircle(context.getMatrices(), new Color(0, 0, 0, 200), selX, selY, 6, 16);
        RenderUtils.renderCircle(context.getMatrices(), Color.WHITE, selX, selY, 4, 16);
        RenderUtils.renderCircle(context.getMatrices(), cur, selX, selY, 3, 16);

        int bl = barLeft();
        int bt = barTop();

        context.fillGradient(bl, bt, bl + WHEEL_SIZE, bt + BAR_HEIGHT,
                new Color(0, 0, 0, 255).getRGB(),
                Color.getHSBColor(hue, saturation, 1f).getRGB());
        RenderUtils.renderRoundedOutline(context, new Color(80, 80, 80, 180),
                bl, bt, bl + WHEEL_SIZE, bt + BAR_HEIGHT,
                2, 2, 2, 2, 1, 12);

        int thumbX = bl + (int)(brightness * WHEEL_SIZE);
        context.fill(thumbX - 1, bt - 2, thumbX + 1, bt + BAR_HEIGHT + 2, Color.WHITE.getRGB());
    }

    private boolean isHoveredHeader(double mx, double my) {
        int rx = parentX();
        int ry = parentY() + parentOffset() + offset;
        return mx > rx && mx < rx + parentWidth() && my > ry && my < ry + parentHeight();
    }

    private boolean isHoveredWheel(double mx, double my) {
        float cx = wheelLeft() + WHEEL_SIZE / 2f;
        float cy = wheelTop()  + WHEEL_SIZE / 2f;
        float dx = (float)(mx - cx);
        float dy = (float)(my - cy);
        return Math.sqrt(dx * dx + dy * dy) <= WHEEL_SIZE / 2f;
    }

    private boolean isHoveredBar(double mx, double my) {
        return mx >= barLeft() && mx <= barRight() && my >= barTop() && my <= barBottom();
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isHoveredHeader(mouseX, mouseY) && expanded) { expanded = false; return; }
            if (expanded) {
                if (isHoveredWheel(mouseX, mouseY)) { draggingWheel = true; updateWheel(mouseX, mouseY); return; }
                if (isHoveredBar(mouseX, mouseY))   { draggingBar   = true; updateBar(mouseX);           return; }
            }
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (isHoveredHeader(mouseX, mouseY)) { expanded = !expanded; return; }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingWheel) { updateWheel(mouseX, mouseY); return; }
        if (draggingBar)   { updateBar(mouseX);           return; }
        super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        draggingWheel = false;
        draggingBar   = false;
        super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateWheel(double mx, double my) {
        float cx   = wheelLeft() + WHEEL_SIZE / 2f;
        float cy   = wheelTop()  + WHEEL_SIZE / 2f;
        float dx   = (float)(mx - cx);
        float dy   = (float)(my - cy);
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        hue        = (float)((Math.atan2(dy, dx) / (2 * Math.PI) + 1.0) % 1.0);
        saturation = Math.min(1f, dist / (WHEEL_SIZE / 2f));
        pushColorToSetting();
    }

    private void updateBar(double mx) {
        brightness = Math.max(0f, Math.min(1f, (float)((mx - barLeft()) / WHEEL_SIZE)));
        pushColorToSetting();
    }
}