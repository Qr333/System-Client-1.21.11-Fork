package dlindustries.vigillant.system.gui;



import dlindustries.vigillant.system.system;


import dlindustries.vigillant.system.gui.components.ModuleButton;

import dlindustries.vigillant.system.gui.components.settings.RenderableSetting;

import dlindustries.vigillant.system.gui.components.windows.ColorWheelSetting;

import dlindustries.vigillant.system.module.Category;

import dlindustries.vigillant.system.module.Module;

import dlindustries.vigillant.system.module.modules.client.ClickGUI;

import dlindustries.vigillant.system.utils.*;

import net.minecraft.client.gui.DrawContext;

import net.minecraft.item.ItemStack;

import net.minecraft.item.Items;

import java.awt.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static dlindustries.vigillant.system.system.mc;



public final class Window {




    public List<ModuleButton> moduleButtons = new ArrayList<>();

    public int x;

    public int y;

    private final int width;

    private final int height;

    public Color currentColor;

    private final Category category;

    public boolean dragging;

    public boolean extended = true;

    private int dragX, dragY;

    public ClickGui parent;



    private double animContentHeight = -1;

    private double targetX, targetY;

    private boolean positionInitialized = false;



    public Window(int x, int y, int width, int height, Category category, ClickGui parent) {

        this.x = x;

        this.y = y;

        this.targetX = x;

        this.targetY = y;

        this.width = width;

        this.height = height;

        this.category = category;

        this.parent = parent;



        rebuildModuleButtons();

    }



    public Category getCategory() {

        return category;

    }



    public void rebuildModuleButtons() {

        moduleButtons.clear();

        int offset = height;

        for (Module module : collectModules()) {

            moduleButtons.add(new ModuleButton(this, module, offset));

            offset += height;

        }

    }



    private List<Module> collectModules() {
        List<Module> all = new ArrayList<>(system.INSTANCE.getModuleManager().getModulesInCategory(category));
        all.sort(Comparator.comparing(m -> m.getName().toString(), String.CASE_INSENSITIVE_ORDER));
        return all;
    }



    private ItemStack categoryIcon() {

        return switch (category) {
            case Combat -> new ItemStack(Items.NETHERITE_SWORD);
            case CrystalPvP -> new ItemStack(Items.END_CRYSTAL);
            case Misc -> new ItemStack(Items.DIAMOND_PICKAXE);
            case CLIENT -> new ItemStack(Items.COMPARATOR);
            case RENDER -> new ItemStack(Items.ENDER_EYE);
        };

    }



    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        int toAlpha = ClickGUI.alphaWindow.getValueInt();

        if (currentColor == null) currentColor = new Color(0, 0, 0, 0);

        else currentColor = new Color(0, 0, 0, currentColor.getAlpha());

        if (currentColor.getAlpha() != toAlpha)

            currentColor = ColorUtils.smoothAlphaTransition(0.05F, toAlpha, currentColor);



        updateButtons(delta);



        int targetContentH = extended ? getRealContentHeight() : 0;

        if (animContentHeight < 0) animContentHeight = targetContentH;



        double speed = Math.min(1.0, 0.35 * delta);

        animContentHeight += (targetContentH - animContentHeight) * speed;

        if (Math.abs(animContentHeight - targetContentH) < 0.5) animContentHeight = targetContentH;



        int displayH = height + (int) animContentHeight;



        double scaledRound = ClickGUI.roundQuads.getValue() * mc.getWindow().getScaleFactor();



        Color bgColor  = new Color(

                Math.max(0, currentColor.getRed()   - 2),

                Math.max(0, currentColor.getGreen() - 2),

                Math.max(0, currentColor.getBlue()  - 2),

                currentColor.getAlpha());

        Color hdrColor = new Color(0, 0, 0, Math.min(255, currentColor.getAlpha() + 40));

        Color olColor  = new Color(90, 90, 95, Math.min(255, currentColor.getAlpha() + 55));



        RenderUtils.renderRoundedQuad(context.getMatrices(), bgColor,

                x, y, x + width, y + displayH,

                scaledRound, scaledRound, scaledRound, scaledRound, 50);



        RenderUtils.renderRoundedQuad(context.getMatrices(), hdrColor,

                x, y, x + width, y + height,

                scaledRound, scaledRound,

                animContentHeight < 2 ? scaledRound : 0,

                animContentHeight < 2 ? scaledRound : 0,

                50);



        Color accent0 = parent.getAccentColor(255, 0);

        Color accent1 = parent.getAccentColor(255, 3);

        context.fillGradient(

                x + 8, y + height - 1, x + width - 8, y + height,

                accent0.getRGB(), accent1.getRGB());



        RenderUtils.renderRoundedOutline(context, new Color(accent0.getRed(), accent0.getGreen(), accent0.getBlue(), 160),

                x + 3, y + 3, x + width - 3, y + height - 3,

                Math.max(2, scaledRound - 2), Math.max(2, scaledRound - 2),

                animContentHeight < 2 ? Math.max(2, scaledRound - 2) : 0,

                animContentHeight < 2 ? Math.max(2, scaledRound - 2) : 0,

                1.0, 28);



        RenderUtils.renderRoundedOutline(context, olColor,

                x, y, x + width, y + displayH,

                scaledRound, scaledRound, scaledRound, scaledRound, 1.2, 30);



        int accentLineAlpha = (int)(animContentHeight > 1 ? Math.min(180, animContentHeight * 3) : 0);

        if (accentLineAlpha > 0) {

            Color sideAccent = parent.getAccentColor(accentLineAlpha, 0);

            context.fill(x, y + height, x + 1, y + displayH, sideAccent.getRGB());

        }



        CharSequence name = category.name;

        int tw = TextRenderer.getWidth(name);

        int iw = 16;

        int gap = 7;

        int total = iw + gap + tw;

        int sx = x + (width / 2) - (total / 2);

        context.drawItem(categoryIcon(), sx, y + 7);

        TextRenderer.drawString(name, context, sx + iw + gap, y + 8,

                new Color(255, 255, 255, extended ? 255 : 175).getRGB());



        if (animContentHeight > 1) {

            int clipBottom = y + displayH;

            for (ModuleButton mb : moduleButtons) {

                if (y + mb.offset < clipBottom)

                    mb.render(context, mouseX, mouseY, delta);

            }

        }

    }



    public void updateButtons(float delta) {

        int offset = height;

        double speed = switch (ClickGUI.animationMode.getMode()) {

            case Positive -> Math.min(1.0, 0.35 * delta);

            case Off      -> 1.0;

            default       -> Math.min(1.0, 0.25 * delta);

        };

        for (ModuleButton mb : moduleButtons) {

            double target = mb.extended

                    ? height * (mb.settings.size() + 1) + getColorWheelExtra(mb)

                    : height;

            mb.animation.animate(speed, target);

            mb.offset = offset;

            offset += Math.max(height, (int) mb.animation.getValue());

        }

    }



    private int getColorWheelExtra(ModuleButton mb) {

        int extra = 0;

        for (RenderableSetting rs : mb.settings) {

            if (rs instanceof ColorWheelSetting cw) {

                extra += cw.expandedExtraHeight();

            }

        }

        return extra;

    }



    private int getRealContentHeight() {
        int total = 0;
        for (ModuleButton mb : moduleButtons)

            total += Math.max(height, (int) mb.animation.getValue());

        return total;

    }



    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY)) {

            if (button == 0 && !parent.isDraggingAlready()) {

                dragging = true;

                dragX = (int)(mouseX - x);

                dragY = (int)(mouseY - y);

            }

            if (button == 1) {

                extended = !extended;

                if (!extended)

                    for (ModuleButton mb : moduleButtons) mb.extended = false;

                return;

            }

        }

        if (extended)

            for (ModuleButton mb : moduleButtons) mb.mouseClicked(mouseX, mouseY, button);

    }



    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {

        if (extended)

            for (ModuleButton mb : moduleButtons) mb.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);

    }



    public void mouseReleased(double mouseX, double mouseY, int button) {

        if (button == 0 && dragging) dragging = false;

        for (ModuleButton mb : moduleButtons) mb.mouseReleased(mouseX, mouseY, button);

    }



    public void mouseScrolled(double mouseX, double mouseY, double h, double v) {

        setY((int)(y + (v * 20)));

    }



    public void keyPressed(int keyCode, int scanCode, int modifiers) {

        for (ModuleButton mb : moduleButtons) mb.keyPressed(keyCode, scanCode, modifiers);

    }



    public void onGuiClose() {

        currentColor        = null;

        animContentHeight   = -1;

        dragging            = false;

        positionInitialized = false;

        for (ModuleButton mb : moduleButtons) mb.onGuiClose();

    }



    public boolean isDraggingAlready() {

        for (Window w : parent.windows) if (w.dragging) return true;

        return false;

    }



    public boolean isHovered(double mx, double my) {

        return mx > x && mx < x + width && my > y && my < y + height;

    }



    public boolean isContentHovered(double mx, double my) {

        int displayH = height + (int) Math.max(0, animContentHeight);

        return mx > x && mx < x + width && my > y && my < y + displayH;

    }



    public void updatePosition(double mouseX, double mouseY, float delta) {

        if (dragging) {

            targetX = mouseX - dragX;

            targetY = mouseY - dragY;

        }

        if (!positionInitialized) {

            positionInitialized = true;

        } else {

            double lerpSpeed = Math.min(1.0, 0.35 * delta);

            x = (int)(x + (targetX - x) * lerpSpeed);

            y = (int)(y + (targetY - y) * lerpSpeed);

        }

    }



    public double getAnimContentHeight() { return animContentHeight; }



    public int getX()       { return x; }

    public int getY()       { return y; }

    public void setX(int x) { this.x = x; this.targetX = x; }

    public void setY(int y) { this.y = y; this.targetY = y; }

    public int getWidth()   { return width; }

    public int getHeight()  { return height; }

}

