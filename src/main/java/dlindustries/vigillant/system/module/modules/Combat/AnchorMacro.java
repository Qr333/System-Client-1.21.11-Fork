package dlindustries.vigillant.system.module.modules.Combat;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.BlockUtils;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.WorldUtils;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class AnchorMacro extends Module implements TickListener {
    public enum Mode { MODE1, MODE2 }

    private final ModeSetting<Mode> mode = new ModeSetting<>(EncryptedString.of("Mode"), Mode.MODE1, Mode.class);
    private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), 71, false);
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0.0, 20.0, 0.0, 1.0);
    private final NumberSetting glowstoneDelay = new NumberSetting(EncryptedString.of("Glowstone Delay"), 0.0, 20.0, 0.0, 1.0);
    private final NumberSetting explodeDelay = new NumberSetting(EncryptedString.of("Explode Delay"), 0.0, 20.0, 0.0, 1.0);
    private final NumberSetting totemSlot = new NumberSetting(EncryptedString.of("Totem Slot"), 1.0, 9.0, 1.0, 1.0);

    private int switchClock;
    private int glowstoneClock;
    private int explodeClock;

    private boolean mode2Active = false;
    private int mode2Step = 0;
    private BlockPos mode2Pos = null;
    private BlockHitResult mode2PlaceHit = null;
    private boolean mode2KeyWasPressed = false;

    public AnchorMacro() {
        super(EncryptedString.of("Anchor Macro"),
                EncryptedString.of("Auto anchor charging and exploding"),
                -1,
                Category.CrystalPvP);
        addSettings(mode, activateKey, switchDelay, glowstoneDelay, explodeDelay, totemSlot);
    }

    @Override
    public void onEnable() {
        resetState();
        eventManager.add(TickListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        resetState();
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;
        if (mc.player == null) return;

        if (mode.getMode() == Mode.MODE1) {
            handleMode1();
        } else {
            handleMode2();
        }
    }

    private void handleMode1() {
        if (isShieldOrFoodActive()) return;
        if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS) {
            resetClocks();
            return;
        }

        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;
        if (!BlockUtils.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) return;

        mc.options.useKey.setPressed(false);

        if (BlockUtils.isAnchorNotCharged(hit.getBlockPos())) {
            handleMode1Charge(hit);
        } else if (BlockUtils.isAnchorCharged(hit.getBlockPos())) {
            handleMode1Explode(hit);
        }
    }

    private void handleMode1Charge(BlockHitResult hit) {
        if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            if (switchClock < switchDelay.getValueInt()) {
                switchClock++;
                return;
            }
            switchClock = 0;
            InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE);
            return;
        }

        if (glowstoneClock < glowstoneDelay.getValueInt()) {
            glowstoneClock++;
            return;
        }
        glowstoneClock = 0;
        WorldUtils.placeBlock(hit, true);
    }

    private void handleMode1Explode(BlockHitResult hit) {
        int slot = totemSlot.getValueInt() - 1;
        if (mc.player.getInventory().getSelectedSlot() != slot) {
            if (switchClock < switchDelay.getValueInt()) {
                switchClock++;
                return;
            }
            switchClock = 0;
            mc.player.getInventory().setSelectedSlot(slot);
            return;
        }

        if (explodeClock < explodeDelay.getValueInt()) {
            explodeClock++;
            return;
        }
        explodeClock = 0;
        WorldUtils.placeBlock(hit, true);
    }

    private void handleMode2() {
        int key = activateKey.getKey();
        boolean keyDown = key != -1 && GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
        boolean keyJustPressed = keyDown && !mode2KeyWasPressed;
        mode2KeyWasPressed = keyDown;

        if (!mode2Active) {
            if (keyJustPressed && mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                mode2Active = true;
                mode2Step = 0;
                mode2PlaceHit = hit;
                mode2Pos = null;
                resetClocks();
            }
            return;
        }

        switch (mode2Step) {
            case 0 -> {
                if (!mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
                    if (switchClock < switchDelay.getValueInt()) {
                        switchClock++;
                        return;
                    }
                    switchClock = 0;
                    InventoryUtils.selectItemFromHotbar(Items.RESPAWN_ANCHOR);
                    return;
                }
                if (mode2PlaceHit == null) {
                    resetState();
                    return;
                }
                WorldUtils.placeBlock(mode2PlaceHit, true);
                mode2Pos = mode2PlaceHit.getBlockPos().offset(mode2PlaceHit.getSide());
                mode2Step = 1;
                switchClock = 0;
            }

            case 1 -> {
                if (mode2Pos == null || !BlockUtils.isBlock(mode2Pos, Blocks.RESPAWN_ANCHOR)) {
                    resetState();
                    return;
                }
                if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                    if (switchClock < switchDelay.getValueInt()) {
                        switchClock++;
                        return;
                    }
                    switchClock = 0;
                    InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE);
                    return;
                }
                if (BlockUtils.isAnchorNotCharged(mode2Pos)) {
                    if (glowstoneClock < glowstoneDelay.getValueInt()) {
                        glowstoneClock++;
                        return;
                    }
                    glowstoneClock = 0;
                    BlockHitResult chargeHit = new BlockHitResult(mode2Pos.toCenterPos(), mc.player.getHorizontalFacing(), mode2Pos, false);
                    WorldUtils.placeBlock(chargeHit, true);
                } else {
                    mode2Step = 2;
                    switchClock = 0;
                }
            }

            case 2 -> {
                if (mode2Pos == null || !BlockUtils.isBlock(mode2Pos, Blocks.RESPAWN_ANCHOR)) {
                    resetState();
                    return;
                }
                int slot = totemSlot.getValueInt() - 1;
                if (mc.player.getInventory().getSelectedSlot() != slot) {
                    if (switchClock < switchDelay.getValueInt()) {
                        switchClock++;
                        return;
                    }
                    switchClock = 0;
                    mc.player.getInventory().setSelectedSlot(slot);
                    return;
                }
                if (explodeClock < explodeDelay.getValueInt()) {
                    explodeClock++;
                    return;
                }
                explodeClock = 0;
                BlockHitResult explodeHit = new BlockHitResult(mode2Pos.toCenterPos(), mc.player.getHorizontalFacing(), mode2Pos, false);
                WorldUtils.placeBlock(explodeHit, true);
                resetState();
            }
        }
    }

    private boolean isShieldOrFoodActive() {
        boolean food = mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD)
                || mc.player.getOffHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD);
        boolean shield = mc.player.getMainHandStack().getItem() instanceof ShieldItem
                || mc.player.getOffHandStack().getItem() instanceof ShieldItem;
        boolean rightClick = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        return (food || shield) && rightClick;
    }

    private void resetClocks() {
        switchClock = 0;
        glowstoneClock = 0;
        explodeClock = 0;
    }

    private void resetState() {
        mode2Active = false;
        mode2Step = 0;
        mode2Pos = null;
        mode2PlaceHit = null;
        mode2KeyWasPressed = false;
        resetClocks();
    }
}