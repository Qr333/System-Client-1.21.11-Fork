package dlindustries.vigillant.system.module.modules.Combat;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.BlockUtils;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.WorldUtils;
import dlindustries.vigillant.system.utils.rotation.Rotation;
import dlindustries.vigillant.system.utils.RotationUtils;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class SafeAnchor extends Module implements TickListener {
    public enum LookMode { INSTA, LEGIT }

    private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), 71, false);
    private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 10, 1, 1);
    private final NumberSetting totemSlot = new NumberSetting(EncryptedString.of("Totem Slot"), 1, 9, 1, 1);
    private final ModeSetting<LookMode> lookMode = new ModeSetting<>(EncryptedString.of("Look Mode"), LookMode.INSTA, LookMode.class);
    private final BooleanSetting autoExplode = new BooleanSetting(EncryptedString.of("Auto Explode"), true);

    private int step = 0;
    private int delayCounter = 0;
    private boolean isRunning = false;
    private BlockPos targetPos = null;
    private boolean keyWasPressed = false;

    public SafeAnchor() {
        super(EncryptedString.of("Safe Anchor"),
                EncryptedString.of("Charges and explodes a respawn anchor safely"),
                -1,
                Category.CrystalPvP);
        addSettings(activateKey, delay, totemSlot, lookMode, autoExplode);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        reset();
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;
        if (mc.player == null || mc.world == null) return;

        int key = activateKey.getKey();
        boolean keyDown = key != -1 && GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
        boolean keyJustPressed = keyDown && !keyWasPressed;
        keyWasPressed = keyDown;

        if (!isRunning) {
            if (keyJustPressed && mc.crosshairTarget instanceof BlockHitResult hit) {
                BlockPos pos = hit.getBlockPos();
                if (BlockUtils.isBlock(pos, Blocks.RESPAWN_ANCHOR)) {
                    targetPos = pos;
                    isRunning = true;
                    step = 0;
                    delayCounter = 0;
                }
            }
            return;
        }

        if (targetPos == null || !BlockUtils.isBlock(targetPos, Blocks.RESPAWN_ANCHOR)) {
            reset();
            return;
        }

        if (delayCounter < delay.getValueInt()) {
            delayCounter++;
            return;
        }
        delayCounter = 0;

        lookAtAnchor();

        BlockHitResult hit = new BlockHitResult(targetPos.toCenterPos(), mc.player.getHorizontalFacing(), targetPos, false);

        switch (step) {
            case 0 -> {
                if (BlockUtils.isAnchorCharged(targetPos)) {
                    step = 1;
                    return;
                }
                if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                    InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE);
                    return;
                }
                WorldUtils.placeBlock(hit, true);
            }

            case 1 -> {
                if (!BlockUtils.isAnchorCharged(targetPos)) {
                    step = 0;
                    return;
                }
                if (!autoExplode.getValue()) {
                    reset();
                    return;
                }
                int slot = totemSlot.getValueInt() - 1;
                if (mc.player.getInventory().getSelectedSlot() != slot) {
                    mc.player.getInventory().setSelectedSlot(slot);
                    return;
                }
                step = 2;
            }

            case 2 -> {
                WorldUtils.placeBlock(hit, true);
                reset();
            }
        }
    }

    private void lookAtAnchor() {
        if (targetPos == null || mc.player == null) return;
        Vec3d anchorCenter = targetPos.toCenterPos();
        Rotation target = RotationUtils.getDirection(mc.player, anchorCenter);

        if (lookMode.getMode() == LookMode.INSTA) {
            mc.player.setYaw((float) target.yaw());
            mc.player.setPitch((float) target.pitch());
        } else {
            float currentYaw = mc.player.getYaw();
            float currentPitch = mc.player.getPitch();
            float targetYaw = (float) target.yaw();
            float targetPitch = (float) target.pitch();

            float deltaYaw = targetYaw - currentYaw;
            while (deltaYaw > 180) deltaYaw -= 360;
            while (deltaYaw < -180) deltaYaw += 360;

            mc.player.setYaw(currentYaw + deltaYaw * 0.3f);
            mc.player.setPitch(currentPitch + (targetPitch - currentPitch) * 0.3f);
        }
    }

    private void reset() {
        isRunning = false;
        step = 0;
        delayCounter = 0;
        targetPos = null;
    }

    public boolean isRunning() {
        return isRunning;
    }
}