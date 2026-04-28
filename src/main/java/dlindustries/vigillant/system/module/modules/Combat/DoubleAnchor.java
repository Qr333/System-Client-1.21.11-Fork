package dlindustries.vigillant.system.module.modules.Combat;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.BlockUtils;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.WorldUtils;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public final class DoubleAnchor extends Module implements TickListener {
    private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), 71, false)
            .setDescription(EncryptedString.of("Key that starts double anchoring"));
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0.0, 20.0, 0.0, 1.0);
    private final NumberSetting totemSlot = new NumberSetting(EncryptedString.of("Totem Slot"), 1.0, 9.0, 1.0, 1.0);

    private int delayCounter = 0;
    private int step = 0;
    private boolean isAnchoring = false;

    public DoubleAnchor() {
        super(EncryptedString.of("Double Anchor"),
                EncryptedString.of("Automatically Places 2 anchors"),
                -1,
                Category.CrystalPvP);
        addSettings(switchDelay, totemSlot, activateKey);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;
        if (mc.player == null) return;
        if (!hasRequiredItems()) return;
        if (!isAnchoring && !checkActivationKey()) return;

        HitResult crosshairTarget = mc.crosshairTarget;
        if (!(crosshairTarget instanceof BlockHitResult hit)
                || BlockUtils.isBlock(hit.getBlockPos(), Blocks.AIR)) {
            isAnchoring = false;
            resetState();
            return;
        }

        if (delayCounter < switchDelay.getValueInt()) {
            delayCounter++;
            return;
        }

        if (step == 0) InventoryUtils.selectItemFromHotbar(Items.RESPAWN_ANCHOR);
        else if (step == 1) WorldUtils.placeBlock(hit, true);
        else if (step == 2) InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE);
        else if (step == 3) WorldUtils.placeBlock(hit, true);
        else if (step == 4) InventoryUtils.selectItemFromHotbar(Items.RESPAWN_ANCHOR);
        else if (step == 5) {
            WorldUtils.placeBlock(hit, true);
            WorldUtils.placeBlock(hit, true);
        } else if (step == 6) InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE);
        else if (step == 7) WorldUtils.placeBlock(hit, true);
        else if (step == 8) InventoryUtils.setInvSlot(totemSlot.getValueInt() - 1);
        else if (step == 9) WorldUtils.placeBlock(hit, true);
        else if (step == 10) {
            isAnchoring = false;
            step = 0;
            resetState();
            return;
        }
        step++;
    }

    private boolean hasRequiredItems() {
        boolean hasAnchor = false;
        boolean hasGlowstone = false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem().equals(Items.RESPAWN_ANCHOR)) hasAnchor = true;
            if (stack.getItem().equals(Items.GLOWSTONE)) hasGlowstone = true;
        }
        return hasAnchor && hasGlowstone;
    }

    private boolean checkActivationKey() {
        int key = activateKey.getKey();
        if (key == -1 || GLFW.glfwGetKey(mc.getWindow().getHandle(), key) != GLFW.GLFW_PRESS) {
            resetState();
            return false;
        }
        return isAnchoring = true;
    }

    private void resetState() {
        delayCounter = 0;
    }

    public boolean isAnchoringActive() {
        return isAnchoring;
    }
}
